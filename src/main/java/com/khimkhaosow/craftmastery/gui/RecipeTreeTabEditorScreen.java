package com.khimkhaosow.craftmastery.gui;

import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.TabData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Editor screen for managing recipe tree tabs (id/title/icon).
 */
public class RecipeTreeTabEditorScreen extends GuiScreen {
    private static final int LIST_WIDTH = 140;
    private static final int FORM_LEFT = LIST_WIDTH + 20;
    private static final int FIELD_WIDTH = 220;
    private static final int FIELD_HEIGHT = 18;
    private static final int FIELD_GAP = 24;

    private static final int BTN_NEW = 310;
    private static final int BTN_SAVE = 311;
    private static final int BTN_DELETE = 312;
    private static final int BTN_CLOSE = 313;

    private final RecipeTreeEditorScreen parent;
    private final RecipeTreeConfigManager config = RecipeTreeConfigManager.getInstance();

    private List<TabData> tabs = new ArrayList<>();
    private TabData editingTab;
    private String selectedTabId;

    private GuiTextField idField;
    private GuiTextField titleField;
    private GuiTextField iconField;
    private GuiTextField requiredTabsField;
    private GuiTextField requiredNodesField;
    private GuiTextField requiredPermissionsField;

    private TabList tabList;

    private String statusMessage;
    private int statusColor = 0xFFFFFFFF;
    private long statusTicks;

    RecipeTreeTabEditorScreen(RecipeTreeEditorScreen parent) {
        this.parent = Objects.requireNonNull(parent, "parent");
    }

    @Override
    public void initGui() {
        buttonList.clear();
        reloadTabs();

        tabList = new TabList();
        tabList.setSlotXBoundsFromLeft(10);

        int y = 40;
        idField = createField(y, "");
        y += FIELD_GAP;
        titleField = createField(y, "");
        y += FIELD_GAP;
        iconField = createField(y, "");
        y += FIELD_GAP;
        requiredTabsField = createField(y, "");
        y += FIELD_GAP;
        requiredNodesField = createField(y, "");
        y += FIELD_GAP;
        requiredPermissionsField = createField(y, "");

        int btnY = height - 40;
        int btnWidth = 80;
        int btnLeft = FORM_LEFT;
        buttonList.add(new GuiButton(BTN_NEW, btnLeft, btnY, btnWidth, 20, "Новая"));
        btnLeft += btnWidth + 10;
        buttonList.add(new GuiButton(BTN_SAVE, btnLeft, btnY, btnWidth, 20, "Сохранить"));
        btnLeft += btnWidth + 10;
        buttonList.add(new GuiButton(BTN_DELETE, btnLeft, btnY, btnWidth, 20, "Удалить"));
        btnLeft += btnWidth + 10;
        buttonList.add(new GuiButton(BTN_CLOSE, btnLeft, btnY, btnWidth, 20, "Назад"));

        if (!tabs.isEmpty()) {
            selectTab(tabs.get(0));
        } else {
            createNewTab();
        }
    }

