package com.komixkat.customdrops.config.schema;

import java.util.List;
import java.util.Map;

public record LootItemEntry(
    String itemId,
    int weight,
    int minCount,
    int maxCount,
    float chance,
    List<LootConditionEntry> conditions,
    List<EnchantmentEntry> enchantments,
    int fortuneBonus
) {
    public LootItemEntry {
        if (weight < 1) throw new IllegalArgumentException("weight must be >= 1");
        if (minCount < 0 || maxCount < minCount) throw new IllegalArgumentException("invalid count range");
        if (chance < 0f || chance > 1f) throw new IllegalArgumentException("chance must be within [0,1]");
        if (conditions == null) throw new IllegalArgumentException("conditions cannot be null");
        if (enchantments == null) throw new IllegalArgumentException("enchantments cannot be null");
        if (fortuneBonus < 0) throw new IllegalArgumentException("fortuneBonus must be >= 0");
    }

    public LootItemEntry(String itemId, int weight, int minCount, int maxCount, float chance,
                         List<LootConditionEntry> conditions, List<EnchantmentEntry> enchantments) {
        this(itemId, weight, minCount, maxCount, chance, conditions, enchantments, 0);
    }
}