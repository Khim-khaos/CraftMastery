package com.khimkhaosow.craftmastery.gui.widgets;

import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.NodeData;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.TabData;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import com.khimkhaosow.craftmastery.tabs.TabAvailabilityHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

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
    private static final int TAB_TEXTURE_SEGMENT_WIDTH = 64;
    private static final int TAB_TEXTURE_HEIGHT = 32;
    private static final int TAB_TEXTURE_TOTAL_WIDTH = 256;
    private static final ResourceLocation TAB_TEXTURE = new ResourceLocation("craftmastery", "textures/gui/tabs.png");
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
    private final Map<String, Boolean> tabAvailability = new LinkedHashMap<>();
    private final List<String> tabOrder = new ArrayList<>();

    public interface TabSelectedCallback {
        void onTabSelected(String tabId);
    }

    private void drawLockedTooltip(TabData tabData, int mouseX, int mouseY) {
        List<String> lines = TabAvailabilityHelper.describeLockReasons(player, tabData);
        if (lines.isEmpty()) {
            return;
        }
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, fontRenderer.getStringWidth(line));
        }
        int padding = 4;
        int height = lines.size() * (fontRenderer.FONT_HEIGHT + 2) + padding * 2;
        int width = maxWidth + padding * 2;
        int x = mouseX + 12;
        int y = mouseY + 12;

        drawRect(x, y, x + width, y + height, 0xCC1E1E1E);
        drawRect(x, y, x + width, y + 1, 0x55FF5555);
        drawRect(x, y + height - 1, x + width, y + height, 0x55FF5555);

        int textY = y + padding;
        for (String line : lines) {
            fontRenderer.drawString(line, x + padding, textY, 0xFFFFAAAA);
            textY += fontRenderer.FONT_HEIGHT + 2;
        }
    }

    private void notifyLockedTab(TabData tabData) {
        if (player == null) {
            return;
        }
        List<String> reasons = TabAvailabilityHelper.describeLockReasons(player, tabData);
        if (reasons.isEmpty()) {
            return;
        }
        player.sendMessage(new TextComponentString(TextFormatting.RED + "Вкладка недоступна:"));
        for (String reason : reasons) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + " - " + reason));
        }
    }

    public com.khimkhaosow.craftmastery.gui.GuiCraftMastery.Page getCurrentPage() {
        String tabId = activeTabId;
        if ("main".equals(tabId)) return com.khimkhaosow.craftmastery.gui.GuiCraftMastery.Page.MAIN;
        if ("settings".equals(tabId)) return com.khimkhaosow.craftmastery.gui.GuiCraftMastery.Page.SETTINGS;
        if ("search".equals(tabId)) return com.khimkhaosow.craftmastery.gui.GuiCraftMastery.Page.SEARCH;
        if ("tabs".equals(tabId)) return com.khimkhaosow.craftmastery.gui.GuiCraftMastery.Page.TABS;
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

        EntityPlayer currentPlayer = player;
        tabAvailability.clear();

        for (TabData tab : RecipeTreeConfigManager.getInstance().getTabs()) {
            if (tab == null || tab.id == null || tab.id.trim().isEmpty()) {
                continue;
            }
            String id = tab.id;
            tabsById.put(id, tab);
            tabOrder.add(id);

            boolean available = true;
            if (currentPlayer != null) {
                available = TabAvailabilityHelper.isTabUnlocked(currentPlayer, tab);
            }
            tabAvailability.put(id, available);
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
        // Основание панелі вкладок
        drawRect(originX, offsetY + TAB_HEIGHT - 2, originX + barWidth, offsetY + TAB_HEIGHT, 0xAA1F1F1F);

        int activeTabX = getActiveTabX();
        if (activeTabX >= 0) {
            int right = Math.min(activeTabX + TAB_WIDTH, originX + barWidth - ARROW_WIDTH - TAB_SPACING);
            drawRect(activeTabX, offsetY, right, offsetY + TAB_HEIGHT - 2, 0xFF4CAF50);
        }

        drawScrollArrows(mouseX, mouseY);
        drawTabs(mouseX, mouseY);
    }

    private void drawScrollArrows(int mouseX, int mouseY) {
        // Левая стрелка
        boolean canScrollLeft = canScrollLeft();
        boolean hoverLeft = isMouseOverLeftArrow(mouseX, mouseY);
        int textColor = canScrollLeft ? 0xFFFFFF : 0x88CCCCCC;
        int arrowLeft = originX;
        int arrowTop = offsetY + TAB_HEIGHT / 2 - 10;
        GlStateManager.color(1F, 1F, 1F, 1F);
        minecraft.getTextureManager().bindTexture(TAB_TEXTURE);
        drawScaledCustomSizeModalRect(arrowLeft, arrowTop, getArrowStateU(canScrollLeft, hoverLeft), 0,
                TAB_TEXTURE_SEGMENT_WIDTH, TAB_TEXTURE_HEIGHT, ARROW_WIDTH, 20, TAB_TEXTURE_TOTAL_WIDTH, TAB_TEXTURE_HEIGHT);
        if (!canScrollLeft) {
            drawRect(arrowLeft, arrowTop, arrowLeft + ARROW_WIDTH, arrowTop + 20, 0x44000000);
        }
        drawCenteredString(fontRenderer, "\u2039", arrowLeft + ARROW_WIDTH / 2, offsetY + TAB_HEIGHT / 2 - 4, textColor);

        // Правая стрелка
        boolean canScrollRight = canScrollRight();
        boolean hoverRight = isMouseOverRightArrow(mouseX, mouseY);
        textColor = canScrollRight ? 0xFFFFFF : 0x88CCCCCC;

        int rightArrowLeft = originX + barWidth - ARROW_WIDTH;
        minecraft.getTextureManager().bindTexture(TAB_TEXTURE);
        drawScaledCustomSizeModalRect(rightArrowLeft, arrowTop, getArrowStateU(canScrollRight, hoverRight), 0,
                TAB_TEXTURE_SEGMENT_WIDTH, TAB_TEXTURE_HEIGHT, ARROW_WIDTH, 20, TAB_TEXTURE_TOTAL_WIDTH, TAB_TEXTURE_HEIGHT);
        if (!canScrollRight) {
            drawRect(rightArrowLeft, arrowTop, rightArrowLeft + ARROW_WIDTH, arrowTop + 20, 0x44000000);
        }
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
            boolean isUnlocked = tabAvailability.getOrDefault(tabId, Boolean.TRUE);

            GlStateManager.color(1F, 1F, 1F, 1F);
            minecraft.getTextureManager().bindTexture(TAB_TEXTURE);
            float stateU = getTabStateU(isActive || !isUnlocked, isHovered && isUnlocked);
            drawScaledCustomSizeModalRect(tabX, offsetY, stateU, 0, TAB_TEXTURE_SEGMENT_WIDTH, TAB_TEXTURE_HEIGHT,
                    TAB_WIDTH, TAB_HEIGHT, TAB_TEXTURE_TOTAL_WIDTH, TAB_TEXTURE_HEIGHT);

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

            int textColor = isUnlocked ? (isActive ? 0xFF101010 : 0xFFEFEFEF) : 0xFF777777;
            int textX = iconX + ICON_SIZE + ICON_TEXT_PADDING;
            int textMaxWidth = tabX + TAB_WIDTH - textX - 8;
            String displayName = tabData.title != null ? tabData.title : tabId;
            String trimmed = fontRenderer.trimStringToWidth(displayName, textMaxWidth);
            fontRenderer.drawString(trimmed, textX, offsetY + (TAB_HEIGHT - fontRenderer.FONT_HEIGHT) / 2, textColor);

            // Счетчик
            String counter = getTabCounter(tabId);
            if (counter != null) {
                int counterColor = isUnlocked ? (isActive ? 0xFF000000 : 0x88FFFFFF) : 0x55888888;
                fontRenderer.drawString(counter, tabX + TAB_WIDTH - fontRenderer.getStringWidth(counter) - 6,
                        offsetY + (TAB_HEIGHT - fontRenderer.FONT_HEIGHT) / 2, counterColor);
            }

            if (!isUnlocked && isHovered) {
                drawLockedTooltip(tabData, mouseX, mouseY);
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

        if (isMouseOverLeftArrow(mouseX, mouseY) && canScrollLeft()) {
            tabScrollOffset--;
            return true;
        }

        if (isMouseOverRightArrow(mouseX, mouseY) && canScrollRight()) {
            tabScrollOffset++;
            return true;
        }

        int startX = originX + ARROW_WIDTH + TAB_SPACING;
        for (int i = tabScrollOffset; i < tabOrder.size() && i < tabScrollOffset + getMaxVisibleTabs(); i++) {
            int tabX = startX + (i - tabScrollOffset) * (TAB_WIDTH + TAB_SPACING);
            if (isMouseOverTab(mouseX, mouseY, tabX)) {
                String tabId = tabOrder.get(i);
                TabData tabData = tabsById.get(tabId);
                boolean unlocked = tabAvailability.getOrDefault(tabId, Boolean.TRUE);
                if (!unlocked) {
                    notifyLockedTab(tabData);
                    if (callback != null) {
                        callback.onTabSelected(null);
                    }
                    return true;
                }
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
                && mouseY >= offsetY && mouseY <= offsetY + TAB_HEIGHT;
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

    private boolean canScrollLeft() {
        return tabScrollOffset > 0;
    }

    private boolean canScrollRight() {
        return tabScrollOffset < Math.max(0, tabOrder.size() - getMaxVisibleTabs());
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

    private float getTabStateU(boolean isActive, boolean isHovered) {
        if (isActive) {
            return TAB_TEXTURE_SEGMENT_WIDTH * 2F;
        }
        if (isHovered) {
            return TAB_TEXTURE_SEGMENT_WIDTH;
        }
        return 0F;
    }

    private float getArrowStateU(boolean enabled, boolean hovered) {
        if (!enabled) {
            return TAB_TEXTURE_SEGMENT_WIDTH * 3F;
        }
        if (hovered) {
            return TAB_TEXTURE_SEGMENT_WIDTH;
        }
        return 0F;
    }
}