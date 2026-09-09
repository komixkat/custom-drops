package com.komixkat.customdrops.client.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RootScreen extends SplitPaneScreen {

    public RootScreen(Screen parent) {
        super(parent, Component.translatable("customdrops.menu.title"));
    }

    @Override
    protected String backButtonLabel() {
        return "Done";
    }

    @Override
    protected void buildNavigation() {
        var config = com.komixkat.customdrops.CustomDropsMod.config();
        int total = config.mobDrops().size() + config.blockDrops().size()
            + config.chestLoot().size() + config.fishingLoot().size()
            + config.equipmentOverrides().size();
        navWidget.addCategory("Drop Categories", total);
        navWidget.addEntry("Drop Categories", "Mob Drops (" + config.mobDrops().size() + ")", () ->
            this.minecraft.gui.setScreen(new MobDropsScreen(this)));
        navWidget.addEntry("Drop Categories", "Block Drops (" + config.blockDrops().size() + ")", () ->
            this.minecraft.gui.setScreen(new BlockDropsScreen(this)));
        navWidget.addEntry("Drop Categories", "Chest Loot (" + config.chestLoot().size() + ")", () ->
            this.minecraft.gui.setScreen(new ChestLootScreen(this)));
        navWidget.addEntry("Drop Categories", "Fishing Loot (" + config.fishingLoot().size() + ")", () ->
            this.minecraft.gui.setScreen(new FishingLootScreen(this)));
        navWidget.addEntry("Drop Categories", "Equipment (" + config.equipmentOverrides().size() + ")", () ->
            this.minecraft.gui.setScreen(new EquipmentScreen(this)));

        navWidget.addCategory("Tools");
        navWidget.addEntry("Tools", "Browse Loot Tables", () ->
            this.minecraft.gui.setScreen(new BrowseScreen(this)));
        navWidget.addEntry("Tools", "Default Values", () ->
            this.minecraft.gui.setScreen(new DefaultValuesScreen(this)));
        navWidget.addEntry("Tools", "Export / Import", () ->
            this.minecraft.gui.setScreen(new ExportImportScreen(this)));
        navWidget.addEntry("Tools", "Server Config", () ->
            this.minecraft.gui.setScreen(new ServerConfigScreen(this)));

        navWidget.addCategory("Configuration");
        navWidget.addEntry("Configuration", "Configs", () ->
            this.minecraft.gui.setScreen(new ConfigsScreen(this)));
        navWidget.addEntry("Configuration", "Presets", () ->
            this.minecraft.gui.setScreen(new PresetsScreen(this)));
        navWidget.addEntry("Configuration", "Settings", () ->
            this.minecraft.gui.setScreen(new SettingsScreen(this)));
    }

    @Override
    protected void initContent() {}

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.text(font, "Custom Drops", rightPanelX + 8, contentY + 8, 0xFFE0E0E0, false);
        guiGraphics.text(font, "Each category configures one kind of drop.", rightPanelX + 8, contentY + 28, 0xFF888888, false);
        guiGraphics.text(font, "Tools lets you browse vanilla loot tables, view", rightPanelX + 8, contentY + 42, 0xFF888888, false);
        guiGraphics.text(font, "defaults, and move configs around. Presets and", rightPanelX + 8, contentY + 52, 0xFF888888, false);
        guiGraphics.text(font, "Settings handle whole-config options.", rightPanelX + 8, contentY + 62, 0xFF888888, false);
    }
}
