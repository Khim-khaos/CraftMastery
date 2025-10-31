// src/main/java/com/khimkhaosow/craftmastery/gui/widgets/TabBarWidget.java
package com.khimkhaosow.craftmastery.gui.widgets;

import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.NodeData;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.TabData;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Виджет верхней панели с вкладками
 */
public class TabBarWidget extends Gui {
    private static final ResourceLocation TAB_TEXTURES = new ResourceLocation("craftmastery", "textures/gui/tabs.png");
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_WIDTH = 120;
    private static final int ARROW_WIDTH = 20;
    private static final int TAB_SPACING = 5;

    private final Minecraft minecraft;
    private final EntityPlayer player;
    private final FontRenderer fontRenderer;
    private final int width;
    private final int offsetY;
    private int tabScrollOffset = 0;
    private String activeTabId;
    private final Map<String, TabData> tabsById = new LinkedHashMap<>();
    private final List<String> tabOrder = new ArrayList<>();

    public interface TabSelectedCallback {
        void onTabSelected(String tabId);
    }

    public com.khimkhaosow.craftmastery.gui.GuiCraftMastery.Page getCurrentPage() {
        String tabId = activeTabId;
        if ("main".equals(tabId)) return com.khimkhaosow.craftmastery.gui.GuiCraftMastery.Page.MAIN;
        if ("settings".equals(tabId)) return com.khimkhaosow.craftmastery.gui.GuiCraftMastery.Page.SETTINGS;
        if ("search".equals(tabId)) return com.khimkhaosow.craftmastery.gui.GuiCraftMastery.Page.SEARCH;
        return com.khimkhaosow.craftmastery.gui.GuiCraftMastery.Page.TABS;
    }

    private final TabSelectedCallback callback;

    public TabBarWidget(Minecraft minecraft, EntityPlayer player, int width, int offsetY, TabSelectedCallback callback) {
        this.minecraft = minecraft;
        this.player = player;
        this.fontRenderer = minecraft.fontRenderer;
        this.width = width;
        this.offsetY = offsetY;
        this.callback = callback;
        updateTabList();
    }

    public void updateTabList() {
        tabsById.clear();
        tabOrder.clear();

        for (TabData tab : RecipeTreeConfigManager.getInstance().getTabs()) {
            if (tab == null || tab.id == null || tab.id.trim().isEmpty()) {
                continue;
            }
            String id = tab.id;
            tabsById.put(id, tab);
            tabOrder.add(id);
        }

        if (activeTabId == null && !tabOrder.isEmpty()) {
            activeTabId = tabOrder.get(0);
        } else if (activeTabId != null && !tabsById.containsKey(activeTabId)) {
            activeTabId = tabOrder.isEmpty() ? null : tabOrder.get(0);
        }
    }

    public void draw(int mouseX, int mouseY) {
        int localMouseY = mouseY - offsetY;

        // Фон панели (необязательно, если рисуется в основном GUI)
        // drawRect(0, offsetY, width, offsetY + TAB_HEIGHT, 0xFF2F2F2F);

        // Фон активной вкладки (теперь заполняет всю высоту вкладки)
        int activeTabX = getActiveTabX();
        if (activeTabX >= 0) {
            drawRect(activeTabX, offsetY, activeTabX + TAB_WIDTH, offsetY + TAB_HEIGHT, 0xFF4CAF50);
        }

        // УБРАНА тень под панелью - это убирало "хвост"
        // drawRect(0, offsetY + TAB_HEIGHT - 2, width, offsetY + TAB_HEIGHT, 0xFF1A1A1A);

        // Стрелки навигации и вкладки
        drawScrollArrows(mouseX, localMouseY);
        drawTabs(mouseX, localMouseY);
    }

