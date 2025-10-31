package com.khimkhaosow.craftmastery.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.config.ModConfig;

public class CraftMasteryHUD extends GuiIngame {
    private static final ResourceLocation HUD_TEXTURE = new ResourceLocation("craftmastery", "textures/gui/hud.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    private float scale = 1.0f;
    private int xOffset = 0;
    private int yOffset = 0;
    private boolean showExperience = true;
    private boolean showLevel = true;

    public CraftMasteryHUD(Minecraft mc) {
        super(mc);
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.EXPERIENCE) return;

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        PlayerExperienceData data = ExperienceManager.getInstance().getPlayerData(player);
        if (data == null) return;

        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        drawHUD(resolution.getScaledWidth(), resolution.getScaledHeight(), data);
    }

    private void drawHUD(int screenWidth, int screenHeight, PlayerExperienceData data) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0f);

        // Применяем смещение из конфигурации
        int x = (int)((screenWidth / 2 - BAR_WIDTH / 2 + xOffset) / scale);
        int y = (int)((screenHeight - 30 + yOffset) / scale);

        // Рисуем фон полосы опыта
        if (showExperience) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(HUD_TEXTURE);
            drawTexturedModalRect(x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT);

            // Рисуем заполнение полосы опыта
            float progress = data.getLevelProgress() / 100.0f;
            int fillWidth = (int)(BAR_WIDTH * progress);
            drawTexturedModalRect(x, y, 0, BAR_HEIGHT, fillWidth, BAR_HEIGHT);
        }

        // Рисуем текст уровня
        if (showLevel) {
            String levelText = "Уровень " + data.getLevel();
            int textX = (int)((screenWidth / 2 - Minecraft.getMinecraft().fontRenderer.getStringWidth(levelText) / 2 + xOffset) / scale);
            int textY = (int)((screenHeight - 42 + yOffset) / scale);

            // Тень для лучшей читаемости
            Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(levelText, textX, textY, 0xFFFFFF);

            // Показываем прогресс в процентах
            String progressText = String.format("%.1f%%", data.getLevelProgress());
            textX = (int)((screenWidth / 2 - Minecraft.getMinecraft().fontRenderer.getStringWidth(progressText) / 2 + xOffset) / scale);
            textY = (int)((screenHeight - 20 + yOffset) / scale);
            Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(progressText, textX, textY, 0xFFFFFF);
        }

        GlStateManager.popMatrix();
    }

    // Методы настройки HUD
    public void setScale(float scale) {
        this.scale = Math.max(0.5f, Math.min(2.0f, scale));
        ModConfig.hudScale = this.scale;
        ModConfig.save();
    }

    public void setPosition(int x, int y) {
        this.xOffset = x;
        this.yOffset = y;
        ModConfig.hudXOffset = x;
        ModConfig.hudYOffset = y;
        ModConfig.save();
    }

    public void toggleExperience() {
        this.showExperience = !this.showExperience;
        ModConfig.showExperience = this.showExperience;
        ModConfig.save();
    }

    public void toggleLevel() {
        this.showLevel = !this.showLevel;
        ModConfig.showLevel = this.showLevel;
        ModConfig.save();
    }

    // Геттеры для текущих настроек
    public float getScale() { return scale; }
    public int getXOffset() { return xOffset; }
    public int getYOffset() { return yOffset; }
    public boolean isShowingExperience() { return showExperience; }
    public boolean isShowingLevel() { return showLevel; }
}