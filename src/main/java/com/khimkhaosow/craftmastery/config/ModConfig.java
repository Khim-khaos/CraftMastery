package com.khimkhaosow.craftmastery.config;

import java.io.File;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.experience.ExperienceType;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Конфигурация мода CraftMastery
 */
public class ModConfig {

    private static Configuration config;

    // Общие настройки
    public static boolean enableExperienceSystem = true;
    public static boolean enablePointsSystem = true;
    public static boolean enableTabSystem = true;
    public static boolean enableRecipeLocking = true;
    public static boolean enableJEIIntegration = true;

    // Настройки опыта
    public static float globalExperienceMultiplier = 1.0f;
    public static boolean enableBlockMiningExperience = true;
    public static boolean enableCraftingExperience = true;
    public static boolean enableMobKillExperience = true;
    public static boolean enablePlayerKillExperience = false; // По умолчанию отключено

    // Настройки множителей опыта по типам
    public static float blockMiningMultiplier = 1.0f;
    public static float craftingMultiplier = 1.0f;
    public static float mobKillMultiplier = 1.0f;
    public static float playerKillMultiplier = 1.0f;

    // Настройки конвертации опыта в очки изучения
    public static float blockMiningToLearningRatio = 0.1f;
    public static float craftingToLearningRatio = 0.5f;
    public static float mobKillToLearningRatio = 0.2f;
    public static float playerKillToLearningRatio = 1.0f;

    // Настройки очков
    public static int startingLearningPoints = 10;
    public static int startingSpecialPoints = 0;
    public static int startingResetRecipesPoints = 5;
    public static int startingResetSpecialPoints = 2;

    // Настройки вкладок
    public static int defaultTabResetCost = 5;
    public static boolean allowTabCreation = true;
    public static boolean allowTabDeletion = false; // По умолчанию только администраторы
    public static boolean enableTabBlockingRelations = true;

    // Настройки HUD
    public static float hudScale = 1.0f;
    public static int hudXOffset = 0;
    public static int hudYOffset = 0;
    public static boolean showExperience = true;
    public static boolean showLevel = true;

    // Настройки интерфейса
    public static boolean showExperienceNotifications = true;
    public static boolean showPointsNotifications = true;
    public static int guiMaxRecipesPerPage = 50;
    public static boolean enableRecipeSearch = true;
    public static boolean enableRecipeFiltering = true;
    public static boolean playersCanOpenInterface = true;
    public static boolean playersCanLearnRecipes = true;
    public static boolean playersCanResetTabs = false;
    public static boolean playersCanManageRecipes = false;
    public static boolean playersCanManageTabs = false;
    public static boolean playersCanGivePoints = false;

    public static void init(FMLPreInitializationEvent event) {
        File configDir = new File(Loader.instance().getConfigDir(), "CraftMastery");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, "craftmastery.cfg");
        config = new Configuration(configFile);

