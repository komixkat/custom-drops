package com.komixkat.customdrops.client.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public final class EntryListWidget {

    private final net.minecraft.client.gui.Font font =
        net.minecraft.client.Minecraft.getInstance().font;
    private int x;
    private int y;
    private int width;
    private int height;
    private final List<EntryRow> rows = new ArrayList<>();
    private int scrollOffset = 0;
    private int selectedRow = -1;
    private boolean thumbDragging = false;
    private int thumbGrabOffset = 0;
    private static final int ROW_HEIGHT = 24;
    private static final int SCROLL_BAR_WIDTH = 6;

    public EntryListWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setRows(List<EntryRow> newRows) {
        rows.clear();
        rows.addAll(newRows);
        scrollOffset = 0;
        selectedRow = -1;
    }

    private int visibleRowCount() {
        return Math.max(1, height / ROW_HEIGHT);
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleRowCount());
    }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset));
    }

    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        clampScroll();

        int visible = Math.min(visibleRowCount(), rows.size() - scrollOffset);
        for (int i = 0; i < visible; i++) {
            int rowIdx = scrollOffset + i;
            if (rowIdx >= rows.size()) break;

            int rowY = y + i * ROW_HEIGHT;

            EntryRow row = rows.get(rowIdx);
            if (row.isSection()) {
                guiGraphics.fill(x, rowY, x + width, rowY + ROW_HEIGHT, 0xFF2E2E38);
                guiGraphics.text(font, row.label(), x + 4, rowY + 7, 0xFFB0B0C0, false);
                continue;
            }

            if (rowIdx == selectedRow) {
                guiGraphics.fill(x, rowY, x + width, rowY + ROW_HEIGHT, 0xFF3050A0);
            } else if (mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                guiGraphics.fill(x, rowY, x + width, rowY + ROW_HEIGHT, 0xFF2A2A2A);
            }

            row.render(guiGraphics, x + 4, rowY + 4, width - 8);
        }

        if (maxScroll() > 0) {
            renderScrollBar(guiGraphics);
        }
    }

    private void renderScrollBar(GuiGraphicsExtractor guiGraphics) {
        int totalRows = rows.size();
        int visRows = visibleRowCount();
        if (totalRows <= visRows) return;

        int barHeight = Math.max(20, (int) ((float) visRows / totalRows * height));
        int barY = y + (int) ((float) scrollOffset / maxScroll() * (height - barHeight));

        guiGraphics.fill(x + width - SCROLL_BAR_WIDTH, barY, x + width, barY + barHeight, 0xFF606060);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return false;

        int oldScroll = scrollOffset;
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset - (int) verticalAmount));
        return scrollOffset != oldScroll;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return false;

        if (button == 0 && tryThumbClick(mouseX, mouseY, true)) return true;

        int visualRow = (int) ((mouseY - y) / ROW_HEIGHT);
        int rowIdx = scrollOffset + visualRow;
        if (rowIdx >= 0 && rowIdx < rows.size()) {
            selectedRow = rowIdx;
            if (button == 0) {
                com.komixkat.customdrops.client.gui.UiSfx.click();
                rows.get(rowIdx).onClick();
            }
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!thumbDragging) return false;
        int totalRows = rows.size();
        int visRows = visibleRowCount();
        int ms = Math.max(0, totalRows - visRows);
        if (ms <= 0) {
            thumbDragging = false;
            return false;
        }
        int barHeight = Math.max(20, (int) ((float) visRows / totalRows * height));
        int usable = Math.max(1, height - barHeight);
        float frac = (float) (mouseY - y - thumbGrabOffset) / usable;
        scrollOffset = Math.max(0, Math.min(ms, Math.round(frac * ms)));
        return true;
    }

    public void mouseReleased(double mouseX, double mouseY) {
        thumbDragging = false;
    }

    public boolean isThumbDragging() {
        return thumbDragging;
    }

    private boolean tryThumbClick(double mouseX, double mouseY, boolean drag) {
        int[] thumb = thumbRect();
        if (thumb == null) return false;
        if (mouseX >= x + width - SCROLL_BAR_WIDTH && mouseX <= x + width
            && mouseY >= thumb[0] && mouseY <= thumb[0] + thumb[1]) {
            if (drag) {
                thumbDragging = true;
                thumbGrabOffset = (int) mouseY - thumb[0];
            }
            return true;
        }
        return false;
    }

    private int[] thumbRect() {
        int totalRows = rows.size();
        int visRows = visibleRowCount();
        int ms = Math.max(0, totalRows - visRows);
        if (ms <= 0) return null;
        int barHeight = Math.max(20, (int) ((float) visRows / totalRows * height));
        int barY = y + (int) ((float) scrollOffset / ms * (height - barHeight));
        return new int[] { barY, barHeight };
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    public List<EntryRow> getRows() {
        return List.copyOf(rows);
    }

    public static EntryRow section(String title) {
        return new SectionRow(title);
    }

    public static abstract class EntryRow {

        public abstract void render(GuiGraphicsExtractor guiGraphics, int x, int y, int width);

        public void onClick() {}

        public boolean isSection() {
            return false;
        }

        public String label() {
            return "";
        }
    }

    private static final class SectionRow extends EntryRow {

        private final String title;

        SectionRow(String title) {
            this.title = title;
        }

        @Override
        public void render(GuiGraphicsExtractor guiGraphics, int x, int y, int width) {}

        @Override
        public boolean isSection() {
            return true;
        }

        @Override
        public String label() {
            return title;
        }
    }
}
