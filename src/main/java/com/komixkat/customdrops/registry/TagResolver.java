package com.komixkat.customdrops.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public final class TagResolver {

    private TagResolver() {}

    public static Optional<TagKey<EntityType<?>>> resolveEntityTag(String rawId) {
        if (rawId == null || !rawId.startsWith("#")) return Optional.empty();
        Identifier location = Identifier.tryParse(rawId.substring(1));
        return location == null ? Optional.empty() : Optional.of(TagKey.create(Registries.ENTITY_TYPE, location));
    }

    public static Optional<TagKey<Block>> resolveBlockTag(String rawId) {
        if (rawId == null || !rawId.startsWith("#")) return Optional.empty();
        Identifier location = Identifier.tryParse(rawId.substring(1));
        return location == null ? Optional.empty() : Optional.of(TagKey.create(Registries.BLOCK, location));
    }
}
