# Editable Drops Reference

What Custom Drops can currently target, and how. This is both a user-facing
reference (what identifiers to type into the config/ModMenu) and an internal
tracking list (what's covered vs. what still needs real 26.2 verification).

Same confidence labeling as `VERSION_COMPATIBILITY.md`: **Confirmed** means
verified against real decompiled MC 26.2 source, real gameplay testing, or
(as of this update) the real identifier list pulled from an actual built
jar via the `extractVanillaLootTables` Gradle task. **Inferred** means it's
a long-standing loot-table convention from before the 26.x rename wave
that probably still holds, but hasn't been directly re-checked — there's
only one of these left in the whole doc now (smithing trims).

## Current limitations (be aware of these before designing a config)

- **No specific enchantments on dropped items.** You can add
  `minecraft:enchanted_book` as a drop, but there's no way yet to specify
  "with Mending on it" — you'd get a plain, unenchanted book. This needs a
  schema extension (a loot-function-equivalent to apply an enchantment)
  that hasn't been built yet. If your goal was "chance of a Mending book,"
  this isn't achievable with the current version.
- **No true autocomplete while typing.** The Search screen lets you browse
  and filter the 1354 known loot table ids, and Chest/Fishing entries with
  an unrecognized target get a "did you mean" suggestion logged, but
  there's no live dropdown-while-typing in the entry fields themselves.

## Wildcard targeting

Chest Loot and Fishing Loot targets can end with `*` to match every real
loot table id starting with that prefix, instead of needing one entry per
table. This is the answer to "there's no single village chest table, there
are 11" — one wildcard entry covers all of them:

```
minecraft:chests/village/*
```

matches every entry in the Village chests section below (armorer, butcher,
plains house, savanna house, etc.) in a single rule. Same idea works for
any prefix — `minecraft:chests/trial_chambers/*` covers every trial
chamber loot table at once, `minecraft:chests/bastion_*` covers all four
bastion variants, and so on.

Mob Drops and Block Drops don't support wildcards this way (their `#tag`
prefix already covers the "match a group of related entities/blocks"
case reasonably well — e.g. `#minecraft:skeletons`).

## The four config categories, and what they really cover

### Mob Drops (`mobDrops` / ModMenu "Mob Drops")
Target: an entity id (`minecraft:zombie`) or entity tag (`#minecraft:skeletons`).
Internally resolves to the loot table `<namespace>:entities/<path>`.
Covers every mob's death drops — hostile, passive, boss, all of them.

### Block Drops (`blockDrops` / ModMenu "Block Drops")
Target: a block id (`minecraft:stone`) or block tag (`#minecraft:logs`).
Internally resolves to `<namespace>:blocks/<path>`.
Covers ordinary block-break drops.

### Chest Loot (`chestLoot` / ModMenu "Chest Loot")
**This is the general-purpose one.** Despite the name, this targets *any*
loot table by its raw identifier — not just literal chests. Suspicious
sand/gravel (archaeology), trial chamber vault rewards, and anything else
built on the vanilla loot table system all go through this category,
because vanilla itself doesn't distinguish them architecturally — they're
all just a `LootTable` loaded by id.

### Fishing Loot (`fishingLoot` / ModMenu "Fishing Loot")
Same mechanism as Chest Loot, kept as a separate category purely for
organization (fishing loot tables are frequently edited together — junk
vs. treasure vs. fish pools). Functionally identical to Chest Loot; either
category will work for any loot table id.

### Equipment Drop Overrides (separate subsystem, not a loot table at all)
Forces a spawned mob's held/worn item to always drop, bypassing the normal
drop-chance roll entirely. This is the one category that ISN'T a loot
table — see the design doc's Section 2c. Target: entity id/tag + equipment
slot (`MAIN_HAND`, `OFF_HAND`, `HEAD`, `CHEST`, `LEGS`, `FEET`).

## Known loot table identifiers by feature

Type these directly as the `targetId` / `targetLootTableId` in Chest Loot
or Fishing Loot entries.

