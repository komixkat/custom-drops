package com.komixkat.customdrops.registry;

import com.komixkat.customdrops.CustomDropsMod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Bundled, game-version-accurate id lists generated at build time from the real
 * Minecraft jars (see the {@code extractVanillaLootTables} Gradle task). Used as a
 * completeness fallback for autocomplete and id validation when the live registry
 * scans turn out to be incomplete (for example item registries populated lazily),
 * and as consistent preset authoring data.
 */
public final class VanillaIdRegistry {

    private static final String ITEM_PATH = "/data/customdrops/generated/vanilla_item_ids.txt";
    private static final String BLOCK_PATH = "/data/customdrops/generated/vanilla_block_ids.txt";
    private static final String ENTITY_PATH = "/data/customdrops/generated/vanilla_entity_ids.txt";

    private static final Set<String> ITEM_IDS = load("item", ITEM_PATH);
    private static final Set<String> BLOCK_IDS = load("block", BLOCK_PATH);
    private static final Set<String> ENTITY_IDS = load("entity", ENTITY_PATH);

    private static final Set<String> DROPPABLE_IDS = new LinkedHashSet<>();
    static {
        DROPPABLE_IDS.addAll(ITEM_IDS);
        DROPPABLE_IDS.addAll(BLOCK_IDS);
    }

    private VanillaIdRegistry() {}

    private static Set<String> load(String kind, String resourcePath) {
        Set<String> ids = new HashSet<>();
        try (InputStream stream = VanillaIdRegistry.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return ids;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        ids.add(line.trim());
                    }
                }
            }
        } catch (IOException e) {
            CustomDropsMod.LOGGER.warn("Failed to read bundled vanilla {} id list", kind, e);
        }
        return ids;
    }

    public static Set<String> droppableItemIds() {
        return DROPPABLE_IDS;
    }

    public static Set<String> blockIds() {
        return BLOCK_IDS;
    }

    public static Set<String> entityIds() {
        return ENTITY_IDS;
    }

    public static int bundledDroppableCount() {
        return DROPPABLE_IDS.size();
    }
}