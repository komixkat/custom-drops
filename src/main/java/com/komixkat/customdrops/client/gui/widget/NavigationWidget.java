package com.komixkat.customdrops.client.gui.widget;

import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.UiSfx;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Left-hand navigation list used by the split-pane screens.
 *
 * Layout is derived from a single flat {@link Row} list that is rebuilt on every
 * structural change ({@link #relayout()}). Both rendering and hit-testing consume
 * exactly that list, so a click can only ever target a row that was drawn this frame.
 * Scroll is clamped eagerly on every change and anchored to the first visible row, so
 * expanding/collapsing categories never shifts the list out from under the cursor.
 */
public final class NavigationWidget {

    /** Serializable view state used by {@link #snapshotState()} / {@link #restoreState(NavState)}. */
    public static final class NavState {
        final Set<String> expanded = new LinkedHashSet<>();
        double scrollFraction = 0.0;
    }

    private static final int CATEGORY_HEIGHT = 22;
    private static final int ENTRY_HEIGHT = 18;
    private static final int INDENT = 12;
    private static final int SCROLLBAR_WIDTH = 8;

    private final net.minecraft.client.gui.Font font =
        net.minecraft.client.Minecraft.getInstance().font;

    private int x;
    private int y;
    private int width;
    private int height;

    private final List<Node> roots = new ArrayList<>();
    private final Map<String, Node> rootByName = new LinkedHashMap<>();
    private final List<Row> rows = new ArrayList<>();
    private String selectedLabel = null;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean thumbDragging = false;
    private int thumbGrabOffset = 0;
    private String filter = "";
    private boolean needsLayout = true;

    /** Category open/closed state kept across screen re-inits (Gui.setScreen re-runs init). */
    private static final Map<String, Boolean> expansionMemory = new HashMap<>();

    public NavigationWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        relayout();
    }

    /**
     * Restricts the visible tree to entries whose label contains the given text
     * (case-insensitive). Empty/null clears the filter. Category headers that no
     * longer contain a match are hidden as well, so every rendered row is hit-testable.
     */
    public void setFilter(String filter) {
        this.filter = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        relayout();
    }

    public String getFilter() {
        return filter;
    }

    public void addCategory(String name) {
        addCategory(name, -1);
    }

    public void addCategory(String name, int count) {
        ensureRoot(name).count = count;
        needsLayout = true;
        applyExpansionMemory();
    }

    public void markEntryWarn(String label, boolean warn) {
        markAllLeaves(roots, label, warn);
    }

    /**
     * Marks the warn flag on the exact row earlier registered by
     * {@link #addEntryPath(List, String, Runnable)} with the same segments and label,
     * preserving deduplicated " (N)" suffixes.
     */
    public void markEntryWarn(List<String> segments, String label, boolean warn) {
        Node leaf = resolveLeaf(segments, label);
        if (leaf != null) {
            leaf.warn = warn;
        }
    }

    private Node resolveLeaf(List<String> segments, String label) {
        if (segments == null || segments.isEmpty()) return null;
        Node parent = null;
        for (String segment : segments) {
            parent = (parent == null) ? rootByName.get(segment) : findChild(parent, segment);
            if (parent == null) return null;
        }
        long dup = parent.children.stream().filter(c -> c.isLeaf() && c.label.equals(label)).count();
        String target = dup > 0 ? label + " (" + (dup + 1) + ")" : label;
        for (Node child : parent.children) {
            if (child.isLeaf() && child.label.equals(target)) {
                return child;
            }
        }
        return null;
    }

    private void markAllLeaves(List<Node> nodes, String label, boolean warn) {
        for (Node node : nodes) {
            if (node.isLeaf()) {
                if (node.label.equals(label)) {
                    node.warn = warn;
                }
            } else {
                markAllLeaves(node.children, label, warn);
            }
        }
    }

    public void addEntry(String category, String label, Runnable onClick) {
        List<String> segments = new ArrayList<>();
        for (String part : category.split("/")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) segments.add(trimmed);
        }
        addEntryPath(segments, label, onClick);
    }

    public void addEntryPath(List<String> segments, String label, Runnable onClick) {
        addEntryPath(segments, label, label, onClick);
    }

    public void addEntryPath(List<String> segments, String label, String searchText, Runnable onClick) {
        if (segments == null || segments.isEmpty()) return;
        Node parent = null;
        boolean rootSegment = true;
        for (String segment : segments) {
            parent = (parent == null) ? ensureRoot(segment) : ensureChild(parent, segment);
            if (!rootSegment) {
                parent.expanded = true;
            }
            rootSegment = false;
        }
        String searchNeeded = searchText == null ? label : searchText;
        Node leaf = new Node(label, searchNeeded, onClick);
        if (parent != null) {
            long dup = parent.children.stream().filter(c -> c.label.equals(label)).count();
            if (dup > 0) {
                leaf = new Node(label + " (" + (dup + 1) + ")", searchNeeded, onClick);
            }
            parent.children.add(leaf);
        }
        needsLayout = true;
        applyExpansionMemory();
    }

    private void applyExpansionMemory() {
        for (Node root : roots) {
            applyExpansionMemory(root, root.label);
        }
    }

    private void applyExpansionMemory(Node node, String path) {
        if (!node.isLeaf()) {
            Boolean remembered = expansionMemory.get(path);
            if (remembered != null) {
                node.expanded = remembered;
            }
            for (Node child : node.children) {
                applyExpansionMemory(child, path + "/" + child.label);
            }
        }
    }

    private void rememberExpansion(Node node) {
        String path = findPath(roots, node, "");
        if (path != null && !node.isLeaf()) {
            expansionMemory.put(path, node.expanded);
        }
    }

    private String findPath(List<Node> nodes, Node target, String prefix) {
        for (Node node : nodes) {
            String path = prefix.isEmpty() ? node.label : prefix + "/" + node.label;
            if (node == target) return path;
            if (!node.isLeaf()) {
                String found = findPath(node.children, target, path);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void persistExpansionToMemory(List<Node> nodes, String prefix) {
        for (Node node : nodes) {
            String path = prefix.isEmpty() ? node.label : prefix + "/" + node.label;
            if (!node.isLeaf()) {
                expansionMemory.put(path, node.expanded);
                persistExpansionToMemory(node.children, path);
            }
        }
    }

    public void clear() {
        roots.clear();
        rootByName.clear();
        rows.clear();
        selectedLabel = null;
        scrollOffset = 0;
        maxScroll = 0;
        thumbDragging = false;
        needsLayout = true;
    }

    /** Rebuilds the flat row list right away if any structural change left it stale. */
    private void ensureLayout() {
        if (needsLayout) {
            relayout();
        }
    }

    /** Captures which categories are expanded plus the current scroll fraction. */
    public NavState snapshotState() {
        ensureLayout();
        persistExpansionToMemory(roots, "");
        NavState state = new NavState();
        collectExpanded(roots, "", state.expanded);
        state.scrollFraction = maxScroll > 0 ? (double) scrollOffset / maxScroll : 0.0;
        return state;
    }

    /** Restores expansion and proportional scroll after the tree has been rebuilt. */
    public void restoreState(NavState state) {
        if (state == null) return;
        applyExpanded(roots, "", state.expanded);
        relayout();
        double fraction = state.scrollFraction;
        if (!Double.isFinite(fraction)) {
            fraction = 0.0;
        }
        fraction = Math.max(0.0, Math.min(1.0, fraction));
        scrollOffset = (int) Math.round(fraction * maxScroll);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }

    private void collectExpanded(List<Node> nodes, String prefix, Set<String> out) {
        for (Node node : nodes) {
            if (!node.isLeaf()) {
                String path = prefix.isEmpty() ? node.label : prefix + "/" + node.label;
                if (node.expanded) {
                    out.add(path);
                }
                collectExpanded(node.children, path, out);
            }
        }
    }

    private void applyExpanded(List<Node> nodes, String prefix, Set<String> expanded) {
        for (Node node : nodes) {
            if (!node.isLeaf()) {
                String path = prefix.isEmpty() ? node.label : prefix + "/" + node.label;
                node.expanded = expanded.contains(path);
                applyExpanded(node.children, path, expanded);
            }
        }
    }

    public void setSelected(String label) {
        this.selectedLabel = label;
    }

    public boolean triggerEntry(String label) {
        return triggerEntry(label, false);
    }

    public boolean triggerEntry(String label, boolean silent) {
        Node leaf = findLeaf(roots, label);
        if (leaf == null) return false;
        expandAncestors(roots, leaf);
        relayout();
        runEntry(leaf, label, silent);
        return true;
    }

    private Node findLeaf(List<Node> nodes, String label) {
        for (Node node : nodes) {
            if (node.isLeaf()) {
                if (node.label.equals(label)) return node;
            } else {
                Node found = findLeaf(node.children, label);
                if (found != null) return found;
            }
        }
        return null;
    }

    private boolean expandAncestors(List<Node> nodes, Node target) {
        for (Node node : nodes) {
            if (node == target) return true;
            if (!node.isLeaf() && expandAncestors(node.children, target)) {
                node.expanded = true;
                return true;
            }
        }
        return false;
    }

    private void runEntry(Node node, String label, boolean silent) {
        if (!silent) {
            UiSfx.click();
        }
        node.onClick.run();
        selectedLabel = label;
        scrollToEntry(label);
    }

    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        ensureLayout();
        guiGraphics.fill(x, y, x + width, y + height, 0xFF181818);
        guiGraphics.enableScissor(x, y, x + width, y + height);
        for (Row row : rows) {
            int rowY = y + row.y - scrollOffset;
            if (rowY + row.height <= y || rowY >= y + height) {
                continue;
            }
            if (row.node.isLeaf()) {
                renderEntry(guiGraphics, mouseX, mouseY, row, rowY);
            } else {
                renderCategory(guiGraphics, row, rowY);
            }
        }
        guiGraphics.disableScissor();

        int[] thumb = thumbRect();
        if (thumb != null) {
            guiGraphics.fill(x + width - SCROLLBAR_WIDTH, thumb[0], x + width, thumb[0] + thumb[1], Ui.SCROLLBAR);
        }
    }

    private void renderCategory(GuiGraphicsExtractor guiGraphics, Row row, int rowY) {
        Node node = row.node;
        int depth = row.depth;
        guiGraphics.fill(x, rowY, x + width, rowY + CATEGORY_HEIGHT, 0xFF252525);
        int textX = x + 6 + depth * INDENT;
        String countSuffix = node.count >= 0 ? " (" + node.count + ")" : "";
        if (countSuffix.isEmpty() && !node.isLeaf()) {
            int n = visibleLeafCount(node);
            countSuffix = n > 0 ? " (" + n + ")" : "";
        }
        int countW = countSuffix.isEmpty() ? 0 : font.width(" " + countSuffix);
        String clipped = clip(textX, node.label, 14 + countW);
        guiGraphics.text(font, clipped, textX, rowY + 7, 0xFFAAAAAA, false);
        if (!countSuffix.isEmpty() && !clipped.endsWith("\u2026")) {
            guiGraphics.text(font, countSuffix, textX + font.width(clipped) + 2, rowY + 7, 0xFF707070, false);
        }
        guiGraphics.text(font, node.expanded ? "-" : "+", x + width - 15, rowY + 7, 0xFF888888, false);
    }

    private void renderEntry(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, Row row, int rowY) {
        Node node = row.node;
        int depth = row.depth;
        boolean selected = node.label.equals(selectedLabel);
        boolean hovered = mouseX >= x && mouseX < x + width
            && mouseY >= rowY && mouseY < rowY + ENTRY_HEIGHT;

        if (selected) {
            guiGraphics.fill(x, rowY, x + width, rowY + ENTRY_HEIGHT, 0xFF3050A0);
        } else if (hovered) {
            guiGraphics.fill(x, rowY, x + width, rowY + ENTRY_HEIGHT, 0xFF303030);
        }

        if (node.warn) {
            guiGraphics.text(font, "!", x + 4, rowY + 5, 0xFFFF5533, false);
        }

        int textX = x + 6 + depth * INDENT;
        String clipped = clip(textX, node.label, 0);
        guiGraphics.text(font, clipped, textX, rowY + 5, selected ? 0xFFFFFFFF : 0xFFD0D0D0, false);
    }

    private String clip(int textX, String label, int reservePx) {
        int maxText = Math.max(8, width - (textX - x + 4) - 4 - reservePx);
        if (font.width(label) <= maxText) {
            return label;
        }
        return font.plainSubstrByWidth(label, Math.max(1, maxText - 6)) + "\u2026";
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return false;
        if (button != 0) return false;
        ensureLayout();

        if (tryThumbClick(mouseX, mouseY)) return true;

        for (Row row : rows) {
            int rowY = y + row.y - scrollOffset;
            if (mouseY >= rowY && mouseY < rowY + row.height) {
                if (row.node.isLeaf()) {
                    runEntry(row.node, row.node.label, false);
                } else {
                    row.node.expanded = !row.node.expanded;
                    rememberExpansion(row.node);
                    UiSfx.click();
                    relayout();
                }
                return true;
            }
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!thumbDragging) return false;
        ensureLayout();
        if (maxScroll <= 0) {
            thumbDragging = false;
            return false;
        }
        int barHeight = thumbHeight();
        int usable = Math.max(1, height - barHeight);
        float frac = (float) (mouseY - y - thumbGrabOffset) / usable;
        scrollOffset = Math.max(0, Math.min(maxScroll, Math.round(frac * maxScroll)));
        return true;
    }

    public void mouseReleased(double mouseX, double mouseY) {
        thumbDragging = false;
    }

    public boolean isThumbDragging() {
        return thumbDragging;
    }

    private boolean tryThumbClick(double mouseX, double mouseY) {
        if (maxScroll <= 0) return false;
        boolean inTrack = mouseX >= x + width - SCROLLBAR_WIDTH && mouseX <= x + width
            && mouseY >= y && mouseY <= y + height;
        if (!inTrack) return false;
        int[] thumb = thumbRect();
        if (thumb != null && mouseY >= thumb[0] && mouseY <= thumb[0] + thumb[1]) {
            thumbDragging = true;
            thumbGrabOffset = (int) mouseY - thumb[0];
        } else {
            int barHeight = thumbHeight();
            int usable = Math.max(1, height - barHeight);
            float frac = (float) (mouseY - y - barHeight / 2.0f) / usable;
            scrollOffset = Math.max(0, Math.min(maxScroll, Math.round(frac * maxScroll)));
            thumbDragging = true;
            thumbGrabOffset = barHeight / 2;
        }
        return true;
    }

    private int[] thumbRect() {
        if (maxScroll <= 0) return null;
        int barHeight = thumbHeight();
        int barY = y + (int) ((float) scrollOffset / maxScroll * (height - barHeight));
        return new int[] { barY, barHeight };
    }

    private int thumbHeight() {
        int content = scrollableHeight();
        return Math.max(16, (int) ((float) height / Math.max(1, content) * height));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return false;
        ensureLayout();
        int step = Math.max(ENTRY_HEIGHT * 3, height / 3);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * step)));
        return true;
    }

    private int scrollableHeight() {
        if (!rows.isEmpty()) {
            Row last = rows.get(rows.size() - 1);
            return last.y + last.height;
        }
        return 0;
    }

    /**
     * Rebuilds the flat visible-row list and eagerly rescales scroll so the first
     * visible row stays where it was. Called after every structural change.
     */
    private void relayout() {
        int previousMax = maxScroll;
        List<String> anchoredPath = null;
        int anchoredOffset = 0;
        for (Row row : rows) {
            if (row.y + row.height > scrollOffset) {
                anchoredPath = row.path;
                anchoredOffset = Math.max(0, scrollOffset - row.y);
                break;
            }
        }

        rows.clear();
        buildRows(roots, 0, 0, List.of());

        int newMax = Math.max(0, scrollableHeight() - height);
        if (anchoredPath != null) {
            int anchoredY = rowYOf(anchoredPath);
            if (anchoredY >= 0) {
                scrollOffset = anchoredY - anchoredOffset;
            } else {
                scrollOffset = Math.min(scrollOffset, newMax);
            }
        } else if (previousMax > 0) {
            scrollOffset = (int) Math.round(
                (float) Math.min(Math.max(0, scrollOffset), previousMax) / previousMax * newMax);
        } else {
            scrollOffset = 0;
        }
        maxScroll = newMax;
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
        needsLayout = false;
    }

    private int buildRows(List<Node> nodes, int depth, int y, List<String> prefix) {
        for (Node node : nodes) {
            if (!isVisible(node)) {
                continue;
            }
            List<String> path = new ArrayList<>(prefix.size() + 1);
            path.addAll(prefix);
            path.add(node.label);
            if (node.isLeaf()) {
                rows.add(new Row(node, y, ENTRY_HEIGHT, depth, path));
                y += ENTRY_HEIGHT;
            } else {
                rows.add(new Row(node, y, CATEGORY_HEIGHT, depth, path));
                y += CATEGORY_HEIGHT;
                if (node.expanded) {
                    y = buildRows(node.children, depth + 1, y, path);
                }
            }
        }
        return y;
    }

    private int rowYOf(List<String> path) {
        for (Row row : rows) {
            if (row.path.equals(path)) {
                return row.y;
            }
        }
        return -1;
    }

    private void scrollToEntry(String label) {
        ensureLayout();
        for (Row row : rows) {
            if (row.node.isLeaf() && row.node.label.equals(label)) {
                scrollOffset = Math.max(0, Math.min(maxScroll, row.y - 4));
                return;
            }
        }
    }

    /** A node is visible when the filter is empty, or it or any descendant matches it. */
    private boolean isVisible(Node node) {
        if (filter.isEmpty()) {
            return true;
        }
        if (containsMatch(node, filter)) {
            return true;
        }
        if (!node.isLeaf()) {
            for (Node child : node.children) {
                if (isVisible(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsMatch(Node node, String needle) {
        if (node.label.toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        return node.searchText != null
            && !node.searchText.equals(node.label)
            && node.searchText.toLowerCase(Locale.ROOT).contains(needle);
    }

    private int visibleLeafCount(Node node) {
        int total = 0;
        for (Node child : node.children) {
            if (!isVisible(child)) {
                continue;
            }
            total += child.isLeaf() ? 1 : child.leafCount();
        }
        return total;
    }

    private Node ensureRoot(String name) {
        Node root = rootByName.get(name);
        if (root == null) {
            root = new Node(name, name, null);
            rootByName.put(name, root);
            roots.add(root);
        }
        return root;
    }

    private Node ensureChild(Node parent, String name) {
        for (Node child : parent.children) {
            if (!child.isLeaf() && child.label.equals(name)) {
                return child;
            }
        }
        Node node = new Node(name, name, null);
        parent.children.add(node);
        return node;
    }

    private Node findChild(Node parent, String name) {
        for (Node child : parent.children) {
            if (!child.isLeaf() && child.label.equals(name)) {
                return child;
            }
        }
        return null;
    }

    private static final class Node {
        final String label;
        final String searchText;
        final Runnable onClick;
        boolean expanded = false;
        boolean warn = false;
        int count = -1;
        final List<Node> children = new ArrayList<>();

        Node(String label, String searchText, Runnable onClick) {
            this.label = label;
            this.searchText = searchText;
            this.onClick = onClick;
        }

        boolean isLeaf() {
            return onClick != null;
        }

        int leafCount() {
            int total = 0;
            for (Node child : children) {
                total += child.isLeaf() ? 1 : child.leafCount();
            }
            return total;
        }
    }

    private static final class Row {
        final Node node;
        final int y;
        final int height;
        final int depth;
        final List<String> path;

        Row(Node node, int y, int height, int depth, List<String> path) {
            this.node = node;
            this.y = y;
            this.height = height;
            this.depth = depth;
            this.path = path;
        }
    }

    public static List<String> segments(String targetId, boolean isTag) {
        List<String> segments = new ArrayList<>();
        if (isTag) {
            segments.add("#tags");
        }
        String t = targetId == null ? "" : targetId;
        if (t.startsWith("#")) t = t.substring(1);
        int colon = t.indexOf(':');
        if (colon > 0) {
            segments.add(t.substring(0, colon));
            t = t.substring(colon + 1);
        } else if (!t.isEmpty()) {
            segments.add("misc");
        }
        if (t.isEmpty()) {
            segments.add("(untitled)");
        } else {
            for (String part : t.split("/")) {
                if (!part.isEmpty()) segments.add(part);
            }
        }
        return segments;
    }
}