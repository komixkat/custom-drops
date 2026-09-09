package com.komixkat.customdrops.config;

import com.komixkat.customdrops.config.schema.BlockDropEntry;
import com.komixkat.customdrops.config.schema.ChestLootEntry;
import com.komixkat.customdrops.config.schema.EquipmentOverrideEntry;
import com.komixkat.customdrops.config.schema.FishingLootEntry;
import com.komixkat.customdrops.config.schema.MobDropEntry;

import java.util.ArrayList;
import java.util.List;

public final class CustomDropsConfig {

    private String activePreset = "";
    private boolean mobDropsEnabled = true;
    private boolean blockDropsEnabled = true;
    private boolean chestLootEnabled = true;
    private boolean fishingLootEnabled = true;
    private boolean equipmentOverridesEnabled = true;

    private final List<MobDropEntry> mobDrops = new ArrayList<>();
    private final List<BlockDropEntry> blockDrops = new ArrayList<>();
    private final List<ChestLootEntry> chestLoot = new ArrayList<>();
    private final List<FishingLootEntry> fishingLoot = new ArrayList<>();
    private final List<EquipmentOverrideEntry> equipmentOverrides = new ArrayList<>();

    public String activePreset() {
        return activePreset;
    }

    public void setActivePreset(String presetId) {
        this.activePreset = presetId;
    }

    public boolean mobDropsEnabled() {
        return mobDropsEnabled;
    }

    public void setMobDropsEnabled(boolean enabled) {
        this.mobDropsEnabled = enabled;
    }

    public boolean blockDropsEnabled() {
        return blockDropsEnabled;
    }

    public void setBlockDropsEnabled(boolean enabled) {
        this.blockDropsEnabled = enabled;
    }

    public boolean chestLootEnabled() {
        return chestLootEnabled;
    }

    public void setChestLootEnabled(boolean enabled) {
        this.chestLootEnabled = enabled;
    }

    public boolean fishingLootEnabled() {
        return fishingLootEnabled;
    }

    public void setFishingLootEnabled(boolean enabled) {
        this.fishingLootEnabled = enabled;
    }

    public boolean equipmentOverridesEnabled() {
        return equipmentOverridesEnabled;
    }

    public void setEquipmentOverridesEnabled(boolean enabled) {
        this.equipmentOverridesEnabled = enabled;
    }

    public List<MobDropEntry> mobDrops() {
        return mobDrops;
    }

    public List<BlockDropEntry> blockDrops() {
        return blockDrops;
    }

    public List<ChestLootEntry> chestLoot() {
        return chestLoot;
    }

    public List<FishingLootEntry> fishingLoot() {
        return fishingLoot;
    }

    public List<EquipmentOverrideEntry> equipmentOverrides() {
        return equipmentOverrides;
    }

    public void clearAll() {
        mobDrops.clear();
        blockDrops.clear();
        chestLoot.clear();
        fishingLoot.clear();
        equipmentOverrides.clear();
    }
}
