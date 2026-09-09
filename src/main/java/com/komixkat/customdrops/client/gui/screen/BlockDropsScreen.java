package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.client.gui.widget.EntryFormWidget;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.config.schema.BlockDropEntry;
import com.komixkat.customdrops.config.schema.LootConditionEntry;
import com.komixkat.customdrops.config.schema.LootItemEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

public final class BlockDropsScreen extends LootRuleScreen<BlockDropEntry> {

    public BlockDropsScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.config.category.blockDrops"));
    }

    public BlockDropsScreen(net.minecraft.client.gui.screens.Screen parent,
                            Supplier<CustomDropsConfig> config, Mode mode) {
        super(parent, Component.translatable("customdrops.config.category.blockDrops"), config, mode);
    }

    @Override
    protected String categoryTitle() {
        return "Block Drop Rules";
    }

    @Override
    protected String singularLabel() {
        return "Block Drop Rule";
    }

    @Override
    protected EntryFormWidget.Host<BlockDropEntry> createHost() {
        return new EntryFormWidget.Host<>() {
            @Override
            public List<BlockDropEntry> list() {
                return config.get().blockDrops();
            }

            @Override
            public BlockDropEntry build(String targetId, boolean isTag, boolean replace, List<LootItemEntry> items) {
                return new BlockDropEntry(targetId, isTag, replace, items);
            }

            @Override
            public String targetOf(BlockDropEntry entry) {
                return entry.targetId();
            }

            @Override
            public boolean isTagOf(BlockDropEntry entry) {
                return entry.isTag();
            }

            @Override
            public boolean replaceOf(BlockDropEntry entry) {
                return entry.replaceVanillaTable();
            }

            @Override
            public String entryLabel(BlockDropEntry entry) {
                return displayId(entry.targetId(), entry.isTag());
            }

            @Override
            public List<LootItemEntry> itemsOf(BlockDropEntry entry) {
                return entry.items();
            }

@Override
    public String defaultTargetId() {
        return "minecraft:stone";
    }

    @Override
    public String defaultItemId() {
        return "minecraft:cobblestone";
    }

            @Override
            public String targetFieldLabel() {
                return "Block id (or #tag)";
            }

            @Override
            public java.util.List<LootConditionEntry.ConditionType> allowedConditions() {
                return java.util.List.of(
                    LootConditionEntry.ConditionType.SILK_TOUCH,
                    LootConditionEntry.ConditionType.NO_SILK_TOUCH,
                    LootConditionEntry.ConditionType.FORTUNE_LEVEL_AT_LEAST);
            }

            @Override
            public String lookupKind() {
                return "block";
            }

            @Override
            public boolean supportsVanillaLink() {
                return true;
            }

            @Override
            public String vanillaLookupTarget(String targetId) {
                if (targetId == null || targetId.isBlank() || targetId.startsWith("#")) return null;
                String clean = targetId.indexOf(':') >= 0 ? targetId.substring(targetId.indexOf(':') + 1) : targetId;
                return "minecraft:blocks/" + clean;
            }
        };
    }

    @Override
    protected int addEntry(String targetId) {
        List<BlockDropEntry> list = config.get().blockDrops();
        list.add(new BlockDropEntry(stripTag(targetId), targetId.startsWith("#"), false, List.of()));
        return list.size() - 1;
    }

    @Override
    protected boolean supportsExampleRules() {
        return true;
    }

    @Override
    protected void insertExamples() {
        List<BlockDropEntry> list = config.get().blockDrops();
        list.add(new BlockDropEntry("minecraft:stone", false, false, List.of(
            new LootItemEntry("minecraft:cobblestone", 1, 1, 1, 1.0f, List.of(), List.of()),
            new LootItemEntry("minecraft:flint", 1, 0, 1, 0.5f, List.of(), List.of()),
            new LootItemEntry("minecraft:gold_nugget", 1, 1, 2, 0.02f, List.of(), List.of()))));
        list.add(new BlockDropEntry("minecraft:copper_ores", true, false, List.of(
            new LootItemEntry("minecraft:raw_copper", 1, 1, 2, 1.0f, List.of(), List.of()))));
    }
}