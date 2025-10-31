import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.gui.NodeState;
import com.khimkhaosow.craftmastery.util.Reference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Улучшенный узел дерева рецептов с анимацией и эффектами
 */
public class AnimatedRecipeNode {
    private static final ResourceLocation NODE_TEXTURES = new ResourceLocation(Reference.MOD_ID, "textures/gui/recipe_nodes.png");
    private static final int TEXTURE_SIZE = 32;
    private static final float HOVER_SCALE = 1.2f;
    private static final float PULSE_SPEED = 0.05f;
    private static final float SELECTION_ROTATION_SPEED = 2.0f;

    private final RecipeEntry recipe;
    private final float x, y;
    private final int width, height;
    private NodeState state;

    private float hoverProgress = 0.0f;
    private float pulseProgress = 0.0f;
    private float selectionRotation = 0.0f;
    private boolean isHovered = false;
    private boolean isSelected = false;

    public AnimatedRecipeNode(RecipeEntry recipe, float x, float y, int width, int height, NodeState state) {
        this.recipe = recipe;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.state = state;
    }

    public void update() {
        // Обновляем анимацию при наведении
        if (isHovered && hoverProgress < 1.0f) {
            hoverProgress += 0.1f;
        } else if (!isHovered && hoverProgress > 0.0f) {
            hoverProgress -= 0.1f;
        }
        hoverProgress = Math.max(0.0f, Math.min(1.0f, hoverProgress));

        // Пульсация для доступных рецептов
        if (state == NodeState.AVAILABLE) {
            pulseProgress += PULSE_SPEED;
            if (pulseProgress >= 1.0f) pulseProgress -= 1.0f;
        }

        // Вращение эффекта выделения
        if (isSelected) {
            selectionRotation += SELECTION_ROTATION_SPEED;
            if (selectionRotation >= 360.0f) selectionRotation -= 360.0f;
        }
    }

    public void draw(Minecraft mc) {
        GlStateManager.pushMatrix();

        // Центрируем узел
        GlStateManager.translate(x, y, 0);

        // Применяем масштаб при наведении
        float scale = 1.0f + (HOVER_SCALE - 1.0f) * hoverProgress;
        GlStateManager.scale(scale, scale, 1.0f);

        // Рисуем фон узла
        drawNodeBackground(mc);

        // Рисуем предмет
        drawRecipeItem(mc);

        // Рисуем эффекты
        drawEffects(mc);

        GlStateManager.popMatrix();
    }

    private void drawNodeBackground(Minecraft mc) {
        mc.getTextureManager().bindTexture(NODE_TEXTURES);
        
        // Выбираем текстуру в зависимости от состояния
        float u = 0, v = 0;
        switch (state) {
            case STUDIED:
                v = 0;
                break;
            case AVAILABLE:
                v = TEXTURE_SIZE;
                break;
            case LOCKED:
                v = TEXTURE_SIZE * 2;
                break;
        }

        // Рисуем фон с учетом пульсации
        float alpha = 1.0f;
        if (state == NodeState.AVAILABLE) {
            alpha = 0.7f + 0.3f * (float)Math.sin(pulseProgress * Math.PI * 2);
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, alpha);
        drawTexturedRect(-width/2, -height/2, width/2, height/2, u, v);
    }

    private void drawRecipeItem(Minecraft mc) {
        ItemStack result = recipe.getRecipeResult();
        if (result.isEmpty()) return;

        RenderItem renderItem = mc.getRenderItem();
        
        GlStateManager.pushMatrix();
        GlStateManager.translate(-8, -8, 0);
        
        RenderHelper.enableGUIStandardItemLighting();
        renderItem.renderItemIntoGUI(result, 0, 0);
        
        // Если рецепт заблокирован, рисуем затемнение
        if (state == NodeState.LOCKED) {
            GlStateManager.disableDepth();
            GlStateManager.colorMask(true, true, true, false);
            drawColoredRect(0, 0, 16, 16, 0x88000000);
            GlStateManager.enableDepth();
            GlStateManager.colorMask(true, true, true, true);
        }
        
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private void drawEffects(Minecraft mc) {
        if (isSelected) {
            // Рисуем вращающееся выделение
            GlStateManager.pushMatrix();
            GlStateManager.rotate(selectionRotation, 0, 0, 1);
            
            mc.getTextureManager().bindTexture(NODE_TEXTURES);
            float u = TEXTURE_SIZE * 2, v = 0;
            float size = width * 1.2f;
            drawTexturedRect(-size/2, -size/2, size/2, size/2, u, v);
            
            GlStateManager.popMatrix();
        }

        if (isHovered && hoverProgress > 0) {
            // Рисуем подсветку при наведении
            float glowSize = width * (1.0f + hoverProgress * 0.2f);
            float alpha = hoverProgress * 0.5f;

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            
            mc.getTextureManager().bindTexture(NODE_TEXTURES);
            float u = TEXTURE_SIZE * 3, v = 0;
            GlStateManager.color(1.0f, 1.0f, 1.0f, alpha);
            drawTexturedRect(-glowSize/2, -glowSize/2, glowSize/2, glowSize/2, u, v);
            
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
    }

    private void drawTexturedRect(float x1, float y1, float x2, float y2, float u, float v) {
        float textureScale = 1.0f / TEXTURE_SIZE;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x1, y2, 0).tex(u * textureScale, (v + TEXTURE_SIZE) * textureScale).endVertex();
        buffer.pos(x2, y2, 0).tex((u + TEXTURE_SIZE) * textureScale, (v + TEXTURE_SIZE) * textureScale).endVertex();
        buffer.pos(x2, y1, 0).tex((u + TEXTURE_SIZE) * textureScale, v * textureScale).endVertex();
        buffer.pos(x1, y1, 0).tex(u * textureScale, v * textureScale).endVertex();
        tessellator.draw();
    }

    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public void setState(NodeState state) {
        this.state = state;
    }

    public RecipeEntry getRecipe() {
        return recipe;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    private void drawColoredRect(float x1, float y1, float x2, float y2, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(r, g, b, a);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buffer.pos(x1, y2, 0.0D).endVertex();
        buffer.pos(x2, y2, 0.0D).endVertex();
        buffer.pos(x2, y1, 0.0D).endVertex();
        buffer.pos(x1, y1, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}