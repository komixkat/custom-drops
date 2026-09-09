package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.client.gui.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class PromptDialogScreen extends Screen {

    private static final int DIALOG_W = 360;
    private static final int DIALOG_H = 128;

    private final Screen parent;
    private final String title;
    private String initial;
    private final String okLabel;
    private final Consumer<String> onOk;

    private EditBox field;

    public PromptDialogScreen(Screen parent, String title, String initial, String okLabel, Consumer<String> onOk) {
        super(Component.literal(title));
        this.parent = parent;
        this.title = title;
        this.initial = initial == null ? "" : initial;
        this.okLabel = okLabel;
        this.onOk = onOk;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int w = 120;
        int buttonY = (height / 2) + DIALOG_H / 2 - 26;
        int fieldX = cx - 140;
        int fieldW = 280;

        field = new EditBox(font, fieldX, (height / 2) - 6, fieldW, 20, Component.literal(""));
        field.setMaxLength(64);
        field.setValue(initial);
        addRenderableWidget(field);
        setInitialFocus(field);

        addRenderableWidget(Button.builder(Component.literal(okLabel), b ->
            ok()
        ).bounds(cx + 4, buttonY, w, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b ->
            this.minecraft.gui.setScreen(parent)
        ).bounds(cx - DIALOG_W / 2 + 12, buttonY, w, 20).build());
    }

    private void ok() {
        String value = field.getValue() == null ? "" : field.getValue().trim();
        if (value.isEmpty()) return;
        onOk.accept(value);
        this.minecraft.gui.setScreen(parent);
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

        guiGraphics.centeredText(font, Component.literal(title), cx, by + 22, Ui.TEXT);

        for (GuiEventListener child : children()) {
            if (child instanceof Renderable r) {
                r.extractRenderState(guiGraphics, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == 257 || event.key() == 335) { // ENTER / KP_ENTER
            ok();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}