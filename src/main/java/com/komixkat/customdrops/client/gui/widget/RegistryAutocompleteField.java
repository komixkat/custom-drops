package com.komixkat.customdrops.client.gui.widget;

import com.komixkat.customdrops.client.autocomplete.RegistryIndex;
import com.komixkat.customdrops.client.autocomplete.SuggestionProvider;
import com.komixkat.customdrops.client.gui.UiSfx;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class RegistryAutocompleteField extends EditBox {

    private final RegistryIndex registryIndex;
    private SuggestionPopup suggestionPopup;
    private boolean popupVisible = false;
    private int selectedSuggestion = -1;

    private Consumer<String> externalResponder;
    private boolean suppressPopupNextResponder = false;
    private boolean editable = true;
    private Set<String> suggestionKinds = Set.of();
    private int popupClampTop = 0;
    private int popupClampBottom = Integer.MAX_VALUE;

    public RegistryAutocompleteField(Screen parent, int x, int y, int width, int height,
                                     Component narrationMessage, RegistryIndex registryIndex) {
        super(net.minecraft.client.Minecraft.getInstance().font, x, y, width, height, narrationMessage);
        this.registryIndex = registryIndex;
        super.setResponder(this::onTextChanged);
        this.setMaxLength(1024);
        this.setCanLoseFocus(true);
    }

    public void setSuggestor(Consumer<String> responder) {
        this.externalResponder = responder;
    }

    @Override
    public void setEditable(boolean editable) {
        this.editable = editable;
        super.setEditable(editable);
    }

    public void setPopupClamp(int top, int bottom) {
        this.popupClampTop = top;
        this.popupClampBottom = bottom;
    }

    public void setSuggestionKinds(Set<String> kinds) {
        this.suggestionKinds = kinds == null || kinds.isEmpty() ? Set.of() : kinds;
    }

    public int popupClampTop() {
        return popupClampTop;
    }

    public int popupClampBottom() {
        return popupClampBottom;
    }

    @Override
    public void setValue(String value) {
        boolean wasSuppressed = suppressPopupNextResponder;
        suppressPopupNextResponder = true;
        super.setValue(value);
        suppressPopupNextResponder = wasSuppressed;
    }

    private void onTextChanged(String text) {
        if (externalResponder != null) {
            externalResponder.accept(text);
        }
        if (suppressPopupNextResponder) {
            return;
        }
        if (!editable || text == null || text.isBlank()) {
            hidePopup();
            return;
        }
        refreshSuggestions(text);
    }

    private void refreshSuggestions(String text) {
        List<SuggestionProvider.Suggestion> suggestions = registryIndex.search(text,
            suggestionKinds.isEmpty() ? null : suggestionKinds, 20);
        if (suggestions.isEmpty()) {
            hidePopup();
            return;
        }
        if (suggestionPopup == null) {
            suggestionPopup = new SuggestionPopup();
        }
        suggestionPopup.update(suggestions);
        popupVisible = true;
        selectedSuggestion = -1;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (popupVisible && suggestionPopup != null) {
            int key = event.key();
            // GLFW key codes
            if (key == 264) { // GLFW_KEY_DOWN
                selectedSuggestion = Math.min(selectedSuggestion + 1, suggestionPopup.size() - 1);
                return true;
            }
            if (key == 265) { // GLFW_KEY_UP
                selectedSuggestion = Math.max(selectedSuggestion - 1, 0);
                return true;
            }
            if ((key == 257 || key == 335) && selectedSuggestion >= 0) { // ENTER / KP_ENTER
                SuggestionProvider.Suggestion selected = suggestionPopup.get(selectedSuggestion);
                acceptSuggestion(selected);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
        boolean inside = containsPoint(event.x(), event.y());
        boolean consumed = super.mouseClicked(event, isDoubleClick);
        if (!inside) {
            hidePopup();
        }
        return consumed;
    }

    public boolean acceptClickAt(double x, double y) {
        if (!popupVisible || suggestionPopup == null) return false;
        int idx = suggestionPopup.indexAt(this, x, y);
        if (idx < 0) return false;
        acceptSuggestion(suggestionPopup.get(idx));
        return true;
    }

    public boolean popupScroll(double x, double y, double verticalAmount, int guiHeight) {
        if (!popupVisible || suggestionPopup == null) return false;
        return suggestionPopup.scroll(this, x, y, verticalAmount, guiHeight);
    }

    private void acceptSuggestion(SuggestionProvider.Suggestion selected) {
        if (selected == null) {
            hidePopup();
            return;
        }
        UiSfx.accept();
        setValue(selected.id());
        hidePopup();
        registryIndex.incrementPopularity(selected.id());
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, delta);
    }

    public void renderPopupLast(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (popupVisible && suggestionPopup != null && this.getY() > -1000) {
            suggestionPopup.render(guiGraphics, this, mouseX, mouseY, selectedSuggestion);
        }
    }

    public boolean containsPoint(double x, double y) {
        return x >= getX() && x < getX() + getWidth() && y >= getY() && y < getY() + getHeight();
    }

    public void hidePopup() {
        popupVisible = false;
        selectedSuggestion = -1;
    }

    public boolean isPopupVisible() {
        return popupVisible;
    }
}