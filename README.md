# Custom Drops

A Fabric mod that edits vanilla loot tables from an in-game menu. The game plays like vanilla until you change a rule.

## Features

| Feature | Description |
| --- | --- |
| **Loot rewrite** | Redefine mob, block, chest, fishing, and equipment drops |
| **Wildcards** | Match every sub-table, e.g. `minecraft:chests/village/*` |
| **Equipment force-drop** | Make mobs always drop their gear (drowned, etc.) |
| **Item control** | Per-item conditions and enchantments |
| **Tags** | Target `#tag` entries; browse live tag members in the GUI |
| **Autocomplete** | Live suggestions on every ID field |
| **Imports** | Save a config and apply it to an editor, world, or server |
| **CHAOS: Lootstorm** | One random item (count 1-64) from every loot source |
| **Per-world configs** | Give each world its own drop config, no restart |
| **Multiplayer-aware** | A server reports what it's running to mod clients |
| **Instant apply** | Singleplayer changes take effect on save |

## Installation

1. Download the jar for your Minecraft version from the [Releases](https://github.com/komixkat/custom-drops/releases) page (tags look like `v26.2`).
2. Put it in your `mods` folder.

## Use

Open the menu from ModMenu or with:

```
/cd gui
```

Rule editors have tags, wildcards, autocomplete, default/vanilla reference, and per-world configs. Changes apply on save.

Reset a world back to vanilla:

```
/cd reset
```

Share a config as a short code from **Codes** (in Settings) — paste it on the other side and apply it as an import.

## Build

```
./gradlew build
```

GitHub Actions builds a release automatically for each Minecraft version. Tags look like `v26.2`.

## License

PolyForm Noncommercial 1.0.0. Free to use and modify, not for commercial use.