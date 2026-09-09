package com.komixkat.customdrops.client.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.komixkat.customdrops.config.schema.LootItemEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class VanillaLootTableReader {

    private static final int MAX_DEPTH = 2;

    private VanillaLootTableReader() {}

    public static List<LootItemEntry> load(String tableId) {
        if (tableId == null || tableId.isBlank()) return List.of();
        String clean = tableId.trim().startsWith("#") ? tableId.trim().substring(1).trim() : tableId.trim();
        if (clean.isEmpty()) return List.of();
        List<LootItemEntry> out = new ArrayList<>();
        collectTable(clean, out, 0, 1);
        return out;
    }

    private static void collectTable(String id, List<LootItemEntry> out, int depth, int weightMul) {
        if (depth > MAX_DEPTH) return;
        JsonObject table = loadTableJson(id);
        if (table == null) return;
        JsonArray pools = table.has("pools") ? table.getAsJsonArray("pools") : null;
        if (pools == null || pools.isEmpty()) return;
        for (JsonElement poolEl : pools) {
            if (!poolEl.isJsonObject()) continue;
            JsonObject pool = poolEl.getAsJsonObject();
            Float poolChance = randomChanceOf(pool.get("conditions"));
            JsonArray entries = pool.has("entries") ? pool.getAsJsonArray("entries") : null;
            if (entries == null) continue;
            for (JsonElement entryEl : entries) {
                if (!entryEl.isJsonObject()) continue;
                collectEntry(entryEl.getAsJsonObject(), out, depth, weightMul * weightOf(entryEl.getAsJsonObject()),
                    poolChance);
            }
        }
    }

    private static void collectEntry(JsonObject entry, List<LootItemEntry> out, int depth, int weightMul,
                                     Float poolChance) {
        String type = strOr(entry.get("type"), "minecraft:item");
        switch (type) {
            case "minecraft:item" -> {
                String itemId = strOr(entry.get("name"), "");
                if (itemId.isBlank()) return;
                int weight = Math.max(1, weightMul);
                int[] counts = countsOf(entry);
                float chance = 1.0f;
                Float entryChance = randomChanceOf(entry.get("conditions"));
                if (entryChance != null) chance = entryChance;
                if (poolChance != null) chance *= poolChance;
                chance = Math.max(0f, Math.min(1f, chance));
                out.add(new LootItemEntry(itemId, weight, counts[0], counts[1], chance, List.of(), List.of()));
            }
            case "minecraft:loot_table" -> {
                String ref = refOf(entry);
                if (ref != null) {
                    collectTable(ref, out, depth + 1, weightMul);
                }
            }
            case "minecraft:group", "minecraft:alternatives" -> {
                JsonArray children = entry.has("children") ? entry.getAsJsonArray("children") : null;
                if (children == null) return;
                for (JsonElement child : children) {
                    if (!child.isJsonObject()) continue;
                    collectEntry(child.getAsJsonObject(), out, depth, weightMul * weightOf(child.getAsJsonObject()),
                        poolChance);
                }
            }
            default -> {
                // empty, tag, dynamic etc. cannot be prefilled reliably
            }
        }
    }

    private static int[] countsOf(JsonObject entry) {
        JsonArray funcs = entry.has("functions") ? entry.getAsJsonArray("functions") : null;
        if (funcs != null) {
            for (JsonElement f : funcs) {
                if (!f.isJsonObject()) continue;
                JsonObject fn = f.getAsJsonObject();
                String name = strOr(fn.get("function"), "");
                if (!name.endsWith("set_count")) continue;
                JsonElement count = fn.get("count");
                if (count == null || count.isJsonNull()) continue;
                if (count.isJsonPrimitive()) {
                    int v = Math.max(0, count.getAsInt());
                    return new int[]{v, v};
                }
                if (count.isJsonObject()) {
                    int min = Math.max(0, intOr(count.getAsJsonObject().get("min"), 1));
                    int max = Math.max(min, intOr(count.getAsJsonObject().get("max"), min));
                    return new int[]{min, max};
                }
            }
        }
        return new int[]{1, 1};
    }

    private static Float randomChanceOf(JsonElement conditionsEl) {
        if (conditionsEl == null || !conditionsEl.isJsonArray()) return null;
        for (JsonElement c : conditionsEl.getAsJsonArray()) {
            if (!c.isJsonObject()) continue;
            JsonObject cond = c.getAsJsonObject();
            String name = strOr(cond.get("condition"), "");
            if (name.endsWith("random_chance") && cond.has("chance")) {
                JsonElement ch = cond.get("chance");
                if (ch.isJsonPrimitive()) {
                    try {
                        return Math.max(0f, Math.min(1f, ch.getAsFloat()));
                    } catch (Exception ignored) {
                        // not a plain number - keep going
                    }
                }
            }
        }
        return null;
    }

    private static int weightOf(JsonObject entry) {
        JsonElement w = entry.get("weight");
        if (w != null && w.isJsonPrimitive()) {
            try {
                return Math.max(1, w.getAsInt());
            } catch (Exception ignored) {
                // fall through
            }
        }
        return 1;
    }

    private static String refOf(JsonObject entry) {
        JsonElement value = entry.get("value");
        if (value != null && value.isJsonPrimitive() && !value.getAsString().isBlank()) {
            return value.getAsString();
        }
        JsonElement name = entry.get("name");
        return name != null && name.isJsonPrimitive() ? name.getAsString() : null;
    }

    private static JsonObject loadTableJson(String id) {
        String[] parts = splitId(id);
        String namespace = parts[0];
        String path = parts[1];
        Minecraft mc = Minecraft.getInstance();

        JsonObject table = readJson(mc, namespace, "loot_table/" + path + ".json");
        if (table == null) {
            table = readJson(mc, namespace, "loot_tables/" + path + ".json");
        }
        return table;
    }

    private static JsonObject readJson(Minecraft mc, String namespace, String resourcePath) {
        Identifier loc = Identifier.tryBuild(namespace, resourcePath);
        if (loc != null) {
            try {
                Optional<Resource> res = mc.getResourceManager().getResource(loc);
                JsonObject table = parseResource(res);
                if (table != null) return table;
            } catch (Exception e) {
                // fall through to bundled lookup
            }
        }
        return readBundled(namespace, resourcePath);
    }

    private static JsonObject readBundled(String namespace, String resourcePath) {
        String normalized = resourcePath;
        if (normalized.startsWith("loot_table/")) {
            normalized = normalized.substring("loot_table/".length());
        } else if (normalized.startsWith("loot_tables/")) {
            normalized = normalized.substring("loot_tables/".length());
        }
        String classpath = "/data/customdrops/generated/loot_tables/" + namespace + "/" + normalized;
        try (var stream = VanillaLootTableReader.class.getResourceAsStream(classpath)) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonObject parseResource(Optional<Resource> res) {
        if (res.isEmpty()) return null;
        try (var stream = res.get().open(); Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String[] splitId(String id) {
        String namespace = "minecraft";
        String path = id;
        int colon = id.indexOf(':');
        if (colon >= 0) {
            namespace = id.substring(0, colon);
            path = id.substring(colon + 1);
        }
        if (path.startsWith("/")) path = path.substring(1);
        return new String[]{namespace, path};
    }

    private static String strOr(JsonElement e, String fallback) {
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) return fallback;
        return e.getAsString();
    }

    private static int intOr(JsonElement e, int fallback) {
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) return fallback;
        try {
            return e.getAsInt();
        } catch (Exception ex) {
            return fallback;
        }
    }
}