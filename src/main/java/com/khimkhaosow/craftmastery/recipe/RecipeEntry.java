package com.khimkhaosow.craftmastery.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.experience.PointsType;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Запись рецепта в системе CraftMastery
 */
public class RecipeEntry {

    // Идентификатор рецепта (имя мода:имя_рецепта)
    private final String recipeId;

    // Местоположение рецепта в реестре
    private final ResourceLocation recipeLocation;

    // Оригинальный рецепт Minecraft
    private IRecipe minecraftRecipe;

    // Требования для изучения
    private int requiredLearningPoints = 0;
    private int requiredLevel = 1;
    private String studyMessage = "Рецепт изучен!";
    private int requiredResetPoints = 10; // Default

    // Позиция в графе рецептов
    private int graphX = 0;
    private int graphY = 0;

    // Требуется ли право для изучения
    private boolean requiresPermission = false;
    private String requiredPermission = "";

    // Требуемые рецепты для изучения
    private final Set<String> requiredRecipes = new HashSet<>();

    // Рецепты, которые блокируются при изучении этого рецепта
    private final Set<String> blockingRecipes = new HashSet<>();

    // Рецепты, которые разблокируются при изучении этого рецепта
    private final Set<String> unlockingRecipes = new HashSet<>();

    // Теги рецепта
    final Set<RecipeTag> tags = new HashSet<>();

    // Дополнительные визуальные данные
    private ResourceLocation customIcon;
    private String nodeTitle = "";

    // Описание и категории
    private String description = "";
    private String tooltip = "";
    private int difficulty = 1; // 1-5
    private String category = "";

    // Кешированные данные игроков
    private final Set<UUID> studiedPlayers = new HashSet<>();
    private final Set<UUID> resetPlayers = new HashSet<>();

    public RecipeEntry(ResourceLocation location) {
        this.recipeLocation = location;
        this.recipeId = location.toString();

        // Инициализируем кеш рецепта
        this.minecraftRecipe = null;
    }

