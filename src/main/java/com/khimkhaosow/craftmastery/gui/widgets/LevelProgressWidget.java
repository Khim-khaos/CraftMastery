package com.khimkhaosow.craftmastery.gui.widgets;

import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.text.TextFormatting;

/**
 * Виджет панели прогресса уровня
 */
public class LevelProgressWidget extends Gui {
    private static final int BAR_HEIGHT = 30;
    private static final int PADDING = 2;

    private final Minecraft minecraft;
    private final FontRenderer fontRenderer;
    private final PlayerExperienceData experienceData;
    private final int width;
    private final int x;
    private final int y;

    public LevelProgressWidget(Minecraft minecraft, PlayerExperienceData experienceData, 
                             int x, int y, int width) {
        this.minecraft = minecraft;
        this.fontRenderer = minecraft.fontRenderer;
        this.experienceData = experienceData;
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void draw() {
        // Черная полоса (фон)
        drawRect(x, y, x + width, y + BAR_HEIGHT, 0xFF000000);

        // Зеленая полоса прогресса
        float progress = experienceData.getLevelProgress() / 100.0f;
        int progressWidth = (int)(width * progress);
        drawRect(x + PADDING, y + PADDING, 
                x + progressWidth - PADDING, y + BAR_HEIGHT - PADDING, 
                0xFF00FF00);

        // Рамка
        drawRect(x, y, x + width, y + 1, 0xFF555555);
        drawRect(x, y + BAR_HEIGHT - 1, x + width, y + BAR_HEIGHT, 0xFF555555);

        // Текст уровня слева
        String levelText = experienceData.getLevel() + " " + 
                         TextFormatting.GREEN + "уровень";
        drawString(fontRenderer, levelText, x + 10, y + 8, 0xFFFFFF);

        // Прогресс по центру
        String progressText = (int)(experienceData.getLevelProgress()) + "/100";
        drawCenteredString(fontRenderer, progressText, 
                         x + width/2, y + 8, 0xFFFFFF);
    }

    public int getHeight() {
        return BAR_HEIGHT;
    }
}