package com.komixkat.customdrops.loot;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.config.schema.BlockDropEntry;
import com.komixkat.customdrops.config.schema.ChestLootEntry;
import com.komixkat.customdrops.config.schema.FishingLootEntry;
import com.komixkat.customdrops.config.schema.LootConditionEntry;
import com.komixkat.customdrops.config.schema.LootItemEntry;
import com.komixkat.customdrops.config.schema.MobDropEntry;
import com.komixkat.customdrops.registry.IdentifierResolver;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class LootTableInjector {

    private final Supplier<CustomDropsConfig> configSupplier;

    public LootTableInjector(Supplier<CustomDropsConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    public void register() {
        LootTableEvents.REPLACE.register((resourceKey, original, source, registries) -> {
            Identifier tableId = resourceKey.identifier();
            return findMatch(configSupplier.get(), tableId)
                .filter(MatchedEntry::replaceVanillaTable)
                .map(this::buildReplacementTable)
                .orElse(null);
        });

        LootTableEvents.MODIFY.register((resourceKey, tableBuilder, source, registries) -> {
            Identifier tableId = resourceKey.identifier();
            findMatch(configSupplier.get(), tableId)
                .filter(match -> !match.replaceVanillaTable())
                .ifPresent(match -> addPool(tableBuilder, match.pool(), match.conditions()));
        });
    }

    public void reinject() {
        CustomDropsMod.LOGGER.info("Loot table config updated; call a server resource reload to apply it to loaded tables.");
    }

    private Optional<MatchedEntry> findMatch(CustomDropsConfig config, Identifier tableId) {
        if (config.mobDropsEnabled()) {
            for (MobDropEntry entry : config.mobDrops()) {
                Optional<MatchedEntry> match = matchMob(entry, tableId);
                if (match.isPresent()) return match;
            }
        }
        if (config.blockDropsEnabled()) {
            for (BlockDropEntry entry : config.blockDrops()) {
                Optional<MatchedEntry> match = matchBlock(entry, tableId);
                if (match.isPresent()) return match;
            }
        }
        if (config.chestLootEnabled()) {
            for (ChestLootEntry entry : config.chestLoot()) {
                Optional<MatchedEntry> match = matchDirect(entry.targetLootTableId(), tableId, entry.pool(), entry.conditions(), entry.replaceVanillaTable());
                if (match.isPresent()) return match;
            }
        }
        if (config.fishingLootEnabled()) {
            for (FishingLootEntry entry : config.fishingLoot()) {
                Optional<MatchedEntry> match = matchDirect(entry.targetLootTableId(), tableId, entry.pool(), entry.conditions(), entry.replaceVanillaTable());
                if (match.isPresent()) return match;
            }
        }
        return Optional.empty();
    }

    private Optional<MatchedEntry> matchMob(MobDropEntry entry, Identifier tableId) {
        return IdentifierResolver.resolve(entry.targetId()).flatMap(entityId -> {
            Identifier expected = Identifier.fromNamespaceAndPath(entityId.getNamespace(), "entities/" + entityId.getPath());
            return expected.equals(tableId)
                ? Optional.of(new MatchedEntry(entry.pool(), entry.conditions(), entry.replaceVanillaTable()))
                : Optional.<MatchedEntry>empty();
        });
    }

    private Optional<MatchedEntry> matchBlock(BlockDropEntry entry, Identifier tableId) {
        return IdentifierResolver.resolve(entry.targetId()).flatMap(blockId -> {
            Identifier expected = Identifier.fromNamespaceAndPath(blockId.getNamespace(), "blocks/" + blockId.getPath());
            return expected.equals(tableId)
                ? Optional.of(new MatchedEntry(entry.pool(), entry.conditions(), entry.replaceVanillaTable()))
                : Optional.<MatchedEntry>empty();
        });
    }

    private Optional<MatchedEntry> matchDirect(String targetId, Identifier tableId, List<LootItemEntry> pool, List<LootConditionEntry> conditions, boolean replaceVanillaTable) {
        if (targetId.endsWith("*")) {
            String prefix = targetId.substring(0, targetId.length() - 1);
            return tableId.toString().startsWith(prefix)
                ? Optional.of(new MatchedEntry(pool, conditions, replaceVanillaTable))
                : Optional.<MatchedEntry>empty();
        }
        return IdentifierResolver.resolve(targetId).flatMap(expected ->
            expected.equals(tableId)
                ? Optional.of(new MatchedEntry(pool, conditions, replaceVanillaTable))
                : Optional.<MatchedEntry>empty());
    }

    private LootTable buildReplacementTable(MatchedEntry match) {
        LootTable.Builder tableBuilder = LootTable.lootTable();
        addPool(tableBuilder, match.pool(), match.conditions());
        return tableBuilder.build();
    }

    private void addPool(LootTable.Builder tableBuilder, List<LootItemEntry> pool, List<LootConditionEntry> conditions) {
        if (pool.isEmpty()) return;
        LootPool.Builder poolBuilder = LootPool.lootPool();

        for (LootItemEntry itemEntry : pool) {
            Identifier itemId = Identifier.tryParse(itemEntry.itemId());
            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                CustomDropsMod.LOGGER.warn("Skipping unknown item id in config: {}", itemEntry.itemId());
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            LootItem.Builder entryBuilder = LootItem.lootTableItem(item).setWeight(itemEntry.weight());

            if (itemEntry.minCount() != itemEntry.maxCount()) {
                entryBuilder.apply(SetItemCountFunction.setCount(
                    UniformGenerator.between(itemEntry.minCount(), itemEntry.maxCount())));
            } else if (itemEntry.minCount() != 1) {
                entryBuilder.apply(SetItemCountFunction.setCount(ConstantValue.exactly(itemEntry.minCount())));
            }

            if (itemEntry.chance() < 1.0f) {
                entryBuilder.when(LootItemRandomChanceCondition.randomChance(itemEntry.chance()));
            }

            poolBuilder.add(entryBuilder);
        }

        for (LootConditionEntry conditionEntry : conditions) {
            LootConditionRegistry.resolve(conditionEntry).ifPresent(poolBuilder::when);
        }

        tableBuilder.withPool(poolBuilder);
    }

    private record MatchedEntry(List<LootItemEntry> pool, List<LootConditionEntry> conditions, boolean replaceVanillaTable) {}
}
