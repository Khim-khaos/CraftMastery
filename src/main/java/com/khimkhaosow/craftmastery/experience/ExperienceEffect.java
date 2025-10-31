package com.khimkhaosow.craftmastery.experience;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Класс для управления эффектами получения опыта
 */
public class ExperienceEffect {
    private final EntityPlayer player;
    private final Vec3d position;
    private final float amount;
    private final ExperienceSource source;
    private int lifetime;
    private float alpha;
    private long creationTime;
    
    public enum ExperienceSource {
        CRAFTING("§aCrafting", 0x55FF55),
        MINING("§bMining", 0x55FFFF),
        COMBAT("§cCombat", 0xFF5555),
        DISCOVERY("§eDiscovery", 0xFFFF55),
        QUEST("§dQuest", 0xFF55FF),
        LEVEL_UP("§6Level Up", 0xFFFF00);
        
        private final String displayName;
        private final int color;
        
        ExperienceSource(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }
    }
    
    public ExperienceEffect(EntityPlayer player, Vec3d position, float amount, ExperienceSource source) {
        this.player = player;
        this.position = position;
        this.amount = amount;
        this.source = source;
        this.lifetime = 60; // 3 секунды при 20 тиках
        this.alpha = 1.0f;
    }
    
    /**
     * Обновляет эффект
     */
    public void update() {
        if (lifetime > 0) {
            lifetime--;
            // Уменьшаем прозрачность в конце жизни эффекта
            if (lifetime < 10) {
                alpha = lifetime / 10.0f;
            }
        }
    }
    
    /**
     * Отображает эффект в мире
     */
    public void render(float partialTicks) {
        if (lifetime <= 0) return;
        
        World world = player.world;
        double x = position.x;
        double y = position.y + (60 - lifetime) * 0.016666667F; // Поднимаем текст
        double z = position.z;
        
        // Получаем позицию камеры для правильного рендеринга
        net.minecraft.client.renderer.entity.RenderManager renderManager = 
            net.minecraft.client.Minecraft.getMinecraft().getRenderManager();
            
        // Настраиваем OpenGL
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.translate(
            x - renderManager.viewerPosX,
            y - renderManager.viewerPosY,
            z - renderManager.viewerPosZ);
        net.minecraft.client.renderer.GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
        net.minecraft.client.renderer.GlStateManager.rotate(
            -renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        net.minecraft.client.renderer.GlStateManager.rotate(
            renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        net.minecraft.client.renderer.GlStateManager.scale(-0.025F, -0.025F, 0.025F);
        
        // Включаем прозрачность
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.blendFunc(
            net.minecraft.client.renderer.GlStateManager.SourceFactor.SRC_ALPHA,
            net.minecraft.client.renderer.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            
        // Рисуем текст
        String text = String.format("%s +%d XP", source.getDisplayName(), (int)amount);
        net.minecraft.client.gui.FontRenderer fontRenderer = 
            net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
            
        // Рисуем тень
        fontRenderer.drawString(text, -fontRenderer.getStringWidth(text) / 2 + 1, 1,
            0x000000 | ((int)(alpha * 127) << 24));
            
        // Рисуем основной текст
        fontRenderer.drawString(text, -fontRenderer.getStringWidth(text) / 2,
            0, source.getColor() | ((int)(alpha * 255) << 24));
            
        // Возвращаем состояние OpenGL
        net.minecraft.client.renderer.GlStateManager.disableBlend();
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }
    
    /**
     * Проверяет, жив ли еще эффект
     */
    public boolean isAlive() {
        return lifetime > 0;
    }
    
    /**
     * Получает игрока
     */
    public EntityPlayer getPlayer() {
        return player;
    }
    
    /**
     * Получает позицию эффекта
     */
    public Vec3d getPosition() {
        return position;
    }
    
    /**
     * Получает количество опыта
     */
    public float getAmount() {
        return amount;
    }
    
    /**
     * Получает источник опыта
     */
    public ExperienceSource getSource() {
        return source;
    }
    
    /**
     * Получает прозрачность
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * Устанавливает время создания
     */
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * Получает время создания
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Проверяет, является ли эффект повышением уровня
     */
    public boolean isLevelUp() {
        return source == ExperienceSource.LEVEL_UP;
    }
}