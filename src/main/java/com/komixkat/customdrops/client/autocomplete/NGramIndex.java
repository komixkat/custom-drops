package com.komixkat.customdrops.client.autocomplete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NGramIndex {

    private static final int NGRAM_SIZE = 3;
    private static final int MAX_EDIT_DISTANCE = 2;

    private final Map<String, Set<Integer>> ngramToEntries = new HashMap<>();
    private final Map<Integer, String> entryStrings = new HashMap<>();

    public void add(int index, String value) {
        String lower = value.toLowerCase();
        entryStrings.put(index, lower);
        for (String ngram : generateNGrams(lower)) {
            ngramToEntries.computeIfAbsent(ngram, k -> new HashSet<>()).add(index);
        }
    }

    public List<Integer> fuzzySearch(String query, int maxResults) {
        String lower = query.toLowerCase();
        Set<Integer> candidates = new HashSet<>();

        for (String ngram : generateNGrams(lower)) {
            Set<Integer> matches = ngramToEntries.get(ngram);
            if (matches != null) candidates.addAll(matches);
        }

        List<int[]> scored = new ArrayList<>();
        for (int idx : candidates) {
            String stored = entryStrings.get(idx);
            if (stored == null) continue;
            int distance = editDistance(lower, stored);
            if (distance <= MAX_EDIT_DISTANCE) {
                scored.add(new int[]{idx, distance});
            }
        }

        scored.sort((a, b) -> {
            if (a[1] != b[1]) return Integer.compare(a[1], b[1]);
            return Integer.compare(a[0], b[0]);
        });

        List<Integer> results = new ArrayList<>();
        for (int[] pair : scored) {
            results.add(pair[0]);
            if (results.size() >= maxResults) break;
        }
        return results;
    }

    private static List<String> generateNGrams(String s) {
        List<String> ngrams = new ArrayList<>();
        String padded = "  " + s + " ";
        for (int i = 0; i <= padded.length() - NGRAM_SIZE; i++) {
            ngrams.add(padded.substring(i, i + NGRAM_SIZE));
        }
        return ngrams;
    }

    private static int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    public void clear() {
        ngramToEntries.clear();
        entryStrings.clear();
    }
}
