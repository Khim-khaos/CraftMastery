package com.khimkhaosow.craftmastery.experience;

import net.minecraft.util.text.TextFormatting;

/**
 * Типы очков в системе
 */
public enum PointsType {
    LEARNING("Очки изучения", TextFormatting.YELLOW, "Позволяют изучать новые рецепты"),
    SPECIAL("Спец-очки", TextFormatting.LIGHT_PURPLE, "Позволяют открывать новые вкладки"),
    RESET_RECIPES("Очки сброса рецептов", TextFormatting.AQUA, "Позволяют сбрасывать изученные рецепты"),
    RESET_SPECIAL("Очки сброса спец-очков", TextFormatting.DARK_PURPLE, "Позволяют сбрасывать целые вкладки"),
    LEVEL_UP("Очки повышения уровня", TextFormatting.GREEN, "Очки за повышение уровня"),
    RESET("Очки сброса", TextFormatting.DARK_AQUA, "Очки сброса");

    private final String displayName;
    private final TextFormatting color;
    private final String description;

    private PointsType(String displayName, TextFormatting color, String description) {
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
