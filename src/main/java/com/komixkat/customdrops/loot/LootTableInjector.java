package com.komixkat.customdrops.loot;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.config.schema.BlockDropEntry;
import com.komixkat.customdrops.config.schema.ChestLootEntry;
import com.komixkat.customdrops.config.schema.FishingLootEntry;
import com.komixkat.customdrops.config.schema.EnchantmentEntry;
import com.komixkat.customdrops.config.schema.LootConditionEntry;
import com.komixkat.customdrops.config.schema.LootItemEntry;
import com.komixkat.customdrops.config.schema.MobDropEntry;
import com.komixkat.customdrops.registry.IdentifierResolver;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.ArrayList;
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
            return findMatch(configSupplier.get(), tableId, registries)
                .filter(MatchedEntry::replaceVanillaTable)
                .map(match -> buildReplacementTable(match, registries))
                .orElse(null);
        });

        LootTableEvents.MODIFY.register((resourceKey, tableBuilder, source, registries) -> {
            Identifier tableId = resourceKey.identifier();
            findMatch(configSupplier.get(), tableId, registries)
                .filter(match -> !match.replaceVanillaTable())
                .ifPresent(match -> addPool(tableBuilder, match.pool(), match.conditions(), registries));
        });
    }

    public void reinject() {
        CustomDropsMod.LOGGER.info("Loot table config updated; call a server resource reload to apply it to loaded tables.");
    }

    private Optional<MatchedEntry> findMatch(CustomDropsConfig config, Identifier tableId, HolderLookup.Provider registries) {
        if (config.mobDropsEnabled()) {
            for (MobDropEntry entry : config.mobDrops()) {
                Optional<MatchedEntry> match = matchMob(entry, tableId, registries);
                if (match.isPresent()) return match;
            }
        }
        if (config.blockDropsEnabled()) {
            for (BlockDropEntry entry : config.blockDrops()) {
                Optional<MatchedEntry> match = matchBlock(entry, tableId, registries);
                if (match.isPresent()) return match;
            }
        }
        if (config.chestLootEnabled()) {
            for (ChestLootEntry entry : config.chestLoot()) {
                Optional<MatchedEntry> match = matchDirect(entry.targetLootTableId(), tableId, entry.items(), entry.replaceVanillaTable());
                if (match.isPresent()) return match;
            }
        }
        if (config.fishingLootEnabled()) {
            for (FishingLootEntry entry : config.fishingLoot()) {
                Optional<MatchedEntry> match = matchDirect(entry.targetLootTableId(), tableId, entry.items(), entry.replaceVanillaTable());
                if (match.isPresent()) return match;
            }
        }
        return Optional.empty();
    }

    private Optional<MatchedEntry> matchMob(MobDropEntry entry, Identifier tableId, HolderLookup.Provider registries) {
        if (entry.isTag()) {
            Identifier tagId = Identifier.tryParse(entry.targetId());
            if (tagId == null) return Optional.empty();
            return mobInTag(TagKey.create(Registries.ENTITY_TYPE, tagId), tableId, registries,
                entry.items(), entry.replaceVanillaTable());
        }
        return IdentifierResolver.resolve(entry.targetId()).flatMap(entityId -> {
            Identifier expected = Identifier.fromNamespaceAndPath(entityId.getNamespace(), "entities/" + entityId.getPath());
            return expected.equals(tableId)
                ? Optional.of(new MatchedEntry(entry.items(), List.of(), entry.replaceVanillaTable()))
                : Optional.<MatchedEntry>empty();
        });
    }

    private Optional<MatchedEntry> mobInTag(TagKey<EntityType<?>> tag, Identifier tableId, HolderLookup.Provider registries,
                                            List<LootItemEntry> items, boolean replace) {
        if (!tableId.getPath().startsWith("entities/")) return Optional.empty();
        Identifier entityId = Identifier.fromNamespaceAndPath(tableId.getNamespace(),
            tableId.getPath().substring("entities/".length()));
        try {
            HolderLookup.RegistryLookup<EntityType<?>> lookup = registries.lookupOrThrow(Registries.ENTITY_TYPE);
            Optional<Holder.Reference<EntityType<?>>> holder =
                lookup.get(ResourceKey.create(Registries.ENTITY_TYPE, entityId));
            Optional<HolderSet.Named<EntityType<?>>> tagSet = lookup.get(tag);
            if (holder.isPresent() && tagSet.isPresent() && tagSet.get().contains(holder.get())) {
                return Optional.of(new MatchedEntry(items, List.of(), replace));
            }
        } catch (RuntimeException e) {
            CustomDropsMod.LOGGER.debug("Could not resolve entity tag {} against loot table {}", tag, tableId, e);
        }
        return Optional.empty();
    }

    private Optional<MatchedEntry> matchBlock(BlockDropEntry entry, Identifier tableId, HolderLookup.Provider registries) {
        if (entry.isTag()) {
            Identifier tagId = Identifier.tryParse(entry.targetId());
            if (tagId == null) return Optional.empty();
            return blockInTag(TagKey.create(Registries.BLOCK, tagId), tableId, registries,
                entry.items(), entry.replaceVanillaTable());
        }
        return IdentifierResolver.resolve(entry.targetId()).flatMap(blockId -> {
            Identifier expected = Identifier.fromNamespaceAndPath(blockId.getNamespace(), "blocks/" + blockId.getPath());
            return expected.equals(tableId)
                ? Optional.of(new MatchedEntry(entry.items(), List.of(), entry.replaceVanillaTable()))
                : Optional.<MatchedEntry>empty();
        });
    }

    private Optional<MatchedEntry> blockInTag(TagKey<Block> tag, Identifier tableId, HolderLookup.Provider registries,
                                              List<LootItemEntry> items, boolean replace) {
        if (!tableId.getPath().startsWith("blocks/")) return Optional.empty();
        Identifier blockId = Identifier.fromNamespaceAndPath(tableId.getNamespace(),
            tableId.getPath().substring("blocks/".length()));
        try {
            HolderLookup.RegistryLookup<Block> lookup = registries.lookupOrThrow(Registries.BLOCK);
            Optional<Holder.Reference<Block>> holder =
                lookup.get(ResourceKey.create(Registries.BLOCK, blockId));
            Optional<HolderSet.Named<Block>> tagSet = lookup.get(tag);
            if (holder.isPresent() && tagSet.isPresent() && tagSet.get().contains(holder.get())) {
                return Optional.of(new MatchedEntry(items, List.of(), replace));
            }
        } catch (RuntimeException e) {
            CustomDropsMod.LOGGER.debug("Could not resolve block tag {} against loot table {}", tag, tableId, e);
        }
        return Optional.empty();
    }

    private Optional<MatchedEntry> matchDirect(String targetId, Identifier tableId, List<LootItemEntry> items, boolean replaceVanillaTable) {
        if (targetId.endsWith("*")) {
            String prefix = targetId.substring(0, targetId.length() - 1);
            return tableId.toString().startsWith(prefix)
                ? Optional.of(new MatchedEntry(items, List.of(), replaceVanillaTable))
                : Optional.<MatchedEntry>empty();
        }
        return IdentifierResolver.resolve(targetId).flatMap(expected ->
            expected.equals(tableId)
                ? Optional.of(new MatchedEntry(items, List.of(), replaceVanillaTable))
                : Optional.<MatchedEntry>empty());
    }

    private LootTable buildReplacementTable(MatchedEntry match, HolderLookup.Provider registries) {
        LootTable.Builder tableBuilder = LootTable.lootTable();
        addPool(tableBuilder, match.pool(), match.conditions(), registries);
        return tableBuilder.build();
    }

    private void addPool(LootTable.Builder tableBuilder, List<LootItemEntry> pool,
                         List<LootConditionEntry> poolConditions, HolderLookup.Provider registries) {
        if (pool == null || pool.isEmpty()) return;
        LootPool.Builder poolBuilder = LootPool.lootPool();

        for (LootItemEntry itemEntry : pool) {
            Identifier itemId = Identifier.tryParse(itemEntry.itemId());
            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                CustomDropsMod.LOGGER.warn("Skipping unknown item id in config: {}", itemEntry.itemId());
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            LootItem.Builder entryBuilder = LootItem.lootTableItem(item).setWeight(itemEntry.weight());

            boolean fortune = itemEntry.fortuneBonus() > 0;
            if (itemEntry.minCount() != itemEntry.maxCount()) {
                entryBuilder.apply(SetItemCountFunction.setCount(
                    UniformGenerator.between(itemEntry.minCount(), itemEntry.maxCount())));
            } else if (itemEntry.minCount() != 1 || fortune) {
                entryBuilder.apply(SetItemCountFunction.setCount(ConstantValue.exactly(itemEntry.minCount())));
            }

            if (fortune) {
                fortuneHolder(registries).ifPresent(holder ->
                    entryBuilder.apply(ApplyBonusCount.addOreBonusCount(holder)));
            }

            if (itemEntry.chance() < 1.0f) {
                entryBuilder.when(LootItemRandomChanceCondition.randomChance(itemEntry.chance()));
            }

            for (LootConditionEntry conditionEntry : itemEntry.conditions()) {
                LootConditionRegistry.resolve(conditionEntry).ifPresent(entryBuilder::when);
            }

            applyEnchantments(entryBuilder, itemEntry.enchantments(), registries);

            poolBuilder.add(entryBuilder);
        }

        for (LootConditionEntry conditionEntry : poolConditions) {
            LootConditionRegistry.resolve(conditionEntry).ifPresent(poolBuilder::when);
        }

        tableBuilder.withPool(poolBuilder);
    }

    private Optional<Holder.Reference<Enchantment>> fortuneHolder(HolderLookup.Provider registries) {
        try {
            return registries.lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.FORTUNE);
        } catch (Throwable t) {
            CustomDropsMod.LOGGER.warn("Could not look up enchantment registry, skipping fortune bonus.", t);
            return Optional.empty();
        }
    }

    private void applyEnchantments(LootItem.Builder entryBuilder, List<EnchantmentEntry> enchantments,
                                   HolderLookup.Provider registries) {
        if (enchantments == null || enchantments.isEmpty()) return;
        HolderLookup.RegistryLookup<Enchantment> enchantRegistry;
        try {
            enchantRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        } catch (Throwable t) {
            CustomDropsMod.LOGGER.warn("Could not look up enchantment registry, skipping enchantments.", t);
            return;
        }
        for (EnchantmentEntry entry : enchantments) {
            Identifier id = Identifier.tryParse(entry.enchantmentId());
            if (id == null) continue;
            Optional<Holder.Reference<Enchantment>> holder = enchantRegistry.get(
                ResourceKey.create(Registries.ENCHANTMENT, id));
            if (holder.isEmpty()) {
                CustomDropsMod.LOGGER.warn("Skipping unknown enchantment id in config: {}", entry.enchantmentId());
                continue;
            }
            entryBuilder.apply(new SetEnchantmentsFunction.Builder()
                .withEnchantment(holder.get(), ConstantValue.exactly(entry.level())));
        }
    }

    private record MatchedEntry(List<LootItemEntry> pool, List<LootConditionEntry> conditions, boolean replaceVanillaTable) {}
}