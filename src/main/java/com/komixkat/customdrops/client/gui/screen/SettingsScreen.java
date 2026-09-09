package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.widget.ScrollablePane;
import com.komixkat.customdrops.config.CustomDropsConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class SettingsScreen extends SplitPaneScreen {

    private ScrollablePane pane;

    public SettingsScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.menu.settings"));
    }

    @Override
    protected boolean isSaveable() {
        return true;
    }

    @Override
    protected String backButtonLabel() {
        return "Back";
    }

    @Override
    protected void buildNavigation() {
        navWidget.addCategory("Settings");
        navWidget.addEntry("Settings", "Category Toggles", () -> {});
        navWidget.addEntry("Settings", "Configs (profiles / reset)", () ->
            this.minecraft.gui.setScreen(new ConfigsScreen(this)));
        navWidget.addEntry("Settings", "Export / Import", () ->
            this.minecraft.gui.setScreen(new ExportImportScreen(this)));
        navWidget.addEntry("Settings", "Server Config", () ->
            this.minecraft.gui.setScreen(new ServerConfigScreen(this)));
    }

    @Override
    protected void initContent() {
        pane = new ScrollablePane(this, rightPanelX + PADDING, contentY + PADDING,
            Math.max(0, rightPanelWidth - PADDING * 2), Math.max(0, contentBottom - contentY - PADDING * 2 - 12));

        CustomDropsConfig config = CustomDropsMod.config();
        ScrollablePane.Cursor cur = pane.newCursor();

        pane.addLabel(cur.x, cur.y, "Category Toggles", Ui.TEXT);
        cur.y += Ui.LINE_H + 8;
        pane.addLabel(cur.x, cur.y, "Disabled categories are skipped by the loot injector.", Ui.MUTED);
        cur.y += Ui.LINE_H + 6;
        pane.addLabel(cur.x, cur.y, "Changes apply to memory; press Save to write to disk.", Ui.DIM);
        cur.y += 18;

        pane.addCheckbox(cur.x, cur.y, "Mob Drops", config.mobDropsEnabled(), v -> toggle(v, () -> {
            CustomDropsMod.config().setMobDropsEnabled(v);
        }));
        cur.y += Ui.CHECKBOX_H + 2;

        pane.addCheckbox(cur.x, cur.y, "Block Drops", config.blockDropsEnabled(), v -> toggle(v, () -> {
            CustomDropsMod.config().setBlockDropsEnabled(v);
        }));
        cur.y += Ui.CHECKBOX_H + 2;

        pane.addCheckbox(cur.x, cur.y, "Chest Loot", config.chestLootEnabled(), v -> toggle(v, () -> {
            CustomDropsMod.config().setChestLootEnabled(v);
        }));
        cur.y += Ui.CHECKBOX_H + 2;

        pane.addCheckbox(cur.x, cur.y, "Fishing Loot", config.fishingLootEnabled(), v -> toggle(v, () -> {
            CustomDropsMod.config().setFishingLootEnabled(v);
        }));
        cur.y += Ui.CHECKBOX_H + 2;

        pane.addCheckbox(cur.x, cur.y, "Equipment Overrides", config.equipmentOverridesEnabled(), v -> toggle(v, () -> {
            CustomDropsMod.config().setEquipmentOverridesEnabled(v);
        }));
        cur.y += Ui.CHECKBOX_H + 8;

        pane.addLabel(cur.x, cur.y, "Tools", Ui.TEXT);
        cur.y += Ui.LINE_H + 6;
        pane.addButton(cur, cur.x, cur.y, Math.min(220, cur.w), "Configs (switch profile / reset)", () ->
            this.minecraft.gui.setScreen(new ConfigsScreen(this)));
        pane.addButton(cur, cur.x, cur.y + Ui.BUTTON_H + 4, Math.min(220, cur.w), "Export / Import Config", () ->
            this.minecraft.gui.setScreen(new ExportImportScreen(this)));
        pane.addButton(cur, cur.x, cur.y + 2 * (Ui.BUTTON_H + 4), Math.min(220, cur.w), "Server Config (view / edit)", () ->
            this.minecraft.gui.setScreen(new ServerConfigScreen(this)));

        cur.y += Ui.BUTTON_H * 3 + 8;
        pane.noteCursorY(cur.y);
        pane.finish(8);
    }

    private void toggle(boolean v, Runnable apply) {
        apply.run();
        markDirty();
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (pane != null) {
            pane.render(guiGraphics, mouseX, mouseY, delta);
        }
    }

    @Override
    protected boolean contentMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return pane != null && pane.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override
    protected boolean contentMouseClicked(double mouseX, double mouseY, int button) {
        if (pane != null && pane.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    protected boolean contentMouseDragged(double mouseX, double mouseY, double dx, double dy) {
        if (pane != null && pane.mouseDragged(mouseX, mouseY)) return true;
        return false;
    }

    @Override
    protected void contentMouseReleased(double mouseX, double mouseY) {
        if (pane != null) {
            pane.mouseReleased(mouseX, mouseY);
        }
    }
}