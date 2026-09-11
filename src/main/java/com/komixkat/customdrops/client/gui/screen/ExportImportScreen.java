package com.komixkat.customdrops.client.gui.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.widget.ScrollablePane;
import com.komixkat.customdrops.config.ConfigProfiles;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.config.ImportLibrary;
import com.mojang.blaze3d.platform.ClipboardManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ExportImportScreen extends SplitPaneScreen {

    private static final Gson GSON = new GsonBuilder().create();
    private EditBox importField;
    private EditBox exportField;
    private ScrollablePane pane;
    private String statusMessage = "";
    private boolean isError = false;

    public ExportImportScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.config.exportImport"));
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
        navWidget.addCategory("Actions");
        navWidget.addEntry("Actions", "Copy Active Config", this::doExport);
        navWidget.addEntry("Actions", "Import a Code", this::doImport);
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
        pane.addLabel(cur.x, cur.y, "Copies your config as a code you can paste in chat or a file.", Ui.MUTED);
        cur.y += Ui.LINE_H + 8;
        pane.addButton(cur, cur.x, cur.y, Math.min(220, cur.w), "Copy Active Config", this::doExport);
        cur.y += Ui.BUTTON_H + 8;

        exportField = pane.addEditBox(cur.x, cur.y, cur.w, "", "Code appears here", null);
        exportField.setEditable(false);
        exportField.setMaxLength(Integer.MAX_VALUE);
        exportField.setHeight(60);
        cur.y += 64;

        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 10;

        pane.addLabel(cur.x, cur.y, "Import", Ui.TEXT);
        cur.y += Ui.LINE_H + 6;
        pane.addLabel(cur.x, cur.y, "Paste a code, then name it. It is saved as an import", Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "and applied later from the Imports screen.", Ui.MUTED);
        cur.y += Ui.LINE_H + 8;

        importField = pane.addEditBox(cur.x, cur.y, cur.w, "", "Paste a code here", null);
        importField.setMaxLength(Integer.MAX_VALUE);
        cur.y += Ui.FIELD_H + 8;

        pane.addButton(cur, cur.x, cur.y, Math.min(220, cur.w), "Decode & Save as Import", this::doImport);
        cur.y += Ui.BUTTON_H + 8;
        pane.addLabel(cur.x, cur.y, "Imports never change your config until you apply them.", Ui.DIM);

        pane.noteCursorY(cur.y);
        pane.finish(8);
    }

    private void doExport() {
        String encoded = encodeConfig(CustomDropsMod.config());
        if (encoded == null) {
            statusMessage = "Could not encode the config.";
            isError = true;
            return;
        }
        if (exportField != null) {
            exportField.setValue(encoded);
        }
        try {
            new ClipboardManager()
                .setClipboard(net.minecraft.client.Minecraft.getInstance().getWindow(), encoded);
        } catch (Throwable t) {
            // clipboard unavailable, code is still shown in the field
        }
        statusMessage = "Code copied.";
        isError = false;
    }

    private void doImport() {
        if (importField == null) return;

        String clipboard = "";
        try {
            clipboard = new ClipboardManager()
                .getClipboard(net.minecraft.client.Minecraft.getInstance().getWindow(), null);
        } catch (Throwable ignored) {}
        String input = (clipboard != null && !clipboard.isBlank()) ? clipboard.trim() : importField.getValue();

        if (input == null || input.isBlank()) {
            statusMessage = "Paste a code first.";
            isError = true;
            return;
        }
        CustomDropsConfig decoded = decodeConfig(input.trim());
        if (decoded == null && clipboard != null && !clipboard.isBlank() && importField != null) {
            String fieldVal = importField.getValue();
            if (fieldVal != null && !fieldVal.isBlank() && !fieldVal.equals(input)) {
                decoded = decodeConfig(fieldVal.trim());
            }
        }
        if (decoded == null) {
            statusMessage = "That code is invalid.";
            isError = true;
            return;
        }
        final CustomDropsConfig staged = decoded;
        this.minecraft.gui.setScreen(new PromptDialogScreen(this, "Save import as",
            ImportLibrary.suggestedName("Imported config"), "Save", name -> {
                String clean = name == null ? "" : name.trim();
                if (!ImportLibrary.save(clean, staged)) {
                    statusMessage = "Could not save \"" + clean + "\" (invalid or already used).";
                    isError = true;
                } else {
                    statusMessage = "Saved \"" + clean + "\" as an import.";
                    isError = false;
                    this.minecraft.gui.setScreen(new ImportsScreen(this, clean, staged));
                }
            }));
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
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

    private static String encodeConfig(CustomDropsConfig config) {
        try {
            byte[] json = GSON.toJson(config).getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
                gz.write(json);
            }
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            CustomDropsMod.LOGGER.error("Failed to encode config", e);
            return null;
        }
    }

    private static CustomDropsConfig decodeConfig(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            if (decoded.length >= 2 && (decoded[0] & 0xFF) == 0x1F && (decoded[1] & 0xFF) == 0x8B) {
                try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(decoded))) {
                    decoded = gz.readAllBytes();
                }
            }
            return GSON.fromJson(new String(decoded, StandardCharsets.UTF_8), CustomDropsConfig.class);
        } catch (Exception e) {
            CustomDropsMod.LOGGER.error("Failed to decode config", e);
            return null;
        }
    }
}