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
    private final Set<String> addedTagIds = new HashSet<>();

    public void scan() {
        entriesByNamespace.clear();
        allEntries.clear();
        entityIds.clear();
        blockIds.clear();
        itemIds.clear();
        enchantmentIds.clear();
        lootTableIds.clear();
        tagKinds.clear();
        addedTagIds.clear();

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
        scanTags();
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
            "Registry scan complete: {} items (white_dye known: {}, bundled fallback source: {} ids), {} blocks, {} entities, {} tags (sample: {}).",
            itemCount, whiteDyeKnown, bundled, blockCount, entityCount, tagKinds.size(),
            tagKinds.keySet().stream().limit(12).toList());
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

    private void scanTags() {
        scanTagsFromStatic(BuiltInRegistries.ENTITY_TYPE, "entity");
        scanTagsFromStatic(BuiltInRegistries.BLOCK, "block");
        scanTagsFromStatic(BuiltInRegistries.ITEM, "item");
        scanTagsFromLive();
        mergeBundledTags(VANILLA_ENTITY_TAG_IDS, "entity");
        mergeBundledTags(VANILLA_BLOCK_TAG_IDS, "block");
    }

    private <T> void scanTagsFromStatic(net.minecraft.core.Registry<T> registry, String kind) {
        try {
            registry.getTags().forEach(named -> addTag(named.key().location().toString(), kind));
        } catch (Exception ignored) {
        }
    }

    private void scanTagsFromLive() {
        try {
            net.minecraft.client.multiplayer.ClientPacketListener connection =
                net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection == null) return;
            net.minecraft.core.HolderLookup.Provider access = connection.registryAccess();
            addLiveTags(access, net.minecraft.core.registries.Registries.ENTITY_TYPE, "entity");
            addLiveTags(access, net.minecraft.core.registries.Registries.BLOCK, "block");
            addLiveTags(access, net.minecraft.core.registries.Registries.ITEM, "item");
        } catch (Throwable ignored) {
        }
    }

    private <T> void addLiveTags(net.minecraft.core.HolderLookup.Provider access,
                                 net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registryKey,
                                 String kind) {
        access.lookupOrThrow(registryKey).listTags()
            .forEach(named -> addTag(named.key().location().toString(), kind));
    }

    private void mergeBundledTags(List<String> tags, String kind) {
        for (String id : tags) {
            addTag(id, kind);
        }
    }

    private void addTag(String id, String kind) {
        if (id == null || id.indexOf(':') < 0) return;
        String entry = "#" + id;
        if (addedTagIds.add(entry)) {
            allEntries.add(entry);
            tagKinds.put(entry, kind);
        }
    }

    private static final List<String> VANILLA_ENTITY_TAG_IDS = List.of(
        "minecraft:skeletons", "minecraft:zombies", "minecraft:raiders",
        "minecraft:undead", "minecraft:arthropod", "minecraft:aquatic",
        "minecraft:bosses", "minecraft:fall_damage_immune",
        "minecraft:beehive_inhabitors", "minecraft:can_breathe_under_water",
        "minecraft:axolotl_always_hostiles", "minecraft:axolotl_tempt_hostiles",
        "minecraft:powder_snow_walkable_mobs", "minecraft:freeze_immune_entity_types",
        "minecraft:sensitive_to_smite", "minecraft:immediate_respawn_requirements");

    private static final List<String> VANILLA_BLOCK_TAG_IDS = List.of(
        "minecraft:logs", "minecraft:logs_that_burn", "minecraft:planks",
        "minecraft:wool", "minecraft:wool_carpets", "minecraft:leaves",
        "minecraft:saplings", "minecraft:wooden_doors", "minecraft:doors",
        "minecraft:wooden_slabs", "minecraft:slabs", "minecraft:wooden_stairs",
        "minecraft:stairs", "minecraft:wooden_fences", "minecraft:fences",
        "minecraft:fence_gates", "minecraft:stone_bricks", "minecraft:anvil",
        "minecraft:candles", "minecraft:flowers", "minecraft:nylium",
        "minecraft:sand", "minecraft:terracotta", "minecraft:ice",
        "minecraft:snow", "minecraft:replaceable");

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
