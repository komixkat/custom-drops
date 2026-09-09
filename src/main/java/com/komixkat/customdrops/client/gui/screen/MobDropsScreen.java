package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.client.gui.widget.EntryFormWidget;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.config.schema.LootConditionEntry;
import com.komixkat.customdrops.config.schema.LootItemEntry;
import com.komixkat.customdrops.config.schema.MobDropEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

public final class MobDropsScreen extends LootRuleScreen<MobDropEntry> {

    public MobDropsScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.config.category.mobDrops"));
    }

    public MobDropsScreen(net.minecraft.client.gui.screens.Screen parent,
                          Supplier<CustomDropsConfig> config, Mode mode) {
        super(parent, Component.translatable("customdrops.config.category.mobDrops"), config, mode);
    }

    @Override
    protected String categoryTitle() {
        return "Mob Drop Rules";
    }

    @Override
    protected String singularLabel() {
        return "Mob Drop Rule";
    }

    @Override
    protected EntryFormWidget.Host<MobDropEntry> createHost() {
        return new EntryFormWidget.Host<>() {
            @Override
            public List<MobDropEntry> list() {
                return config.get().mobDrops();
            }

            @Override
            public MobDropEntry build(String targetId, boolean isTag, boolean replace, List<LootItemEntry> items) {
                return new MobDropEntry(targetId, isTag, replace, items);
            }

            @Override
            public String targetOf(MobDropEntry entry) {
                return entry.targetId();
            }

            @Override
            public boolean isTagOf(MobDropEntry entry) {
                return entry.isTag();
            }

            @Override
            public boolean replaceOf(MobDropEntry entry) {
                return entry.replaceVanillaTable();
            }

            @Override
            public String entryLabel(MobDropEntry entry) {
                return displayId(entry.targetId(), entry.isTag());
            }

            @Override
            public List<LootItemEntry> itemsOf(MobDropEntry entry) {
                return entry.items();
            }

@Override
    public String defaultTargetId() {
        return "minecraft:zombie";
    }

    @Override
    public String defaultItemId() {
        return "minecraft:rotten_flesh";
    }

            @Override
            public String targetFieldLabel() {
                return "Entity id (or #tag)";
            }

            @Override
            public String lookupKind() {
                return "entity";
            }

            @Override
            public boolean supportsVanillaLink() {
                return true;
            }

            @Override
            public java.util.List<LootConditionEntry.ConditionType> allowedConditions() {
                return java.util.List.of(
                    LootConditionEntry.ConditionType.KILLED_BY_PLAYER,
                    LootConditionEntry.ConditionType.ON_FIRE,
                    LootConditionEntry.ConditionType.SILK_TOUCH,
                    LootConditionEntry.ConditionType.NO_SILK_TOUCH,
                    LootConditionEntry.ConditionType.ENTITY_ON_FIRE,
                    LootConditionEntry.ConditionType.LOOTING_LEVEL_AT_LEAST);
            }

            @Override
            public String vanillaLookupTarget(String targetId) {
                if (targetId == null || targetId.isBlank() || targetId.startsWith("#")) return null;
                String clean = targetId.indexOf(':') >= 0 ? targetId.substring(targetId.indexOf(':') + 1) : targetId;
                return "minecraft:entities/" + clean;
            }
        };
    }

    @Override
    protected int addEntry(String targetId) {
        List<MobDropEntry> list = config.get().mobDrops();
        list.add(new MobDropEntry(stripTag(targetId), targetId.startsWith("#"), false, List.of()));
        return list.size() - 1;
    }

    @Override
    protected boolean supportsExampleRules() {
        return true;
    }

    @Override
    protected void insertExamples() {
        List<MobDropEntry> list = config.get().mobDrops();
        list.add(new MobDropEntry("minecraft:zombie", false, false, List.of(
            new LootItemEntry("minecraft:rotten_flesh", 3, 1, 1, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:iron_ingot", 1, 0, 1, 0.085f, List.of(), List.of()),
            new LootItemEntry("minecraft:carrot", 1, 0, 1, 0.085f, List.of(), List.of()),
            new LootItemEntry("minecraft:potato", 1, 0, 1, 0.085f, List.of(), List.of()))));
        list.add(new MobDropEntry("minecraft:shulker", false, false, List.of(
            new LootItemEntry("minecraft:shulker_shell", 2, 6, 10, 1.0f, List.of(), List.of()))));
        list.add(new MobDropEntry("minecraft:skeletons", true, false, List.of(
            new LootItemEntry("minecraft:bone", 3, 0, 2, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:arrow", 2, 0, 2, 0.5f, List.of(), List.of()))));
    }
}