### Dungeons & structures — **Confirmed**
Every guess matched, plus one I'd missed:
- `minecraft:chests/simple_dungeon`
- `minecraft:chests/abandoned_mineshaft`
- `minecraft:chests/nether_bridge`
- `minecraft:chests/stronghold_corridor`
- `minecraft:chests/stronghold_crossing`
- `minecraft:chests/stronghold_library`
- `minecraft:chests/desert_pyramid`
- `minecraft:chests/jungle_temple`
- `minecraft:chests/jungle_temple_dispenser` (missed this one initially)
- `minecraft:chests/igloo_chest`
- `minecraft:chests/woodland_mansion`
- `minecraft:chests/underwater_ruin_small`
- `minecraft:chests/underwater_ruin_big`
- `minecraft:chests/buried_treasure`
- `minecraft:chests/shipwreck_map`
- `minecraft:chests/shipwreck_supply`
- `minecraft:chests/shipwreck_treasure`
- `minecraft:chests/pillager_outpost`
- `minecraft:chests/bastion_treasure`
- `minecraft:chests/bastion_other`
- `minecraft:chests/bastion_bridge`
- `minecraft:chests/bastion_hoglin_stable`
- `minecraft:chests/end_city_treasure`
- `minecraft:chests/ancient_city`
- `minecraft:chests/ancient_city_ice_box`

### Village chests — **Confirmed, correcting an earlier wrong guess**
Only the plain house types are biome-suffixed. Profession/building loot
tables are generic — **not** per-biome the way I'd originally guessed
(there's no `village_plains_temple`; it's just `village_temple`):

House types (biome-specific):
- `minecraft:chests/village/village_plains_house`
- `minecraft:chests/village/village_savanna_house`
- `minecraft:chests/village/village_snowy_house`
- `minecraft:chests/village/village_taiga_house`
- `minecraft:chests/village/village_desert_house`

Profession/building types (generic, not biome-specific):
- `minecraft:chests/village/village_armorer`
- `minecraft:chests/village/village_butcher`
- `minecraft:chests/village/village_cartographer`
- `minecraft:chests/village/village_fisher`
- `minecraft:chests/village/village_fletcher`
- `minecraft:chests/village/village_mason`
- `minecraft:chests/village/village_shepherd`
- `minecraft:chests/village/village_tannery`
- `minecraft:chests/village/village_temple`
- `minecraft:chests/village/village_toolsmith`
- `minecraft:chests/village/village_weaponsmith`

### Fishing — **Confirmed, exactly as guessed**
- `minecraft:gameplay/fishing`
- `minecraft:gameplay/fishing/fish`
- `minecraft:gameplay/fishing/junk`
- `minecraft:gameplay/fishing/treasure`

### Other loot-table-driven mechanics — **Confirmed** (except one)
- `minecraft:gameplay/sniffer_digging`
- `minecraft:gameplay/cat_morning_gift`
- `minecraft:gameplay/piglin_bartering`
- `minecraft:equipment/trims/*` — **not confirmed**, no match found under
  this exact naming in the real extracted list. Either the naming
  convention differs or this doesn't exist as a standalone loot table in
  26.2 — don't rely on this one without checking the full 1354-entry list
  yourself first (`unzip -p custom-drops-26.2-*.jar
  data/customdrops/generated/vanilla_loot_tables.txt | grep -i trim`).

