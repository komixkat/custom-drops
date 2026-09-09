package com.komixkat.customdrops.config.schema;

public record LootItemEntry(
    String itemId,
    int weight,
    int minCount,
    int maxCount,
    float chance
) {
    public LootItemEntry {
        if (weight < 1) throw new IllegalArgumentException("weight must be >= 1");
        if (minCount < 0 || maxCount < minCount) throw new IllegalArgumentException("invalid count range");
        if (chance < 0f || chance > 1f) throw new IllegalArgumentException("chance must be within [0,1]");
    }
}