    /**
     * Проверяет, может ли игрок изучить этот рецепт
     */
    public boolean canPlayerStudy(EntityPlayer player, PlayerExperienceData expData) {
        // Проверяем права доступа
        if (requiresPermission) {
            if (!PermissionManager.getInstance().hasPermission(player, PermissionType.LEARN_RECIPES)) {
                return false;
            }
        }

        // Проверяем очки изучения
        if (expData.getPoints(PointsType.LEARNING) < requiredLearningPoints) {
            return false;
        }

        // Проверяем уровень
        if (expData.getLevel() < requiredLevel) {
            return false;
        }

        // Проверяем требуемые рецепты
        for (String requiredId : requiredRecipes) {
            RecipeEntry requiredRecipe = RecipeManager.getInstance().getRecipe(requiredId);
            if (requiredRecipe == null || !requiredRecipe.isStudiedByPlayer(player.getUniqueID())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Изучает рецепт для игрока
     */
    public boolean studyForPlayer(EntityPlayer player, PlayerExperienceData expData) {
        if (!canPlayerStudy(player, expData)) {
            return false;
        }

        // Проверяем права доступа
        if (requiresPermission && !PermissionManager.getInstance().hasPermission(player, PermissionType.LEARN_RECIPES)) {
            return false;
        }

        // Проверяем очки изучения
        if (expData.getPoints(PointsType.LEARNING) < requiredLearningPoints) {
            return false;
        }

        // Проверяем уровень
        if (expData.getLevel() < requiredLevel) {
            return false;
        }

        // Проверяем требуемые рецепты
        for (String requiredId : requiredRecipes) {
            RecipeEntry requiredRecipe = RecipeManager.getInstance().getRecipe(requiredId);
            if (requiredRecipe == null || !requiredRecipe.isStudiedByPlayer(player.getUniqueID())) {
                return false;
            }
        }

        // Снимаем очки изучения
        if (!ExperienceManager.getInstance().spendPoints(player, PointsType.LEARNING, requiredLearningPoints)) {
            return false; // Хотя проверка выше должна это предотвратить
        }

        // Добавляем игрока в изучившие
        studiedPlayers.add(player.getUniqueID());
        resetPlayers.remove(player.getUniqueID()); // Убираем из сброшенных

        // Блокируем конфликтующие рецепты
        for (String blockId : blockingRecipes) {
            RecipeEntry blockRecipe = RecipeManager.getInstance().getRecipe(blockId);
            if (blockRecipe != null && blockRecipe.isStudiedByPlayer(player.getUniqueID())) {
                // Сбрасываем конфликтующий рецепт
                blockRecipe.resetForPlayer(player.getUniqueID(), expData);
            }
        }

        // Отправляем сообщение
        if (studyMessage != null && !studyMessage.isEmpty()) {
            player.sendMessage(new net.minecraft.util.text.TextComponentString(net.minecraft.util.text.TextFormatting.GREEN + studyMessage));
        }

        return true;
    }

    /**
     * Сбрасывает изучение рецепта для игрока
     */
    public void resetForPlayer(UUID playerUUID, PlayerExperienceData expData) {
        studiedPlayers.remove(playerUUID);
        resetPlayers.add(playerUUID);

        // Возвращаем очки изучения
        // NOTE: Возврат очков может быть сложной логикой, в зависимости от требований.
        // Для упрощения, не возвращаем.
        // ExperienceManager.getInstance().addPoints(player, PointsType.LEARNING, requiredLearningPoints);
    }

    /**
     * Проверяет, изучен ли рецепт игроком
     */
    public boolean isStudiedByPlayer(UUID playerUUID) {
        return studiedPlayers.contains(playerUUID);
    }

    /**
     * Проверяет, сброшен ли рецепт игроком
     */
    public boolean isResetByPlayer(UUID playerUUID) {
        return resetPlayers.contains(playerUUID);
    }

    /**
     * Добавляет требуемый рецепт
     */
    public void addRequiredRecipe(String recipeId) {
        requiredRecipes.add(recipeId);
    }

    /**
     * Добавляет разблокирующий рецепт
     */
    public void addUnlockingRecipe(String recipeId) {
        unlockingRecipes.add(recipeId);
    }

    /**
     * Проверяет, содержит ли рецепт указанный тег
     */
    public boolean hasTag(RecipeTag tag) {
        return tags.contains(tag);
    }

    /**
     * Добавляет тег
     */
    public void addTag(RecipeTag tag) {
        tags.add(tag);
    }

    /**
     * Получает результат рецепта
     */
    public ItemStack getRecipeResult() {
        IRecipe recipe = getMinecraftRecipe();
        if (recipe != null) {
            return recipe.getRecipeOutput();
        }
        return ItemStack.EMPTY;
    }

    /**
     * Получает оригинальный рецепт Minecraft
     */
    public IRecipe getMinecraftRecipe() {
        if (minecraftRecipe == null) {
            minecraftRecipe = ForgeRegistries.RECIPES.getValue(recipeLocation);
        }
        return minecraftRecipe;
    }

    // Геттеры и сеттеры
    public String getRecipeId() { return recipeId; }
    public ResourceLocation getRecipeLocation() { return recipeLocation; }
    public int getRequiredLearningPoints() { return requiredLearningPoints; }
    public void setRequiredLearningPoints(int requiredLearningPoints) {
        this.requiredLearningPoints = Math.max(0, requiredLearningPoints);
    }
    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = Math.max(1, requiredLevel);
    }
    public Set<String> getRequiredRecipes() { return new HashSet<>(requiredRecipes); }
    public Set<RecipeTag> getTags() { return new HashSet<>(tags); }
    public Set<String> getBlockingRecipes() { return new HashSet<>(blockingRecipes); }
    public Set<String> getUnlockingRecipes() { return new HashSet<>(unlockingRecipes); }
    public int getGraphX() { return graphX; }
    public void setGraphX(int graphX) { this.graphX = graphX; }
    public int getGraphY() { return graphY; }
    public void setGraphY(int graphY) { this.graphY = graphY; }

    // Методы для работы с требованиями рецепта
    public List<RecipeEntry> getRecipeRequirements() {
        List<RecipeEntry> requirements = new ArrayList<>();
        for (String id : requiredRecipes) {
            RecipeEntry recipe = RecipeManager.getInstance().getRecipe(id);
            if (recipe != null) {
                requirements.add(recipe);
            }
        }
        return requirements;
    }

    public List<RecipeEntry> getRecipeUnlockables() {
        List<RecipeEntry> unlockables = new ArrayList<>();
        for (String id : unlockingRecipes) {
            RecipeEntry recipe = RecipeManager.getInstance().getRecipe(id);
            if (recipe != null) {
                unlockables.add(recipe);
            }
        }
        return unlockables;
    }

    public void setRecipeRequirements(List<RecipeEntry> requirements) {
        requiredRecipes.clear();
        for (RecipeEntry recipe : requirements) {
            requiredRecipes.add(recipe.getRecipeId());
        }
    }

    public void setRecipeUnlockables(List<RecipeEntry> unlockables) {
        unlockingRecipes.clear();
        for (RecipeEntry recipe : unlockables) {
            unlockingRecipes.add(recipe.getRecipeId());
        }
    }

    public String getDisplayName() {
        ItemStack result = getRecipeResult();
        return result.isEmpty() ? recipeId : result.getDisplayName();
    }

    public void setDisplayName(String name) {
        this.studyMessage = "Рецепт " + name + " изучен!";
    }

    public void setRecipe(IRecipe recipe) {
        this.minecraftRecipe = recipe;
    }

    // Геттеры и сеттеры для новых полей
    public String getDescription() {
        return description != null ? description : "";
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getTooltip() {
        return tooltip != null ? tooltip : "";
    }
    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }
    public int getDifficulty() {
        return difficulty;
    }
    public void setDifficulty(int difficulty) {
        this.difficulty = Math.max(1, Math.min(5, difficulty));
    }
    public String getCategory() {
        return category != null ? category : "";
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public ResourceLocation getCustomIcon() {
        return customIcon;
    }

    public void setCustomIcon(ResourceLocation customIcon) {
        this.customIcon = customIcon;
    }

    public String getNodeTitle() {
        return nodeTitle != null ? nodeTitle : "";
    }

    public void setNodeTitle(String nodeTitle) {
        this.nodeTitle = nodeTitle;
    }

    public boolean requiresPermission() {
        return requiresPermission;
    }

    public void setRequiresPermission(boolean requiresPermission) {
        this.requiresPermission = requiresPermission;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public void setRequiredPermission(String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    public String getStudyMessage() {
        return studyMessage;
    }

    public void setStudyMessage(String studyMessage) {
        this.studyMessage = studyMessage;
    }

    public void addBlockingRecipe(String recipeId) {
        blockingRecipes.add(recipeId);
    }

    public boolean canPlayerReset(EntityPlayer player, PlayerExperienceData expData) {
        return isStudiedByPlayer(player.getUniqueID());
    }

    public int getRequiredResetPoints() {
        return requiredLearningPoints / 2;
    }

    /**
     * Получает полное описание рецепта для отображения в подсказке
     */
    public List<String> getFullTooltip(EntityPlayer player, PlayerExperienceData expData) {
        List<String> tooltipLines = new ArrayList<>();

        // Основное описание
        if (!getDescription().isEmpty()) {
            tooltipLines.add(getDescription());
        }

        // Сложность
        // --- ИСПРАВЛЕНО: используем StringBuilder вместо String.repeat() ---
        StringBuilder difficultyStars = new StringBuilder();
        for (int i = 0; i < getDifficulty(); i++) {
            difficultyStars.append("★");
        }
        tooltipLines.add("§7Сложность: §f" + difficultyStars.toString());

        // Категория
        if (!getCategory().isEmpty()) {
            tooltipLines.add("§7Категория: §e" + getCategory());
        }

        // Требования
        List<RecipeEntry> requirements = getRecipeRequirements();
        if (!requirements.isEmpty()) {
            tooltipLines.add("§7Требуется:");
            for (RecipeEntry recipe : requirements) {
                String color = recipe.isStudiedByPlayer(player.getUniqueID()) ? "§a" : "§c";
                tooltipLines.add(" " + color + "• " + recipe.getDisplayName());
            }
        }

        // Разблокирует
        List<RecipeEntry> unlockables = getRecipeUnlockables();
        if (!unlockables.isEmpty()) {
            tooltipLines.add("§7Разблокирует:");
            for (RecipeEntry recipe : unlockables) {
                String color = recipe.isStudiedByPlayer(player.getUniqueID()) ? "§a" : "§c";
                tooltipLines.add(" " + color + "• " + recipe.getDisplayName());
            }
        }

        // Добавляем особый текст для изученных рецептов
        if (isStudiedByPlayer(player.getUniqueID())) {
            tooltipLines.add("");
            tooltipLines.add("§a✓ Рецепт изучен");
        }

        return tooltipLines;
    }
}