    private GuiTextField createField(int y, String value) {
        GuiTextField field = new GuiTextField(0, fontRenderer, FORM_LEFT, y, FIELD_WIDTH, FIELD_HEIGHT);
        field.setMaxStringLength(256);
        field.setText(value);
        return field;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        idField.updateCursorCounter();
        titleField.updateCursorCounter();
        iconField.updateCursorCounter();
        requiredTabsField.updateCursorCounter();
        requiredNodesField.updateCursorCounter();
        requiredPermissionsField.updateCursorCounter();
        if (statusMessage != null && mc.world != null && mc.world.getTotalWorldTime() - statusTicks > 200) {
            statusMessage = null;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (idField.textboxKeyTyped(typedChar, keyCode)) return;
        if (titleField.textboxKeyTyped(typedChar, keyCode)) return;
        if (iconField.textboxKeyTyped(typedChar, keyCode)) return;
        if (requiredTabsField.textboxKeyTyped(typedChar, keyCode)) return;
        if (requiredNodesField.textboxKeyTyped(typedChar, keyCode)) return;
        if (requiredPermissionsField.textboxKeyTyped(typedChar, keyCode)) return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        idField.mouseClicked(mouseX, mouseY, mouseButton);
        titleField.mouseClicked(mouseX, mouseY, mouseButton);
        iconField.mouseClicked(mouseX, mouseY, mouseButton);
        requiredTabsField.mouseClicked(mouseX, mouseY, mouseButton);
        requiredNodesField.mouseClicked(mouseX, mouseY, mouseButton);
        requiredPermissionsField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (tabList != null) {
            tabList.handleMouseInput();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_NEW:
                createNewTab();
                break;
            case BTN_SAVE:
                saveCurrentTab();
                break;
            case BTN_DELETE:
                deleteCurrentTab();
                break;
            case BTN_CLOSE:
                closeEditor();
                break;
            default:
                break;
        }
    }

    private void createNewTab() {
        editingTab = new TabData();
        editingTab.id = "";
        editingTab.title = "";
        editingTab.icon = "";
        editingTab.requiredTabs = new ArrayList<>();
        editingTab.requiredNodes = new ArrayList<>();
        editingTab.requiredPermissions = new ArrayList<>();
        selectedTabId = null;
        loadTabToForm(editingTab);
        idField.setFocused(true);
    }

    private void saveCurrentTab() {
        TabData data = collectForm();
        if (data == null) {
            return;
        }
        config.upsertTab(data);
        config.save();
        selectedTabId = data.id;
        reloadTabs();
        selectTabById(selectedTabId);
        parent.onTabsUpdated(selectedTabId, "Вкладка сохранена", false);
    }

    private void deleteCurrentTab() {
        if (selectedTabId == null || selectedTabId.trim().isEmpty()) {
            setStatus("Выберите вкладку", true);
            return;
        }
        if ("default".equalsIgnoreCase(selectedTabId)) {
            setStatus("Нельзя удалить вкладку по умолчанию", true);
            return;
        }
        if (tabs.size() <= 1) {
            setStatus("Должна остаться хотя бы одна вкладка", true);
            return;
        }
        if (config.removeTab(selectedTabId)) {
            config.save();
            reloadTabs();
            if (!tabs.isEmpty()) {
                selectTab(tabs.get(0));
            } else {
                createNewTab();
            }
            parent.onTabsUpdated(null, "Вкладка удалена", false);
        } else {
            setStatus("Вкладка не найдена", true);
        }
    }

    private void closeEditor() {
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    private TabData collectForm() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            setStatus("ID вкладки не может быть пустым", true);
            return null;
        }
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            title = id;
        }
        String icon = iconField.getText().trim();

