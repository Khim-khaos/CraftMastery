package com.khimkhaosow.craftmastery.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.experience.PointsType;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import com.khimkhaosow.craftmastery.recipe.RecipeTag;
import com.khimkhaosow.craftmastery.tabs.Tab;
import com.khimkhaosow.craftmastery.tabs.TabManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;

/**
 * GUI древа рецептов CraftMastery - соответствует дизайну книги с узлами
 */
public class GuiRecipeTree extends GuiScreen {

    private static final ResourceLocation BOOK_TEXTURE = new ResourceLocation("craftmastery", "textures/gui/craftmastery_book.png");

    private final EntityPlayer player;
    private PlayerExperienceData experienceData;

    // Данные древа
    private List<RecipeNode> recipeNodes;
    private List<ConnectionLine> connectionLines;
    private Map<String, RecipeNode> nodeMap;

    // Навигация по древу
    private float offsetX = 0;
    private float offsetY = 0;
    private float scale = 1.0f;

    // Выбранный узел
    private RecipeNode selectedNode;

    // Кнопки
    private GuiButton backButton;
    private GuiButton zoomInButton;
    private GuiButton zoomOutButton;
    private GuiButton centerButton;
    private GuiButton studyButton;

    // Фильтры
    private boolean showOnlyAvailable = false;
    private boolean showOnlyStudied = false;
    private Tab currentTab = null;

    // GUI размеры
    private int treeAreaWidth;
    private int treeAreaHeight;

    public GuiRecipeTree(EntityPlayer player) {
        this.player = player;
        this.experienceData = ExperienceManager.getInstance().getPlayerData(player);
        this.recipeNodes = new ArrayList<>();
        this.connectionLines = new ArrayList<>();
        this.nodeMap = new HashMap<>();

        buildRecipeTree();
    }

    private void buildRecipeTree() {
        recipeNodes.clear();
        connectionLines.clear();
        nodeMap.clear();

        List<RecipeEntry> recipes;
        if (currentTab != null) {
            // Показываем только рецепты из текущей вкладки
            recipes = new ArrayList<>();
            for (RecipeEntry recipe : RecipeManager.getInstance().getAllRecipes()) {
                if (currentTab.getRecipeIds().contains(recipe.getRecipeId())) {
                    recipes.add(recipe);
                }
            }
        } else {
            // Показываем все доступные рецепты
            recipes = RecipeManager.getInstance().getAvailableRecipes(player);
        }

        // Создаем узлы для рецептов
        for (RecipeEntry recipe : recipes) {
            RecipeNode node = createRecipeNode(recipe);
            recipeNodes.add(node);
            nodeMap.put(recipe.getRecipeId(), node);
        }

        // Создаем связи между узлами
        createConnections();
    }

    private RecipeNode createRecipeNode(RecipeEntry recipe) {
        UUID playerUUID = player.getUniqueID();
        boolean isStudied = recipe.isStudiedByPlayer(playerUUID);
        boolean canStudy = recipe.canPlayerStudy(player, experienceData);
        boolean canReset = recipe.canPlayerReset(player, experienceData); // Предполагаем наличие метода canPlayerReset

        RecipeNode node = new RecipeNode();
        node.recipe = recipe;
        node.x = recipe.getGraphX();
        node.y = recipe.getGraphY();
        node.width = 64; // Увеличиваем ширину для размещения текста
        node.height = 48; // Увеличиваем высоту для размещения текста

        // Определяем состояние узла
        if (isStudied) {
            node.state = NodeState.STUDIED;
        } else if (canStudy) {
            node.state = NodeState.AVAILABLE;
        } else {
            node.state = NodeState.LOCKED;
        }

        return node;
    }

