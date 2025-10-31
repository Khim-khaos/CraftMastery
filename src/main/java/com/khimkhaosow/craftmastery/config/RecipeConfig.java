package com.khimkhaosow.craftmastery.config;

import com.google.gson.*;
import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import com.khimkhaosow.craftmastery.recipe.RecipeTag;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Управляет конфигурацией рецептов и их сохранением/загрузкой
 */
public class RecipeConfig {
    private static final String RECIPES_DIR = "config/craftmastery/recipes";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Сохраняет все рецепты в JSON файлы
     */
    public static void saveRecipes(RecipeManager recipeManager) {
        if (recipeManager == null) {
            recipeManager = RecipeManager.getInstance();
        }

        saveRecipesInternal(recipeManager);
    }

    private static void saveRecipesInternal(RecipeManager recipeManager) {
        File recipesDir = new File(RECIPES_DIR);
        if (!recipesDir.exists()) {
            recipesDir.mkdirs();
        }

        // Группируем рецепты по категориям
        Map<String, List<RecipeEntry>> recipesByCategory = new HashMap<>();
        for (RecipeEntry recipe : recipeManager.getAllRecipes()) {
            String category = recipe.getCategory();
            if (category == null || category.isEmpty()) {
                category = "uncategorized";
            }
            recipesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(recipe);
        }

        // Сохраняем каждую категорию в отдельный файл
        for (Map.Entry<String, List<RecipeEntry>> entry : recipesByCategory.entrySet()) {
            String category = entry.getKey();
            List<RecipeEntry> recipes = entry.getValue();

            JsonArray recipesArray = new JsonArray();
            for (RecipeEntry recipe : recipes) {
                JsonObject recipeJson = new JsonObject();
                
                // Базовая информация
                recipeJson.addProperty("recipeId", recipe.getRecipeId());
                recipeJson.addProperty("recipeLocation", recipe.getRecipeLocation().toString());
                
                // Требования
                recipeJson.addProperty("requiredLearningPoints", recipe.getRequiredLearningPoints());
                recipeJson.addProperty("requiredLevel", recipe.getRequiredLevel());
                
                // Требуемые рецепты
                JsonArray requiredRecipes = new JsonArray();
                for (String reqRecipe : recipe.getRequiredRecipes()) {
                    requiredRecipes.add(reqRecipe);
                }
                recipeJson.add("requiredRecipes", requiredRecipes);
                
                // Теги
                JsonArray tags = new JsonArray();
                for (RecipeTag tag : recipe.getTags()) {
                    tags.add(tag.name());
                }
                recipeJson.add("tags", tags);
                
                // Блокировки и разблокировки
                JsonArray blockingRecipes = new JsonArray();
                for (String blockRecipe : recipe.getBlockingRecipes()) {
                    blockingRecipes.add(blockRecipe);
                }
                recipeJson.add("blockingRecipes", blockingRecipes);
                
                JsonArray unlockingRecipes = new JsonArray();
                for (String unlockRecipe : recipe.getUnlockingRecipes()) {
                    unlockingRecipes.add(unlockRecipe);
                }
                recipeJson.add("unlockingRecipes", unlockingRecipes);
                
                // Права доступа
                recipeJson.addProperty("requiresPermission", recipe.requiresPermission());
                recipeJson.addProperty("requiredPermission", recipe.getRequiredPermission());
                
                // Отображение
                recipeJson.addProperty("studyMessage", recipe.getStudyMessage());
                recipeJson.addProperty("description", recipe.getDescription());
                recipeJson.addProperty("tooltip", recipe.getTooltip());
                recipeJson.addProperty("difficulty", recipe.getDifficulty());
                recipeJson.addProperty("category", recipe.getCategory());
                
                // Позиция в графе
                recipeJson.addProperty("graphX", recipe.getGraphX());
                recipeJson.addProperty("graphY", recipe.getGraphY());
                
                recipesArray.add(recipeJson);
            }

            // Сохраняем в файл
            File categoryFile = new File(recipesDir, category + ".json");
            try {
                FileUtils.writeStringToFile(categoryFile, GSON.toJson(recipesArray), StandardCharsets.UTF_8);
            } catch (IOException e) {
                CraftMastery.logger.error("Failed to save recipes for category: " + category, e);
            }
        }
    }

