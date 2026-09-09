package com.komixkat.customdrops.client.autocomplete;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RegistryScanner {

    private final Map<String, List<String>> entriesByNamespace = new HashMap<>();
    private final List<String> allEntries = new ArrayList<>();
    private final Set<String> entityIds = new HashSet<>();
    private final Set<String> blockIds = new HashSet<>();
    private final Set<String> itemIds = new HashSet<>();
    private final Set<String> enchantmentIds = new HashSet<>();
    private final Set<String> lootTableIds = new HashSet<>();
    private final Map<String, String> tagKinds = new HashMap<>();

    public void scan() {
        entriesByNamespace.clear();
        allEntries.clear();
        entityIds.clear();
        blockIds.clear();
        itemIds.clear();
        enchantmentIds.clear();
        lootTableIds.clear();
        tagKinds.clear();

        scanRegistry(BuiltInRegistries.ENTITY_TYPE, "entity");
        scanRegistry(BuiltInRegistries.BLOCK, "block");
        scanRegistry(BuiltInRegistries.ITEM, "item");
        mergeBundledIds("entity",
            com.komixkat.customdrops.registry.VanillaIdRegistry.entityIds(), entityIds);
        mergeBundledIds("block",
            com.komixkat.customdrops.registry.VanillaIdRegistry.blockIds(), blockIds);
        mergeBundledIds("item",
            com.komixkat.customdrops.registry.VanillaIdRegistry.droppableItemIds(), itemIds);
        scanEnchantments();
        scanTags(BuiltInRegistries.ENTITY_TYPE, "entity");
        scanTags(BuiltInRegistries.BLOCK, "block");
        scanTags(BuiltInRegistries.ITEM, "item");
        scanLootTables();

        logCompleteness();
    }

    /** Adds any bundled game-version ids the live registry did not report, so autocomplete stays complete. */
    private void mergeBundledIds(String category, Set<String> bundled, Set<String> target) {
        for (String id : bundled) {
            if (target.add(id)) {
                allEntries.add(id);
                entriesByNamespace.computeIfAbsent(
                    id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft",
                    k -> new ArrayList<>()).add(id);
            }
        }
    }

    private void logCompleteness() {
        String itemCount = String.valueOf(itemIds.size());
        String blockCount = String.valueOf(blockIds.size());
        String entityCount = String.valueOf(entityIds.size());
        boolean whiteDyeKnown = itemIds.contains("minecraft:white_dye");
        int bundled = com.komixkat.customdrops.registry.VanillaIdRegistry.bundledDroppableCount();
        com.komixkat.customdrops.CustomDropsMod.LOGGER.info(
            "Registry scan complete: {} items (white_dye known: {}, bundled fallback source: {} ids), {} blocks, {} entities.",
            itemCount, whiteDyeKnown, bundled, blockCount, entityCount);
        com.komixkat.customdrops.CustomDropsMod.LOGGER.debug(
            "Registry scan kinds unavailable entries count per namespace: {}", entriesByNamespace.size());
    }

    private void scanLootTables() {
        for (String id : com.komixkat.customdrops.registry.VanillaLootTableRegistry.search("")) {
            allEntries.add(id);
            lootTableIds.add(id);
        }
    }

    private void scanEnchantments() {
        Set<String> scanned = new HashSet<>();
        try {
            net.minecraft.client.multiplayer.ClientPacketListener connection =
                net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .listElementIds()
                    .forEach(key -> scanned.add(key.identifier().toString()));
            }
        } catch (Throwable ignored) {
        }
        if (scanned.isEmpty()) {
            scanned.addAll(VANILLA_ENCHANTMENT_IDS);
        }
        for (String id : scanned) {
            allEntries.add(id);
            enchantmentIds.add(id);
            entriesByNamespace.computeIfAbsent(
                id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft",
                k -> new ArrayList<>()).add(id);
        }
    }

    private static final List<String> VANILLA_ENCHANTMENT_IDS = List.of(
        "minecraft:protection", "minecraft:fire_protection", "minecraft:feather_falling",
        "minecraft:blast_protection", "minecraft:projectile_protection", "minecraft:respiration",
        "minecraft:aqua_affinity", "minecraft:thorns", "minecraft:depth_strider",
        "minecraft:frost_walker", "minecraft:binding_curse", "minecraft:soul_speed",
        "minecraft:swift_sneak", "minecraft:sharpness", "minecraft:smite",
        "minecraft:bane_of_arthropods", "minecraft:knockback", "minecraft:fire_aspect",
        "minecraft:looting", "minecraft:sweeping_edge", "minecraft:power", "minecraft:punch",
        "minecraft:flame", "minecraft:infinity", "minecraft:luck_of_the_sea", "minecraft:lure",
        "minecraft:unbreaking", "minecraft:efficiency", "minecraft:silk_touch", "minecraft:fortune",
        "minecraft:mending", "minecraft:vanishing_curse", "minecraft:riptide", "minecraft:loyalty",
        "minecraft:channeling", "minecraft:multishot", "minecraft:quick_charge", "minecraft:piercing",
        "minecraft:density", "minecraft:breach", "minecraft:wind_burst");

    private <T> void scanTags(net.minecraft.core.Registry<T> registry, String kind) {
        try {
            registry.getTags().forEach(named -> {
                String tagId = "#" + named.key().location().toString();
                allEntries.add(tagId);
                tagKinds.put(tagId, kind);
            });
        } catch (Exception ignored) {
        }
    }

    private <T> void scanRegistry(net.minecraft.core.Registry<T> registry, String category) {
        Set<String> target = switch (category) {
            case "entity" -> entityIds;
            case "block" -> blockIds;
            case "item" -> itemIds;
            case "enchantment" -> enchantmentIds;
            default -> null;
        };
        for (Identifier id : registry.keySet()) {
            String fullId = id.toString();
            allEntries.add(fullId);
            if (target != null) {
                target.add(fullId);
            }
            entriesByNamespace.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>()).add(fullId);
        }
    }

    public boolean isKnown(String kind, String id) {
        if (id == null || id.isBlank()) return false;
        String clean = id.startsWith("#") ? id.substring(1) : id;
        if (clean.indexOf(':') < 0) {
            clean = "minecraft:" + clean;
        }
        Set<String> set = switch (kind) {
            case "entity", "entities", "mob" -> entityIds;
            case "block" -> blockIds;
            case "item" -> itemIds;
            case "enchantment", "enchantments" -> enchantmentIds;
            case "loot", "loot_table", "data" -> lootTableIds;
            default -> null;
        };
        return set != null && set.contains(clean);
    }

    public Map<String, String> tagKinds() {
        return Map.copyOf(tagKinds);
    }

    public List<String> getAllEntries() {
        return List.copyOf(allEntries);
    }

    public List<String> getByNamespace(String namespace) {
        return entriesByNamespace.getOrDefault(namespace, List.of());
    }

    public Map<String, List<String>> getByNamespace() {
        return Map.copyOf(entriesByNamespace);
    }

    public int totalEntries() {
        return allEntries.size();
    }
}
