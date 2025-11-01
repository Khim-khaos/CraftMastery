package com.khimkhaosow.craftmastery.gui.widgets;

import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.NodeData;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.TabData;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
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
    private static final int TAB_HEIGHT = 28;
    private static final int TAB_WIDTH = 120;
    private static final int ARROW_WIDTH = 20;
    private static final int TAB_SPACING = 5;
    private static final int ICON_SIZE = 16;
    private static final int ICON_TEXT_PADDING = 6;
    private static final int MIN_WIDTH = TAB_WIDTH + (ARROW_WIDTH + TAB_SPACING) * 2;
    private static final ResourceLocation DEFAULT_TAB_ICON = new ResourceLocation("craftmastery", "textures/gui/default_tab_icon.png");

    private final Minecraft minecraft;
    private final EntityPlayer player;
    private final FontRenderer fontRenderer;
    private int originX;
    private int barWidth;
    private int offsetY;
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

    public TabBarWidget(Minecraft minecraft, EntityPlayer player, int originX, int width, int offsetY, TabSelectedCallback callback) {
        this.minecraft = minecraft;
        this.player = player;
        this.fontRenderer = minecraft.fontRenderer;
        this.callback = callback;
        setLayout(originX, width, offsetY);
        updateTabList();
    }

    public void setLayout(int originX, int width, int offsetY) {
        this.originX = Math.max(0, originX);
        this.barWidth = Math.max(width, MIN_WIDTH);
        this.offsetY = offsetY;
        clampTabScrollOffset();
    }

    private void clampTabScrollOffset() {
        int maxVisible = Math.max(1, getMaxVisibleTabs());
        int maxOffset = Math.max(0, tabOrder.size() - maxVisible);
        if (tabScrollOffset > maxOffset) {
            tabScrollOffset = maxOffset;
        }
        if (tabScrollOffset < 0) {
            tabScrollOffset = 0;
        }
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
        clampTabScrollOffset();
    }

    public void draw(int mouseX, int mouseY) {
        if (barWidth <= 0) {
            return;
        }
        int localMouseY = mouseY - offsetY;

        // Основание панелі вкладок
        drawRect(originX, offsetY + TAB_HEIGHT - 2, originX + barWidth, offsetY + TAB_HEIGHT, 0xAA1F1F1F);

        int activeTabX = getActiveTabX();
        if (activeTabX >= 0) {
            int right = Math.min(activeTabX + TAB_WIDTH, originX + barWidth - ARROW_WIDTH - TAB_SPACING);
            drawRect(activeTabX, offsetY, right, offsetY + TAB_HEIGHT - 2, 0xFF4CAF50);
        }

        drawScrollArrows(mouseX, localMouseY);
        drawTabs(mouseX, localMouseY);
    }

    private void drawScrollArrows(int mouseX, int mouseY) {
        // Левая стрелка
        boolean canScrollLeft = canScrollLeft();
        boolean hoverLeft = isMouseOverLeftArrow(mouseX, mouseY);
        int arrowColor = canScrollLeft ? (hoverLeft ? 0xFF666666 : 0xFF555555) : 0xFF333333;
        int textColor = canScrollLeft ? 0xFFFFFF : 0x888888;

        int arrowLeft = originX;
        int arrowTop = offsetY + TAB_HEIGHT / 2 - 10;
        drawRect(arrowLeft, arrowTop, arrowLeft + ARROW_WIDTH, arrowTop + 20, arrowColor);
        drawCenteredString(fontRenderer, "\u2039", arrowLeft + ARROW_WIDTH / 2, offsetY + TAB_HEIGHT / 2 - 4, textColor);

        // Правая стрелка
        boolean canScrollRight = canScrollRight();
        boolean hoverRight = isMouseOverRightArrow(mouseX, mouseY);
        arrowColor = canScrollRight ? (hoverRight ? 0xFF666666 : 0xFF555555) : 0xFF333333;
        textColor = canScrollRight ? 0xFFFFFF : 0x888888;

        int rightArrowLeft = originX + barWidth - ARROW_WIDTH;
        drawRect(rightArrowLeft, arrowTop, rightArrowLeft + ARROW_WIDTH, arrowTop + 20, arrowColor);
        drawCenteredString(fontRenderer, "\u203A", rightArrowLeft + ARROW_WIDTH / 2, offsetY + TAB_HEIGHT / 2 - 4, textColor);
    }

    private void drawTabs(int mouseX, int mouseY) {
        int startX = originX + ARROW_WIDTH + TAB_SPACING;
        int maxVisible = getMaxVisibleTabs();

        for (int i = tabScrollOffset; i < tabOrder.size() && i < tabScrollOffset + maxVisible; i++) {
            String tabId = tabOrder.get(i);
            TabData tabData = tabsById.get(tabId);
            if (tabData == null) {
                continue;
            }
            int tabX = startX + (i - tabScrollOffset) * (TAB_WIDTH + TAB_SPACING);
            int tabRightLimit = originX + barWidth - ARROW_WIDTH;
            if (tabX + TAB_WIDTH > tabRightLimit) {
                break;
            }
            boolean isActive = tabId.equals(activeTabId);
            boolean isHovered = isMouseOverTab(mouseX, mouseY, tabX);

            int tabColor = isActive ? 0xFF4CAF50 : (isHovered ? 0xFF5A5A5A : 0xFF3C3C3C);
            drawRect(tabX, offsetY, tabX + TAB_WIDTH, offsetY + TAB_HEIGHT - 2, tabColor);

            ResourceLocation iconLocation = resolveIcon(tabData);
            int iconX = tabX + 6;
            int iconY = offsetY + (TAB_HEIGHT - ICON_SIZE) / 2 - 1;
            GlStateManager.color(1F, 1F, 1F, 1F);
            try {
                minecraft.getTextureManager().bindTexture(iconLocation);
                drawModalRectWithCustomSizedTexture(iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            } catch (Exception e) {
                minecraft.getTextureManager().bindTexture(DEFAULT_TAB_ICON);
                drawModalRectWithCustomSizedTexture(iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }

            int textColor = isActive ? 0xFF101010 : 0xFFEFEFEF;
            int textX = iconX + ICON_SIZE + ICON_TEXT_PADDING;
            int textMaxWidth = tabX + TAB_WIDTH - textX - 8;
            String displayName = tabData.title != null ? tabData.title : tabId;
            String trimmed = fontRenderer.trimStringToWidth(displayName, textMaxWidth);
            fontRenderer.drawString(trimmed, textX, offsetY + (TAB_HEIGHT - fontRenderer.FONT_HEIGHT) / 2, textColor);

            // Счетчик
            String counter = getTabCounter(tabId);
            if (counter != null) {
                fontRenderer.drawString(counter, tabX + TAB_WIDTH - fontRenderer.getStringWidth(counter) - 6,
                        offsetY + (TAB_HEIGHT - fontRenderer.FONT_HEIGHT) / 2, isActive ? 0xFF000000 : 0x88FFFFFF);
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

        int startX = originX + ARROW_WIDTH + TAB_SPACING;
        for (int i = tabScrollOffset; i < tabOrder.size() && i < tabScrollOffset + getMaxVisibleTabs(); i++) {
            int tabX = startX + (i - tabScrollOffset) * (TAB_WIDTH + TAB_SPACING);
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
        return mouseX >= originX && mouseX <= originX + ARROW_WIDTH
                && localMouseY >= TAB_HEIGHT / 2 - 10 && localMouseY <= TAB_HEIGHT / 2 + 10;
    }

    private boolean isMouseOverRightArrow(int mouseX, int mouseY) {
        int localMouseY = mouseY - offsetY;
        int rightArrowLeft = originX + barWidth - ARROW_WIDTH;
        return mouseX >= rightArrowLeft && mouseX <= rightArrowLeft + ARROW_WIDTH
                && localMouseY >= TAB_HEIGHT / 2 - 10 && localMouseY <= TAB_HEIGHT / 2 + 10;
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
        int startX = originX + ARROW_WIDTH + TAB_SPACING;
        for (int i = tabScrollOffset; i < tabOrder.size() && i < tabScrollOffset + getMaxVisibleTabs(); i++) {
            if (tabOrder.get(i).equals(activeTabId)) {
                return startX + (i - tabScrollOffset) * (TAB_WIDTH + TAB_SPACING);
            }
        }
        return -1;
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

    private int getMaxVisibleTabs() {
        int usableWidth = barWidth - (ARROW_WIDTH * 2) - (TAB_SPACING * 2);
        if (usableWidth <= 0) {
            return 1;
        }
        return Math.max(1, usableWidth / (TAB_WIDTH + TAB_SPACING));
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

    public static int getMinimumWidth() {
        return MIN_WIDTH;
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

    private ResourceLocation resolveIcon(TabData tab) {
        if (tab != null && tab.icon != null && !tab.icon.trim().isEmpty()) {
            try {
                return new ResourceLocation(tab.icon.trim());
            } catch (Exception ignored) {
                return DEFAULT_TAB_ICON;
            }
        }
        return DEFAULT_TAB_ICON;
    }
}