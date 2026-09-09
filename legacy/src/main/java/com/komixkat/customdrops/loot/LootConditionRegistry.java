package com.komixkat.customdrops.loot;

import com.mojang.serialization.MapCodec;
import com.komixkat.customdrops.config.schema.LootConditionEntry;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

import java.util.Optional;
import java.util.Set;

public final class LootConditionRegistry {

    private LootConditionRegistry() {}

    public static Optional<LootItemCondition.Builder> resolve(LootConditionEntry entry) {
        return switch (entry.type()) {
            case KILLED_BY_PLAYER -> Optional.of(LootItemKilledByPlayerCondition.killedByPlayer());
            case RANDOM_CHANCE -> Optional.of(LootItemRandomChanceCondition.randomChance(
                Float.parseFloat(entry.params().getOrDefault("chance", "1.0"))));
            case SILK_TOUCH -> Optional.of(SilkTouchCondition.builder(false));
            case NO_SILK_TOUCH -> Optional.of(SilkTouchCondition.builder(true));
            default -> Optional.empty();
        };
    }

    private record SilkTouchCondition(boolean inverted) implements LootItemCondition {

        static LootItemCondition.Builder builder(boolean inverted) {
            SilkTouchCondition condition = new SilkTouchCondition(inverted);
            return () -> condition;
        }

        @Override
        public MapCodec<? extends LootItemCondition> codec() {
            return MatchTool.MAP_CODEC;
        }

        @Override
        public Set<ContextKey<?>> getReferencedContextParams() {
            return Set.of(LootContextParams.TOOL);
        }

        @Override
        public boolean test(LootContext context) {
            ItemInstance tool = context.getOptionalParameter(LootContextParams.TOOL);
            return inverted != (tool != null && hasSilkTouch(context, tool));
        }

        private boolean hasSilkTouch(LootContext context, ItemInstance tool) {
            ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            Optional<Holder.Reference<Enchantment>> silkTouch = context.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.SILK_TOUCH);
            return silkTouch
                .map(holder -> new EnchantmentPredicate(holder, MinMaxBounds.Ints.atLeast(1)).containedIn(enchantments))
                .orElse(false);
        }
    }
}
