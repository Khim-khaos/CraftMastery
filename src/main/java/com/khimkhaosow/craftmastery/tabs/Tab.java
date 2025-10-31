package com.khimkhaosow.craftmastery.tabs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.experience.PointsType;

import net.minecraft.util.text.TextFormatting;

/**
 * Вкладка для организации рецептов
 */
public class Tab {

    private final String id;
    private String name;
    private String description;
    private TextFormatting color;

    // Рецепты в этой вкладке
    private final Set<String> recipeIds;

    // Требования для открытия вкладки
    private int requiredSpecialPoints;

    // Вкладки, которые блокируются при открытии этой вкладки
    private final Set<String> blockingTabs;

    // Вкладки, которые разблокируются при открытии этой вкладки
    private final Set<String> unlockingTabs;

    // Права доступа к вкладке
    private boolean requiresPermission;
    private String requiredPermission;

    // Стоимость сброса
    private int resetCost;

    // Изучена ли вкладка игроком
    private final Set<UUID> studiedByPlayers;

    public Tab(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = "";
        this.color = TextFormatting.WHITE;

        this.recipeIds = new HashSet<>();
        this.blockingTabs = new HashSet<>();
        this.unlockingTabs = new HashSet<>();
        this.studiedByPlayers = new HashSet<>();

        this.requiredSpecialPoints = 0;
        this.requiresPermission = false;
        this.requiredPermission = "";
        this.resetCost = 10;
    }

    /**
     * Добавляет рецепт во вкладку
     */
    public void addRecipe(String recipeId) {
        recipeIds.add(recipeId);
        CraftMastery.logger.debug("Added recipe {} to tab {}", recipeId, id);
    }

    /**
     * Удаляет рецепт из вкладки
     */
    public void removeRecipe(String recipeId) {
        recipeIds.remove(recipeId);
        CraftMastery.logger.debug("Removed recipe {} from tab {}", recipeId, id);
    }

    /**
     * Проверяет, содержит ли вкладка указанный рецепт
     */
    public boolean containsRecipe(String recipeId) {
        return recipeIds.contains(recipeId);
    }

    /**
     * Добавляет блокирующую связь с другой вкладкой
     */
    public void addBlockingTab(String tabId) {
        blockingTabs.add(tabId);
        CraftMastery.logger.debug("Tab {} now blocks tab {}", id, tabId);
    }

    /**
     * Добавляет разблокирующую связь с другой вкладкой
     */
    public void addUnlockingTab(String tabId) {
        unlockingTabs.add(tabId);
        CraftMastery.logger.debug("Tab {} now unlocks tab {}", id, tabId);
    }

    /**
     * Проверяет, может ли игрок изучить эту вкладку
     */
    public boolean canPlayerStudy(UUID playerUUID, com.khimkhaosow.craftmastery.experience.PlayerExperienceData expData) {
        // Проверяем спец-очки
        if (expData.getPoints(PointsType.SPECIAL) < requiredSpecialPoints) {
            return false;
        }

        // Проверяем права доступа
        if (requiresPermission) {
            // В будущем: проверка прав через PermissionManager
            // Пока всегда возвращаем true для упрощения
        }

        return true;
    }

    /**
     * Изучает вкладку для игрока
     */
    public void studyForPlayer(UUID playerUUID, com.khimkhaosow.craftmastery.experience.PlayerExperienceData expData) {
        if (!canPlayerStudy(playerUUID, expData)) {
            return;
        }

        // Тратим спец-очки
        if (requiredSpecialPoints > 0) {
            expData.spendPoints(PointsType.SPECIAL, requiredSpecialPoints);
        }

        // Отмечаем как изученную
        studiedByPlayers.add(playerUUID);

        CraftMastery.logger.info("Player {} studied tab {}", playerUUID, id);
    }

    /**
     * Сбрасывает изучение вкладки для игрока
     */
    public void resetForPlayer(UUID playerUUID, com.khimkhaosow.craftmastery.experience.PlayerExperienceData expData) {
        if (!studiedByPlayers.contains(playerUUID)) {
            return;
        }

        // Проверяем, есть ли очки для сброса
        if (expData.getPoints(PointsType.RESET_SPECIAL) < resetCost) {
            return;
        }

        // Тратим очки сброса
        expData.spendPoints(PointsType.RESET_SPECIAL, resetCost);

        // Убираем из изученных
        studiedByPlayers.remove(playerUUID);

        CraftMastery.logger.info("Player {} reset tab {}", playerUUID, id);
    }

    /**
     * Проверяет, изучена ли вкладка игроком
     */
    public boolean isStudiedByPlayer(UUID playerUUID) {
        return studiedByPlayers.contains(playerUUID);
    }

    /**
     * Получает все рецепты вкладки
     */
    public Set<String> getRecipeIds() {
        return new HashSet<>(recipeIds);
    }

    /**
     * Получает количество рецептов во вкладке
     */
    public int getRecipeCount() {
        return recipeIds.size();
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TextFormatting getColor() { return color; }
    public void setColor(TextFormatting color) { this.color = color; }

    public int getRequiredSpecialPoints() { return requiredSpecialPoints; }
    public void setRequiredSpecialPoints(int requiredSpecialPoints) {
        this.requiredSpecialPoints = Math.max(0, requiredSpecialPoints);
    }

    public Set<String> getBlockingTabs() { return new HashSet<>(blockingTabs); }
    public Set<String> getUnlockingTabs() { return new HashSet<>(unlockingTabs); }

    public boolean requiresPermission() { return requiresPermission; }
    public void setRequiresPermission(boolean requiresPermission) { this.requiresPermission = requiresPermission; }

    public String getRequiredPermission() { return requiredPermission; }
    public void setRequiredPermission(String requiredPermission) { this.requiredPermission = requiredPermission; }

    public int getResetCost() { return resetCost; }
    public void setResetCost(int resetCost) { this.resetCost = Math.max(0, resetCost); }

    /**
     * Возвращает список изученных рецептов для указанного игрока
     */
    public Set<String> getStudiedRecipeIds(UUID playerUUID) {
        Set<String> studiedRecipes = new HashSet<>();
        for (String recipeId : recipeIds) {
            if (com.khimkhaosow.craftmastery.recipe.RecipeManager.getInstance()
                    .getRecipe(recipeId).isStudiedByPlayer(playerUUID)) {
                studiedRecipes.add(recipeId);
            }
        }
        return studiedRecipes;
    }

    /**
     * Возвращает список всех рецептов вкладки
     */
    public List<com.khimkhaosow.craftmastery.recipe.RecipeEntry> getRecipes() {
        List<com.khimkhaosow.craftmastery.recipe.RecipeEntry> recipes = new ArrayList<>();
        for (String recipeId : recipeIds) {
            com.khimkhaosow.craftmastery.recipe.RecipeEntry recipe = 
                com.khimkhaosow.craftmastery.recipe.RecipeManager.getInstance().getRecipe(recipeId);
            if (recipe != null) {
                recipes.add(recipe);
            }
        }
        return recipes;
    }
}
