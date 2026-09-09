package com.komixkat.customdrops.client.autocomplete;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SuggestionProvider {

    private final List<String> entries = new ArrayList<>();
    private final Map<String, Integer> popularity = new LinkedHashMap<>();
    private final Trie trie = new Trie();
    private final NGramIndex ngramIndex = new NGramIndex();

    public void rebuild(List<String> allEntries) {
        trie.clear();
        ngramIndex.clear();
        entries.clear();

        for (int i = 0; i < allEntries.size(); i++) {
            entries.add(allEntries.get(i));
            trie.insert(allEntries.get(i).toLowerCase(), i);
            ngramIndex.add(i, allEntries.get(i));
        }
    }

    public void incrementPopularity(String id) {
        popularity.merge(id, 1, Integer::sum);
    }

    public List<Suggestion> getSuggestions(String query, int maxResults) {
        if (query == null || query.isBlank()) return List.of();

        String lower = query.toLowerCase();
        if (lower.endsWith("*")) {
            lower = lower.substring(0, lower.length() - 1).trim();
        }
        if (lower.isEmpty()) return List.of();

        List<Integer> prefixMatches = trie.searchByPrefix(lower, maxResults * 2);
        List<Integer> segmentMatches = segmentSearch(lower, maxResults * 2);
        List<Integer> containsMatches = substringSearch(lower, maxResults * 2);
        List<Integer> fuzzyMatches = ngramIndex.fuzzySearch(lower, maxResults);

        Map<Integer, Double> combined = new LinkedHashMap<>();
        score(combined, prefixMatches, 3, 100.0);
        score(combined, segmentMatches, 2, 95.0);
        score(combined, containsMatches, 1, 85.0);
        score(combined, fuzzyMatches, 0, 40.0);

        List<Map.Entry<Integer, Double>> sorted = combined.entrySet().stream()
            .sorted((a, b) -> {
                int byScore = Double.compare(b.getValue(), a.getValue());
                if (byScore != 0) return byScore;
                return Double.compare(
                    popularity.getOrDefault(entries.get(a.getKey()), 0),
                    popularity.getOrDefault(entries.get(b.getKey()), 0));
            })
            .limit(maxResults)
            .toList();

        List<Suggestion> results = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : sorted) {
            int idx = entry.getKey();
            if (idx >= 0 && idx < entries.size()) {
                String id = entries.get(idx);
                String namespace = extractNamespace(id);
                int pop = popularity.getOrDefault(id, 0);
                results.add(new Suggestion(id, namespace, pop, entry.getValue()));
            }
        }
        return results;
    }

    private void score(Map<Integer, Double> combined, List<Integer> matches, double tierDecay, double base) {
        for (int i = 0; i < matches.size(); i++) {
            int idx = matches.get(i);
            double score = base - (i * 0.05) - tierDecay;
            combined.merge(idx, score, Math::max);
        }
    }

    private List<Integer> segmentSearch(String query, int limit) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < entries.size() && out.size() < limit; i++) {
            String lower = entries.get(i).toLowerCase();
            String segment = lastSegment(lower);
            if (segment.startsWith(query) || segment.contains(query)) {
                out.add(i);
            }
        }
        return out;
    }

    private List<Integer> substringSearch(String query, int limit) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < entries.size() && out.size() < limit; i++) {
            if (entries.get(i).toLowerCase().contains(query)) {
                out.add(i);
            }
        }
        return out;
    }

    private static String lastSegment(String id) {
        int slash = id.lastIndexOf('/');
        int colon = id.indexOf(':');
        String tail = slash >= 0 ? id.substring(slash + 1) : (colon >= 0 ? id.substring(colon + 1) : id);
        if (tail.startsWith("#")) tail = tail.substring(1);
        return tail;
    }

    private static String extractNamespace(String id) {
        int colon = id.indexOf(':');
        return colon > 0 ? id.substring(0, colon) : "minecraft";
    }

    public record Suggestion(String id, String namespace, int popularity, double score) {}
}
