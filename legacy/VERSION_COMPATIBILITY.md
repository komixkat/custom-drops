# Version Compatibility Notes

This file tracks what's actually been confirmed (against real decompiled
source or real Maven metadata) for each supported Minecraft version, so
adding support for the next one (26.3, 27.x, ...) doesn't require
re-discovering everything from scratch the way 26.2 did.

Two kinds of entries below: **Confirmed** (verified against real decompiled
source or a real registry response) and **Inferred** (a reasonable guess
following a pattern seen elsewhere, not independently verified). Treat
Inferred entries as the first thing to double-check if something breaks on
a new version.

## Toolchain requirements

| Minecraft | Required JDK | Loom plugin | Gradle | Notes |
|---|---|---|---|---|
| 26.2 | 25 | `net.fabricmc.fabric-loom` 1.17.19 | 9.5.1 | Non-obfuscated (see below). JDK requirement is now auto-detected in CI from Mojang's per-version manifest (`javaVersion.majorVersion`) — see `discover-versions` job in `build.yml`. Gradle 9.5.1 is still hand-pinned; if a future MC version needs a JDK that 9.5.1 can't run under, that'll need bumping manually (no automated way to query "which Gradle line supports which JDK" was found this session). |

## Obfuscation status

Minecraft versions **after 1.21.11** ship non-obfuscated — the game jar
uses Mojang's real class/method names directly, no deobfuscation mapping
step needed. This is why `build.gradle` uses the plain
`net.fabricmc.fabric-loom` plugin ID with no `mappings { }` block at all
(the older `fabric-loom` legacy ID and `net.fabricmc.fabric-loom-remap` ID
are for pre-1.21.11 obfuscated versions only).

**Confirmed**: Loom publishes real releases under
`net.fabricmc.fabric-loom:net.fabricmc.fabric-loom.gradle.plugin` on
`maven.fabricmc.net`, versioned like `1.17.19` (three-part, not `1.17`).
Check `https://maven.fabricmc.net/net/fabricmc/fabric-loom/net.fabricmc.fabric-loom.gradle.plugin/maven-metadata.xml`
directly for the current list — this was far more reliable than trying to
infer a version number from directory listings or web search.

## Confirmed API renames (Minecraft 26.1+)

All of these were verified against real decompiled source
(`gradle genSourcesWithVineflower -Pminecraft_version=26.2`), not guessed:

| Old (pre-26.x / Yarn-style) | New (26.2+) | Where |
|---|---|---|
| `net.minecraft.resources.ResourceLocation` | `net.minecraft.resources.Identifier` | everywhere |
| `Registry.get(id)` returning the value | `Registry.getValue(id)` | `BuiltInRegistries.*` |
| `Registry.get(id)` | now returns `Optional<Holder.Reference<T>>` instead of the value directly | `BuiltInRegistries.*` |
| `ResourceKey<T>.location()` | `ResourceKey<T>.identifier()` | *Inferred* — followed the Identifier rename pattern, not independently re-verified beyond compiling clean |
| `WorldVersion.getName()` | `WorldVersion.name()` | *Inferred* — same caveat |
| `EntityType<?>.is(TagKey)` | doesn't exist; call `.is(TagKey)` on the **entity instance** itself instead (e.g. `Mob`/`LivingEntity`) | confirmed via `Mob.java`'s own `this.is(EntityTypeTags.BURN_IN_DAYLIGHT)` |
| `CommandSourceStack.hasPermission(int)` | gone; use `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` (or `LEVEL_MODERATORS`/`LEVEL_ADMINS`/`LEVEL_OWNERS`) as the Brigadier `.requires(...)` predicate | confirmed via `Commands.java` |
| `net.minecraft.advancements.critereon.*` (predicates package) | `net.minecraft.advancements.predicates.*` | confirmed (`EnchantmentPredicate`, `MinMaxBounds` both moved here; `ItemPredicate` very likely also moved here, seen in `MatchTool`'s import) |
| `net.minecraft.world.item.enchantment.ItemEnchantmentsPredicate` | `net.minecraft.core.component.predicates.EnchantmentsPredicate` (component-based predicate system) | confirmed exists at this path; full wiring into `ItemPredicate.Builder` not verified — see `LootConditionRegistry`'s custom condition workaround |
| `ItemPredicate.Builder.withSubPredicate(DataComponentType, predicate)` | `ItemPredicate.Builder.withComponents(DataComponentMatchers)` | confirmed method exists on `ItemPredicate.Builder`; exact `DataComponentMatchers` wiring not verified |
| `LootItemCondition` requiring `getType()`/`LootItemConditionType` | now requires `MapCodec<? extends LootItemCondition> codec()` plus `getReferencedContextParams()` (from `LootContextUser`) | confirmed via real `LootItemCondition.java` and `MatchTool.java` |
| — | `ItemStack` still exists (mutable stack), but a new `ItemInstance` interface also exists (`TypedInstance<Item> & DataComponentGetter`) — `LootContextParams.TOOL` is typed as `ItemInstance`, and both types share `.getOrDefault(DataComponentType, default)` for reading components like `DataComponents.ENCHANTMENTS` | confirmed via `MatchTool.java`, `ItemInstance.java`, `ItemStack.java` |
| `fabric-permissions-api` (`net.fabricmc.fabric.api.permission.v1.Permissions`) | not a dependency of this project — vanilla now has its own `net.minecraft.server.permissions.Permissions`/`PermissionSet`/`PermissionCheck` system, and `CommandSourceStack` implements Fabric Loader's `PermissionContextOwner` marker interface directly (not from a separate fabric-api module) | confirmed |
| `me.shedaniel.cloth:cloth-config-fabric` version scheme | now matches the Minecraft version (`26.2.155`, not `15.0.130`-style) | confirmed via Modrinth |
| ModMenu Gradle dependency | `implementation`, not `modImplementation`, from Minecraft 26.x onward | confirmed via ModMenu's own docs |
| all `modImplementation` for Fabric Loader / Fabric API / Cloth Config | plain `implementation` | confirmed — Loom's non-obfuscated plugin ID provides no `mod*` configurations at all, since there's no remapping step for them to manage |

## How this file gets used

`build.yml`'s `check-api-drift` job decompiles the newest discovered MC
version and diffs a handful of tracked vanilla files
(`api-baseline/tracked-files.txt`) against a stored baseline
(`api-baseline/**/*.sig`), posting any differences to the job summary.
This catches signature changes in files we already know we depend on —
it does **not** discover brand-new renames on its own, and its per-file
"signature" extraction only catches lines starting with `public`,
`protected`, or `default` (interface methods with no modifier, like
`codec()` on `LootItemCondition`, won't show up even if their signature
changes — a known gap, not a bug).

When bumping to a new Minecraft version:
1. Check this file's toolchain table for what auto-detects vs. what needs
   manual bumping (Gradle version, mainly).
2. Check the `check-api-drift` job's summary output for the run against
   the new version.
3. Anything it flags (or anything not in `tracked-files.txt` at all, which
   is most of the API surface) still needs the same manual process this
   session used: `gradle genSourcesWithVineflower -Pminecraft_version=<new>`
   locally, then grep the real decompiled source for the symbol that broke.
4. Add newly-confirmed renames to the table above and, if it's a file this
   mod depends on directly, add it to `api-baseline/tracked-files.txt` with
   a matching `.sig` baseline for next time.
