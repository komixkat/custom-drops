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
        navWidget.addCategory("Profiles");
        for (String name : ConfigProfiles.names()) {
            String label = name + "  (" + ruleCountFor(name) + ")";
            navWidget.addEntry("Profiles", label, () -> doSwitch(name));
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

        pane.addLabel(cur.x, cur.y, "Per-World Config", Ui.TEXT);
        cur.y += Ui.LINE_H + 6;
        MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        String worldName = server != null && server.getWorldData() != null
            ? server.getWorldData().getLevelName() : null;
        Path worldCfgDir = server == null ? null : currentWorldConfigDir(server);

        if (worldName == null) {
            pane.addLabel(cur.x, cur.y, "Not in a single-player world right now.", Ui.DIM);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, "Enter a world to manage its per-world config here.", Ui.DIM);
            cur.y += Ui.LINE_H + 12;
        } else {
            boolean linked = worldCfgDir != null && Files.exists(worldCfgDir.resolve("mob_drops.json"))
                || worldCfgDir != null && Files.exists(worldCfgDir.resolve("meta.json"));
            pane.addLabel(cur.x, cur.y, "World: \"" + worldName + "\"", Ui.TEXT);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, linked
                ? "This world has its own config overriding the active profile."
                : "This world currently uses the active profile with no override.", linked ? Ui.WARN : Ui.DIM);
            cur.y += Ui.LINE_H + 8;

            if (linked) {
                int bw = Math.max(120, (cur.w - 4) / 2);
                pane.addButton(cur, cur.x, cur.y, bw,
                    "Unlink (revert to active profile)", () -> doUnlinkWorld(worldCfgDir));
                pane.addButton(cur, cur.x + bw + 4, cur.y, bw,
                    "Re-link from active profile", () -> doLinkWorld(worldCfgDir));
            } else {
                pane.addButton(cur, cur.x, cur.y, Math.min(220, cur.w),
                    "Link active profile to this world", () -> doLinkWorld(worldCfgDir));
            }
            cur.y += Ui.BUTTON_H + 12;
        }

        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 12;

        pane.addLabel(cur.x, cur.y, "Reset the ACTIVE config back to vanilla defaults.", Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "This wipes every rule in the active config, in memory and on disk.", Ui.DIM);
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
            List.of("The active profile is what new edits and the running world use.",
                "The current active config \"" + ConfigProfiles.active() + "\" stays on disk."),
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

    private void doLinkWorld(Path worldCfgDir) {
        this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
            "Link the active config \"" + ConfigProfiles.active() + "\" to world \"" + worldNameSafe() + "\"?",
            List.of("The world \"" + worldNameSafe() + "\" will get its own copy of",
                "the active profile and keep it even if you switch profiles."),
            "Link", () -> { performLinkWorld(worldCfgDir); }));
    }

    private void performLinkWorld(Path worldCfgDir) {
        MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            statusMessage = "Not in a single-player world right now.";
            buildHub();
            return;
        }
        CustomDropsMod.linkActiveProfileToWorld(server);
        CustomDropsMod.reloadForRunningWorld();
        statusMessage = "Linked the active profile \"" + ConfigProfiles.active()
            + "\" to world \"" + worldNameSafe() + "\" and reloaded it.";
        buildHub();
    }

    private void doUnlinkWorld(Path worldCfgDir) {
        this.minecraft.gui.setScreen(new ConfirmDialogScreen(this,
            "Unlink world \"" + worldNameSafe() + "\" from its per-world config?",
            List.of("The world's own config files will remain on disk,",
                "but the world will go back to using the active profile."),
            "Unlink", () -> { performUnlinkWorld(worldCfgDir); }));
    }

    private void performUnlinkWorld(Path worldCfgDir) {
        MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            statusMessage = "Not in a single-player world right now.";
            buildHub();
            return;
        }
        CustomDropsMod.unlinkWorldConfig(server);
        CustomDropsMod.reloadForRunningWorld();
        statusMessage = "Removed the per-world config. World \"" + worldNameSafe()
            + "\" now uses the active profile, reloaded.";
        buildHub();
    }

    private static Path currentWorldConfigDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("customdrops");
    }

    private static String worldNameSafe() {
        MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        return server != null && server.getWorldData() != null ? server.getWorldData().getLevelName() : "?";
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