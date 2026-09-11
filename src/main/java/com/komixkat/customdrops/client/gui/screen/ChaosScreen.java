package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.chaos.ChaosGenerator;
import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.widget.ScrollablePane;
import com.komixkat.customdrops.config.ConfigProfiles;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class ChaosScreen extends SplitPaneScreen {

    private ScrollablePane pane;
    private boolean mobs = true;
    private boolean blocks = true;
    private boolean chests = true;
    private boolean fishing = true;
    private boolean sensibleOnly = true;
    private String statusMessage = "";

    public ChaosScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.menu.chaos"));
    }

    @Override
    protected boolean isSaveable() {
        return false;
    }

    @Override
    protected String backButtonLabel() {
        return "Back";
    }

    @Override
    protected void buildNavigation() {
        navWidget.addCategory("Chaos");
        navWidget.addEntry("Chaos", "Generate a Lootstorm", () -> {});
    }

    @Override
    protected void initContent() {
        pane = new ScrollablePane(this, rightPanelX + PADDING, contentY + PADDING,
            Math.max(0, rightPanelWidth - PADDING * 2), Math.max(0, contentBottom - contentY - PADDING * 2 - 38));
        buildHub();
    }

    private void buildHub() {
        if (pane == null) return;
        pane.clear();
        ScrollablePane.Cursor cur = pane.newCursor();

        pane.addLabel(cur.x, cur.y, "CHAOS: Lootstorm", Ui.TEXT);
        cur.y += Ui.LINE_H + 6;
        pane.addLabel(cur.x, cur.y, "Generates a config where every loot source drops", Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "one random item, count 1-64.", Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "It only generates. You apply it afterwards.", Ui.DIM);
        cur.y += Ui.LINE_H + 12;

        pane.addLabel(cur.x, cur.y, "Categories", Ui.SECTION_TEXT);
        cur.y += Ui.LINE_H + 4;
        pane.addCheckbox(cur.x, cur.y, "Mob Drops", mobs, v -> mobs = v);
        cur.y += Ui.CHECKBOX_H + 2;
        pane.addCheckbox(cur.x, cur.y, "Block Drops", blocks, v -> blocks = v);
        cur.y += Ui.CHECKBOX_H + 2;
        pane.addCheckbox(cur.x, cur.y, "Chest Loot", chests, v -> chests = v);
        cur.y += Ui.CHECKBOX_H + 2;
        pane.addCheckbox(cur.x, cur.y, "Fishing Loot", fishing, v -> fishing = v);
        cur.y += Ui.CHECKBOX_H + 2;
        pane.addCheckbox(cur.x, cur.y, "Sensible items only", sensibleOnly, v -> sensibleOnly = v);
        cur.y += Ui.CHECKBOX_H + 10;

        pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Generate Lootstorm Config", this::doGenerate);
        cur.y += Ui.BUTTON_H + 8;

        pane.noteCursorY(cur.y);
        pane.finish(8);
    }

    private void doGenerate() {
        ChaosGenerator.Result result = ChaosGenerator.generate(mobs, blocks, chests, fishing, sensibleOnly);
        if (result.sources() == 0) {
            statusMessage = "No loot sources found for the selected categories.";
            return;
        }
        String baseName = "CHAOS " + java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
        String name = baseName;
        int attempt = 0;
        while (!ConfigProfiles.create(name, result.config()) && attempt < 5) {
            attempt++;
            name = baseName + " " + (attempt + 1);
        }
        if (!ConfigProfiles.names().contains(name)) {
            statusMessage = "Could not save the lootstorm as a config.";
            return;
        }
        this.minecraft.gui.setScreen(
            new ChaosApplyScreen(this, name, result.config(), result.items(), result.sources()));
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (pane != null) {
            pane.render(guiGraphics, mouseX, mouseY, delta);
        }
        if (!statusMessage.isEmpty() && pane != null) {
            drawStatus(guiGraphics, statusMessage, Ui.ACCENT);
        }
    }

    @Override
    protected boolean contentMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return pane != null && pane.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override
    protected boolean contentMouseClicked(double mouseX, double mouseY, int button) {
        return pane != null && pane.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean contentMouseDragged(double mouseX, double mouseY, double dx, double dy) {
        return pane != null && pane.mouseDragged(mouseX, mouseY);
    }

    @Override
    protected void contentMouseReleased(double mouseX, double mouseY) {
        if (pane != null) {
            pane.mouseReleased(mouseX, mouseY);
        }
    }
}