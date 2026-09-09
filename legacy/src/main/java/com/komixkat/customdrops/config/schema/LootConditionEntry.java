package com.komixkat.customdrops.config.schema;

import java.util.Map;

public record LootConditionEntry(
    ConditionType type,
    Map<String, String> params
) {
    public enum ConditionType {
        KILLED_BY_PLAYER,
        ON_FIRE,
        SILK_TOUCH,
        NO_SILK_TOUCH,
        LOOTING_LEVEL_AT_LEAST,
        RANDOM_CHANCE,
        ENTITY_ON_FIRE
    }
}
