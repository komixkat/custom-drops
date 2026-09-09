package com.komixkat.customdrops.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.config.schema.BlockDropEntry;
import com.komixkat.customdrops.config.schema.ChestLootEntry;
import com.komixkat.customdrops.config.schema.EquipmentOverrideEntry;
import com.komixkat.customdrops.config.schema.FishingLootEntry;
import com.komixkat.customdrops.config.schema.MobDropEntry;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigLoader() {}

    public static CustomDropsConfig load(Path configDir) {
        CustomDropsConfig config = new CustomDropsConfig();
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Could not create config directory {}", configDir, e);
            return config;
        }

        applyMeta(config, readMeta(configDir));
        config.mobDrops().addAll(readList(configDir.resolve("mob_drops.json"), new TypeToken<List<MobDropEntry>>() {}.getType()));
        config.blockDrops().addAll(readList(configDir.resolve("block_drops.json"), new TypeToken<List<BlockDropEntry>>() {}.getType()));
        config.chestLoot().addAll(readList(configDir.resolve("chest_loot.json"), new TypeToken<List<ChestLootEntry>>() {}.getType()));
        config.fishingLoot().addAll(readList(configDir.resolve("fishing_loot.json"), new TypeToken<List<FishingLootEntry>>() {}.getType()));
        config.equipmentOverrides().addAll(readList(configDir.resolve("equipment_overrides.json"), new TypeToken<List<EquipmentOverrideEntry>>() {}.getType()));
        return config;
    }

    /**
     * Loads the global default config, then overrides it category-by-category with
     * whatever's present in worldConfigDir. A category is only overridden if its file
     * actually exists in the world-specific directory; missing categories fall back
     * to the global default. This lets a world opt into its own drop rules without
     * needing to duplicate every category if it only wants to change one.
     */
    public static CustomDropsConfig loadWithWorldOverride(Path globalConfigDir, Path worldConfigDir) {
        CustomDropsConfig config = load(globalConfigDir);

        try {
            Files.createDirectories(worldConfigDir);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Could not create per-world config directory {}, using global config only", worldConfigDir, e);
            return config;
        }

        Path worldMeta = worldConfigDir.resolve("meta.json");
        if (Files.exists(worldMeta)) {
            applyMeta(config, readMeta(worldConfigDir));
        }

        overrideIfPresent(worldConfigDir.resolve("mob_drops.json"), new TypeToken<List<MobDropEntry>>() {}.getType(),
            (List<MobDropEntry> list) -> { config.mobDrops().clear(); config.mobDrops().addAll(list); });
        overrideIfPresent(worldConfigDir.resolve("block_drops.json"), new TypeToken<List<BlockDropEntry>>() {}.getType(),
            (List<BlockDropEntry> list) -> { config.blockDrops().clear(); config.blockDrops().addAll(list); });
        overrideIfPresent(worldConfigDir.resolve("chest_loot.json"), new TypeToken<List<ChestLootEntry>>() {}.getType(),
            (List<ChestLootEntry> list) -> { config.chestLoot().clear(); config.chestLoot().addAll(list); });
        overrideIfPresent(worldConfigDir.resolve("fishing_loot.json"), new TypeToken<List<FishingLootEntry>>() {}.getType(),
            (List<FishingLootEntry> list) -> { config.fishingLoot().clear(); config.fishingLoot().addAll(list); });
        overrideIfPresent(worldConfigDir.resolve("equipment_overrides.json"), new TypeToken<List<EquipmentOverrideEntry>>() {}.getType(),
            (List<EquipmentOverrideEntry> list) -> { config.equipmentOverrides().clear(); config.equipmentOverrides().addAll(list); });

        return config;
    }

    private static <T> void overrideIfPresent(Path path, Type type, java.util.function.Consumer<List<T>> applier) {
        if (!Files.exists(path)) return;
        List<T> list = readList(path, type);
        applier.accept(list);
    }

    public static void save(Path configDir, CustomDropsConfig config) {
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Could not create config directory {}", configDir, e);
            return;
        }
        writeMeta(configDir, config);
        writeList(configDir.resolve("mob_drops.json"), config.mobDrops());
        writeList(configDir.resolve("block_drops.json"), config.blockDrops());
        writeList(configDir.resolve("chest_loot.json"), config.chestLoot());
        writeList(configDir.resolve("fishing_loot.json"), config.fishingLoot());
        writeList(configDir.resolve("equipment_overrides.json"), config.equipmentOverrides());
    }

    private static <T> List<T> readList(Path path, Type type) {
        if (!Files.exists(path)) return List.of();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<T> result = GSON.fromJson(reader, type);
            return result != null ? result : List.of();
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to read {}, falling back to empty list", path, e);
            return List.of();
        }
    }

    private static <T> void writeList(Path path, List<T> value) {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(value, writer);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to write {}", path, e);
        }
    }

    private static MetaFile readMeta(Path configDir) {
        Path meta = configDir.resolve("meta.json");
        if (!Files.exists(meta)) return new MetaFile();
        try (Reader reader = Files.newBufferedReader(meta, StandardCharsets.UTF_8)) {
            MetaFile metaFile = GSON.fromJson(reader, MetaFile.class);
            return metaFile != null ? metaFile : new MetaFile();
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to read {}", meta, e);
            return new MetaFile();
        }
    }

    // Boolean fields use the nullable wrapper type deliberately: Gson's reflection-based
    // instantiation bypasses field initializers, so a missing JSON field would otherwise
    // deserialize to false rather than our intended enabled-by-default of true. null means
    // "not present in this file" and is treated as true here instead.
    private static void applyMeta(CustomDropsConfig config, MetaFile metaFile) {
        config.setActivePreset(metaFile.activePreset != null ? metaFile.activePreset : "");
        config.setMobDropsEnabled(metaFile.mobDropsEnabled == null || metaFile.mobDropsEnabled);
        config.setBlockDropsEnabled(metaFile.blockDropsEnabled == null || metaFile.blockDropsEnabled);
        config.setChestLootEnabled(metaFile.chestLootEnabled == null || metaFile.chestLootEnabled);
        config.setFishingLootEnabled(metaFile.fishingLootEnabled == null || metaFile.fishingLootEnabled);
        config.setEquipmentOverridesEnabled(metaFile.equipmentOverridesEnabled == null || metaFile.equipmentOverridesEnabled);
    }

    private static void writeMeta(Path configDir, CustomDropsConfig config) {
        Path meta = configDir.resolve("meta.json");
        try (Writer writer = Files.newBufferedWriter(meta, StandardCharsets.UTF_8)) {
            MetaFile metaFile = new MetaFile();
            metaFile.activePreset = config.activePreset();
            metaFile.mobDropsEnabled = config.mobDropsEnabled();
            metaFile.blockDropsEnabled = config.blockDropsEnabled();
            metaFile.chestLootEnabled = config.chestLootEnabled();
            metaFile.fishingLootEnabled = config.fishingLootEnabled();
            metaFile.equipmentOverridesEnabled = config.equipmentOverridesEnabled();
            GSON.toJson(metaFile, writer);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to write {}", meta, e);
        }
    }

    private static final class MetaFile {
        String activePreset;
        Boolean mobDropsEnabled;
        Boolean blockDropsEnabled;
        Boolean chestLootEnabled;
        Boolean fishingLootEnabled;
        Boolean equipmentOverridesEnabled;
    }
}
