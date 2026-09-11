package com.komixkat.customdrops.client.gui.screen;

import com.komixkat.customdrops.client.gui.Ui;
import com.komixkat.customdrops.client.gui.UiSfx;
import com.komixkat.customdrops.client.gui.widget.EntryListWidget;
import com.mojang.blaze3d.platform.ClipboardManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TagsBrowserScreen extends SplitPaneScreen {

    private EntryListWidget entryList;
    private EditBox searchField;
    private Button clearSearchButton;
    private final Map<String, String> tagKinds = new LinkedHashMap<>();
    private final Map<String, Integer> tagSizes = new HashMap<>();
    private String activeKind = null;
    private String searchQuery = "";
    private String statusMessage = "";
    private long statusUntil = 0;
    private String selectedTag = null;
    private final Map<String, List<String>> membersCache = new HashMap<>();
    private final String preselectTag;

    public TagsBrowserScreen(net.minecraft.client.gui.screens.Screen parent) {
        this(parent, null);
    }

    public TagsBrowserScreen(net.minecraft.client.gui.screens.Screen parent, String preselectTag) {
        super(parent, Component.translatable("customdrops.menu.tags"));
        this.preselectTag = preselectTag;
    }

    private void resolveSizes() {
        try {
            var connection = Minecraft.getInstance().getConnection();
            if (connection == null) return;
            var access = connection.registryAccess();
            collect(access, net.minecraft.core.registries.Registries.ENTITY_TYPE);
            collect(access, net.minecraft.core.registries.Registries.BLOCK);
            collect(access, net.minecraft.core.registries.Registries.ITEM);
        } catch (Throwable ignored) {
        }
    }

    private <T> void collect(net.minecraft.core.HolderLookup.Provider access,
                             net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registryKey) {
        access.lookupOrThrow(registryKey).listTags()
            .forEach(named -> tagSizes.put(named.key().location().toString(), named.size()));
    }

    @Override
    protected String backButtonLabel() {
        return "Back";
    }

    @Override
    protected void buildNavigation() {
        tagKinds.clear();
        registryIndex.scanner().tagKinds()
            .forEach((id, kind) -> tagKinds.put(id.startsWith("#") ? id.substring(1) : id, kind));
        resolveSizes();

        if (preselectTag != null && selectedTag == null && tagKinds.containsKey(preselectTag)) {
            selectedTag = preselectTag;
        }

        navWidget.addCategory("Tags");
        navWidget.addEntry("Tags", "All (" + tagKinds.size() + ")", () -> setKind(null));
        int entities = 0, blocks = 0, items = 0;
        for (String kind : tagKinds.values()) {
            if ("entity".equals(kind)) entities++;
            else if ("block".equals(kind)) blocks++;
            else if ("item".equals(kind)) items++;
        }
        navWidget.addEntry("Tags", "Entity Tags (" + entities + ")", () -> setKind("entity"));
        navWidget.addEntry("Tags", "Block Tags (" + blocks + ")", () -> setKind("block"));
        navWidget.addEntry("Tags", "Item Tags (" + items + ")", () -> setKind("item"));
    }

    @Override
    protected void initContent() {
        int fieldX = rightPanelX + PADDING + 52;
        int fieldW = Math.max(0, rightPanelWidth - PADDING * 2 - 52 - 26);

        searchField = new EditBox(font, fieldX, contentY, fieldW, 18, Component.literal("Search tag..."));
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

    private void setKind(String kind) {
        activeKind = kind;
        selectedTag = null;
        rebuildList();
    }

    private void onSearchChanged(String query) {
        searchQuery = query == null ? "" : query.trim();
        if (searchQuery.startsWith("#")) {
            searchQuery = searchQuery.substring(1).trim();
        }
        if (clearSearchButton != null) {
            clearSearchButton.active = !searchQuery.isEmpty();
        }
        rebuildList();
    }

    private void rebuildList() {
        if (entryList == null) return;
        String needle = searchQuery.toLowerCase();
        List<EntryListWidget.EntryRow> rows = new ArrayList<>();
        int shown = 0;

        if (selectedTag != null) {
            String kind = tagKinds.getOrDefault(selectedTag, "?");
            List<String> members = new ArrayList<>(resolveMembers(kind, selectedTag));
            if (!needle.isEmpty()) {
                members.removeIf(m -> !m.toLowerCase().contains(needle));
            }
            rows.add(backRow());
            rows.add(EntryListWidget.section("#" + selectedTag + "  \u00B7  " + kind + " tag"));
            if (members.isEmpty()) {
                rows.add(EntryListWidget.section("(no members resolved \u2014 connect to a world for datapack tags)"));
            } else {
                for (String member : members) {
                    rows.add(memberRow(member));
                    shown++;
                }
            }
            int total = tagSizes.getOrDefault(selectedTag, -1);
            String totalText = total >= 0 ? String.valueOf(total) : "?";
            statusMessage = shown + " of " + totalText
                + " member" + (shown == 1 ? "" : "s")
                + "  \u00B7  click an entry to copy it  \u00B7  right-click a tag to copy its id";
        } else {
            List<String> matches = new ArrayList<>();
            for (Map.Entry<String, String> e : tagKinds.entrySet()) {
                if (activeKind != null && !activeKind.equals(e.getValue())) continue;
                if (!needle.isEmpty() && !e.getKey().toLowerCase().contains(needle)) continue;
                matches.add(e.getKey());
            }
            matches.sort(String::compareTo);

            int listIndex = 0;
            if (activeKind == null && needle.isEmpty()) {
                rows.add(EntryListWidget.section("entity tags"));
                for (String id : matches) {
                    if ("entity".equals(tagKinds.get(id))) {
                        rows.add(rowFor(id, listIndex++));
                        shown++;
                    }
                }
                rows.add(EntryListWidget.section("block tags"));
                for (String id : matches) {
                    if ("block".equals(tagKinds.get(id))) {
                        rows.add(rowFor(id, listIndex++));
                        shown++;
                    }
                }
                rows.add(EntryListWidget.section("item tags"));
                for (String id : matches) {
                    if ("item".equals(tagKinds.get(id))) {
                        rows.add(rowFor(id, listIndex++));
                        shown++;
                    }
                }
            } else {
                for (String id : matches) {
                    rows.add(rowFor(id, listIndex++));
                    shown++;
                }
            }
            statusMessage = shown + " of " + tagKinds.size()
                + " tag" + (tagKinds.size() == 1 ? "" : "s")
                + "  \u00B7  click a tag to see its entries  \u00B7  right-click to copy";
        }
        entryList.setRows(rows);
    }

    private EntryListWidget.EntryRow rowFor(String id, int index) {
        String kind = tagKinds.getOrDefault(id, "?");
        int size = tagSizes.getOrDefault(id, -1);
        boolean selected = id.equals(selectedTag);
        return new EntryListWidget.EntryRow() {
            @Override
            public void render(GuiGraphicsExtractor guiGraphics, int x, int y, int width) {
                String suffix = size >= 0 ? kind + " \u00B7 " + size : (size == -2 ? kind + " \u00B7 bundled" : kind);
                int suffixW = font.width(suffix);
                int idW = Math.max(20, width - suffixW - 14);
                guiGraphics.text(font, font.plainSubstrByWidth("#" + id, idW), x + 2, y + 4,
                    selected ? Ui.ACCENT : Ui.TEXT, false);
                guiGraphics.text(font, suffix, x + width - suffixW - 6, y + 4, size >= 0 ? Ui.DIM : Ui.MUTED, false);
            }

            @Override
            public void onClick() {
                selectedTag = id;
                rebuildList();
            }

            @Override
            public void onRightClick() {
                copyText("#" + id, "paste it into a Mob/Block target field");
            }
        };
    }

    private EntryListWidget.EntryRow backRow() {
        return new EntryListWidget.EntryRow() {
            @Override
            public void render(GuiGraphicsExtractor guiGraphics, int x, int y, int width) {
                guiGraphics.text(font, "\u2039  All tags", x + 2, y + 4, Ui.ACCENT, false);
            }

            @Override
            public void onClick() {
                selectedTag = null;
                rebuildList();
            }
        };
    }

    private EntryListWidget.EntryRow memberRow(String memberId) {
        return new EntryListWidget.EntryRow() {
            @Override
            public void render(GuiGraphicsExtractor guiGraphics, int x, int y, int width) {
                guiGraphics.text(font, font.plainSubstrByWidth(memberId, Math.max(20, width - 2)),
                    x + 2, y + 4, Ui.TEXT, false);
            }

            @Override
            public void onClick() {
                copyText(memberId, "paste it into a Mob/Block/Item field");
            }
        };
    }

    private List<String> resolveMembers(String kind, String id) {
        List<String> cached = membersCache.get(id);
        if (cached != null) return cached;
        List<String> members = new ArrayList<>();
        try {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                var access = connection.registryAccess();
                switch (kind) {
                    case "entity" -> members.addAll(resolveLive(access, net.minecraft.core.registries.Registries.ENTITY_TYPE, id));
                    case "block" -> members.addAll(resolveLive(access, net.minecraft.core.registries.Registries.BLOCK, id));
                    case "item" -> members.addAll(resolveLive(access, net.minecraft.core.registries.Registries.ITEM, id));
                    default -> {}
                }
            }
        } catch (Throwable ignored) {
        }
        if (members.isEmpty()) {
            switch (kind) {
                case "entity" -> members.addAll(resolveStatic(BuiltInRegistries.ENTITY_TYPE, net.minecraft.core.registries.Registries.ENTITY_TYPE, id));
                case "block" -> members.addAll(resolveStatic(BuiltInRegistries.BLOCK, net.minecraft.core.registries.Registries.BLOCK, id));
                case "item" -> members.addAll(resolveStatic(BuiltInRegistries.ITEM, net.minecraft.core.registries.Registries.ITEM, id));
                default -> {}
            }
        }
        membersCache.put(id, members);
        return members;
    }

    private <T> List<String> resolveLive(HolderLookup.Provider access,
                                         ResourceKey<? extends Registry<T>> registryKey, String id) {
        List<String> out = new ArrayList<>();
        Identifier loc = Identifier.tryParse(id);
        if (loc == null) return out;
        HolderLookup.RegistryLookup<T> lookup = access.lookupOrThrow(registryKey);
        lookup.get(TagKey.create(registryKey, loc))
            .ifPresent(named -> collectNamed(named, out));
        return out;
    }

    private <T> List<String> resolveStatic(Registry<T> registry,
                                           ResourceKey<? extends Registry<T>> registryKey, String id) {
        List<String> out = new ArrayList<>();
        Identifier loc = Identifier.tryParse(id);
        if (loc == null) return out;
        registry.getTags()
            .filter(named -> named.key().location().equals(loc))
            .findFirst()
            .ifPresent(named -> collectNamed(named, out));
        return out;
    }

    private <T> void collectNamed(net.minecraft.core.HolderSet<T> set, List<String> out) {
        for (Holder<T> h : set) {
            String name = h.getRegisteredName();
            if (name != null && !name.isBlank()) {
                out.add(name);
            }
        }
    }

    private void copyText(String text, String hint) {
        try {
            new ClipboardManager().setClipboard(Minecraft.getInstance().getWindow(), text);
            UiSfx.click();
            statusMessage = "Copied: " + text + "  \u00B7  " + hint;
            statusUntil = System.currentTimeMillis() + 3000;
        } catch (Throwable t) {
            statusMessage = "Could not access clipboard for: " + text;
        }
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