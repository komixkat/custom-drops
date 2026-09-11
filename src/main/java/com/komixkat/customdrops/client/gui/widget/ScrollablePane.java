package com.komixkat.customdrops.client.gui.widget;

import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.screen.SplitPaneScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ScrollablePane {

    private final SplitPaneScreen owner;
    private final List<GuiEventListener> owned = new ArrayList<>();
    private final List<Slot> slots = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private final List<Divider> dividers = new ArrayList<>();

    /** Maps live-label keys to their index in {@link #labels} for in-place text updates. */
    private final Map<String, Integer> liveIndex = new LinkedHashMap<>();

    private int px;
    private int py;
    private int pw;
    private int ph;

    private int scroll = 0;
    private int contentHeight = 0;
    private int lastCursorY = 0;
    private boolean thumbDragging = false;
    private int thumbGrabOffset = 0;

    public ScrollablePane(SplitPaneScreen owner, int x, int y, int width, int height) {
        this.owner = owner;
        this.px = x;
        this.py = y;
        this.pw = width;
        this.ph = height;
    }

    public int x() {
        return px;
    }

    public int y() {
        return py;
    }

    public int width() {
        return pw;
    }

    public int height() {
        return ph;
    }

    public int contentHeight() {
        return contentHeight;
    }

    public Cursor newCursor() {
        return new Cursor(Ui.GAP, Ui.GAP, Math.max(4, pw - 2 * Ui.GAP - 6 - Ui.GAP));
    }

    public void addLabel(int x, int y, String text, int color) {
        labels.add(new Label(x, y, text, color));
    }

    public void addLiveLabel(String key, int x, int y, String text, int color) {
        liveIndex.put(key, labels.size());
        labels.add(new Label(x, y, text, color));
    }

    public void setLiveLabel(String key, String text) {
        Integer idx = liveIndex.get(key);
        if (idx == null) return;
        Label l = labels.get(idx);
        labels.set(idx, new Label(l.x(), l.y(), text, l.color()));
    }

    public void addFieldLabel(int x, int y, String text, int color) {
        labels.add(new Label(x, y + (Ui.FIELD_H - Ui.LINE_H) / 2, text, color));
    }

    public void addDivider(int x, int y, int w) {
        dividers.add(new Divider(x, y, Math.max(1, w)));
    }

    public void addButton(Cursor cur, int x, int y, int w, String label, Runnable onPress) {
        Button b = Button.builder(Component.literal(label), pressed -> onPress.run())
            .bounds(x, y, Math.max(1, w), Ui.BUTTON_H)
            .build();
        register(b, x, y, w, Ui.BUTTON_H);
    }

    public Button addButtonAndGet(Cursor cur, int x, int y, int w, String label, Runnable onPress) {
        Button b = Button.builder(Component.literal(label), pressed -> onPress.run())
            .bounds(x, y, Math.max(1, w), Ui.BUTTON_H)
            .build();
        register(b, x, y, w, Ui.BUTTON_H);
        return b;
    }

    public <T extends net.minecraft.client.gui.components.AbstractWidget> T addWidget(T widget, int relX, int relY) {
        register(widget, relX, relY, widget.getWidth(), widget.getHeight());
        return widget;
    }

    public Checkbox addCheckbox(int x, int y, String label, boolean selected, Consumer<Boolean> onChange) {
        Checkbox cb = Checkbox.builder(Component.literal(label), owner.font())
            .selected(selected)
            .onValueChange((box, val) -> onChange.accept(val))
            .pos(x, y)
            .build();
        register(cb, x, y, 0, 0);
        return cb;
    }

    public EditBox addEditBox(int x, int y, int w, String value, String hint, Consumer<String> onChange) {
        EditBox box = new EditBox(owner.font(), x, y, Math.max(1, w), Ui.FIELD_H,
            Component.literal(hint == null ? "" : hint));
        box.setValue(value == null ? "" : value);
        box.setResponder(onChange);
        box.setMaxLength(1024);
        register(box, x, y, Math.max(1, w), Ui.FIELD_H);
        return box;
    }

    public RegistryAutocompleteField addAutocomplete(int x, int y, int w, String value, Consumer<String> onChange) {
        return addAutocomplete(x, y, w, value, onChange, null);
    }

    public RegistryAutocompleteField addAutocomplete(int x, int y, int w, String value, Consumer<String> onChange,
                                                     java.util.Set<String> kinds) {
        RegistryAutocompleteField box = new RegistryAutocompleteField(owner, x, y, Math.max(1, w), Ui.FIELD_H,
            Component.literal(""), owner.registryIndex());
        box.setSuggestor(onChange);
        if (kinds != null && !kinds.isEmpty()) {
            box.setSuggestionKinds(kinds);
        }
        box.setValue(value == null ? "" : value);
        box.setPopupClamp(px, py + ph);
        register(box, x, y, Math.max(1, w), Ui.FIELD_H);
        return box;
    }

    public void finish(int contentBottomOffset) {
        int slotsBottom = 0;
        for (Slot s : slots) {
            slotsBottom = Math.max(slotsBottom, s.relY + s.h);
        }
        int labelsBottom = 0;
        for (Label l : labels) {
            labelsBottom = Math.max(labelsBottom, l.y + Ui.LINE_H + 4);
        }
        contentHeight = Math.max(0, Math.max(slotsBottom, Math.max(labelsBottom, lastCursorY)) + contentBottomOffset);
        layout();
    }

    public void noteCursorY(int y) {
        lastCursorY = y;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.px = x;
        this.py = y;
        this.pw = width;
        this.ph = height;
        for (RegistryAutocompleteField f : popupClamped) {
            f.setPopupClamp(px, py + ph);
        }
        layout();
    }

    private final List<RegistryAutocompleteField> popupClamped = new ArrayList<>();

    public boolean isOpenRegion(double mouseX, double mouseY) {
        return mouseX >= px && mouseX < px + pw && mouseY >= py && mouseY < py + ph;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (!isOpenRegion(mouseX, mouseY)) return false;
        int maxScroll = Math.max(0, contentHeight - (ph - Ui.GAP * 2));
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (verticalAmount * 28)));
        layout();
        return true;
    }

    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.fill(px, py, px + pw, py + ph, Ui.SURFACE_BG);

        clampScroll();

        var font = net.minecraft.client.Minecraft.getInstance().font;
        for (Label label : labels) {
            int y = py + label.y - scroll;
            if (y < py || y + Ui.LINE_H > py + ph) continue;
            guiGraphics.text(font, label.text, px + label.x, y + 2, label.color, false);
        }

        for (Divider divider : dividers) {
            int y = py + divider.y - scroll;
            if (y < py || y + 1 > py + ph) continue;
            guiGraphics.fill(px + divider.x, y, px + divider.x + divider.w, y + 1, 0xFF2E2E38);
        }

        if (contentHeight > ph - Ui.GAP * 2) {
            int[] thumb = thumbRect();
            if (thumb != null) {
                guiGraphics.fill(px + pw - 5, thumb[0], px + pw, thumb[0] + thumb[1], Ui.SCROLLBAR);
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOpenRegion(mouseX, mouseY) && button == 0) {
            int[] thumb = thumbRect();
            if (thumb != null && mouseX >= px + pw - 5 && mouseX <= px + pw
                && mouseY >= thumb[0] && mouseY <= thumb[0] + thumb[1]) {
                thumbDragging = true;
                thumbGrabOffset = (int) mouseY - thumb[0];
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!thumbDragging) return false;
        int maxScroll = Math.max(0, contentHeight - (ph - Ui.GAP * 2));
        if (maxScroll <= 0) {
            thumbDragging = false;
            return false;
        }
        int trackH = ph - Ui.GAP * 2;
        int barH = Math.max(16, (int) ((float) trackH / contentHeight * trackH));
        int usable = Math.max(1, trackH - barH);
        float frac = (float) (mouseY - (py + Ui.GAP) - thumbGrabOffset) / usable;
        scroll = Math.max(0, Math.min(maxScroll, Math.round(frac * maxScroll)));
        layout();
        return true;
    }

    public void mouseReleased(double mouseX, double mouseY) {
        thumbDragging = false;
    }

    public boolean isThumbDragging() {
        return thumbDragging;
    }

    private int[] thumbRect() {
        if (contentHeight <= ph - Ui.GAP * 2) return null;
        int trackH = ph - Ui.GAP * 2;
        int maxScroll = Math.max(0, contentHeight - trackH);
        int barH = Math.max(16, (int) ((float) trackH / contentHeight * trackH));
        int barY = py + Ui.GAP + (int) ((float) scroll / maxScroll * (trackH - barH));
        return new int[] { barY, barH };
    }

    public void clampScroll() {
        int maxScroll = Math.max(0, contentHeight - (ph - Ui.GAP * 2));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    public void clear() {
        clear(false);
    }

    public void clear(boolean keepScroll) {
        for (GuiEventListener widget : owned) {
            owner.unregisterWidget(widget);
        }
        owned.clear();
        slots.clear();
        labels.clear();
        dividers.clear();
        liveIndex.clear();
        popupClamped.clear();
        if (!keepScroll) {
            scroll = 0;
            contentHeight = 0;
            lastCursorY = 0;
        }
        layout();
    }

    public void setReadOnly(boolean readOnly) {
        for (GuiEventListener widget : owned) {
            if (widget instanceof EditBox box) {
                box.setEditable(!readOnly);
            } else if (widget instanceof net.minecraft.client.gui.components.AbstractWidget aw) {
                aw.active = !readOnly;
            }
        }
    }

    private void register(GuiEventListener widget, int relX, int relY, int w, int h) {
        owned.add(widget);
        if (widget instanceof net.minecraft.client.gui.components.AbstractWidget aw) {
            slots.add(new Slot(aw, relX, relY, w > 0 ? w : aw.getWidth(), h > 0 ? h : aw.getHeight()));
        }
        if (widget instanceof RegistryAutocompleteField f) {
            f.setPopupClamp(px, py + ph);
            popupClamped.add(f);
        }
        owner.registerWidget(widget);
        layout();
    }

    private void layout() {
        clampScroll();
        for (Slot slot : slots) {
            int y = py + slot.relY - scroll;
            if (y < py || y + slot.h > py + ph) {
                slot.widget.setX(-4000);
                slot.widget.setY(-4000);
            } else {
                slot.widget.setX(px + slot.relX);
                slot.widget.setY(y);
            }
        }
    }

    public record Label(int x, int y, String text, int color) {}

    public record Divider(int x, int y, int w) {}

    public record Slot(net.minecraft.client.gui.components.AbstractWidget widget, int relX, int relY, int w, int h) {}

    public static final class Cursor {
        public int x;
        public int y;
        public int w;

        Cursor(int x, int y, int w) {
            this.x = x;
            this.y = y;
            this.w = w;
        }

        public int right() {
            return x + w;
        }
    }
}