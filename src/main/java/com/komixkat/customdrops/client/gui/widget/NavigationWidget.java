package com.komixkat.customdrops.client.gui.widget;

import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.UiSfx;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class NavigationWidget {

    /** Serializable view state used by {@link #snapshotState()} / {@link #restoreState(NavState)}. */
    public static final class NavState {
        final Set<String> expanded = new LinkedHashSet<>();
        double scrollFraction = 0.0;
    }

    private final net.minecraft.client.gui.Font font =
        net.minecraft.client.Minecraft.getInstance().font;

    private int x;
    private int y;
    private int width;
    private int height;

    private final List<Node> roots = new ArrayList<>();
    private final Map<String, Node> rootByName = new LinkedHashMap<>();
    private String selectedLabel = null;
    private int scrollOffset = 0;
    private boolean thumbDragging = false;
    private int thumbGrabOffset = 0;
    private String filter = "";

    private static final int CATEGORY_HEIGHT = 22;
    private static final int ENTRY_HEIGHT = 18;
    private static final int INDENT = 12;
    private static final int SCROLLBAR_WIDTH = 8;

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
        clampScroll();
    }

    /**
     * Restricts the visible tree to entries whose label contains the given text
     * (case-insensitive). Empty/null clears the filter. Category headers that no
     * longer contain a match are hidden as well, so every rendered row is hit-testable.
     */
    public void setFilter(String filter) {
        this.filter = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        clampScroll();
    }

    public String getFilter() {
        return filter;
    }

    public void addCategory(String name) {
        addCategory(name, -1);
    }

    public void addCategory(String name, int count) {
        ensureRoot(name).expanded = true;
        ensureRoot(name).count = count;
    }

    public void markEntryWarn(String label, boolean warn) {
        markWarnRecursive(roots, label, warn);
    }

    private boolean markWarnRecursive(List<Node> nodes, String label, boolean warn) {
        for (Node node : nodes) {
            if (node.isLeaf()) {
                if (node.label.equals(label)) {
                    node.warn = warn;
                    return true;
                }
            } else if (markWarnRecursive(node.children, label, warn)) {
                return true;
            }
        }
        return false;
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
        if (segments == null || segments.isEmpty()) return;
        Node parent = null;
        for (String segment : segments) {
            parent = (parent == null) ? ensureRoot(segment) : ensureChild(parent, segment);
            parent.expanded = true;
        }
        Node leaf = new Node(label, onClick);
        if (parent != null) {
            long dup = parent.children.stream().filter(c -> c.label.equals(label)).count();
            if (dup > 0) {
                leaf = new Node(label + " (" + (dup + 1) + ")", onClick);
            }
            parent.children.add(leaf);
        }
    }

    public void clear() {
        roots.clear();
        rootByName.clear();
        selectedLabel = null;
        scrollOffset = 0;
    }

    /** Captures which categories are expanded plus the current scroll fraction. */
    public NavState snapshotState() {
        NavState state = new NavState();
        collectExpanded(roots, "", state.expanded);
        int maxScroll = maxScroll();
        state.scrollFraction = maxScroll > 0 ? (double) effectiveScroll() / maxScroll : 0.0;
        return state;
    }

    /** Restores expansion and proportional scroll after the tree has been rebuilt. */
    public void restoreState(NavState state) {
        if (state == null) return;
        applyExpanded(roots, "", state.expanded);
        double fraction = state.scrollFraction;
        if (!Double.isFinite(fraction)) {
            fraction = 0.0;
        }
        fraction = Math.max(0.0, Math.min(1.0, fraction));
        int maxScroll = maxScroll();
        scrollOffset = (int) Math.round(fraction * maxScroll);
        clampScroll();
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
        return triggerEntryRecursive(roots, label, silent);
    }

    private boolean triggerEntryRecursive(List<Node> nodes, String label, boolean silent) {
        for (Node node : nodes) {
            if (node.isLeaf()) {
                if (node.label.equals(label)) {
                    runEntry(node, label, silent);
                    return true;
                }
            } else if (triggerEntryRecursive(node.children, label, silent)) {
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
        guiGraphics.fill(x, y, x + width, y + height, 0xFF181818);
        int currentY = y - effectiveScroll();
        guiGraphics.enableScissor(x, y, x + width, y + height);
        renderNodes(guiGraphics, mouseX, mouseY, roots, currentY, 0);
        guiGraphics.disableScissor();

        int[] thumb = thumbRect();
        if (thumb != null) {
            guiGraphics.fill(x + width - SCROLLBAR_WIDTH, thumb[0], x + width, thumb[0] + thumb[1], Ui.SCROLLBAR);
        }
    }

    private int renderNodes(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, List<Node> nodes,
                             int currentY, int depth) {
        for (Node node : nodes) {
            if (!isVisible(node)) {
                continue;
            }
            if (node.isLeaf()) {
                if (currentY < y + height && currentY + ENTRY_HEIGHT > y) {
                    renderEntry(guiGraphics, mouseX, mouseY, node, currentY, depth);
                }
                currentY += ENTRY_HEIGHT;
            } else {
                if (currentY < y + height && currentY + CATEGORY_HEIGHT > y) {
                    renderCategory(guiGraphics, mouseX, mouseY, node, currentY, depth);
                }
                currentY += CATEGORY_HEIGHT;
                if (node.expanded) {
                    currentY = renderNodes(guiGraphics, mouseX, mouseY, node.children, currentY, depth + 1);
                }
            }
        }
        return currentY;
    }

    private void renderCategory(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                Node node, int currentY, int depth) {
        guiGraphics.fill(x, currentY, x + width, currentY + CATEGORY_HEIGHT, 0xFF252525);
        int textX = x + 6 + depth * INDENT;
        String countSuffix = node.count >= 0 ? " (" + node.count + ")" : "";
        if (countSuffix.isEmpty() && !node.isLeaf()) {
            int n = visibleLeafCount(node);
            countSuffix = n > 0 ? " (" + n + ")" : "";
        }
        int countW = countSuffix.isEmpty() ? 0 : font.width(" " + countSuffix);
        String clipped = clip(textX, node.label, 14 + countW);
        guiGraphics.text(font, clipped, textX, currentY + 7, 0xFFAAAAAA, false);
        if (!countSuffix.isEmpty() && !clipped.endsWith("\u2026")) {
            guiGraphics.text(font, countSuffix, textX + font.width(clipped) + 2, currentY + 7, 0xFF707070, false);
        }
        guiGraphics.text(font, node.expanded ? "-" : "+", x + width - 15, currentY + 7, 0xFF888888, false);
    }

    private void renderEntry(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                             Node node, int currentY, int depth) {
        boolean selected = node.label.equals(selectedLabel);
        boolean hovered = mouseX >= x && mouseX < x + width
            && mouseY >= currentY && mouseY < currentY + ENTRY_HEIGHT;

        if (selected) {
            guiGraphics.fill(x, currentY, x + width, currentY + ENTRY_HEIGHT, 0xFF3050A0);
        } else if (hovered) {
            guiGraphics.fill(x, currentY, x + width, currentY + ENTRY_HEIGHT, 0xFF303030);
        }

        if (node.warn) {
            guiGraphics.text(font, "!", x + 4, currentY + 5, 0xFFFF5533, false);
        }

        int textX = x + 6 + depth * INDENT;
        String clipped = clip(textX, node.label, node.warn ? 0 : 0);
        guiGraphics.text(font, clipped, textX, currentY + 5, selected ? 0xFFFFFFFF : 0xFFD0D0D0, false);
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

        if (tryThumbClick(mouseX, mouseY)) return true;

        int currentY = y - effectiveScroll();
        if (clickNodes(mouseX, mouseY, roots, currentY, 0) < 0) {
            return true;
        }
        com.komixkat.customdrops.CustomDropsMod.LOGGER.debug(
            "NavigationWidget click miss at ({}, {}) scrollOffset={} height={}",
            mouseX, mouseY, effectiveScroll(), height);
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!thumbDragging) return false;
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            thumbDragging = false;
            return false;
        }
        int barH = Math.max(16, (int) ((float) height / scrollableHeight() * height));
        int usable = Math.max(1, height - barH);
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
        int maxScroll = maxScroll();
        if (maxScroll <= 0) return false;
        boolean inTrack = mouseX >= x + width - SCROLLBAR_WIDTH && mouseX <= x + width
            && mouseY >= y && mouseY <= y + height;
        if (!inTrack) return false;
        int[] thumb = thumbRect();
        if (thumb != null && mouseY >= thumb[0] && mouseY <= thumb[0] + thumb[1]) {
            thumbDragging = true;
            thumbGrabOffset = (int) mouseY - thumb[0];
        } else {
            int barH = Math.max(16, (int) ((float) height / scrollableHeight() * height));
            int usable = Math.max(1, height - barH);
            float frac = (float) (mouseY - y - barH / 2.0f) / usable;
            scrollOffset = Math.max(0, Math.min(maxScroll, Math.round(frac * maxScroll)));
            thumbDragging = true;
            thumbGrabOffset = barH / 2;
        }
        return true;
    }

    private int[] thumbRect() {
        if (scrollableHeight() <= height) return null;
        int maxScroll = maxScroll();
        int barH = Math.max(16, (int) ((float) height / scrollableHeight() * height));
        int barY = y + (int) ((float) effectiveScroll() / maxScroll * (height - barH));
        return new int[] { barY, barH };
    }

    private int clickNodes(double mouseX, double mouseY, List<Node> nodes, int currentY, int depth) {
        for (Node node : nodes) {
            if (!isVisible(node)) {
                continue;
            }
            if (node.isLeaf()) {
                if (mouseY >= currentY && mouseY < currentY + ENTRY_HEIGHT) {
                    runEntry(node, node.label, false);
                    return -1;
                }
                currentY += ENTRY_HEIGHT;
            } else {
                if (mouseY >= currentY && mouseY < currentY + CATEGORY_HEIGHT) {
                    node.expanded = !node.expanded;
                    UiSfx.click();
                    return -1;
                }
                currentY += CATEGORY_HEIGHT;
                if (node.expanded) {
                    int consumed = clickNodes(mouseX, mouseY, node.children, currentY, depth + 1);
                    if (consumed < 0) return -1;
                    currentY = consumed;
                }
            }
        }
        return currentY;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return false;
        int maxScroll = maxScroll();
        int step = Math.max(ENTRY_HEIGHT * 3, height / 3);
        scrollOffset = Math.max(0, Math.min(maxScroll, effectiveScroll() - (int) (verticalAmount * step)));
        return true;
    }

    private int maxScroll() {
        return Math.max(0, scrollableHeight() - height);
    }

    private int effectiveScroll() {
        return Math.max(0, Math.min(maxScroll(), scrollOffset));
    }

    private void clampScroll() {
        scrollOffset = effectiveScroll();
    }

    private int scrollableHeight() {
        return totalHeight(roots);
    }

    private int totalHeight(List<Node> nodes) {
        int total = 0;
        for (Node node : nodes) {
            if (!isVisible(node)) {
                continue;
            }
            total += node.isLeaf() ? ENTRY_HEIGHT : CATEGORY_HEIGHT;
            if (!node.isLeaf() && node.expanded) {
                total += totalHeight(node.children);
            }
        }
        return total;
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
        return node.label.toLowerCase(Locale.ROOT).contains(needle);
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

    private void scrollToEntry(String label) {
        int[] pos = findEntryY(roots, label, 0);
        if (pos == null) return;
        int targetTop = pos[0];
        int maxScroll = maxScroll();
        scrollOffset = Math.max(0, Math.min(maxScroll, targetTop - 4));
    }

    private int[] findEntryY(List<Node> nodes, String label, int currentY) {
        for (Node node : nodes) {
            if (!isVisible(node)) {
                continue;
            }
            if (node.isLeaf()) {
                if (node.label.equals(label)) {
                    return new int[]{currentY};
                }
                currentY += ENTRY_HEIGHT;
            } else {
                currentY += CATEGORY_HEIGHT;
                if (node.expanded) {
                    int[] found = findEntryY(node.children, label, currentY);
                    if (found != null) return found;
                    currentY = yOfChildren(node.children, currentY);
                }
            }
        }
        return null;
    }

    private int yOfChildren(List<Node> nodes, int currentY) {
        for (Node node : nodes) {
            if (!isVisible(node)) {
                continue;
            }
            currentY += node.isLeaf() ? ENTRY_HEIGHT : CATEGORY_HEIGHT;
            if (!node.isLeaf() && node.expanded) {
                currentY = yOfChildren(node.children, currentY);
            }
        }
        return currentY;
    }

    private Node ensureRoot(String name) {
        Node root = rootByName.get(name);
        if (root == null) {
            root = new Node(name, null);
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
        Node node = new Node(name, null);
        parent.children.add(node);
        return node;
    }

    private static final class Node {
        final String label;
        final Runnable onClick;
        boolean expanded = false;
        boolean warn = false;
        int count = -1;
        final List<Node> children = new ArrayList<>();

        Node(String label, Runnable onClick) {
            this.label = label;
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