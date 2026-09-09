package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.client.gui.widget.EntryFormWidget;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.config.schema.FishingLootEntry;
import com.komixkat.customdrops.config.schema.LootConditionEntry;
import com.komixkat.customdrops.config.schema.LootItemEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

public final class FishingLootScreen extends LootRuleScreen<FishingLootEntry> {

    public FishingLootScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.config.category.fishingLoot"));
    }

    public FishingLootScreen(net.minecraft.client.gui.screens.Screen parent,
                             Supplier<CustomDropsConfig> config, Mode mode) {
        super(parent, Component.translatable("customdrops.config.category.fishingLoot"), config, mode);
    }

    @Override
    protected String categoryTitle() {
        return "Fishing Loot Rules";
    }

    @Override
    protected String singularLabel() {
        return "Fishing Loot Rule";
    }

    @Override
    protected EntryFormWidget.Host<FishingLootEntry> createHost() {
        return new EntryFormWidget.Host<>() {
            @Override
            public List<FishingLootEntry> list() {
                return config.get().fishingLoot();
            }

            @Override
            public FishingLootEntry build(String targetId, boolean isTag, boolean replace, List<LootItemEntry> items) {
                return new FishingLootEntry(targetId, replace, items);
            }

            @Override
            public String targetOf(FishingLootEntry entry) {
                return entry.targetLootTableId();
            }

            @Override
            public boolean isTagOf(FishingLootEntry entry) {
                return false;
            }

            @Override
            public boolean replaceOf(FishingLootEntry entry) {
                return entry.replaceVanillaTable();
            }

            @Override
            public String entryLabel(FishingLootEntry entry) {
                return entry.targetLootTableId();
            }

            @Override
            public List<LootItemEntry> itemsOf(FishingLootEntry entry) {
                return entry.items();
            }

@Override
    public String defaultTargetId() {
        return "minecraft:gameplay/fishing";
    }

    @Override
    public String defaultItemId() {
        return "minecraft:cod";
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
        List<FishingLootEntry> list = config.get().fishingLoot();
        list.add(new FishingLootEntry(stripTag(targetId), false, List.of()));
        return list.size() - 1;
    }

    @Override
    protected boolean supportsExampleRules() {
        return true;
    }

    @Override
    protected void insertExamples() {
        List<FishingLootEntry> list = config.get().fishingLoot();
        list.add(new FishingLootEntry("minecraft:gameplay/fishing", false, List.of(
            new LootItemEntry("minecraft:cod", 60, 1, 1, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:salmon", 25, 1, 1, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:pufferfish", 13, 1, 1, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:tropical_fish", 2, 1, 1, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:name_tag", 1, 1, 1, 0.2f, List.of(), List.of()),
            new LootItemEntry("minecraft:enchanted_book", 1, 1, 1, 0.15f, List.of(), List.of()),
            new LootItemEntry("minecraft:saddle", 1, 1, 1, 0.1f, List.of(), List.of()),
            new LootItemEntry("minecraft:bone", 5, 1, 2, 0.5f, List.of(), List.of()))));
    }
}