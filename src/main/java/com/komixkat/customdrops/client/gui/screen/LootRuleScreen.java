package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.client.gui.widget.EntryFormWidget;
import com.komixkat.customdrops.config.CustomDropsConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class LootRuleScreen<T> extends SplitPaneScreen {

    public enum Mode { LOCAL, STAGING, REMOTE }

    protected EntryFormWidget<T> form;
    protected final Supplier<CustomDropsConfig> config;
    protected final Mode mode;
    private boolean readOnly = false;

    protected LootRuleScreen(net.minecraft.client.gui.screens.Screen parent, Component title) {
        super(parent, title);
        this.config = CustomDropsMod::config;
        this.mode = Mode.LOCAL;
    }

    protected LootRuleScreen(net.minecraft.client.gui.screens.Screen parent, Component title,
                             Supplier<CustomDropsConfig> config, Mode mode) {
        super(parent, title);
        this.config = config == null ? CustomDropsMod::config : config;
        this.mode = mode == null ? Mode.LOCAL : mode;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (form != null) {
            form.setReadOnly(readOnly);
        }
    }

    @Override
    protected boolean isSaveable() {
        return mode == Mode.LOCAL;
    }

    protected abstract EntryFormWidget.Host<T> createHost();

    protected abstract String categoryTitle();

    protected abstract String singularLabel();

    protected void openEntry(int index) {
        List<T> list = createHost().list();
        if (index < 0 || index >= list.size()) return;
        selectedKey = createHost().entryLabel(list.get(index));
        form.open(index);
    }

    @Override
    protected void buildNavigation() {
        EntryFormWidget.Host<T> host = createHost();
        String title = categoryTitle();
        navWidget.addCategory(title, host.list().size());

        List<T> list = host.list();
        for (int i = 0; i < list.size(); i++) {
            T entry = list.get(i);
            int index = i;
            String label = host.entryLabel(entry);
            List<String> segments = new ArrayList<>();
            segments.add(title);
            segments.addAll(com.komixkat.customdrops.client.gui.widget.NavigationWidget
                .segments(host.targetOf(entry), host.isTagOf(entry)));
            navWidget.addEntryPath(segments, label, () -> openEntry(index));
            navWidget.markEntryWarn(label, warnOf(host, entry));
        }

        if (!readOnly) {
            navWidget.addCategory("Actions");
            navWidget.addEntry("Actions", "New " + singularLabel(), () -> addNewRule());
        }
    }

    private boolean warnOf(EntryFormWidget.Host<T> host, T entry) {
        String target = host.targetOf(entry);
        boolean isTag = host.isTagOf(entry);
        boolean warn = target == null || target.isBlank();
        if (!warn && !isTag) {
            String clean = target.endsWith("*") ? target.substring(0, target.length() - 1) : target;
            warn = !registryIndex.isKnown(host.lookupKind(), clean);
        }
        if (!warn) {
            List<com.komixkat.customdrops.config.schema.LootItemEntry> items = host.itemsOf(entry);
            if (items == null || items.isEmpty()) {
                warn = true;
            } else {
                for (com.komixkat.customdrops.config.schema.LootItemEntry it : items) {
                    if (it.chance() > 1.0f || it.itemId() == null || it.itemId().isBlank()) {
                        warn = true;
                        break;
                    }
                }
            }
        }
        return warn;
    }

    protected void addNewRule() {
        if (readOnly) return;
        form.open(addEntry(""));
    }

    protected abstract int addEntry(String targetId);

    protected boolean supportsExampleRules() {
        return false;
    }

    protected String exampleRulesLabel() {
        return "Insert example rules";
    }

    protected void insertExamples() {}

    private void runExampleRules() {
        if (readOnly) return;
        insertExamples();
        markChanged();
        onEntryListChanged();
    }

    protected static String displayId(String targetId, boolean isTag) {
        if (targetId == null || targetId.isEmpty()) return "(untitled)";
        if (isTag && !targetId.startsWith("#")) return "#" + targetId;
        return targetId;
    }

    protected static String stripTag(String targetId) {
        return targetId != null && targetId.startsWith("#") ? targetId.substring(1) : targetId;
    }

    private Button exampleButton;
    private EditBox ruleSearch;

    private static final int SEARCH_BAR_HEIGHT = 28;

    @Override
    protected void initContent() {
        if (navWidget != null) {
            navWidget.setBounds(PADDING, PADDING + SEARCH_BAR_HEIGHT, navWidth,
                Math.max(0, height - BOTTOM_BAR_HEIGHT - PADDING - SEARCH_BAR_HEIGHT));
            applyRuleFilter();
        }

        ruleSearch = new EditBox(font, PADDING + 2, PADDING + 4, navWidth - 4, SEARCH_BAR_HEIGHT - 8,
            Component.literal("Search " + singularLabel().toLowerCase() + "s..."));
        ruleSearch.setMaxLength(64);
        ruleSearch.setResponder(query -> applyRuleFilter());
        addRenderableWidget(ruleSearch);

        form = new EntryFormWidget<>(this, createHost(), rightPanelX + PADDING, contentY + PADDING,
            Math.max(0, rightPanelWidth - PADDING * 2), Math.max(0, contentBottom - contentY - PADDING * 2 - 12));
        form.setReadOnly(readOnly);

        exampleButton = Button.builder(Component.literal(exampleRulesLabel()), b -> runExampleRules())
            .bounds(rightPanelX + PADDING, contentY + 80, Math.min(220, rightPanelWidth - PADDING * 2), 20)
            .build();
        exampleButton.visible = false;
        addRenderableWidget(exampleButton);
    }

    private void applyRuleFilter() {
        if (navWidget != null && ruleSearch != null) {
            navWidget.setFilter(ruleSearch.getValue());
        }
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (exampleButton != null) {
            boolean showExamples = supportsExampleRules()
                && !readOnly
                && createHost() != null
                && createHost().list().isEmpty()
                && (form == null || !form.isOpen());
            exampleButton.visible = showExamples;
        }
        if (form != null && form.isOpen()) {
            form.render(guiGraphics, mouseX, mouseY, delta);
            return;
        }
        var font = net.minecraft.client.Minecraft.getInstance().font;
        guiGraphics.text(font, categoryTitle(), rightPanelX + 8, contentY + 16, 0xFFE0E0E0, false);
        guiGraphics.text(font, "This screen stores your " + singularLabel().toLowerCase() + " drop rules.",
            rightPanelX + 8, contentY + 36, 0xFF888888, false);
        guiGraphics.text(font, "Pick a rule on the left to edit it, or press \"New " + singularLabel() + "\".",
            rightPanelX + 8, contentY + 50, 0xFF888888, false);
        guiGraphics.text(font, "Rules only take effect after you press Save.",
            rightPanelX + 8, contentY + 64, 0xFF888888, false);
    }

    @Override
    protected boolean contentMouseClicked(double mouseX, double mouseY, int button) {
        if (form != null && form.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    protected boolean contentMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (form != null && form.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean contentMouseDragged(double mouseX, double mouseY, double dx, double dy) {
        if (form != null && form.mouseDragged(mouseX, mouseY)) return true;
        return false;
    }

    @Override
    protected void contentMouseReleased(double mouseX, double mouseY) {
        if (form != null) {
            form.mouseReleased(mouseX, mouseY);
        }
    }

    protected Runnable remoteSendHandler = null;

    public void setRemoteSendHandler(Runnable handler) {
        this.remoteSendHandler = handler;
    }

    @Override
    protected void addExtraBottomBarButtons(int barY) {
        if (mode == Mode.LOCAL || mode == Mode.STAGING) {
            if (!readOnly) {
                addBottomBarButton("+ New " + singularLabel(), this::addNewRule);
            }
        } else if (mode == Mode.REMOTE && !readOnly && remoteSendHandler != null) {
            addBottomBarButton("Send to Server", remoteSendHandler);
        }
    }

    @Override
    public void onEntryListChanged() {
        if (navWidget == null) return;
        com.komixkat.customdrops.client.gui.widget.NavigationWidget.NavState state = navWidget.snapshotState();
        navWidget.clear();
        buildNavigation();
        navWidget.restoreState(state);
        applyRuleFilter();
        if (selectedKey != null) {
            navWidget.setSelected(selectedKey);
        }
    }

    @Override
    protected void onDiscarded() {
        selectedKey = null;
        if (form != null) {
            form.close();
        }
        refreshScreen();
    }
}