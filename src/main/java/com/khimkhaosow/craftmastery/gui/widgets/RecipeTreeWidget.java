package com.khimkhaosow.craftmastery.gui.widgets;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.Availability;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.NodeData;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.Position;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.TabData;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.Unlocks;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.gui.RecipeTreeEditorScreen;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import com.khimkhaosow.craftmastery.util.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Виджет для отображения и взаимодействия с деревом рецептов
 */
public class RecipeTreeWidget extends Gui {

    private static final ResourceLocation NODE_TEXTURE_UNLOCKED = new ResourceLocation("craftmastery", "textures/gui/node_unlocked.png");
    private static final ResourceLocation NODE_TEXTURE_STUDIED = new ResourceLocation("craftmastery", "textures/gui/node_studied.png");
    private static final ResourceLocation FALLBACK_ICON = TextureMap.LOCATION_MISSING_TEXTURE;
    private static final int NODE_SIZE = 32;
    private static final int NODE_TEXTURE_SIZE = 64;
    private static final int NODE_PADDING = 20;
    private static final int CONNECTOR_WIDTH = 2;
    private static final int CONNECTOR_COLOR_STUDIED = 0xFF8CE5A4;
    private static final int CONNECTOR_COLOR_LOCKED = 0xFFB7BEC7;
    private static final int PIN_RADIUS = 4;
    private static final int PIN_OFFSET = NODE_SIZE / 2 + 6;
    private static final int PIN_INACTIVE_COLOR = 0xCCAAAAAA;
    private static final int PIN_ACTIVE_COLOR = 0xFF4CAF50;
    private static final int PIN_HOVER_COLOR = 0xFFFFC107;
    private static final int GL_LINES = 0x0001;
    private static final int GL_LINE_STRIP = 0x0003;
    private static final int GL_TRIANGLES = 0x0004;

    private final Minecraft minecraft;
    private final EntityPlayer player;
    private final PlayerExperienceData experienceData;
    private final FontRenderer fontRenderer;

    private Map<String, Node> nodes;
    private Node selectedNode;
    private String selectedNodeId;
    private float lastScale = 1.0f;
    private boolean isDragging = false;
    private int lastMouseX, lastMouseY;
    private boolean editingEnabled = false;
    private Node draggingNode;
    private float dragStartWorldX;
    private float dragStartWorldY;
    private int dragStartNodeX;
    private int dragStartNodeY;
    private boolean nodeMovedDuringDrag;
    private boolean linkActive = false;
    private Node linkSourceNode;
    private float linkCursorX;
    private float linkCursorY;
    private Node linkHoverTarget;
    private String activeTabId;

    private static class Node {
        final String id;
        final NodeData data;
        final RecipeEntry recipe;
        final boolean hasRecipe;
        final boolean recipeMissing;
        final ResourceLocation customIcon;
        final List<Node> children = new ArrayList<>();
        final List<Node> parents = new ArrayList<>();
        final String displayName;
        final String nodeTitle;
        final int studyCost;
        final String category;

        Node(String id, NodeData data, RecipeEntry recipe) {
            this.id = id;
            this.data = data;
            this.recipe = recipe;
            this.hasRecipe = recipe != null;
            String rawRecipeId = data != null ? data.recipeId : null;
            this.recipeMissing = !hasRecipe && rawRecipeId != null && !rawRecipeId.trim().isEmpty();

            ResourceLocation resolvedIcon = parseResource(data != null ? data.customIcon : null);
            String configuredTitle = data != null ? data.nodeTitle : null;
            String configuredName = data != null ? data.displayName : null;
            int configuredCost = data != null ? data.studyCost : 0;

            if (configuredName != null && !configuredName.trim().isEmpty()) {
                this.displayName = configuredName;
            } else if (hasRecipe) {
                this.displayName = recipe.getDisplayName();
            } else if (data != null && data.id != null && !data.id.trim().isEmpty()) {
                this.displayName = data.id;
            } else {
                this.displayName = "<узел>";
            }

            this.nodeTitle = configuredTitle != null ? configuredTitle : (hasRecipe ? recipe.getNodeTitle() : "");

            int resolvedCost = configuredCost > 0
                    ? configuredCost
                    : (hasRecipe ? recipe.getRequiredLearningPoints() : 0);
            this.studyCost = Math.max(0, resolvedCost);

            if (hasRecipe) {
                if (configuredCost > 0) {
                    recipe.setRequiredLearningPoints(this.studyCost);
                }
                if (resolvedIcon != null) {
                    recipe.setCustomIcon(resolvedIcon);
                } else {
                    resolvedIcon = recipe.getCustomIcon();
                }
                if (this.nodeTitle != null && !this.nodeTitle.isEmpty()) {
                    recipe.setNodeTitle(this.nodeTitle);
                }
            }

            this.customIcon = resolvedIcon;
            this.category = hasRecipe ? recipe.getCategory() : (data != null ? data.tab : null);
        }

