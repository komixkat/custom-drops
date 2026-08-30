# Implementation Notes

This is a full source scaffold of the mod described in the design doc, built
against the project structure in Section 5 exactly. It has **not** been
compiled against a real Minecraft jar — this environment has no network
access and no Minecraft/Fabric artifacts to resolve, and Minecraft 26.2 is a
future release not present in any training data, so a handful of class/method
names are best-effort and flagged below rather than guessed silently.

## What's implemented

- Full Gradle project (`build.gradle`, `settings.gradle`, `gradle.properties`)
  parameterized by `-Pminecraft_version` / `-Pmod_version`, matching the
  `<mc_version>-b<build>` scheme in Section 7b.
- `fabric.mod.json` and `customdrops.mixins.json`.
- Config schema records for all five categories (mob, block, chest, fishing,
  equipment), matching Section 5's file layout, with `LootItemEntry` and
  `LootConditionEntry` as shared building blocks.
- `ConfigLoader` — reads/writes one JSON file per category plus a `meta.json`
  for the active preset, defaulting to empty lists (vanilla-identical) when
  no config exists yet.
- `LootTableInjector` — hooks `LootTableEvents.MODIFY` (Fabric API loot v3),
  matches mob/block config entries to their conventional loot table IDs
  (`entities/<path>`, `blocks/<path>`) and chest/fishing entries directly by
  loot table ID, and appends a `LootPool` built from the config's item pool
  and conditions. This is the "no mixins required" path from Section 2b.
- `LootConditionRegistry` — maps the condition enum onto vanilla
  `LootItemCondition` builders (killed-by-player, random chance, silk touch /
  no silk touch). Looting-level and on-fire conditions are stubbed as TODO-free
  no-ops (return `Optional.empty()`) rather than faked — wire these up against
  real MC 26.2 predicate classes before shipping.
- `EquipmentDropMixin` — injects into `Mob.setDropChance` and overrides it
  when a configured entity/slot match is found, using the vanilla `2.0f`
  always-drop sentinel from Section 2c. This assumes `Mob#setDropChance`
  keeps its Yarn-era name and signature under official mappings for 26.2 —
  **this is exactly the item flagged as an open question in Section 10** and
  needs confirming against the real mappings. If Fabric API ships a
  dedicated entity-equip callback by 26.2, prefer that over the mixin per the
  design doc's own preference ordering.
- `PresetLoader` / `PresetVersionValidator` — loads bundled preset JSON from
  mod resources, refuses to apply a preset whose `targetMinecraftVersion`
  doesn't match the running instance (Rule 4, Section 6), and only then
  merges its entries into the config.
- Three bundled presets (`cozy_survival`, `better_dungeon_loot`,
  `guaranteed_trophies`) covering the example cases from Section 4, with
  `targetMinecraftVersion` templated to `${minecraft_version}` and expanded
  by `processResources` per build target, same mechanism as the mod jar
  version string.
- `IdentifierResolver` / `TagResolver` — every config-supplied ID goes through
  these rather than being compared as a raw string, per Rule 3.
- ModMenu + Cloth Config screen (`CustomDropsModMenuIntegration`) — an
  overview category showing entry counts per category with a pointer to the
  JSON files for detailed pool/condition editing, plus a fully editable list
  for equipment overrides (that schema is flat enough for Cloth Config's
  declarative list builder). Building a nested pool/condition editor for the
  other four categories in Cloth Config's API is a real chunk of additional
  UI work intentionally left out of this pass — the JSON files are fully
  functional in the meantime, and `/customdrops reload` picks up edits
  without a restart.
- `/customdrops reload` command.
- CI workflow exactly as specified in Section 7d, including the numeric
  (not lexicographic) version comparison fix called out in 7a.
- README and LICENSE as specified in Sections 8–9.

## Open questions from Section 10 — status

1. **Fabric API entity-equip callback vs. mixin** — unresolved, as flagged
   above. Shipped with the mixin approach since it's guaranteed to exist;
   swap it out if a cleaner callback lands.
2. **Stonecutter + official mappings compatibility** — not exercised here
   (no build environment). The Gradle setup uses `officialMojangMappings()`
   directly rather than wiring in Stonecutter's multi-version preprocessing,
   since Stonecutter's exact 26.x-era API isn't something I can verify
   offline. The CI matrix builds each version as a separate Gradle
   invocation instead, which is more redundant but doesn't depend on
   Stonecutter's specifics — swap in Stonecutter once you've confirmed it
   against a real checkout.
