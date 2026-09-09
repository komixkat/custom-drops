package com.komixkat.customdrops.client.autocomplete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Trie {

    private final TrieNode root = new TrieNode();

    public static final class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd;
        int entryIndex = -1;
    }

    public void insert(String key, int index) {
        TrieNode current = root;
        for (int i = 0; i < key.length(); i++) {
            current = current.children.computeIfAbsent(key.charAt(i), k -> new TrieNode());
        }
        current.isEnd = true;
        current.entryIndex = index;
    }

    public List<Integer> searchByPrefix(String prefix, int maxResults) {
        List<Integer> results = new ArrayList<>();
        TrieNode current = root;
        for (int i = 0; i < prefix.length(); i++) {
            TrieNode next = current.children.get(prefix.charAt(i));
            if (next == null) return results;
            current = next;
        }
        collect(current, results, maxResults);
        return results;
    }

    private void collect(TrieNode node, List<Integer> results, int max) {
        if (results.size() >= max) return;
        if (node.isEnd) results.add(node.entryIndex);
        for (TrieNode child : node.children.values()) {
            collect(child, results, max);
            if (results.size() >= max) return;
        }
    }

    public void clear() {
        root.children.clear();
    }
}
