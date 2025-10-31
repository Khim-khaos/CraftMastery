package com.khimkhaosow.craftmastery.recipe;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.khimkhaosow.craftmastery.CraftMastery;

/**
 * Сохранение и загрузка данных рецептов в JSON
 */
public class RecipeDataManager {

    private static final String RECIPES_DATA_FILE = "craftmastery_recipes.json";
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private final File dataFile;
    private final Map<String, RecipeData> recipeData;

    public RecipeDataManager(File configDir) {
        this.dataFile = new File(configDir, RECIPES_DATA_FILE);
        this.recipeData = new HashMap<>();

        loadData();
    }

    /**
     * Загружает данные рецептов из файла
     */
    public void loadData() {
        if (!dataFile.exists()) {
            CraftMastery.logger.info("Recipe data file not found, creating default");
            saveData();
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            RecipeDataContainer container = GSON.fromJson(reader, RecipeDataContainer.class);
            if (container != null && container.recipes != null) {
                recipeData.clear();
                recipeData.putAll(container.recipes);
                CraftMastery.logger.info("Loaded {} recipe configurations", recipeData.size());
            }
        } catch (IOException e) {
            CraftMastery.logger.error("Error loading recipe data: ", e);
        }
    }

    /**
     * Сохраняет данные рецептов в файл
     */
    public void saveData() {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }

            RecipeDataContainer container = new RecipeDataContainer();
            container.recipes = recipeData;

            try (FileWriter writer = new FileWriter(dataFile)) {
                GSON.toJson(container, writer);
            }

            CraftMastery.logger.info("Saved {} recipe configurations", recipeData.size());
        } catch (IOException e) {
            CraftMastery.logger.error("Error saving recipe data: ", e);
        }
    }

    /**
     * Получает данные рецепта
     */
    public RecipeData getRecipeData(String recipeId) {
        return recipeData.computeIfAbsent(recipeId, key -> new RecipeData());
    }

    /**
     * Сохраняет данные рецепта
     */
    public void setRecipeData(String recipeId, RecipeData data) {
        recipeData.put(recipeId, data);
        saveData();
    }

    /**
     * Контейнер для сериализации
     */
    private static class RecipeDataContainer {
        public Map<String, RecipeData> recipes;
    }

    /**
     * Данные отдельного рецепта
     */
    public static class RecipeData {
        public int requiredLearningPoints = 0;
        public int requiredLevel = 1;
        public String studyMessage = "Рецепт изучен!";
        public int graphX = 0;
        public int graphY = 0;
        public boolean requiresPermission = false;
        public String requiredPermission = "";
        public String[] requiredRecipes = new String[0];
        public String[] blockingRecipes = new String[0];
        public String[] unlockingRecipes = new String[0];
        public String[] tags = new String[0];

        public RecipeData() {
            // Конструктор по умолчанию
        }

        public RecipeData(RecipeEntry entry) {
            this.requiredLearningPoints = entry.getRequiredLearningPoints();
            this.requiredLevel = entry.getRequiredLevel();
            this.studyMessage = entry.getStudyMessage();
            this.graphX = entry.getGraphX();
            this.graphY = entry.getGraphY();
            this.requiresPermission = entry.requiresPermission();
            this.requiredPermission = entry.getRequiredPermission();
            this.requiredRecipes = entry.getRequiredRecipes().toArray(new String[0]);
            this.blockingRecipes = entry.getBlockingRecipes().toArray(new String[0]);
            this.unlockingRecipes = entry.getUnlockingRecipes().toArray(new String[0]);
            this.tags = entry.getTags().stream().map(Enum::name).toArray(String[]::new);
        }

        /**
         * Применяет данные к RecipeEntry
         */
        public void applyTo(RecipeEntry entry) {
            entry.setRequiredLearningPoints(this.requiredLearningPoints);
            entry.setRequiredLevel(this.requiredLevel);
            entry.setStudyMessage(this.studyMessage);
            entry.setGraphX(this.graphX);
            entry.setGraphY(this.graphY);
            entry.setRequiresPermission(this.requiresPermission);
            entry.setRequiredPermission(this.requiredPermission);

            // Очищаем и добавляем новые связи
            entry.getRequiredRecipes().clear();
            for (String recipeId : this.requiredRecipes) {
                entry.addRequiredRecipe(recipeId);
            }

            entry.getBlockingRecipes().clear();
            for (String recipeId : this.blockingRecipes) {
                entry.addBlockingRecipe(recipeId);
            }

            entry.getUnlockingRecipes().clear();
            for (String recipeId : this.unlockingRecipes) {
                entry.addUnlockingRecipe(recipeId);
            }

            // Очищаем и добавляем новые теги
            entry.tags.clear();
            for (String tagName : this.tags) {
                try {
                    RecipeTag tag = RecipeTag.valueOf(tagName);
                    entry.addTag(tag);
                } catch (IllegalArgumentException e) {
                    CraftMastery.logger.warn("Unknown recipe tag: {}", tagName);
                }
            }
        }
    }
}
