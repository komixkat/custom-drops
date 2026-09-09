package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.client.autocomplete.RegistryIndex;
import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.widget.NavigationWidget;
import com.komixkat.customdrops.client.gui.widget.RegistryAutocompleteField;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class SplitPaneScreen extends Screen {

    protected static final int PADDING = Ui.PADDING;
    protected static final int BOTTOM_BAR_HEIGHT = 24;
    protected static final int NAV_MIN_WIDTH = 180;
    protected static final int NAV_MAX_WIDTH = 280;

    protected final Screen parent;
    protected final RegistryIndex registryIndex;
    protected NavigationWidget navWidget;
    protected int navWidth;
    protected int rightPanelX;
    protected int rightPanelWidth;
    protected int contentY;
    protected int contentBottom;

    protected String selectedKey = null;

    public void preSelect(String entryLabel) {
        this.selectedKey = entryLabel;
    }
    private boolean dirty = false;
    private Button saveButton;
    private Button backButton;

    protected SplitPaneScreen(Screen parent, Component title) {
        super(title);
        this.parent = parent;
        this.registryIndex = new RegistryIndex();
    }

    public void setReadOnly(boolean readOnly) {}

    protected boolean isSaveable() {
        return false;
    }

    protected String backButtonLabel() {
        return "Back";
    }

    @Override
    protected void init() {
        navWidth = Math.min(NAV_MAX_WIDTH, Math.max(NAV_MIN_WIDTH, width / 4));
        rightPanelX = navWidth + PADDING;
        rightPanelWidth = Math.max(0, width - rightPanelX - PADDING);
        contentY = PADDING;
        contentBottom = height - BOTTOM_BAR_HEIGHT - PADDING;

        navWidget = new NavigationWidget(PADDING, PADDING, navWidth, Math.max(0, height - BOTTOM_BAR_HEIGHT - PADDING * 2));
        buildNavigation();
        initContent();
        addBottomBar();
        registryIndex.build();
        afterInit();
    }

    protected void afterInit() {
        if (selectedKey != null && navWidget != null) {
            navWidget.triggerEntry(selectedKey, true);
        }
    }

    protected void addBottomBar() {
        int barY = height - BOTTOM_BAR_HEIGHT;
        int backX = rightPanelX + PADDING;

        backButton = Button.builder(
            Component.literal(backButtonLabel()),
            btn -> onClose()
        ).bounds(backX, barY + 2, 72, BOTTOM_BAR_HEIGHT - 4).build();

        extraButtonRight = rightPanelX + rightPanelWidth;
        if (isSaveable()) {
            saveButton = Button.builder(
                Component.translatable("customdrops.menu.save"),
                btn -> save()
            ).bounds(width - PADDING - 88, barY + 2, 88, BOTTOM_BAR_HEIGHT - 4).build();
            saveButton.active = isDirty();
            extraButtonRight = saveButton.getX() - Ui.GAP;
        }
        addRenderableWidget(backButton);
        if (saveButton != null) {
            addRenderableWidget(saveButton);
        }
        addExtraBottomBarButtons(barY);
    }

    protected void addExtraBottomBarButtons(int barY) {}

    private int extraButtonRight = 0;

    protected final Button addBottomBarButton(String label, Runnable onPress) {
        int barY = height - BOTTOM_BAR_HEIGHT;
        int backEnd = rightPanelX + PADDING + 72 + Ui.GAP;
        int textW = font.width(label) + 14;
        int available = extraButtonRight - backEnd;
        int w = Math.min(textW, Math.max(1, available));
        int x = extraButtonRight - w;
        Button b = Button.builder(Component.literal(label), btn -> onPress.run())
            .bounds(x, barY + 2, Math.max(1, w), BOTTOM_BAR_HEIGHT - 4).build();
        boolean fits = available >= Math.min(56, w);
        b.visible = fits;
        b.active = fits;
        extraButtonRight = x - Ui.GAP;
        addRenderableWidget(b);
        return b;
    }

    protected final void drawStatus(GuiGraphicsExtractor guiGraphics, String text, int color) {
        if (text == null || text.isBlank()) return;
        int inset = 6;
        int maxW = Math.max(20, rightPanelWidth - PADDING * 2 - inset * 2 - 6);
        String clipped = font.plainSubstrByWidth(text, maxW);
        int textW = font.width(clipped);
        int x = rightPanelX + PADDING + inset;
        int y = contentBottom - Ui.LINE_H - 12;
        int bw = textW + 10;
        int bh = Ui.LINE_H + 5;
        guiGraphics.fill(x, y, x + bw, y + bh, 0xFF1C1C22);
        guiGraphics.fill(x, y, x + bw, y + 1, 0xFF3A3A44);
        guiGraphics.text(font, clipped, x + 5, y + 2, color, false);
    }

    public void openVanillaLookup(String id) {
        if (id == null || id.isBlank()) return;
        this.minecraft.gui.setScreen(new DefaultValuesScreen(this, id));
    }

    protected void updateSaveButton() {
        if (saveButton != null) {
            saveButton.active = isDirty();
        }
    }

    protected abstract void buildNavigation();

    protected abstract void initContent();

    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(guiGraphics);

        int barTop = height - BOTTOM_BAR_HEIGHT;
        guiGraphics.fill(0, barTop, width, height, 0xFF1E1E24);
        guiGraphics.fill(0, barTop, width, barTop + 1, 0xFF3A3A44);

        int navRight = navWidth + PADDING;
        guiGraphics.fill(navRight, PADDING, navRight + 1, barTop, 0xFF303038);

        if (navWidget != null) {
            navWidget.render(guiGraphics, mouseX, mouseY, delta);
        }

        renderContent(guiGraphics, mouseX, mouseY, delta);

        if (saveButton != null && isDirty()) {
            int bx = saveButton.getX();
            int by = saveButton.getY();
            int bw = saveButton.getWidth();
            int bh = saveButton.getHeight();
            guiGraphics.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0x88FF8800);
        }

        for (GuiEventListener child : children()) {
            if (child instanceof Renderable r) {
                r.extractRenderState(guiGraphics, mouseX, mouseY, delta);
            }
        }

        for (GuiEventListener child : children()) {
            if (child instanceof RegistryAutocompleteField f) {
                f.renderPopupLast(guiGraphics, mouseX, mouseY, delta);
            }
        }
    }

    protected boolean contentMouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    protected boolean contentMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    protected boolean contentMouseDragged(double mouseX, double mouseY, double dx, double dy) {
        return false;
    }

    protected void contentMouseReleased(double mouseX, double mouseY) {}

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (navWidget != null && navWidget.isThumbDragging()) {
            navWidget.mouseDragged(mouseX, mouseY);
            return true;
        }
        if (contentMouseDragged(mouseX, mouseY, dx, dy)) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (navWidget != null) {
            navWidget.mouseReleased(event.x(), event.y());
        }
        contentMouseReleased(event.x(), event.y());
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean focused) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (tryClickSuggestionPopup(mouseX, mouseY)) {
            return true;
        }
        if (mouseX < navWidth + PADDING && navWidget != null) {
            return navWidget.mouseClicked(mouseX, mouseY, button);
        }
        if (contentMouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        for (GuiEventListener child : children()) {
            if (child instanceof RegistryAutocompleteField f && !f.containsPoint(mouseX, mouseY)) {
                f.hidePopup();
            }
        }
        return super.mouseClicked(event, focused);
    }

    private boolean tryClickSuggestionPopup(double mouseX, double mouseY) {
        for (GuiEventListener child : children()) {
            if (child instanceof RegistryAutocompleteField f) {
                if (f.acceptClickAt(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (GuiEventListener child : children()) {
            if (child instanceof RegistryAutocompleteField f && f.popupScroll(mouseX, mouseY, verticalAmount, height)) {
                return true;
            }
        }
        if (mouseX < navWidth + PADDING && navWidget != null) {
            return navWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (contentMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        if (isSaveable() && isDirty()) {
            this.minecraft.gui.setScreen(new UnsavedChangesScreen(this));
            return;
        }
        this.minecraft.gui.setScreen(this.parent);
    }

    protected final boolean isDirty() {
        return dirty;
    }

    protected final void markDirty() {
        dirty = true;
        updateSaveButton();
    }

    public void markChanged() {
        markDirty();
    }

    protected void save() {
        CustomDropsMod.saveGlobalConfig();
        CustomDropsMod.reloadForRunningWorld();
        dirty = false;
        updateSaveButton();
        onSaved();
    }

    protected final void discard() {
        CustomDropsMod.reloadGlobalConfig();
        dirty = false;
        updateSaveButton();
        onDiscarded();
    }

    protected void onSaved() {}

    protected void onDiscarded() {
        refreshScreen();
    }

    public void onEntryListChanged() {}

    public void onTargetIdEdited(String targetId) {}

    public net.minecraft.client.gui.Font font() {
        return net.minecraft.client.Minecraft.getInstance().font;
    }

    protected final void refreshScreen() {
        clearWidgets();
        clearFocus();
        init();
    }

    public void registerWidget(GuiEventListener widget) {
        if (widget instanceof net.minecraft.client.gui.components.AbstractWidget aw) {
            addRenderableWidget(aw);
        }
    }

    public final void unregisterWidget(GuiEventListener widget) {
        if (getFocused() == widget) {
            setFocused((GuiEventListener) null);
        }
        removeWidget(widget);
    }

    public RegistryIndex registryIndex() {
        return registryIndex;
    }

    public net.minecraft.client.gui.screens.Screen parent() {
        return parent;
    }
}