package com.komixkat.customdrops.registry;

import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class IdentifierResolver {

    private IdentifierResolver() {}

    public static Optional<Identifier> resolve(String rawId) {
        if (rawId == null || rawId.isBlank() || rawId.startsWith("#")) {
            return Optional.empty();
        }
        Identifier parsed = Identifier.tryParse(rawId);
        return parsed != null ? Optional.of(parsed) : Optional.empty();
    }
}