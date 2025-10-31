package com.khimkhaosow.craftmastery.permissions;

public enum PermissionType {
    OPEN_INTERFACE("Открытие интерфейса мода"),
    LEARN_RECIPES("Изучение рецептов"),
    RESET_TABS("Сброс вкладок"),
    MANAGE_RECIPES("Управление рецептами"),
    CREATE_RECIPES("Создание рецептов"),
    MANAGE_TABS("Управление вкладками"),
    MANAGE_PERMISSIONS("Управление правами доступа"),
    ADMIN_SETTINGS("Админ настройки"),
    GIVE_POINTS("Выдача очков и опыта");

    private final String description;

    private PermissionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
