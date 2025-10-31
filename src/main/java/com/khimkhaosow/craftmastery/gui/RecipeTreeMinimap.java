package com.khimkhaosow.craftmastery.gui;

// 1. Импорты для Minecraft/Forge
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
// 2. Импорты для Java
import java.util.List;
// 3. Импорты для других компонентов мода
import com.khimkhaosow.craftmastery.util.Reference; // Добавлен импорт Reference
import com.khimkhaosow.craftmastery.gui.RecipeNode; // Добавлен импорт RecipeNode
// 4. Импорт для GL11
import org.lwjgl.opengl.GL11; // Добавлен импорт GL11

/**
 * Рисует миникарту для навигации по дереву рецептов
 */
public class RecipeTreeMinimap {
    // Reference.MOD_ID должен быть найден
    private static final ResourceLocation MINIMAP_TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/gui/minimap_background.png");
    private static final int MINIMAP_SIZE = 100;
    private static final float MINIMAP_SCALE = 0.1f;

    private final GuiScreen parent;
    private final int x, y;
    private float viewportX, viewportY;
    private float viewportScale;

    public RecipeTreeMinimap(GuiScreen parent, int x, int y) {
        this.parent = parent;
        this.x = x;
        this.y = y;
    }

    public void draw(List<RecipeNode> nodes, float treeOffsetX, float treeOffsetY, float treeScale) {
        // Сохраняем состояние
        GlStateManager.pushMatrix();

        // Рисуем фон миникарты
        parent.mc.getTextureManager().bindTexture(MINIMAP_TEXTURE);
        GuiScreen.drawModalRectWithCustomSizedTexture(x, y, 0, 0, MINIMAP_SIZE, MINIMAP_SIZE, MINIMAP_SIZE, MINIMAP_SIZE);

        // Настраиваем отображение в миникарте
        // GL11.GL_SCISSOR_TEST должен быть найден
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        // Конвертируем координаты в экранные для обрезки
        int scale = new ScaledResolution(parent.mc).getScaleFactor();
        // GL11.glScissor должен быть найден
        GL11.glScissor(x * scale, parent.height - (y + MINIMAP_SIZE) * scale,
                      MINIMAP_SIZE * scale, MINIMAP_SIZE * scale);

        // Масштабируем и смещаем для отображения узлов
        GlStateManager.translate(x + MINIMAP_SIZE/2, y + MINIMAP_SIZE/2, 0);
        GlStateManager.scale(MINIMAP_SCALE, MINIMAP_SCALE, 1.0f);

        // Рисуем узлы
        for (RecipeNode node : nodes) { // RecipeNode должен быть найден
            drawMinimapNode(node);
        }

        // Рисуем видимую область
        drawViewport(treeOffsetX, treeOffsetY, treeScale);

        // GL11.glDisable должен быть найден
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.popMatrix();
    }

    private void drawMinimapNode(RecipeNode node) { // RecipeNode должен быть найден
        int color;
        switch (node.state) { // NodeState, предположительно, внутри RecipeNode или импортирован отдельно
            case STUDIED:
                color = 0xFF00FF00;
                break;
            case AVAILABLE:
                color = 0xFFFFFF00;
                break;
            default:
                color = 0xFF666666;
                break;
        }

        // Рисуем точку для узла
        int nodeSize = 2;
        GuiScreen.drawRect(
            (int)(node.x - nodeSize),
            (int)(node.y - nodeSize),
            (int)(node.x + nodeSize),
            (int)(node.y + nodeSize),
            color
        );
    }

    private void drawViewport(float treeOffsetX, float treeOffsetY, float treeScale) {
        // Вычисляем размер и положение видимой области
        float vpWidth = parent.width / (treeScale / MINIMAP_SCALE);
        float vpHeight = parent.height / (treeScale / MINIMAP_SCALE);

        float vpX = -treeOffsetX / treeScale;
        float vpY = -treeOffsetY / treeScale;

        // Рисуем прямоугольник видимой области
        GlStateManager.enableBlend();
        GuiScreen.drawRect(
            (int)(vpX - vpWidth/2),
            (int)(vpY - vpHeight/2),
            (int)(vpX + vpWidth/2),
            (int)(vpY + vpHeight/2),
            0x33FFFFFF // Полупрозрачный белый
        );

        // Рамка видимой области
        GuiScreen.drawRect(
            (int)(vpX - vpWidth/2) - 1,
            (int)(vpY - vpHeight/2) - 1,
            (int)(vpX + vpWidth/2) + 1,
            (int)(vpY - vpHeight/2),
            0xFFFFFFFF
        );
        GuiScreen.drawRect(
            (int)(vpX - vpWidth/2) - 1,
            (int)(vpY + vpHeight/2),
            (int)(vpX + vpWidth/2) + 1,
            (int)(vpY + vpHeight/2) + 1,
            0xFFFFFFFF
        );
        GuiScreen.drawRect(
            (int)(vpX - vpWidth/2) - 1,
            (int)(vpY - vpHeight/2),
            (int)(vpX - vpWidth/2),
            (int)(vpY + vpHeight/2),
            0xFFFFFFFF
        );
        GuiScreen.drawRect(
            (int)(vpX + vpWidth/2),
            (int)(vpY - vpHeight/2),
            (int)(vpX + vpWidth/2) + 1,
            (int)(vpY + vpHeight/2),
            0xFFFFFFFF
        );
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + MINIMAP_SIZE &&
               mouseY >= y && mouseY <= y + MINIMAP_SIZE;
    }

    public void handleMouseDrag(int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return;

        // Преобразуем координаты мыши в координаты дерева
        float treeX = (mouseX - x - MINIMAP_SIZE/2) / MINIMAP_SCALE;
        float treeY = (mouseY - y - MINIMAP_SIZE/2) / MINIMAP_SCALE;

        // Обновляем позицию просмотра
        viewportX = treeX;
        viewportY = treeY;
    }

    public float getViewportX() { return viewportX; }
    public float getViewportY() { return viewportY; }
}