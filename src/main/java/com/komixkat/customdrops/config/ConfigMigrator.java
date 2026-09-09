package com.komixkat.customdrops.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.komixkat.customdrops.CustomDropsMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ConfigMigrator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int TARGET_VERSION = 2;

    private ConfigMigrator() {}

    public record MigrationResult(boolean migrated, List<String> changes) {}

    public static MigrationResult migrateIfNeeded(Path configDir) {
        List<String> changes = new ArrayList<>();
        changes.addAll(migrateV1ToV2(configDir));

        if (!changes.isEmpty()) {
            writeSchemaVersion(configDir.resolve("meta.json"), TARGET_VERSION);
            CustomDropsMod.LOGGER.info("Config migrated to schema v{} with {} changes", TARGET_VERSION, changes.size());
            return new MigrationResult(true, changes);
        }

        int currentVersion = readSchemaVersion(configDir.resolve("meta.json"));
        if (currentVersion < TARGET_VERSION) {
            writeSchemaVersion(configDir.resolve("meta.json"), TARGET_VERSION);
            CustomDropsMod.LOGGER.info("Config schema version bumped to v{} (no data changes)", TARGET_VERSION);
            return new MigrationResult(true, List.of());
        }

        return new MigrationResult(false, List.of());
    }

    private static List<String> migrateV1ToV2(Path configDir) {
        List<String> changes = new ArrayList<>();

        Path mobDrops = configDir.resolve("mob_drops.json");
        if (Files.exists(mobDrops)) {
            changes.addAll(migrateItemsList(mobDrops, "mob_drops.json"));
        }

        Path blockDrops = configDir.resolve("block_drops.json");
        if (Files.exists(blockDrops)) {
            changes.addAll(migrateItemsList(blockDrops, "block_drops.json"));
        }

        Path chestLoot = configDir.resolve("chest_loot.json");
        if (Files.exists(chestLoot)) {
            changes.addAll(migrateItemsList(chestLoot, "chest_loot.json"));
        }

        Path fishingLoot = configDir.resolve("fishing_loot.json");
        if (Files.exists(fishingLoot)) {
            changes.addAll(migrateItemsList(fishingLoot, "fishing_loot.json"));
        }

        return changes;
    }

    private static List<String> migrateItemsList(Path path, String fileName) {
        List<String> changes = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) return changes;

            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject entry = element.getAsJsonObject();

                if (entry.has("pool") && !entry.has("items")) {
                    JsonElement pool = entry.get("pool");
                    entry.add("items", pool);
                    entry.remove("pool");
                    changes.add(fileName + ": renamed 'pool' to 'items'");
                }

                JsonElement entryConditions = null;
                if (entry.has("conditions")) {
                    JsonElement conditions = entry.get("conditions");
                    entry.remove("conditions");
                    if (conditions.isJsonArray() && conditions.getAsJsonArray().size() > 0) {
                        entryConditions = conditions;
                        changes.add(fileName + ": moved entry-level conditions onto its items");
                    }
                }

                if (entry.has("items") && entry.get("items").isJsonArray()) {
                    for (JsonElement itemElem : entry.getAsJsonArray("items")) {
                        if (!itemElem.isJsonObject()) continue;
                        JsonObject item = itemElem.getAsJsonObject();

                        if (!item.has("conditions")) {
                            item.add("conditions",
                                entryConditions != null ? entryConditions.deepCopy() : new JsonArray());
                        } else if (entryConditions != null && item.get("conditions").isJsonArray()) {
                            JsonArray merged = new JsonArray();
                            merged.addAll(item.getAsJsonArray("conditions"));
                            merged.addAll(entryConditions.deepCopy().getAsJsonArray());
                            item.add("conditions", merged);
                        }

                        if (!item.has("enchantments")) {
                            item.add("enchantments", new JsonArray());
                            changes.add(fileName + ": added empty 'enchantments' to item");
                        }
                    }
                }
            }

            if (!changes.isEmpty()) {
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    GSON.toJson(root, writer);
                }
            }
            return changes;
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to migrate {}", path, e);
            return changes;
        }
    }

    private static int readSchemaVersion(Path metaPath) {
        try (Reader reader = Files.newBufferedReader(metaPath, StandardCharsets.UTF_8)) {
            JsonObject meta = JsonParser.parseReader(reader).getAsJsonObject();
            if (meta.has("schemaVersion")) return meta.get("schemaVersion").getAsInt();
        } catch (Exception e) {
            CustomDropsMod.LOGGER.warn("Could not read schema version from {}", metaPath);
        }
        return 1;
    }

    private static void writeSchemaVersion(Path metaPath, int version) {
        try {
            JsonObject meta;
            if (Files.exists(metaPath)) {
                try (Reader reader = Files.newBufferedReader(metaPath, StandardCharsets.UTF_8)) {
                    meta = JsonParser.parseReader(reader).getAsJsonObject();
                }
            } else {
                meta = new JsonObject();
            }
            meta.addProperty("schemaVersion", version);
            try (Writer writer = Files.newBufferedWriter(metaPath, StandardCharsets.UTF_8)) {
                GSON.toJson(meta, writer);
            }
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to write schema version to {}", metaPath, e);
        }
    }

    public static String formatDiff(List<String> changes) {
        if (changes.isEmpty()) return "No changes needed.";
        StringBuilder sb = new StringBuilder();
        sb.append("Config migration changes (v1 -> v2):\n");
        for (String change : changes) {
            sb.append("  - ").append(change).append("\n");
        }
        return sb.toString();
    }
}