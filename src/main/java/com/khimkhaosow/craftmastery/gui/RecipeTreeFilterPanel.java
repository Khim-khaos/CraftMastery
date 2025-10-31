package com.khimkhaosow.craftmastery.gui;

import java.util.ArrayList;
import java.util.List;

// 1. Импорты для Minecraft/Forge
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.entity.player.EntityPlayer; // Добавлен импорт EntityPlayer

// 2. Импорты для других компонентов мода
import com.khimkhaosow.craftmastery.recipe.RecipeEntry; // Уже был
import com.khimkhaosow.craftmastery.experience.ExperienceManager; // Уже был
import com.khimkhaosow.craftmastery.util.Reference; // Добавлен импорт Reference
import com.khimkhaosow.craftmastery.gui.NodeState; // Добавлен импорт NodeState
import com.khimkhaosow.craftmastery.gui.ConnectionType; // Добавлен импорт ConnectionType

/**
 * Панель фильтров и поиска для дерева рецептов
 */
public class RecipeTreeFilterPanel {
    // Reference.MOD_ID должен быть найден
    private static final ResourceLocation FILTER_TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/gui/filter_panel.png");

    private final GuiScreen parent;
    private final int x, y, width;
    private GuiTextField searchField;
    private List<FilterButton> filterButtons;
    private String searchQuery = "";
    private boolean showLocked = true;
    private boolean showAvailable = true;
    private boolean showStudied = true;
    private boolean showRequirements = true;
    private boolean showUnlocks = true;

    public RecipeTreeFilterPanel(GuiScreen parent, int x, int y, int width) {
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.width = width;

        initComponents();
    }

    private void initComponents() {
        searchField = new GuiTextField(0, parent.mc.fontRenderer, x + 5, y + 5, width - 10, 20);
        searchField.setMaxStringLength(50);
        searchField.setEnableBackgroundDrawing(true);
        searchField.setVisible(true);
        searchField.setTextColor(0xFFFFFF);
        searchField.setDisabledTextColour(0x7F7F7F);

        filterButtons = new ArrayList<>();
        int buttonY = y + 30;

        // Кнопки фильтров
        filterButtons.add(new FilterButton(0, x + 5, buttonY, width - 10, "Заблокированные", true));
        buttonY += 25;
        filterButtons.add(new FilterButton(1, x + 5, buttonY, width - 10, "Доступные", true));
        buttonY += 25;
        filterButtons.add(new FilterButton(2, x + 5, buttonY, width - 10, "Изученные", true));
        buttonY += 25;
        filterButtons.add(new FilterButton(3, x + 5, buttonY, width - 10, "Требования", true));
        buttonY += 25;
        filterButtons.add(new FilterButton(4, x + 5, buttonY, width - 10, "Разблокирует", true));
    }

    public void draw(int mouseX, int mouseY) {
        // Фон панели
        parent.mc.getTextureManager().bindTexture(FILTER_TEXTURE);
        GuiScreen.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, 180, width, 180);

        // Поле поиска
        GlStateManager.pushMatrix();
        searchField.drawTextBox();
        GlStateManager.popMatrix();

        // Заголовок фильтров
        parent.drawString(parent.mc.fontRenderer,
            TextFormatting.YELLOW + "Фильтры:", x + 5, y + 30, 0xFFFFFF);

        // Кнопки фильтров
        for (FilterButton button : filterButtons) {
            button.draw(mouseX, mouseY);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        // Проверяем клик по полю поиска
        searchField.mouseClicked(mouseX, mouseY, mouseButton);

        // Проверяем клики по кнопкам
        for (FilterButton button : filterButtons) {
            if (button.isMouseOver(mouseX, mouseY)) {
                button.toggleState();
                updateFilters();
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            searchQuery = searchField.getText().toLowerCase();
        }
    }

    private void updateFilters() {
        showLocked = filterButtons.get(0).isEnabled();
        showAvailable = filterButtons.get(1).isEnabled();
        showStudied = filterButtons.get(2).isEnabled();
        showRequirements = filterButtons.get(3).isEnabled();
        showUnlocks = filterButtons.get(4).isEnabled();
    }

    public boolean shouldShowRecipe(RecipeEntry recipe) {
        if (!searchQuery.isEmpty()) {
            String recipeName = recipe.getRecipeResult().getDisplayName().toLowerCase();
            if (!recipeName.contains(searchQuery)) return false;
        }

        NodeState state = getRecipeState(recipe); // NodeState должен быть найден
        switch (state) {
            case LOCKED:
                return showLocked;
            case AVAILABLE:
                return showAvailable;
            case STUDIED:
                return showStudied;
            default:
                return true;
        }
    }

    public boolean shouldShowConnection(ConnectionType type) { // ConnectionType должен быть найден
        switch (type) {
            case REQUIREMENT: // REQUIREMENT должен быть найден в enum ConnectionType
                return showRequirements;
            case UNLOCKS:     // UNLOCKS должен быть найден в enum ConnectionType
                return showUnlocks;
            default:
                return true;
        }
    }

    private class FilterButton {
        private final int id;
        private final int x, y, width;
        private final String text;
        private boolean enabled;

        public FilterButton(int id, int x, int y, int width, String text, boolean enabled) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.width = width;
            this.text = text;
            this.enabled = enabled;
        }

        public void draw(int mouseX, int mouseY) {
            boolean hover = isMouseOver(mouseX, mouseY);
            int color = enabled ?
                (hover ? 0xFF00FF00 : 0xFF008000) :
                (hover ? 0xFFFF0000 : 0xFF800000);

            // Фон кнопки
            GuiScreen.drawRect(x, y, x + width, y + 20, color);

            // Текст
            String displayText = (enabled ? "✓ " : "✗ ") + text;
            // Используем FontRenderer напрямую
            parent.mc.fontRenderer.drawStringWithShadow(displayText,
                x + width/2 - parent.mc.fontRenderer.getStringWidth(displayText)/2,
                y + 6, enabled ? 0xFFFFFF : 0xAAAAAA);
        }

        public boolean isMouseOver(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width &&
                   mouseY >= y && mouseY <= y + 20;
        }

        public void toggleState() {
            enabled = !enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    private NodeState getRecipeState(RecipeEntry recipe) {
        // EntityPlayer должен быть найден
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;
        if (player != null) {
            if (recipe.isStudiedByPlayer(player.getUniqueID())) {
                return NodeState.STUDIED; // NodeState.STUDIED должен быть найден
            } else if (recipe.canPlayerStudy(player, ExperienceManager.getInstance().getPlayerData(player))) {
                return NodeState.AVAILABLE; // NodeState.AVAILABLE должен быть найден
            }
        }
        return NodeState.LOCKED; // NodeState.LOCKED должен быть найден
    }
}