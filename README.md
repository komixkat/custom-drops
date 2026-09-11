# Custom Drops

A Fabric mod that lets you customize what mobs, blocks, chests, fishing spots, and equipment drop, entirely through an in-game menu. No datapack editing required.

## Features

- Rewrite loot tables for mobs, blocks, chests, fishing, and more (trial chambers, archaeology, shearing, charged-creeper heads, and anything else built on Minecraft's loot table system)
- Wildcard targeting: `minecraft:chests/village/*` matches every village building's loot table in one entry
- Force mob equipment (like a drowned's trident) to always drop
- Per-item conditions and enchantments on dropped items
- Imports: save a config as an import, then apply it to the editor, a single-player world, or a connected server later
- CHAOS: Lootstorm: one random item per loot source at a random count (1-64), with every item placed somewhere
- Real in-game menu: navigate categories directly from the main screen using actual buttons
- Per-world config that overrides the default config, without needing a server restart
- Multiplayer-aware: a connected server quietly reports what it's running to clients with the mod installed (visible in Settings, never a chat announcement)
- Auto-reload in singleplayer: changes apply immediately when saving in the GUI
- Default install behaves exactly like vanilla until you change something

## Installation

Requires Fabric Loader 0.19.3+, Fabric API, and ModMenu (all installed together as normal Fabric setup). Download the jar for your Minecraft version from the Releases page and drop it in your mods folder.

## Configuration

Open the config screen from ModMenu (this mod ships with a ModMenu entry point). The menu is organized as:

- **Mob Drops** — control what specific mobs drop when they die
- **Block Drops** — control what specific blocks drop when broken
- **Chest Loot** — control any loot table by its id (dungeons, villages, trial chambers, archaeology, etc.)
- **Fishing Loot** — control fishing-specific loot tables
- **Equipment** — force mob equipment to always drop
- **Imports** — paste a code, name it, and apply the saved config to the editor, a single-player world, or a connected server
- **CHAOS: Lootstorm** — generate a config where every loot source drops one random item at a random count. Generating auto-saves it as `CHAOS <timestamp>` and drops you on an apply screen: overwrite a single-player world's drops with it, or make it the active config.
- **Configs** — switch configs, create or duplicate, reset to vanilla, and give a single-player world its own config (or clear it again with "Clear this World's Config")
- **Settings** — category toggles, browse loot tables, codes, server config

Changes take effect immediately in singleplayer when you save.

## Commands

Type `/cd` to see both subcommands in the command list.

- `/cd gui` — open the config menu from anywhere (server sends the menu to you).
- `/cd reset` (operator) — restore to vanilla: empties the active config's drop rules **and removes this world's own config** (so a world forced onto CHAOS goes back to following the active config). Configs and imports stay saved.

## Codes

Configs can be shared between worlds or with friends as short codes. From **Codes** in the Settings menu, copy your active config as a code (a full config fits in one chat message), then paste and name it on the other side — it's saved as an import and applied from the Imports screen. Codes from older versions still import fine.

## Autocomplete

All ID fields (items, blocks, entities, loot tables, enchantments) feature live autocomplete dropdowns. Start typing to see matching suggestions from the game's registries.

Entity and block target fields also suggest tags. Type `#` (or just the tag name, like `logs`) to see `#tag` suggestions from the first letter — `#l` surfaces `#minecraft:logs` immediately. The matched part of each suggestion is highlighted in the dropdown. Tags you pick often float to the top when you type `#` alone (remembered across sessions), and the stored id is the tag name without the `#`. A `#` query that matches no tag of the field's kind shows a hint instead of an empty box. Entity and block tags in use by the world are gathered from the live registry, with a bundled vanilla tag list as an offline fallback.

For a full scrollable list with member counts, use **Browse Tags** under Tools (also linked from Default Values, Browse Loot Tables, and the "Browse entries in this tag" button on a rule that targets a tag). Click a tag to drill into its actual entries (live datapack tags like `#c:natural_logs/nether` resolve to the real block/entity/item ids from the connected world), click an entry to copy that id, right-click a tag to copy its `#tag` id, and **‹ All tags** to go back. Browsing survives window resize thanks to an on-the-fly relayout, and saving shows a brief "Saved ✓" toast in the corner.

The rule search bar at the top of the Mob/Block/Chest/Fishing screens filters the rule list by the target id **or** any item in the rule's output (e.g. `stone` shows rules that drop stone).

Navigation categories remember whether they're open or collapsed across screens, so they stay expanded when you come back to a menu. Editor buttons (`+ Add Item`, `Duplicate`, `Delete Entry`, `Load Defaults`, `View Vanilla`) reflow to stack vertically instead of overlapping on small windows.

## Build

```
./gradlew build
```

GitHub Actions runs daily and automatically builds a release for each Minecraft version that has a Fabric loader. Tags look like `v26.2`.

## License

PolyForm Noncommercial 1.0.0. Free to use and modify, not for commercial use.