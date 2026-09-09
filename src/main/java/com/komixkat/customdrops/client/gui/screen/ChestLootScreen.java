package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.client.gui.widget.EntryFormWidget;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.config.schema.ChestLootEntry;
import com.komixkat.customdrops.config.schema.LootConditionEntry;
import com.komixkat.customdrops.config.schema.LootItemEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

public final class ChestLootScreen extends LootRuleScreen<ChestLootEntry> {

    public ChestLootScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.config.category.chestLoot"));
    }

    public ChestLootScreen(net.minecraft.client.gui.screens.Screen parent,
                           Supplier<CustomDropsConfig> config, Mode mode) {
        super(parent, Component.translatable("customdrops.config.category.chestLoot"), config, mode);
    }

    @Override
    protected String categoryTitle() {
        return "Chest Loot Rules";
    }

    @Override
    protected String singularLabel() {
        return "Chest Loot Rule";
    }

    @Override
    protected EntryFormWidget.Host<ChestLootEntry> createHost() {
        return new EntryFormWidget.Host<>() {
            @Override
            public List<ChestLootEntry> list() {
                return config.get().chestLoot();
            }

            @Override
            public ChestLootEntry build(String targetId, boolean isTag, boolean replace, List<LootItemEntry> items) {
                return new ChestLootEntry(targetId, replace, items);
            }

            @Override
            public String targetOf(ChestLootEntry entry) {
                return entry.targetLootTableId();
            }

            @Override
            public boolean isTagOf(ChestLootEntry entry) {
                return false;
            }

            @Override
            public boolean replaceOf(ChestLootEntry entry) {
                return entry.replaceVanillaTable();
            }

            @Override
            public String entryLabel(ChestLootEntry entry) {
                return entry.targetLootTableId();
            }

            @Override
            public List<LootItemEntry> itemsOf(ChestLootEntry entry) {
                return entry.items();
            }

@Override
    public String defaultTargetId() {
        return "minecraft:chests/simple_dungeon";
    }

    @Override
    public String defaultItemId() {
        return "minecraft:bread";
    }

            @Override
            public String targetFieldLabel() {
                return "Loot table id (can end with *)";
            }

            @Override
            public boolean supportsTags() {
                return false;
            }

            @Override
            public java.util.List<LootConditionEntry.ConditionType> allowedConditions() {
                return java.util.List.of();
            }

            @Override
            public String lookupKind() {
                return "loot";
            }

            @Override
            public boolean supportsVanillaLink() {
                return true;
            }

            @Override
            public String vanillaLookupTarget(String targetId) {
                if (targetId == null || targetId.isBlank()) return null;
                String clean = targetId.endsWith("*") ? targetId.substring(0, targetId.length() - 1) : targetId;
                return clean.indexOf(':') >= 0 ? clean : "minecraft:" + clean;
            }
        };
    }

    @Override
    protected int addEntry(String targetId) {
        List<ChestLootEntry> list = config.get().chestLoot();
        list.add(new ChestLootEntry(stripTag(targetId), false, List.of()));
        return list.size() - 1;
    }

    @Override
    protected boolean supportsExampleRules() {
        return true;
    }

    @Override
    protected void insertExamples() {
        List<ChestLootEntry> list = config.get().chestLoot();
        list.add(new ChestLootEntry("minecraft:chests/simple_dungeon", false, List.of(
            new LootItemEntry("minecraft:bread", 10, 1, 1, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:string", 5, 1, 2, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:iron_ingot", 8, 1, 2, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:gold_nugget", 6, 1, 2, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:saddle", 1, 1, 1, 0.25f, List.of(), List.of()),
            new LootItemEntry("minecraft:diamond", 1, 1, 1, 0.1f, List.of(), List.of()),
            new LootItemEntry("minecraft:cake", 1, 1, 1, 0.1f, List.of(), List.of()))));
        list.add(new ChestLootEntry("minecraft:chests/abandoned_mineshaft", false, List.of(
            new LootItemEntry("minecraft:torch", 30, 4, 10, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:rail", 10, 4, 8, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:golden_apple", 1, 1, 1, 0.05f, List.of(), List.of()))));
    }
}