3. **Minimum supported version** — set to 26.2 per the doc's explicit floor.

## ModMenu UI overhaul (unverified — written without a test build)

Direct response to real usability feedback from testing the previous UI:
hover tooltips were covering buttons and blocking clicks, tooltip text was
confusing, empty lists gave a new player no idea what to do, and Presets
was too prominent for what's meant to be an optional shortcut.

Changes:
- **No more hover tooltips anywhere.** Every `.setTooltip(...)` call is
  gone. Guidance is now a plain `startTextDescription` entry sitting above
  the relevant control — always visible, never a popup, can't cover or
  block anything since it doesn't render on hover.
- **Mob Drops / Block Drops / Chest Loot / Fishing Loot are each their own
  top-level category** (Cloth Config tab), not nested subcategories inside
  one "Loot Tables" tab. Closer to the "click a category, get its own
  screen" feel that was asked for, using Cloth Config's real native
  mechanism (top tab bar) rather than guessing at custom Screen/Button
  code — deliberately avoided writing raw vanilla GUI widget code here,
  since that's a much bigger unverified-API surface (Button/Screen/Layout
  classes were never checked against real 26.2 source this session) and
  guessing there risked repeating the networking-payload failure on
  something far more complex.
- **Every category has a real, working, pre-filled example line** when
  its list is empty, instead of a blank box. E.g. Mob Drops starts with
  `minecraft:zombie|minecraft:diamond*1*1*1*1.0|KILLED_BY_PLAYER|` already
  in the box — delete it, edit it, or leave it (it's a real, harmless,
  small effect if left in, not a broken placeholder string).
- **Every category has a visible boolean toggle at the top** ("Enabled"),
  wired to new `CustomDropsConfig` fields
  (`mobDropsEnabled`/`blockDropsEnabled`/`chestLootEnabled`/
  `fishingLootEnabled`/`equipmentOverridesEnabled`, all default `true`).
  `LootTableInjector` and `EquipmentDropMixin` both check these before
  matching anything. This is the practical equivalent of "a toggle button
  per drop" that's actually achievable with Cloth Config's real widget
  set — a true per-line toggle inside the string-list widget isn't
  something Cloth Config supports without much more custom rendering work.
- **Presets moved to the last category**, retitled "Presets (optional
  shortcuts)", with a text description clarifying it's optional and
  destructive if used (replaces everything below).
- **New "Server Info" category** — shows what a connected server reports
  running (see below), or "not connected" if none. Read-only, no popup.
- **Config schema gotcha handled**: the new boolean fields use nullable
  `Boolean` (not primitive `boolean`) in the JSON meta-file intermediate
  class specifically because Gson's reflection-based deserialization
  bypasses Java field initializers — a missing field in an old config file
  would otherwise silently deserialize to `false` rather than the intended
  default of `true`. `ConfigLoader.applyMeta` treats `null` as `true`
  explicitly.

**Not implemented, explicitly deferred**: a rich item-picker dropdown with
icons and live search (as requested). Cloth Config doesn't provide this as
a built-in widget — it would need custom rendering code (drawing item
icons, hooking into `Minecraft.getInstance().getItemRenderer()` or
similar, live filtering as you type) that hasn't been checked against real
26.2 source at all. Given how much even "safe" guesses needed correcting
this session, this needs its own dedicated phase with a real test loop,
not a blind attempt. The `Browse Loot Tables` category (already
implemented, text-based, filterable via Cloth Config's own built-in search
bar) is the current stand-in.

### Silent server-to-client sync (replacing the chat message)
`ClientCustomDropsMod` no longer sends a chat message on receiving
`CustomDropsSyncPayload` — it just stores the summary string silently.
The server still sends it automatically on join and after `/customdrops
reload` (see `CustomDropsMod`), matching "the server in the background
automatically communicates with the client" rather than an intrusive
chat announcement. The info surfaces only when the player actually opens
ModMenu's new "Server Info" category.

## Multiplayer & per-world config phase (unverified — written without a test build)

This phase adds architecture the mod didn't have before, requested directly:
per-world config that overrides a global default, an explicit client-side
safety guard, and an informational server-to-client sync. None of this has
been compiled or run. Confidence varies a lot by piece:

**High confidence:**
- `EquipmentDropMixin` now returns immediately if `self.level().isClientSide()`
  is true, before touching any config. This is a simple, well-established
  check unrelated to the 26.x rename wave.
