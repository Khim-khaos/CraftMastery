package com.khimkhaosow.craftmastery.gui; // Убедитесь, что пакет указан правильно

import com.khimkhaosow.craftmastery.util.Reference;
import com.khimkhaosow.craftmastery.gui.ConnectionType; // Добавлен импорт ConnectionType

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class ConnectionRenderer {
    private static final ResourceLocation ARROW_TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/gui/connection_arrow.png");
    private static final float ARROW_SIZE = 8.0f;
    private static final float LINE_WIDTH = 2.0f;

    /**
     * Рисует изогнутую линию между двумя точками с анимацией
     */
    public static void drawCurvedConnection(float startX, float startY, float endX, float endY,
                                          int color, ConnectionType type, float animationProgress) { // ConnectionType должен быть найден
        // Настройка OpenGL
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        // Рисуем кривую Безье
        float controlX = (startX + endX) / 2;
        float controlY1 = startY + (endY - startY) / 4;
        float controlY2 = endY - (endY - startY) / 4;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // Получаем компоненты цвета
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        // Настраиваем ширину линии
        GL11.glLineWidth(LINE_WIDTH);

        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        // Рисуем кривую с анимацией
        for (float t = 0; t <= animationProgress; t += 0.05f) {
            float mt = 1 - t;
            float x = mt * mt * mt * startX +
                     3 * mt * mt * t * controlX +
                     3 * mt * t * t * controlX +
                     t * t * t * endX;

            float y = mt * mt * mt * startY +
                     3 * mt * mt * t * controlY1 +
                     3 * mt * t * t * controlY2 +
                     t * t * t * endY;

            buffer.pos(x, y, 0)
                  .color(red, green, blue, alpha * Math.min(1.0f, (1.0f - Math.abs(t - 0.5f)) * 2))
                  .endVertex();
        }

        tessellator.draw();

        // Рисуем стрелку если линия полностью отрисована
        if (animationProgress >= 1.0f) {
            drawArrow(endX, endY, startX, startY, color, type); // ConnectionType должен быть найден
        }

        // Восстанавливаем состояние OpenGL
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Рисует стрелку на конце линии
     */
    private static void drawArrow(float x, float y, float fromX, float fromY, int color, ConnectionType type) { // ConnectionType должен быть найден
        // Вычисляем угол стрелки
        double angle = Math.atan2(y - fromY, x - fromX);

        // Выбираем текстуру стрелки в зависимости от типа связи
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().getTextureManager().bindTexture(ARROW_TEXTURE);

        // Настраиваем цвет
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        GlStateManager.color(red, green, blue, alpha);

        // Рисуем стрелку
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.rotate((float)Math.toDegrees(angle), 0, 0, 1);

        float u = type == ConnectionType.REQUIREMENT ? 0 : 0.5f; // ConnectionType должен быть найден
        drawTexturedRect(-ARROW_SIZE, -ARROW_SIZE/2,
                        ARROW_SIZE, ARROW_SIZE/2,
                        u, 0, u + 0.5f, 1);

        GlStateManager.popMatrix();
    }

    /**
     * Вспомогательный метод для рисования текстурированного прямоугольника
     */
    private static void drawTexturedRect(float x1, float y1, float x2, float y2,
                                       float u1, float v1, float u2, float v2) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x1, y2, 0).tex(u1, v2).endVertex();
        buffer.pos(x2, y2, 0).tex(u2, v2).endVertex();
        buffer.pos(x2, y1, 0).tex(u2, v1).endVertex();
        buffer.pos(x1, y1, 0).tex(u1, v1).endVertex();
        tessellator.draw();
    }
}