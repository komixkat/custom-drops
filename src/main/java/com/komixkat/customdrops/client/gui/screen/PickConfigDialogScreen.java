package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.client.gui.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class PickConfigDialogScreen extends Screen {

    private static final int MIN_W = 300;
    private static final int MAX_W = 460;
    private static final int ROW_H = 24;

    private final Screen parent;
    private final String title;
    private final List<String> options;
    private final String active;
    private final Consumer<String> onPick;

    public PickConfigDialogScreen(Screen parent, String title, List<String> options, String active,
                                  Consumer<String> onPick) {
        super(Component.literal(title));
        this.parent = parent;
        this.title = title;
        this.options = options;
        this.active = active;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        int dialogW = dialogWidth();
        int dialogH = dialogHeight();
        int cx = width / 2;
        int by = height / 2 - dialogH / 2;
        int bx = cx - dialogW / 2;
        int y = by + 32;
        for (String option : options) {
            final String picked = option;
            addRenderableWidget(Button.builder(Component.literal(labelFor(picked)), b -> {
                onPick.accept(picked);
                this.minecraft.gui.setScreen(parent);
            }).bounds(bx + 16, y, dialogW - 32, 20).build());
            y += ROW_H;
        }
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b ->
            this.minecraft.gui.setScreen(parent)
        ).bounds(cx - 60, y + 4, 120, 20).build());
    }

    private String labelFor(String option) {
        return option.equals(active) ? option + "  [ACTIVE]" : option;
    }

    private int dialogWidth() {
        var f = net.minecraft.client.Minecraft.getInstance().font;
        int maxW = f.width(title);
        for (String option : options) {
            maxW = Math.max(maxW, f.width(labelFor(option)));
        }
        return Math.max(MIN_W, Math.min(MAX_W, maxW + 40));
    }

    private int dialogHeight() {
        return 40 + options.size() * ROW_H + 40;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(guiGraphics);
        int dialogW = dialogWidth();
        int dialogH = dialogHeight();
        int cx = width / 2;
        int by = height / 2 - dialogH / 2;
        int bx = cx - dialogW / 2;

        guiGraphics.fill(bx, by, bx + dialogW, by + dialogH, 0xFF2A2A2E);
        guiGraphics.fill(bx, by, bx + dialogW, by + 2, Ui.SELECT_BG);
        guiGraphics.centeredText(font, Component.literal(title), cx, by + 20, Ui.TEXT);

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