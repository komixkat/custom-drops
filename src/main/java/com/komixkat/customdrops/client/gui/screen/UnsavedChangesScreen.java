package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.client.gui.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class UnsavedChangesScreen extends Screen {

    private static final int DIALOG_W = 340;
    private static final int DIALOG_H = 128;

    private final SplitPaneScreen host;

    public UnsavedChangesScreen(SplitPaneScreen host) {
        super(Component.translatable("customdrops.menu.unsaved.title"));
        this.host = host;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        int buttonY = cy + 22;
        int w = 100;
        int gap = 8;
        int left = cx - DIALOG_W / 2 + 12;

        addRenderableWidget(Button.builder(
            Component.translatable("customdrops.menu.unsaved.save"),
            b -> {
                host.save();
                this.minecraft.gui.setScreen(host.parent());
            }
        ).bounds(left, buttonY, w, 20).build());

        addRenderableWidget(Button.builder(
            Component.translatable("customdrops.menu.unsaved.cancel"),
            b -> this.minecraft.gui.setScreen(host)
        ).bounds(left + w + gap, buttonY, w, 20).build());

        addRenderableWidget(Button.builder(
            Component.translatable("customdrops.menu.unsaved.discard"),
            b -> {
                host.discard();
                this.minecraft.gui.setScreen(host.parent());
            }
        ).bounds(left + 2 * (w + gap), buttonY, w, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(guiGraphics);
        int cx = width / 2;
        int cy = height / 2;

        int bx = cx - DIALOG_W / 2;
        int by = cy - DIALOG_H / 2;

        guiGraphics.fill(bx, by, bx + DIALOG_W, by + DIALOG_H, 0xFF2A2A2E);
        guiGraphics.fill(bx, by, bx + DIALOG_W, by + 2, Ui.SELECT_BG);

        guiGraphics.centeredText(font, Component.translatable("customdrops.menu.unsaved.title"), cx, by + 20, Ui.TEXT);
        guiGraphics.centeredText(font, Component.translatable("customdrops.menu.unsaved.message"), cx, by + 42, Ui.MUTED);
        guiGraphics.centeredText(font, Component.literal("If you continue without saving, your changes are lost."), cx, by + 56, Ui.DIM);

        for (GuiEventListener child : children()) {
            if (child instanceof Renderable r) {
                r.extractRenderState(guiGraphics, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(host);
    }
}