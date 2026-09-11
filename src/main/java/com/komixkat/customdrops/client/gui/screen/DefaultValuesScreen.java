package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.widget.RegistryAutocompleteField;
import com.komixkat.customdrops.client.gui.widget.ScrollablePane;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DefaultValuesScreen extends SplitPaneScreen {

    private ScrollablePane pane;
    private RegistryAutocompleteField tableField;
    private String statusMessage = "";
    private String loadedId = "";
    private JsonObject loadedTable = null;
    private static final int MAX_NEST_DEPTH = 1;

    private final String initialId;

    public DefaultValuesScreen(net.minecraft.client.gui.screens.Screen parent) {
        this(parent, null);
    }

    public DefaultValuesScreen(net.minecraft.client.gui.screens.Screen parent, String initialId) {
        super(parent, Component.translatable("customdrops.config.defaults.active"));
        this.initialId = initialId;
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
        navWidget.addCategory("Examples");
        addExample("chests/simple_dungeon");
        addExample("chests/abandoned_mineshaft");
        addExample("archaeology/desert_pyramid");
        addExample("entities/zombie");
        addExample("entities/shulker");
        addExample("gameplay/fishing");
        addExample("blocks/stone");
        navWidget.addCategory("Tags");
        navWidget.addEntry("Tags", "Tags list\u2026", () ->
            this.minecraft.gui.setScreen(new TagsBrowserScreen(this)));
    }

    private void addExample(String id) {
        String display = id.replace("chests/", "Chest: ").replace("archaeology/", "Archaeology: ")
            .replace("entities/", "Entity: ").replace("gameplay/", "Gameplay: ")
            .replace("blocks/", "Block: ");
        navWidget.addEntry("Examples", display, () -> {
            load(id);
            if (tableField != null) {
                tableField.setValue(id);
            }
        });
    }

    @Override
    protected void initContent() {
        int x = rightPanelX + PADDING;
        int totalW = Math.max(0, rightPanelWidth - PADDING * 2);
        int btnW = 64;

        pane = new ScrollablePane(this, x, contentY + 60, totalW,
            Math.max(0, contentBottom - (contentY + 60) - PADDING - 24));

        tableField = new RegistryAutocompleteField(this, x, PADDING, totalW - btnW - 4, Ui.FIELD_H, Component.literal(""), registryIndex);
        tableField.setSuggestor(this::loadIfTableField);
        tableField.setSuggestionKinds(java.util.Set.of("loot"));
        tableField.setPopupClamp(x + totalW, contentBottom);
        addRenderableWidget(tableField);

        Button loadBtn = Button.builder(Component.literal("Load"), b -> load(tableField.getValue()))
            .bounds(x + totalW - btnW, PADDING, btnW, Ui.FIELD_H)
            .build();
        addRenderableWidget(loadBtn);

        buildHub();
        if (initialId != null && !initialId.isBlank()) {
            tableField.setValue(initialId);
            load(initialId);
        }
    }

    private String lastAttempted = "";

    private void loadIfTableField(String v) {
        String text = v == null ? "" : v.trim();
        if (text.isEmpty() || text.equals(lastAttempted)) return;
        lastAttempted = text;
        // Trigger suggestions on any text change for live autocomplete
        load(text);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        guiGraphics.text(font, "Default Values (from vanilla data packs)", rightPanelX + 8, contentY + 34, 0xFFE0E0E0, false);
        guiGraphics.text(font, "Type a loot table id, or pick an example on the left.", rightPanelX + 8, contentY + 48, 0xFF888888, false);
        if (pane != null) {
            pane.render(guiGraphics, mouseX, mouseY, delta);
        }
        if (!statusMessage.isEmpty()) {
            drawStatus(guiGraphics, statusMessage, loadedTable == null ? Ui.WARN : Ui.DIM);
        }
    }

    @Override
    protected boolean contentMouseClicked(double mouseX, double mouseY, int button) {
        if (pane != null && pane.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    protected boolean contentMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (pane != null) return pane.mouseScrolled(mouseX, mouseY, verticalAmount);
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

    private void buildHub() {
        if (pane == null) return;
        pane.clear();
        ScrollablePane.Cursor cur = pane.newCursor();
        if (loadedTable == null) {
            pane.addLabel(cur.x, cur.y, "Nothing loaded yet.", Ui.TEXT);
            cur.y += Ui.LINE_H + 4;
            pane.addLabel(cur.x, cur.y, "Type an id into the box above (e.g. minecraft:chests/simple_dungeon)", Ui.MUTED);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, "and press Enter, or click a Table Load button / example.",
                Ui.MUTED);
            cur.y += Ui.LINE_H + 2;
            pane.addLabel(cur.x, cur.y, "Works for chests, fishing, archaeology, entities and blocks.", Ui.DIM);
            pane.noteCursorY(cur.y);
            pane.finish(8);
            return;
        }

        pane.addLabel(cur.x, cur.y, "Table: " + loadedId, Ui.TEXT);
        cur.y += Ui.LINE_H + 4;

        JsonElement type = loadedTable.get("type");
        if (type != null && type.isJsonPrimitive()) {
            pane.addLabel(cur.x, cur.y, "type: " + type.getAsString(), Ui.DIM);
            cur.y += Ui.LINE_H;
        }

        JsonArray pools = loadedTable.has("pools") ? loadedTable.getAsJsonArray("pools") : null;
        if (pools == null || pools.isEmpty()) {
            pane.addLabel(cur.x, cur.y, "(this table holds no pools — just the type marker)", Ui.DIM);
            cur.y += Ui.LINE_H + 6;
        } else {
            for (int p = 0; p < pools.size(); p++) {
                JsonObject pool = pools.get(p).isJsonObject() ? pools.get(p).getAsJsonObject() : null;
                if (pool == null) continue;
                String rolls = rollsOf(pool.get("rolls"));
                pane.addLabel(cur.x, cur.y, "Pool " + (p + 1) + " of " + pools.size() + "   rolls: " + rolls, Ui.SECTION_TEXT);
                cur.y += Ui.LINE_H + 2;
                JsonArray entries = pool.has("entries") ? pool.getAsJsonArray("entries") : new JsonArray();
                if (entries.isEmpty()) {
                    pane.addLabel(cur.x + 8, cur.y, "  (empty pool)", Ui.DIM);
                    cur.y += Ui.LINE_H + 2;
                } else {
                    for (JsonElement entryEl : entries) {
                        if (entryEl.isJsonObject()) {
                            cur = renderEntry(cur, entryEl.getAsJsonObject(), 0);
                        }
                    }
                }
                List<String> conds = conditionsOf(pool.get("conditions"));
                if (!conds.isEmpty()) {
                    pane.addLabel(cur.x + 8, cur.y, "  pool conditions: " + String.join(", ", conds), Ui.DIM);
                    cur.y += Ui.LINE_H + 2;
                }
                cur.y += 4;
            }
        }
        pane.noteCursorY(cur.y);
        pane.finish(8);
    }

    private ScrollablePane.Cursor renderEntry(ScrollablePane.Cursor cur, JsonObject entry, int depth) {
        String indent = " ".repeat(depth * 2);
        String type = strOr(entry.get("type"), "");
        String ref = refOf(entry);
        int weight = intOr(entry.get("weight"), 1);
        int quality = intOr(entry.get("quality"), 0);

        String symbol;
        String head;
        if (type.endsWith("loot_table")) {
            symbol = "→";
            head = indent + symbol + " table " + ref;
        } else if (type.endsWith("tag")) {
            symbol = "⊞";
            head = indent + symbol + " tag " + ref;
        } else {
            symbol = "+";
            head = indent + symbol + " " + (type.isEmpty() ? "item" : type.replace("minecraft:", "")) + " " + ref;
        }
        String meta = "   weight " + weight + (quality > 0 ? "  quality " + quality : "");
        int metaW = font.width(meta) + 2;
        int headW = Math.max(20, cur.w - metaW);
        pane.addLabel(cur.x, cur.y, truncAt(head, headW), Ui.TEXT);
        pane.addLabel(cur.x + cur.w - metaW, cur.y, meta, Ui.DIM);
        cur.y += Ui.LINE_H;

        List<String> fns = functionsOf(entry.get("functions"));
        for (String fn : fns) {
            pane.addLabel(cur.x + 14, cur.y, truncAt(fn, cur.w), Ui.DIM);
            cur.y += Ui.LINE_H;
        }
        List<String> conds = conditionsOf(entry.get("conditions"));
        for (String cond : conds) {
            pane.addLabel(cur.x + 14, cur.y, "cond: " + truncAt(cond, cur.w), Ui.DIM);
            cur.y += Ui.LINE_H;
        }

        if (type.endsWith("loot_table") && depth < MAX_NEST_DEPTH) {
            JsonObject nested = loadTableJson(ref);
            if (nested != null) {
                JsonArray pools = nested.has("pools") ? nested.getAsJsonArray("pools") : null;
                if (pools != null) {
                    for (int p = 0; p < pools.size(); p++) {
                        JsonObject pool = pools.get(p).isJsonObject() ? pools.get(p).getAsJsonObject() : null;
                        if (pool == null) continue;
                        JsonArray entries = pool.has("entries") ? pool.getAsJsonArray("entries") : new JsonArray();
                        for (JsonElement entryEl : entries) {
                            if (entryEl.isJsonObject()) {
                                cur = renderEntry(cur, entryEl.getAsJsonObject(), depth + 1);
                            }
                        }
                    }
                }
            } else {
                pane.addLabel(cur.x + 14, cur.y, "  (nested table could not be resolved)", Ui.DIM);
                cur.y += Ui.LINE_H;
            }
        }
        return cur;
    }

    private String truncAt(String text, int maxWidth) {
        String shortName = text;
        if (shortName != null && !shortName.isEmpty()) {
            return font.plainSubstrByWidth(shortName, Math.max(20, maxWidth));
        }
        return text;
    }

    private static String refOf(JsonObject entry) {
        String type = strOr(entry.get("type"), "");
        if (type.endsWith("loot_table")) {
            if (entry.has("value") && entry.get("value").isJsonPrimitive()) {
                String value = entry.get("value").getAsString();
                if (value != null && !value.isBlank()) return value;
            }
        }
        return strOr(entry.get("name"), "");
    }

    private String rollsOf(JsonElement rolls) {
        if (rolls == null || !rolls.isJsonObject()) {
            return rolls == null || rolls.isJsonNull() ? "1" : rolls.toString();
        }
        JsonObject o = rolls.getAsJsonObject();
        String min = rangeVal(o.get("min"));
        String max = rangeVal(o.get("max"));
        return (min.isEmpty() ? "?" : min) + " - " + (max.isEmpty() ? "?" : max);
    }

    private String rangeVal(JsonElement e) {
        if (e == null || e.isJsonNull()) return "";
        if (e.isJsonPrimitive()) return e.getAsString();
        return e.toString();
    }

    private List<String> functionsOf(JsonElement functionsEl) {
        List<String> out = new ArrayList<>();
        if (functionsEl == null || !functionsEl.isJsonArray()) return out;
        JsonArray functions = functionsEl.getAsJsonArray();
        for (JsonElement fnEl : functions) {
            if (!fnEl.isJsonObject()) continue;
            JsonObject fn = fnEl.getAsJsonObject();
            String fnName = strOr(fn.get("function"), "function").replace("minecraft:", "");
            switch (fnName) {
                case "set_count" -> out.add("count: " + rangeVal(fn.get("count")));
                case "set_enchantments" -> out.add("sets enchantments");
                case "enchant_randomly" -> out.add("enchant: random (level bonus from roll)");
                case "set_contents" -> out.add("stacks contents of: " + strOr(fn.get("contents"), "?"));
                case "apply_bonus" -> out.add("bonus apply with formula");
                case "looting_enchant" -> out.add("scales with Looting");
                case "smelt" -> out.add("smelted form");
                case "explosion_decay" -> out.add("chance to be destroyed by explosions");
                case "set_lore" -> out.add("adds lore");
                case "set_name" -> out.add("renamed item");
                case "set_attributes" -> out.add("applies attributes");
                case "set_nbt" -> out.add("NBT changes");
                case "furnace_smelt" -> out.add("smelted form");
                default -> out.add("function: " + fnName);
            }
        }
        return out;
    }

    private List<String> conditionsOf(JsonElement condsEl) {
        List<String> out = new ArrayList<>();
        if (condsEl == null || !condsEl.isJsonArray()) return out;
        JsonArray conditions = condsEl.getAsJsonArray();
        for (JsonElement cEl : conditions) {
            if (!cEl.isJsonObject()) continue;
            JsonObject c = cEl.getAsJsonObject();
            String cName = strOr(c.get("condition"), "condition").replace("minecraft:", "");
            switch (cName) {
                case "random_chance" -> out.add("random_chance " + rangeVal(c.get("chance")));
                case "killed_by_player" -> out.add("killed_by_player");
                case "entity_scores" -> out.add("entity_scores");
                case "table_condition" -> out.add("table_condition");
                default -> out.add(cName);
            }
        }
        return out;
    }

    private static String strOr(JsonElement e, String fallback) {
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) return fallback;
        return e.getAsString();
    }

    private static int intOr(JsonElement e, int fallback) {
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) return fallback;
        try {
            return e.getAsInt();
        } catch (Exception ex) {
            return fallback;
        }
    }

    private void load(String id) {
        if (id == null) return;
        String clean = id.trim().startsWith("#") ? id.trim().substring(1).trim() : id.trim();
        if (clean.isEmpty()) return;
        JsonObject table = loadTableJson(clean);
        if (table == null) {
            loadedTable = null;
            String hint = clean.contains("/") ? clean : "e.g. minecraft:blocks/stone or minecraft:chests/simple_dungeon";
            statusMessage = "Loot table '" + clean + "' not found. " + hint + ".";
        } else {
            loadedId = clean;
            loadedTable = table;
            statusMessage = "Loaded '" + clean + "'.";
        }
        buildHub();
    }

    private JsonObject loadTableJson(String id) {
        String[] parts = splitId(id);
        String namespace = parts[0];
        String path = parts[1];
        Minecraft mc = Minecraft.getInstance();

        // Try the path as-is first (for proper loot table paths like "chests/simple_dungeon")
        JsonObject table = readJson(mc, namespace, "loot_table/" + path + ".json");
        if (table == null) {
            table = readJson(mc, namespace, "loot_tables/" + path + ".json");
        }

        // Bare id like "stone" or "minecraft:stone" isn't a loot table path by itself;
        // map it to the most likely block/entity loot table.
        if (table == null && !path.contains("/")) {
            // Try blocks first (most common for bare ids like "stone", "diamond", etc.)
            table = readJson(mc, namespace, "loot_table/blocks/" + path + ".json");
            if (table == null) {
                table = readJson(mc, namespace, "loot_tables/blocks/" + path + ".json");
            }
            if (table != null) {
                loadedId = namespace + ":blocks/" + path;
                return table;
            }
            // Try entities (for mob names like "zombie", "creeper")
            table = readJson(mc, namespace, "loot_table/entities/" + path + ".json");
            if (table == null) {
                table = readJson(mc, namespace, "loot_tables/entities/" + path + ".json");
            }
            if (table != null) {
                loadedId = namespace + ":entities/" + path;
                return table;
            }
            // Try gameplay (for fishing, etc.)
            table = readJson(mc, namespace, "loot_table/gameplay/" + path + ".json");
            if (table == null) {
                table = readJson(mc, namespace, "loot_tables/gameplay/" + path + ".json");
            }
            if (table != null) {
                loadedId = namespace + ":gameplay/" + path;
            }
        }
        return table;
    }

    private static String[] splitId(String id) {
        String namespace = "minecraft";
        String path = id;
        int colon = id.indexOf(':');
        if (colon >= 0) {
            namespace = id.substring(0, colon);
            path = id.substring(colon + 1);
        }
        if (path.startsWith("/")) path = path.substring(1);
        return new String[]{namespace, path};
    }

    private JsonObject readJson(Minecraft mc, String namespace, String resourcePath) {
        JsonObject bundled = readBundled(namespace, resourcePath);
        if (bundled != null) return bundled;
        Identifier loc = Identifier.tryBuild(namespace, resourcePath);
        if (loc == null) return null;
        try {
            Optional<Resource> res = mc.getResourceManager().getResource(loc);
            return parseResource(res);
        } catch (Exception e) {
            return null;
        }
    }

    private JsonObject readBundled(String namespace, String resourcePath) {
        String normalized = resourcePath;
        if (normalized.startsWith("loot_table/")) {
            normalized = normalized.substring("loot_table/".length());
        } else if (normalized.startsWith("loot_tables/")) {
            normalized = normalized.substring("loot_tables/".length());
        }
        String classpath = "/data/customdrops/generated/loot_tables/" + namespace + "/" + normalized;
        try (var stream = getClass().getResourceAsStream(classpath)) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonObject parseResource(Optional<Resource> res) {
        if (res.isEmpty()) return null;
        try (var stream = res.get().open(); Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }
}