    /**
     * Загружает все рецепты из JSON файлов
     */
    public static void loadRecipes() {
        File recipesDir = new File(RECIPES_DIR);
        if (!recipesDir.exists()) {
            return;
        }

        RecipeManager recipeManager = RecipeManager.getInstance();
        recipeManager.clearRecipes();

        // Загружаем все JSON файлы из директории
        File[] categoryFiles = recipesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (categoryFiles != null) {
            for (File categoryFile : categoryFiles) {
                try {
                    String json = FileUtils.readFileToString(categoryFile, StandardCharsets.UTF_8);
                    JsonParser parser = new JsonParser();
                    JsonArray recipesArray = parser.parse(json).getAsJsonArray();

                    for (JsonElement element : recipesArray) {
                        JsonObject recipeJson = element.getAsJsonObject();
                        
                        // Создаем базовый рецепт
                        ResourceLocation recipeLocation = new ResourceLocation(
                            recipeJson.get("recipeLocation").getAsString());
                        RecipeEntry recipe = new RecipeEntry(recipeLocation);
                        
                        // Загружаем требования
                        recipe.setRequiredLearningPoints(
                            recipeJson.get("requiredLearningPoints").getAsInt());
                        recipe.setRequiredLevel(
                            recipeJson.get("requiredLevel").getAsInt());
                        
                        // Загружаем требуемые рецепты
                        JsonArray requiredRecipes = recipeJson.getAsJsonArray("requiredRecipes");
                        for (JsonElement reqRecipe : requiredRecipes) {
                            recipe.addRequiredRecipe(reqRecipe.getAsString());
                        }
                        
                        // Загружаем теги
                        JsonArray tags = recipeJson.getAsJsonArray("tags");
                        for (JsonElement tag : tags) {
                            recipe.addTag(RecipeTag.valueOf(tag.getAsString()));
                        }
                        
                        // Загружаем блокировки и разблокировки
                        JsonArray blockingRecipes = recipeJson.getAsJsonArray("blockingRecipes");
                        for (JsonElement blockRecipe : blockingRecipes) {
                            recipe.addBlockingRecipe(blockRecipe.getAsString());
                        }
                        
                        JsonArray unlockingRecipes = recipeJson.getAsJsonArray("unlockingRecipes");
                        for (JsonElement unlockRecipe : unlockingRecipes) {
                            recipe.addUnlockingRecipe(unlockRecipe.getAsString());
                        }
                        
                        // Загружаем права доступа
                        recipe.setRequiresPermission(
                            recipeJson.get("requiresPermission").getAsBoolean());
                        recipe.setRequiredPermission(
                            recipeJson.get("requiredPermission").getAsString());
                        
                        // Загружаем отображение
                        recipe.setStudyMessage(
                            recipeJson.get("studyMessage").getAsString());
                        recipe.setDescription(
                            recipeJson.get("description").getAsString());
                        recipe.setTooltip(
                            recipeJson.get("tooltip").getAsString());
                        recipe.setDifficulty(
                            recipeJson.get("difficulty").getAsInt());
                        recipe.setCategory(
                            recipeJson.get("category").getAsString());
                        
                        // Загружаем позицию в графе
                        recipe.setGraphX(recipeJson.get("graphX").getAsInt());
                        recipe.setGraphY(recipeJson.get("graphY").getAsInt());
                        
                        // Добавляем рецепт в менеджер
                        recipeManager.addRecipe(recipe);
                    }
                } catch (Exception e) {
                    CraftMastery.logger.error("Failed to load recipes from file: " + 
                        categoryFile.getName(), e);
                }
            }
        }
    }

    /**
     * Экспортирует рецепт в JSON
     */
    public static JsonObject exportRecipe(RecipeEntry recipe) {
        JsonObject json = new JsonObject();
        
        // Базовая информация
        json.addProperty("recipeId", recipe.getRecipeId());
        json.addProperty("recipeLocation", recipe.getRecipeLocation().toString());
        
        // Требования
        json.addProperty("requiredLearningPoints", recipe.getRequiredLearningPoints());
        json.addProperty("requiredLevel", recipe.getRequiredLevel());
        
        // Требуемые рецепты
        JsonArray requiredRecipes = new JsonArray();
        for (String reqRecipe : recipe.getRequiredRecipes()) {
            requiredRecipes.add(reqRecipe);
        }
        json.add("requiredRecipes", requiredRecipes);
        
        // Теги
        JsonArray tags = new JsonArray();
        for (RecipeTag tag : recipe.getTags()) {
            tags.add(tag.name());
        }
        json.add("tags", tags);
        
        // Остальные поля...
        json.addProperty("description", recipe.getDescription());
        json.addProperty("tooltip", recipe.getTooltip());
        json.addProperty("difficulty", recipe.getDifficulty());
        json.addProperty("category", recipe.getCategory());
        
        return json;
    }

    /**
     * Импортирует рецепт из JSON
     */
    public static RecipeEntry importRecipe(JsonObject json) {
        ResourceLocation recipeLocation = new ResourceLocation(
            json.get("recipeLocation").getAsString());
        RecipeEntry recipe = new RecipeEntry(recipeLocation);
        
        // Загружаем все поля...
        recipe.setRequiredLearningPoints(
            json.get("requiredLearningPoints").getAsInt());
        recipe.setRequiredLevel(
            json.get("requiredLevel").getAsInt());
        
        JsonArray requiredRecipes = json.getAsJsonArray("requiredRecipes");
        for (JsonElement reqRecipe : requiredRecipes) {
            recipe.addRequiredRecipe(reqRecipe.getAsString());
        }
        
        JsonArray tags = json.getAsJsonArray("tags");
        for (JsonElement tag : tags) {
            recipe.addTag(RecipeTag.valueOf(tag.getAsString()));
        }
        
        recipe.setDescription(json.get("description").getAsString());
        recipe.setTooltip(json.get("tooltip").getAsString());
        recipe.setDifficulty(json.get("difficulty").getAsInt());
        recipe.setCategory(json.get("category").getAsString());
        
        return recipe;
    }
}