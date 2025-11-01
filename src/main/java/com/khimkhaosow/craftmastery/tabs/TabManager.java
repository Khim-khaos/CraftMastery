package com.khimkhaosow.craftmastery.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.TabData;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;

/**
 * Менеджер вкладок для организации рецептов
 */
public class TabManager {

    private static TabManager instance;

    // Все вкладки в системе
    private final Map<String, Tab> tabs;

    // Базовая вкладка с ванильными рецептами
    private Tab defaultTab;

    public TabManager() {
        this.tabs = new HashMap<>();
        initializeDefaultTabs();
    }

    public static TabManager getInstance() {
        if (instance == null) {
            instance = new TabManager();
        }
        return instance;
    }

    private void initializeDefaultTabs() {
        // Создаем базовую вкладку с ванильными рецептами
        defaultTab = new Tab("vanilla", "Ванильные рецепты");
        defaultTab.setDescription("Стандартные рецепты Minecraft");
        defaultTab.setColor(TextFormatting.WHITE);
        defaultTab.setRequiredSpecialPoints(0); // Бесплатно
        defaultTab.setResetCost(5);

        tabs.put(defaultTab.getId(), defaultTab);

        // В будущем здесь можно добавить другие предустановленные вкладки
        // Например: "Ученик механик", "Ученик магии" и т.д.

        CraftMastery.logger.info("Initialized {} default tabs", tabs.size());
    }

    /**
     * Создает новую вкладку
     */
    public Tab createTab(String id, String name) {
        if (tabs.containsKey(id)) {
            CraftMastery.logger.warn("Tab with id {} already exists", id);
            return tabs.get(id);
        }

        Tab newTab = new Tab(id, name);
        tabs.put(id, newTab);

        CraftMastery.logger.info("Created new tab: {} ({})", name, id);
        return newTab;
    }

    /**
     * Удаляет вкладку
     */
    public boolean removeTab(String id) {
        Tab tab = tabs.get(id);
        if (tab == null) return false;

        // Нельзя удалить базовую вкладку
        if (tab == defaultTab) {
            CraftMastery.logger.warn("Cannot remove default tab: {}", id);
            return false;
        }

        tabs.remove(id);
        CraftMastery.logger.info("Removed tab: {} ({})", tab.getName(), id);
        return true;
    }

    /**
     * Получает вкладку по ID
     */
    public Tab getTab(String id) {
        return tabs.get(id);
    }

    /**
     * Получает все вкладки
     */
    public List<Tab> getAllTabs() {
        return new ArrayList<>(tabs.values());
    }

    /**
     * Получает вкладки, доступные игроку
     */
    public List<Tab> getAvailableTabs(EntityPlayer player) {
        List<Tab> available = new ArrayList<>();

        RecipeTreeConfigManager config = RecipeTreeConfigManager.getInstance();

        for (Tab tab : tabs.values()) {
            // Проверяем права доступа
            if (tab.requiresPermission()) {
                if (!PermissionManager.getInstance().hasPermission(player, PermissionType.OPEN_INTERFACE)) {
                    continue;
                }
            }

            TabData configTab = config.getTab(tab.getId()).orElse(null);
            if (configTab != null && !TabAvailabilityHelper.isTabUnlocked(player, configTab)) {
                continue;
            }

            available.add(tab);
        }

        return available;
    }

    /**
     * Получает вкладки, изученные игроком
     */
    public List<Tab> getStudiedTabs(EntityPlayer player) {
        if (player == null) return new ArrayList<>();

        List<Tab> studied = new ArrayList<>();
        UUID playerUUID = player.getUniqueID();

        for (Tab tab : tabs.values()) {
            if (tab.isStudiedByPlayer(playerUUID)) {
                studied.add(tab);
            }
        }

        return studied;
    }