    private void drawScrollArrows(int mouseX, int mouseY) {
        // Левая стрелка
        boolean canScrollLeft = canScrollLeft();
        boolean hoverLeft = isMouseOverLeftArrow(mouseX, mouseY);
        int arrowColor = canScrollLeft ? (hoverLeft ? 0xFF666666 : 0xFF555555) : 0xFF333333;
        int textColor = canScrollLeft ? 0xFFFFFF : 0x888888;

        int arrowTop = offsetY + TAB_HEIGHT / 2 - 10;
        drawRect(10, arrowTop, 30, arrowTop + 20, arrowColor);
        drawCenteredString(fontRenderer, "\u2039", 20, offsetY + TAB_HEIGHT / 2 - 4, textColor);

        // Правая стрелка
        boolean canScrollRight = canScrollRight();
        boolean hoverRight = isMouseOverRightArrow(mouseX, mouseY);
        arrowColor = canScrollRight ? (hoverRight ? 0xFF666666 : 0xFF555555) : 0xFF333333;
        textColor = canScrollRight ? 0xFFFFFF : 0x888888;

        int rightArrowLeft = width - 30;
        drawRect(rightArrowLeft, arrowTop, rightArrowLeft + 20, arrowTop + 20, arrowColor);
        drawCenteredString(fontRenderer, "\u203A", width - 20, offsetY + TAB_HEIGHT / 2 - 4, textColor);
    }

    private void drawTabs(int mouseX, int mouseY) {
        // Пытаемся загрузить текстуру с обработкой ошибок
        try {
            minecraft.getTextureManager().bindTexture(TAB_TEXTURES);
        } catch (Exception e) {
            com.khimkhaosow.craftmastery.CraftMastery.logger.error("Failed to load tab textures: " + e.getMessage());
        }
        int startX = 40;

        for (int i = tabScrollOffset; i < tabOrder.size() && i < tabScrollOffset + getMaxVisibleTabs(); i++) {
            String tabId = tabOrder.get(i);
            TabData tabData = tabsById.get(tabId);
            if (tabData == null) {
                continue;
            }
            int tabX = startX + (i - tabScrollOffset) * (TAB_WIDTH + TAB_SPACING);
            boolean isActive = tabId.equals(activeTabId);
            // Исправлено: используем mouseY напрямую, так как isMouseOverTab теперь корректно проверяет координаты
            boolean isHovered = isMouseOverTab(mouseX, mouseY, tabX);

            // Фон вкладки (исправлено: заполняет всю высоту, hover непрозрачный)
            // int tabColor = isActive ? 0xFF4CAF50 : (isHovered ? 0x88555555 : 0x66555555); // Старый hover цвет (полупрозрачный)
            int tabColor = isActive ? 0xFF4CAF50 : (isHovered ? 0xFF555555 : 0xFF444444); // Новый hover цвет (непрозрачный)
            // int tabColor = isActive ? 0xFF4CAF50 : (isHovered ? 0xFFAAAAAA : 0xFF888888); // Альтернативный hover цвет (светлее)
            drawRect(tabX, offsetY, tabX + TAB_WIDTH, offsetY + TAB_HEIGHT, tabColor);

            // Значок и текст
            String icon = getTabIcon(tabData);
            int iconX = tabX + 10;
            int iconY = offsetY + TAB_HEIGHT / 2 - 4;
            int textColor = isActive ? 0xFF000000 : 0xFFFFFFFF;

            drawString(fontRenderer, icon, iconX, iconY, textColor);
            String displayName = truncateTabName(tabData.title != null ? tabData.title : tabId);
            drawString(fontRenderer, displayName, iconX + 20, iconY, textColor);

            // Счетчик
            String counter = getTabCounter(tabId);
            if (counter != null) {
                drawString(fontRenderer, counter, tabX + TAB_WIDTH - 25, iconY, isActive ? 0xFF000000 : 0x88FFFFFF);
            }
        }
    }

    public boolean handleMouseClick(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return false;
        }

        int localMouseY = mouseY - offsetY;
        if (localMouseY < 0 || localMouseY > TAB_HEIGHT) {
            return false;
        }

        if (isMouseOverLeftArrow(mouseX, localMouseY) && canScrollLeft()) {
            tabScrollOffset--;
            return true;
        }

        if (isMouseOverRightArrow(mouseX, localMouseY) && canScrollRight()) {
            tabScrollOffset++;
            return true;
        }

