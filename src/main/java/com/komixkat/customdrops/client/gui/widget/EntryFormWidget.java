package com.komixkat.customdrops.client.gui.widget;

import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.screen.SplitPaneScreen;
import com.komixkat.customdrops.client.loot.VanillaLootTableReader;
import com.komixkat.customdrops.config.schema.EnchantmentEntry;
import com.komixkat.customdrops.config.schema.LootConditionEntry;
import com.komixkat.customdrops.config.schema.LootItemEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EntryFormWidget<T> {

    public interface Host<T> {

        List<T> list();

        T build(String targetId, boolean isTag, boolean replace, List<LootItemEntry> items);

        String targetOf(T entry);

        boolean isTagOf(T entry);

        boolean replaceOf(T entry);

        String entryLabel(T entry);

        List<LootItemEntry> itemsOf(T entry);

        String defaultTargetId();

        String targetFieldLabel();

        default boolean supportsTags() {
            return true;
        }

        default String defaultItemId() {
            return "minecraft:stone";
        }

        default java.util.List<LootConditionEntry.ConditionType> allowedConditions() {
            return java.util.List.of(
                LootConditionEntry.ConditionType.KILLED_BY_PLAYER,
                LootConditionEntry.ConditionType.ON_FIRE,
                LootConditionEntry.ConditionType.SILK_TOUCH,
                LootConditionEntry.ConditionType.NO_SILK_TOUCH,
                LootConditionEntry.ConditionType.ENTITY_ON_FIRE,
                LootConditionEntry.ConditionType.LOOTING_LEVEL_AT_LEAST);
        }

        default boolean supportsVanillaLink() {
            return false;
        }

        default boolean supportsLoadDefaults() {
            return supportsVanillaLink();
        }

        default List<LootItemEntry> loadVanillaDefaults(String targetId) {
            String table = vanillaLookupTarget(targetId);
            if (table == null || table.isBlank()) return List.of();
            return VanillaLootTableReader.load(table);
        }

        default String lookupKind() {
            return "item";
        }

        default String vanillaLookupTarget(String targetId) {
            return null;
        }
    }

    private final SplitPaneScreen owner;
    private final Host<T> host;
    private final ScrollablePane pane;

    private int index = -1;
    private boolean open = false;
    private boolean readOnly = false;
    private boolean keepScrollOnRebuild = true;
    private String targetId = "";
    private boolean isTag = false;
    private boolean replace = false;
    private List<LootItemEntry> items = List.of();
    private String formNotice = "";

    public EntryFormWidget(SplitPaneScreen owner, Host<T> host, int x, int y, int width, int height) {
        this.owner = owner;
        this.host = host;
        this.pane = new ScrollablePane(owner, x, y, width, height);
    }

    public void setBounds(int x, int y, int width, int height) {
        pane.setBounds(x, y, width, height);
    }

    public boolean isOpen() {
        return open;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (open) {
            rebuild();
        }
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void open(int index) {
        List<T> list = host.list();
        if (index < 0 || index >= list.size()) {
            close();
            return;
        }
        T entry = list.get(index);
        this.index = index;
        this.targetId = host.targetOf(entry);
        this.isTag = host.isTagOf(entry);
        this.replace = host.replaceOf(entry);
        this.items = new ArrayList<>(host.itemsOf(entry));
        this.open = true;
        keepScrollOnRebuild = false;
        rebuild();
        keepScrollOnRebuild = true;
    }

    public void close() {
        this.open = false;
        this.index = -1;
        pane.clear();
    }

    public int getIndex() {
        return index;
    }

    public void rebuild() {
        pane.clear(keepScrollOnRebuild);
        if (!open) return;

        ScrollablePane.Cursor cur = pane.newCursor();

        pane.addLabel(cur.x, cur.y - 1, "Editing" + (readOnly ? " (read-only)" : ""), Ui.MUTED);
        pane.addLabel(cur.x, cur.y + Ui.LINE_H, targetId.isEmpty() ? "(untitled)" : targetId, Ui.ACCENT);

        if (!readOnly) {
            int actionY = cur.y + Ui.LINE_H - 1;
            pane.addButton(cur, cur.right() - 70, actionY, 66, "+ Add Item", this::addItem);
            pane.addButton(cur, cur.right() - 152, actionY, 78, "Duplicate", this::duplicateEntry);
            pane.addButton(cur, cur.right() - 244, actionY, 88, "Delete Entry", this::deleteEntry);
        }
        cur.y += 2 * Ui.LINE_H + 22;

        if (!formNotice.isEmpty()) {
            pane.addLabel(cur.x, cur.y, formNotice, Ui.MUTED);
            cur.y += Ui.LINE_H + 4;
        }

        pane.addLabel(cur.x, cur.y, host.targetFieldLabel(), Ui.MUTED);
        boolean hasVanillaLink = host.supportsVanillaLink() && !readOnly;
        boolean hasLoadDefaults = host.supportsLoadDefaults() && !readOnly;
        int buttonRowY = cur.y + Ui.LINE_H + 2;
        if (hasLoadDefaults) {
            pane.addButton(cur, cur.right() - 204, buttonRowY, 100, "Load Defaults", this::loadDefaults);
        }
        if (hasVanillaLink) {
            pane.addButton(cur, cur.right() - 96, buttonRowY, 92, "View Vanilla", this::openVanilla);
        }
        int reserve = (hasVanillaLink ? 104 : 0) + (hasLoadDefaults ? 108 : 0);
        RegistryAutocompleteField targetBox = pane.addAutocomplete(cur.x, cur.y + Ui.LINE_H + 3,
            cur.w - reserve,
            targetId, this::onTargetChanged, java.util.Set.of(host.lookupKind()));
        targetBox.setEditable(!readOnly);
        cur.y += Ui.LINE_H + Ui.FIELD_H + 10;

        pane.addCheckbox(cur.x, cur.y, "Replace vanilla table entirely", replace,
            v -> {
                if (!readOnly) setReplace(v);
            });
        cur.y += Ui.CHECKBOX_H + 4;

        if (host.supportsTags()) {
            pane.addCheckbox(cur.x, cur.y, "Use as tag (#prefix)", isTag,
                v -> {
                    if (!readOnly) setIsTag(v);
                });
            cur.y += Ui.CHECKBOX_H + 4;
        }

        cur.y += 4;
        if (items.isEmpty()) {
            pane.addLabel(cur.x, cur.y, "No items yet. Press \"+ Add Item\" above to list a drop.", Ui.DIM);
            cur.y += Ui.LINE_H + 10;
        } else {
            pane.addLabel(cur.x, cur.y, "How each item card works:", Ui.SECTION_TEXT);
            cur.y += Ui.LINE_H;
            pane.addLabel(cur.x, cur.y, "Weight: the bigger the number, the more likely it is picked.", Ui.DIM);
            cur.y += Ui.LINE_H;
            pane.addLabel(cur.x, cur.y, "Chance %: the odds the item is allowed to drop on that pick.", Ui.DIM);
            cur.y += Ui.LINE_H;
            pane.addLabel(cur.x, cur.y, "Count min/max: how many drop in one go.", Ui.DIM);
            cur.y += Ui.LINE_H + 6;
        }

int totalWeight = 0;
        float avgChance = 0f;
        for (LootItemEntry it : items) {
            totalWeight += Math.max(1, it.weight());
            avgChance += Math.max(0f, Math.min(1f, it.chance()));
        }
        if (!items.isEmpty()) {
            avgChance /= items.size();
        }

        pane.addLabel(cur.x, cur.y, "Total summary", Ui.ACCENT);
        cur.y += Ui.LINE_H;
        pane.addLiveLabel("itemCount", cur.x, cur.y, items.size() + " item(s) listed \u00B7 combined weight " + totalWeight, Ui.MUTED);
        cur.y += Ui.LINE_H;
        pane.addLiveLabel("avgChance", cur.x, cur.y, "Average chance per item: " + String.format("%.0f%%", avgChance * 100f), Ui.MUTED);
        cur.y += Ui.LINE_H + 6;

        for (int i = 0; i < items.size(); i++) {
            buildItemBlock(cur, i, totalWeight);
        }

        pane.noteCursorY(cur.y);
        pane.finish(8);
        if (readOnly) {
            pane.setReadOnly(true);
        }
    }

    private void buildItemBlock(ScrollablePane.Cursor cur, int itemIndex, int totalWeight) {
        pane.addDivider(cur.x, cur.y, cur.w);
        cur.y += 8;
        pane.addLabel(cur.x, cur.y + 4, "Item " + (itemIndex + 1) + " of " + items.size(), Ui.SECTION_TEXT);
        if (!readOnly) {
            pane.addButton(cur, cur.right() - 96, cur.y + 2, 92, "Remove", () -> removeItem(itemIndex));
        }
        cur.y += 30;

        LootItemEntry item = items.get(itemIndex);
        int labW = 62;

        pane.addFieldLabel(cur.x, cur.y, "Item id", Ui.MUTED);
        pane.addAutocomplete(cur.x + labW, cur.y, cur.w - labW, item.itemId(),
            v -> {
                if (!readOnly) setItemId(itemIndex, v);
            }, java.util.Set.of("item"))
            .setEditable(!readOnly);
        cur.y += Ui.LINE_H + Ui.FIELD_H + 10;

        pane.addLabel(cur.x, cur.y, "Count min", Ui.MUTED);
        pane.addLabel(cur.x + 54, cur.y, "Count max", Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        pane.addEditBox(cur.x, cur.y, 46, String.valueOf(item.minCount()), "", v -> {
            if (!readOnly) setItemMin(itemIndex, v);
        });
        pane.addEditBox(cur.x + 54, cur.y, 46, String.valueOf(item.maxCount()), "", v -> {
            if (!readOnly) setItemMax(itemIndex, v);
        });
        cur.y += Ui.FIELD_H + 10;

        if (host.allowedConditions().contains(LootConditionEntry.ConditionType.FORTUNE_LEVEL_AT_LEAST)) {
            pane.addLabel(cur.x, cur.y, "Fortune bonus/level", Ui.MUTED);
            pane.addLabel(cur.x + 130, cur.y, "0 = off  \u00B7 1 = ore-style extra rolls", Ui.DIM);
            cur.y += Ui.LINE_H + 2;
            pane.addEditBox(cur.x, cur.y, 46, String.valueOf(item.fortuneBonus()), "", v -> {
                if (!readOnly) setItemFortuneBonus(itemIndex, v);
            });
            cur.y += Ui.FIELD_H + 10;
        }

        pane.addLabel(cur.x, cur.y, "Weight", Ui.MUTED);
        pane.addLabel(cur.x + 54, cur.y, "Chance %", Ui.MUTED);
        cur.y += Ui.LINE_H + 2;
        pane.addEditBox(cur.x, cur.y, 46, String.valueOf(item.weight()), "", v -> {
            if (!readOnly) setItemWeight(itemIndex, v);
        });
        pane.addEditBox(cur.x + 54, cur.y, 64, parseChance(item.chance()), "", v -> {
            if (!readOnly) setItemChance(itemIndex, v);
        });
        cur.y += Ui.FIELD_H + 12;

        float share = totalWeight > 0 ? (float) Math.max(1, item.weight()) / totalWeight : 0f;
        float effective = share * item.chance();
        pane.addLiveLabel("share." + itemIndex, cur.x, cur.y, "In practice, each roll lands on this item about "
            + String.format("%.1f%%", effective * 100f) + " of the time.", Ui.DIM);
        cur.y += Ui.LINE_H;
        pane.addLiveLabel("weightDetail." + itemIndex, cur.x, cur.y, "(Its weight " + Math.max(1, item.weight()) + " of " + totalWeight
            + " total, times the Chance % above.)", Ui.DIM);
        cur.y += Ui.LINE_H + 8;

        pane.addLabel(cur.x, cur.y, "Conditions", Ui.MUTED);
        cur.y += Ui.LINE_H + 4;

        int rowBase = cur.y;
        int colW = (cur.w - Ui.GAP) / 2;
        int rightX = cur.x + colW + Ui.GAP;

        List<LootConditionEntry.ConditionType> left = host.allowedConditions();

        String[][] labels = condLabels();

        if (left.isEmpty()) {
            pane.addLabel(cur.x, cur.y, "  (no conditions apply here)", Ui.DIM);
            cur.y += Ui.LINE_H + 4;
        }
        for (int i = 0; i < left.size() && i < 5; i++) {
            int yy = rowBase + i * Ui.CHECKBOX_H;
            LootConditionEntry.ConditionType type = left.get(i);
            String lab = labelFor(type, labels);
            pane.addCheckbox(cur.x, yy, lab, hasCondition(item, type),
                v -> {
                    if (!readOnly) setCondition(itemIndex, type, v, defaultParamsFor(type));
                });
        }
        int shown = Math.min(left.size(), 5);
        int rowsUsedLeft = shown;
        for (int i = 5; i < left.size() && i < 10; i++) {
            int r = i - 5;
            int yy = rowBase + r * Ui.CHECKBOX_H;
            LootConditionEntry.ConditionType type = left.get(i);
            String lab = labelFor(type, labels);
            pane.addCheckbox(rightX, yy, lab, hasCondition(item, type),
                v -> {
                    if (!readOnly) setCondition(itemIndex, type, v, defaultParamsFor(type));
                });
            rowsUsedLeft = Math.max(rowsUsedLeft, r + 1);
        }
        cur.y = rowBase + rowsUsedLeft * Ui.CHECKBOX_H + 4;

        if (hasCondition(item, LootConditionEntry.ConditionType.RANDOM_CHANCE)) {
            pane.addLabel(cur.x, cur.y, "Random chance value (0-1):", Ui.MUTED);
            pane.addEditBox(cur.x + 150, cur.y, cur.w - 150,
                paramOf(item, LootConditionEntry.ConditionType.RANDOM_CHANCE, "chance", "1.0"),
                "", v -> { if (!readOnly) setConditionParam(itemIndex, LootConditionEntry.ConditionType.RANDOM_CHANCE, "chance", v); });
            cur.y += Ui.LINE_H + Ui.FIELD_H + 8;
        }

        if (hasCondition(item, LootConditionEntry.ConditionType.LOOTING_LEVEL_AT_LEAST)) {
            pane.addLabel(cur.x, cur.y, "Looting level at least:", Ui.MUTED);
            pane.addEditBox(cur.x + 150, cur.y, cur.w - 150,
                paramOf(item, LootConditionEntry.ConditionType.LOOTING_LEVEL_AT_LEAST, "level", "1"),
                "", v -> { if (!readOnly) setConditionParam(itemIndex, LootConditionEntry.ConditionType.LOOTING_LEVEL_AT_LEAST, "level", v); });
            cur.y += Ui.LINE_H + Ui.FIELD_H + 8;
        }

        if (hasCondition(item, LootConditionEntry.ConditionType.FORTUNE_LEVEL_AT_LEAST)) {
            pane.addLabel(cur.x, cur.y, "Fortune level at least:", Ui.MUTED);
            pane.addEditBox(cur.x + 150, cur.y, cur.w - 150,
                paramOf(item, LootConditionEntry.ConditionType.FORTUNE_LEVEL_AT_LEAST, "level", "1"),
                "", v -> { if (!readOnly) setConditionParam(itemIndex, LootConditionEntry.ConditionType.FORTUNE_LEVEL_AT_LEAST, "level", v); });
            cur.y += Ui.LINE_H + Ui.FIELD_H + 8;
        }

        cur.y += 6;
        pane.addLabel(cur.x, cur.y, "Enchantments (chance applies to the whole item)", Ui.MUTED);
        cur.y += Ui.LINE_H + 6;

        List<EnchantmentEntry> enchants = item.enchantments();
        if (enchants.isEmpty()) {
            pane.addLabel(cur.x, cur.y, "  (none)", Ui.DIM);
        } else {
            for (int e = 0; e < enchants.size(); e++) {
                int enchIndex = e;
                EnchantmentEntry ench = enchants.get(e);
                int idW = cur.w - 40 - 2 * Ui.GAP - 36;
                pane.addAutocomplete(cur.x, cur.y, idW, ench.enchantmentId(), v -> {
                    if (!readOnly) setEnchantId(itemIndex, enchIndex, v);
                }, java.util.Set.of("enchantment")).setEditable(!readOnly);
                pane.addEditBox(cur.x + idW + Ui.GAP, cur.y, 28, String.valueOf(ench.level()), "", v -> { if (!readOnly) setEnchantLevel(itemIndex, enchIndex, v); });
                if (!readOnly) {
                    pane.addButton(cur, cur.x + cur.w - 36, cur.y - 1, 34, "x", () -> removeEnchantment(itemIndex, enchIndex));
                }
                cur.y += Ui.FIELD_H + 3;
            }
        }

        if (!readOnly) {
            pane.addButton(cur, cur.x, cur.y, 120, "+ Add Enchantment", () -> addEnchantment(itemIndex));
            cur.y += Ui.BUTTON_H + 16;
        } else {
            cur.y += 6;
        }
    }

    private void addItem() {
        LootItemEntry entry = new LootItemEntry(host.defaultItemId(), 1, 1, 1, 1.0f, List.of(), List.of());
        List<LootItemEntry> next = new ArrayList<>(items);
        next.add(entry);
        items = next;
        commit();
        rebuild();
    }

    private void removeItem(int itemIndex) {
        if (itemIndex < 0 || itemIndex >= items.size()) return;
        List<LootItemEntry> next = new ArrayList<>(items);
        next.remove(itemIndex);
        if (next.isEmpty()) {
            next.add(new LootItemEntry(host.defaultItemId(), 1, 1, 1, 1.0f, List.of(), List.of()));
        }
        items = next;
        commit();
        rebuild();
    }

    private void duplicateEntry() {
        T built = host.build(targetId, isTag, replace, List.copyOf(items));
        int insertAt = open ? index + 1 : host.list().size();
        host.list().add(insertAt, built);
        owner.markChanged();
        open(insertAt);
        owner.onEntryListChanged();
    }

    private void deleteEntry() {
        if (!open) return;
        List<T> list = host.list();
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            owner.markChanged();
        }
        close();
        owner.onEntryListChanged();
    }

    private void addEnchantment(int itemIndex) {
        LootItemEntry item = items.get(itemIndex);
        List<EnchantmentEntry> next = new ArrayList<>(item.enchantments());
        next.add(new EnchantmentEntry("minecraft:unbreaking", 1));
        items.set(itemIndex, withEnchantments(item, next));
        commit();
        rebuild();
    }

    private void removeEnchantment(int itemIndex, int enchIndex) {
        LootItemEntry item = items.get(itemIndex);
        List<EnchantmentEntry> next = new ArrayList<>(item.enchantments());
        if (enchIndex >= 0 && enchIndex < next.size()) {
            next.remove(enchIndex);
        }
        items.set(itemIndex, withEnchantments(item, next));
        commit();
        rebuild();
    }

    private void setEnchantId(int itemIndex, int enchIndex, String value) {
        LootItemEntry item = items.get(itemIndex);
        List<EnchantmentEntry> next = new ArrayList<>(item.enchantments());
        if (enchIndex < 0 || enchIndex >= next.size()) return;
        EnchantmentEntry old = next.get(enchIndex);
        try {
            next.set(enchIndex, new EnchantmentEntry(value, old.level()));
        } catch (Exception e) {
            return;
        }
        items.set(itemIndex, withEnchantments(item, next));
        commit();
    }

    private void setEnchantLevel(int itemIndex, int enchIndex, String value) {
        LootItemEntry item = items.get(itemIndex);
        List<EnchantmentEntry> next = new ArrayList<>(item.enchantments());
        if (enchIndex < 0 || enchIndex >= next.size()) return;
        EnchantmentEntry old = next.get(enchIndex);
        try {
            next.set(enchIndex, new EnchantmentEntry(old.enchantmentId(), Integer.parseInt(value.trim())));
        } catch (Exception e) {
            return;
        }
        items.set(itemIndex, withEnchantments(item, next));
        commit();
    }

    private void setCondition(int itemIndex, LootConditionEntry.ConditionType type, boolean enabled, Map<String, String> defaultParams) {
        LootItemEntry item = items.get(itemIndex);
        List<LootConditionEntry> next = new ArrayList<>(item.conditions());

        if (enabled && type == LootConditionEntry.ConditionType.SILK_TOUCH) {
            next.removeIf(c -> c.type() == LootConditionEntry.ConditionType.NO_SILK_TOUCH);
        }
        if (enabled && type == LootConditionEntry.ConditionType.NO_SILK_TOUCH) {
            next.removeIf(c -> c.type() == LootConditionEntry.ConditionType.SILK_TOUCH);
        }

        boolean found = false;
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).type() == type) {
                if (enabled) {
                    next.set(i, new LootConditionEntry(type, next.get(i).params().isEmpty() ? defaultParams : next.get(i).params()));
                } else {
                    next.remove(i);
                }
                found = true;
                break;
            }
        }
        if (!found && enabled) {
            next.add(new LootConditionEntry(type, defaultParams));
        }
        items.set(itemIndex, new LootItemEntry(item.itemId(), item.weight(), item.minCount(), item.maxCount(), item.chance(), next, item.enchantments(), item.fortuneBonus()));
        commit();
        if (type == LootConditionEntry.ConditionType.RANDOM_CHANCE
            || type == LootConditionEntry.ConditionType.LOOTING_LEVEL_AT_LEAST
            || type == LootConditionEntry.ConditionType.FORTUNE_LEVEL_AT_LEAST) {
            rebuild();
        }
    }

    private void setConditionParam(int itemIndex, LootConditionEntry.ConditionType type, String key, String value) {
        if (value == null || value.isBlank()) return;
        LootItemEntry item = items.get(itemIndex);
        List<LootConditionEntry> next = new ArrayList<>(item.conditions());
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).type() == type) {
                Map<String, String> params = new HashMap<>(next.get(i).params());
                params.put(key, value);
                next.set(i, new LootConditionEntry(type, params));
            }
        }
        items.set(itemIndex, new LootItemEntry(item.itemId(), item.weight(), item.minCount(), item.maxCount(), item.chance(), next, item.enchantments(), item.fortuneBonus()));
        commit();
    }

    private void setItemId(int itemIndex, String value) {
        LootItemEntry item = items.get(itemIndex);
        items.set(itemIndex, new LootItemEntry(value, item.weight(), item.minCount(), item.maxCount(), item.chance(), item.conditions(), item.enchantments(), item.fortuneBonus()));
        commit();
    }

    private void setItemWeight(int itemIndex, String value) {
        try {
            int w = Integer.parseInt(value.trim());
            if (w < 1) return;
            LootItemEntry item = items.get(itemIndex);
            items.set(itemIndex, new LootItemEntry(item.itemId(), w, item.minCount(), item.maxCount(), item.chance(), item.conditions(), item.enchantments(), item.fortuneBonus()));
            commit();
            refreshPercentStats();
        } catch (Exception e) {
            // invalid input, keep previous value
        }
    }

    private void setItemMin(int itemIndex, String value) {
        try {
            int v = Integer.parseInt(value.trim());
            LootItemEntry item = items.get(itemIndex);
            if (v < 0 || v > item.maxCount()) return;
            items.set(itemIndex, new LootItemEntry(item.itemId(), item.weight(), v, item.maxCount(), item.chance(), item.conditions(), item.enchantments(), item.fortuneBonus()));
            commit();
        } catch (Exception e) {
            // invalid input, keep previous value
        }
    }

    private void setItemMax(int itemIndex, String value) {
        try {
            int v = Integer.parseInt(value.trim());
            LootItemEntry item = items.get(itemIndex);
            if (v < item.minCount()) return;
            items.set(itemIndex, new LootItemEntry(item.itemId(), item.weight(), item.minCount(), v, item.chance(), item.conditions(), item.enchantments(), item.fortuneBonus()));
            commit();
        } catch (Exception e) {
            // invalid input, keep previous value
        }
    }

    private void setItemChance(int itemIndex, String value) {
        String cleaned = value != null ? value.trim() : "";
        if (cleaned.isEmpty()) return;
        try {
            float f;
            if (!cleaned.contains(".")) {
                int percent = Integer.parseInt(cleaned);
                if (percent < 0 || percent > 100) return;
                f = percent / 100.0f;
            } else {
                f = Float.parseFloat(cleaned);
                if (f < 0f || f > 1f) return;
            }
            LootItemEntry item = items.get(itemIndex);
            items.set(itemIndex, new LootItemEntry(item.itemId(), item.weight(), item.minCount(), item.maxCount(), f, item.conditions(), item.enchantments(), item.fortuneBonus()));
            commit();
            refreshPercentStats();
        } catch (Exception e) {
            // invalid input, keep previous value
        }
    }

    private void setItemFortuneBonus(int itemIndex, String value) {
        try {
            int v = Integer.parseInt(value.trim());
            if (v < 0) return;
            LootItemEntry item = items.get(itemIndex);
            items.set(itemIndex, new LootItemEntry(item.itemId(), item.weight(), item.minCount(), item.maxCount(), item.chance(), item.conditions(), item.enchantments(), v));
            commit();
        } catch (Exception e) {
            // invalid input, keep previous value
        }
    }

    private void setReplace(boolean val) {
        replace = val;
        commit();
    }

    private void setIsTag(boolean val) {
        isTag = val;
        commit();
    }

    private void onTargetChanged(String value) {
        targetId = value == null ? "" : value;
        commit();
        owner.onTargetIdEdited(targetId);
    }

    private void commit() {
        if (!open || index < 0 || index >= host.list().size()) return;
        if (readOnly) return;
        T built = host.build(targetId, isTag, replace, List.copyOf(items));
        if (Objects.equals(host.list().get(index), built)) return;
        host.list().set(index, built);
        owner.markChanged();
    }

    private void refreshPercentStats() {
        int totalWeight = 0;
        float avgChance = 0f;
        for (LootItemEntry it : items) {
            totalWeight += Math.max(1, it.weight());
            avgChance += Math.max(0f, Math.min(1f, it.chance()));
        }
        if (!items.isEmpty()) {
            avgChance /= items.size();
        }
        pane.setLiveLabel("itemCount", items.size() + " item(s) listed \u00B7 combined weight " + totalWeight);
        pane.setLiveLabel("avgChance", "Average chance per item: " + String.format("%.0f%%", avgChance * 100f));
        for (int i = 0; i < items.size(); i++) {
            LootItemEntry item = items.get(i);
            float share = totalWeight > 0 ? (float) Math.max(1, item.weight()) / totalWeight : 0f;
            float effective = share * item.chance();
            pane.setLiveLabel("share." + i,
                "In practice, each roll lands on this item about " + String.format("%.1f%%", effective * 100f) + " of the time.");
            pane.setLiveLabel("weightDetail." + i,
                "(Its weight " + Math.max(1, item.weight()) + " of " + totalWeight + " total, times the Chance % above.)");
        }
    }

    private static LootItemEntry withEnchantments(LootItemEntry item, List<EnchantmentEntry> enchants) {
        return new LootItemEntry(item.itemId(), item.weight(), item.minCount(), item.maxCount(), item.chance(), item.conditions(), enchants, item.fortuneBonus());
    }

    private static Map<String, String> defaultParamsFor(LootConditionEntry.ConditionType type) {
        return switch (type) {
            case RANDOM_CHANCE -> Map.of("chance", "1.0");
            case LOOTING_LEVEL_AT_LEAST, FORTUNE_LEVEL_AT_LEAST -> Map.of("level", "1");
            default -> Map.of();
        };
    }

    private static boolean hasCondition(LootItemEntry item, LootConditionEntry.ConditionType type) {
        return item.conditions().stream().anyMatch(c -> c.type() == type);
    }

    private static String paramOf(LootItemEntry item, LootConditionEntry.ConditionType type, String key, String fallback) {
        for (LootConditionEntry c : item.conditions()) {
            if (c.type() == type) {
                return c.params().getOrDefault(key, fallback);
            }
        }
        return fallback;
    }

    private static String parseChance(float chance) {
        if (Math.round(chance * 100) == chance * 100) {
            return String.valueOf(Math.round(chance * 100));
        }
        return String.format("%.3f", chance);
    }

    private static String[][] condLabels() {
        return new String[][] {
            { "Killed by player", "On fire", "Require Silk Touch", "Require NOT Silk Touch", "Entity on fire" },
            { "Random chance", "Looting level", "Fortune level" }
        };
    }

    private static String labelFor(LootConditionEntry.ConditionType type, String[][] labels) {
        return switch (type) {
            case KILLED_BY_PLAYER -> labels[0][0];
            case ON_FIRE -> labels[0][1];
            case SILK_TOUCH -> labels[0][2];
            case NO_SILK_TOUCH -> labels[0][3];
            case ENTITY_ON_FIRE -> labels[0][4];
            case RANDOM_CHANCE -> labels[1][0];
            case LOOTING_LEVEL_AT_LEAST -> labels[1][1];
            case FORTUNE_LEVEL_AT_LEAST -> labels[1][2];
            default -> type.name().toLowerCase();
        };
    }

    private void openVanilla() {
        String vanillaId = host.vanillaLookupTarget(targetId);
        if (vanillaId == null || vanillaId.isBlank()) {
            return;
        }
        owner.openVanillaLookup(vanillaId);
    }

    private void loadDefaults() {
        if (readOnly) return;
        List<LootItemEntry> defaults = host.loadVanillaDefaults(targetId);
        if (defaults == null || defaults.isEmpty()) {
            formNotice = "No vanilla default items found for '" + targetId + "'.";
        } else {
            items = new ArrayList<>(defaults);
            formNotice = "Loaded " + defaults.size() + " vanilla default item(s) for '" + targetId + "'.";
            commit();
        }
        rebuild();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!open) return false;
        return pane.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) return false;
        return pane.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!open) return false;
        return pane.mouseDragged(mouseX, mouseY);
    }

    public void mouseReleased(double mouseX, double mouseY) {
        if (open) {
            pane.mouseReleased(mouseX, mouseY);
        }
    }

    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (!open) return;
        pane.render(guiGraphics, mouseX, mouseY, delta);
    }
}