    /**
     * Изучает вкладку для игрока
     */
    public boolean studyTab(EntityPlayer player, String tabId) {
        if (player == null) return false;

        Tab tab = getTab(tabId);
        if (tab == null) return false;

        // Проверяем права
        if (!PermissionManager.getInstance().hasPermission(player, PermissionType.LEARN_RECIPES)) {
            return false;
        }

        TabData configTab = RecipeTreeConfigManager.getInstance().getTab(tabId).orElse(null);
        if (configTab != null && !TabAvailabilityHelper.isTabUnlocked(player, configTab)) {
            return false;
        }

        // Получаем данные опыта игрока
        PlayerExperienceData expData = ExperienceManager.getInstance().getPlayerData(player);

        if (!tab.canPlayerStudy(player.getUniqueID(), expData)) {
            return false;
        }

        // Изучаем вкладку
        tab.studyForPlayer(player.getUniqueID(), expData);

        // Разблокируем связанные вкладки
        for (String unlockId : tab.getUnlockingTabs()) {
            Tab unlockTab = getTab(unlockId);
            if (unlockTab != null) {
                // Логика разблокировки - в будущем
            }
        }

        // Блокируем конфликтующие вкладки
        for (String blockId : tab.getBlockingTabs()) {
            Tab blockTab = getTab(blockId);
            if (blockTab != null && blockTab.isStudiedByPlayer(player.getUniqueID())) {
                // Сбрасываем конфликтующую вкладку
                resetTab(player, blockId);
            }
        }

        CraftMastery.logger.info("Player {} studied tab {}", player.getName(), tab.getName());
        return true;
    }

    /**
     * Сбрасывает вкладку для игрока
     */
    public boolean resetTab(EntityPlayer player, String tabId) {
        if (player == null) return false;

        Tab tab = getTab(tabId);
        if (tab == null) return false;

        // Проверяем права
        if (!PermissionManager.getInstance().hasPermission(player, PermissionType.RESET_TABS)) {
            return false;
        }

        // Получаем данные опыта игрока
        PlayerExperienceData expData = ExperienceManager.getInstance().getPlayerData(player);

        tab.resetForPlayer(player.getUniqueID(), expData);

        CraftMastery.logger.info("Player {} reset tab {}", player.getName(), tab.getName());
        return true;
    }

    public void unlockTabForPlayer(EntityPlayer player, String tabId) {
        if (player == null || tabId == null || tabId.trim().isEmpty()) {
            return;
        }

        Tab tab = getTab(tabId);
        if (tab == null) {
            return;
        }

        tab.forceStudyForPlayer(player.getUniqueID());
        CraftMastery.logger.info("Player {} unlocked tab {} via node unlock", player.getName(), tab.getName());
    }

    /**
     * Добавляет рецепт во вкладку
     */
    public boolean addRecipeToTab(String tabId, String recipeId) {
        Tab tab = getTab(tabId);
        if (tab == null) return false;

        tab.addRecipe(recipeId);
        return true;
    }

    /**
     * Удаляет рецепт из вкладки
     */
    public boolean removeRecipeFromTab(String tabId, String recipeId) {
        Tab tab = getTab(tabId);
        if (tab == null) return false;

        tab.removeRecipe(recipeId);
        return true;
    }

    /**
     * Создает связь блокировки между вкладками
     */
    public void createBlockingRelation(String sourceTabId, String targetTabId) {
        Tab sourceTab = getTab(sourceTabId);
        if (sourceTab == null) return;

        sourceTab.addBlockingTab(targetTabId);
    }

    /**
     * Создает связь разблокировки между вкладками
     */
    public void createUnlockingRelation(String sourceTabId, String targetTabId) {
        Tab sourceTab = getTab(sourceTabId);
        if (sourceTab == null) return;

        sourceTab.addUnlockingTab(targetTabId);
    }

    /**
     * Получает базовую вкладку
     */
    public Tab getDefaultTab() {
        return defaultTab;
    }

    /**
     * Проверяет, есть ли вкладка с указанным ID
     */
    public boolean hasTab(String id) {
        return tabs.containsKey(id);
    }

    /**
     * Получает количество вкладок
     */
    public int getTabCount() {
        return tabs.size();
    }

    /**
     * Сбрасывает все данные вкладок (для отладки/тестирования)
     */
    public void resetAllTabs() {
        for (Tab tab : tabs.values()) {
            tab.getRecipeIds().clear();
            tab.getBlockingTabs().clear();
            tab.getUnlockingTabs().clear();
        }

        CraftMastery.logger.info("Reset all tabs data");
    }

    /**
     * Находит вкладку по её имени
     */
    public Tab findTabByName(String name) {
        for (Tab tab : tabs.values()) {
            if (tab.getName().equalsIgnoreCase(name)) {
                return tab;
            }
        }
        return null;
    }
}
