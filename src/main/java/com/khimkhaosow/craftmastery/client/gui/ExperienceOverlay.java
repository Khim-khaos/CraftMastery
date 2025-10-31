package com.khimkhaosow.craftmastery.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

// 1. Добавь импорты для недостающих классов:
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData; // Импорт PlayerExperienceData
import com.khimkhaosow.craftmastery.experience.ExperienceType;       // Импорт ExperienceType
import com.khimkhaosow.craftmastery.util.Reference; // Импорт Reference, предполагается, что он находится в util

/**
 * Отрисовка интерфейса опыта
 */
public class ExperienceOverlay extends Gui {
    // 2. Теперь Reference должен быть найден, если он существует.
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/gui/experience_bar.png");
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int XP_NUMBER_COLOR = 0x80FF20;

    private final Minecraft mc;
    private long lastExperienceGain;
    private float lastExperienceAmount;
    private String lastExperienceType;
    private int fadeOutTicks;

    public ExperienceOverlay() {
        this.mc = Minecraft.getMinecraft();
        this.fadeOutTicks = 0;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.EXPERIENCE) return;
        if (mc.player == null) return;

        // 3. Теперь PlayerExperienceData должен быть найден
        PlayerExperienceData data = ExperienceManager.getInstance().getPlayerData(mc.player);
        if (data == null) return;

        ScaledResolution scaledRes = new ScaledResolution(mc);
        int width = scaledRes.getScaledWidth();
        int height = scaledRes.getScaledHeight();

        // Отрисовка основного прогресс-бара
        drawExperienceBar(width / 2 - 91, height - 29, data);

        // Отрисовка всплывающего уведомления об опыте
        if (fadeOutTicks > 0) {
            drawExperiencePopup(width / 2, height - 45);
            fadeOutTicks--;
        }
    }

    // 4. Аргумент data теперь имеет правильный тип
    private void drawExperienceBar(int x, int y, PlayerExperienceData data) {
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(GUI_TEXTURE);

        // Фон прогресс-бара
        drawTexturedModalRect(x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT);

        // Заполнение прогресс-бара
        float progress = data.getLevelProgress() / 100.0f;
        int fillWidth = (int)(BAR_WIDTH * progress);
        drawTexturedModalRect(x, y, 0, BAR_HEIGHT, fillWidth, BAR_HEIGHT);

        // Текст уровня
        String levelText = String.format("Уровень %d", data.getLevel());
        drawCenteredString(mc.fontRenderer, levelText, x + BAR_WIDTH/2, y - 10, XP_NUMBER_COLOR);

        // Текст опыта
        String xpText = String.format("%.0f / %.0f XP", data.getCurrentLevelExperience(), data.getExperienceForNextLevel());
        drawCenteredString(mc.fontRenderer, xpText, x + BAR_WIDTH/2, y + BAR_HEIGHT + 2, XP_NUMBER_COLOR);

        GlStateManager.disableBlend();
    }

    private void drawExperiencePopup(int x, int y) {
        if (fadeOutTicks <= 0) return;

        float alpha = Math.min(1.0f, fadeOutTicks / 60.0f);
        int color = ((int)(alpha * 255) << 24) | XP_NUMBER_COLOR;

        String text = String.format("+%.1f XP (%s)", lastExperienceAmount, lastExperienceType);
        drawCenteredString(mc.fontRenderer, text, x, y, color);
    }

    /**
     * Показывает всплывающее уведомление о полученном опыте
     */
    // 5. Аргумент type теперь имеет правильный тип
    public void showExperienceGain(float amount, ExperienceType type) {
        this.lastExperienceAmount = amount;
        this.lastExperienceType = type.getDisplayName(); // Предполагается, что у ExperienceType есть метод getDisplayName()
        this.lastExperienceGain = System.currentTimeMillis();
        this.fadeOutTicks = 60; // 3 секунды при 20 тиках/сек
    }
}
