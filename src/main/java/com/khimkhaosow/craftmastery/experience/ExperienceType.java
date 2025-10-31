package com.khimkhaosow.craftmastery.experience;

import net.minecraft.util.text.TextFormatting;

/**
 * Типы источников опыта
 */
public enum ExperienceType {
    BLOCK_MINING("Добыча блоков", TextFormatting.GREEN),
    CRAFTING("Создание предметов", TextFormatting.BLUE),
    MOB_KILL("Убийство мобов", TextFormatting.RED),
    PLAYER_KILL("Убийство игроков", TextFormatting.DARK_RED);

    private final String displayName;
    private final TextFormatting color;

    private ExperienceType(String displayName, TextFormatting color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TextFormatting getColor() {
        return color;
    }

    /**
     * Получает описание типа опыта
     */
    public String getDescription() {
        switch (this) {
            case BLOCK_MINING:
                return "Опыт, получаемый за добычу различных блоков";
            case CRAFTING:
                return "Опыт, получаемый за создание предметов в верстаке";
            case MOB_KILL:
                return "Опыт, получаемый за убийство враждебных мобов";
            case PLAYER_KILL:
                return "Опыт, получаемый за победу над другими игроками";
            default:
                return "Неизвестный тип опыта";
        }
    }
}