        int startX = 40;
        for (int i = tabScrollOffset; i < tabOrder.size() && i < tabScrollOffset + getMaxVisibleTabs(); i++) {
            int tabX = startX + (i - tabScrollOffset) * (TAB_WIDTH + TAB_SPACING);
            // Исправлено: передаём глобальную mouseY в isMouseOverTab
            if (isMouseOverTab(mouseX, mouseY, tabX)) {
                String tabId = tabOrder.get(i);
                if (!tabId.equals(activeTabId)) {
                    setActiveTab(tabId);
                    if (callback != null) {
                        callback.onTabSelected(tabId);
                    }
                }
                return true;
            }
        }

        return false;
    }

    private boolean isMouseOverLeftArrow(int mouseX, int mouseY) {
        int localMouseY = mouseY - offsetY;
        return mouseX >= 10 && mouseX <= 30 && localMouseY >= TAB_HEIGHT / 2 - 10 && localMouseY <= TAB_HEIGHT / 2 + 10;
    }

    private boolean isMouseOverRightArrow(int mouseX, int mouseY) {
        int localMouseY = mouseY - offsetY;
        return mouseX >= width - 30 && mouseX <= width - 10 && localMouseY >= TAB_HEIGHT / 2 - 10 && localMouseY <= TAB_HEIGHT / 2 + 10;
    }

    // Исправлено: проверка mouseY относительно offsetY
    private boolean isMouseOverTab(int mouseX, int mouseY, int tabX) {
        return mouseX >= tabX && mouseX <= tabX + TAB_WIDTH
                && mouseY >= offsetY + 5 && mouseY <= offsetY + TAB_HEIGHT - 5; // Было: mouseY >= 5 && mouseY <= TAB_HEIGHT - 5
    }

    private int getActiveTabX() {
        if (activeTabId == null) {
            return -1;
        }
        int startX = 40;
        for (int i = tabScrollOffset; i < tabOrder.size() && i < tabScrollOffset + getMaxVisibleTabs(); i++) {
            if (tabOrder.get(i).equals(activeTabId)) {
                return startX + (i - tabScrollOffset) * (TAB_WIDTH + TAB_SPACING);
            }
        }
        return -1;
    }

    private String getTabIcon(TabData tab) {
        if (tab != null && tab.icon != null && !tab.icon.trim().isEmpty()) {
            return tab.icon;
        }
        return "\u270E";
    }

    private String getTabCounter(String tabId) {
        if (tabId == null || tabId.equalsIgnoreCase("search")) {
            return null;
        }

        int total = 0;
        int studied = 0;
        RecipeManager recipeManager = RecipeManager.getInstance();
        for (NodeData node : RecipeTreeConfigManager.getInstance().getNodes()) {
            if (node == null || node.tab == null || !tabId.equals(node.tab)) {
                continue;
            }
            RecipeEntry entry = resolveRecipe(node, recipeManager);
            if (entry == null) {
                continue;
            }
            total++;
            if (player != null && entry.isStudiedByPlayer(player.getUniqueID())) {
                studied++;
            }
        }

        return total == 0 ? null : studied + "/" + total;
    }

    private RecipeEntry resolveRecipe(NodeData node, RecipeManager recipeManager) {
        if (node == null || node.recipeId == null || node.recipeId.trim().isEmpty()) {
            return null;
        }

        RecipeEntry entry = recipeManager.getRecipe(node.recipeId);
        if (entry != null) {
            return entry;
        }

        try {
            ResourceLocation location = new ResourceLocation(node.recipeId);
            return recipeManager.getRecipe(location.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String truncateTabName(String name) {
        return name.length() > 12 ? name.substring(0, 10) + "..." : name;
    }

    private boolean canScrollLeft() {
        return tabScrollOffset > 0;
    }

    private boolean canScrollRight() {
        return tabScrollOffset < Math.max(0, tabOrder.size() - getMaxVisibleTabs());
    }

    private int getMaxVisibleTabs() {
        return (width - 80) / (TAB_WIDTH + TAB_SPACING);
    }

    public void setActiveTab(String tabId) {
        if (tabId != null && tabsById.containsKey(tabId)) {
            activeTabId = tabId;
        }
    }

    public String getActiveTab() {
        return activeTabId;
    }

    public static int getPreferredHeight() {
        return TAB_HEIGHT;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getHeight() {
        return TAB_HEIGHT;
    }

    public boolean containsY(int screenY) {
        int local = screenY - offsetY;
        return local >= 0 && local <= TAB_HEIGHT;
    }
}