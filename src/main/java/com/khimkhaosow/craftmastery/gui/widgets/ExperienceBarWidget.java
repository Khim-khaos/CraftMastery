package com.khimkhaosow.craftmastery.gui.widgets;

import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Виджет для отображения прогресса опыта
 */
public class ExperienceBarWidget extends Gui {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation("craftmastery", "textures/gui/experience_bar.png");

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final float ANIMATION_SPEED = 0.05f;

    private final Minecraft minecraft;
    private final EntityPlayer player;

    private float currentProgress;
    private float targetProgress;
    private boolean isAnimating;
    private boolean showLevelUp;
    private int levelUpTimer;
    private int lastLevel;

    public ExperienceBarWidget(Minecraft minecraft, EntityPlayer player) {
        this.minecraft = minecraft;
        this.player = player;
        this.currentProgress = 0f;
        this.targetProgress = 0f;
        this.isAnimating = false;
        this.showLevelUp = false;
        this.levelUpTimer = 0;
        this.lastLevel = 0;
    }

    /**
     * Отрисовывает полосу опыта
     */
    public void draw() {
        if (player == null) return;

        ScaledResolution scaledRes = new ScaledResolution(minecraft);
        int screenWidth = scaledRes.getScaledWidth();
        int screenHeight = scaledRes.getScaledHeight();

        // Получаем данные об опыте
        PlayerExperienceData expData = ExperienceManager.getInstance().getPlayerData(player);
        int level = expData.getLevel();
        // --- ИСПРАВЛЕНО: используем getLevelProgress() ---
        float progress = expData.getLevelProgress() / 100.0f; // getLevelProgress() возвращает 0-100, нужно 0.0-1.0

        // Проверяем повышение уровня
        if (level > lastLevel) {
            showLevelUp = true;
            levelUpTimer = 60; // 3 секунды при 20 тиках
            lastLevel = level;
        }

        // Обновляем анимацию
        targetProgress = progress;
        if (currentProgress != targetProgress) {
            isAnimating = true;
            if (currentProgress < targetProgress) {
                currentProgress = Math.min(currentProgress + ANIMATION_SPEED, targetProgress);
            } else {
                currentProgress = Math.max(currentProgress - ANIMATION_SPEED, targetProgress);
            }
        } else {
            isAnimating = false;
        }

        // Позиция полосы опыта
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - 32;

        // Рисуем фон полосы
        minecraft.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.enableBlend();
        drawTexturedModalRect(x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT);

        // Рисуем заполнение полосы с анимацией
        int fillWidth = (int)(BAR_WIDTH * currentProgress);
        if (fillWidth > 0) {
            // Добавляем свечение
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            drawTexturedModalRect(x, y, 0, BAR_HEIGHT, fillWidth, BAR_HEIGHT);

            // Возвращаем нормальное смешивание
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        // Рисуем текст уровня
        String levelText = String.valueOf(level);
        int textWidth = minecraft.fontRenderer.getStringWidth(levelText);
        minecraft.fontRenderer.drawString(levelText,
            x - textWidth - 4, y - 4, 0x80FF20);

        // Рисуем текст прогресса
        String progressText = String.format("%.1f%%", progress * 100);
        minecraft.fontRenderer.drawString(progressText,
            x + BAR_WIDTH + 4, y - 4, 0xFFFFFF);

        // Отображаем анимацию повышения уровня
        if (showLevelUp && levelUpTimer > 0) {
            drawLevelUpAnimation(screenWidth, screenHeight, level);
            levelUpTimer--;
            if (levelUpTimer <= 0) {
                showLevelUp = false;
            }
        }

        GlStateManager.disableBlend();
    }

    /**
     * Отрисовывает анимацию повышения уровня
     */
    private void drawLevelUpAnimation(int screenWidth, int screenHeight, int level) {
        float alpha = Math.min(1.0f, levelUpTimer / 20.0f);
        int color = ((int)(alpha * 255) << 24) | 0x00FFFF;

        String levelUpText = "Уровень повышен!";
        String newLevelText = "Уровень " + level;

        int textWidth1 = minecraft.fontRenderer.getStringWidth(levelUpText);
        int textWidth2 = minecraft.fontRenderer.getStringWidth(newLevelText);

        int y = screenHeight / 2 - 20;

        // Эффект появления/исчезновения
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Рисуем текст с тенью
        minecraft.fontRenderer.drawString(levelUpText,
            screenWidth/2 - textWidth1/2 + 1, y + 1, 0x000000 | ((int)(alpha * 255) << 24));
        minecraft.fontRenderer.drawString(levelUpText,
            screenWidth/2 - textWidth1/2, y, color);

        minecraft.fontRenderer.drawString(newLevelText,
            screenWidth/2 - textWidth2/2 + 1, y + 11, 0x000000 | ((int)(alpha * 255) << 24));
        minecraft.fontRenderer.drawString(newLevelText,
            screenWidth/2 - textWidth2/2, y + 10, color);

        // Рисуем частицы (если есть время в анимации)
        if (levelUpTimer > 30) {
            drawLevelUpParticles(screenWidth, screenHeight);
        }
    }

    /**
     * Отрисовывает частицы для эффекта повышения уровня
     */
    private void drawLevelUpParticles(int screenWidth, int screenHeight) {
        float time = minecraft.world.getTotalWorldTime() + minecraft.getRenderPartialTicks();

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableTexture2D();

        // Рисуем светящиеся частицы
        for (int i = 0; i < 20; i++) {
            float angle = (i * 18 + time * 2) * (float)Math.PI / 180;
            float distance = 30 + (float)Math.sin(time * 0.1f + i) * 10;

            float x = screenWidth/2 + (float)Math.cos(angle) * distance;
            float y = screenHeight/2 - 10 + (float)Math.sin(angle) * distance * 0.5f;

            float particleAlpha = 0.5f + 0.5f * (float)Math.sin(time * 0.2f + i);
            int particleSize = 2;

            drawRect((int)x - particleSize, (int)y - particleSize,
                    (int)x + particleSize, (int)y + particleSize,
                    ((int)(particleAlpha * 255) << 24) | 0x00FFFF);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /**
     * Проверяет, идет ли анимация
     */
    public boolean isAnimating() {
        return isAnimating || showLevelUp;
    }

    /**
     * Устанавливает текущий прогресс
     */
    public void setProgress(float progress) {
        this.targetProgress = progress;
    }

    /**
     * Устанавливает текущий уровень
     */
    public void setLevel(int level) {
        this.lastLevel = level;
    }

    /**
     * Обновляет отображение
     */
    public void updateDisplay() {
        // Обновление отображения, если необходимо
    }

    /**
     * Отрисовывает виджет
     */
    public void render() {
        draw();
    }

    /**
     * Обновляет масштаб при изменении размера экрана
     */
    public void updateScale(int width, int height) {
        // Логика обновления масштаба, если необходимо
    }
}