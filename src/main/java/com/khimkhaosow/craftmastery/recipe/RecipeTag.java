package com.khimkhaosow.craftmastery.recipe;

import net.minecraft.util.text.TextFormatting;

/**
 * Теги рецептов для фильтрации и отображения
 */
public enum RecipeTag {
    COMMON("Обычные", TextFormatting.WHITE, "Обычные рецепты, доступные всем"),
    TECHNICAL("Технические", TextFormatting.AQUA, "Рецепты, связанные с техникой"),
    MAGICAL("Магические", TextFormatting.LIGHT_PURPLE, "Рецепты, связанные с магией"),
    MAGICAL_TECHNICAL("Магико-технические", TextFormatting.AQUA, "Рецепты, сочетающие магию и технологии"),
    STUDIED("Изученные", TextFormatting.GREEN, "Рецепты, которые игрок уже изучил"),
    NOT_STUDIED("Неизученные", TextFormatting.GRAY, "Рецепты, которые игрок еще не изучил"),
    // --- ДОБАВЛЕНЫ НОВЫЕ ТЕГИ ---
    LEVEL_UP("Повышение уровня", TextFormatting.GOLD, "Специальный тег для эффектов повышения уровня"),
    RESET("Сброс", TextFormatting.RED, "Рецепты, связанные сбросом");

    private final String displayName;
    private final TextFormatting color;
    private final String description;

    private RecipeTag(String displayName, TextFormatting color, String description) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TextFormatting getColor() {
        return color;
    }

    public String getDescription() {
        return description;
    }
}