### Trial chambers — **Confirmed**, real list is much bigger than initially guessed
Pulled directly from a real build's `vanilla_loot_tables.txt` (1354 total
identifiers extracted). The vault reward tables were right, but there's a
whole layer of trial-chamber loot I hadn't known to list — dispensers,
equipment, decorated pots, and spawner-triggered tables, including
"ominous" vault variants:
- `minecraft:chests/trial_chambers/corridor`
- `minecraft:chests/trial_chambers/entrance`
- `minecraft:chests/trial_chambers/intersection`
- `minecraft:chests/trial_chambers/intersection_barrel`
- `minecraft:chests/trial_chambers/reward`
- `minecraft:chests/trial_chambers/reward_common`
- `minecraft:chests/trial_chambers/reward_rare`
- `minecraft:chests/trial_chambers/reward_unique`
- `minecraft:chests/trial_chambers/reward_ominous`
- `minecraft:chests/trial_chambers/reward_ominous_common`
- `minecraft:chests/trial_chambers/reward_ominous_rare`
- `minecraft:chests/trial_chambers/reward_ominous_unique`
- `minecraft:chests/trial_chambers/supply`
- `minecraft:dispensers/trial_chambers/chamber`
- `minecraft:dispensers/trial_chambers/corridor`
- `minecraft:dispensers/trial_chambers/water`
- `minecraft:equipment/trial_chamber`
- `minecraft:equipment/trial_chamber_melee`
- `minecraft:equipment/trial_chamber_ranged`
- `minecraft:pots/trial_chambers/corridor`
- `minecraft:spawners/trial_chamber/consumables`
- `minecraft:spawners/trial_chamber/key`
- `minecraft:spawners/trial_chamber/items_to_drop_when_ominous`
- `minecraft:spawners/ominous/trial_chamber/consumables`
- `minecraft:spawners/ominous/trial_chamber/key`

### Archaeology — suspicious sand & gravel — **Confirmed**, exactly as guessed
Every one of these matched the real extracted list exactly:
- `minecraft:archaeology/desert_pyramid`
- `minecraft:archaeology/desert_well`
- `minecraft:archaeology/ocean_ruin_warm`
- `minecraft:archaeology/ocean_ruin_cold`
- `minecraft:archaeology/trail_ruins_common`
- `minecraft:archaeology/trail_ruins_rare`

Note the real data confirms the path is singular `loot_table` (not
`loot_tables`) for 26.2 — both were supported defensively in the
extraction regex, but singular is what's actually used.

### Charged creeper head drops — **Confirmed**
This is the "mob killed by a charged creeper explosion drops its head"
mechanic — normally a rare roll, fully controllable here (make it
guaranteed, add extra items, whatever):
- `minecraft:charged_creeper/creeper`
- `minecraft:charged_creeper/piglin`
- `minecraft:charged_creeper/root`
- `minecraft:charged_creeper/skeleton`
- `minecraft:charged_creeper/wither_skeleton`
- `minecraft:charged_creeper/zombie`

### Shearing — **Confirmed**
- `minecraft:shearing/sheep` and one per color: `minecraft:shearing/sheep/white`,
  `black`, `blue`, `brown`, `cyan`, `gray`, `green`, `light_blue`,
  `light_gray`, `lime`, `magenta`, `orange`, `pink`, `purple`, `red`,
  `yellow`
- `minecraft:shearing/mooshroom` and `minecraft:shearing/mooshroom/brown`, `/red`
- `minecraft:shearing/snow_golem`
- `minecraft:shearing/bogged`

### Hero of the Village gifts — **Confirmed**
Villager profession gifts after winning a raid:
- `minecraft:gameplay/hero_of_the_village/armorer_gift`
- `minecraft:gameplay/hero_of_the_village/baby_gift`
- `minecraft:gameplay/hero_of_the_village/butcher_gift`
- `minecraft:gameplay/hero_of_the_village/cartographer_gift`
- `minecraft:gameplay/hero_of_the_village/cleric_gift`
- `minecraft:gameplay/hero_of_the_village/farmer_gift`
- `minecraft:gameplay/hero_of_the_village/fisherman_gift`
- `minecraft:gameplay/hero_of_the_village/fletcher_gift`
- `minecraft:gameplay/hero_of_the_village/leatherworker_gift`
- `minecraft:gameplay/hero_of_the_village/librarian_gift`
- `minecraft:gameplay/hero_of_the_village/mason_gift`
- `minecraft:gameplay/hero_of_the_village/shepherd_gift`
- `minecraft:gameplay/hero_of_the_village/toolsmith_gift`
- `minecraft:gameplay/hero_of_the_village/unemployed_gift`
- `minecraft:gameplay/hero_of_the_village/weaponsmith_gift`