    private void createConnections() {
        connectionLines.clear();

        for (RecipeNode node : recipeNodes) {
            RecipeEntry recipe = node.recipe;

            // Создаем связи с требуемыми рецептами
            for (String requiredRecipeId : recipe.getRequiredRecipes()) {
                RecipeNode requiredNode = nodeMap.get(requiredRecipeId);
                if (requiredNode != null) {
                    ConnectionLine line = new ConnectionLine();
                    line.startNode = requiredNode;
                    line.endNode = node;
                    line.type = ConnectionType.REQUIREMENT;
                    connectionLines.add(line);
                }
            }

            // Создаем связи с разблокирующими рецептами
            for (String unlockingRecipeId : recipe.getUnlockingRecipes()) {
                RecipeNode unlockingNode = nodeMap.get(unlockingRecipeId);
                if (unlockingNode != null) {
                    ConnectionLine line = new ConnectionLine();
                    line.startNode = node;
                    line.endNode = unlockingNode;
                    line.type = ConnectionType.UNLOCKS;
                    connectionLines.add(line);
                }
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        // Адаптивные размеры в зависимости от размера экрана
        int minDimension = Math.min(width, height);
        treeAreaWidth = width - 120; // Оставляем место для панели информации
        treeAreaHeight = height - 80; // Оставляем место для кнопок

        // На маленьких экранах уменьшаем отступы
        if (minDimension < 400) {
            treeAreaWidth = width - 80;
            treeAreaHeight = height - 60;
        }

        // Кнопки навигации - адаптивное позиционирование 
        int buttonY = minDimension < 400 ? 5 : 10;
        int buttonHeight = minDimension < 400 ? 16 : 20;
        int smallButtonWidth = minDimension < 400 ? 25 : 30;
        int normalButtonWidth = minDimension < 400 ? 45 : 60;

        backButton = new GuiButton(0, 5, buttonY, normalButtonWidth, buttonHeight, "Назад");
        zoomInButton = new GuiButton(1, backButton.x + backButton.width + 5, buttonY, smallButtonWidth, buttonHeight, "+");
        zoomOutButton = new GuiButton(2, zoomInButton.x + zoomInButton.width + 5, buttonY, smallButtonWidth, buttonHeight, "-");
        centerButton = new GuiButton(3, zoomOutButton.x + zoomOutButton.width + 5, buttonY, normalButtonWidth, buttonHeight, "Центр");
        studyButton = new GuiButton(4, 5, height - buttonHeight - 5, normalButtonWidth + 10, buttonHeight, "Изучить");

        // На маленьких экранах делаем кнопки меньше
        if (minDimension < 400) {
            backButton.width = 50;
            studyButton.width = 60;
            studyButton.y = height - 25;
        }

        buttonList.add(backButton);
        buttonList.add(zoomInButton);
        buttonList.add(zoomOutButton);
        buttonList.add(centerButton);
        buttonList.add(studyButton);

        // Кнопки фильтров
        int filterX = width - (minDimension < 400 ? 65 : 85);
        int filterButtonWidth = minDimension < 400 ? 60 : 80;
        int filterButtonSpacing = minDimension < 400 ? 20 : 25;

        buttonList.add(new GuiButton(10, filterX, buttonY, filterButtonWidth, buttonHeight, "Доступные"));
        buttonList.add(new GuiButton(11, filterX, buttonY + filterButtonSpacing, filterButtonWidth, buttonHeight, "Изученные"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws java.io.IOException {
        switch (button.id) {
            case 0: // Назад
                mc.displayGuiScreen(new GuiCraftMastery(player));
                break;
            case 1: // Увеличить
                scale = Math.min(scale * 1.2f, 3.0f);
                break;
            case 2: // Уменьшить
                scale = Math.max(scale / 1.2f, 0.3f);
                break;
            case 3: // Центрировать
                offsetX = (width - treeAreaWidth) / 2;
                offsetY = (height - treeAreaHeight) / 2;
                scale = 1.0f;
                break;
            case 4: // Изучить
                if (selectedNode != null && selectedNode.state == NodeState.AVAILABLE) {
                    studySelectedRecipe();
                }
                break;
            case 10: // Фильтр доступные
                showOnlyAvailable = !showOnlyAvailable;
                updateFilterButtons();
                break;
            case 11: // Фильтр изученные
                showOnlyStudied = !showOnlyStudied;
                updateFilterButtons();
                break;
        }
    }

    private void updateFilterButtons() {
        // Обновляем текст кнопок фильтров
        for (GuiButton button : buttonList) {
            if (button.id == 10) {
                button.displayString = (showOnlyAvailable ? TextFormatting.GREEN : TextFormatting.WHITE) + "Доступные";
            } else if (button.id == 11) {
                button.displayString = (showOnlyStudied ? TextFormatting.GREEN : TextFormatting.WHITE) + "Изученные";
            }
        }

        // Перестраиваем древо с учетом фильтров
        buildRecipeTree();
    }

    private void studySelectedRecipe() {
        if (selectedNode == null || selectedNode.recipe == null) return;

        RecipeEntry recipe = selectedNode.recipe;

        // Проверяем права
        if (!PermissionManager.getInstance().hasPermission(player, PermissionType.LEARN_RECIPES)) {
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.RED + "У вас нет прав на изучение рецептов"));
            return;
        }

        // Пытаемся изучить рецепт
        boolean success = RecipeManager.getInstance().studyRecipe(player, recipe.getRecipeId());

        if (success) {
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.GREEN + "Рецепт '" + recipe.getRecipeResult().getDisplayName() + "' изучен!"));

            // Обновляем данные опыта
            experienceData = ExperienceManager.getInstance().getPlayerData(player);

            // Перестраиваем древо
            buildRecipeTree();
            updateStudyButton();
        } else {
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.RED + "Не удалось изучить рецепт"));
        }
    }

    private void updateStudyButton() {
        if (studyButton != null) {
            if (selectedNode != null && selectedNode.state == NodeState.AVAILABLE) {
                studyButton.enabled = true;
                studyButton.displayString = "Изучить";
            } else {
                studyButton.enabled = false;
                studyButton.displayString = "Недоступно";
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Рисуем фон книги
        drawBookBackground();

        // Рисуем заголовок
        drawCenteredString(mc.fontRenderer, TextFormatting.BOLD + "Древо рецептов", width / 2, 20, 0xFFFFFF);

        // Рисуем информацию об игроке
        drawPlayerInfo();

        // Рисуем древо рецептов
        drawRecipeTree(mouseX, mouseY);

        // Рисуем кнопки
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBookBackground() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (BOOK_TEXTURE != null) {
            mc.getTextureManager().bindTexture(BOOK_TEXTURE);
            drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
        } else {
            // Рисуем простой фон
            drawRect(0, 0, width, height, 0xFF8B4513); // Коричневый цвет книги
            drawRect(20, 20, width - 20, height - 20, 0xFFF5DEB3); // Бежевый цвет страниц
        }
    }

    private void drawPlayerInfo() {
        int y = 35;

        // Уровень и опыт
        String levelText = TextFormatting.WHITE + "Уровень: " + TextFormatting.YELLOW + experienceData.getLevel();
        drawString(mc.fontRenderer, levelText, 10, y, 0xFFFFFF);
        y += 15;

        String progressText = TextFormatting.WHITE + "Прогресс: " + TextFormatting.GREEN +
            String.format("%.1f%%", experienceData.getLevelProgress());
        drawString(mc.fontRenderer, progressText, 10, y, 0xFFFFFF);
        y += 15;

        // Очки
        y += 10;
        drawString(mc.fontRenderer, TextFormatting.GOLD + "Очки:", 10, y, 0xFFFFFF);
        y += 15;

        for (PointsType type : PointsType.values()) {
            String pointsText = type.getColor() + type.getDisplayName() + ": " + TextFormatting.WHITE + experienceData.getPoints(type);
            drawString(mc.fontRenderer, pointsText, 10, y, 0xFFFFFF);
            y += 12;
        }
    }

    private void drawRecipeTree(int mouseX, int mouseY) {
        // Сохраняем текущую матрицу трансформации
        GlStateManager.pushMatrix();

        // Применяем масштаб и смещение
        GlStateManager.translate(offsetX + treeAreaWidth / 2, offsetY + treeAreaHeight / 2, 0);
        GlStateManager.scale(scale, scale, 1.0f);

        // Рисуем линии связей
        for (ConnectionLine line : connectionLines) {
            drawConnectionLine(line);
        }

        // Рисуем узлы
        for (RecipeNode node : recipeNodes) {
            if (isNodeVisible(node)) {
                drawRecipeNode(node, mouseX, mouseY);
            }
        }

        // Восстанавливаем матрицу трансформации
        GlStateManager.popMatrix();
    }

    private boolean isNodeVisible(RecipeNode node) {
        if (showOnlyAvailable && node.state != NodeState.AVAILABLE) return false;
        if (showOnlyStudied && node.state != NodeState.STUDIED) return false;
        return true;
    }

    private void drawConnectionLine(ConnectionLine line) {
        if (line.startNode == null || line.endNode == null) return;

        float startX = line.startNode.x;
        float startY = line.startNode.y;
        float endX = line.endNode.x;
        float endY = line.endNode.y;

        // Цвет линии в зависимости от типа связи
        int color;
        switch (line.type) {
            case REQUIREMENT:
                color = line.startNode.state == NodeState.LOCKED ? 0xFF666666 : 0xFF000000;
                break;
            case UNLOCKS:
                color = 0xFF00AA00;
                break;
            default:
                color = 0xFF000000;
                break;
        }

        // Рисуем линию
        drawLine(startX, startY, endX, endY, color);
    }

    private void drawLine(float x1, float y1, float x2, float y2, int color) {
        // Простая реализация линии
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length == 0) return;

        float angle = (float) Math.atan2(dy, dx);

        // Рисуем стрелку на конце линии
        float arrowSize = 5.0f;
        float arrowX = x2 - (float) Math.cos(angle) * arrowSize;
        float arrowY = y2 - (float) Math.sin(angle) * arrowSize;

        // Рисуем основную линию
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableTexture2D();

        // Используем простые линии (можно улучшить)
        drawRect((int) (x1 - 1), (int) (y1 - 1), (int) (x2 + 1), (int) (y1 + 1), color);
        drawRect((int) (x2 - 1), (int) (y1 - 1), (int) (x2 + 1), (int) (y2 + 1), color);

        GlStateManager.enableTexture2D();
    }

    private void drawRecipeNode(RecipeNode node, int mouseX, int mouseY) {
        if (node.recipe == null) return;

        // Проверяем, находится ли курсор над узлом
        boolean isHovered = isMouseOverNode(node, mouseX, mouseY);

        // Выбираем цвет узла в зависимости от состояния
        int nodeColor;
        switch (node.state) {
            case STUDIED:
                nodeColor = 0xFF00FF00; // Зеленый
                break;
            case AVAILABLE:
                nodeColor = 0xFFFFFF00; // Желтый
                break;
            case LOCKED:
                nodeColor = 0xFF666666; // Серый
                break;
            default:
                nodeColor = 0xFF666666;
                break;
        }

        // Подсвечиваем узел при наведении
        if (isHovered) {
            nodeColor = (nodeColor & 0x00FFFFFF) | 0xFF000000; // Делаем ярче
            selectedNode = node;
        }

        // Рисуем фон узла (прямоугольник)
        int nodeX = (int) (node.x - node.width / 2);
        int nodeY = (int) (node.y - node.height / 2);
        drawRect(nodeX, nodeY, nodeX + node.width, nodeY + node.height, nodeColor);

        // Рисуем рамку
        int borderColor = isHovered ? 0xFFFFFFFF : 0xFF000000;
        drawRect(nodeX - 1, nodeY - 1, nodeX + node.width + 1, nodeY, borderColor);
        drawRect(nodeX - 1, nodeY + node.height, nodeX + node.width + 1, nodeY + node.height + 1, borderColor);
        drawRect(nodeX - 1, nodeY - 1, nodeX, nodeY + node.height + 1, borderColor);
        drawRect(nodeX + node.width, nodeY - 1, nodeX + node.width + 1, nodeY + node.height + 1, borderColor);

        // Рисуем иконку предмета
        ItemStack result = node.recipe.getRecipeResult();
        if (!result.isEmpty()) {
            // Рисуем предмет с помощью RenderItem
            GlStateManager.pushMatrix();
            GlStateManager.translate(node.x - 8, node.y - 8, 0);
            GlStateManager.scale(1.0f, 1.0f, 1.0f);

            // Используем RenderItem для правильного отображения предмета
            RenderItem renderItem = mc.getRenderItem();
            renderItem.renderItemIntoGUI(result, 0, 0);
            renderItem.renderItemOverlayIntoGUI(mc.fontRenderer, result, 0, 0, "");

            GlStateManager.popMatrix();
        }

        // Рисуем текст внутри узла
        int textX = nodeX + 5;
        int textY = nodeY + 5;

        // Название рецепта (обрезаем, если длинное)
        String recipeName = result.getDisplayName();
        if (mc.fontRenderer.getStringWidth(recipeName) > node.width - 10) {
            recipeName = recipeName.substring(0, Math.min(recipeName.length(), 10)) + "...";
        }
        drawString(mc.fontRenderer, recipeName, textX, textY, 0xFFFFFF);
        textY += 12;

        // Стоимость изучения (если не изучен)
        if (node.state == NodeState.AVAILABLE) {
            int cost = node.recipe.getRequiredLearningPoints();
            int costColor = experienceData.getPoints(PointsType.LEARNING) >= cost ? 0x00FF00 : 0xFF0000;
            drawString(mc.fontRenderer, "Изучение: " + cost, textX, textY, costColor);
            textY += 12;
        }

        // Стоимость сброса (если изучен)
        if (node.state == NodeState.STUDIED) {
            int cost = node.recipe.getRequiredResetPoints(); // Предполагаем наличие метода getRequiredResetPoints
            int costColor = experienceData.getPoints(PointsType.RESET) >= cost ? 0x00FFFF : 0xFF0000;
            drawString(mc.fontRenderer, "Сброс: " + cost, textX, textY, costColor);
            textY += 12;
        }

        // Индикатор изученности (галочка)
        if (node.state == NodeState.STUDIED) {
            drawString(mc.fontRenderer, "✓", nodeX + node.width - 15, nodeY + 5, 0xFF00FF00);
        }

        // Если узел выбран, показываем подсказку (как на изображении)
        if (selectedNode != null && selectedNode.equals(node)) {
            // Рисуем подсказку (реализация упрощена)
            List<String> tooltip = node.recipe.getFullTooltip(player, experienceData);
            if (!tooltip.isEmpty()) {
                int tooltipWidth = 0;
                for (String line : tooltip) {
                    tooltipWidth = Math.max(tooltipWidth, mc.fontRenderer.getStringWidth(line));
                }
                int tooltipX = nodeX + node.width / 2 - tooltipWidth / 2;
                int tooltipY = nodeY - tooltip.size() * 10 - 10; // Над узлом

                // Фон подсказки
                drawGradientRect(tooltipX - 3, tooltipY - 3,
                               tooltipX + tooltipWidth + 3, tooltipY + tooltip.size() * 10 + 3,
                               0xF0100010, 0xF0100010);

                // Текст подсказки
                for (int i = 0; i < tooltip.size(); i++) {
                    drawString(mc.fontRenderer, tooltip.get(i),
                             tooltipX, tooltipY + i * 10,
                             0xFFFFFFFF);
                }
            }
        }
    }

    private boolean isMouseOverNode(RecipeNode node, int mouseX, int mouseY) {
        // Проверяем, находится ли курсор над узлом с учетом масштаба и смещения
        float worldMouseX = (mouseX - offsetX - treeAreaWidth / 2) / scale;
        float worldMouseY = (mouseY - offsetY - treeAreaHeight / 2) / scale;

        return worldMouseX >= node.x - node.width / 2 &&
               worldMouseX <= node.x + node.width / 2 &&
               worldMouseY >= node.y - node.height / 2 &&
               worldMouseY <= node.y + node.height / 2;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // Внутренние классы для древа
    private enum NodeState {
        STUDIED, AVAILABLE, LOCKED
    }

    private enum ConnectionType {
        REQUIREMENT, UNLOCKS
    }

    private static class RecipeNode {
        RecipeEntry recipe;
        float x, y;
        int width, height;
        NodeState state;
    }

    private static class ConnectionLine {
        RecipeNode startNode;
        RecipeNode endNode;
        ConnectionType type;
    }
}