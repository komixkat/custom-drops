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
import net.minecraft.SharedConstants;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PresetLoader {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String LEGACY_PRESET_ROOT = "/data/customdrops/presets/";
    private static final String ASSET_PRESET_ROOT = "/assets/customdrops/presets/";

    public static final List<String> KNOWN_PRESET_IDS = List.of(
        "trophy_hunter", "lootery_plus", "explorers_fortune",
        "civilized_survival", "ocean_depths", "op_x");

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
        if (preset.targetMinecraftVersion != null && !preset.targetMinecraftVersion.isBlank()) {
            try {
                PresetVersionValidator.validate(preset.presetId != null ? preset.presetId : activePreset,
                    preset.targetMinecraftVersion, runningVersion);
            } catch (PresetVersionValidator.PresetVersionMismatchException e) {
                CustomDropsMod.LOGGER.error(e.getMessage());
                return;
            }
        }

        config.mobDrops().addAll(preset.mobDrops);
        config.blockDrops().addAll(preset.blockDrops);
        config.chestLoot().addAll(preset.chestLoot);
        config.fishingLoot().addAll(preset.fishingLoot);
        config.equipmentOverrides().addAll(preset.equipmentOverrides);
    }

    public static boolean fill(String presetId, CustomDropsConfig target) {
        if (presetId == null || presetId.isBlank() || target == null) return false;
        Optional<PresetFile> presetFile = read(presetId);
        if (presetFile.isEmpty()) return false;
        PresetFile preset = presetFile.get();
        target.mobDrops().addAll(preset.mobDrops);
        target.blockDrops().addAll(preset.blockDrops);
        target.chestLoot().addAll(preset.chestLoot);
        target.fishingLoot().addAll(preset.fishingLoot);
        target.equipmentOverrides().addAll(preset.equipmentOverrides);
        return true;
    }

    private static Optional<PresetFile> read(String presetId) {
        Optional<PresetFile> fromAssets = readResource(ASSET_PRESET_ROOT, presetId);
        if (fromAssets.isPresent()) return fromAssets;
        return readResource(LEGACY_PRESET_ROOT, presetId);
    }

    public record PresetSummary(
        String presetId,
        String displayName,
        String description,
        int mobDrops,
        int blockDrops,
        int chestLoot,
        int fishingLoot,
        int equipmentOverrides
    ) {
        public int total() {
            return mobDrops + blockDrops + chestLoot + fishingLoot + equipmentOverrides;
        }
    }

    public static Optional<PresetSummary> summarize(String presetId) {
        return read(presetId).map(p -> new PresetSummary(
            p.presetId != null ? p.presetId : presetId,
            p.displayName != null ? p.displayName : presetId.replace('_', ' '),
            p.description != null ? p.description : "",
            safeSize(p.mobDrops),
            safeSize(p.blockDrops),
            safeSize(p.chestLoot),
            safeSize(p.fishingLoot),
            safeSize(p.equipmentOverrides)));
    }

    private static int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static Optional<PresetFile> readResource(String root, String presetId) {
        String resourcePath = root + presetId + ".json";
        try (var stream = PresetLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) return Optional.empty();
            PresetFile preset = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PresetFile.class);
            return Optional.ofNullable(preset);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to read preset {} from {}", presetId, root, e);
            return Optional.empty();
        }
    }

    private static final class PresetFile {
        String presetId;
        String displayName;
        String description;
        String targetMinecraftVersion;
        List<MobDropEntry> mobDrops = List.of();
        List<BlockDropEntry> blockDrops = List.of();
        List<ChestLootEntry> chestLoot = List.of();
        List<FishingLootEntry> fishingLoot = List.of();
        List<EquipmentOverrideEntry> equipmentOverrides = List.of();
    }
}
