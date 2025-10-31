package com.khimkhaosow.craftmastery.util;

import net.minecraft.client.renderer.GlStateManager;

/**
 * Вспомогательный класс для рендеринга
 */
public class RenderHelper {

    /**
     * Рисует прямоугольник с заданным цветом
     */
    public static void drawRect(int x, int y, int width, int height, int color) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(
            (color >> 16 & 255) / 255.0F,
            (color >> 8 & 255) / 255.0F,
            (color & 255) / 255.0F,
            (color >> 24 & 255) / 255.0F
        );
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION);
        bufferbuilder.pos(x, y + height, 0.0D).endVertex();
        bufferbuilder.pos(x + width, y + height, 0.0D).endVertex();
        bufferbuilder.pos(x + width, y, 0.0D).endVertex();
        bufferbuilder.pos(x, y, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /**
     * Рисует текст с тенью
     */
    public static void drawStringWithShadow(String text, int x, int y, int color) {
        net.minecraft.client.Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, x, y, color);
    }
}
