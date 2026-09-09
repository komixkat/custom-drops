package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.client.gui.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ConfirmDialogScreen extends Screen {

    private static final int PADDING_X = 16;
    private static final int TITLE_Y_OFFSET = 22;
    private static final int TEXT_START_OFFSET = 44;
    private static final int LINE_H = 14;
    private static final int BUTTON_AREA_HEIGHT = 30;
    private static final int MIN_DIALOG_W = 300;
    private static final int MAX_DIALOG_W = 460;

    private final Screen parent;
    private final String title;
    private final List<String> lines;
    private final String confirmLabel;
    private final Runnable onConfirm;

    private int dialogW;
    private int dialogH;

    public ConfirmDialogScreen(Screen parent, String title, List<String> lines, String confirmLabel, Runnable onConfirm) {
        super(Component.literal(title));
        this.parent = parent;
        this.title = title;
        this.lines = lines == null ? List.of() : lines;
        this.confirmLabel = confirmLabel;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        var f = net.minecraft.client.Minecraft.getInstance().font;
        int maxTextW = 0;
        for (String line : lines) {
            maxTextW = Math.max(maxTextW, f.width(line));
        }
        maxTextW = Math.max(maxTextW, f.width(title));

        dialogW = Math.max(MIN_DIALOG_W, Math.min(MAX_DIALOG_W, maxTextW + PADDING_X * 2 + 20));
        int textH = lines.size() * LINE_H;
        dialogH = TITLE_Y_OFFSET + textH + BUTTON_AREA_HEIGHT + 20;

        int cx = width / 2;
        int cy = height / 2;
        int bx = cx - dialogW / 2;
        int by = cy - dialogH / 2;
        int buttonY = by + dialogH - 26;
        int btnW = 120;

        addRenderableWidget(Button.builder(Component.literal(confirmLabel), b -> {
            onConfirm.run();
            this.minecraft.gui.setScreen(parent);
        }).bounds(cx + 4, buttonY, btnW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b ->
            this.minecraft.gui.setScreen(parent)
        ).bounds(cx - btnW - 4, buttonY, btnW, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(guiGraphics);
        int cx = width / 2;
        int cy = height / 2;

        int bx = cx - dialogW / 2;
        int by = cy - dialogH / 2;

        guiGraphics.fill(bx, by, bx + dialogW, by + dialogH, 0xFF2A2A2E);
        guiGraphics.fill(bx, by, bx + dialogW, by + 2, Ui.SELECT_BG);

        guiGraphics.centeredText(font, Component.literal(title), cx, by + TITLE_Y_OFFSET, Ui.TEXT);
        int lineY = by + TEXT_START_OFFSET;
        for (String line : lines) {
            String clipped = font.plainSubstrByWidth(line, dialogW - PADDING_X * 2);
            guiGraphics.centeredText(font, Component.literal(clipped), cx, lineY, Ui.MUTED);
            lineY += LINE_H;
        }

        for (GuiEventListener child : children()) {
            if (child instanceof Renderable r) {
                r.extractRenderState(guiGraphics, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}
