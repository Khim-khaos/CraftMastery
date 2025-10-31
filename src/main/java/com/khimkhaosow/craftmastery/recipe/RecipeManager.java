package com.khimkhaosow.craftmastery.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.khimkhaosow.craftmastery.CraftMastery; // Предполагается, что logger доступен
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
// --- Импорты для Gson ---
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Менеджер рецептов в системе CraftMastery
 */
public class RecipeManager {

    private static RecipeManager instance;

    // Все рецепты в системе
    private final Map<String, RecipeEntry> recipes;

    // Рецепты по тегам
    private final Map<RecipeTag, List<RecipeEntry>> recipesByTag;

    // Фильтры для отображения рецептов
    private final Map<String, List<RecipeEntry>> filteredRecipes;

    public RecipeManager() {
        // Устанавливаем singleton заранее, чтобы избежать рекурсивных вызовов getInstance() во время загрузки
        instance = this;

        this.recipes = new HashMap<>();
        this.recipesByTag = new HashMap<>();
        this.filteredRecipes = new HashMap<>();

        // Инициализация тегов
        for (RecipeTag tag : RecipeTag.values()) {
            recipesByTag.put(tag, new ArrayList<>());
            filteredRecipes.put(tag.name(), new ArrayList<>());
        }

        loadRecipes();
    }

    public static RecipeManager getInstance() {
        if (instance == null) {
            instance = new RecipeManager();
        }
        return instance;
    }

    private void loadRecipes() {
        // Очищаем существующие рецепты
        recipes.clear();
        for (List<RecipeEntry> tagList : recipesByTag.values()) {
            tagList.clear();
        }
        
        // Загружаем сохраненные настройки рецептов
        com.khimkhaosow.craftmastery.config.RecipeConfig.loadRecipes();
        
        // Загружаем и оборачиваем все рецепты из Minecraft
        for (IRecipe recipe : ForgeRegistries.RECIPES) {
            if (recipe.getRecipeOutput().isEmpty()) continue;
            if (recipe instanceof RestrictedRecipe) continue; // Пропускаем уже обёрнутые

            ResourceLocation location = recipe.getRegistryName();
            if (location == null) continue;

            // Создаём обёртку и регистрируем её вместо оригинального рецепта
            // NOTE: Замена рецепта в ForgeRegistries.RECIPES может быть сложной и потенциально небезопасной.
            // Обычно делают обертку или используют события для изменения поведения.
            // Предположим, RestrictedRecipe работает корректно.
            RestrictedRecipe restricted = new RestrictedRecipe(recipe);
            // ForgeRegistries.RECIPES.register(restricted, location); // Осторожно! Перезапись оригинального рецепта.
            // Лучше хранить обёртки отдельно и использовать их в обработчиках крафта/JEI.
            // Для упрощения примера, просто создадим RecipeEntry.
            // ResourceLocation location = recipe.getRegistryName(); // Уже получено выше
            RecipeEntry entry = new RecipeEntry(location);

            // Добавляем базовые теги
            entry.addTag(RecipeTag.COMMON);
            entry.addTag(RecipeTag.NOT_STUDIED);

            // Определяем теги на основе типа рецепта
            determineRecipeTags(entry, recipe);

            recipes.put(location.toString(), entry);

            // Добавляем в списки по тегам
            for (RecipeTag tag : entry.getTags()) {
                recipesByTag.get(tag).add(entry);
            }
        }

        CraftMastery.logger.info("Loaded {} recipes", recipes.size());
    }

    private void determineRecipeTags(RecipeEntry entry, IRecipe recipe) {
        // Определяем теги на основе рецепта
        if (recipe.getRecipeOutput().getItem().getRegistryName().getNamespace().equals("minecraft")) {
            // Ванильные рецепты - обычные
            entry.addTag(RecipeTag.COMMON);
        }

        // В будущем можно добавить логику определения технических/магических рецептов
        // на основе модов (например, рецепты из IC2 - технические, из Thaumcraft - магические)
    }

    /**
     * Получает рецепт по ID
     */
    public RecipeEntry getRecipe(String recipeId) {
        return recipes.get(recipeId);
    }

    /**
     * Получает рецепт по ResourceLocation
     */
    public RecipeEntry getRecipe(ResourceLocation location) {
        return recipes.get(location.toString());
    }

    /**
     * Получает все рецепты
     */
    public List<RecipeEntry> getAllRecipes() {
        return new ArrayList<>(recipes.values());
    }

    /**
     * Получает рецепты по тегу
     */
    public List<RecipeEntry> getRecipesByTag(RecipeTag tag) {
        return new ArrayList<>(recipesByTag.get(tag));
    }

