package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.UiSfx;
import com.komixkat.customdrops.client.gui.widget.EntryListWidget;
import com.komixkat.customdrops.registry.VanillaLootTableRegistry;
import com.mojang.blaze3d.platform.ClipboardManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BrowseScreen extends SplitPaneScreen {

    private EntryListWidget entryList;
    private EditBox searchField;
    private Button clearSearchButton;
    private List<String> allTables = new ArrayList<>();
    private String activeGroup = null;
    private String searchQuery = "";
    private String statusMessage = "";
    private long statusUntil = 0;

    public BrowseScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(parent, Component.translatable("customdrops.menu.browse"));
    }

    @Override
    protected String backButtonLabel() {
        return "Back";
    }

    @Override
    protected void buildNavigation() {
        allTables.clear();
        allTables.addAll(VanillaLootTableRegistry.search(""));

        navWidget.addCategory("Loot Tables");
        navWidget.addEntry("Loot Tables", "All (" + allTables.size() + ")", () -> setGroup(null));

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String table : allTables) {
            counts.merge(folderOf(table), 1, Integer::sum);
        }
        counts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> navWidget.addEntry("Loot Tables",
                e.getKey() + " (" + e.getValue() + ")", () -> setGroup(e.getKey())));
    }

    @Override
    protected void initContent() {
        int fieldX = rightPanelX + PADDING + 52;
        int fieldW = Math.max(0, rightPanelWidth - PADDING * 2 - 52 - 26);

        searchField = new EditBox(font, fieldX, contentY, fieldW, 18, Component.literal("Search id..."));
        searchField.setMaxLength(256);
        searchField.setResponder(this::onSearchChanged);
        addWidget(searchField);

        clearSearchButton = Button.builder(Component.literal("x"), b -> clearSearch())
            .bounds(fieldX + fieldW + 4, contentY, 20, 18).build();
        clearSearchButton.active = !searchQuery.isEmpty();
        addRenderableWidget(clearSearchButton);

        entryList = new EntryListWidget(
            rightPanelX + PADDING, contentY + 26,
            Math.max(0, rightPanelWidth - PADDING * 2),
            Math.max(0, contentBottom - contentY - 26 - 26));
        rebuildList();
    }

    private void clearSearch() {
        if (searchField != null) searchField.setValue("");
        searchQuery = "";
        rebuildList();
    }

    private void setGroup(String group) {
        activeGroup = group;
        rebuildList();
    }

    private void onSearchChanged(String query) {
        searchQuery = query == null ? "" : query.trim();
        if (clearSearchButton != null) {
            clearSearchButton.active = !searchQuery.isEmpty();
        }
        rebuildList();
    }

    private void rebuildList() {
        if (entryList == null) return;

        List<EntryRow2> matches = new ArrayList<>();
        boolean searching = !searchQuery.isEmpty();

        for (String table : allTables) {
            if (!searching) {
                if (activeGroup != null && !folderOf(table).equals(activeGroup)) continue;
                matches.add(new EntryRow2(table, 0));
            } else {
                int score = fuzzyScore(table, searchQuery);
                if (score >= 0) {
                    matches.add(new EntryRow2(table, score));
                }
            }
        }

        if (searching) {
            matches.sort((a, b) -> {
                int byScore = Integer.compare(b.score, a.score);
                return byScore != 0 ? byScore : a.id.compareTo(b.id);
            });
        } else {
            matches.sort((a, b) -> a.id.compareTo(b.id));
        }

        List<EntryListWidget.EntryRow> rows = new ArrayList<>();
        Map<String, List<EntryRow2>> sections = new LinkedHashMap<>();
        if (!searching && activeGroup != null) {
            for (EntryRow2 m : matches) {
                rows.add(rowFor(m));
            }
        } else {
            for (EntryRow2 m : matches) {
                sections.computeIfAbsent(folderOf(m.id), k -> new ArrayList<>()).add(m);
            }
            for (Map.Entry<String, List<EntryRow2>> section : sections.entrySet()) {
                rows.add(EntryListWidget.section(section.getKey()));
                for (EntryRow2 m : section.getValue()) {
                    rows.add(rowFor(m));
                }
            }
        }

        entryList.setRows(rows);
        statusMessage = (searching ? "Matches: " : "") + matches.size() + " of " + allTables.size()
            + " table" + (allTables.size() == 1 ? "" : "s")
            + "  \u00B7  click a row to copy its id";
    }

    private EntryListWidget.EntryRow rowFor(EntryRow2 m) {
        String id = m.id;
        return new EntryListWidget.EntryRow() {
            @Override
            public void render(GuiGraphicsExtractor guiGraphics, int x, int y, int width) {
                guiGraphics.text(font, id, x, y + 4, Ui.TEXT, false);
            }

            @Override
            public void onClick() {
                copyId(id);
            }
        };
    }

    private void copyId(String id) {
        try {
            new ClipboardManager().setClipboard(Minecraft.getInstance().getWindow(), id);
            UiSfx.click();
            statusMessage = "Copied: " + id;
            statusUntil = System.currentTimeMillis() + 3000;
        } catch (Throwable t) {
            statusMessage = "Could not access clipboard for: " + id;
        }
    }

    private static int fuzzyScore(String hay, String needle) {
        String h = hay.toLowerCase();
        String n = needle.toLowerCase();
        if (n.isEmpty()) return 0;
        if (h.startsWith(n)) return 1000;
        if (h.contains(n)) return 500 + (300 - Math.min(300, n.length() * 3));
        if (n.length() < 2) return -1;
        int idx = 0;
        int used = 0;
        for (int i = 0; i < n.length() && idx < h.length(); i++) {
            int found = h.indexOf(n.charAt(i), idx);
            if (found < 0) return -1;
            int gap = found - idx;
            if (gap <= 2) used += 2;
            idx = found + 1;
        }
        return used;
    }

    private record EntryRow2(String id, int score) {}

    private static String folderOf(String id) {
        int colon = id.indexOf(':');
        String rest = colon >= 0 ? id.substring(colon + 1) : id;
        int slash = rest.indexOf('/');
        return slash >= 0 ? rest.substring(0, slash) : (rest.isEmpty() ? "(root)" : rest);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (searchField == null) return;
        guiGraphics.text(font, "Search:", rightPanelX + PADDING, contentY + 5, Ui.MUTED, false);
        guiGraphics.fill(rightPanelX + PADDING, contentY + 22,
            rightPanelX + rightPanelWidth - PADDING, contentY + 23, Ui.DIVIDER);

        if (entryList != null) {
            entryList.render(guiGraphics, mouseX, mouseY, delta);
        }

        if (!statusMessage.isEmpty()) {
            boolean recent = System.currentTimeMillis() < statusUntil;
            drawStatus(guiGraphics, statusMessage, recent ? Ui.ACCENT : Ui.MUTED);
        }
    }

    @Override
    protected boolean contentMouseClicked(double mouseX, double mouseY, int button) {
        if (entryList != null && entryList.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    protected boolean contentMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (entryList != null) return entryList.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        return false;
    }

    @Override
    protected boolean contentMouseDragged(double mouseX, double mouseY, double dx, double dy) {
        if (entryList != null && entryList.mouseDragged(mouseX, mouseY)) return true;
        return false;
    }

    @Override
    protected void contentMouseReleased(double mouseX, double mouseY) {
        if (entryList != null) {
            entryList.mouseReleased(mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (searchField != null && searchField.keyPressed(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (searchField != null && searchField.charTyped(event)) return true;
        return super.charTyped(event);
    }
}