        try {
            config.load();
            loadConfiguration();
        } catch (Exception e) {
            CraftMastery.logger.error("Error loading configuration: ", e);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    private static void loadConfiguration() {
        // Общие настройки
        enableExperienceSystem = config.getBoolean("enableExperienceSystem", "general", true,
            "Включить систему опыта");
        enablePointsSystem = config.getBoolean("enablePointsSystem", "general", true,
            "Включить систему очков");
        enableTabSystem = config.getBoolean("enableTabSystem", "general", true,
            "Включить систему вкладок");
        enableRecipeLocking = config.getBoolean("enableRecipeLocking", "general", true,
            "Блокировать рецепты по умолчанию");
        enableJEIIntegration = config.getBoolean("enableJEIIntegration", "general", true,
            "Включить интеграцию с JEI");

        // Настройки опыта
        globalExperienceMultiplier = (float) config.get("experience", "globalMultiplier", 1.0f,
            "Глобальный множитель опыта (0.1-10.0)").getDouble();

        enableBlockMiningExperience = config.getBoolean("enableBlockMiningExperience", "experience", true,
            "Включить опыт за добычу блоков");
        enableCraftingExperience = config.getBoolean("enableCraftingExperience", "experience", true,
            "Включить опыт за крафт");
        enableMobKillExperience = config.getBoolean("enableMobKillExperience", "experience", true,
            "Включить опыт за убийство мобов");
        enablePlayerKillExperience = config.getBoolean("enablePlayerKillExperience", "experience", false,
            "Включить опыт за убийство игроков (PVP)");

        // Множители опыта по типам
        blockMiningMultiplier = (float) config.get("experience", "blockMiningMultiplier", 1.0f,
            "Множитель опыта за добычу блоков").getDouble();
        craftingMultiplier = (float) config.get("experience", "craftingMultiplier", 1.0f,
            "Множитель опыта за крафт").getDouble();
        mobKillMultiplier = (float) config.get("experience", "mobKillMultiplier", 1.0f,
            "Множитель опыта за убийство мобов").getDouble();
        playerKillMultiplier = (float) config.get("experience", "playerKillMultiplier", 1.0f,
            "Множитель опыта за убийство игроков").getDouble();

        // Конвертация опыта в очки изучения
        blockMiningToLearningRatio = (float) config.get("experience", "blockMiningToLearningRatio", 0.1f,
            "Конвертация опыта добычи в очки изучения").getDouble();
        craftingToLearningRatio = (float) config.get("experience", "craftingToLearningRatio", 0.5f,
            "Конвертация опыта крафта в очки изучения").getDouble();
        mobKillToLearningRatio = (float) config.get("experience", "mobKillToLearningRatio", 0.2f,
            "Конвертация опыта убийства мобов в очки изучения").getDouble();
        playerKillToLearningRatio = (float) config.get("experience", "playerKillToLearningRatio", 1.0f,
            "Конвертация опыта убийства игроков в очки изучения").getDouble();

        // Стартовые очки
        startingLearningPoints = config.getInt("startingLearningPoints", "points", 10, 0, 1000,
            "Стартовые очки изучения");
        startingSpecialPoints = config.getInt("startingSpecialPoints", "points", 0, 0, 100,
            "Стартовые спец-очки");
        startingResetRecipesPoints = config.getInt("startingResetRecipesPoints", "points", 5, 0, 100,
            "Стартовые очки сброса рецептов");
        startingResetSpecialPoints = config.getInt("startingResetSpecialPoints", "points", 2, 0, 50,
            "Стартовые очки сброса спец-очков");

        // Настройки вкладок
        defaultTabResetCost = config.getInt("defaultTabResetCost", "tabs", 5, 0, 1000,
            "Стоимость сброса вкладки по умолчанию");
        allowTabCreation = config.getBoolean("allowTabCreation", "tabs", true,
            "Разрешить создание вкладок");
        allowTabDeletion = config.getBoolean("allowTabDeletion", "tabs", false,
            "Разрешить удаление вкладок (только администраторы)");
        enableTabBlockingRelations = config.getBoolean("enableTabBlockingRelations", "tabs", true,
            "Включить связи блокировки между вкладками");

        // Настройки интерфейса
        showExperienceNotifications = config.getBoolean("showExperienceNotifications", "gui", true,
            "Показывать уведомления об опыте");
        showPointsNotifications = config.getBoolean("showPointsNotifications", "gui", true,
            "Показывать уведомления об очках");
        guiMaxRecipesPerPage = config.getInt("guiMaxRecipesPerPage", "gui", 50, 10, 200,
            "Максимальное количество рецептов на странице");
        enableRecipeSearch = config.getBoolean("enableRecipeSearch", "gui", true,
            "Включить поиск рецептов");
        enableRecipeFiltering = config.getBoolean("enableRecipeFiltering", "gui", true,
            "Включить фильтрацию рецептов");

        // Настройки HUD
        hudScale = (float) config.get("hud", "hudScale", 1.0f,
            "Масштаб HUD (0.5-2.0)").getDouble();
        hudXOffset = config.getInt("hudXOffset", "hud", 0, -1000, 1000,
            "Смещение HUD по X");
        hudYOffset = config.getInt("hudYOffset", "hud", 0, -1000, 1000,
            "Смещение HUD по Y");
        showExperience = config.getBoolean("showExperience", "hud", true,
            "Показывать полосу опыта");
        showLevel = config.getBoolean("showLevel", "hud", true,
            "Показывать уровень и прогресс");
        playersCanOpenInterface = config.getBoolean("playersCanOpenInterface", "permissions", true,
            "Игроки могут открывать интерфейс по умолчанию");
        playersCanLearnRecipes = config.getBoolean("playersCanLearnRecipes", "permissions", true,
            "Игроки могут изучать рецепты по умолчанию");
        playersCanResetTabs = config.getBoolean("playersCanResetTabs", "permissions", false,
            "Игроки могут сбрасывать вкладки по умолчанию");
        playersCanManageRecipes = config.getBoolean("playersCanManageRecipes", "permissions", false,
            "Игроки могут управлять рецептами по умолчанию");
        playersCanManageTabs = config.getBoolean("playersCanManageTabs", "permissions", false,
            "Игроки могут управлять вкладками по умолчанию");
        playersCanGivePoints = config.getBoolean("playersCanGivePoints", "permissions", false,
            "Игроки могут выдавать очки по умолчанию");

        // Валидация значений
        validateConfiguration();
    }

    private static void validateConfiguration() {
        // Валидация множителей
        globalExperienceMultiplier = Math.max(0.1f, Math.min(10.0f, globalExperienceMultiplier));
        blockMiningMultiplier = Math.max(0.0f, blockMiningMultiplier);
        craftingMultiplier = Math.max(0.0f, craftingMultiplier);
        mobKillMultiplier = Math.max(0.0f, mobKillMultiplier);
        playerKillMultiplier = Math.max(0.0f, playerKillMultiplier);

        // Валидация коэффициентов конвертации
        blockMiningToLearningRatio = Math.max(0.0f, blockMiningToLearningRatio);
        craftingToLearningRatio = Math.max(0.0f, craftingToLearningRatio);
        mobKillToLearningRatio = Math.max(0.0f, mobKillToLearningRatio);
        playerKillToLearningRatio = Math.max(0.0f, playerKillToLearningRatio);

        // Валидация стартовых значений
        startingLearningPoints = Math.max(0, startingLearningPoints);
        startingSpecialPoints = Math.max(0, startingSpecialPoints);
        startingResetRecipesPoints = Math.max(0, startingResetRecipesPoints);
        startingResetSpecialPoints = Math.max(0, startingResetSpecialPoints);

        // Валидация настроек HUD
        hudScale = Math.max(0.5f, Math.min(2.0f, hudScale));
        hudXOffset = Math.max(-1000, Math.min(1000, hudXOffset));
        hudYOffset = Math.max(-1000, Math.min(1000, hudYOffset));
    }

    /**
     * Сохраняет конфигурацию
     */
    public static void save() {
        if (config != null && config.hasChanged()) {
            config.save();
        }
    }

    public static void applyPermissionSettings(boolean openInterface,
                                                boolean learnRecipes,
                                                boolean resetTabs,
                                                boolean manageRecipes,
                                                boolean manageTabs,
                                                boolean givePoints) {
        playersCanOpenInterface = openInterface;
        playersCanLearnRecipes = learnRecipes;
        playersCanResetTabs = resetTabs;
        playersCanManageRecipes = manageRecipes;
        playersCanManageTabs = manageTabs;
        playersCanGivePoints = givePoints;

        if (config != null) {
            config.get("permissions", "playersCanOpenInterface", true).set(openInterface);
            config.get("permissions", "playersCanLearnRecipes", true).set(learnRecipes);
            config.get("permissions", "playersCanResetTabs", false).set(resetTabs);
            config.get("permissions", "playersCanManageRecipes", false).set(manageRecipes);
            config.get("permissions", "playersCanManageTabs", false).set(manageTabs);
            config.get("permissions", "playersCanGivePoints", false).set(givePoints);
            config.save();
        }
    }

    public static void applyGuiSettings(boolean showExpNotices, boolean showPointsNotices,
                                        boolean showExpHud, boolean showLvlHud,
                                        float newHudScale, int newHudXOffset, int newHudYOffset) {
        showExperienceNotifications = showExpNotices;
        showPointsNotifications = showPointsNotices;
        showExperience = showExpHud;
        showLevel = showLvlHud;
        hudScale = newHudScale;
        hudXOffset = newHudXOffset;
        hudYOffset = newHudYOffset;
        if (config != null) {
            config.get("gui", "showExperienceNotifications", true).set(showExpNotices);
            config.get("gui", "showPointsNotifications", true).set(showPointsNotices);
            config.get("gui", "enableRecipeSearch", enableRecipeSearch).set(enableRecipeSearch); // ensure category touched
            config.get("gui", "enableRecipeFiltering", enableRecipeFiltering).set(enableRecipeFiltering);
            config.get("hud", "hudScale", 1.0f).set(newHudScale);
            config.get("hud", "hudXOffset", 0).set(newHudXOffset);
            config.get("hud", "hudYOffset", 0).set(newHudYOffset);
            config.get("hud", "showExperience", true).set(showExpHud);
            config.get("hud", "showLevel", true).set(showLvlHud);
            config.save();
        }
    }

    public static void applyExperienceSettings(float global,
                                               float blockMultiplierValue,
                                               float craftingMultiplierValue,
                                               float mobMultiplierValue,
                                               float playerMultiplierValue,
                                               float blockConversionValue,
                                               float craftingConversionValue,
                                               float mobConversionValue,
                                               float playerConversionValue) {
        globalExperienceMultiplier = global;
        blockMiningMultiplier = blockMultiplierValue;
        craftingMultiplier = craftingMultiplierValue;
        mobKillMultiplier = mobMultiplierValue;
        playerKillMultiplier = playerMultiplierValue;

        blockMiningToLearningRatio = blockConversionValue;
        craftingToLearningRatio = craftingConversionValue;
        mobKillToLearningRatio = mobConversionValue;
        playerKillToLearningRatio = playerConversionValue;

        validateConfiguration();
        if (config != null) {
            config.get("experience", "globalMultiplier", 1.0f).set(globalExperienceMultiplier);
            config.get("experience", "blockMiningMultiplier", 1.0f).set(blockMiningMultiplier);
            config.get("experience", "craftingMultiplier", 1.0f).set(craftingMultiplier);
            config.get("experience", "mobKillMultiplier", 1.0f).set(mobKillMultiplier);
            config.get("experience", "playerKillMultiplier", 1.0f).set(playerKillMultiplier);
            config.get("experience", "blockMiningToLearningRatio", 0.1f).set(blockMiningToLearningRatio);
            config.get("experience", "craftingToLearningRatio", 0.5f).set(craftingToLearningRatio);
            config.get("experience", "mobKillToLearningRatio", 0.2f).set(mobKillToLearningRatio);
            config.get("experience", "playerKillToLearningRatio", 1.0f).set(playerKillToLearningRatio);
            config.save();
        }
    }

    /**
     * Получает категорию конфигурации для отображения
     */
    public static String getConfigCategory(String category) {
        switch (category.toLowerCase()) {
            case "general": return "Общие настройки";
            case "experience": return "Настройки опыта";
            case "points": return "Настройки очков";
            case "tabs": return "Настройки вкладок";
            case "gui": return "Настройки интерфейса";
            case "permissions": return "Права доступа";
            default: return category;
        }
    }

    /**
     * Синхронизирует настройки с менеджером опыта
     */
    public static void syncWithExperienceManager(com.khimkhaosow.craftmastery.experience.ExperienceManager expManager) {
        if (!enableExperienceSystem) return;

        // Устанавливаем глобальный множитель
        for (ExperienceType type : ExperienceType.values()) {
            float multiplier = getMultiplierForType(type);
            expManager.setGlobalMultiplier(type, multiplier);
        }
    }

    private static float getMultiplierForType(ExperienceType type) {
        switch (type) {
            case BLOCK_MINING:
                return globalExperienceMultiplier * blockMiningMultiplier;
            case CRAFTING:
                return globalExperienceMultiplier * craftingMultiplier;
            case MOB_KILL:
                return globalExperienceMultiplier * mobKillMultiplier;
            case PLAYER_KILL:
                return globalExperienceMultiplier * playerKillMultiplier;
            default:
                return globalExperienceMultiplier;
        }
    }
}
