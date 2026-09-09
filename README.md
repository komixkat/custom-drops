# Custom Drops

Custom Drops is a Fabric mod that lets you control what things drop in Minecraft, right from an in-game menu. No datapack skill needed.

You can change what mobs drop when they die, what blocks drop when you break them, what shows up in chests and fishing, and force a mob's equipment (like a drowned's trident) to always drop. Every change applies immediately in singleplayer when you save.

## Features

- **Loot table control.** Mob drops, block drops, chest loot, fishing loot, and more (shearing, archaeology, trial chambers, even charged-creeper heads). Anything built on Minecraft's loot table system is fair game.
- **Wildcard targeting.** One rule like `minecraft:chests/village/*` covers every village building at once, instead of writing a rule per building.
- **Force equipment drops.** Want a drowned to always drop its trident, or an evoker to drop its totem? Just turn it on for that mob and slot.
- **Per-item conditions and enchantments.** Fine-tune when an item drops (killed by player, on fire, silk touch, fortune level) and what enchantments ride along with it.
- **Fortune support.** Ore rules can scale extra drops with your Fortune level instead of being a flat number.
- **Rule search.** With a few hundred rules loaded, a search box filters the list instantly.
- **Complete autocomplete.** Every item, block, entity, and loot table field offers live suggestions as you type.
- **Built-in presets.** Six hand-made presets, each with roughly 100 rules, replace the config in one click:
  - **Trophy Hunter** (every mob has a rare head or keepsake to chase)
  - **Lootery Plus** (a rich, slightly above vanilla loot overhaul)
  - **Explorer's Fortune** (every structure and archaeology site pays off)
  - **Civilized Survival** (fair quality of life, crops and animals give a little extra)
  - **Ocean Depths** (the sea is worth diving into)
  - **OP X** (goofy amounts of loot and bosses drop endgame spoils)
- **Explicit confirmations.** Switching presets, changing the active profile, or linking a world to a config always asks for confirmation first, so nothing is ever overwritten by accident.
- **Per-world config.** Each world can have its own drop settings that override the global default, without a server restart.
- **Multiplayer aware.** When you connect to a server running the mod, the client quietly shows what the server uses in the settings screen. It never broadcasts it in chat, and it never touches loot tables on someone else's server.
- **Vanilla until you change something.** Fresh installs behave exactly like plain Minecraft. The menu is only there when you want it.

## Presets

The in-game presets screen lists all six presets with a short description. Picking one replaces every category's entries with that preset's rules. You can start from a preset and then edit individual rules on top of it.

## Requirements

- Minecraft Java Edition for the version you download (see Releases below)
- Fabric Loader 0.19.3 or newer
- Fabric API (hard dependency)

ModMenu is recommended. It is the easiest way to open the config screen. Without it you can still open the menu with the `/customdrops` command.

## Installation

1. Download the jar for your Minecraft version from the [Releases](https://github.com/komixkat/custom-drops/releases) page. Each Minecraft release has its own tag (for example `v26.2`), so pick the one matching your game version.
2. Put the jar in your `mods` folder.
3. Launch the game with Fabric and Fabric API installed.

Updates to the same Minecraft version replace the previous jar in place. When a new Minecraft version is released, its own tag and jar show up on the Releases page automatically.

## In-game configuration

Open the config screen from ModMenu, or run `/customdrops`. The menu is organized into:

- **Mob Drops.** What mobs drop when they die.
- **Block Drops.** What blocks drop when broken.
- **Chest Loot.** Any loot table by its id (dungeons, villages, trial chambers, archaeology, and more).
- **Fishing Loot.** What the fishing loot tables hand out.
- **Equipment.** Force mob equipment to always drop.
- **Settings.** Category toggles, the preset selector, browsing loot tables, and export/import of your config.

## Building from source

Clone the repository and run:

```
./gradlew build
```

The build also bundles a full reference list of vanilla loot tables and item, block, and entity ids for the running game version, so autocomplete and preset validation always match the version they were built for.

Every Minecraft version is built and released automatically by GitHub Actions, so you usually never need to build it yourself.

## License

PolyForm Noncommercial 1.0.0. Free to use and modify for personal and noncommercial projects. See the LICENSE file for details.

The pre-rebuild version of this mod is archived under `legacy/` for reference.