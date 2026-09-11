package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.widget.NavigationWidget;
import com.komixkat.customdrops.client.gui.widget.ScrollablePane;
import com.komixkat.customdrops.client.network.ServerConfigCache;
import com.komixkat.customdrops.config.ConfigLoader;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.config.ImportLibrary;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class ImportsScreen extends SplitPaneScreen {

    private ScrollablePane pane;
    private CustomDropsConfig staging;
    private String stagedName = "";
    private boolean staged = false;
    private String statusMessage = "";

    public ImportsScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.menu.imports"));
    }

    public ImportsScreen(net.minecraft.client.gui.screens.Screen parent, String name, CustomDropsConfig config) {
        super(parent, Component.translatable("customdrops.menu.imports"));
        staging = config;
        stagedName = name;
        staged = true;
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
        navWidget.addCategory("Saved Imports");
        for (String name : ImportLibrary.names()) {
            String label = name;
            if (staged && name.equals(stagedName)) {
                label += "  [STAGED]";
            }
            String entryLabel = label;
            navWidget.addEntry("Saved Imports", entryLabel, () -> {
                selectedKey = entryLabel;
                stageFromLibrary(name);
            });
        }

        if (staged) {
            navWidget.addCategory("Staged Import");
            navWidget.addEntry("Staged Import", "Mob Drops (" + size(staging.mobDrops()) + ")", () -> {
                selectedKey = null;
                this.minecraft.gui.setScreen(new MobDropsScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING));
            });
            navWidget.addEntry("Staged Import", "Block Drops (" + size(staging.blockDrops()) + ")", () -> {
                selectedKey = null;
                this.minecraft.gui.setScreen(new BlockDropsScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING));
            });
            navWidget.addEntry("Staged Import", "Chest Loot (" + size(staging.chestLoot()) + ")", () -> {
                selectedKey = null;
                this.minecraft.gui.setScreen(new ChestLootScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING));
            });
            navWidget.addEntry("Staged Import", "Fishing Loot (" + size(staging.fishingLoot()) + ")", () -> {
                selectedKey = null;
                this.minecraft.gui.setScreen(new FishingLootScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING));
            });
            navWidget.addEntry("Staged Import", "Equipment (" + size(staging.equipmentOverrides()) + ")", () -> {
                selectedKey = null;
                this.minecraft.gui.setScreen(new EquipmentScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING));
            });
            navWidget.addEntry("Staged Import", "Apply to Active Config", this::applyToActive);
            if (inSinglePlayer()) {
                navWidget.addEntry("Staged Import", "Apply to This World", this::applyToWorld);
            }
            if (ServerConfigCache.connected() && ServerConfigCache.canEdit()) {
                navWidget.addEntry("Staged Import", "Send to Server", this::sendToServer);
            }
            navWidget.addEntry("Staged Import", "Discard Staging", this::discardStaging);
        }

        navWidget.addCategory("Actions");
        navWidget.addEntry("Actions", "Import a Code", () ->
            this.minecraft.gui.setScreen(new ExportImportScreen(this)));
        navWidget.addEntry("Actions", "Save Current Config as an Import", this::saveCurrentAsImport);
        navWidget.addEntry("Actions", "Clear All Rules", this::clearAllRules);
    }

    private static boolean inSinglePlayer() {
        return net.minecraft.client.Minecraft.getInstance().getSingleplayerServer() != null;
    }

    private static int size(java.util.List<?> list) {
        return list == null ? 0 : list.size();
    }

    private void stageFromLibrary(String name) {
        CustomDropsConfig config = ImportLibrary.load(name);
        if (config == null) {
            statusMessage = "Could not load \"" + name + "\".";
            return;
        }
        staging = config;
        stagedName = name;
        staged = true;
        statusMessage = "Staged \"" + name + "\". Pick where to apply it.";
        refreshNav();
        buildHub();
    }

    private void applyToActive() {
        if (!staged) return;
        this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
            "Apply \"" + stagedName + "\" to the active config?",
            java.util.List.of("Replaces the active config's rules with this import.",
                "It stays memory-only until you press Save."),
            "Apply", this::performApplyToActive));
    }

    private void performApplyToActive() {
        if (!staged) return;
        CustomDropsConfig target = CustomDropsMod.config();
        staging.copyInto(target);
        markDirty();
        statusMessage = "Applied \"" + stagedName + "\" to your config. Press Save.";
        buildHub();
    }

    private void applyToWorld() {
        if (!staged) return;
        this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
            "Apply \"" + stagedName + "\" to this world?",
            java.util.List.of("Replaces this world's drops right now.",
                "It cannot be rolled back from here.",
                "Keep a backup of the world first."),
            "Apply", this::performApplyToWorld));
    }

    private void performApplyToWorld() {
        if (!staged) return;
        net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            statusMessage = "Not in a single-player world right now.";
            return;
        }
        ConfigLoader.save(CustomDropsMod.worldConfigDir(server), staging);
        CustomDropsMod.reloadForRunningWorld();
        statusMessage = "Applied \"" + stagedName + "\" to this world and reloaded.";
        buildHub();
    }

    private void sendToServer() {
        if (!staged || !ServerConfigCache.canEdit() || !ServerConfigCache.connected()) {
            statusMessage = "Need to be an operator on a connected server.";
            return;
        }
        this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
            "Send \"" + stagedName + "\" to the server?",
            java.util.List.of("Replaces the server's drops for everyone.",
                "It cannot be rolled back from here.",
                "Keep a backup of the server world first."),
            "Send", this::performSendToServer));
    }

    private void performSendToServer() {
        if (!staged || !ServerConfigCache.canEdit() || !ServerConfigCache.connected()) {
            statusMessage = "Need to be an operator on a connected server.";
            return;
        }
        ServerConfigCache.pushConfig(staging);
        statusMessage = "Sent \"" + stagedName + "\" to the server.";
        buildHub();
    }

    private void discardStaging() {
        staged = false;
        stagedName = "";
        statusMessage = "";
        selectedKey = null;
        refreshNav();
        buildHub();
    }

    private void saveCurrentAsImport() {
        this.minecraft.gui.setScreen(new PromptDialogScreen(this, "Save current config as an import",
            ImportLibrary.suggestedName("My config"), "Save", name -> {
                String clean = name == null ? "" : name.trim();
                if (ImportLibrary.save(clean, CustomDropsMod.config())) {
                    statusMessage = "Saved \"" + clean + "\" as an import.";
                } else {
                    statusMessage = "Could not save \"" + clean + "\" (invalid or already used).";
                }
                refreshNav();
                buildHub();
            }));
    }

    private void clearAllRules() {
        CustomDropsConfig config = CustomDropsMod.config();
        config.clearAll();
        markDirty();
        statusMessage = "Cleared all rules. Press Save.";
        buildHub();
    }

    private void doRename(String name) {
        this.minecraft.gui.setScreen(new PromptDialogScreen(this, "Rename import \"" + name + "\"",
            name, "Rename", newName -> {
                String clean = newName == null ? "" : newName.trim();
                if (ImportLibrary.rename(name, clean)) {
                    if (staged && name.equals(stagedName)) stagedName = clean;
                    statusMessage = "Renamed \"" + name + "\".";
                } else {
                    statusMessage = "Could not rename (invalid or already used).";
                }
                refreshNav();
                buildHub();
            }));
    }

    private void doDelete(String name) {
        this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
            "Delete import \"" + name + "\"?",
            java.util.List.of("Removes it from disk. Cannot be undone."),
            "Delete", () -> {
                if (ImportLibrary.delete(name)) {
                    if (staged && name.equals(stagedName)) {
                        staged = false;
                        stagedName = "";
                        selectedKey = null;
                    }
                    statusMessage = "Deleted \"" + name + "\".";
                } else {
                    statusMessage = "Could not delete \"" + name + "\".";
                }
                refreshNav();
                buildHub();
            }));
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

        if (!staged) {
            hubStamp = -1;
            pane.addLabel(cur.x, cur.y, "Imports", Ui.TEXT);
            cur.y += Ui.LINE_H + 6;
            pane.addLabel(cur.x, cur.y, "Imports are saved configs you can apply later.", Ui.MUTED);
            cur.y += Ui.LINE_H + 8;

            java.util.List<String> names = ImportLibrary.names();
            if (names.isEmpty()) {
                pane.addLabel(cur.x, cur.y, "No imports yet. Import a code to get started.", Ui.DIM);
                cur.y += Ui.LINE_H + 12;
            } else {
                int w0 = 70;
                int w1 = 64;
                int w2 = 64;
                int gap = 4;
                int btnArea = w0 + w1 + w2 + gap * 2;
                int rowH = Ui.LINE_H * 2 + 8;
                int nameW = Math.max(40, cur.w - btnArea - 10);
                var font = net.minecraft.client.Minecraft.getInstance().font;
                for (String name : names) {
                    CustomDropsConfig c = ImportLibrary.load(name);
                    String counts = c == null ? "?" : countsFor(c);
                    pane.addLabel(cur.x, cur.y, font.plainSubstrByWidth(name, nameW), Ui.TEXT);
                    pane.addLabel(cur.x, cur.y + Ui.LINE_H + 2, font.plainSubstrByWidth(counts, nameW), Ui.DIM);
                    int btnY = cur.y + (rowH - Ui.BUTTON_H) / 2;
                    int bx = cur.x + cur.w - btnArea;
                    pane.addButton(cur, bx, btnY, w0, "Stage",
                        () -> stageFromLibrary(name));
                    pane.addButton(cur, bx + w0 + gap, btnY, w1, "Rename",
                        () -> doRename(name));
                    pane.addButton(cur, bx + w0 + w1 + 2 * gap, btnY, w2, "Delete",
                        () -> doDelete(name));
                    cur.y += rowH + 6;
                }
            }

            pane.addDivider(cur.x, cur.y, cur.w);
            cur.y += 10;
            pane.addLabel(cur.x, cur.y, "Actions", Ui.SECTION_TEXT);
            cur.y += Ui.LINE_H + 4;
            pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Import a Code", () ->
                this.minecraft.gui.setScreen(new ExportImportScreen(this)));
            cur.y += Ui.BUTTON_H + 4;
            pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Save Current Config as an Import",
                this::saveCurrentAsImport);
            cur.y += Ui.BUTTON_H + 4;
            pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Clear All Rules", this::clearAllRules);
            cur.y += Ui.BUTTON_H + 8;

            pane.noteCursorY(cur.y);
            pane.finish(8);
            return;
        }

        pane.addLabel(cur.x, cur.y, "Import: " + stagedName, Ui.TEXT);
        cur.y += Ui.LINE_H + 6;
        pane.addLabel(cur.x, cur.y, "Not applied yet. Pick where to apply it.", Ui.MUTED);
        cur.y += Ui.LINE_H + 8;

        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 10;

        pane.addLabel(cur.x, cur.y, "Apply where:", Ui.MUTED);
        cur.y += Ui.LINE_H + 6;
        pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Apply to Active Config", this::applyToActive);
        cur.y += Ui.BUTTON_H + 4;
        if (inSinglePlayer()) {
            pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Apply to This World", this::applyToWorld);
            cur.y += Ui.BUTTON_H + 4;
        }
        if (ServerConfigCache.connected() && ServerConfigCache.canEdit()) {
            pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Send to Server", this::sendToServer);
            cur.y += Ui.BUTTON_H + 4;
        }
        pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Discard Staging", this::discardStaging);
        cur.y += Ui.BUTTON_H + 8;

        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 10;

        ruleSection(cur, "Mob drops", staging.mobDrops(), mobLabels(), i ->
            openStaged(new MobDropsScreen(this, () -> staging, LootRuleScreen.Mode.STAGING),
                mobLabels().get(i)));
        ruleSection(cur, "Block drops", staging.blockDrops(), blockLabels(), i ->
            openStaged(new BlockDropsScreen(this, () -> staging, LootRuleScreen.Mode.STAGING),
                blockLabels().get(i)));
        ruleSection(cur, "Chest loot", staging.chestLoot(), chestLabels(), i ->
            openStaged(new ChestLootScreen(this, () -> staging, LootRuleScreen.Mode.STAGING),
                chestLabels().get(i)));
        ruleSection(cur, "Fishing loot", staging.fishingLoot(), fishingLabels(), i ->
            openStaged(new FishingLootScreen(this, () -> staging, LootRuleScreen.Mode.STAGING),
                fishingLabels().get(i)));
        ruleSection(cur, "Equipment", staging.equipmentOverrides(), equipmentLabels(), i ->
            openStaged(new EquipmentScreen(this, () -> staging, LootRuleScreen.Mode.STAGING),
                equipmentLabels().get(i)));

        pane.noteCursorY(cur.y);
        pane.finish(8);
        hubStamp = fingerprint();
    }

    private void ruleSection(ScrollablePane.Cursor cur, String title,
                             java.util.List<?> rules, java.util.List<String> labels,
                             java.util.function.IntConsumer onEdit) {
        pane.addLabel(cur.x, cur.y, title + "  (" + rules.size() + ")", Ui.SECTION_TEXT);
        cur.y += Ui.LINE_H + 2;
        if (rules.isEmpty()) {
            pane.addLabel(cur.x + 6, cur.y, "  (no rules)", Ui.DIM);
            cur.y += Ui.LINE_H + 6;
            return;
        }
        int editW = 52;
        int labelW = Math.max(40, cur.w - editW - 6);
        var font = net.minecraft.client.Minecraft.getInstance().font;
        for (int i = 0; i < labels.size(); i++) {
            String shown = font.plainSubstrByWidth(labels.get(i), labelW);
            pane.addLabel(cur.x, cur.y + 5, shown, Ui.TEXT);
            int idx = i;
            pane.addButton(cur, cur.x + cur.w - editW, cur.y, editW, "Edit", () -> onEdit.accept(idx));
            cur.y += Ui.BUTTON_H + 3;
        }
        cur.y += 6;
    }

    private static String countsFor(CustomDropsConfig c) {
        return "M " + c.mobDrops().size() + " B " + c.blockDrops().size()
            + " C " + c.chestLoot().size() + " F " + c.fishingLoot().size()
            + " E " + c.equipmentOverrides().size();
    }

    private void openStaged(net.minecraft.client.gui.screens.Screen screen, String preSelectLabel) {
        if (screen instanceof SplitPaneScreen sp) sp.preSelect(preSelectLabel);
        selectedKey = null;
        this.minecraft.gui.setScreen(screen);
    }

    private java.util.List<String> mobLabels() {
        return staging.mobDrops().stream()
            .map(e -> LootRuleScreen.displayId(e.targetId(), e.isTag()))
            .toList();
    }

    private java.util.List<String> blockLabels() {
        return staging.blockDrops().stream()
            .map(e -> LootRuleScreen.displayId(e.targetId(), e.isTag()))
            .toList();
    }

    private java.util.List<String> chestLabels() {
        return staging.chestLoot().stream()
            .map(e -> {
                String t = e.targetLootTableId();
                return t == null || t.isEmpty() ? "(untitled)" : t;
            })
            .toList();
    }

    private java.util.List<String> fishingLabels() {
        return staging.fishingLoot().stream()
            .map(e -> {
                String t = e.targetLootTableId();
                return t == null || t.isEmpty() ? "(untitled)" : t;
            })
            .toList();
    }

    private java.util.List<String> equipmentLabels() {
        return staging.equipmentOverrides().stream()
            .map(e -> {
                String t = e.targetEntityId();
                String id = t != null && !t.isEmpty() ? t : "(untitled)";
                if (e.isTag() && !id.startsWith("#")) id = "#" + id;
                return id + " " + e.slot();
            })
            .toList();
    }

    private long fingerprint() {
        CustomDropsConfig s = staging;
        if (s == null) return 0;
        long h = 7;
        h = h * 31 + s.mobDrops().size();
        h = h * 31 + s.blockDrops().size();
        h = h * 31 + s.chestLoot().size();
        h = h * 31 + s.fishingLoot().size();
        h = h * 31 + s.equipmentOverrides().size();
        return h;
    }

    private long hubStamp = -1;

    private void refreshNav() {
        if (navWidget == null) return;
        NavigationWidget.NavState state = navWidget.snapshotState();
        navWidget.clear();
        buildNavigation();
        navWidget.restoreState(state);
        if (selectedKey != null) {
            navWidget.setSelected(selectedKey);
        }
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (staged && pane != null && fingerprint() != hubStamp) {
            refreshNav();
            buildHub();
        }
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

    @Override
    protected void onDiscarded() {
        selectedKey = null;
        statusMessage = "";
        refreshScreen();
    }
}