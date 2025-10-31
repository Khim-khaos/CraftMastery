package com.khimkhaosow.craftmastery.permissions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.khimkhaosow.craftmastery.config.ModConfig;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Менеджер прав доступа игроков
 */
public class PermissionManager {

    private static PermissionManager instance;

    // Права по умолчанию для игроков
    private final Map<PermissionType, Boolean> defaultPlayerPermissions;

    // Права по умолчанию для операторов
    private final Map<PermissionType, Boolean> defaultOpPermissions;

    // Права по умолчанию для администраторов (все права)
    private final Map<PermissionType, Boolean> defaultAdminPermissions;

    // Индивидуальные права игроков
    private final Map<UUID, Map<PermissionType, Boolean>> playerPermissions;

    // Права групп
    private final Map<String, Map<PermissionType, Boolean>> groupPermissions;

    private PermissionManager() {
        this.playerPermissions = new HashMap<>();
        this.groupPermissions = new HashMap<>();

        // Инициализация прав по умолчанию
        this.defaultPlayerPermissions = new HashMap<>();
        this.defaultOpPermissions = new HashMap<>();
        this.defaultAdminPermissions = new HashMap<>();

        setupDefaultPermissions();
        applyDefaultsFromConfig();
    }

    public static PermissionManager getInstance() {
        if (instance == null) {
            instance = new PermissionManager();
        }
        return instance;
    }

    private void setupDefaultPermissions() {
        // Права игроков по умолчанию
        defaultPlayerPermissions.put(PermissionType.OPEN_INTERFACE, true);
        defaultPlayerPermissions.put(PermissionType.LEARN_RECIPES, true);
        defaultPlayerPermissions.put(PermissionType.RESET_TABS, false);
        defaultPlayerPermissions.put(PermissionType.MANAGE_RECIPES, false);
        defaultPlayerPermissions.put(PermissionType.MANAGE_TABS, false);
        defaultPlayerPermissions.put(PermissionType.MANAGE_PERMISSIONS, false);
        defaultPlayerPermissions.put(PermissionType.GIVE_POINTS, false);

        // Права операторов (расширенные права)
        defaultOpPermissions.put(PermissionType.OPEN_INTERFACE, true);
        defaultOpPermissions.put(PermissionType.LEARN_RECIPES, true);
        defaultOpPermissions.put(PermissionType.RESET_TABS, true);
        defaultOpPermissions.put(PermissionType.MANAGE_RECIPES, true);
        defaultOpPermissions.put(PermissionType.MANAGE_TABS, true);
        defaultOpPermissions.put(PermissionType.MANAGE_PERMISSIONS, false);
        defaultOpPermissions.put(PermissionType.GIVE_POINTS, true);

        // Права администраторов (все права)
        for (PermissionType permission : PermissionType.values()) {
            defaultAdminPermissions.put(permission, true);
        }
    }

    public Map<PermissionType, Boolean> getDefaultPlayerPermissions() {
        return new HashMap<>(defaultPlayerPermissions);
    }

    public void setDefaultPlayerPermission(PermissionType permission, boolean value) {
        defaultPlayerPermissions.put(permission, value);
    }

    public void applyDefaultsFromConfig() {
        setDefaultPlayerPermission(PermissionType.OPEN_INTERFACE, ModConfig.playersCanOpenInterface);
        setDefaultPlayerPermission(PermissionType.LEARN_RECIPES, ModConfig.playersCanLearnRecipes);
        setDefaultPlayerPermission(PermissionType.RESET_TABS, ModConfig.playersCanResetTabs);
        setDefaultPlayerPermission(PermissionType.MANAGE_RECIPES, ModConfig.playersCanManageRecipes);
        setDefaultPlayerPermission(PermissionType.MANAGE_TABS, ModConfig.playersCanManageTabs);
        setDefaultPlayerPermission(PermissionType.GIVE_POINTS, ModConfig.playersCanGivePoints);
    }

    /**
     * Проверяет, есть ли у игрока указанное право
     */
    public boolean hasPermission(EntityPlayer player, PermissionType permission) {
        if (player == null) return false;

        UUID playerUUID = player.getUniqueID();

        // Проверяем индивидуальные права игрока
        Map<PermissionType, Boolean> playerPerms = playerPermissions.get(playerUUID);
        if (playerPerms != null && playerPerms.containsKey(permission)) {
            return playerPerms.get(permission);
        }

        // Проверяем права группы игрока
        String group = getPlayerGroup(player);
        if (group != null) {
            Map<PermissionType, Boolean> groupPerms = groupPermissions.get(group);
            if (groupPerms != null && groupPerms.containsKey(permission)) {
                return groupPerms.get(permission);
            }
        }

        // Проверяем права по статусу игрока
        if (player.canUseCommand(2, "")) {
            // Администратор (уровень 2+)
            return defaultAdminPermissions.get(permission);
        } else if (player.canUseCommand(1, "")) {
            // Оператор (уровень 1+)
            return defaultOpPermissions.get(permission);
        }

        // Права обычного игрока по умолчанию
        return defaultPlayerPermissions.get(permission);
    }

    /**
     * Устанавливает индивидуальное право для игрока
     */
    public void setPlayerPermission(UUID playerUUID, PermissionType permission, boolean value) {
        playerPermissions.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(permission, value);
    }

    /**
     * Устанавливает право для группы
     */
    public void setGroupPermission(String group, PermissionType permission, boolean value) {
        groupPermissions.computeIfAbsent(group, k -> new HashMap<>()).put(permission, value);
    }

    /**
     * Получает группу игрока (простая реализация)
     */
    private String getPlayerGroup(EntityPlayer player) {
        // В будущем можно интегрировать с системами вроде LuckPerms
        // Пока возвращаем null, чтобы использовать права по статусу
        return null;
    }

    /**
     * Сбрасывает все индивидуальные права игрока
     */
    public void resetPlayerPermissions(UUID playerUUID) {
        playerPermissions.remove(playerUUID);
    }

    /**
     * Сбрасывает все права группы
     */
    public void resetGroupPermissions(String group) {
        groupPermissions.remove(group);
    }

    /**
     * Получает все права игрока
     */
    public Map<PermissionType, Boolean> getPlayerPermissions(EntityPlayer player) {
        if (player == null) return new HashMap<>();

        UUID playerUUID = player.getUniqueID();
        Map<PermissionType, Boolean> permissions = new HashMap<>();

        // Получаем базовые права по статусу
        if (player.canUseCommand(2, "")) {
            permissions.putAll(defaultAdminPermissions);
        } else if (player.canUseCommand(1, "")) {
            permissions.putAll(defaultOpPermissions);
        } else {
            permissions.putAll(defaultPlayerPermissions);
        }

        // Применяем групповые права
        String group = getPlayerGroup(player);
        if (group != null) {
            Map<PermissionType, Boolean> groupPerms = groupPermissions.get(group);
            if (groupPerms != null) {
                permissions.putAll(groupPerms);
            }
        }

        // Применяем индивидуальные права (они имеют наивысший приоритет)
        Map<PermissionType, Boolean> playerPerms = playerPermissions.get(playerUUID);
        if (playerPerms != null) {
            permissions.putAll(playerPerms);
        }

        return permissions;
    }
}
