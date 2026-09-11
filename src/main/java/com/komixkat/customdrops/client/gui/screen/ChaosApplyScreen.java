package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.widget.ScrollablePane;
import com.komixkat.customdrops.config.ConfigProfiles;
import com.komixkat.customdrops.config.CustomDropsConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public final class ChaosApplyScreen extends SplitPaneScreen {

    private ScrollablePane pane;
    private final String configName;
    private final CustomDropsConfig config;
    private final int itemCount;
    private final int sourceCount;
    private String statusMessage = "";

    public ChaosApplyScreen(net.minecraft.client.gui.screens.Screen parent,
                            String configName, CustomDropsConfig config, int itemCount, int sourceCount) {
        super(parent, Component.translatable("customdrops.menu.chaos"));
        this.configName = configName;
        this.config = config;
        this.itemCount = itemCount;
        this.sourceCount = sourceCount;
        preSelect(configName);
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
        navWidget.addCategory("Lootstorm");
        navWidget.addEntry("Lootstorm", configName, () -> {});
        navWidget.addCategory("Actions");
        navWidget.addEntry("Actions", "Open in Configs", this::openConfigs);
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

        pane.addLabel(cur.x, cur.y, "Lootstorm \"" + configName + "\"", Ui.TEXT);
        cur.y += Ui.LINE_H + 6;
        pane.addLabel(cur.x, cur.y, "Saved as a config. Apply it however you like.", Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y,
            itemCount + " random items spread across " + sourceCount + " loot sources.", Ui.DIM);
        cur.y += Ui.LINE_H + 12;

        pane.addLabel(cur.x, cur.y, countsFor(config), Ui.DIM);
        cur.y += Ui.LINE_H + 12;

        MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            pane.addLabel(cur.x, cur.y, "You are not in a single-player world right now.", Ui.WARN);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, "Join a world to apply the lootstorm to it.", Ui.DIM);
            cur.y += Ui.LINE_H + 10;
        } else {
            pane.addLabel(cur.x, cur.y, "Applies the lootstorm to this world's drops right now.", Ui.MUTED);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, "The change stays on this world until you clear it in Configs.", Ui.DIM);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, "Keep a backup of the world first.", Ui.DIM);
            cur.y += Ui.LINE_H + 8;
            pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Apply to This World", this::confirmApplyToWorld);
            cur.y += Ui.BUTTON_H + 12;
        }

        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 12;

        pane.addLabel(cur.x, cur.y, "The active config is what the editor and the world", Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "use by default. The old config stays saved.", Ui.DIM);
        cur.y += Ui.LINE_H + 8;
        pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Set as Active Config", this::confirmSetActive);
        cur.y += Ui.BUTTON_H + 12;

        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 12;

        pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Back to Configs", this::openConfigs);
        cur.y += Ui.BUTTON_H + 12;

        pane.noteCursorY(cur.y);
        pane.finish(8);
    }

    private static String countsFor(CustomDropsConfig config) {
        return "Mob " + config.mobDrops().size() + "  \u00B7  Block " + config.blockDrops().size()
            + "  \u00B7  Chest " + config.chestLoot().size() + "  \u00B7  Fishing " + config.fishingLoot().size()
            + "  \u00B7  Equipment " + config.equipmentOverrides().size();
    }

    private void confirmApplyToWorld() {
        this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
            "Overwrite this world's drops with \"" + configName + "\"?",
            List.of("Replaces this world's drops right now.",
                "It stays on this world in its save until you",
                "clear it in Configs ('Clear this World's Config').",
                "Keep a backup of the world first."),
            "Overwrite", this::performApplyToWorld));
    }

    private void performApplyToWorld() {
        MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            statusMessage = "Not in a single-player world right now.";
            buildHub();
            return;
        }
        CustomDropsMod.linkConfigToWorld(server, configName);
        CustomDropsMod.reloadForRunningWorld();
        statusMessage = "Overwrote this world with \"" + configName + "\".";
        buildHub();
    }

    private void confirmSetActive() {
        this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
            "Make \"" + configName + "\" the active config?",
            List.of("The editor and the running world use the active config.",
                "The old config stays saved."),
            "Set as Active", () -> {
                if (ConfigProfiles.switchTo(configName)) {
                    statusMessage = "Switched active config to \"" + configName + "\".";
                } else {
                    statusMessage = "Could not switch to \"" + configName + "\".";
                }
                buildHub();
            }));
    }

    private void openConfigs() {
        this.minecraft.gui.setScreen(new ConfigsScreen(this));
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
    protected boolean contentMouseClicked(double mouseX, double mouseY, int button) {
        return pane != null && pane.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean contentMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return pane != null && pane.mouseScrolled(mouseX, mouseY, verticalAmount);
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