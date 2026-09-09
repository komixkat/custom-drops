package com.komixkat.customdrops.config.schema;

public record EquipmentOverrideEntry(
    String targetEntityId,
    boolean isTag,
    EquipmentSlotGroup slot,
    float dropChance
) {
    public static final float ALWAYS_DROP = 2.0f;

    public enum EquipmentSlotGroup {
        MAIN_HAND,
        OFF_HAND,
        HEAD,
        CHEST,
        LEGS,
        FEET
    }

    public EquipmentOverrideEntry {
        if (dropChance < 0f) throw new IllegalArgumentException("dropChance must be >= 0");
    }
}