        int getX() {
            return data != null && data.position != null ? data.position.x : 0;
        }

        int getY() {
            return data != null && data.position != null ? data.position.y : 0;
        }
    }

    public RecipeTreeWidget(Minecraft minecraft, EntityPlayer player, PlayerExperienceData experienceData) {
        this.minecraft = minecraft;
        this.player = player;
        this.experienceData = experienceData;
        this.fontRenderer = minecraft.fontRenderer;
        this.nodes = new LinkedHashMap<>();
        buildTree();
    }

    private void buildTree() {
        nodes = new LinkedHashMap<>();

        RecipeTreeConfigManager configManager = RecipeTreeConfigManager.getInstance();
        List<NodeData> configuredNodes = configManager.getNodes();
        configuredNodes = normalizeNodes(configuredNodes);
        if (configuredNodes.isEmpty()) {
            this.activeTabId = null;
            return;
        }

        Map<String, NodeData> dataById = new LinkedHashMap<>();
        for (NodeData data : configuredNodes) {
            if (data == null || data.id == null || data.id.trim().isEmpty()) {
                continue;
            }
            dataById.put(data.id, data);
        }

        for (Map.Entry<String, NodeData> entry : dataById.entrySet()) {
            NodeData data = entry.getValue();
            RecipeEntry recipe = resolveRecipe(data);
            if (recipe == null && data != null && data.recipeId != null && !data.recipeId.trim().isEmpty()) {
                CraftMastery.logger.warn("Recipe tree node '{}' references missing recipe '{}'", data.id, data.recipeId);
            }
            nodes.put(entry.getKey(), new Node(entry.getKey(), data, recipe));
        }

        for (Node node : nodes.values()) {
            NodeData data = node.data;
            if (data == null) {
                continue;
            }

            if (data.availability != null && data.availability.requiredNodes != null) {
                for (String requiredId : data.availability.requiredNodes) {
                    Node parent = nodes.get(requiredId);
                    if (parent != null && !node.parents.contains(parent)) {
                        node.parents.add(parent);
                        if (!parent.children.contains(node)) {
                            parent.children.add(node);
                        }
                    }
                }
            }

            if (data.unlocks != null && data.unlocks.nodes != null) {
                for (String childId : data.unlocks.nodes) {
                    Node child = nodes.get(childId);
                    if (child != null && !node.children.contains(child)) {
                        node.children.add(child);
                        if (!child.parents.contains(node)) {
                            child.parents.add(node);
                        }
                    }
                }
            }
        }

        if (activeTabId == null) {
            activeTabId = determineInitialTabId(configuredNodes);
        }

        if (selectedNodeId != null) {
            selectedNode = nodes.get(selectedNodeId);
            if (!isNodeVisible(selectedNode)) {
                selectedNode = null;
                selectedNodeId = null;
            }
        } else {
            selectedNode = null;
        }
    }

    private RecipeEntry resolveRecipe(NodeData data) {
        if (data == null || data.recipeId == null || data.recipeId.trim().isEmpty()) {
            return null;
        }

        RecipeEntry entry = RecipeManager.getInstance().getRecipe(data.recipeId);
        if (entry != null) {
            return entry;
        }

        ResourceLocation location = parseResource(data.recipeId);
        if (location != null) {
            entry = RecipeManager.getInstance().getRecipe(location.toString());
        }

        return entry;
    }

    public void draw(int width, int height, float offsetX, float offsetY, float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(width/2 + offsetX, height/2 + offsetY, 0);
        GlStateManager.scale(scale, scale, 1.0f);
        
        // Сначала рисуем соединительные линии
        for (Node node : nodes.values()) {
            if (!isNodeVisible(node)) continue;
            for (Node child : node.children) {
                if (!isNodeVisible(child)) continue;
                drawConnector(node, child);
            }
        }
        
        // Затем рисуем узлы
        for (Node node : nodes.values()) {
            if (isNodeVisible(node)) {
                drawNode(node);
            }
        }

        if (editingEnabled) {
            for (Node node : nodes.values()) {
                if (isNodeVisible(node)) {
                    drawPins(node);
                }
            }
            drawLinkPreview();
        }

        GlStateManager.popMatrix();
        lastScale = scale;
    }

