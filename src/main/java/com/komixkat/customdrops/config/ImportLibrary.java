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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ImportLibrary {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");

    private ImportLibrary() {}

    public static String suggestedName(String base) {
        return base + " " + STAMP.format(LocalDateTime.now());
    }

    public static Path root() {
        return CustomDropsMod.globalConfigDir().resolve("imports");
    }

    public static Path indexFile() {
        return root().resolve("index.json");
    }

    public static Path fileFor(String name) {
        return root().resolve(ConfigProfiles.sanitize(name) + ".json");
    }

    public static synchronized List<String> names() {
        ensureDir();
        Index idx = readIndex();
        return idx == null || idx.names == null ? List.of() : List.copyOf(idx.names);
    }

    public static synchronized boolean save(String name, CustomDropsConfig config) {
        if (!ConfigProfiles.validName(name) || names().contains(name)) return false;
        try {
            Files.createDirectories(root());
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Could not create imports directory {}", root(), e);
            return false;
        }
        Path file = fileFor(name);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to write import {}", file, e);
            return false;
        }
        appendName(name);
        return true;
    }

    public static synchronized CustomDropsConfig load(String name) {
        if (!names().contains(name)) return null;
        Path file = fileFor(name);
        if (!Files.exists(file)) return null;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, CustomDropsConfig.class);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to read import {}", file, e);
            return null;
        }
    }

    public static synchronized boolean delete(String name) {
        if (!names().contains(name)) return false;
        try {
            Files.deleteIfExists(fileFor(name));
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Could not delete import {}", fileFor(name), e);
            return false;
        }
        Index idx = readIndex();
        if (idx != null && idx.names != null) {
            idx.names.remove(name);
            writeIndex(idx);
        }
        return true;
    }

    public static synchronized boolean rename(String oldName, String newName) {
        if (!names().contains(oldName) || !ConfigProfiles.validName(newName) || names().contains(newName)) return false;
        try {
            Files.move(fileFor(oldName), fileFor(newName));
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Could not rename import {} to {}", oldName, newName, e);
            return false;
        }
        Index idx = readIndex();
        if (idx == null) {
            idx = new Index();
            idx.names = new ArrayList<>();
        }
        idx.names.replaceAll(n -> n.equals(oldName) ? newName : n);
        writeIndex(idx);
        return true;
    }

    private static void ensureDir() {
        try {
            Files.createDirectories(root());
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Could not create imports directory {}", root(), e);
        }
    }

    private static void appendName(String name) {
        Index idx = readIndex();
        if (idx == null) {
            idx = new Index();
            idx.names = new ArrayList<>();
        }
        if (!idx.names.contains(name)) {
            idx.names.add(name);
        }
        writeIndex(idx);
    }

    private static void writeIndex(Index idx) {
        try (Writer writer = Files.newBufferedWriter(indexFile(), StandardCharsets.UTF_8)) {
            GSON.toJson(idx, writer);
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to write import index {}", indexFile(), e);
        }
    }

    private static Index readIndex() {
        if (!Files.exists(indexFile())) return null;
        try (Reader reader = Files.newBufferedReader(indexFile(), StandardCharsets.UTF_8)) {
            Index idx = GSON.fromJson(reader, Index.class);
            if (idx == null) idx = new Index();
            if (idx.names == null) idx.names = new ArrayList<>();
            return idx;
        } catch (IOException e) {
            CustomDropsMod.LOGGER.error("Failed to read import index {}", indexFile(), e);
            return null;
        }
    }

    private static final class Index {
        List<String> names = new ArrayList<>();
    }
}