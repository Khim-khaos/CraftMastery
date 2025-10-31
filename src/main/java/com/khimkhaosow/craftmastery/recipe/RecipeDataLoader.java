package com.khimkhaosow.craftmastery.recipe;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.khimkhaosow.craftmastery.CraftMastery;

/**
 * Загрузчик данных рецептов из ресурсов
 */
public class RecipeDataLoader {

    private static final String VANILLA_RECIPES_RESOURCE = "/assets/craftmastery/data/vanilla_recipes.json";
    private static final String VANILLA_RECIPES_FILE = "vanilla_recipes.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Загружает настройки рецептов из ресурсов в конфигурационную директорию
     */
    public static void loadRecipeDefaults(File configDir) {
        File dataDir = new File(configDir, "craftmastery");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File recipesFile = new File(dataDir, VANILLA_RECIPES_FILE);

        // Если файл уже существует, не перезаписываем
        if (recipesFile.exists()) {
            CraftMastery.logger.info("Vanilla recipes configuration already exists");
            return;
        }

        // Копируем файл из ресурсов
        try (InputStream resourceStream = RecipeDataLoader.class.getResourceAsStream(VANILLA_RECIPES_RESOURCE)) {
            if (resourceStream != null) {
                Files.copy(resourceStream, recipesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                CraftMastery.logger.info("Copied vanilla recipes configuration to {}", recipesFile.getPath());
            } else {
                CraftMastery.logger.warn("Could not find vanilla recipes resource file");
                createDefaultRecipesFile(recipesFile);
            }
        } catch (Exception e) {
            CraftMastery.logger.error("Error copying vanilla recipes file: ", e);
            createDefaultRecipesFile(recipesFile);
        }
    }

    private static void createDefaultRecipesFile(File recipesFile) {
        try {
            // Создаем базовый файл с настройками для верстака
            RecipeDataContainer container = new RecipeDataContainer();

            // Добавляем базовые настройки для верстака
            RecipeDataManager.RecipeData craftingTableData = new RecipeDataManager.RecipeData();
            craftingTableData.requiredLearningPoints = 0;
            craftingTableData.requiredLevel = 1;
            craftingTableData.studyMessage = "Вы научились создавать верстак!";
            craftingTableData.graphX = 0;
            craftingTableData.graphY = 0;
            craftingTableData.requiresPermission = false;
            craftingTableData.requiredRecipes = new String[0];
            craftingTableData.blockingRecipes = new String[0];
            craftingTableData.unlockingRecipes = new String[]{"minecraft:stick", "minecraft:wooden_sword"};
            craftingTableData.tags = new String[]{"common", "studied"};

            container.recipes.put("minecraft:crafting_table", craftingTableData);

            // Сохраняем файл
            try (java.io.FileWriter writer = new java.io.FileWriter(recipesFile)) {
                GSON.toJson(container, writer);
            }

            CraftMastery.logger.info("Created default recipes configuration file");
        } catch (Exception e) {
            CraftMastery.logger.error("Error creating default recipes file: ", e);
        }
    }

    /**
     * Применяет настройки рецептов из конфигурации
     */
    public static void applyRecipeSettings(RecipeManager recipeManager, RecipeDataManager dataManager) {
        CraftMastery.logger.info("Applying recipe settings...");

        // Загружаем настройки для всех рецептов
        for (RecipeEntry recipe : recipeManager.getAllRecipes()) {
            RecipeDataManager.RecipeData data = dataManager.getRecipeData(recipe.getRecipeId());
            data.applyTo(recipe);
        }

        CraftMastery.logger.info("Applied settings to {} recipes", recipeManager.getAllRecipes().size());
    }

    /**
     * Контейнер для данных рецептов
     */
    private static class RecipeDataContainer {
        public java.util.Map<String, RecipeDataManager.RecipeData> recipes = new java.util.HashMap<>();
    }
}
