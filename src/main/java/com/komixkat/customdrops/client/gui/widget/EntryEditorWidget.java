package com.komixkat.customdrops.client.gui.widget;

import com.komixkat.customdrops.client.autocomplete.RegistryIndex;
import com.komixkat.customdrops.config.schema.LootItemEntry;
import com.komixkat.customdrops.config.schema.MobDropEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class EntryEditorWidget {

    private int x;
    private int y;
    private int width;
    private boolean visible = false;
    private MobDropEntry currentEntry;
    private RegistryAutocompleteField targetField;
    private final List<ItemRowWidget> itemRows = new ArrayList<>();

    private static final int HEADER_HEIGHT = 30;

    public EntryEditorWidget(int x, int y, int width, Screen parent, RegistryIndex registryIndex) {
        this.x = x;
        this.y = y;
        this.width = width;

        targetField = new RegistryAutocompleteField(
            parent, x + 4, y + 6, width - 8, 18,
            Component.literal("Target"), registryIndex
        );
    }

    public void open(MobDropEntry entry) {
        this.currentEntry = entry;
        this.visible = true;
        itemRows.clear();

        if (targetField != null) {
            targetField.setValue(entry != null ? entry.targetId() : "");
        }

        if (entry != null) {
            int iy = y + HEADER_HEIGHT + 4;
            for (LootItemEntry item : entry.items() != null ? entry.items() : List.<LootItemEntry>of()) {
                ItemRowWidget row = new ItemRowWidget(x + 4, iy, width - 8, item);
                itemRows.add(row);
                iy += row.getHeight() + 2;
            }
        }
    }

    public void close() {
        this.visible = false;
        this.currentEntry = null;
        itemRows.clear();
    }

    public boolean isVisible() {
        return visible;
    }

    public MobDropEntry getCurrentEntry() {
        return currentEntry;
    }

    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        var font = net.minecraft.client.Minecraft.getInstance().font;
        guiGraphics.fill(x, y, x + width, y + HEADER_HEIGHT, 0xFF222222);
        guiGraphics.text(font, "Edit Entry", x + 6, y + 8, 0xFFE0E0E0, false);

        if (targetField != null) {
            targetField.extractWidgetRenderState(guiGraphics, mouseX, mouseY, delta);
        }

        int iy = y + HEADER_HEIGHT + 4;
        for (ItemRowWidget row : itemRows) {
            if (iy + row.getHeight() < y + 400) {
                row.render(guiGraphics, mouseX, mouseY, delta);
            }
            iy += row.getHeight() + 2;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        if (targetField != null && targetField.isMouseOver(mouseX, mouseY)) {
            return true;
        }

        for (ItemRowWidget row : itemRows) {
            if (row.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEADER_HEIGHT;
    }

    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (!visible) return false;
        if (targetField != null && targetField.keyPressed(event)) {
            return true;
        }
        return false;
    }
}