        TabData tab = new TabData();
        tab.id = id;
        tab.title = title;
        tab.icon = icon.isEmpty() ? null : icon;
        tab.requiredTabs = parseList(requiredTabsField.getText());
        tab.requiredNodes = parseList(requiredNodesField.getText());
        tab.requiredPermissions = parseList(requiredPermissionsField.getText());
        return tab;
    }

    private void reloadTabs() {
        tabs = new ArrayList<>(config.getTabs());
        tabs.sort(Comparator.comparing(tab -> tab.title != null ? tab.title : tab.id, String.CASE_INSENSITIVE_ORDER));
    }

    private void selectTab(TabData tab) {
        editingTab = copyTab(tab);
        selectedTabId = tab.id;
        loadTabToForm(editingTab);
    }

    private void selectTabById(String tabId) {
        if (tabId == null) {
            return;
        }
        for (TabData tab : tabs) {
            if (Objects.equals(tabId, tab.id)) {
                selectTab(tab);
                return;
            }
        }
    }

    private void loadTabToForm(TabData tab) {
        idField.setText(tab.id != null ? tab.id : "");
        titleField.setText(tab.title != null ? tab.title : "");
        iconField.setText(tab.icon != null ? tab.icon : "");
        requiredTabsField.setText(joinList(tab.requiredTabs));
        requiredNodesField.setText(joinList(tab.requiredNodes));
        requiredPermissionsField.setText(joinList(tab.requiredPermissions));
    }

    private TabData copyTab(TabData original) {
        TabData copy = new TabData();
        copy.id = original.id;
        copy.title = original.title;
        copy.icon = original.icon;
        copy.requiredTabs = original.requiredTabs == null ? new ArrayList<>() : new ArrayList<>(original.requiredTabs);
        copy.requiredNodes = original.requiredNodes == null ? new ArrayList<>() : new ArrayList<>(original.requiredNodes);
        copy.requiredPermissions = original.requiredPermissions == null ? new ArrayList<>() : new ArrayList<>(original.requiredPermissions);
        return copy;
    }

    private void setStatus(String message, boolean error) {
        statusMessage = message;
        statusColor = error ? 0xFFFF5555 : 0xFF55FF55;
        statusTicks = mc.world != null ? mc.world.getTotalWorldTime() : 0;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, TextFormatting.GOLD + "Редактор вкладок", width / 2, 10, 0xFFFFFF);
        drawString(fontRenderer, TextFormatting.YELLOW + "Вкладки", 20, 25, 0xFFFFFF);
        if (tabList != null) {
            tabList.drawScreen(mouseX, mouseY, partialTicks);
        }

        drawLabeledField(40, "ID", idField);
        drawLabeledField(40 + FIELD_GAP, "Название", titleField);
        drawLabeledField(40 + FIELD_GAP * 2, "Иконка (resource)", iconField);
        drawLabeledField(40 + FIELD_GAP * 3, "Требуемые вкладки", requiredTabsField);
        drawLabeledField(40 + FIELD_GAP * 4, "Требуемые узлы", requiredNodesField);
        drawLabeledField(40 + FIELD_GAP * 5, "Требуемые права", requiredPermissionsField);

        int hintBaseY = 40 + FIELD_GAP * 6 + 6;
        drawString(fontRenderer, TextFormatting.GRAY + "Пример: modid:textures/gui/icon.png", FORM_LEFT, hintBaseY, 0xFFFFFF);
        drawString(fontRenderer, TextFormatting.GRAY + "Можно оставить пустым или указать Unicode-символ", FORM_LEFT, hintBaseY + 12, 0xFFFFFF);
        drawString(fontRenderer, TextFormatting.GRAY + "Списки требований: ID через запятую", FORM_LEFT, hintBaseY + 30, 0xFFFFFF);

        if (statusMessage != null) {
            drawCenteredString(fontRenderer, statusMessage, width / 2, height - 60, statusColor);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawLabeledField(int y, String label, GuiTextField field) {
        drawString(fontRenderer, label, FORM_LEFT, y, 0xFFFFFF);
        field.y = y + 12;
        field.x = FORM_LEFT;
        field.drawTextBox();
    }

    private class TabList extends GuiSlot {
        TabList() {
            super(RecipeTreeTabEditorScreen.this.mc, LIST_WIDTH, RecipeTreeTabEditorScreen.this.height,
                    40, RecipeTreeTabEditorScreen.this.height - 60, 20);
        }

        @Override
        protected int getSize() {
            return tabs.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
            if (index >= 0 && index < tabs.size()) {
                selectTab(tabs.get(index));
            }
        }

        @Override
        protected boolean isSelected(int index) {
            return index >= 0 && index < tabs.size() && Objects.equals(tabs.get(index).id, selectedTabId);
        }

        @Override
        protected void drawBackground() {
        }

        @Override
        protected void drawSlot(int idx, int right, int top, int heightIn, int mouseXIn, int mouseYIn, float partialTicks) {
            if (idx < 0 || idx >= tabs.size()) {
                return;
            }
            TabData tab = tabs.get(idx);
            String name = tab.title != null && !tab.title.trim().isEmpty() ? tab.title : tab.id;
            fontRenderer.drawString(name != null ? name : "<без id>", this.left + 3, top + 2, 0xFFFFFF);
            if (tab.id != null) {
                fontRenderer.drawString(TextFormatting.GRAY + tab.id, this.left + 3, top + 12, 0xAAAAAA);
            }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.width = LIST_WIDTH;
            this.left = 10;
            this.right = this.left + LIST_WIDTH;
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected int getScrollBarX() {
            return this.right - 6;
        }
    }

    private static List<String> parseList(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(", ", values);
    }
}
