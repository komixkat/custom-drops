package com.komixkat.customdrops.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CustomDropsRootScreen extends Screen {

    private static final int BUTTON_WIDTH = 204;
    private final Screen parent;

    public CustomDropsRootScreen(Screen parent) {
        super(Component.translatable("customdrops.menu.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4, 4, 4, 0);
        GridLayout.RowHelper helper = gridLayout.createRowHelper(1);

        helper.addChild(Button.builder(Component.translatable("customdrops.menu.info"),
            b -> this.minecraft.gui.setScreen(CustomDropsScreens.buildInfoScreen(this))).width(BUTTON_WIDTH).build());

        helper.addChild(Button.builder(Component.translatable("customdrops.menu.settings"),
            b -> this.minecraft.gui.setScreen(CustomDropsScreens.buildSettingsScreen(this))).width(BUTTON_WIDTH).build());

        helper.addChild(Button.builder(Component.translatable("customdrops.menu.config"),
            b -> this.minecraft.gui.setScreen(new CustomDropsConfigCategoriesScreen(this, false))).width(BUTTON_WIDTH).build());

        helper.addChild(Button.builder(Component.translatable("customdrops.menu.worldSettings"),
            b -> this.minecraft.gui.setScreen(new CustomDropsConfigCategoriesScreen(this, true))).width(BUTTON_WIDTH).build());

        helper.addChild(Button.builder(Component.translatable("customdrops.menu.serverConfig"),
            b -> this.minecraft.gui.setScreen(CustomDropsScreens.buildServerConfigScreen(this))).width(BUTTON_WIDTH).build());

        helper.addChild(Button.builder(Component.translatable("customdrops.menu.search"),
            b -> this.minecraft.gui.setScreen(CustomDropsScreens.buildSearchScreen(this))).width(BUTTON_WIDTH).build());

        helper.addChild(Button.builder(Component.translatable("gui.done"),
            b -> this.minecraft.gui.setScreen(this.parent)).width(BUTTON_WIDTH).build());

        gridLayout.arrangeElements();
        FrameLayout.alignInRectangle(gridLayout, 0, 0, this.width, this.height, 0.5F, 0.5F);
        gridLayout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
