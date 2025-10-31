package com.khimkhaosow.craftmastery.gui.widgets;

import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.experience.PointsType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

/**
 * Виджет левой панели с очками
 */
public class PointsPanelWidget extends Gui {
    private static final ResourceLocation POINTS_TEXTURES = new ResourceLocation("craftmastery", "textures/gui/points.png");
    private static final int ICON_SIZE = 16;
    private static final int ROW_HEIGHT = 25;
    private static final int PADDING = 10;

    private final Minecraft minecraft;
    private final FontRenderer fontRenderer;
    private final PlayerExperienceData experienceData;
    private final int x;
    private final int y;
    private final int height;
    private final int panelWidth;

    public PointsPanelWidget(Minecraft minecraft, PlayerExperienceData experienceData, 
                           int x, int y, int height, int panelWidth) {
        this.minecraft = minecraft;
        this.fontRenderer = minecraft.fontRenderer;
        this.experienceData = experienceData;
        this.x = x;
        this.y = y;
        this.height = height;
        this.panelWidth = panelWidth;
    }

    public void draw() {
        // Фон панели
        drawRect(x, y, x + panelWidth, y + height, 0xCC000000);

        // Заголовок
        drawString(fontRenderer, TextFormatting.GOLD + "Текущие очки", 
                  x + PADDING, y + PADDING, 0xFFFFFF);

        int contentY = y + PADDING * 3;

        // Спец-очки
        drawPointsRow(contentY, "\u2B50", "Спец-очки",
                     experienceData.getPoints(PointsType.SPECIAL), 
                     TextFormatting.YELLOW);
        contentY += ROW_HEIGHT;

        // Очки изучения
        drawPointsRow(contentY, "\u270D", "Изучения",
                     experienceData.getPoints(PointsType.LEARNING), 
                     TextFormatting.GREEN);
        contentY += ROW_HEIGHT;

        // Очки сброса
        drawPointsRow(contentY, "\u21BB", "Сброса",
                     experienceData.getPoints(PointsType.RESET_RECIPES), 
                     TextFormatting.RED);
    }

    private void drawPointsRow(int y, String icon, String label, 
                             int points, TextFormatting color) {
        int rowX = x + PADDING;

        // Иконка
        drawString(fontRenderer, icon, rowX, y, 0xFFFFFF);

        // Название
        drawString(fontRenderer, color + label + ":", 
                  rowX + ICON_SIZE + 5, y, 0xFFFFFF);

        // Значение
        String value = String.valueOf(points);
        TextFormatting valueColor = points > 0 ? TextFormatting.WHITE : TextFormatting.RED;
        int valueX = rowX + panelWidth - PADDING - fontRenderer.getStringWidth(value);
        drawString(fontRenderer, valueColor + value,
                  valueX,
                  y, 0xFFFFFF);
    }

    public int getWidth() {
        return panelWidth;
    }
}