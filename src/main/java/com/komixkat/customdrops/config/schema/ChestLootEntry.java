package com.komixkat.customdrops.config.schema;

import java.util.List;

public record ChestLootEntry(
    String targetLootTableId,
    boolean replaceVanillaTable,
    List<LootItemEntry> pool,
    List<LootConditionEntry> conditions
) {}
