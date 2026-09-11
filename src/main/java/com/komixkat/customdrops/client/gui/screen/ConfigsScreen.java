package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.widget.NavigationWidget;
import com.komixkat.customdrops.client.gui.widget.ScrollablePane;
import com.komixkat.customdrops.config.ConfigLoader;
import com.komixkat.customdrops.config.ConfigProfiles;
import com.komixkat.customdrops.config.CustomDropsConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigsScreen extends SplitPaneScreen {

    private ScrollablePane pane;
    private EditBox nameField;
    private String statusMessage = "";

    public ConfigsScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.menu.configs"));
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
        navWidget.addCategory("Configs");
        for (String name : ConfigProfiles.names()) {
            String label = name + "  (" + ruleCountFor(name) + ")";
            navWidget.addEntry("Configs", label, () -> doSwitch(name));
        }
        navWidget.addCategory("Actions");
        navWidget.addEntry("Actions", "New Config", this::focusNewName);
        navWidget.addEntry("Actions", "Reset to Vanilla", this::confirmReset);
    }

    private void focusNewName() {
        if (nameField != null) {
            nameField.setFocused(true);
        }
        statusMessage = "Type a name above, then press Create.";
        buildHub();
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

        String active = ConfigProfiles.active();
        pane.addLabel(cur.x, cur.y, "Configs", Ui.TEXT);
        cur.y += Ui.LINE_H + 6;
        pane.addLabel(cur.x, cur.y, "Each config is a full drop setup; one is active at a time.", Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "Switching loads that config into the editor immediately.", Ui.MUTED);
        cur.y += Ui.LINE_H + 6;

        pane.addLabel(cur.x, cur.y, "New config name:", Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        int editW = Math.max(80, cur.w - 124);
        nameField = pane.addEditBox(cur.x, cur.y, editW, "", "", v -> {});
        nameField.setMaxLength(64);
        pane.addButton(cur, cur.x + editW + 4, cur.y - 2, 120, "+ Create", this::doCreate);
        cur.y += Ui.FIELD_H + 10;

        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 10;

        for (String name : ConfigProfiles.names()) {
            boolean isActive = name.equals(active);

            pane.addLabel(cur.x, cur.y, isActive ? name + "   [ACTIVE]" : name,
                isActive ? Ui.ACCENT : Ui.TEXT);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, countsFor(name), Ui.DIM);
            cur.y += Ui.LINE_H + 8;

            int bw = 78;
            int gap = 4;
            if (isActive) {
                pane.addButton(cur, cur.x, cur.y, bw, "In use", () -> {});
            } else {
                pane.addButton(cur, cur.x, cur.y, bw, "Switch", () -> doSwitch(name));
            }
            pane.addButton(cur, cur.x + (bw + gap), cur.y, bw, "Rename", () -> doRename(name));
            pane.addButton(cur, cur.x + 2 * (bw + gap), cur.y, bw, "Duplicate", () -> doDuplicate(name));
            if (!isActive) {
                pane.addButton(cur, cur.x + 3 * (bw + gap), cur.y, bw, "Delete", () -> doDelete(name));
            }
            cur.y += Ui.BUTTON_H + 12;
        }

        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 12;

        pane.addLabel(cur.x, cur.y, "This World's Config", Ui.TEXT);
        cur.y += Ui.LINE_H + 6;
        MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        Path worldCfgDir = server == null ? null : currentWorldConfigDir(server);

        if (server == null) {
            pane.addLabel(cur.x, cur.y, "Not in a single-player world right now.", Ui.DIM);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, "Join a world to see its setting here.", Ui.DIM);
            cur.y += Ui.LINE_H + 12;
        } else {
            boolean linked = worldCfgDir != null && Files.exists(worldCfgDir.resolve("mob_drops.json"))
                || worldCfgDir != null && Files.exists(worldCfgDir.resolve("meta.json"));
            if (linked) {
                pane.addLabel(cur.x, cur.y, "Uses its own config. It ignores the active config.", Ui.WARN);
                cur.y += Ui.LINE_H + 2;
                pane.addLabel(cur.x, cur.y, "A world keeps its own config until you clear it.", Ui.DIM);
            } else {
                pane.addLabel(cur.x, cur.y, "Uses the active config: \"" + ConfigProfiles.active() + "\"", Ui.DIM);
                cur.y += Ui.LINE_H + 2;
                pane.addLabel(cur.x, cur.y, "It keeps using this config until you overwrite it.", Ui.DIM);
            }
            cur.y += Ui.LINE_H + 8;
            pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w),
                "Overwrite With a Config...", this::pickOverwriteConfig);
            cur.y += Ui.BUTTON_H + 12;
            if (linked) {
                pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w),
                    "Clear this World's Config (use Active)", this::confirmClearWorldConfig);
                cur.y += Ui.BUTTON_H + 12;
            }
        }

        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 12;

        pane.addLabel(cur.x, cur.y, "Reset. Wipes every rule in the active config.", Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "This includes the saved copy on disk.", Ui.DIM);
        cur.y += Ui.LINE_H + 6;
        pane.addButton(cur, cur.x, cur.y, Math.min(260, cur.w), "Reset Active Config to Vanilla", this::confirmReset);
        cur.y += Ui.BUTTON_H + 10;

        pane.noteCursorY(cur.y);
        pane.finish(8);
    }

    private void doCreate() {
        String name = nameField == null ? "" : nameField.getValue().trim();
        if (name.isEmpty()) {
            statusMessage = "Enter a name for the new config first.";
            return;
        }
        if (!ConfigProfiles.validName(name)) {
            statusMessage = "That name has invalid characters; use letters, numbers, spaces, - or _.";
            return;
        }
        if (ConfigProfiles.names().contains(name)) {
            statusMessage = "A config named \"" + name + "\" already exists.";
            return;
        }
        if (ConfigProfiles.create(name)) {
            statusMessage = "Created config \"" + name + "\" as a copy of the active config.";
            nameField.setValue("");
        } else {
            statusMessage = "Could not create config \"" + name + "\".";
        }
        refreshNav();
        buildHub();
    }

    private void doSwitch(String name) {
        if (name.equals(ConfigProfiles.active())) return;
        this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
            "Switch the active config to \"" + name + "\"?",
            List.of("The active config is what the editor and the",
                "running world use right now. The old config stays saved."),
            "Switch", () -> {
                if (ConfigProfiles.switchTo(name)) {
                    statusMessage = "Switched to config \"" + name + "\".";
                } else {
                    statusMessage = "Could not switch to \"" + name + "\".";
                }
                refreshNav();
                buildHub();
            }));
    }

    private void doRename(String name) {
        this.minecraft.gui.setScreen(new PromptDialogScreen(this, "Rename config \"" + name + "\"",
            name, "Rename", newName -> {
                if (ConfigProfiles.rename(name, newName)) {
                    statusMessage = "Renamed \"" + name + "\" to \"" + newName + "\".";
                } else {
                    statusMessage = "Could not rename \"" + name + "\" to \"" + newName + "\".";
                }
                refreshNav();
                buildHub();
            }));
    }

    private void doDuplicate(String name) {
        this.minecraft.gui.setScreen(new PromptDialogScreen(this, "Duplicate config \"" + name + "\" as",
            name + " copy", "Duplicate", newName -> {
                if (ConfigProfiles.duplicate(name, newName)) {
                    statusMessage = "Duplicated \"" + name + "\" as \"" + newName + "\".";
                } else {
                    statusMessage = "Could not duplicate \"" + name + "\".";
                }
                refreshNav();
                buildHub();
            }));
    }

    private void doDelete(String name) {
        this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
            "Delete config \"" + name + "\"?",
            List.of("This removes the config and all its rules from disk.", "This cannot be undone."),
            "Delete", () -> {
                if (ConfigProfiles.delete(name)) {
                    statusMessage = "Deleted config \"" + name + "\".";
                } else {
                    statusMessage = "Could not delete \"" + name + "\".";
                }
                refreshNav();
                buildHub();
            }));
    }

    private void confirmReset() {
        this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
            "Reset active config to vanilla defaults?",
            List.of("All mob, block, chest, fishing and equipment rules", "in \"" + ConfigProfiles.active() + "\" will be wiped.",
                "This cannot be undone."),
            "Reset to Vanilla", () -> {
                ConfigProfiles.resetActiveToVanilla();
                statusMessage = "Active config reset to vanilla defaults (no custom rules).";
                refreshNav();
                buildHub();
            }));
    }

    private void confirmClearWorldConfig() {
        MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            statusMessage = "Not in a single-player world right now.";
            buildHub();
            return;
        }
        this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
            "Clear this world's config?",
            List.of("The world will stop using its own drops",
                "and go back to the active config.",
                "This cannot be undone."),
            "Clear", () -> {
                if (CustomDropsMod.clearWorldConfig(server)) {
                    statusMessage = "World config cleared; it now uses the active config.";
                } else {
                    statusMessage = "No world config to clear.";
                }
                CustomDropsMod.reloadForRunningWorld();
                buildHub();
            }));
    }

    private void pickOverwriteConfig() {
        if (net.minecraft.client.Minecraft.getInstance().getSingleplayerServer() == null) {
            statusMessage = "Not in a single-player world right now.";
            buildHub();
            return;
        }
        this.minecraft.gui.setScreen(new PickConfigDialogScreen(this,
            "Overwrite this world's config with...",
            ConfigProfiles.names(), ConfigProfiles.active(),
            name -> this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
                "Overwrite with \"" + name + "\"?",
                java.util.List.of("Replaces this world's drops right now.",
                    "It cannot be rolled back from here.",
                    "Keep a backup of the world first."),
                "Overwrite",
                () -> performOverwriteWorld(name)))));
    }

    private void performOverwriteWorld(String name) {
        MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            statusMessage = "Not in a single-player world right now.";
            buildHub();
            return;
        }
        CustomDropsMod.linkConfigToWorld(server, name);
        CustomDropsMod.reloadForRunningWorld();
        statusMessage = "Overwrote this world with \"" + name + "\".";
        buildHub();
    }

    private static Path currentWorldConfigDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("customdrops");
    }

    private static int ruleCountFor(String name) {
        CustomDropsConfig c = ConfigLoader.load(ConfigProfiles.dirFor(name));
        return c.mobDrops().size() + c.blockDrops().size() + c.chestLoot().size()
            + c.fishingLoot().size() + c.equipmentOverrides().size();
    }

    private static String countsFor(String name) {
        CustomDropsConfig c = ConfigLoader.load(ConfigProfiles.dirFor(name));
        return "Mob " + c.mobDrops().size() + "  \u00B7  Block " + c.blockDrops().size()
            + "  \u00B7  Chest " + c.chestLoot().size() + "  \u00B7  Fishing " + c.fishingLoot().size()
            + "  \u00B7  Equipment " + c.equipmentOverrides().size();
    }

    private void refreshNav() {
        if (navWidget == null) return;
        NavigationWidget.NavState state = navWidget.snapshotState();
        navWidget.clear();
        buildNavigation();
        navWidget.restoreState(state);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (pane != null) {
            pane.render(guiGraphics, mouseX, mouseY, delta);
        }
        if (!statusMessage.isEmpty() && pane != null) {
            drawStatus(guiGraphics, statusMessage, Ui.ACCENT);
        }
    }

    @Override
    protected boolean contentMouseClicked(double mouseX, double mouseY, int button) {
        if (pane != null && pane.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    protected boolean contentMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return pane != null && pane.mouseScrolled(mouseX, mouseY, verticalAmount);
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

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (nameField != null && nameField.isFocused() && nameField.keyPressed(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (nameField != null && nameField.isFocused() && nameField.charTyped(event)) return true;
        return super.charTyped(event);
    }
}