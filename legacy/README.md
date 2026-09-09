# Custom Drops

A Fabric mod that lets you customize what mobs, blocks, chests, fishing
spots, and equipment drop, entirely through an in-game menu. No datapack
editing required.

## Features

- Rewrite loot tables for mobs, blocks, chests, fishing, and more (trial
  chambers, archaeology, shearing, charged-creeper heads, and anything
  else built on Minecraft's loot table system - see `EDITABLE_DROPS.md`)
- Wildcard targeting: `minecraft:chests/village/*` matches every village
  building's loot table in one entry, instead of needing one per building
- Force mob equipment (like a drowned's trident) to always drop
- Built-in presets: Cozy Survival, Better Dungeon Loot, Guaranteed Trophies
- Real in-game menu: navigate Mod Info → Mod Settings → Mod Config →
  category → entry, using actual buttons, sliders, toggles, and dropdowns
  (works via ModMenu, or standalone with the `/customdrops` command)
- Per-world config that overrides the global default, without needing a
  server restart
- Multiplayer-aware: a connected server quietly reports what it's running
  to clients with the mod installed (visible in the Server Config screen,
  never a chat announcement), and client-side logic never touches loot
  tables itself when connected to someone else's server
- Default install behaves exactly like vanilla until you change something

## Installation

Requires Fabric Loader 0.19.3+ and Fabric API. Fabric API is a hard
dependency; ModMenu and Cloth Config are recommended (the mod's core
logic works without them, but you'll need Cloth Config for the in-game
menu). Download the jar for your Minecraft version from the Releases page
and drop it in your mods folder.

## Configuration

Open the config screen from ModMenu (or via `/customdrops` if you're not
using ModMenu). The menu is organized as:

- **Mod Info** — what the mod does and how to get started
- **Mod Settings** — quick per-category on/off toggles and the preset
  switcher
- **Mod Config** — the actual editing screens, one per category (Mob
  Drops, Block Drops, Chest Loot, Fishing Loot, Equipment Overrides),
  each with real text fields, sliders, and toggles per entry
- **World Settings** — the same editing screens, but scoped to your
  current singleplayer/hosted world specifically, overriding the global
  default for that world only
- **Server Config** — read-only: what a connected server reports running
- **Search** — browse and filter every known real loot table id

Changes take effect on save; use `/customdrops reload` in-game to apply
edits made directly to the JSON files without restarting. See
`EDITABLE_DROPS.md` for a full list of known loot table ids and
`VERSION_COMPATIBILITY.md` for what's been verified against real
decompiled game data versus what's still inferred.

## License

PolyForm Noncommercial 1.0.0. Free to use and modify, not for commercial
use.
