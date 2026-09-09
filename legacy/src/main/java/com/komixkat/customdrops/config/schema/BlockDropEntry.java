package com.komixkat.customdrops.config.schema;

import java.util.List;

public record BlockDropEntry(
    String targetId,
    boolean isTag,
    boolean replaceVanillaTable,
    List<LootItemEntry> pool,
    List<LootConditionEntry> conditions
) {}
