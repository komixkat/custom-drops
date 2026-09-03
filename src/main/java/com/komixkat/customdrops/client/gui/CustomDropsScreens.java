package com.komixkat.customdrops.client.gui;

import com.komixkat.customdrops.ClientCustomDropsMod;
import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.config.ConfigLoader;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.config.schema.BlockDropEntry;
import com.komixkat.customdrops.config.schema.ChestLootEntry;
import com.komixkat.customdrops.config.schema.EquipmentOverrideEntry;
import com.komixkat.customdrops.config.schema.FishingLootEntry;
import com.komixkat.customdrops.config.schema.LootConditionEntry;
import com.komixkat.customdrops.config.schema.LootItemEntry;
import com.komixkat.customdrops.config.schema.MobDropEntry;
import com.komixkat.customdrops.preset.PresetLoader;
import com.komixkat.customdrops.registry.VanillaLootTableRegistry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class CustomDropsScreens {

    private static final int SLOTS = 8;
    private static final int ITEMS_PER_SLOT = 2;

    private CustomDropsScreens() {}

    private record EntrySeed(String targetId, List<LootItemEntry> pool, List<LootConditionEntry> conditions, boolean replace) {}

    private static Path saveDirFor(boolean worldMode) {
        if (worldMode) return WorldConfigLocator.activeWorldConfigDirOrNull();
        return FabricLoader.getInstance().getConfigDir().resolve(CustomDropsMod.MOD_ID);
    }

    private static Screen unavailableWorldScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("customdrops.menu.worldSettings"));
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("customdrops.menu.worldSettings"));
        category.addEntry(builder.entryBuilder().startTextDescription(
            Component.translatable("customdrops.worldSettings.unavailable")).build());
        return builder.build();
    }

    public static Screen buildInfoScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("customdrops.menu.info"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("customdrops.menu.info"));
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.info.what")).build());
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.info.howToStart")).build());
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.info.wildcards")).build());
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.info.docs")).build());
        return builder.build();
    }

    public static Screen buildSettingsScreen(Screen parent) {
        CustomDropsConfig config = ConfigLoader.load(saveDirFor(false));
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("customdrops.menu.settings"))
            .setSavingRunnable(() -> ConfigLoader.save(saveDirFor(false), config));
        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("customdrops.menu.settings"));

        category.addEntry(eb.startBooleanToggle(Component.translatable("customdrops.config.category.mobDrops"), config.mobDropsEnabled())
            .setSaveConsumer(config::setMobDropsEnabled).build());
        category.addEntry(eb.startBooleanToggle(Component.translatable("customdrops.config.category.blockDrops"), config.blockDropsEnabled())
            .setSaveConsumer(config::setBlockDropsEnabled).build());
        category.addEntry(eb.startBooleanToggle(Component.translatable("customdrops.config.category.chestLoot"), config.chestLootEnabled())
            .setSaveConsumer(config::setChestLootEnabled).build());
        category.addEntry(eb.startBooleanToggle(Component.translatable("customdrops.config.category.fishingLoot"), config.fishingLootEnabled())
            .setSaveConsumer(config::setFishingLootEnabled).build());
        category.addEntry(eb.startBooleanToggle(Component.translatable("customdrops.config.category.equipment"), config.equipmentOverridesEnabled())
            .setSaveConsumer(config::setEquipmentOverridesEnabled).build());

        List<String> options = new ArrayList<>();
        options.add("none");
        options.addAll(PresetLoader.KNOWN_PRESET_IDS);
        String current = config.activePreset() == null || config.activePreset().isBlank() ? "none" : config.activePreset();
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.config.presets.description")).build());
        category.addEntry(eb.startSelector(Component.translatable("customdrops.config.presets.active"), options.toArray(new String[0]), current)
            .setSaveConsumer(selected -> applyPresetSelection(config, selected))
            .build());

        return builder.build();
    }

    public static Screen buildPresetsScreen(Screen parent, boolean worldMode) {
        Path dir = saveDirFor(worldMode);
        if (worldMode && dir == null) return unavailableWorldScreen(parent);

        CustomDropsConfig config = ConfigLoader.load(dir);
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("customdrops.config.category.presets"))
            .setSavingRunnable(() -> ConfigLoader.save(dir, config));
        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("customdrops.config.category.presets"));

        List<String> options = new ArrayList<>();
        options.add("none");
        options.addAll(PresetLoader.KNOWN_PRESET_IDS);
        String current = config.activePreset() == null || config.activePreset().isBlank() ? "none" : config.activePreset();
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.config.presets.description")).build());
        category.addEntry(eb.startSelector(Component.translatable("customdrops.config.presets.active"), options.toArray(new String[0]), current)
            .setSaveConsumer(selected -> applyPresetSelection(config, selected))
            .build());

        return builder.build();
    }

    private static void applyPresetSelection(CustomDropsConfig config, String selected) {
        config.clearAll();
        if (!"none".equals(selected)) {
            config.setActivePreset(selected);
            PresetLoader.applyActivePreset(config);
        } else {
            config.setActivePreset("");
        }
    }

    public static Screen buildMobDropsScreen(Screen parent, boolean worldMode) {
        Path dir = saveDirFor(worldMode);
        if (worldMode && dir == null) return unavailableWorldScreen(parent);
        CustomDropsConfig config = ConfigLoader.load(dir);

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("customdrops.config.category.mobDrops"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("customdrops.config.category.mobDrops"));
        List<Runnable> finalizers = new ArrayList<>();
        builder.setSavingRunnable(() -> { finalizers.forEach(Runnable::run); ConfigLoader.save(dir, config); });

        category.addEntry(eb.startBooleanToggle(Component.translatable("customdrops.config.enabled"), config.mobDropsEnabled())
            .setSaveConsumer(config::setMobDropsEnabled).build());
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.config.mobDrops.description")).build());

        List<EntrySeed> existing = new ArrayList<>();
        for (MobDropEntry e : config.mobDrops()) existing.add(new EntrySeed(e.targetId(), e.pool(), e.conditions(), e.replaceVanillaTable()));

        buildSlots(category, eb, existing, "customdrops.config.target.entity", seeds -> {
            config.mobDrops().clear();
            for (EntrySeed seed : seeds) {
                config.mobDrops().add(new MobDropEntry(seed.targetId(), seed.targetId().startsWith("#"), seed.replace(), seed.pool(), seed.conditions()));
            }
        }, finalizers);

        return builder.build();
    }

    public static Screen buildBlockDropsScreen(Screen parent, boolean worldMode) {
        Path dir = saveDirFor(worldMode);
        if (worldMode && dir == null) return unavailableWorldScreen(parent);
        CustomDropsConfig config = ConfigLoader.load(dir);

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("customdrops.config.category.blockDrops"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("customdrops.config.category.blockDrops"));
        List<Runnable> finalizers = new ArrayList<>();
        builder.setSavingRunnable(() -> { finalizers.forEach(Runnable::run); ConfigLoader.save(dir, config); });

        category.addEntry(eb.startBooleanToggle(Component.translatable("customdrops.config.enabled"), config.blockDropsEnabled())
            .setSaveConsumer(config::setBlockDropsEnabled).build());
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.config.blockDrops.description")).build());

        List<EntrySeed> existing = new ArrayList<>();
        for (BlockDropEntry e : config.blockDrops()) existing.add(new EntrySeed(e.targetId(), e.pool(), e.conditions(), e.replaceVanillaTable()));

        buildSlots(category, eb, existing, "customdrops.config.target.block", seeds -> {
            config.blockDrops().clear();
            for (EntrySeed seed : seeds) {
                config.blockDrops().add(new BlockDropEntry(seed.targetId(), seed.targetId().startsWith("#"), seed.replace(), seed.pool(), seed.conditions()));
            }
        }, finalizers);

        return builder.build();
    }

    public static Screen buildChestLootScreen(Screen parent, boolean worldMode) {
        Path dir = saveDirFor(worldMode);
        if (worldMode && dir == null) return unavailableWorldScreen(parent);
        CustomDropsConfig config = ConfigLoader.load(dir);

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("customdrops.config.category.chestLoot"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("customdrops.config.category.chestLoot"));
        List<Runnable> finalizers = new ArrayList<>();
        builder.setSavingRunnable(() -> { finalizers.forEach(Runnable::run); ConfigLoader.save(dir, config); });

        category.addEntry(eb.startBooleanToggle(Component.translatable("customdrops.config.enabled"), config.chestLootEnabled())
            .setSaveConsumer(config::setChestLootEnabled).build());
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.config.chestLoot.description")).build());
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.config.wildcard.hint")).build());

        List<EntrySeed> existing = new ArrayList<>();
        for (ChestLootEntry e : config.chestLoot()) existing.add(new EntrySeed(e.targetLootTableId(), e.pool(), e.conditions(), e.replaceVanillaTable()));

        buildSlots(category, eb, existing, "customdrops.config.target.lootTable", seeds -> {
            config.chestLoot().clear();
            for (EntrySeed seed : seeds) {
                config.chestLoot().add(new ChestLootEntry(seed.targetId(), seed.replace(), seed.pool(), seed.conditions()));
            }
        }, finalizers);

        return builder.build();
    }

    public static Screen buildFishingLootScreen(Screen parent, boolean worldMode) {
        Path dir = saveDirFor(worldMode);
        if (worldMode && dir == null) return unavailableWorldScreen(parent);
        CustomDropsConfig config = ConfigLoader.load(dir);

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("customdrops.config.category.fishingLoot"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("customdrops.config.category.fishingLoot"));
        List<Runnable> finalizers = new ArrayList<>();
        builder.setSavingRunnable(() -> { finalizers.forEach(Runnable::run); ConfigLoader.save(dir, config); });

        category.addEntry(eb.startBooleanToggle(Component.translatable("customdrops.config.enabled"), config.fishingLootEnabled())
            .setSaveConsumer(config::setFishingLootEnabled).build());
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.config.fishingLoot.description")).build());
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.config.wildcard.hint")).build());

        List<EntrySeed> existing = new ArrayList<>();
        for (FishingLootEntry e : config.fishingLoot()) existing.add(new EntrySeed(e.targetLootTableId(), e.pool(), e.conditions(), e.replaceVanillaTable()));

        buildSlots(category, eb, existing, "customdrops.config.target.lootTable", seeds -> {
            config.fishingLoot().clear();
            for (EntrySeed seed : seeds) {
                config.fishingLoot().add(new FishingLootEntry(seed.targetId(), seed.replace(), seed.pool(), seed.conditions()));
            }
        }, finalizers);

        return builder.build();
    }

    private static void buildSlots(ConfigCategory category, ConfigEntryBuilder eb, List<EntrySeed> existing,
                                    String targetLabelKey, Consumer<List<EntrySeed>> onFinalize, List<Runnable> finalizers) {
        if (existing.size() > SLOTS) {
            category.addEntry(eb.startTextDescription(
                Component.translatable("customdrops.config.overflow", existing.size(), SLOTS)).build());
        }

        String[] targetId = new String[SLOTS];
        boolean[] replace = new boolean[SLOTS];
        String[][] itemId = new String[SLOTS][ITEMS_PER_SLOT];
        int[][] weight = new int[SLOTS][ITEMS_PER_SLOT];
        int[][] minCount = new int[SLOTS][ITEMS_PER_SLOT];
        int[][] maxCount = new int[SLOTS][ITEMS_PER_SLOT];
        float[][] chance = new float[SLOTS][ITEMS_PER_SLOT];
        boolean[] killedByPlayer = new boolean[SLOTS];
        boolean[] silkTouch = new boolean[SLOTS];
        boolean[] noSilkTouch = new boolean[SLOTS];
        boolean[] useRandomChance = new boolean[SLOTS];
        float[] randomChanceValue = new float[SLOTS];

        for (int s = 0; s < SLOTS; s++) {
            EntrySeed seed = s < existing.size() ? existing.get(s) : null;
            targetId[s] = seed != null ? seed.targetId() : "";
            replace[s] = seed != null && seed.replace();
            randomChanceValue[s] = 1.0f;

            for (int it = 0; it < ITEMS_PER_SLOT; it++) {
                LootItemEntry seedItem = seed != null && seed.pool().size() > it ? seed.pool().get(it) : null;
                itemId[s][it] = seedItem != null ? seedItem.itemId() : "";
                weight[s][it] = seedItem != null ? seedItem.weight() : 1;
                minCount[s][it] = seedItem != null ? seedItem.minCount() : 1;
                maxCount[s][it] = seedItem != null ? seedItem.maxCount() : 1;
                chance[s][it] = seedItem != null ? seedItem.chance() : 1.0f;
            }

            if (seed != null) {
                for (LootConditionEntry c : seed.conditions()) {
                    switch (c.type()) {
                        case KILLED_BY_PLAYER -> killedByPlayer[s] = true;
                        case SILK_TOUCH -> silkTouch[s] = true;
                        case NO_SILK_TOUCH -> noSilkTouch[s] = true;
                        case RANDOM_CHANCE -> {
                            useRandomChance[s] = true;
                            try {
                                randomChanceValue[s] = Float.parseFloat(c.params().getOrDefault("chance", "1.0"));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        default -> {}
                    }
                }
            }

            List<AbstractConfigListEntry> slotEntries = new ArrayList<>();
            int idx = s;

            slotEntries.add(eb.startTextField(Component.translatable(targetLabelKey), targetId[s])
                .setSaveConsumer(value -> targetId[idx] = value).build());
            slotEntries.add(eb.startBooleanToggle(Component.translatable("customdrops.config.replace"), replace[s])
                .setSaveConsumer(value -> replace[idx] = value).build());

            for (int it = 0; it < ITEMS_PER_SLOT; it++) {
                int itemIdx = it;
                slotEntries.add(eb.startTextField(Component.translatable("customdrops.config.item.id", it + 1), itemId[s][it])
                    .setSaveConsumer(value -> itemId[idx][itemIdx] = value).build());
                slotEntries.add(eb.startIntSlider(Component.translatable("customdrops.config.item.weight", it + 1), weight[s][it], 1, 100)
                    .setSaveConsumer(value -> weight[idx][itemIdx] = value).build());
                slotEntries.add(eb.startIntSlider(Component.translatable("customdrops.config.item.minCount", it + 1), minCount[s][it], 1, 64)
                    .setSaveConsumer(value -> minCount[idx][itemIdx] = value).build());
                slotEntries.add(eb.startIntSlider(Component.translatable("customdrops.config.item.maxCount", it + 1), maxCount[s][it], 1, 64)
                    .setSaveConsumer(value -> maxCount[idx][itemIdx] = value).build());
                slotEntries.add(eb.startFloatField(Component.translatable("customdrops.config.item.chance", it + 1), chance[s][it])
                    .setMin(0f).setMax(1f)
                    .setSaveConsumer(value -> chance[idx][itemIdx] = value).build());
            }

            slotEntries.add(eb.startBooleanToggle(Component.translatable("customdrops.config.condition.killedByPlayer"), killedByPlayer[s])
                .setSaveConsumer(value -> killedByPlayer[idx] = value).build());
            slotEntries.add(eb.startBooleanToggle(Component.translatable("customdrops.config.condition.silkTouch"), silkTouch[s])
                .setSaveConsumer(value -> silkTouch[idx] = value).build());
            slotEntries.add(eb.startBooleanToggle(Component.translatable("customdrops.config.condition.noSilkTouch"), noSilkTouch[s])
                .setSaveConsumer(value -> noSilkTouch[idx] = value).build());
            slotEntries.add(eb.startBooleanToggle(Component.translatable("customdrops.config.condition.useRandomChance"), useRandomChance[s])
                .setSaveConsumer(value -> useRandomChance[idx] = value).build());
            slotEntries.add(eb.startFloatField(Component.translatable("customdrops.config.condition.randomChanceValue"), randomChanceValue[s])
                .setMin(0f).setMax(1f)
                .setSaveConsumer(value -> randomChanceValue[idx] = value).build());

            String slotTitle = targetId[s] != null && !targetId[s].isBlank()
                ? targetId[s]
                : "Entry " + (s + 1) + " (empty)";
            category.addEntry(eb.startSubCategory(Component.literal(slotTitle), slotEntries).build());
        }

        finalizers.add(() -> {
            List<EntrySeed> result = new ArrayList<>();
            for (int s = 0; s < SLOTS; s++) {
                if (targetId[s] == null || targetId[s].isBlank()) continue;
                List<LootItemEntry> pool = new ArrayList<>();
                for (int it = 0; it < ITEMS_PER_SLOT; it++) {
                    if (itemId[s][it] == null || itemId[s][it].isBlank()) continue;
                    int min = Math.max(0, minCount[s][it]);
                    int max = Math.max(min, maxCount[s][it]);
                    pool.add(new LootItemEntry(itemId[s][it], Math.max(1, weight[s][it]), min, max, chance[s][it]));
                }
                if (pool.isEmpty()) continue;

                List<LootConditionEntry> conditions = new ArrayList<>();
                if (killedByPlayer[s]) conditions.add(new LootConditionEntry(LootConditionEntry.ConditionType.KILLED_BY_PLAYER, Map.of()));
                if (silkTouch[s]) conditions.add(new LootConditionEntry(LootConditionEntry.ConditionType.SILK_TOUCH, Map.of()));
                if (noSilkTouch[s]) conditions.add(new LootConditionEntry(LootConditionEntry.ConditionType.NO_SILK_TOUCH, Map.of()));
                if (useRandomChance[s]) conditions.add(new LootConditionEntry(LootConditionEntry.ConditionType.RANDOM_CHANCE, Map.of("chance", String.valueOf(randomChanceValue[s]))));

                result.add(new EntrySeed(targetId[s], pool, conditions, replace[s]));
            }
            for (int s = SLOTS; s < existing.size(); s++) {
                result.add(existing.get(s));
            }
            onFinalize.accept(result);
        });
    }

    public static Screen buildEquipmentScreen(Screen parent, boolean worldMode) {
        Path dir = saveDirFor(worldMode);
        if (worldMode && dir == null) return unavailableWorldScreen(parent);
        CustomDropsConfig config = ConfigLoader.load(dir);

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("customdrops.config.category.equipment"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("customdrops.config.category.equipment"));

        category.addEntry(eb.startBooleanToggle(Component.translatable("customdrops.config.enabled"), config.equipmentOverridesEnabled())
            .setSaveConsumer(config::setEquipmentOverridesEnabled).build());
        category.addEntry(eb.startTextDescription(Component.translatable("customdrops.config.equipment.description")).build());

        List<EquipmentOverrideEntry> existing = new ArrayList<>(config.equipmentOverrides());
        if (existing.size() > SLOTS) {
            category.addEntry(eb.startTextDescription(
                Component.translatable("customdrops.config.overflow", existing.size(), SLOTS)).build());
        }

        String[] slotNames = new String[EquipmentOverrideEntry.EquipmentSlotGroup.values().length];
        for (int i = 0; i < slotNames.length; i++) {
            slotNames[i] = EquipmentOverrideEntry.EquipmentSlotGroup.values()[i].name();
        }

        String[] entityId = new String[SLOTS];
        String[] slotChoice = new String[SLOTS];

        for (int s = 0; s < SLOTS; s++) {
            EquipmentOverrideEntry seed = s < existing.size() ? existing.get(s) : null;
            entityId[s] = seed != null ? seed.targetEntityId() : "";
            slotChoice[s] = seed != null ? seed.slot().name() : slotNames[0];

            List<AbstractConfigListEntry> slotEntries = new ArrayList<>();
            int idx = s;

            slotEntries.add(eb.startTextField(Component.translatable("customdrops.config.target.entity"), entityId[s])
                .setSaveConsumer(value -> entityId[idx] = value).build());
            slotEntries.add(eb.startSelector(Component.translatable("customdrops.config.equipment.slot"), slotNames, slotChoice[s])
                .setSaveConsumer(value -> slotChoice[idx] = value).build());

            String slotTitle = entityId[s] != null && !entityId[s].isBlank()
                ? entityId[s] + " (" + slotChoice[s] + ")"
                : "Entry " + (s + 1) + " (empty)";
            category.addEntry(eb.startSubCategory(Component.literal(slotTitle), slotEntries).build());
        }

        builder.setSavingRunnable(() -> {
            List<EquipmentOverrideEntry> result = new ArrayList<>();
            for (int s = 0; s < SLOTS; s++) {
                if (entityId[s] == null || entityId[s].isBlank()) continue;
                EquipmentOverrideEntry.EquipmentSlotGroup slot;
                try {
                    slot = EquipmentOverrideEntry.EquipmentSlotGroup.valueOf(slotChoice[s]);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                result.add(new EquipmentOverrideEntry(entityId[s], entityId[s].startsWith("#"), slot, EquipmentOverrideEntry.ALWAYS_DROP));
            }
            for (int s = SLOTS; s < existing.size(); s++) {
                result.add(existing.get(s));
            }
            config.equipmentOverrides().clear();
            config.equipmentOverrides().addAll(result);
            ConfigLoader.save(dir, config);
        });

        return builder.build();
    }

    public static Screen buildBrowseCategory(Screen parent) {
        return buildSearchScreen(parent);
    }

    public static Screen buildSearchScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("customdrops.menu.search"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory browse = builder.getOrCreateCategory(Component.translatable("customdrops.menu.search"));

        List<String> knownIds = VanillaLootTableRegistry.search("");
        if (knownIds.isEmpty()) {
            browse.addEntry(eb.startTextDescription(Component.translatable("customdrops.config.browse.empty")).build());
            return builder.build();
        }

        browse.addEntry(eb.startTextDescription(Component.translatable("customdrops.config.browse.hint", knownIds.size())).build());
        for (String id : knownIds) {
            browse.addEntry(eb.startTextDescription(Component.literal(id)).build());
        }

        return builder.build();
    }

    public static Screen buildServerConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("customdrops.menu.serverConfig"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory serverInfo = builder.getOrCreateCategory(Component.translatable("customdrops.menu.serverConfig"));

        if (ClientCustomDropsMod.isConnectedToCustomDropsServer()) {
            String summary = ClientCustomDropsMod.lastReceivedSummary();
            for (String line : summary.split("\n")) {
                if (line.isBlank()) continue;
                serverInfo.addEntry(eb.startTextDescription(Component.literal(line)).build());
            }
        } else {
            serverInfo.addEntry(eb.startTextDescription(
                Component.translatable("customdrops.config.serverInfo.notConnected")).build());
        }

        return builder.build();
    }
}
