package com.komixkat.customdrops.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.komixkat.customdrops.CustomDropsMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class ConfigProfiles {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] LEGACY_FILES = {
        "meta.json", "mob_drops.json", "block_drops.json", "chest_loot.json",
        "fishing_loot.json", "equipment_overrides.json"
    };

    private ConfigProfiles() {}

    public static Path root() {
        return CustomDropsMod.globalConfigDir().resolve("profiles");
    }

    public static Path indexFile() {
        return root().resolve("profiles.json");
    }

    public static synchronized void ensureGlobal() {
        if (Files.exists(indexFile())) return;
        try {
            Files.createDirectories(root());
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Could not create config profiles directory {}", root(), e);
            return;
        }
        List<String> names = new ArrayList<>();
        Path defDir = dirFor("Default");
        Path legacyDir = CustomDropsMod.globalConfigDir();
        boolean hasLegacy = false;
        for (String f : LEGACY_FILES) {
            if (Files.exists(legacyDir.resolve(f))) {
                hasLegacy = true;
                break;
            }
        }
        if (hasLegacy) {
            try {
                Files.createDirectories(defDir);
            } catch (IOException e) {
                CustomDropsMod.LOGGER.error("Could not create Default profile directory", e);
            }
            for (String f : LEGACY_FILES) {
                Path src = legacyDir.resolve(f);
                if (!Files.exists(src)) continue;
                try {
                    Files.move(src, defDir.resolve(f), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    CustomDropsMod.LOGGER.warn("Could not move {} into Default profile", src, e);
                }
            }
        }
        names.add("Default");
        writeIndex(names, "Default");
        CustomDropsMod.LOGGER.info("Custom Drops initialized config profiles; active profile is \"Default\".");
    }

    public static synchronized List<String> names() {
        ensureGlobal();
        Index idx = readIndex();
        if (idx == null || idx.names == null || idx.names.isEmpty()) {
            return List.of("Default");
        }
        return List.copyOf(idx.names);
    }

    public static synchronized String active() {
        ensureGlobal();
        Index idx = readIndex();
        if (idx != null && idx.active != null && !idx.active.isEmpty()
            && idx.names != null && idx.names.contains(idx.active)) {
            return idx.active;
        }
        List<String> names = names();
        return names.isEmpty() ? "Default" : names.get(0);
    }

    public static Path dirFor(String name) {
        return root().resolve(sanitize(name));
    }

    public static Path activeDir() {
        return dirFor(active());
    }

    public static synchronized boolean create(String name) {
        return create(name, CustomDropsMod.config());
    }

    public static synchronized boolean create(String name, CustomDropsConfig config) {
        if (!validName(name) || names().contains(name)) return false;
        Path dir = dirFor(name);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Could not create profile directory {}", dir, e);
            return false;
        }
        ConfigLoader.save(dir, config);
        Index idx = readIndex();
        if (idx == null) {
            idx = new Index();
            idx.names = new ArrayList<>();
            idx.active = active();
        }
        if (!idx.names.contains(name)) {
            idx.names.add(name);
        }
        writeIndex(idx);
        return true;
    }

    public static synchronized boolean switchTo(String name) {
        if (!names().contains(name)) return false;
        Index idx = readIndex();
        if (idx == null) {
            idx = new Index();
            idx.names = new ArrayList<>(names());
        }
        idx.active = name;
        writeIndex(idx);
        CustomDropsMod.reloadFromProfile();
        return true;
    }

    public static synchronized boolean rename(String oldName, String newName) {
        if (!validName(newName) || !names().contains(oldName) || names().contains(newName)) return false;
        Path oldDir = dirFor(oldName);
        Path newDir = dirFor(newName);
        try {
            Files.move(oldDir, newDir);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Could not rename profile {} to {}", oldName, newName, e);
            return false;
        }
        Index idx = readIndex();
        if (idx == null) {
            idx = new Index();
            idx.names = new ArrayList<>(names());
        }
        idx.names.replaceAll(n -> n.equals(oldName) ? newName : n);
        if (oldName.equals(idx.active)) {
            idx.active = newName;
        }
        writeIndex(idx);
        return true;
    }

    public static synchronized boolean duplicate(String from, String newName) {
        if (!names().contains(from) || !validName(newName) || names().contains(newName)) return false;
        Path src = dirFor(from);
        Path dst = dirFor(newName);
        try {
            Files.createDirectories(dst);
            try (var stream = Files.walk(src)) {
                java.util.List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .toList();
                for (Path file : files) {
                    Path rel = src.relativize(file);
                    Path target = dst.resolve(rel);
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Could not duplicate profile {} to {}", from, newName, e);
            return false;
        }
        Index idx = readIndex();
        if (idx == null) {
            idx = new Index();
            idx.names = new ArrayList<>(names());
        }
        if (!idx.names.contains(newName)) {
            idx.names.add(newName);
        }
        writeIndex(idx);
        return true;
    }

    public static synchronized boolean delete(String name) {
        if (!names().contains(name)) return false;
        if (name.equals(active())) return false;
        deleteRecursive(dirFor(name));
        Index idx = readIndex();
        if (idx == null) {
            idx = new Index();
            idx.names = new ArrayList<>(names());
        }
        idx.names.remove(name);
        writeIndex(idx);
        return true;
    }

    public static synchronized void resetActiveToVanilla() {
        ConfigLoader.save(activeDir(), new CustomDropsConfig());
        CustomDropsMod.reloadFromProfile();
    }

    private static void deleteRecursive(Path dir) {
        try {
            if (Files.exists(dir)) {
                try (var stream = Files.walk(dir)) {
                    java.util.List<Path> all = stream.sorted(java.util.Comparator.reverseOrder()).toList();
                    for (Path p : all) {
                        Files.deleteIfExists(p);
                    }
                }
            }
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Could not delete profile directory {}", dir, e);
        }
    }

    public static boolean validName(String name) {
        return name != null && !name.trim().isEmpty() && sanitize(name).equals(name.trim());
    }

    public static String sanitize(String name) {
        String trimmed = name == null ? "" : name.trim();
        String safe = trimmed.replaceAll("[^A-Za-z0-9 _./-]", "_");
        while (safe.contains("__")) {
            safe = safe.replace("__", "_");
        }
        return safe;
    }

    private static void writeIndex(List<String> names, String active) {
        Index idx = new Index();
        idx.names = new ArrayList<>(names);
        idx.active = active;
        writeIndex(idx);
    }

    private static void writeIndex(Index idx) {
        try (Writer writer = Files.newBufferedWriter(indexFile(), StandardCharsets.UTF_8)) {
            GSON.toJson(idx, writer);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to write profile index {}", indexFile(), e);
        }
    }

    private static Index readIndex() {
        if (!Files.exists(indexFile())) return null;
        try (Reader reader = Files.newBufferedReader(indexFile(), StandardCharsets.UTF_8)) {
            Index idx = GSON.fromJson(reader, Index.class);
            if (idx != null && idx.names == null) {
                idx.names = new ArrayList<>();
            }
            return idx;
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to read profile index {}", indexFile(), e);
            return null;
        }
    }

    private static final class Index {
        String active;
        List<String> names = new ArrayList<>();
    }
}