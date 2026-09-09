# Custom Drops

A Fabric mod that edits vanilla loot tables from an in-game menu. The game plays like vanilla until you change a rule.

## Install

1. Download the jar for your Minecraft version from the [Releases](https://github.com/komixkat/custom-drops/releases) page. Tags look like `v26.2`.
2. Put it in your `mods` folder.

Requires Fabric Loader and Fabric API. ModMenu recommended.

## Use

Open the menu from ModMenu or with `/customdrops`. It has a screen per loot category: mob drops, block drops, chests, fishing, equipment, and settings (toggles, presets, export/import). Changes apply on save.

## Presets

Six presets replace the whole config in one click. Each has about 100 rules.

- Trophy Hunter: mobs drop rare heads and keepsakes
- Lootery Plus: overall loot overhaul, richer than vanilla
- Explorer's Fortune: structures and archaeology pay out
- Civilized Survival: small quality-of-life extra drops
- Ocean Depths: more reasons to go diving
- OP X: exaggerated loot and endgame boss spoils

## Build

```
./gradlew build
```

GitHub Actions checks daily for a new Minecraft release and builds it automatically.

## License

PolyForm Noncommercial 1.0.0. The pre-rebuild code is archived in `legacy/`.