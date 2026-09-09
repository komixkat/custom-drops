package com.komixkat.customdrops.client.gui.widget;

import com.komixkat.customdrops.client.autocomplete.SuggestionProvider;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SuggestionPopup {

    private static final int MAX_VISIBLE = 10;
    private static final int ROW_HEIGHT = 14;

    private final List<SuggestionEntry> entries = new ArrayList<>();
    private int scrollOffset = 0;

    public void update(List<SuggestionProvider.Suggestion> suggestions) {
        entries.clear();
        scrollOffset = 0;

        Map<String, List<SuggestionProvider.Suggestion>> grouped = new LinkedHashMap<>();
        for (SuggestionProvider.Suggestion s : suggestions) {
            grouped.computeIfAbsent(s.namespace(), k -> new ArrayList<>()).add(s);
        }

        for (Map.Entry<String, List<SuggestionProvider.Suggestion>> group : grouped.entrySet()) {
            entries.add(new SuggestionEntry(null, group.getKey(), true));
            for (SuggestionProvider.Suggestion s : group.getValue()) {
                entries.add(new SuggestionEntry(s, null, false));
            }
        }
    }

    private Bounds computeBounds(RegistryAutocompleteField field, int guiHeight) {
        int fieldX = field.getX();
        int fieldTop = field.getY();
        int fieldBottom = fieldTop + field.getHeight();
        int fieldWidth = Math.max(field.getWidth(), 40);

        int visibleCount = Math.min(MAX_VISIBLE, entries.size());
        if (visibleCount == 0) return null;

        int popupHeight = visibleCount * ROW_HEIGHT;
        int clampBottom = Math.min(field.popupClampBottom(), Math.max(field.popupClampTop(), guiHeight - 4));

        if (fieldBottom + popupHeight <= clampBottom) {
            return new Bounds(fieldX, fieldBottom, popupHeight, fieldBottom + popupHeight, fieldWidth, false);
        }
        int aboveTop = fieldTop - popupHeight;
        if (aboveTop >= field.popupClampTop()) {
            return new Bounds(fieldX, aboveTop, popupHeight, fieldTop, fieldWidth, true);
        }
        int top = Math.max(field.popupClampTop(), fieldBottom);
        int clampedHeight = Math.max(ROW_HEIGHT, Math.min(popupHeight, clampBottom - top));
        return new Bounds(fieldX, top, clampedHeight, top + clampedHeight, fieldWidth, false);
    }

    public void render(GuiGraphicsExtractor guiGraphics, RegistryAutocompleteField field, int mouseX, int mouseY, int selectedIndex) {
        Bounds b = computeBounds(field, guiGraphics.guiHeight());
        if (b == null) return;
        int visibleRows = Math.max(1, (b.bottom - b.top) / ROW_HEIGHT);

        guiGraphics.fill(b.x - 1, b.top - 1, b.x + b.width + 1, b.bottom + 1, 0xFF101010);
        guiGraphics.fill(b.x, b.top, b.x + b.width, b.bottom, 0xFF202020);

        int firstVisible = b.above ? Math.max(0, entries.size() - visibleRows) : scrollOffset;
        int rowY = b.top;
        for (int i = firstVisible; i < entries.size() && rowY + ROW_HEIGHT <= b.bottom; i++) {
            SuggestionEntry entry = entries.get(i);
            if (i == selectedIndex) {
                guiGraphics.fill(b.x, rowY, b.x + b.width, rowY + ROW_HEIGHT, 0xFF3050A0);
            }
            if (entry.isHeader) {
                guiGraphics.text(net.minecraft.client.Minecraft.getInstance().font, entry.namespace, b.x + 3, rowY + 3, 0xFF808080, false);
            } else {
                guiGraphics.text(net.minecraft.client.Minecraft.getInstance().font, entry.suggestion.id(), b.x + 3, rowY + 3, 0xFFE0E0E0, false);
            }
            rowY += ROW_HEIGHT;
        }
    }

    public int size() {
        return entries.size();
    }

    public SuggestionProvider.Suggestion get(int index) {
        if (index < 0 || index >= entries.size()) return null;
        SuggestionEntry entry = entries.get(index);
        return entry.suggestion;
    }

    public boolean scroll(RegistryAutocompleteField field, double x, double y, double verticalAmount, int guiHeight) {
        Bounds b = computeBounds(field, guiHeight);
        if (b == null || b.above) return false;
        if (x < b.x || x > b.x + b.width || y < b.top || y > b.bottom) return false;
        int visibleRows = Math.max(1, (b.bottom - b.top) / ROW_HEIGHT);
        int maxOffset = Math.max(0, entries.size() - visibleRows);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset + (verticalAmount > 0 ? -1 : 1)));
        return true;
    }

    public int indexAt(RegistryAutocompleteField field, double x, double y) {
        int guiHeight = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
        Bounds b = computeBounds(field, guiHeight);
        if (b == null) return -1;
        if (x < b.x || x > b.x + b.width || y < b.top || y > b.bottom) return -1;
        int visibleRows = Math.max(1, (b.bottom - b.top) / ROW_HEIGHT);
        int firstVisible = b.above ? Math.max(0, entries.size() - visibleRows) : scrollOffset;
        int local = (int) ((y - b.top) / ROW_HEIGHT);
        int idx = firstVisible + local;
        if (idx < 0 || idx >= entries.size()) return -1;
        return idx;
    }

    private record Bounds(int x, int top, int height, int bottom, int width, boolean above) {
        Bounds(int x, int top, int height, int bottom, int width) {
            this(x, top, height, bottom, width, false);
        }
    }

    private record SuggestionEntry(SuggestionProvider.Suggestion suggestion, String namespace, boolean isHeader) {}
}