- Per-world config resolution logic itself (`ConfigLoader.loadWithWorldOverride`)
  is pure Java/Gson, no vanilla API surface at all — the risk here is only in
  what *calls* it, not the method itself.

**Medium confidence (established vanilla/Fabric API, not touched by anything
we found renamed this session, but not independently re-verified against
26.2's real decompiled source either):**
- `server.getWorldPath(LevelResource.ROOT)` for finding the world save folder.
- `ServerLifecycleEvents.SERVER_STARTED` / `SERVER_STOPPING` — these are
  Fabric API's own events (not vanilla), and have been stable for years.
- `Commands.hasPermission` / `Command.SINGLE_SUCCESS` reuse from earlier
  session work — already confirmed working.
- `server.getWorldData().getLevelName()` (used only for a log line).

**Low confidence — genuinely likely to need fixing:**
- `CustomDropsSyncPayload` and `ClientCustomDropsMod`. Fabric's
  payload/`StreamCodec` API has changed shape multiple times across
  versions, and this hasn't been checked against real decompiled MC 26.2 or
  fabric-api 0.158.0+26.2 source at all. Kept deliberately simple (one
  string field, not a multi-field composite codec) specifically to minimize
  how much could be wrong, and every registration/send call is wrapped in
  `try/catch (Throwable)` so a *runtime* failure degrades to "no server info
  shown" rather than crashing — but a *compile-time* signature mismatch
  (wrong method name, wrong generic bounds) will still fail the build, and
  try/catch can't help with that. If `check-api-drift` or a real build
  flags this file, that's expected — verify against decompiled
  `net.fabricmc.fabric.api.networking.v1.*` and
  `net.minecraft.network.protocol.common.custom.CustomPacketPayload`
  sources the same way everything else this session got nailed down.

### What this does NOT do (by design)
- The client never registers `LootTableEvents` or runs any drop-modifying
  logic. Per Minecraft's architecture, a client connected to someone else's
  server never runs a local `MinecraftServer` instance at all — loot tables
  are purely server-authoritative and never loaded/evaluated on a pure
  remote client. `ClientCustomDropsMod` only ever displays what a server
  reports; it cannot and does not act on it.
- The sync payload carries only counts and the active preset name (a
  human-readable summary string) — never full pool/item contents, since the
  point is "let players see the server is customized," not exposing exact
  drop tables as a strategy/spoiler concern.
- World-to-world config migration across Minecraft version upgrades (26.2
  world opened in a future 26.3 build) is handled "for free" by storing
  config inside the world save folder rather than the global mod config
  folder — it travels with the world automatically. What's *not* handled
  yet: if a future mod version changes the JSON schema (e.g. adds a new
  required field to a record), old per-world JSON could fail to parse. This
  needs a real schema-versioning/migration story before it's fully solid;
  right now `ConfigLoader` just falls back to an empty list on any parse
  failure, which is safe but silently drops the user's old per-world config
  on a schema change. Worth revisiting once presets/schemas actually
  change between versions in practice.

### Still deferred
- A visual in-mod pool/entry editor (replacing the compact line-format
  strings) as an alternative to an external website generator. The current
  ModMenu UI (Presets + per-category Loot Tables sections) plus the new
  `/customdrops search <query>` command are the existing answer to "make
  this easy without external tools" — a richer visual editor is a bigger,
  separate undertaking on top of Cloth Config's constraints, not something
  to guess at blind.
- Schema versioning/migration for per-world config across mod version
  updates that change the JSON shape (not just Minecraft version updates,
  which are already handled for free via world-relative storage).

## What to do before testing this phase

1. Build and run. If `CustomDropsSyncPayload`/`ClientCustomDropsMod` fail to
   compile, that's the expected highest-risk spot — check them against real
   decompiled `net.fabricmc.fabric.api.networking.v1.*` source the same way
   `EquipmentDropMixin` and `LootConditionRegistry` got fixed earlier.
2. Test per-world config: put a `mob_drops.json` inside a world's
   `<world>/customdrops/` folder (not the global `config/customdrops/`) and
   confirm it applies to that world specifically, while a different world
   still uses the global default.
3. Test the client-side guard: this can't really be "tested" for absence of
   effect directly, but confirm the mod still works correctly in
   singleplayer (where the integrated server IS the authoritative side) —
   that's the case that must keep working.
4. Confirm the join-message shows up when connecting to a server running
   this mod, and doesn't show anything when connecting to a vanilla server.