    private void drawNode(Node node) {
        TextureManager textureManager = minecraft.getTextureManager();

        boolean isStudied = isNodeStudied(node);
        boolean canStudy = canStudyNode(node);

        drawNodeIcon(node, textureManager, isStudied);
        drawNodeLabels(node, isStudied, canStudy);
        drawCostLabel(node, canStudy, isStudied);

        if (node == selectedNode) {
            drawSelectionHighlight(node);
            drawNodeTooltip(node);
        }
    }

    private void drawNodeIcon(Node node, TextureManager textureManager, boolean isStudied) {
        ResourceLocation customIcon = node.customIcon;
        if (customIcon != null) {
            bindTextureSafe(textureManager, customIcon);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawModalRectWithCustomSizedTexture(
                    node.getX() - 12,
                    node.getY() - 12,
                    0,
                    0,
                    24,
                    24,
                    24,
                    24);
            return;
        }

        if (node.hasRecipe) {
            ItemStack result = node.recipe.getRecipeResult();
            if (!result.isEmpty()) {
                GlStateManager.pushMatrix();
                RenderHelper.enableGUIStandardItemLighting();
                minecraft.getRenderItem().renderItemAndEffectIntoGUI(result, node.getX() - 8, node.getY() - 10);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.popMatrix();

                if (isStudied) {
                    GlStateManager.pushMatrix();
                    GlStateManager.enableBlend();
                    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                    minecraft.getRenderItem().renderItemAndEffectIntoGUI(result, node.getX() - 8, node.getY() - 10);
                    GlStateManager.disableBlend();
                    GlStateManager.popMatrix();
                }
                return;
            }
        }

        bindTextureSafe(textureManager, FALLBACK_ICON);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawModalRectWithCustomSizedTexture(
                node.getX() - 8,
                node.getY() - 11,
                0,
                0,
                16,
                16,
                16,
                16);
    }

    private void drawNodeLabels(Node node, boolean isStudied, boolean canStudy) {
        String title = !node.nodeTitle.isEmpty()
                ? node.nodeTitle
                : (node.hasRecipe ? node.recipe.getNodeTitle() : node.category);
        if ((title == null || title.isEmpty()) && node.data != null) {
            title = node.data.tab;
        }
        if (title != null && !title.isEmpty()) {
            drawCenteredLabel(title, node.getX(), node.getY() - NODE_SIZE / 2 - 10, 0xFFFFFF);
        }

        drawCenteredLabel(node.displayName, node.getX(), node.getY() + NODE_SIZE / 2 + 2, 0xFFCCCCCC);

        if (isStudied) {
            drawCenteredLabel(TextFormatting.GREEN + "Изучено", node.getX(), node.getY() + NODE_SIZE / 2 + 12, 0x80FFFFFF);
        } else if (canStudy) {
            drawCenteredLabel(TextFormatting.YELLOW + "Доступно", node.getX(), node.getY() + NODE_SIZE / 2 + 12, 0x80FFFFFF);
        } else if (node.hasRecipe) {
            drawCenteredLabel(TextFormatting.DARK_GRAY + "Недоступно", node.getX(), node.getY() + NODE_SIZE / 2 + 12, 0x80FFFFFF);
        } else {
            drawCenteredLabel(TextFormatting.AQUA + "Пользовательский", node.getX(), node.getY() + NODE_SIZE / 2 + 12, 0x80FFFFFF);
        }
    }

    private void drawCostLabel(Node node, boolean canStudy, boolean isStudied) {
        if (isStudied || node.studyCost <= 0) {
            return;
        }

        int cost = node.studyCost;
        String costLabel = TextFormatting.GOLD + String.valueOf(cost) + " очков";
        int color = canStudy ? 0xFFEAA91A : 0xFFAA4444;
        drawCenteredLabel(costLabel, node.getX(), node.getY() + NODE_SIZE / 2 + 24, color);
    }

