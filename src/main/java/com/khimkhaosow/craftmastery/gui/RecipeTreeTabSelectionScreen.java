package com.khimkhaosow.craftmastery.gui;

import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.TabData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Простое окно выбора вкладки для редактора дерева рецептов.
 */
public class RecipeTreeTabSelectionScreen extends GuiScreen {

    private final RecipeTreeEditorScreen parent;
    private final List<TabData> tabs;
    private TabList tabList;
    private GuiButton selectButton;
    private GuiButton cancelButton;
    private int selectedIndex = -1;

    public RecipeTreeTabSelectionScreen(RecipeTreeEditorScreen parent) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.tabs = new ArrayList<>(RecipeTreeConfigManager.getInstance().getTabs());
    }

    @Override
    public void initGui() {
        buttonList.clear();
        tabList = new TabList();
        tabList.registerScrollButtons(7, 8);

        int buttonY = height - 40;
        selectButton = new GuiButton(0, width / 2 - 110, buttonY, 100, 20, "Выбрать");
        cancelButton = new GuiButton(1, width / 2 + 10, buttonY, 100, 20, "Отмена");
        buttonList.add(selectButton);
        buttonList.add(cancelButton);
        updateButtons();
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
            case 0:
                confirmSelection();
                break;
            case 1:
                returnToParent();
                break;
            default:
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
            returnToParent();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, TextFormatting.GOLD + "Выбор вкладки", width / 2, 15, 0xFFFFFF);
        if (tabList != null) {
            tabList.drawScreen(mouseX, mouseY, partialTicks);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void confirmSelection() {
        if (selectedIndex < 0 || selectedIndex >= tabs.size()) {
            return;
        }
        TabData selected = tabs.get(selectedIndex);
        parent.onTabSelected(selected.id);
        returnToParent();
    }

    private void updateButtons() {
        if (selectButton != null) {
            selectButton.enabled = selectedIndex >= 0 && selectedIndex < tabs.size();
        }
    }

    private void returnToParent() {
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    private class TabList extends GuiSlot {

        TabList() {
            super(RecipeTreeTabSelectionScreen.this.mc, RecipeTreeTabSelectionScreen.this.width,
                    RecipeTreeTabSelectionScreen.this.height, 40,
                    RecipeTreeTabSelectionScreen.this.height - 50, 24);
        }

        @Override
        protected int getSize() {
            return tabs.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
            if (index < 0 || index >= tabs.size()) {
                return;
            }
            selectedIndex = index;
            updateButtons();
            if (doubleClick) {
                confirmSelection();
            }
        }

        @Override
        protected boolean isSelected(int index) {
            return index == selectedIndex;
        }

        @Override
        protected void drawBackground() {
            // already handled by parent drawDefaultBackground
        }

        @Override
        protected void drawSlot(int idx, int right, int top, int heightIn, int mouseXIn, int mouseYIn, float partialTicks) {
            if (idx < 0 || idx >= tabs.size()) {
                return;
            }
            TabData tab = tabs.get(idx);
            String title = tab.title != null && !tab.title.trim().isEmpty() ? tab.title : tab.id;
            RecipeTreeTabSelectionScreen.this.drawString(fontRenderer, title, this.left + 4, top + 6, 0xFFFFFF);
            if (tab.id != null) {
                RecipeTreeTabSelectionScreen.this.drawString(fontRenderer, TextFormatting.GRAY + tab.id,
                        this.left + 4, top + 16, 0xAAAAAA);
            }
        }
    }
}