    /**
     * Получает доступные рецепты для игрока
     */
    public List<RecipeEntry> getAvailableRecipes(EntityPlayer player) {
        List<RecipeEntry> available = new ArrayList<>();
        PlayerExperienceData expData = ExperienceManager.getInstance().getPlayerData(player);
        UUID playerUUID = player.getUniqueID();

        for (RecipeEntry recipe : recipes.values()) {
            // Проверяем, не заблокирован ли рецепт вкладками
            boolean tabBlocked = isRecipeBlockedByTabs(recipe, playerUUID);
            if (tabBlocked) continue;

            // Проверяем, может ли игрок изучить рецепт
            if (recipe.canPlayerStudy(player, expData)) {
                available.add(recipe);
            }
        }

        return available;
    }

    /**
     * Получает изученные рецепты игрока
     */
    public List<RecipeEntry> getStudiedRecipes(EntityPlayer player) {
        List<RecipeEntry> studied = new ArrayList<>();
        UUID playerUUID = player.getUniqueID();

        for (RecipeEntry recipe : recipes.values()) {
            if (recipe.isStudiedByPlayer(playerUUID)) {
                studied.add(recipe);
            }
        }

        return studied;
    }

    /**
     * Изучает рецепт для игрока
     */
    public boolean studyRecipe(EntityPlayer player, String recipeId) {
        RecipeEntry recipe = getRecipe(recipeId);
        if (recipe == null) return false;

        PlayerExperienceData expData = ExperienceManager.getInstance().getPlayerData(player);

        boolean success = recipe.studyForPlayer(player, expData);

        if (success) {
            // Обновляем теги
            updateRecipeTags(recipe, player.getUniqueID());

            // Блокируем рецепты из тех же вкладок
            blockConflictingRecipes(recipe, player.getUniqueID());

            // Обновляем видимость рецептов в JEI сразу после изучения
            try {
                if (com.khimkhaosow.craftmastery.integration.jei.JEIIntegration.isJEILoaded()) {
                    com.khimkhaosow.craftmastery.integration.jei.JEIIntegration.updateRecipeVisibility(player);
                }
            } catch (Exception e) {
                CraftMastery.logger.debug("Failed to notify JEI about recipe study: {}", e.getMessage());
            }
        }

        return success;
    }

    /**
     * Сбрасывает рецепт для игрока
     */
    public boolean resetRecipe(EntityPlayer player, String recipeId) {
        RecipeEntry recipe = getRecipe(recipeId);
        if (recipe == null) return false;

        PlayerExperienceData expData = ExperienceManager.getInstance().getPlayerData(player);

        recipe.resetForPlayer(player.getUniqueID(), expData);

        if (recipe.isStudiedByPlayer(player.getUniqueID())) {
            // Обновляем теги
            updateRecipeTags(recipe, player.getUniqueID());
        }

        return true;
    }

    /**
     * Обновляет теги рецепта для игрока
     */
    private void updateRecipeTags(RecipeEntry recipe, UUID playerUUID) {
        Set<RecipeTag> currentTags = recipe.getTags();

        // Убираем старые теги состояния
        recipe.tags.remove(RecipeTag.STUDIED);
        recipe.tags.remove(RecipeTag.NOT_STUDIED);

        // Добавляем новый тег состояния
        if (recipe.isStudiedByPlayer(playerUUID)) {
            recipe.addTag(RecipeTag.STUDIED);
        } else {
            recipe.addTag(RecipeTag.NOT_STUDIED);
        }
    }

    /**
     * Проверяет, заблокирован ли рецепт вкладками
     */
    private boolean isRecipeBlockedByTabs(RecipeEntry recipe, UUID playerUUID) {
        // Логика проверки блокировок через вкладки
        // В будущем интегрировать с TabManager

        return false; // Пока не блокируем
    }

    /**
     * Блокирует конфликтующие рецепты
     */
    private void blockConflictingRecipes(RecipeEntry recipe, UUID playerUUID) {
        // Блокируем рецепты из тех же вкладок, если есть связи
        for (String blockingId : recipe.getBlockingRecipes()) {
            RecipeEntry blockingRecipe = getRecipe(blockingId);
            if (blockingRecipe != null && blockingRecipe.isStudiedByPlayer(playerUUID)) {
                resetRecipe(null, blockingId); // TODO: передать реального игрока
            }
        }
    }

