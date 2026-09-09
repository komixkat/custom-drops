package com.komixkat.customdrops.config.schema;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record EnchantmentEntry(
    String enchantmentId,
    int level
) {
    public static final Codec<EnchantmentEntry> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("enchantmentId").forGetter(EnchantmentEntry::enchantmentId),
            Codec.INT.fieldOf("level").forGetter(EnchantmentEntry::level)
        ).apply(instance, EnchantmentEntry::new)
    );

    public EnchantmentEntry {
        if (level < 1 || level > 255) {
            throw new IllegalArgumentException("Enchantment level must be between 1 and 255");
        }
    }
}