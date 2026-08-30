package com.komixkat.customdrops.registry;

import com.komixkat.customdrops.CustomDropsMod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VanillaLootTableRegistry {

    private static final String RESOURCE_PATH = "/data/customdrops/generated/vanilla_loot_tables.txt";
    private static final Set<String> KNOWN_IDS = load();

    private VanillaLootTableRegistry() {}

    private static Set<String> load() {
        Set<String> ids = new HashSet<>();
        try (InputStream stream = VanillaLootTableRegistry.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                CustomDropsMod.LOGGER.info("No generated vanilla loot table list bundled (extractVanillaLootTables did not run or found nothing); id validation warnings are disabled this session.");
                return ids;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) ids.add(line.trim());
                }
            }
        } catch (IOException e) {
            CustomDropsMod.LOGGER.warn("Failed to read bundled vanilla loot table list", e);
        }
        if (!ids.isEmpty()) {
            CustomDropsMod.LOGGER.info("Loaded {} real vanilla loot table identifiers for id validation.", ids.size());
        }
        return ids;
    }

    public static boolean isKnownOrUnverifiable(String id) {
        return KNOWN_IDS.isEmpty() || KNOWN_IDS.contains(id);
    }

    public static int knownCount() {
        return KNOWN_IDS.size();
    }

    public static List<String> search(String query) {
        String lower = query.toLowerCase();
        return KNOWN_IDS.stream()
            .filter(id -> id.toLowerCase().contains(lower))
            .sorted()
            .toList();
    }
}
