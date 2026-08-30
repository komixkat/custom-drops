package com.komixkat.customdrops.preset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.config.schema.BlockDropEntry;
import com.komixkat.customdrops.config.schema.ChestLootEntry;
import com.komixkat.customdrops.config.schema.EquipmentOverrideEntry;
import com.komixkat.customdrops.config.schema.FishingLootEntry;
import com.komixkat.customdrops.config.schema.MobDropEntry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public final class PresetLoader {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String PRESET_RESOURCE_ROOT = "/data/customdrops/presets/";

    public static final List<String> KNOWN_PRESET_IDS = List.of(
        "cozy_survival", "better_dungeon_loot", "guaranteed_trophies");

    private PresetLoader() {}

    public static void applyActivePreset(CustomDropsConfig config) {
        String activePreset = config.activePreset();
        if (activePreset == null || activePreset.isBlank()) {
            return;
        }

        Optional<PresetFile> presetFile = read(activePreset);
        if (presetFile.isEmpty()) {
            CustomDropsMod.LOGGER.error("Preset '{}' not found, leaving config untouched.", activePreset);
            return;
        }

        PresetFile preset = presetFile.get();
        String runningVersion = SharedConstants.getCurrentVersion().name();
        try {
            PresetVersionValidator.validate(preset.presetId, preset.targetMinecraftVersion, runningVersion);
        } catch (PresetVersionValidator.PresetVersionMismatchException e) {
            CustomDropsMod.LOGGER.error(e.getMessage());
            return;
        }

        config.mobDrops().addAll(preset.mobDrops);
        config.blockDrops().addAll(preset.blockDrops);
        config.chestLoot().addAll(preset.chestLoot);
        config.fishingLoot().addAll(preset.fishingLoot);
        config.equipmentOverrides().addAll(preset.equipmentOverrides);
    }

    private static Optional<PresetFile> read(String presetId) {
        String resourcePath = PRESET_RESOURCE_ROOT + presetId + ".json";
        try (var stream = PresetLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) return Optional.empty();
            PresetFile preset = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PresetFile.class);
            return Optional.ofNullable(preset);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to read preset {}", presetId, e);
            return Optional.empty();
        }
    }

    private static final class PresetFile {
        String presetId;
        String targetMinecraftVersion;
        List<MobDropEntry> mobDrops = List.of();
        List<BlockDropEntry> blockDrops = List.of();
        List<ChestLootEntry> chestLoot = List.of();
        List<FishingLootEntry> fishingLoot = List.of();
        List<EquipmentOverrideEntry> equipmentOverrides = List.of();
    }
}