    /**
     * Фильтрует рецепты по тегам
     */
    public List<RecipeEntry> filterRecipes(Set<RecipeTag> filterTags) {
        List<RecipeEntry> filtered = new ArrayList<>();

        if (filterTags.isEmpty()) {
            return getAllRecipes();
        }

        for (RecipeEntry recipe : recipes.values()) {
            boolean matches = false;
            for (RecipeTag tag : filterTags) {
                if (recipe.hasTag(tag)) {
                    matches = true;
                    break;
                }
            }
            if (matches) {
                filtered.add(recipe);
            }
        }

        return filtered;
    }

    /**
     * Ищет рецепты по названию результата
     */
    public List<RecipeEntry> searchRecipes(String query) {
        List<RecipeEntry> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (RecipeEntry recipe : recipes.values()) {
            if (recipe.getRecipeResult().isEmpty()) continue;

            String itemName = recipe.getRecipeResult().getDisplayName().toLowerCase();
            String modName = recipe.getRecipeResult().getItem().getRegistryName().getNamespace().toLowerCase();

            if (itemName.contains(lowerQuery) || modName.contains(lowerQuery)) {
                results.add(recipe);
            }
        }

        return results;
    }

    /**
     * Регистрирует новый рецепт в системе
     * (Это может быть метод для добавления рецептов из конфига или других источников)
     */
    public void registerRecipe(RecipeEntry recipe) {
        if (recipe == null) return;

        String recipeId = recipe.getRecipeId();
        if (recipes.containsKey(recipeId)) {
             CraftMastery.logger.warn("Recipe with ID {} already exists, overwriting.", recipeId);
             // Удаляем старый рецепт из списков по тегам
             RecipeEntry oldRecipe = recipes.get(recipeId);
             for (RecipeTag tag : oldRecipe.getTags()) {
                 recipesByTag.get(tag).remove(oldRecipe);
             }
        }
        recipes.put(recipeId, recipe);
        
        // Обновляем списки по тегам
        for (RecipeTag tag : recipe.getTags()) {
            recipesByTag.get(tag).add(recipe);
        }
        
        // Сохраняем изменения в конфигурацию
        com.khimkhaosow.craftmastery.config.RecipeConfig.saveRecipes(this);

        CraftMastery.logger.info("Registered/Updated recipe: {}", recipeId);
    }

    // --- ДОБАВЛЕН МЕТОД addRecipe ---
    /**
     * Добавляет рецепт в систему (для использования в RecipeConfig)
     */
    public void addRecipe(RecipeEntry recipe) {
        registerRecipe(recipe); // Используем существующий метод
    }

    /**
     * Класс для статистики рецептов
     */
    public static class RecipeStats {
        public int totalRecipes = 0;
        public int commonRecipes = 0;
        public int technicalRecipes = 0;
        public int magicalRecipes = 0;
        public int magicalTechnicalRecipes = 0;

        @Override
        public String toString() {
            return String.format("Recipes: %d total (Common: %d, Technical: %d, Magical: %d, MagiTech: %d)",
                totalRecipes, commonRecipes, technicalRecipes, magicalRecipes, magicalTechnicalRecipes);
        }
    }
    
    /**
     * Очищает все рецепты
     */
    public void clearRecipes() {
        recipes.clear();
        for (List<RecipeEntry> tagList : recipesByTag.values()) {
            tagList.clear();
        }
    }

    /**
     * Экспортирует рецепт в JSON
     */
    public String exportRecipeToJson(String recipeId) {
        RecipeEntry recipe = getRecipe(recipeId);
        if (recipe == null) return "{}";
        
        // Используем GsonBuilder
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        // Предполагаем, что RecipeConfig.exportRecipe возвращает объект, который можно сериализовать
        Object exportData = com.khimkhaosow.craftmastery.config.RecipeConfig.exportRecipe(recipe);
        if (exportData == null) return "{}";
        return gson.toJson(exportData);
    }

    /**
     * Импортирует рецепт из JSON
     */
    public boolean importRecipeFromJson(String json) {
        try {
            // Используем JsonParser
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                
            RecipeEntry recipe = com.khimkhaosow.craftmastery.config.RecipeConfig.importRecipe(jsonObject);
            if (recipe != null) {
                registerRecipe(recipe);
                return true;
            }
        } catch (Exception e) {
            CraftMastery.logger.error("Failed to import recipe from JSON", e);
        }
        return false;
    }
    
    /**
     * Сохраняет все рецепты
     */
    public void saveAllRecipes() {
        com.khimkhaosow.craftmastery.config.RecipeConfig.saveRecipes(this);
    }
    
    /**
     * Перезагружает все рецепты
     */
    public void reloadRecipes() {
        clearRecipes();
        loadRecipes();
    }
}