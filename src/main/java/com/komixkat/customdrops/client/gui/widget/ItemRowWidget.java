package com.komixkat.customdrops.client.gui.widget;

import com.komixkat.customdrops.config.schema.LootConditionEntry;
import com.komixkat.customdrops.config.schema.LootItemEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ItemRowWidget {

    private final int x;
    private final int y;
    private final int width;
    private LootItemEntry entry;
    private boolean expanded = false;

    private static final int ROW_HEIGHT = 22;
    private static final int EXPANDED_HEIGHT = 120;
    private static final int CONDITION_ROW_HEIGHT = 18;

    public ItemRowWidget(int x, int y, int width, LootItemEntry entry) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.entry = entry;
    }

    public int getHeight() {
        if (!expanded) return ROW_HEIGHT;
        int h = EXPANDED_HEIGHT;
        if (!entry.conditions().isEmpty()) {
            h += entry.conditions().size() * CONDITION_ROW_HEIGHT;
        }
        return h;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        guiGraphics.fill(x, y, x + width, y + ROW_HEIGHT, expanded ? 0xFF282828 : 0xFF202020);

        String summary = entry.itemId() + " x" + entry.minCount() + "-" + entry.maxCount() +
            " (w:" + entry.weight() + ", " + (int)(entry.chance() * 100) + "%)";
        guiGraphics.text(font, summary, x + 6, y + 7, 0xFFE0E0E0, false);

        guiGraphics.text(font, expanded ? "v" : ">",
            x + width - 16, y + 7, 0xFF888888, false);

        if (expanded) {
            int ey = y + ROW_HEIGHT + 2;

            guiGraphics.text(font, "Conditions:", x + 6, ey, 0xFFAAAAAA, false);
            ey += 12;

            if (entry.conditions().isEmpty()) {
                guiGraphics.text(font, "  (none)", x + 6, ey, 0xFF666666, false);
                ey += CONDITION_ROW_HEIGHT;
            } else {
                for (LootConditionEntry cond : entry.conditions()) {
                    guiGraphics.text(font, "  " + cond.type(), x + 6, ey, 0xFFCCCCCC, false);
                    ey += CONDITION_ROW_HEIGHT;
                }
            }

            guiGraphics.text(font, "Enchantments:", x + 6, ey, 0xFFAAAAAA, false);
            ey += 12;

            if (entry.enchantments().isEmpty()) {
                guiGraphics.text(font, "  (none)", x + 6, ey, 0xFF666666, false);
            } else {
                for (var ench : entry.enchantments()) {
                    guiGraphics.text(font, "  " + ench.enchantmentId() + " Lv" + ench.level(), x + 6, ey, 0xFFCCCCCC, false);
                    ey += CONDITION_ROW_HEIGHT;
                }
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ROW_HEIGHT) {
            if (button == 0) {
                expanded = !expanded;
                return true;
            }
        }
        return false;
    }
}
