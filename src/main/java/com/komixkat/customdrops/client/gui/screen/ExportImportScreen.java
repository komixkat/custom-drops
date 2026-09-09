package com.komixkat.customdrops.client.gui.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.widget.NavigationWidget;
import com.komixkat.customdrops.client.gui.widget.ScrollablePane;
import com.komixkat.customdrops.config.ConfigProfiles;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.mojang.blaze3d.platform.ClipboardManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.Base64;

public final class ExportImportScreen extends SplitPaneScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private EditBox importField;
    private EditBox exportField;
    private ScrollablePane pane;
    private String statusMessage = "";
    private boolean isError = false;

    private CustomDropsConfig staging;
    private long stageStamp = -1;
    private String applyStatus = "";

    public ExportImportScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.config.exportImport"));
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
        navWidget.addCategory("Actions");
        navWidget.addEntry("Actions", "Export Current Config", this::doExport);
        navWidget.addEntry("Actions", "Stage Base64 Import", this::doStageImport);
        if (staging != null) {
            navWidget.addCategory("Staged Import");
            navWidget.addEntry("Staged Import", "Mob Drops (" + size(staging.mobDrops()) + ")", () ->
                this.minecraft.gui.setScreen(new MobDropsScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING)));
            navWidget.addEntry("Staged Import", "Block Drops (" + size(staging.blockDrops()) + ")", () ->
                this.minecraft.gui.setScreen(new BlockDropsScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING)));
            navWidget.addEntry("Staged Import", "Chest Loot (" + size(staging.chestLoot()) + ")", () ->
                this.minecraft.gui.setScreen(new ChestLootScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING)));
            navWidget.addEntry("Staged Import", "Fishing Loot (" + size(staging.fishingLoot()) + ")", () ->
                this.minecraft.gui.setScreen(new FishingLootScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING)));
            navWidget.addEntry("Staged Import", "Equipment (" + size(staging.equipmentOverrides()) + ")", () ->
                this.minecraft.gui.setScreen(new EquipmentScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING)));
            navWidget.addEntry("Staged Import", "Apply to Active Config", this::applyStaged);
            navWidget.addEntry("Staged Import", "Save As New Profile", this::saveAsProfile);
            navWidget.addEntry("Staged Import", "Discard Staging", this::discardStaging);
        }
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

        pane.addLabel(cur.x, cur.y, "Export", Ui.TEXT);
        cur.y += Ui.LINE_H + 6;
        pane.addLabel(cur.x, cur.y, "Base64 snapshot of the active config.", Ui.MUTED);
        cur.y += Ui.LINE_H + 6;
        pane.addButton(cur, cur.x, cur.y, Math.min(220, cur.w), "Copy Active Config to Clipboard", this::doExport);
        cur.y += Ui.BUTTON_H + 8;

        exportField = pane.addEditBox(cur.x, cur.y, cur.w, "", "Exported Base64 appears here", null);
        exportField.setEditable(false);
        exportField.setMaxLength(Integer.MAX_VALUE);
        exportField.setHeight(60);
        cur.y += 64;

        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 10;

        pane.addLabel(cur.x, cur.y, "Import", Ui.TEXT);
        cur.y += Ui.LINE_H + 6;
        pane.addLabel(cur.x, cur.y, "Paste a base64-encoded config string below.", Ui.MUTED);
        cur.y += Ui.LINE_H + 6;
        pane.addLabel(cur.x, cur.y, "It is staged for review and editing first,", Ui.DIM);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "then you decide to apply it or keep it as a profile.", Ui.DIM);
        cur.y += Ui.LINE_H + 6;

        importField = pane.addEditBox(cur.x, cur.y, cur.w, "", "Base64 config", null);
        importField.setMaxLength(Integer.MAX_VALUE);
        cur.y += Ui.FIELD_H + 8;

        pane.addButton(cur, cur.x, cur.y, Math.min(220, cur.w), "Decode & Stage for Review", this::doStageImport);
        cur.y += Ui.BUTTON_H + 10;

        if (staging != null) {
            renderStaged(cur);
        }

        pane.noteCursorY(cur.y);
        pane.finish(8);
    }

    private void renderStaged(ScrollablePane.Cursor cur) {
        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 12;
        pane.addLabel(cur.x, cur.y, "Staged import review — NOT applied to your config yet", Ui.SECTION_TEXT);
        cur.y += Ui.LINE_H + 4;
        pane.addLabel(cur.x, cur.y, "Mob: " + size(staging.mobDrops()) + "  \u00B7  Block: " + size(staging.blockDrops())
            + "  \u00B7  Chest: " + size(staging.chestLoot()) + "  \u00B7  Fishing: " + size(staging.fishingLoot())
            + "  \u00B7  Equipment: " + size(staging.equipmentOverrides()), Ui.MUTED);
        cur.y += Ui.LINE_H + 4;
        pane.addLabel(cur.x, cur.y, "Open a category from the left panel to review or edit it.", Ui.DIM);
        cur.y += Ui.LINE_H + 8;

        pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Edit Mob Drops (" + size(staging.mobDrops()) + ")", () ->
            this.minecraft.gui.setScreen(new MobDropsScreen(this, () -> staging, LootRuleScreen.Mode.STAGING)));
        pane.addButton(cur, cur.x, cur.y + Ui.BUTTON_H + 4, Math.min(260, cur.w), "Edit Block Drops (" + size(staging.blockDrops()) + ")", () ->
            this.minecraft.gui.setScreen(new BlockDropsScreen(this, () -> staging, LootRuleScreen.Mode.STAGING)));
        pane.addButton(cur, cur.x, cur.y + 2 * (Ui.BUTTON_H + 4), Math.min(260, cur.w), "Edit Chest Loot (" + size(staging.chestLoot()) + ")", () ->
            this.minecraft.gui.setScreen(new ChestLootScreen(this, () -> staging, LootRuleScreen.Mode.STAGING)));
        pane.addButton(cur, cur.x, cur.y + 3 * (Ui.BUTTON_H + 4), Math.min(260, cur.w), "Edit Fishing Loot (" + size(staging.fishingLoot()) + ")", () ->
            this.minecraft.gui.setScreen(new FishingLootScreen(this, () -> staging, LootRuleScreen.Mode.STAGING)));
        pane.addButton(cur, cur.x, cur.y + 4 * (Ui.BUTTON_H + 4), Math.min(260, cur.w), "Edit Equipment (" + size(staging.equipmentOverrides()) + ")", () ->
            this.minecraft.gui.setScreen(new EquipmentScreen(this, () -> staging, LootRuleScreen.Mode.STAGING)));
        cur.y += 5 * (Ui.BUTTON_H + 4) + 8;

        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 10;

        pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Apply Staged Import to Active Config", this::applyStaged);
        cur.y += Ui.BUTTON_H + 6;
        pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Save Staged Import as New Profile...", this::saveAsProfile);
        cur.y += Ui.BUTTON_H + 6;
        pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Discard Staging", this::discardStaging);
        cur.y += Ui.BUTTON_H + 8;
    }

    private static int size(java.util.List<?> list) {
        return list == null ? 0 : list.size();
    }

    private long fingerprint() {
        if (staging == null) return -1;
        long h = 7;
        h = h * 31 + size(staging.mobDrops());
        h = h * 31 + size(staging.blockDrops());
        h = h * 31 + size(staging.chestLoot());
        h = h * 31 + size(staging.fishingLoot());
        h = h * 31 + size(staging.equipmentOverrides());
        return h;
    }

    private void refreshNav() {
        if (navWidget == null) return;
        NavigationWidget.NavState state = navWidget.snapshotState();
        navWidget.clear();
        buildNavigation();
        navWidget.restoreState(state);
    }

    private void doExport() {
        String encoded = encodeConfig(CustomDropsMod.config());
        if (exportField != null) {
            exportField.setValue(encoded);
        }
        try {
            new com.mojang.blaze3d.platform.ClipboardManager()
                .setClipboard(net.minecraft.client.Minecraft.getInstance().getWindow(), encoded);
        } catch (Throwable t) {
            // clipboard unavailable, still shown in the field
        }
        statusMessage = "Active config encoded and copied to clipboard.";
        isError = false;
    }

    private void doStageImport() {
        if (importField == null) return;

        // First try clipboard (handles large configs that exceed EditBox limits)
        String clipboard = "";
        try {
            clipboard = new ClipboardManager()
                .getClipboard(net.minecraft.client.Minecraft.getInstance().getWindow(), null);
        } catch (Throwable ignored) {}
        String input = (clipboard != null && !clipboard.isBlank()) ? clipboard.trim() : importField.getValue();

        if (input == null || input.isBlank()) {
            statusMessage = "Please paste a base64-encoded config string first.";
            isError = true;
            return;
        }
        CustomDropsConfig decoded = decodeConfig(input.trim());
        if (decoded == null) {
            // If clipboard decode failed, try the field value as fallback
            if (clipboard != null && !clipboard.isBlank() && importField != null) {
                String fieldVal = importField.getValue();
                if (fieldVal != null && !fieldVal.isBlank() && !fieldVal.equals(input)) {
                    decoded = decodeConfig(fieldVal.trim());
                }
            }
            if (decoded == null) {
                statusMessage = "Invalid config data. Could not decode or parse it.";
                isError = true;
                return;
            }
        }
        staging = decoded;
        statusMessage = "Staged the imported config for review. No changes applied yet.";
        isError = false;
        refreshNav();
        buildHub();
    }

    private void applyStaged() {
        if (staging == null) return;
        CustomDropsConfig config = CustomDropsMod.config();
        config.clearAll();
        config.setSchemaVersion(staging.schemaVersion());
        config.setActivePreset("");
        config.setMobDropsEnabled(staging.mobDropsEnabled());
        config.setBlockDropsEnabled(staging.blockDropsEnabled());
        config.setChestLootEnabled(staging.chestLootEnabled());
        config.setFishingLootEnabled(staging.fishingLootEnabled());
        config.setEquipmentOverridesEnabled(staging.equipmentOverridesEnabled());
        config.mobDrops().addAll(staging.mobDrops());
        config.blockDrops().addAll(staging.blockDrops());
        config.chestLoot().addAll(staging.chestLoot());
        config.fishingLoot().addAll(staging.fishingLoot());
        config.equipmentOverrides().addAll(staging.equipmentOverrides());
        markDirty();
        statusMessage = "Staged import applied to the active config in memory. Press Save to write it to disk.";
        isError = false;
        buildHub();
    }

    private void saveAsProfile() {
        if (staging == null) return;
        this.minecraft.gui.setScreen(new PromptDialogScreen(this, "Save staged import as new config profile",
            "Imported config", "Create Profile", name -> {
                if (ConfigProfiles.create(name, staging)) {
                    statusMessage = "Saved staged import as profile \"" + name + "\". Switch to it in Configs to use it.";
                    isError = false;
                } else {
                    statusMessage = "Could not create profile \"" + name + "\" (the name may be invalid or taken).";
                    isError = true;
                }
                buildHub();
            }));
    }

    private void discardStaging() {
        staging = null;
        statusMessage = "Staged import discarded.";
        isError = false;
        refreshNav();
        buildHub();
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (staging != null && fingerprint() != stageStamp) {
            stageStamp = fingerprint();
            buildHub();
        }
        if (pane != null) {
            pane.render(guiGraphics, mouseX, mouseY, delta);
        }
        if (!statusMessage.isEmpty() && pane != null) {
            drawStatus(guiGraphics, statusMessage, isError ? Ui.ERROR : Ui.ACCENT);
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

    private static String encodeConfig(CustomDropsConfig config) {
        String json = GSON.toJson(config);
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    private static CustomDropsConfig decodeConfig(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            String json = new String(decoded);
            return GSON.fromJson(json, CustomDropsConfig.class);
        } catch (Exception e) {
            CustomDropsMod.LOGGER.error("Failed to decode config", e);
            return null;
        }
    }
}