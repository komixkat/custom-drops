package com.komixkat.customdrops.registry;

import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class IdentifierResolver {

    private IdentifierResolver() {}

    public static Optional<Identifier> resolve(String rawId) {
        if (rawId == null || rawId.isBlank() || rawId.startsWith("#")) {
            return Optional.empty();
        }
        return Identifier.tryParse(rawId) != null
            ? Optional.of(Identifier.parse(rawId))
            : Optional.empty();
    }
}