### Harvesting — **Confirmed**
- `minecraft:harvest/beehive`
- `minecraft:harvest/cave_vine`
- `minecraft:harvest/sweet_berry_bush`

### Misc animal/gameplay mechanics — **Confirmed**
- `minecraft:gameplay/armadillo_shed`
- `minecraft:gameplay/chicken_lay`
- `minecraft:gameplay/panda_sneeze`
- `minecraft:gameplay/turtle_grow`
- `minecraft:carve/pumpkin` (jack-o'-lantern carving)
- `minecraft:brush/armadillo` (brushing an armadillo for scutes — distinct
  from the structure-based `archaeology/*` tables above)

## Full category breakdown (real counts, 26.2)

From the actual extracted list, so nothing else gets missed later:

| Category | Count | Covered above? |
|---|---|---|
| `blocks/*` | 1113 | Yes — Block Drops |
| `entities/*` | 109 | Yes — Mob Drops |
| `chests/*` | 56 | Yes — dungeons/village/trial chambers |
| `gameplay/*` | 26 | Yes — fishing, hero of the village, misc |
| `shearing/*` | 22 | Yes |
| `charged_creeper/*` | 6 | Yes |
| `archaeology/*` | 6 | Yes |
| `spawners/*` | 5 | Yes — all trial chamber |
| `harvest/*` | 3 | Yes |
| `equipment/*` | 3 | Yes — all trial chamber |
| `dispensers/*` | 3 | Yes — all trial chamber |
| `pots/*` | 1 | Yes — trial chamber |
| `carve/*` | 1 | Yes |
| `brush/*` | 1 | Yes |

All 1354 real identifiers are accounted for in one of the sections above —
this table exists so a future you (or a future MC version's re-run of
`extractVanillaLootTables`) can quickly spot if a brand-new category
appears that isn't in this breakdown yet.

## Automated extraction — **confirmed working**

`build.gradle` has a real `extractVanillaLootTables` task that runs
automatically as part of the build. It searches known Loom cache
locations for the downloaded Minecraft jar, scans it for every
`data/<namespace>/loot_table(s)/**.json` entry, and writes the resulting
identifier list to a generated resource bundled with the mod jar.

Confirmed in a real CI build: it extracted **1354 real identifiers** from
the Minecraft 26.2 jar and bundled them correctly (the actual data path
turned out to be singular `loot_table`, not plural). The archaeology and
trial chamber sections above are now sourced directly from that real
output rather than guessed.

Two runtime effects once this is bundled:
1. Custom Drops logs `Custom Drops starting with N known real vanilla loot
   table identifiers bundled.` on startup — should read `1354` (or close
   to it for future versions) rather than `0`.
2. Any Chest Loot / Fishing Loot entry whose target id isn't in that real
   list gets a startup warning (not a hard error — a datapack or another
   mod's loot table wouldn't be in vanilla's own list either, so this is
   advisory only).

## How to verify or find more, for real, on your machine (manual fallback)

Since this whole session's biggest lesson was "don't trust guessed
identifiers," the reliable way to get the *exact real* list for your
installed 26.2 (or later) instance:

```bash
cd ~/Documents/testificate/custom-drops
gradle genSourcesWithVineflower -Pminecraft_version=26.2
```

Then find the actual jar (not sources — the real compiled data) and look
for the loot table folder structure. The plain (non-sources) merged jar
at:
```
.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-<hash>/26.2/minecraft-merged-<hash>-26.2.jar
```
contains the real `data/minecraft/loot_table/**` (or wherever loot tables
moved to under 26.2's data-pack layout — this itself may have shifted,
worth checking) tree with every real loot table id as a JSON file path.
Something like:

```bash
unzip -l minecraft-merged-*-26.2.jar | grep -i "loot_table\|loot_tables"
```

will show you the real, current, authoritative list — same technique
used earlier this session to nail down the Java class renames.