    private void drawSelectionHighlight(Node node) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        drawGradientRect(node.getX() - NODE_SIZE / 2 - 2, node.getY() - NODE_SIZE / 2 - 2,
                node.getX() + NODE_SIZE / 2 + 2, node.getY() + NODE_SIZE / 2 + 2,
                0x80FFFF00, 0x80FFD700);
        GlStateManager.disableBlend();
    }

    private void drawNodeTooltip(Node node) {
        if (!node.hasRecipe) {
            return;
        }

        List<String> tooltip = node.recipe.getFullTooltip(player, experienceData);
        if (tooltip.isEmpty()) {
            return;
        }

        int tooltipWidth = tooltip.stream().mapToInt(fontRenderer::getStringWidth).max().orElse(0);
        int tooltipX = node.getX() + NODE_SIZE / 2 + 10;
        int tooltipY = node.getY() - tooltip.size() * 10 / 2;

        drawGradientRect(tooltipX - 3, tooltipY - 3,
                tooltipX + tooltipWidth + 3, tooltipY + tooltip.size() * 10 + 3,
                0xF0100010, 0xF0100010);

        for (int i = 0; i < tooltip.size(); i++) {
            drawString(fontRenderer, tooltip.get(i), tooltipX, tooltipY + i * 10, 0xFFFFFFFF);
        }
    }

    private void drawCenteredLabel(String text, int centerX, int y, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int width = fontRenderer.getStringWidth(text);
        drawString(fontRenderer, text, centerX - width / 2, y, color);
    }

    private void drawPins(Node node) {
        int inputX = getInputPinX(node);
        int outputX = getOutputPinX(node);
        int pinY = node.getY();

        boolean inputHover = linkHoverTarget != null && linkHoverTarget == node;
        drawPinSquare(inputX, pinY, inputHover ? PIN_HOVER_COLOR : PIN_INACTIVE_COLOR);

        boolean isSource = linkActive && linkSourceNode == node;
        drawPinSquare(outputX, pinY, isSource ? PIN_ACTIVE_COLOR : PIN_INACTIVE_COLOR);
    }

    private void drawPinSquare(int centerX, int centerY, int color) {
        int left = centerX - PIN_RADIUS;
        int top = centerY - PIN_RADIUS;
        drawRect(left, top, centerX + PIN_RADIUS, centerY + PIN_RADIUS, color);
    }

    private void drawLinkPreview() {
        if (!linkActive || linkSourceNode == null) {
            return;
        }
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.color(1.0F, 0.8F, 0.2F, 0.9F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL_LINES, DefaultVertexFormats.POSITION);
        buffer.pos(getOutputPinX(linkSourceNode), linkSourceNode.getY(), 0).endVertex();
        buffer.pos(linkCursorX, linkCursorY, 0).endVertex();
        tessellator.draw();

        GlStateManager.glLineWidth(1.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawConnector(Node from, Node to) {
        // Рисуем изогнутую линию от центра одного узла к центру другого
        boolean studied = from.hasRecipe && from.recipe.isStudiedByPlayer(player.getUniqueID());
        int color = studied ? CONNECTOR_COLOR_STUDIED : CONNECTOR_COLOR_LOCKED;
                   
        // Координаты начала и конца линии
        int x1 = from.getX();
        int y1 = from.getY();
        int x2 = to.getX();
        int y2 = to.getY();
        
        // Вычисляем контрольные точки для кривой Безье
        int dx = x2 - x1;
        int dy = y2 - y1;
        int controlX1 = x1 + dx / 3;
        int controlY1 = y1;
        int controlX2 = x2 - dx / 3;
        int controlY2 = y2;
        
        // Включаем сглаживание линий
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
        GlStateManager.glLineWidth((float) CONNECTOR_WIDTH);
        
        // Рисуем изогнутую линию
        drawBezierCurve(x1, y1, controlX1, controlY1, controlX2, controlY2, x2, y2, color);
        
        // Рисуем стрелку
        double angle = Math.atan2(y2 - controlY2, x2 - controlX2);
        drawArrow(x2, y2, angle, color);
        
        GlStateManager.disableBlend();
        GlStateManager.glLineWidth(1.0F);
    }

    private void drawBezierCurve(int x1, int y1, int cx1, int cy1, int cx2, int cy2, int x2, int y2, int color) {
        int steps = 30; // Количество сегментов кривой
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        
        GlStateManager.disableTexture2D();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float tt = t * t;
            float ttt = tt * t;
            float u = 1 - t;
            float uu = u * u;
            float uuu = uu * u;
            
            float x = uuu * x1 + 
                     3 * uu * t * cx1 + 
                     3 * u * tt * cx2 +
                     ttt * x2;
                     
            float y = uuu * y1 +
                     3 * uu * t * cy1 +
                     3 * u * tt * cy2 +
                     ttt * y2;
                     
            buffer.pos(x, y, 0).color(r, g, b, a).endVertex();
        }

        tessellator.draw();
        GlStateManager.enableTexture2D();
    }
    
    private void drawArrow(int x, int y, double angle, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        
        int arrowSize = 8;
        double arrowAngle = Math.PI / 6; // 30 градусов
        
        int x1 = x - (int)(arrowSize * Math.cos(angle - arrowAngle));
        int y1 = y - (int)(arrowSize * Math.sin(angle - arrowAngle));
        int x2 = x - (int)(arrowSize * Math.cos(angle + arrowAngle));
        int y2 = y - (int)(arrowSize * Math.sin(angle + arrowAngle));
        
        GlStateManager.disableTexture2D();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
    }

    private boolean isPointInNode(float x, float y, Node node) {
        int nodeX = node.getX();
        int nodeY = node.getY();
        int nodeSize = NODE_SIZE;
        return x >= nodeX - nodeSize / 2 && x <= nodeX + nodeSize / 2 && y >= nodeY - nodeSize / 2 && y <= nodeY + nodeSize / 2;
    }

    public boolean handleMouseClick(int mouseX, int mouseY, float offsetX, float offsetY, float scale) {
        // Пересчитываем координаты мыши с учетом смещения и масштаба
        float scaledX = (mouseX - offsetX) / scale;
        float scaledY = (mouseY - offsetY) / scale;

        // Проверяем попадание в узлы
        for (Node node : nodes.values()) {
            if (!isNodeVisible(node)) {
                continue;
            }
            if (isPointInNode(scaledX, scaledY, node)) {
                return handleNodeClick(node);
            }
        }

        // Если клик не попал ни в один узел, сбрасываем выделение
        clearSelection();
        return false;
    }

    private long lastClickTime;
    private String lastClickedNodeId;
    private static final long DOUBLE_CLICK_THRESHOLD_MS = 300L;

    private boolean handleNodeClick(Node node) {
        long now = System.currentTimeMillis();
        boolean isDoubleClick = node.id.equals(lastClickedNodeId) && (now - lastClickTime) <= DOUBLE_CLICK_THRESHOLD_MS;
        lastClickedNodeId = node.id;
        lastClickTime = now;
        setSelectedNode(node);
        return isDoubleClick;
    }

    public void startDragging(int mouseX, int mouseY) {
        isDragging = true;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public void stopDragging() {
        isDragging = false;
    }

    /**
     * Возвращает смещение мыши за один шаг перетаскивания.
     */
    public int[] handleMouseDrag(int mouseX, int mouseY) {
        if (!isDragging) {
            return null;
        }
        int deltaX = mouseX - lastMouseX;
        int deltaY = mouseY - lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return new int[] { deltaX, deltaY };
    }

    public void setEditingEnabled(boolean editingEnabled) {
        this.editingEnabled = editingEnabled;
        if (!editingEnabled) {
            draggingNode = null;
            nodeMovedDuringDrag = false;
            cancelLink(false);
        }
    }

    public boolean beginNodeDrag(float mouseX, float mouseY, float offsetX, float offsetY, float scale) {
        if (!editingEnabled) {
            return false;
        }
        float worldX = (mouseX - offsetX) / scale;
        float worldY = (mouseY - offsetY) / scale;
        Node node = findNodeAt(worldX, worldY);
        if (node == null) {
            return false;
        }
        draggingNode = node;
        dragStartWorldX = worldX;
        dragStartWorldY = worldY;
        if (node.data.position == null) {
            node.data.position = new RecipeTreeConfigManager.Position();
        }
        dragStartNodeX = node.data.position.x;
        dragStartNodeY = node.data.position.y;
        nodeMovedDuringDrag = false;
        setSelectedNode(node);
        return true;
    }

    public boolean updateNodeDrag(float mouseX, float mouseY, float offsetX, float offsetY, float scale) {
        if (!editingEnabled || draggingNode == null) {
            return false;
        }
        float worldX = (mouseX - offsetX) / scale;
        float worldY = (mouseY - offsetY) / scale;
        int newX = Math.round(dragStartNodeX + (worldX - dragStartWorldX));
        int newY = Math.round(dragStartNodeY + (worldY - dragStartWorldY));
        if (draggingNode.data.position == null) {
            draggingNode.data.position = new RecipeTreeConfigManager.Position();
        }
        if (draggingNode.data.position.x != newX || draggingNode.data.position.y != newY) {
            draggingNode.data.position.x = newX;
            draggingNode.data.position.y = newY;
            nodeMovedDuringDrag = true;
        }
        return nodeMovedDuringDrag;
    }

    public void endNodeDrag(boolean commit) {
        if (draggingNode == null) {
            return;
        }
        if (!nodeMovedDuringDrag) {
            draggingNode = null;
            return;
        }
        if (!commit) {
            draggingNode.data.position.x = dragStartNodeX;
            draggingNode.data.position.y = dragStartNodeY;
            draggingNode = null;
            nodeMovedDuringDrag = false;
            return;
        }

        RecipeTreeConfigManager manager = RecipeTreeConfigManager.getInstance();
        manager.upsertNode(draggingNode.data);
        manager.save();
        draggingNode = null;
        nodeMovedDuringDrag = false;
    }

    public boolean handleEditorClick(float mouseX, float mouseY, float offsetX, float offsetY, float scale, int mouseButton) {
        if (!editingEnabled) {
            return false;
        }
        float worldX = (mouseX - offsetX) / scale;
        float worldY = (mouseY - offsetY) / scale;

        if (mouseButton == 0) {
            Node node = findNodeAtOutputPin(worldX, worldY);
            if (node != null) {
                linkActive = true;
                linkSourceNode = node;
                linkCursorX = getOutputPinX(node);
                linkCursorY = node.getY();
                linkHoverTarget = null;
                return true;
            }
        } else if (mouseButton == 1 && linkActive) {
            cancelLink();
            return true;
        }
        return false;
    }

    public void updateLinkCursor(float mouseX, float mouseY, float offsetX, float offsetY, float scale) {
        if (!linkActive) {
            return;
        }
        float worldX = (mouseX - offsetX) / scale;
        float worldY = (mouseY - offsetY) / scale;
        linkCursorX = worldX;
        linkCursorY = worldY;
        linkHoverTarget = findNodeAtInputPin(worldX, worldY);
        if (linkHoverTarget == linkSourceNode) {
            linkHoverTarget = null;
        }
    }

    public boolean completeLink(float mouseX, float mouseY, float offsetX, float offsetY, float scale) {
        if (!linkActive || linkSourceNode == null) {
            return false;
        }
        float worldX = (mouseX - offsetX) / scale;
        float worldY = (mouseY - offsetY) / scale;
        Node target = findNodeAtInputPin(worldX, worldY);
        RecipeTreeEditorScreen editor = RecipeTreeEditorScreen.getActiveInstance();
        boolean success = false;
        if (target == null) {
            if (editor != null) {
                editor.notifyLinkFailed("Выберите целевой узел");
            }
        } else if (target == linkSourceNode) {
            if (editor != null) {
                editor.notifyLinkFailed("Нельзя связать узел с самим собой");
            }
        } else {
            success = addConnection(linkSourceNode, target);
        }
        cancelLink(false);
        return success;
    }

    public void cancelLink() {
        cancelLink(true);
    }

    public void cancelLink(boolean notify) {
        if (!linkActive) {
            return;
        }
        linkActive = false;
        linkSourceNode = null;
        linkHoverTarget = null;
        if (notify) {
            RecipeTreeEditorScreen editor = RecipeTreeEditorScreen.getActiveInstance();
            if (editor != null) {
                editor.notifyLinkCancelled();
            }
        }
    }

    public boolean isLinking() {
        return linkActive;
    }

    public String getSelectedNodeId() {
        return selectedNodeId;
    }

    public NodeData getSelectedNodeData() {
        return selectedNode != null ? selectedNode.data : null;
    }

    public void selectNode(String nodeId) {
        if (nodeId == null) {
            clearSelection();
            return;
        }
        Node node = nodes.get(nodeId);
        if (node != null) {
            setSelectedNode(node);
        }
    }

    private Node findNodeAt(float worldX, float worldY) {
        for (Node node : nodes.values()) {
            if (!isNodeVisible(node)) {
                continue;
            }
            if (isPointInNode(worldX, worldY, node)) {
                return node;
            }
        }
        return null;
    }

    private Node findNodeAtInputPin(float worldX, float worldY) {
        for (Node node : nodes.values()) {
            if (!isNodeVisible(node)) {
                continue;
            }
            if (isPointInPin(worldX, worldY, getInputPinX(node), node.getY())) {
                return node;
            }
        }
        return null;
    }

    private Node findNodeAtOutputPin(float worldX, float worldY) {
        for (Node node : nodes.values()) {
            if (!isNodeVisible(node)) {
                continue;
            }
            if (isPointInPin(worldX, worldY, getOutputPinX(node), node.getY())) {
                return node;
            }
        }
        return null;
    }

    private boolean isPointInPin(float worldX, float worldY, int centerX, int centerY) {
        return Math.abs(worldX - centerX) <= PIN_RADIUS && Math.abs(worldY - centerY) <= PIN_RADIUS;
    }

    private int getInputPinX(Node node) {
        return node.getX() - PIN_OFFSET;
    }

    private int getOutputPinX(Node node) {
        return node.getX() + PIN_OFFSET;
    }

    private Unlocks ensureUnlocks(NodeData node) {
        if (node.unlocks == null) {
            node.unlocks = new Unlocks();
        }
        if (node.unlocks.nodes == null) {
            node.unlocks.nodes = new ArrayList<>();
        }
        if (node.unlocks.tabs == null) {
            node.unlocks.tabs = new ArrayList<>();
        }
        if (node.unlocks.permissions == null) {
            node.unlocks.permissions = new ArrayList<>();
        }
        return node.unlocks;
    }

    private Availability ensureAvailability(NodeData node) {
        if (node.availability == null) {
            node.availability = new Availability();
        }
        if (node.availability.mode == null || node.availability.mode.isEmpty()) {
            node.availability.mode = Availability.MODE_ALWAYS;
        }
        if (node.availability.requiredNodes == null) {
            node.availability.requiredNodes = new ArrayList<>();
        }
        return node.availability;
    }

    private boolean addConnection(Node source, Node target) {
        Unlocks unlocks = ensureUnlocks(source.data);
        Availability availability = ensureAvailability(target.data);
        boolean changed = false;

        if (!unlocks.nodes.contains(target.id)) {
            unlocks.nodes.add(target.id);
            changed = true;
        }
        if (!availability.requiredNodes.contains(source.id)) {
            availability.requiredNodes.add(source.id);
            changed = true;
        }

        if (!changed) {
            return false;
        }

        RecipeTreeConfigManager manager = RecipeTreeConfigManager.getInstance();
        manager.upsertNode(source.data);
        manager.upsertNode(target.data);
        manager.save();

        String previousSelection = selectedNodeId;
        buildTree();
        if (previousSelection != null) {
            selectNode(previousSelection);
        }

        RecipeTreeEditorScreen editor = RecipeTreeEditorScreen.getActiveInstance();
        if (editor != null) {
            editor.refreshFromConfig(target.id);
            editor.notifyLinkCreated(source.id, target.id);
        }
        return true;
    }

    public RecipeEntry getSelectedRecipe() {
        return selectedNode != null ? selectedNode.recipe : null;
    }

    public boolean hasSelectedRecipe() {
        return selectedNode != null;
    }

    public void setActiveTab(String tabId) {
        if (tabId == null || tabId.isEmpty()) {
            this.activeTabId = null;
        } else if (!Objects.equals(this.activeTabId, tabId)) {
            this.activeTabId = tabId;
        }
        if (!isNodeVisible(selectedNode)) {
            clearSelection();
        }
    }

    private List<NodeData> normalizeNodes(List<NodeData> rawNodes) {
        if (rawNodes == null || rawNodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<NodeData> result = new ArrayList<>(rawNodes.size());
        for (NodeData node : rawNodes) {
            if (node == null) {
                continue;
            }
            NodeData copy = cloneNodeData(node);
            copy.customIcon = normalizeResourcePath(copy.customIcon, node.customIcon);
            copy.tab = normalizeTabId(copy.tab);
            result.add(copy);
        }
        return result;
    }

    private NodeData cloneNodeData(NodeData source) {
        NodeData copy = new NodeData();
        copy.id = source.id;
        copy.tab = source.tab;
        copy.recipeId = source.recipeId;
        copy.displayName = source.displayName;
        copy.studyCost = source.studyCost;
        copy.position = source.position != null ? clonePosition(source.position) : null;
        copy.availability = source.availability;
        copy.unlocks = source.unlocks;
        copy.grantsCraftAccess = source.grantsCraftAccess;
        copy.customIcon = source.customIcon;
        copy.nodeTitle = source.nodeTitle;
        return copy;
    }

    private Position clonePosition(Position position) {
        Position result = new Position();
        result.x = position.x;
        result.y = position.y;
        return result;
    }

    private String normalizeTabId(String tabId) {
        if (tabId == null) {
            return null;
        }
        String trimmed = tabId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeResourcePath(String raw, String fallback) {
        String value = raw != null ? raw : fallback;
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String normalized = trimmed;
        if (normalized.startsWith("minecraft:textures/")) {
            normalized = normalized.substring("minecraft:textures/".length());
        }
        if (normalized.endsWith(".png")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        if (!normalized.contains(":")) {
            normalized = Reference.MOD_ID + ":" + normalized;
        }
        return normalized;
    }

    public String getActiveTabId() {
        return activeTabId;
    }

    public void refresh() {
        buildTree();
    }

    public Map<String, Node> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public NodeData getNodeData(String nodeId) {
        Node node = nodes.get(nodeId);
        return node != null ? node.data : null;
    }

    public List<NodeData> getVisibleNodeData() {
        List<NodeData> result = new ArrayList<>();
        for (Node node : nodes.values()) {
            if (isNodeVisible(node)) {
                result.add(node.data);
            }
        }
        return result;
    }

    private void setSelectedNode(Node node) {
        selectedNode = node;
        selectedNodeId = node != null ? node.id : null;
    }

    private void clearSelection() {
        selectedNode = null;
        selectedNodeId = null;
    }

    private boolean isNodeVisible(Node node) {
        if (node == null) {
            return false;
        }
        if (activeTabId == null || activeTabId.isEmpty()) {
            return true;
        }
        String nodeTab = node.data != null ? node.data.tab : null;
        if (nodeTab == null || nodeTab.trim().isEmpty()) {
            return activeTabId == null || activeTabId.isEmpty();
        }
        return activeTabId.equals(nodeTab);
    }

    private boolean canStudyNode(Node node) {
        if (node == null) {
            return false;
        }

        UUID playerId = player.getUniqueID();
        if (node.recipe != null && node.recipe.isStudiedByPlayer(playerId)) {
            return false;
        }

        if (node.recipe == null) {
            return false;
        }

        boolean base = node.recipe.canPlayerStudy(player, experienceData);
        if (!base) {
            return false;
        }

        Availability availability = node.data != null ? node.data.availability : null;
        if (availability == null || Availability.MODE_ALWAYS.equalsIgnoreCase(availability.mode)) {
            return true;
        }

        if (Availability.MODE_REQUIRES_NODES.equalsIgnoreCase(availability.mode)) {
            boolean parentsStudied = node.parents.stream()
                    .filter(Objects::nonNull)
                    .allMatch(parent -> parent.recipe.isStudiedByPlayer(playerId));
            if (parentsStudied) {
                return true;
            }
            return availability.allowIndependentStudy;
        }

        return base;
    }

    private boolean isNodeStudied(Node node) {
        return node.hasRecipe && node.recipe.isStudiedByPlayer(player.getUniqueID());
    }

    private void bindTextureSafe(TextureManager textureManager, ResourceLocation location) {
        try {
            textureManager.bindTexture(location);
        } catch (Exception ex) {
            CraftMastery.logger.warn("Failed to bind texture {}: {}", location, ex.getMessage());
            textureManager.bindTexture(TextureMap.LOCATION_MISSING_TEXTURE);
        }
    }

    private String determineInitialTabId(List<NodeData> configuredNodes) {
        String configuredBySelection = selectedNodeId != null ? selectedNodeId : null;
        if (configuredBySelection != null) {
            NodeData data = configuredNodes.stream()
                    .filter(nodeData -> nodeData != null && configuredBySelection.equals(nodeData.id))
                    .findFirst()
                    .orElse(null);
            if (data != null && data.tab != null) {
                return data.tab;
            }
        }

        List<TabData> tabs = RecipeTreeConfigManager.getInstance().getTabs();
        if (!tabs.isEmpty()) {
            return tabs.get(0).id;
        }

        for (NodeData data : configuredNodes) {
            if (data != null && data.tab != null && !data.tab.trim().isEmpty()) {
                return data.tab;
            }
        }

        return "default";
    }

    private static ResourceLocation parseResource(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return new ResourceLocation(raw.trim());
        } catch (Exception ex) {
            CraftMastery.logger.warn("Invalid resource location '{}': {}", raw, ex.getMessage());
            return null;
        }
    }
}