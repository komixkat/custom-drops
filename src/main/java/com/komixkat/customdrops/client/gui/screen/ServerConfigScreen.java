package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.widget.ScrollablePane;
import com.komixkat.customdrops.client.network.ServerConfigCache;
import com.komixkat.customdrops.config.CustomDropsConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public final class ServerConfigScreen extends SplitPaneScreen {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    private ScrollablePane pane;
    private boolean localPending = false;
    private String statusMessage = "";

    public ServerConfigScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.config.server"));
    }

    @Override
    protected String backButtonLabel() {
        return "Back";
    }

    @Override
    protected void buildNavigation() {
        boolean connected = ServerConfigCache.connected();
        boolean canEdit = connected && ServerConfigCache.canEdit();

        if (!connected) {
            navWidget.addCategory("Status");
            navWidget.addEntry("Status", "Not connected", () -> {});
            return;
        }

        navWidget.addCategory("Server Rules");
        navWidget.addEntry("Server Rules", "Mob Drops (" + size(ServerConfigCache.config().mobDrops()) + ")",
            () -> openRules(new MobDropsScreen(this, ServerConfigCache::config, LootRuleScreen.Mode.REMOTE)));
        navWidget.addEntry("Server Rules", "Block Drops (" + size(ServerConfigCache.config().blockDrops()) + ")",
            () -> openRules(new BlockDropsScreen(this, ServerConfigCache::config, LootRuleScreen.Mode.REMOTE)));
        navWidget.addEntry("Server Rules", "Chest Loot (" + size(ServerConfigCache.config().chestLoot()) + ")",
            () -> openRules(new ChestLootScreen(this, ServerConfigCache::config, LootRuleScreen.Mode.REMOTE)));
        navWidget.addEntry("Server Rules", "Fishing Loot (" + size(ServerConfigCache.config().fishingLoot()) + ")",
            () -> openRules(new FishingLootScreen(this, ServerConfigCache::config, LootRuleScreen.Mode.REMOTE)));
        navWidget.addEntry("Server Rules", "Equipment (" + size(ServerConfigCache.config().equipmentOverrides()) + ")",
            () -> openRules(new EquipmentScreen(this, ServerConfigCache::config, LootRuleScreen.Mode.REMOTE)));

        navWidget.addCategory("Status");
        navWidget.addEntry("Status", canEdit ? "You can edit (operator)" : "Read-only (not operator)",
            () -> {});
    }

    private void openRules(SplitPaneScreen screen) {
        boolean canEdit = ServerConfigCache.canEdit();
        if (screen instanceof LootRuleScreen<?> lootScreen) {
            lootScreen.setReadOnly(!canEdit);
            lootScreen.setRemoteSendHandler(canEdit ? this::onSend : null);
        } else if (screen instanceof EquipmentScreen equipmentScreen) {
            equipmentScreen.setReadOnly(!canEdit);
            equipmentScreen.setRemoteSendHandler(canEdit ? this::onSend : null);
        }
        this.minecraft.gui.setScreen(screen);
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    @Override
    protected void initContent() {
        pane = new ScrollablePane(this, rightPanelX + PADDING, contentY + PADDING,
            Math.max(0, rightPanelWidth - PADDING * 2), Math.max(0, contentBottom - contentY - PADDING * 2 - 38));
        rebuildPanel();
    }

    private void rebuildPanel() {
        if (pane == null) return;
        pane.clear();
        ScrollablePane.Cursor cur = pane.newCursor();
        boolean connected = ServerConfigCache.connected();
        boolean canEdit = connected && ServerConfigCache.canEdit();

        if (!connected) {
            pane.addLabel(cur.x, cur.y, "Server Config", Ui.TEXT);
            cur.y += Ui.LINE_H + 6;
            pane.addLabel(cur.x, cur.y, "Not connected to a Custom Drops server.", Ui.TEXT);
            cur.y += Ui.LINE_H + 4;
            pane.addLabel(cur.x, cur.y, "Join a server running this mod to see its exact config.", Ui.MUTED);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, "The server pushes its config; operators can edit it from here,", Ui.DIM);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, "everyone else gets a read-only view.", Ui.DIM);
            return;
        }

        var config = ServerConfigCache.config();
        pane.addLabel(cur.x, cur.y, "Server Config", Ui.TEXT);
        cur.y += Ui.LINE_H + 6;
        pane.addLabel(cur.x, cur.y,
            "Synced from server at " + TIME.format(Instant.ofEpochMilli(ServerConfigCache.receivedAt())),
            canEdit ? Ui.ACCENT : Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y,
            canEdit ? "You are an operator - edits can be sent to the server."
                : "Read-only view - you are not an operator on this server.",
            canEdit ? Ui.MUTED : Ui.ACCENT);
        cur.y += Ui.LINE_H + 12;

        pane.addLabel(cur.x, cur.y, "Rules on the server", Ui.MUTED);
        cur.y += Ui.LINE_H + 4;
        pane.addLabel(cur.x, cur.y, "Mob drops:       " + size(config.mobDrops())
            + (config.mobDropsEnabled() ? "  (enabled)" : "  (disabled)"), Ui.TEXT);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "Block drops:     " + size(config.blockDrops())
            + (config.blockDropsEnabled() ? "  (enabled)" : "  (disabled)"), Ui.TEXT);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "Chest loot:      " + size(config.chestLoot())
            + (config.chestLootEnabled() ? "  (enabled)" : "  (disabled)"), Ui.TEXT);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "Fishing loot:    " + size(config.fishingLoot())
            + (config.fishingLootEnabled() ? "  (enabled)" : "  (disabled)"), Ui.TEXT);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "Equipment:       " + size(config.equipmentOverrides())
            + (config.equipmentOverridesEnabled() ? "  (enabled)" : "  (disabled)"), Ui.TEXT);
        cur.y += Ui.LINE_H + 12;

        if (canEdit) {
            pane.addLabel(cur.x, cur.y, "Category toggles (pending until you send)", Ui.MUTED);
            cur.y += Ui.LINE_H + 4;
            pane.addCheckbox(cur.x, cur.y, "Enable Mob Drops", config.mobDropsEnabled(),
                v -> queueToggle(c -> c.setMobDropsEnabled(v)));
            cur.y += Ui.CHECKBOX_H + 2;
            pane.addCheckbox(cur.x, cur.y, "Enable Block Drops", config.blockDropsEnabled(),
                v -> queueToggle(c -> c.setBlockDropsEnabled(v)));
            cur.y += Ui.CHECKBOX_H + 2;
            pane.addCheckbox(cur.x, cur.y, "Enable Chest Loot", config.chestLootEnabled(),
                v -> queueToggle(c -> c.setChestLootEnabled(v)));
            cur.y += Ui.CHECKBOX_H + 2;
            pane.addCheckbox(cur.x, cur.y, "Enable Fishing Loot", config.fishingLootEnabled(),
                v -> queueToggle(c -> c.setFishingLootEnabled(v)));
            cur.y += Ui.CHECKBOX_H + 2;
            pane.addCheckbox(cur.x, cur.y, "Enable Equipment Overrides", config.equipmentOverridesEnabled(),
                v -> queueToggle(c -> c.setEquipmentOverridesEnabled(v)));
            cur.y += Ui.CHECKBOX_H + 12;

            pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w),
                localPending ? "Send Changes to Server" : "Send to Server", this::onSend);
            cur.y += Ui.BUTTON_H + 6;
            pane.addLabel(cur.x, cur.y, "Rule editing happens from the left panel.", Ui.DIM);
        } else {
            pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Refresh View",
                () -> this.minecraft.gui.setScreen(new ServerConfigScreen(this)));
        }

        pane.noteCursorY(cur.y);
        pane.finish(28);
    }

    private void queueToggle(Consumer<CustomDropsConfig> change) {
        ServerConfigCache.mutateToggles(() -> change.accept(ServerConfigCache.config()));
        localPending = true;
        rebuildPanel();
    }

    private void onSend() {
        ServerConfigCache.sendToServer();
        localPending = false;
        statusMessage = "Sent to server. Waiting for confirmation...";
        rebuildPanel();
    }

    @Override
    protected boolean isSaveable() {
        return false;
    }

    @Override
    protected void addExtraBottomBarButtons(int barY) {
        if (ServerConfigCache.connected() && ServerConfigCache.canEdit()) {
            addBottomBarButton(localPending ? "Send Changes to Server" : "Send to Server", this::onSend);
        }
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (pane != null) {
            pane.render(guiGraphics, mouseX, mouseY, delta);
        }
        if (!statusMessage.isEmpty() && pane != null) {
            drawStatus(guiGraphics, statusMessage, Ui.WARN);
        }
    }

    @Override
    protected boolean contentMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return pane != null && pane.mouseScrolled(mouseX, mouseY, verticalAmount);
    }
}