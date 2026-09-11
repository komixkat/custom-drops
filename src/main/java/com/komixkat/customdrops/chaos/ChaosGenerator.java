package com.komixkat.customdrops.chaos;

import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.config.schema.BlockDropEntry;
import com.komixkat.customdrops.config.schema.ChestLootEntry;
import com.komixkat.customdrops.config.schema.FishingLootEntry;
import com.komixkat.customdrops.config.schema.LootItemEntry;
import com.komixkat.customdrops.config.schema.MobDropEntry;
import com.komixkat.customdrops.registry.VanillaLootTableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ChaosGenerator {

    public static final int MIN_COUNT = 1;
    public static final int MAX_COUNT = 64;

    private static final Set<String> SENSIBLE_EXCLUSIONS = Set.of(
        "air", "cave_air", "void_air", "light", "barrier", "structure_void",
        "command_block", "chain_command_block", "repeating_command_block",
        "jigsaw", "debug_stick", "knowledge_book", "spawner"
    );

    public record Result(CustomDropsConfig config, int items, int sources) {}

    private enum Kind { MOB, BLOCK, CHEST, FISHING }

    private ChaosGenerator() {}

    public static Result generate(boolean mobs, boolean blocks, boolean chests, boolean fishing, boolean sensibleOnly) {
        List<String> items = candidateItemIds(sensibleOnly);
        List<Source> sources = sources(mobs, blocks, chests, fishing);

        Collections.shuffle(items);
        Collections.sort(sources);

        List<List<String>> perSource = new ArrayList<>(sources.size());
        for (int i = 0; i < sources.size(); i++) {
            perSource.add(new ArrayList<>());
        }
        if (!items.isEmpty() && !sources.isEmpty()) {
            int bound = Math.max(sources.size(), items.size());
            for (int i = 0; i < bound; i++) {
                perSource.get(i % sources.size()).add(items.get(i % items.size()));
            }
        }

        CustomDropsConfig config = new CustomDropsConfig();
        for (int i = 0; i < sources.size(); i++) {
            List<LootItemEntry> pool = perSource.get(i).stream()
                .map(id -> new LootItemEntry(id, 1, MIN_COUNT, MAX_COUNT, 1.0f, List.of(), List.of()))
                .toList();
            Source source = sources.get(i);
            switch (source.kind()) {
                case MOB -> config.mobDrops().add(new MobDropEntry(
                    stripPrefix(source.target(), "entities/"), false, true, pool));
                case BLOCK -> config.blockDrops().add(new BlockDropEntry(
                    stripPrefix(source.target(), "blocks/"), false, true, pool));
                case FISHING -> config.fishingLoot().add(new FishingLootEntry(source.target(), true, pool));
                case CHEST -> config.chestLoot().add(new ChestLootEntry(source.target(), true, pool));
            }
        }
        return new Result(config, items.size(), sources.size());
    }

    private static List<String> candidateItemIds(boolean sensibleOnly) {
        Set<String> excluded = new HashSet<>();
        if (sensibleOnly) {
            excluded.addAll(SENSIBLE_EXCLUSIONS);
        }
        List<String> ids = new ArrayList<>();
        for (var itemId : BuiltInRegistries.ITEM.keySet()) {
            String id = itemId.toString();
            if (excluded.contains(shortName(id))) continue;
            ids.add(id);
        }
        return ids;
    }

    private static List<Source> sources(boolean mobs, boolean blocks, boolean chests, boolean fishing) {
        List<Source> sources = new ArrayList<>();
        for (String table : VanillaLootTableRegistry.search("")) {
            if (table.startsWith("entities/")) {
                if (mobs) sources.add(new Source(Kind.MOB, table));
            } else if (table.startsWith("blocks/")) {
                if (blocks) sources.add(new Source(Kind.BLOCK, table));
            } else if (table.startsWith("gameplay/fishing")) {
                if (fishing) sources.add(new Source(Kind.FISHING, table));
            } else if (chests) {
                sources.add(new Source(Kind.CHEST, table));
            }
        }
        return sources;
    }

    private record Source(Kind kind, String target) implements Comparable<Source> {
        @Override
        public int compareTo(Source other) {
            return this.target.compareTo(other.target);
        }
    }

    private static String stripPrefix(String id, String prefix) {
        return id.startsWith(prefix) ? id.substring(prefix.length()) : id;
    }

    private static String shortName(String id) {
        int idx = id.lastIndexOf(':');
        return idx < 0 ? id : id.substring(idx + 1);
    }
}