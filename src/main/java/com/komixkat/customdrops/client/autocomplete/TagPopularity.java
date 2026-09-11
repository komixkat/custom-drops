package com.komixkat.customdrops.client.autocomplete;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers which tags the user picks, so frequently used tags float to the top of the
 * tag picker (typing {@code #}) across screens and game sessions. Persisted to a small
 * json file next to the global config.
 */
public final class TagPopularity {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Integer> counts = new LinkedHashMap<>();
    private static boolean loaded = false;

    private TagPopularity() {}

    public static int get(String tagId) {
        loadIfNeeded();
        return counts.getOrDefault(tagId, 0);
    }

    public static void add(String tagId) {
        loadIfNeeded();
        counts.merge(tagId, 1, Integer::sum);
        flush();
    }

    public static void loadInto(Map<String, Integer> target) {
        loadIfNeeded();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            target.merge(e.getKey(), e.getValue(), Integer::sum);
        }
    }

    public static List<Map.Entry<String, Integer>> top(int limit) {
        loadIfNeeded();
        List<Map.Entry<String, Integer>> out = new ArrayList<>(counts.entrySet());
        out.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    private static void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        try {
            Path path = recentTagsPath();
            if (Files.exists(path)) {
                String json = Files.readString(path);
                Map<String, Integer> parsed = GSON.fromJson(json, new TypeToken<LinkedHashMap<String, Integer>>() {}.getType());
                if (parsed != null) {
                    counts.putAll(parsed);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void flush() {
        try {
            Path path = recentTagsPath();
            Files.createDirectories(path.getParent());
            String json = GSON.toJson(counts);
            Files.writeString(path, json);
        } catch (Throwable ignored) {
        }
    }

    private static Path recentTagsPath() {
        return com.komixkat.customdrops.CustomDropsMod.globalConfigDir().resolve("recent-tags.json");
    }
}