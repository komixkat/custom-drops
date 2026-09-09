package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.widget.ScrollablePane;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.config.schema.EquipmentOverrideEntry;
import com.komixkat.customdrops.config.schema.EquipmentOverrideEntry.EquipmentSlotGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class EquipmentScreen extends SplitPaneScreen {

    private final List<SlotButton> slotButtons = new ArrayList<>();
    private ScrollablePane pane;
    private int selectedIndex = -1;
    private boolean readOnly = false;
    private String targetId = "";
    private boolean isTag = false;
    private EquipmentSlotGroup slot = EquipmentSlotGroup.MAIN_HAND;
    private float dropChance = EquipmentOverrideEntry.ALWAYS_DROP;

    private final Supplier<CustomDropsConfig> config;
    private final LootRuleScreen.Mode mode;
    private Runnable remoteSendHandler = null;

    public EquipmentScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.config.category.equipment"));
        this.config = CustomDropsMod::config;
        this.mode = LootRuleScreen.Mode.LOCAL;
    }

    public EquipmentScreen(net.minecraft.client.gui.screens.Screen parent,
                           Supplier<CustomDropsConfig> config, LootRuleScreen.Mode mode) {
        super(parent, Component.translatable("customdrops.config.category.equipment"));
        this.config = config == null ? CustomDropsMod::config : config;
        this.mode = mode == null ? LootRuleScreen.Mode.LOCAL : mode;
    }

    public void setRemoteSendHandler(Runnable handler) {
        this.remoteSendHandler = handler;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (selectedIndex >= 0) {
            rebuildDetails();
        }
    }

    private List<EquipmentOverrideEntry> list() {
        return config.get().equipmentOverrides();
    }

    @Override
    protected boolean isSaveable() {
        return mode == LootRuleScreen.Mode.LOCAL;
    }

    @Override
    protected String backButtonLabel() {
        return "Back";
    }

    @Override
    protected void buildNavigation() {
        navWidget.addCategory("Equipment Overrides", list().size());
        List<EquipmentOverrideEntry> list = list();
        for (int i = 0; i < list.size(); i++) {
            EquipmentOverrideEntry e = list.get(i);
            int index = i;
            List<String> segments = new ArrayList<>();
            segments.add("Equipment Overrides");
            segments.addAll(com.komixkat.customdrops.client.gui.widget.NavigationWidget
                .segments(e.targetEntityId(), e.isTag()));
            String label = entryLabel(e);
            navWidget.addEntryPath(segments, label, () -> openDetails(index));
            String target = e.targetEntityId();
            boolean warn = target == null || target.isBlank()
                || (!e.isTag() && !registryIndex.isKnown("entity", target))
                || e.dropChance() > 50.0f;
            navWidget.markEntryWarn(label, warn);
        }
        if (!readOnly) {
            navWidget.addCategory("Actions");
            navWidget.addEntry("Actions", "New Equipment Override", this::addNew);
        }
    }

    private static String entryLabel(EquipmentOverrideEntry e) {
        String t = e.targetEntityId();
        String id = (e.isTag() && !t.startsWith("#")) ? "#" + t : t;
        if (t == null || t.isEmpty()) id = "(untitled)";
        return id + " " + e.slot();
    }

    @Override
    protected void initContent() {
        rebuildDetails();
    }

    private void addNew() {
        if (readOnly) return;
        List<EquipmentOverrideEntry> list = list();
        list.add(new EquipmentOverrideEntry("", false,
            EquipmentSlotGroup.MAIN_HAND, EquipmentOverrideEntry.ALWAYS_DROP));
        selectedKey = entryLabel(list.get(list.size() - 1));
        refreshNav();
        openDetails(list.size() - 1);
    }

    private void openDetails(int index) {
        List<EquipmentOverrideEntry> list = list();
        if (index < 0 || index >= list.size()) {
            clearDetails();
            return;
        }
        EquipmentOverrideEntry e = list.get(index);
        selectedIndex = index;
        targetId = e.targetEntityId();
        isTag = e.isTag();
        slot = e.slot();
        dropChance = e.dropChance();
        selectedKey = entryLabel(e);
        rebuildDetails();
    }

    private void clearDetails() {
        selectedIndex = -1;
        if (pane != null) {
            pane.clear();
        }
    }

    private void rebuildDetails() {
        if (pane == null) {
            pane = new ScrollablePane(this, rightPanelX + PADDING, contentY + PADDING,
                Math.max(0, rightPanelWidth - PADDING * 2), Math.max(0, contentBottom - contentY - PADDING * 2 - 24));
        } else {
            pane.setBounds(rightPanelX + PADDING, contentY + PADDING,
                Math.max(0, rightPanelWidth - PADDING * 2), Math.max(0, contentBottom - contentY - PADDING * 2 - 24));
            pane.clear(true);
        }
        slotButtons.clear();
        if (selectedIndex < 0) return;

        ScrollablePane.Cursor cur = pane.newCursor();
        pane.addLabel(cur.x, cur.y - 1, "Editing" + (readOnly ? " (read-only)" : ""), Ui.MUTED);
        pane.addLabel(cur.x, cur.y + Ui.LINE_H, targetId, Ui.ACCENT);

        if (!readOnly) {
            pane.addButton(cur, cur.right() - 82, -2, 78, "Duplicate", this::duplicate);
            pane.addButton(cur, cur.right() - 174, -2, 88, "Delete Entry", this::deleteEntry);
        }
        cur.y += 2 * Ui.LINE_H + 22;

        pane.addLabel(cur.x, cur.y, "Entity id (or #tag)", Ui.MUTED);
        pane.addAutocomplete(cur.x, cur.y + Ui.LINE_H + 3, cur.w - 126, targetId, v -> {
            if (readOnly) return;
            targetId = v == null ? "" : v;
            commit();
        }).setEditable(!readOnly);
        if (!readOnly) {
            pane.addButton(cur, cur.right() - 120, cur.y + Ui.LINE_H + 3, 120, "Open wiki page", this::openTargetWiki);
        }
        cur.y += Ui.LINE_H + Ui.FIELD_H + 10;

        pane.addCheckbox(cur.x, cur.y, "Use as tag (#prefix)", isTag, v -> {
            if (readOnly) return;
            isTag = v;
            commit();
        });
        cur.y += Ui.CHECKBOX_H + 6;

        pane.addLabel(cur.x, cur.y, "Slot to drop (which equipment piece this rule targets)", Ui.MUTED);
        cur.y += Ui.LINE_H + 6;

        int btnW = Math.min(104, (cur.w - 2) / 3);
        EquipmentSlotGroup[] slots = EquipmentSlotGroup.values();
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                int i = r * 3 + c;
                if (i >= slots.length) break;
                EquipmentSlotGroup s = slots[i];
                SlotButton slotBtn = pane.addWidget(new SlotButton(
                    cur.x + c * (btnW + 2), cur.y, btnW, Ui.BUTTON_H, s.name(), () -> {
                        if (readOnly) return;
                        slot = s;
                        commit();
                        refreshSlotButtons();
                    }), cur.x + c * (btnW + 2), cur.y);
                slotButtons.add(slotBtn);
            }
            cur.y += Ui.BUTTON_H + 2;
        }
        refreshSlotButtons();
        pane.addLabel(cur.x, cur.y, "Main hand / off hand / head / chest / legs / feet.", Ui.DIM);
        cur.y += Ui.LINE_H + 6;

        pane.addLabel(cur.x, cur.y, "Drop chance %  (100 = always drops, 0 = never)", Ui.MUTED);
        pane.addEditBox(cur.x, cur.y + Ui.LINE_H + 3, Math.min(90, cur.w), String.valueOf(Math.round(dropChance * 50f)), "", v -> {
            if (readOnly) return;
            try {
                float p = Float.parseFloat(v.trim());
                if (p < 0f || p > 100f) return;
                dropChance = p / 50f;
                commit();
            } catch (Exception ignored) {
                // keep previous value
            }
        });
        cur.y += Ui.LINE_H + Ui.FIELD_H + 12;

        pane.addLabel(cur.x, cur.y, "Hint: this only affects equipment the mob already spawned with.", Ui.DIM);

        pane.noteCursorY(cur.y);
        pane.finish(8);
        if (readOnly) {
            pane.setReadOnly(true);
        }
    }

    private void refreshSlotButtons() {
        for (SlotButton b : slotButtons) {
            b.active = !readOnly;
            b.setSelected(slot.name().equals(b.getMessage().getString()));
        }
    }

    private void openTargetWiki() {
        if (targetId == null || targetId.isBlank() || isTag) return;
        String clean = targetId.indexOf(':') >= 0 ? targetId.substring(targetId.indexOf(':') + 1) : targetId;
        openVanillaLookup("minecraft:entities/" + clean);
    }

    private void commit() {
        if (readOnly) return;
        List<EquipmentOverrideEntry> list = list();
        if (selectedIndex < 0 || selectedIndex >= list.size()) return;
        EquipmentOverrideEntry built = new EquipmentOverrideEntry(stripHash(targetId), isTag, slot, dropChance);
        if (list.get(selectedIndex).equals(built)) return;
        list.set(selectedIndex, built);
        markDirty();
    }

    private void duplicate() {
        if (readOnly) return;
        List<EquipmentOverrideEntry> list = list();
        if (selectedIndex < 0) return;
        list.add(new EquipmentOverrideEntry(stripHash(targetId), isTag, slot, dropChance));
        markDirty();
        refreshNav();
        selectedIndex = list.size() - 1;
        selectedKey = entryLabel(list.get(selectedIndex));
        rebuildDetails();
    }

    private void deleteEntry() {
        if (readOnly) return;
        List<EquipmentOverrideEntry> list = list();
        if (selectedIndex >= 0 && selectedIndex < list.size()) {
            list.remove(selectedIndex);
            markDirty();
        }
        clearDetails();
        refreshNav();
    }

    private static String stripHash(String targetId) {
        return targetId != null && targetId.startsWith("#") ? targetId.substring(1) : targetId;
    }

    private static final class SlotButton extends net.minecraft.client.gui.components.AbstractWidget {
        private final Runnable onPress;
        private boolean selected = false;

        SlotButton(int x, int y, int w, int h, String label, Runnable onPress) {
            super(x, y, w, h, Component.literal(label));
            this.onPress = onPress;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        protected void extractWidgetRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            int bg = selected ? 0xFF3F4FA5 : (isHoveredOrFocused() ? 0xFF3C3C48 : 0xFF2B2B33);
            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            if (selected) {
                g.fill(getX(), getY(), getX() + getWidth(), getY() + 1, 0xFFFFFFFF);
            }
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int textColor = active ? (selected ? 0xFFFFFFFF : 0xFFE0E0E0) : 0xFF6A6A72;
            String msg = getMessage().getString();
            g.text(font, msg, getX() + (getWidth() - font.width(msg)) / 2, getY() + (getHeight() - 8) / 2, textColor, false);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean focused) {
            if (active && event.button() < 2 && isMouseOver(event.x(), event.y())) {
                onPress.run();
                return true;
            }
            return false;
        }
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
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (selectedIndex < 0) {
            guiGraphics.text(font, "Equipment Drop Overrides", rightPanelX + 8, contentY + 16, Ui.TEXT, false);
            guiGraphics.text(font, "No override selected. Pick one from the left", rightPanelX + 8, contentY + 36, Ui.MUTED, false);
            guiGraphics.text(font, "panel, or \"New Equipment Override\".", rightPanelX + 8, contentY + 48, Ui.MUTED, false);
            return;
        }
        if (pane != null) {
            pane.render(guiGraphics, mouseX, mouseY, delta);
        }
    }

    @Override
    protected boolean contentMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (pane != null && selectedIndex >= 0) {
            return pane.mouseScrolled(mouseX, mouseY, verticalAmount);
        }
        return false;
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
    protected void addExtraBottomBarButtons(int barY) {
        if (mode == LootRuleScreen.Mode.LOCAL || mode == LootRuleScreen.Mode.STAGING) {
            if (!readOnly) {
                addBottomBarButton("+ New Override", this::addNew);
            }
        } else if (mode == LootRuleScreen.Mode.REMOTE && !readOnly && remoteSendHandler != null) {
            addBottomBarButton("Send to Server", remoteSendHandler);
        }
    }

    @Override
    public void onEntryListChanged() {
        refreshNav();
    }

    @Override
    protected void onDiscarded() {
        selectedKey = null;
        clearDetails();
        refreshScreen();
    }
}