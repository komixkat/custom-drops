package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.widget.ScrollablePane;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.preset.PresetLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class PresetsScreen extends SplitPaneScreen {

    private ScrollablePane pane;
    private CustomDropsConfig staging;
    private String stagingPreset = "";
    private boolean staged = false;
    private String statusMessage = "";

    public PresetsScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.config.presets.active"));
    }

    @Override
    protected boolean isSaveable() {
        return true;
    }

    @Override
    protected String backButtonLabel() {
        return "Back";
    }

    @Override
    protected void buildNavigation() {
        navWidget.addCategory("Available Presets");
        CustomDropsConfig config = CustomDropsMod.config();
        String active = config.activePreset();

        for (String presetId : PresetLoader.KNOWN_PRESET_IDS) {
            String display = presetId.replace('_', ' ');
            if (presetId.equals(active) && !staged) {
                display += " [ACTIVE]";
            }
            String entryLabel = display;
            String entryPreset = presetId;
            navWidget.addEntry("Available Presets", entryLabel, () -> {
                loadStaging(entryPreset);
                selectedKey = entryLabel;
            });
        }

        if (staged) {
            navWidget.addCategory("Staged Preset");
            navWidget.addEntry("Staged Preset", "Mob Drops (" + size(staging.mobDrops()) + ")", () -> {
                selectedKey = null;
                this.minecraft.gui.setScreen(new MobDropsScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING));
            });
            navWidget.addEntry("Staged Preset", "Block Drops (" + size(staging.blockDrops()) + ")", () -> {
                selectedKey = null;
                this.minecraft.gui.setScreen(new BlockDropsScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING));
            });
            navWidget.addEntry("Staged Preset", "Chest Loot (" + size(staging.chestLoot()) + ")", () -> {
                selectedKey = null;
                this.minecraft.gui.setScreen(new ChestLootScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING));
            });
            navWidget.addEntry("Staged Preset", "Fishing Loot (" + size(staging.fishingLoot()) + ")", () -> {
                selectedKey = null;
                this.minecraft.gui.setScreen(new FishingLootScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING));
            });
            navWidget.addEntry("Staged Preset", "Equipment (" + size(staging.equipmentOverrides()) + ")", () -> {
                selectedKey = null;
                this.minecraft.gui.setScreen(new EquipmentScreen(this,
                    () -> staging, LootRuleScreen.Mode.STAGING));
            });
            navWidget.addEntry("Staged Preset", "Apply to my config", () -> applyStaged());
            navWidget.addEntry("Staged Preset", "Discard staging", () -> {
                staged = false;
                stagingPreset = "";
                statusMessage = "";
                selectedKey = null;
                refreshNav();
                buildHub();
            });
        }

        navWidget.addCategory("Actions");
        navWidget.addEntry("Actions", "Clear All Rules", this::clearAllRules);
    }

    private static int size(java.util.List<?> list) {
        return list == null ? 0 : list.size();
    }

    private void loadStaging(String presetId) {
        staging = new CustomDropsConfig();
        if (!PresetLoader.fill(presetId, staging)) {
            statusMessage = "Preset '" + presetId + "' not found on classpath.";
            return;
        }
        stagingPreset = presetId;
        staged = true;
        statusMessage = "Loaded \"" + presetId.replace('_', ' ') + "\" for editing. Open a category, then Apply when ready.";
        refreshNav();
        buildHub();
    }

    private void applyStaged() {
        if (!staged) return;
        CustomDropsConfig target = CustomDropsMod.config();
        target.clearAll();
        target.setSchemaVersion(staging.schemaVersion());
        target.setActivePreset("");
        target.setMobDropsEnabled(staging.mobDropsEnabled());
        target.setBlockDropsEnabled(staging.blockDropsEnabled());
        target.setChestLootEnabled(staging.chestLootEnabled());
        target.setFishingLootEnabled(staging.fishingLootEnabled());
        target.setEquipmentOverridesEnabled(staging.equipmentOverridesEnabled());
        target.mobDrops().addAll(staging.mobDrops());
        target.blockDrops().addAll(staging.blockDrops());
        target.chestLoot().addAll(staging.chestLoot());
        target.fishingLoot().addAll(staging.fishingLoot());
        target.equipmentOverrides().addAll(staging.equipmentOverrides());
        staged = false;
        stagingPreset = "";
        selectedKey = null;
        refreshNav();
        markDirty();
        statusMessage = "Staged preset applied to your config. Press Save to write it to disk.";
        buildHub();
    }

    @Override
    protected void save() {
        if (staged) {
            applyStaged();
        }
        super.save();
    }

    private void clearAllRules() {
        CustomDropsConfig config = CustomDropsMod.config();
        config.clearAll();
        config.setActivePreset("");
        markDirty();
        statusMessage = "Cleared all rules in memory. Press Save to write to disk.";
        buildHub();
    }

    private void refreshNav() {
        if (navWidget == null) return;
        com.komixkat.customdrops.client.gui.widget.NavigationWidget.NavState state = navWidget.snapshotState();
        navWidget.clear();
        buildNavigation();
        navWidget.restoreState(state);
        if (selectedKey != null) {
            navWidget.setSelected(selectedKey);
        }
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

        if (!staged) {
            hubStamp = -1;
            pane.addLabel(cur.x, cur.y, "Presets", Ui.TEXT);
            cur.y += Ui.LINE_H + 6;
            pane.addLabel(cur.x, cur.y, "Pick a preset from the left panel to load its rules", Ui.MUTED);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, "into an editable staging area.", Ui.MUTED);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, "Edit any category, then Apply to make it your config.", Ui.DIM);
            pane.noteCursorY(cur.y);
            pane.finish(8);
            return;
        }

        pane.addLabel(cur.x, cur.y, "Preset: " + stagingPreset.replace('_', ' '), Ui.TEXT);
        cur.y += Ui.LINE_H + 6;
        pane.addLabel(cur.x, cur.y, "These rules are staged for editing (not yet applied).", Ui.MUTED);
        cur.y += Ui.LINE_H + 8;
        pane.addLabel(cur.x, cur.y, "Every rule is listed below with an Edit button that", Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        pane.addLabel(cur.x, cur.y, "opens it right where it lives.", Ui.MUTED);
        cur.y += Ui.LINE_H + 10;

        ruleSection(cur, "Mob drops", staging.mobDrops(), mobLabels(), i ->
            openStaged(new MobDropsScreen(this, () -> staging, LootRuleScreen.Mode.STAGING),
                mobLabels().get(i)));
        ruleSection(cur, "Block drops", staging.blockDrops(), blockLabels(), i ->
            openStaged(new BlockDropsScreen(this, () -> staging, LootRuleScreen.Mode.STAGING),
                blockLabels().get(i)));
        ruleSection(cur, "Chest loot", staging.chestLoot(), chestLabels(), i ->
            openStaged(new ChestLootScreen(this, () -> staging, LootRuleScreen.Mode.STAGING),
                chestLabels().get(i)));
        ruleSection(cur, "Fishing loot", staging.fishingLoot(), fishingLabels(), i ->
            openStaged(new FishingLootScreen(this, () -> staging, LootRuleScreen.Mode.STAGING),
                fishingLabels().get(i)));
        ruleSection(cur, "Equipment", staging.equipmentOverrides(), equipmentLabels(), i ->
            openStaged(new EquipmentScreen(this, () -> staging, LootRuleScreen.Mode.STAGING),
                equipmentLabels().get(i)));

        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 10;

        pane.addLabel(cur.x, cur.y, "Your config right now:", Ui.MUTED);
        cur.y += Ui.LINE_H + 4;
        CustomDropsConfig current = CustomDropsMod.config();
        pane.addLabel(cur.x, cur.y, "Mob: " + size(current.mobDrops()) + "  Block: " + size(current.blockDrops())
            + "  Chest: " + size(current.chestLoot()) + "  Fishing: " + size(current.fishingLoot())
            + "  Equipment: " + size(current.equipmentOverrides()), Ui.DIM);
        cur.y += Ui.LINE_H + 14;

        pane.addButton(cur, cur.x, cur.y, Math.min(220, cur.w), "Apply Staged Preset to My Config", this::applyStaged);
        cur.y += Ui.BUTTON_H + 4;
        pane.addButton(cur, cur.x, cur.y, Math.min(220, cur.w), "Discard Staging", () -> {
            staged = false;
            stagingPreset = "";
            statusMessage = "";
            selectedKey = null;
            refreshNav();
            buildHub();
        });
        pane.noteCursorY(cur.y);
        pane.finish(8);
        hubStamp = fingerprint();
    }

    private void ruleSection(ScrollablePane.Cursor cur, String title,
                             java.util.List<?> rules, java.util.List<String> labels,
                             java.util.function.IntConsumer onEdit) {
        pane.addLabel(cur.x, cur.y, title + "  (" + rules.size() + ")", Ui.SECTION_TEXT);
        cur.y += Ui.LINE_H + 2;
        if (rules.isEmpty()) {
            pane.addLabel(cur.x + 6, cur.y, "  (no rules)", Ui.DIM);
            cur.y += Ui.LINE_H + 6;
            return;
        }
        int editW = 52;
        int labelW = Math.max(40, cur.w - editW - 6);
        var font = net.minecraft.client.Minecraft.getInstance().font;
        for (int i = 0; i < labels.size(); i++) {
            String shown = font.plainSubstrByWidth(labels.get(i), labelW);
            pane.addLabel(cur.x, cur.y + 5, shown, Ui.TEXT);
            int idx = i;
            pane.addButton(cur, cur.x + cur.w - editW, cur.y, editW, "Edit", () -> onEdit.accept(idx));
            cur.y += Ui.BUTTON_H + 3;
        }
        cur.y += 6;
    }

    private void openStaged(net.minecraft.client.gui.screens.Screen screen, String preSelectLabel) {
        if (screen instanceof SplitPaneScreen sp) sp.preSelect(preSelectLabel);
        selectedKey = null;
        this.minecraft.gui.setScreen(screen);
    }

    private java.util.List<String> mobLabels() {
        return staging.mobDrops().stream()
            .map(e -> LootRuleScreen.displayId(e.targetId(), e.isTag()))
            .toList();
    }

    private java.util.List<String> blockLabels() {
        return staging.blockDrops().stream()
            .map(e -> LootRuleScreen.displayId(e.targetId(), e.isTag()))
            .toList();
    }

    private java.util.List<String> chestLabels() {
        return staging.chestLoot().stream()
            .map(e -> {
                String t = e.targetLootTableId();
                return t == null || t.isEmpty() ? "(untitled)" : t;
            })
            .toList();
    }

    private java.util.List<String> fishingLabels() {
        return staging.fishingLoot().stream()
            .map(e -> {
                String t = e.targetLootTableId();
                return t == null || t.isEmpty() ? "(untitled)" : t;
            })
            .toList();
    }

    private java.util.List<String> equipmentLabels() {
        return staging.equipmentOverrides().stream()
            .map(e -> {
                String t = e.targetEntityId();
                String id = t != null && !t.isEmpty() ? t : "(untitled)";
                if (e.isTag() && !id.startsWith("#")) id = "#" + id;
                return id + " " + e.slot();
            })
            .toList();
    }

    private long fingerprint() {
        CustomDropsConfig s = staging;
        if (s == null) return 0;
        long h = 7;
        h = h * 31 + s.mobDrops().size();
        h = h * 31 + s.blockDrops().size();
        h = h * 31 + s.chestLoot().size();
        h = h * 31 + s.fishingLoot().size();
        h = h * 31 + s.equipmentOverrides().size();
        return h;
    }

    private long hubStamp = -1;

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (staged && pane != null && fingerprint() != hubStamp) {
            refreshNav();
            buildHub();
        }
        if (pane != null) {
            pane.render(guiGraphics, mouseX, mouseY, delta);
        }
        if (!statusMessage.isEmpty() && pane != null) {
            drawStatus(guiGraphics, statusMessage, Ui.WARN);
        }
    }

    @Override
    protected boolean contentMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return pane != null && pane.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override
    protected boolean contentMouseClicked(double mouseX, double mouseY, int button) {
        if (pane != null && pane.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
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
    protected void onDiscarded() {
        selectedKey = null;
        statusMessage = "";
        refreshScreen();
    }
}