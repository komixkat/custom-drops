package com.komixkat.customdrops.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CustomDropsConfigCategoriesScreen extends Screen {

    private static final int BUTTON_WIDTH = 204;
    private final Screen parent;
    private final boolean worldMode;

    public CustomDropsConfigCategoriesScreen(Screen parent, boolean worldMode) {
        super(Component.translatable(worldMode ? "customdrops.menu.worldSettings" : "customdrops.menu.config"));
        this.parent = parent;
        this.worldMode = worldMode;
    }

    @Override
    protected void init() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4, 4, 4, 0);
        GridLayout.RowHelper helper = gridLayout.createRowHelper(1);

        helper.addChild(Button.builder(Component.translatable("customdrops.config.category.mobDrops"),
            b -> this.minecraft.gui.setScreen(CustomDropsScreens.buildMobDropsScreen(this, worldMode))).width(BUTTON_WIDTH).build());

        helper.addChild(Button.builder(Component.translatable("customdrops.config.category.blockDrops"),
            b -> this.minecraft.gui.setScreen(CustomDropsScreens.buildBlockDropsScreen(this, worldMode))).width(BUTTON_WIDTH).build());

        helper.addChild(Button.builder(Component.translatable("customdrops.config.category.chestLoot"),
            b -> this.minecraft.gui.setScreen(CustomDropsScreens.buildChestLootScreen(this, worldMode))).width(BUTTON_WIDTH).build());

        helper.addChild(Button.builder(Component.translatable("customdrops.config.category.fishingLoot"),
            b -> this.minecraft.gui.setScreen(CustomDropsScreens.buildFishingLootScreen(this, worldMode))).width(BUTTON_WIDTH).build());

        helper.addChild(Button.builder(Component.translatable("customdrops.config.category.equipment"),
            b -> this.minecraft.gui.setScreen(CustomDropsScreens.buildEquipmentScreen(this, worldMode))).width(BUTTON_WIDTH).build());

        if (worldMode) {
            helper.addChild(Button.builder(Component.translatable("customdrops.config.category.presets"),
                b -> this.minecraft.gui.setScreen(CustomDropsScreens.buildPresetsScreen(this, true))).width(BUTTON_WIDTH).build());
        } else {
            helper.addChild(Button.builder(Component.translatable("customdrops.config.category.presets"),
                b -> this.minecraft.gui.setScreen(CustomDropsScreens.buildPresetsScreen(this, false))).width(BUTTON_WIDTH).build());
        }

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
