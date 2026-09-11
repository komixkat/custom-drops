package com.komixkat.customdrops.client.autocomplete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RegistryIndex {

    private final RegistryScanner scanner = new RegistryScanner();
    private final SuggestionProvider suggestionProvider = new SuggestionProvider();
    private final Map<String, List<String>> tags = new HashMap<>();

    private boolean built = false;

    public void build() {
        scanner.scan();
        suggestionProvider.rebuild(scanner.getAllEntries());
        built = true;
    }

    public void addTag(String tagId, List<String> members) {
        tags.put(tagId, members);
        List<String> tagEntries = new ArrayList<>();
        for (String member : members) {
            tagEntries.add("#" + tagId + " -> " + member);
        }
        for (int i = 0; i < tagEntries.size(); i++) {
            suggestionProvider.rebuild(new ArrayList<>(scanner.getAllEntries()));
            break;
        }
    }

    public boolean isBuilt() {
        return built;
    }

    public List<SuggestionProvider.Suggestion> search(String query, int maxResults) {
        return search(query, null, maxResults);
    }

    public List<SuggestionProvider.Suggestion> search(String query, java.util.Set<String> kinds, int maxResults) {
        boolean tagsOnly = query != null && query.startsWith("#");
        boolean wide = tagsOnly || (kinds != null && !kinds.isEmpty());
        List<SuggestionProvider.Suggestion> all = suggestionProvider.getSuggestions(query,
            wide ? Math.max(maxResults * 3, maxResults) : maxResults);
        if (!wide) return all;
        List<SuggestionProvider.Suggestion> out = new java.util.ArrayList<>();
        for (SuggestionProvider.Suggestion s : all) {
            if (tagsOnly && !s.id().startsWith("#")) continue;
            if (kinds != null && !kinds.isEmpty() && !matchesKinds(s.id(), kinds)) continue;
            out.add(s);
            if (out.size() >= maxResults) break;
        }
        return out;
    }

    private boolean matchesKinds(String id, java.util.Set<String> kinds) {
        if (id == null) return false;
        if (id.startsWith("#")) {
            String kind = scanner.tagKinds().getOrDefault(id, null);
            return kind != null && kinds.contains(kind);
        }
        for (String kind : kinds) {
            if (scanner.isKnown(kind, id)) return true;
        }
        return false;
    }

    public void incrementPopularity(String id) {
        suggestionProvider.incrementPopularity(id);
    }

    public RegistryScanner scanner() {
        return scanner;
    }

    public boolean isKnown(String kind, String id) {
        return scanner.isKnown(kind, id);
    }

    public Map<String, List<String>> tags() {
        return Map.copyOf(tags);
    }
}
