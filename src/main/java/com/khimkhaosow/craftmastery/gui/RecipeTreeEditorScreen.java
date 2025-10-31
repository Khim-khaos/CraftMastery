package com.khimkhaosow.craftmastery.gui;

import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.Availability;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.NodeData;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.Position;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.Unlocks;
import com.khimkhaosow.craftmastery.gui.widgets.RecipeTreeWidget;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;
import net.minecraft.util.text.TextFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Полноэкранный редактор конфигурации узлов дерева рецептов.
 */
public class RecipeTreeEditorScreen extends GuiScreen {
    private static final int LIST_WIDTH = 160;
    private static final int FORM_LEFT = LIST_WIDTH + 24;
    private static final int FIELD_WIDTH = 220;
    private static final int FIELD_HEIGHT = 16;
    private static final int FIELD_GAP = 30;
    private static final int SECTION_GAP = 56;
    private static final int PREVIEW_WIDTH = 200;
    private static final int PREVIEW_HEIGHT = 180;

    private static final int BTN_NEW = 200;
    private static final int BTN_SAVE = 201;
    private static final int BTN_DELETE = 202;
    private static final int BTN_CLOSE = 203;
    private static final int BTN_MODE = 204;
    private static final int BTN_CUSTOM_NODE = 205;
    private static final int BTN_SELECT_TAB = 206;
    private static final int BTN_SELECT_ICON = 207;
    private static final int BTN_SELECT_RECIPE = 208;

    private final GuiCraftMastery parent;
    private final EntityPlayer player;
    private final RecipeTreeWidget treeWidget;
    private RecipeEntry pendingRecipe;
    private static final Logger LOGGER = LogManager.getLogger("RecipeTreeEditorScreen");
    private final RecipeTreeConfigManager config = RecipeTreeConfigManager.getInstance();

    private List<NodeData> nodes = new ArrayList<>();
    private NodeData editingNode;
    private String selectedNodeId;
    private NodeList nodeList;

    private GuiTextField idField;
    private GuiTextField tabField;
    private GuiTextField recipeIdField;
    private GuiCheckBox customNodeCheck;
    private GuiTextField displayNameField;
    private GuiTextField titleField;
    private GuiTextField iconField;
    private GuiTextField costField;
    private GuiTextField posXField;
    private GuiTextField posYField;
    private GuiTextField requiresField;
    private GuiTextField unlockNodesField;
    private GuiTextField unlockTabsField;
    private GuiTextField unlockPermsField;

    private GuiButton modeButton;
    private GuiButton tabSelectButton;
    private GuiButton iconSelectButton;
    private GuiButton recipeSelectButton;
    private GuiCheckBox allowIndependentCheck;
    private GuiCheckBox grantAccessCheck;

    private String statusMessage;
    private int statusColor = 0xFFFFFFFF;
    private long statusTicks;
    private static boolean editorOpen;
    private static RecipeTreeEditorScreen activeInstance;

    private boolean creationDialogVisible;
    private GuiTextField creationIdField;
    private GuiTextField creationCostField;
    private GuiButton creationConfirmButton;
    private GuiButton creationCancelButton;

    public RecipeTreeEditorScreen(GuiCraftMastery parent, EntityPlayer player, RecipeTreeWidget treeWidget) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.player = Objects.requireNonNull(player, "player");
        this.treeWidget = treeWidget;
    }

    private void openIconPicker() {
        Minecraft.getMinecraft().displayGuiScreen(new TextureResourceBrowserScreen(this));
    }

    void onIconSelected(ResourceLocation icon) {
        if (iconField != null && icon != null) {
            iconField.setText(icon.toString());
            if (editingNode != null) {
                editingNode.customIcon = icon.toString();
            }
        }
        restoreAfterChild();
    }

    private void openTabPicker() {
        Minecraft.getMinecraft().displayGuiScreen(new RecipeTreeTabSelectionScreen(this));
    }

    void onTabSelected(String tabId) {
        if (tabId == null || tabField == null) {
            return;
        }
        tabField.setText(tabId);
        if (editingNode != null) {
            editingNode.tab = tabId;
        }
        if (treeWidget != null) {
            treeWidget.setActiveTab(tabId);
        }
        restoreAfterChild();
    }

    public static boolean isEditorOpen() {
        return editorOpen;
    }

    public static RecipeTreeEditorScreen getActiveInstance() {
        return activeInstance;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        reloadNodes();
        editorOpen = true;
        activeInstance = this;

        nodeList = new NodeList();
        nodeList.setSlotXBoundsFromLeft(10);

        int y = 40;
        idField = createField(y, "");
        y += FIELD_GAP;
        tabField = createField(y, "");
        tabSelectButton = new GuiButton(BTN_SELECT_TAB, FORM_LEFT + FIELD_WIDTH + 10, y + 6, 80, 18, "Выбрать");
        buttonList.add(tabSelectButton);
        y += FIELD_GAP;
        recipeIdField = createField(y, "");
        recipeIdField.setEnabled(false);
        y += FIELD_GAP;
        customNodeCheck = new GuiCheckBox(BTN_CUSTOM_NODE, FORM_LEFT, y, "Без рецепта (пользовательский узел)", false);
        buttonList.add(customNodeCheck);
        updateCustomNodeState();

        recipeSelectButton = new GuiButton(BTN_SELECT_RECIPE, FORM_LEFT + FIELD_WIDTH + 10, y + 6, 80, 18, "Выбрать");
        buttonList.add(recipeSelectButton);

        y += SECTION_GAP;
        displayNameField = createField(y, "");
        y += FIELD_GAP;
        titleField = createField(y, "");
        y += FIELD_GAP;
        iconField = createField(y, "");
        iconSelectButton = new GuiButton(BTN_SELECT_ICON, FORM_LEFT + FIELD_WIDTH + 10, y + 6, 80, 18, "Выбрать");
        buttonList.add(iconSelectButton);
        y += FIELD_GAP;
        costField = createField(y, "0");
        y += FIELD_GAP;
        posXField = createField(y, "0");
        y += FIELD_GAP;
        posYField = createField(y, "0");

        y += SECTION_GAP;
        modeButton = new GuiButton(BTN_MODE, FORM_LEFT, y, FIELD_WIDTH, 20, "Режим: всегда");
        buttonList.add(modeButton);
        y += FIELD_GAP;
        allowIndependentCheck = new GuiCheckBox(300, FORM_LEFT, y, "Разрешить самостоятельное изучение", false);
        buttonList.add(allowIndependentCheck);
        y += FIELD_GAP;
        grantAccessCheck = new GuiCheckBox(301, FORM_LEFT, y, "Открывает доступ к крафту", false);
        buttonList.add(grantAccessCheck);

        y += SECTION_GAP;
        requiresField = createField(y, "");
        y += FIELD_GAP;
        unlockNodesField = createField(y, "");
        y += FIELD_GAP;
        unlockTabsField = createField(y, "");
        y += FIELD_GAP;
        unlockPermsField = createField(y, "");

        int btnRowTop = height - 48;
        int btnRowSpacing = 6;
        int btnWidth = 92;
        int btnHeight = 20;
        int btnGap = 8;
        int firstRowX = FORM_LEFT;
        int secondRowX = FORM_LEFT;

        buttonList.add(new GuiButton(BTN_NEW, firstRowX, btnRowTop, btnWidth, btnHeight, "Новый"));
        buttonList.add(new GuiButton(BTN_SAVE, firstRowX + btnWidth + btnGap, btnRowTop, btnWidth, btnHeight, "Сохранить"));

        int secondRowY = btnRowTop + btnHeight + btnRowSpacing;
        buttonList.add(new GuiButton(BTN_DELETE, secondRowX, secondRowY, btnWidth, btnHeight, "Удалить"));
        buttonList.add(new GuiButton(BTN_CLOSE, secondRowX + btnWidth + btnGap, secondRowY, btnWidth, btnHeight, "Закрыть"));

        if (pendingRecipe != null) {
            createNewNode();
            LOGGER.info("RecipeTreeEditorScreen init: creating node from pending recipe");
        } else if (selectedNodeId != null) {
            selectNodeById(selectedNodeId);
        } else if (editingNode != null) {
            boolean allowCustomReset = editingNode.recipeId == null || editingNode.recipeId.trim().isEmpty();
            loadNodeToForm(editingNode, allowCustomReset);
        } else if (!nodes.isEmpty()) {
            beginSelectExistingNode(nodes.get(0));
        } else {
            createNewNode();
        }
    }

    private GuiTextField createField(int y, String value) {
        GuiTextField field = new GuiTextField(0, fontRenderer, FORM_LEFT, y, FIELD_WIDTH, FIELD_HEIGHT);
        field.setMaxStringLength(256);
        field.setText(value);
        return field;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        editorOpen = false;
        if (activeInstance == this) {
            activeInstance = null;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        GuiTextField[] fields = {
            idField, tabField, recipeIdField, displayNameField, titleField, iconField,
            costField, posXField, posYField, requiresField, unlockNodesField,
            unlockTabsField, unlockPermsField
        };
        for (GuiTextField field : fields) {
            field.updateCursorCounter();
        }
        if (statusMessage != null && mc.world != null && mc.world.getTotalWorldTime() - statusTicks > 200) {
            statusMessage = null;
        }
        if (creationDialogVisible) {
            if (creationIdField != null) {
                creationIdField.updateCursorCounter();
            }
            if (creationCostField != null) {
                creationCostField.updateCursorCounter();
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_NEW:
                createNewNode();
                break;
            case BTN_SAVE:
                if (creationDialogVisible) {
                    break;
                }
                saveCurrentNode();
                break;
            case BTN_DELETE:
                if (creationDialogVisible) {
                    break;
                }
                deleteCurrentNode();
                break;
            case BTN_CLOSE:
                if (creationDialogVisible) {
                    closeCreationDialog();
                    break;
                }
                closeEditor(false, null);
                break;
            case BTN_MODE:
                if (creationDialogVisible) {
                    break;
                }
                toggleAvailabilityMode();
                break;
            case BTN_CUSTOM_NODE:
                if (creationDialogVisible) {
                    break;
                }
                boolean customMode = !customNodeCheck.isChecked();
                if (customNodeCheck.isChecked()) {
                    recipeIdField.setText("");
                }
                break;
            case BTN_SELECT_TAB:
                if (!creationDialogVisible) {
                    openTabPicker();
                }
                break;
            case BTN_SELECT_ICON:
                if (!creationDialogVisible) {
                    openIconPicker();
                }
                break;
            case BTN_SELECT_RECIPE:
                if (!creationDialogVisible) {
                    openRecipePicker();
                }
                break;
            default:
                break;
        }
    }

    private void toggleAvailabilityMode() {
        if (editingNode == null) {
            return;
        }
        Availability availability = ensureAvailability(editingNode);
        if (Availability.MODE_ALWAYS.equalsIgnoreCase(availability.mode)) {
            availability.mode = Availability.MODE_REQUIRES_NODES;
        } else {
            availability.mode = Availability.MODE_ALWAYS;
        }
        updateModeButton();
    }

    private void createNewNode() {
        editingNode = new NodeData();
        editingNode.id = "";
        String activeTab = treeWidget != null ? treeWidget.getActiveTabId() : null;
        editingNode.tab = (activeTab == null || activeTab.trim().isEmpty()) ? "default" : activeTab;
        editingNode.recipeId = "";
        editingNode.displayName = "";
        editingNode.nodeTitle = "";
        editingNode.customIcon = "";
        editingNode.studyCost = 0;
        editingNode.position = new Position();
        editingNode.position.x = 0;
        editingNode.position.y = 0;
        editingNode.availability = new Availability();
        editingNode.availability.mode = Availability.MODE_ALWAYS;
        editingNode.availability.requiredNodes = new ArrayList<>();
        editingNode.unlocks = new Unlocks();
        editingNode.unlocks.nodes = new ArrayList<>();
        editingNode.unlocks.tabs = new ArrayList<>();
        editingNode.unlocks.permissions = new ArrayList<>();
        editingNode.grantsCraftAccess = false;
        editingNode.availability.allowIndependentStudy = false;
        selectedNodeId = null;
        loadNodeToForm(editingNode);
        if (customNodeCheck != null) {
            customNodeCheck.setIsChecked(false);
            updateCustomNodeState();
        }
        idField.setFocused(true);
        if (pendingRecipe != null) {
            applyPendingRecipe(true);
        }
        LOGGER.info("RecipeTreeEditorScreen createNewNode on tab '{}'", editingNode.tab);
    }

    private void applyPendingRecipe(boolean createDialog) {
        if (editingNode == null || pendingRecipe == null) {
            return;
        }
        String recipeId = pendingRecipe.getRecipeLocation().toString();
        if (editingNode.id == null || editingNode.id.trim().isEmpty()) {
            editingNode.id = recipeId.replace(':', '_');
        }
        editingNode.recipeId = recipeId;
        editingNode.displayName = pendingRecipe.getRecipeResult().getDisplayName();
        editingNode.studyCost = Math.max(0, pendingRecipe.getRequiredLearningPoints());
        editingNode.nodeTitle = pendingRecipe.getNodeTitle();
        ResourceLocation icon = pendingRecipe.getCustomIcon();
        editingNode.customIcon = icon != null ? icon.toString() : editingNode.customIcon;
        if (customNodeCheck != null) {
            customNodeCheck.setIsChecked(false);
            logCustomState("applyPendingRecipe.beforeLoad");
        }
        logSelectionState("applyPendingRecipe.beforeLoad");
        loadNodeToForm(editingNode, false);
        LOGGER.info("RecipeTreeEditorScreen applied pending recipe: id='{}', display='{}', cost={}, title='{}'", editingNode.id, editingNode.displayName, editingNode.studyCost, editingNode.nodeTitle);
        if (icon != null) {
            LOGGER.info("RecipeTreeEditorScreen custom icon set to {}", icon);
        }
        pendingRecipe = null;
        if (createDialog) {
            openCreationDialog();
        } else {
            restoreAfterChild();
        }
    }

    private void saveCurrentNode() {
        NodeData data = collectForm();
        if (data == null) {
            return;
        }
        try {
            NodeData copy = copyNode(data);
            LOGGER.info("RecipeTreeEditorScreen saving node id='{}' tab='{}' recipeId='{}' cost={}", copy.id, copy.tab, copy.recipeId, copy.studyCost);
            config.upsertNode(copy);
            config.save();
            selectedNodeId = data.id;
            reloadNodes();
            selectNodeById(selectedNodeId);
            if (treeWidget != null) {
                treeWidget.refresh();
                treeWidget.setActiveTab(copy.tab);
            }
            setStatus("Узел сохранен", false);
            LOGGER.info("RecipeTreeEditorScreen node saved successfully: {}", copy.id);
        } catch (Exception ex) {
            setStatus("Ошибка: " + ex.getMessage(), true);
            LOGGER.error("RecipeTreeEditorScreen failed to save node {}", data != null ? data.id : "<null>", ex);
        }
    }

    private void deleteCurrentNode() {
        if (selectedNodeId == null || selectedNodeId.trim().isEmpty()) {
            setStatus("Выберите узел", true);
            return;
        }
        if (config.removeNode(selectedNodeId)) {
            config.save();
            String removedId = selectedNodeId;
            selectedNodeId = null;
            reloadNodes();
            if (!nodes.isEmpty()) {
                selectNode(nodes.get(0));
            } else {
                createNewNode();
            }
            if (treeWidget != null) {
                treeWidget.refresh();
            }
            setStatus("Узел удален", false);
        } else {
            setStatus("Узел не найден", true);
        }
    }

    private void closeEditor(boolean reload, String focusTab) {
        if (parent != null) {
            parent.onRecipeEditorClosed(reload, focusTab);
        }
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    private void openTabEditor() {
        Minecraft.getMinecraft().displayGuiScreen(new RecipeTreeTabEditorScreen(this));
    }

    void onTabsUpdated(String focusTabId, String message, boolean error) {
        if (treeWidget != null) {
            treeWidget.refresh();
            if (focusTabId != null) {
                treeWidget.setActiveTab(focusTabId);
            }
        }
        if (parent != null) {
            parent.refreshTabs(focusTabId);
        }
        if (message != null) {
            setStatus(message, error);
        }
    }

    private void selectNode(NodeData data) {
        beginSelectExistingNode(data);
    }

    private void beginSelectExistingNode(NodeData data) {
        if (data == null) {
            return;
        }
        editingNode = copyNode(data);
        selectedNodeId = data.id;
        loadNodeToForm(editingNode);
        if (treeWidget != null) {
            treeWidget.selectNode(selectedNodeId);
            treeWidget.setActiveTab(editingNode.tab);
        }
    }

    private void selectNodeById(String nodeId) {
        if (nodeId == null) {
            return;
        }
        for (NodeData node : nodes) {
            if (Objects.equals(nodeId, node.id)) {
                selectNode(node);
                return;
            }
        }
    }

    private void loadNodeToForm(NodeData data) {
        loadNodeToForm(data, true);
    }

    private void loadNodeToForm(NodeData data, boolean allowCustomReset) {
        idField.setText(nullToEmpty(data.id));
        tabField.setText(nullToEmpty(data.tab));
        recipeIdField.setText(nullToEmpty(data.recipeId));
        displayNameField.setText(nullToEmpty(data.displayName));
        titleField.setText(nullToEmpty(data.nodeTitle));
        iconField.setText(nullToEmpty(data.customIcon));
        costField.setText(Integer.toString(Math.max(0, data.studyCost)));
        Position pos = ensurePosition(data);
        posXField.setText(Integer.toString(pos.x));
        posYField.setText(Integer.toString(pos.y));
        Availability avail = ensureAvailability(data);
        allowIndependentCheck.setIsChecked(avail.allowIndependentStudy);
        requiresField.setText(String.join(", ", avail.requiredNodes));
        grantAccessCheck.setIsChecked(data.grantsCraftAccess);
        Unlocks unlocks = ensureUnlocks(data);
        unlockNodesField.setText(String.join(", ", unlocks.nodes));
        unlockTabsField.setText(String.join(", ", unlocks.tabs));
        unlockPermsField.setText(String.join(", ", unlocks.permissions));
        updateModeButton();
        if (customNodeCheck != null) {
            boolean custom = allowCustomReset && (data.recipeId == null || data.recipeId.trim().isEmpty());
            customNodeCheck.setIsChecked(custom);
            updateCustomNodeState();
            logCustomState("loadNodeToForm.afterUpdate");
        }
        logSelectionState("loadNodeToForm.afterUpdate");
    }

    private void updateModeButton() {
        if (editingNode == null) {
            modeButton.displayString = "Режим: всегда";
            return;
        }
        String mode = ensureAvailability(editingNode).mode;
        modeButton.displayString = Availability.MODE_REQUIRES_NODES.equalsIgnoreCase(mode)
                ? "Режим: требуется узлы"
                : "Режим: всегда";
    }

    private NodeData collectForm() {
        NodeData data = new NodeData();
        data.id = idField.getText().trim();
        if (data.id.isEmpty()) {
            setStatus("ID не может быть пустым", true);
            LOGGER.warn("RecipeTreeEditorScreen collectForm: empty node id");
            return null;
        }
        data.tab = tabField.getText().trim().isEmpty() ? "default" : tabField.getText().trim();
        boolean customNode = isCustomNodeMode();
        String recipeIdValue = recipeIdField.getText().trim();
        data.recipeId = customNode ? null : recipeIdValue;
        if (!customNode && (data.recipeId == null || data.recipeId.isEmpty())) {
            setStatus("Укажите recipeId", true);
            LOGGER.warn("RecipeTreeEditorScreen collectForm: empty recipeId for node {}", data.id);
            return null;
        }
        data.displayName = emptyToNull(displayNameField.getText());
        data.nodeTitle = emptyToNull(titleField.getText());
        data.customIcon = emptyToNull(iconField.getText());
        try {
            data.studyCost = parseInt(costField.getText(), 0, "стоимость");
        } catch (NumberFormatException ex) {
            setStatus(ex.getMessage(), true);
            LOGGER.warn("RecipeTreeEditorScreen collectForm: invalid study cost '{}': {}", costField.getText(), ex.getMessage());
            return null;
        }
        data.position = new Position();
        try {
            data.position.x = parseInt(posXField.getText(), 0, "позиция X");
            data.position.y = parseInt(posYField.getText(), 0, "позиция Y");
        } catch (NumberFormatException ex) {
            setStatus(ex.getMessage(), true);
            LOGGER.warn("RecipeTreeEditorScreen collectForm: invalid position ({}, {}): {}", posXField.getText(), posYField.getText(), ex.getMessage());
            return null;
        }
        Availability availability = new Availability();
        availability.mode = ensureAvailability(editingNode).mode;
        availability.allowIndependentStudy = allowIndependentCheck.isChecked();
        availability.requiredNodes = splitList(requiresField.getText());
        data.availability = availability;
        data.grantsCraftAccess = grantAccessCheck.isChecked();
        Unlocks unlocks = new Unlocks();
        unlocks.nodes = splitList(unlockNodesField.getText());
        unlocks.tabs = splitList(unlockTabsField.getText());
        unlocks.permissions = splitList(unlockPermsField.getText());
        data.unlocks = unlocks;
        return data;
    }

    private int parseInt(String text, int fallback, String field) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Некорректное число для '" + field + "'");
        }
    }

    private void reloadNodes() {
        nodes = new ArrayList<>(config.getNodes());
        nodes.sort(Comparator.comparing((NodeData node) -> node.tab == null ? "" : node.tab)
                .thenComparing(node -> node.displayName == null ? "" : node.displayName)
                .thenComparing(node -> node.id == null ? "" : node.id));
        if (nodeList != null) {
            nodeList = new NodeList();
            nodeList.setSlotXBoundsFromLeft(10);
        }
    }

    private List<String> splitList(String value) {
        List<String> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        String[] parts = value.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private NodeData copyNode(NodeData original) {
        NodeData copy = new NodeData();
        copy.id = original.id;
        copy.tab = original.tab;
        copy.recipeId = original.recipeId;
        copy.displayName = original.displayName;
        copy.nodeTitle = original.nodeTitle;
        copy.customIcon = original.customIcon;
        copy.studyCost = original.studyCost;
        copy.grantsCraftAccess = original.grantsCraftAccess;
        Position pos = ensurePosition(original);
        copy.position = new Position();
        copy.position.x = pos.x;
        copy.position.y = pos.y;
        Availability availability = ensureAvailability(original);
        copy.availability = new Availability();
        copy.availability.mode = availability.mode;
        copy.availability.allowIndependentStudy = availability.allowIndependentStudy;
        copy.availability.requiredNodes = new ArrayList<>(availability.requiredNodes);
        Unlocks unlocks = ensureUnlocks(original);
        copy.unlocks = new Unlocks();
        copy.unlocks.nodes = new ArrayList<>(unlocks.nodes);
        copy.unlocks.tabs = new ArrayList<>(unlocks.tabs);
        copy.unlocks.permissions = new ArrayList<>(unlocks.permissions);
        return copy;
    }

    private Position ensurePosition(NodeData node) {
        if (node.position == null) {
            node.position = new Position();
            node.position.x = 0;
            node.position.y = 0;
        }
        return node.position;
    }

    private Availability ensureAvailability(NodeData node) {
        if (node.availability == null) {
            node.availability = new Availability();
            node.availability.mode = Availability.MODE_ALWAYS;
            node.availability.requiredNodes = new ArrayList<>();
        }
        if (node.availability.requiredNodes == null) {
            node.availability.requiredNodes = new ArrayList<>();
        }
        return node.availability;
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

    private void setStatus(String message, boolean error) {
        statusMessage = message;
        statusColor = error ? 0xFFFF5555 : 0xFF55FF55;
        statusTicks = mc.world != null ? mc.world.getTotalWorldTime() : 0;
    }

    public void refreshFromConfig(String focusNodeId) {
        String targetId = focusNodeId != null ? focusNodeId : selectedNodeId;
        reloadNodes();
        if (targetId != null) {
            selectNodeById(targetId);
        }
        if (selectedNodeId == null && !nodes.isEmpty()) {
            selectNode(nodes.get(0));
        }
    }

    public void notifyLinkCreated(String sourceId, String targetId) {
        setStatus("Связь " + sourceId + " → " + targetId + " добавлена", false);
    }

    public void notifyLinkFailed(String message) {
        setStatus(message, true);
    }

    public void notifyLinkCancelled() {
        setStatus("Создание связи отменено", false);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isCustomNodeMode() {
        return customNodeCheck != null && customNodeCheck.isChecked();
    }

    private void updateCustomNodeState() {
        boolean custom = isCustomNodeMode();
        if (recipeIdField != null) {
            recipeIdField.setEnabled(!custom);
            if (custom) {
                recipeIdField.setText("");
            }
        }
        if (recipeSelectButton != null) {
            recipeSelectButton.enabled = !custom;
        }
        logCustomState("updateCustomNodeState");
        logSelectionState("updateCustomNodeState");
    }

    private void logCustomState(String context) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }
        boolean custom = customNodeCheck != null && customNodeCheck.isChecked();
        String fieldValue = recipeIdField != null ? recipeIdField.getText() : "<field-null>";
        String editingValue = editingNode != null ? editingNode.recipeId : "<node-null>";
        LOGGER.debug("[{}] customNodeCheck={}, recipeIdField='{}', editingNode.recipeId='{}'", context, custom, fieldValue, editingValue);
    }

    private void logSelectionState(String context) {
        if (!LOGGER.isInfoEnabled()) {
            return;
        }
        String fieldValue = recipeIdField != null ? recipeIdField.getText() : "<field-null>";
        String editingValue = editingNode != null ? editingNode.recipeId : "<node-null>";
        String pendingValue = pendingRecipe != null ? pendingRecipe.getRecipeLocation().toString() : "<none>";
        boolean customMode = isCustomNodeMode();
        LOGGER.info("[{}] customMode={}, recipeField='{}', editingNode.recipeId='{}', pendingRecipe='{}', dialogVisible={}",
                context, customMode, fieldValue, editingValue, pendingValue, creationDialogVisible);
    }

    private void openRecipePicker() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiRecipeBrowser(this, player, this::onRecipeSelected));
    }

    private void onRecipeSelected(RecipeEntry recipe) {
        LOGGER.info("RecipeTreeEditorScreen.onRecipeSelected recipe={}", recipe != null ? recipe.getRecipeLocation() : "<null>");
        if (recipe == null) {
            restoreAfterChild();
            return;
        }
        this.pendingRecipe = recipe;
        if (customNodeCheck != null) {
            customNodeCheck.setIsChecked(false);
            logCustomState("onRecipeSelected.resetToggle");
        }
        logSelectionState("onRecipeSelected.beforeApply");
        if (editingNode == null) {
            createNewNode();
        } else {
            applyPendingRecipe(false);
        }
    }

    public void setPendingRecipe(RecipeEntry recipe) {
        this.pendingRecipe = recipe;
        if (recipe != null) {
            LOGGER.info("RecipeTreeEditorScreen pending recipe set: {}", recipe.getRecipeLocation());
            populateRecipeFields(recipe);
            updatePreviewFromRecipe(recipe);
            if (customNodeCheck != null) {
                customNodeCheck.setIsChecked(false);
                updateCustomNodeState();
                logCustomState("setPendingRecipe.afterPopulate");
            }
            logSelectionState("setPendingRecipe.afterPopulate");
        }
    }

    private void populateRecipeFields(RecipeEntry recipe) {
        if (recipe == null) {
            return;
        }
        ResourceLocation location = recipe.getRecipeLocation();
        if (recipeIdField != null) {
            recipeIdField.setText(location != null ? location.toString() : "");
        }
        if (displayNameField != null && (displayNameField.getText() == null || displayNameField.getText().trim().isEmpty())) {
            displayNameField.setText(recipe.getDisplayName());
        }
        if (titleField != null && (titleField.getText() == null || titleField.getText().trim().isEmpty())) {
            titleField.setText(recipe.getNodeTitle());
        }
        if (iconField != null && (iconField.getText() == null || iconField.getText().trim().isEmpty())) {
            ResourceLocation customIcon = recipe.getCustomIcon();
            if (customIcon != null) {
                iconField.setText(customIcon.toString());
            }
        }
        if (costField != null && (costField.getText() == null || costField.getText().trim().isEmpty())) {
            costField.setText(String.valueOf(recipe.getRequiredLearningPoints()));
        }
    }

    private void updatePreviewFromRecipe(RecipeEntry recipe) {
        if (recipe == null) {
            return;
        }
        if (displayNameField != null && (displayNameField.getText() == null || displayNameField.getText().trim().isEmpty())) {
            displayNameField.setText(recipe.getDisplayName());
        }
        if (titleField != null && (titleField.getText() == null || titleField.getText().trim().isEmpty())) {
            titleField.setText(recipe.getNodeTitle());
        }
        if (costField != null && (costField.getText() == null || costField.getText().trim().isEmpty())) {
            costField.setText(String.valueOf(recipe.getRequiredLearningPoints()));
        }
    }

    void restoreAfterChild() {
        Minecraft.getMinecraft().displayGuiScreen(this);
    }

    private void drawPreviewPanel() {
        int panelX = width - PREVIEW_WIDTH - 20;
        int panelY = 40;
        int right = panelX + PREVIEW_WIDTH;
        int bottom = panelY + PREVIEW_HEIGHT;

        drawGradientRect(panelX, panelY, right, bottom, 0xAA1E1E1E, 0xAA111111);
        drawRect(panelX, panelY, right, panelY + 22, 0xFF2B2B2B);
        drawCenteredString(fontRenderer, "Предпросмотр", panelX + PREVIEW_WIDTH / 2, panelY + 7, 0xFFFFFF);

        NodeData previewData = buildPreviewData();
        RecipeEntry recipe = resolvePreviewRecipe(previewData);

        int contentX = panelX + 12;
        int contentY = panelY + 30;

        boolean iconDrawn = false;
        ResourceLocation icon = determinePreviewIcon(previewData, recipe);
        if (icon != null) {
            try {
                mc.getTextureManager().bindTexture(icon);
                drawModalRectWithCustomSizedTexture(contentX, contentY, 0, 0, 48, 48, 48, 48);
                iconDrawn = true;
            } catch (Exception ex) {
                drawCenteredString(fontRenderer, TextFormatting.RED + "Иконка не найдена", panelX + PREVIEW_WIDTH / 2, contentY + 12, 0xFFFFFF);
            }
        } else if (recipe != null) {
            ItemStack result = recipe.getRecipeResult();
            if (!result.isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(result, contentX + 16, contentY + 8);
                RenderHelper.disableStandardItemLighting();
                iconDrawn = true;
            }
        }

        if (!iconDrawn) {
            drawCenteredString(fontRenderer, TextFormatting.GRAY + "Иконка не выбрана", panelX + PREVIEW_WIDTH / 2, contentY + 20, 0xFFFFFF);
        }

        int textX = contentX + 56;
        drawString(fontRenderer, TextFormatting.YELLOW + getPreviewTitle(previewData, recipe), textX, contentY, 0xFFFFFF);
        drawString(fontRenderer, TextFormatting.GRAY + "ID: " + safeString(previewData.id), textX, contentY + 12, 0xFFFFFF);
        drawString(fontRenderer, TextFormatting.GRAY + "Tab: " + safeString(previewData.tab), textX, contentY + 24, 0xFFFFFF);
        drawString(fontRenderer, "Стоимость: " + previewData.studyCost, contentX, contentY + 64, 0xFFFFFF);

        String recipeStatus;
        if (isCustomNodeMode() || previewData.recipeId == null || previewData.recipeId.trim().isEmpty()) {
            recipeStatus = TextFormatting.AQUA + "Кастомный узел";
        } else if (recipe == null) {
            recipeStatus = TextFormatting.RED + "Рецепт не найден";
        } else {
            recipeStatus = TextFormatting.GREEN + "Рецепт: " + recipe.getRecipeLocation();
        }
        drawString(fontRenderer, recipeStatus, contentX, contentY + 78, 0xFFFFFF);

        drawString(fontRenderer, TextFormatting.GRAY + "Разблокирует: " + (previewData.unlocks != null && previewData.unlocks.nodes != null ? previewData.unlocks.nodes.size() : 0), contentX, contentY + 96, 0xFFFFFF);
        drawString(fontRenderer, TextFormatting.GRAY + "Требует: " + (previewData.availability != null && previewData.availability.requiredNodes != null ? previewData.availability.requiredNodes.size() : 0), contentX, contentY + 108, 0xFFFFFF);
    }

    private NodeData buildPreviewData() {
        NodeData data = new NodeData();
        data.id = idField != null ? idField.getText().trim() : "";
        data.tab = tabField != null && !tabField.getText().trim().isEmpty() ? tabField.getText().trim() : "default";
        data.recipeId = (!isCustomNodeMode() && recipeIdField != null) ? recipeIdField.getText().trim() : null;
        data.displayName = displayNameField != null ? emptyToNull(displayNameField.getText()) : null;
        data.nodeTitle = titleField != null ? emptyToNull(titleField.getText()) : null;
        data.customIcon = iconField != null ? emptyToNull(iconField.getText()) : null;
        data.studyCost = tryParseInt(costField != null ? costField.getText() : null, 0);
        Position pos = new Position();
        pos.x = tryParseInt(posXField != null ? posXField.getText() : null, 0);
        pos.y = tryParseInt(posYField != null ? posYField.getText() : null, 0);
        data.position = pos;
        data.grantsCraftAccess = grantAccessCheck != null && grantAccessCheck.isChecked();
        data.unlocks = ensureUnlocks(new NodeData());
        data.availability = ensureAvailability(new NodeData());
        if (requiresField != null) {
            data.availability.requiredNodes = splitList(requiresField.getText());
        }
        if (unlockNodesField != null) {
            data.unlocks.nodes = splitList(unlockNodesField.getText());
        }
        if (unlockTabsField != null) {
            data.unlocks.tabs = splitList(unlockTabsField.getText());
        }
        if (unlockPermsField != null) {
            data.unlocks.permissions = splitList(unlockPermsField.getText());
        }
        data.availability.allowIndependentStudy = allowIndependentCheck != null && allowIndependentCheck.isChecked();
        return data;
    }

    private RecipeEntry resolvePreviewRecipe(NodeData data) {
        if (data == null || data.recipeId == null || data.recipeId.trim().isEmpty()) {
            return null;
        }
        return RecipeManager.getInstance().getRecipe(data.recipeId.trim());
    }

    private ResourceLocation determinePreviewIcon(NodeData data, RecipeEntry recipe) {
        ResourceLocation icon = parseResourceSafe(data != null ? data.customIcon : null);
        if (icon != null) {
            return icon;
        }
        if (recipe != null) {
            ResourceLocation recipeIcon = recipe.getCustomIcon();
            if (recipeIcon != null) {
                return recipeIcon;
            }
        }
        return null;
    }

    private String getPreviewTitle(NodeData data, RecipeEntry recipe) {
        if (data != null && data.displayName != null && !data.displayName.trim().isEmpty()) {
            return data.displayName;
        }
        if (recipe != null) {
            return recipe.getDisplayName();
        }
        if (data != null && data.id != null && !data.id.trim().isEmpty()) {
            return data.id;
        }
        return "Новый узел";
    }

    private ResourceLocation parseResourceSafe(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return new ResourceLocation(raw.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private int tryParseInt(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String safeString(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    private void openCreationDialog() {
        creationDialogVisible = true;
        int dialogWidth = 220;
        int dialogHeight = 130;
        int centerX = width / 2;
        int centerY = height / 2;
        creationIdField = new GuiTextField(600, fontRenderer, centerX - dialogWidth / 2 + 12, centerY - 30, dialogWidth - 24, 18);
        creationIdField.setMaxStringLength(64);
        creationIdField.setText(idField.getText());
        creationIdField.setFocused(true);

        creationCostField = new GuiTextField(601, fontRenderer, centerX - dialogWidth / 2 + 12, centerY + 4, dialogWidth - 24, 18);
        creationCostField.setMaxStringLength(6);
        creationCostField.setText(costField.getText());

        creationConfirmButton = new GuiButton(602, centerX - 90, centerY + 40, 80, 20, "Создать");
        creationCancelButton = new GuiButton(603, centerX + 10, centerY + 40, 80, 20, "Отмена");
    }

    private void closeCreationDialog() {
        creationDialogVisible = false;
        creationIdField = null;
        creationCostField = null;
        creationConfirmButton = null;
        creationCancelButton = null;
    }

    private void confirmCreationDialog() {
        if (!creationDialogVisible) {
            return;
        }
        String idValue = creationIdField != null ? creationIdField.getText().trim() : idField.getText().trim();
        if (idValue.isEmpty()) {
            setStatus("ID не может быть пустым", true);
            return;
        }
        idField.setText(idValue);
        if (creationCostField != null) {
            String costValue = creationCostField.getText().trim();
            if (!costValue.isEmpty()) {
                costField.setText(costValue);
            }
        }
        closeCreationDialog();
        saveCurrentNode();
    }

    private int drawFieldWithLabel(int labelY, String label, GuiTextField field) {
        drawString(fontRenderer, label, FORM_LEFT, labelY, 0xFFFFFF);
        field.x = FORM_LEFT;
        field.y = labelY + 12;
        field.drawTextBox();
        return labelY + FIELD_GAP;
    }

    private void drawSectionTitle(String title, int y) {
        drawString(fontRenderer, TextFormatting.AQUA + title, FORM_LEFT, y, 0xFFFFFF);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        GuiTextField[] fields = {idField, tabField, recipeIdField, displayNameField, titleField, iconField,
                costField, posXField, posYField, requiresField, unlockNodesField, unlockTabsField, unlockPermsField};
        for (GuiTextField field : fields) {
            if (field.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
        }
        if (creationDialogVisible && creationIdField != null) {
            if (creationIdField.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
            if (creationCostField != null && creationCostField.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
        }
        if (creationDialogVisible && (keyCode == 28 || keyCode == 156)) { // Enter
            confirmCreationDialog();
            return;
        }
        if (creationDialogVisible && keyCode == 1) { // ESC
            closeCreationDialog();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (creationDialogVisible) {
            if (creationIdField != null) {
                creationIdField.mouseClicked(mouseX, mouseY, mouseButton);
            }
            if (creationCostField != null) {
                creationCostField.mouseClicked(mouseX, mouseY, mouseButton);
            }
            if (creationConfirmButton != null && creationConfirmButton.mousePressed(mc, mouseX, mouseY)) {
                confirmCreationDialog();
                return;
            }
            if (creationCancelButton != null && creationCancelButton.mousePressed(mc, mouseX, mouseY)) {
                closeCreationDialog();
                return;
            }
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
        GuiTextField[] fields = {idField, tabField, recipeIdField, displayNameField, titleField, iconField,
                costField, posXField, posYField, requiresField, unlockNodesField, unlockTabsField, unlockPermsField};
        for (GuiTextField field : fields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (nodeList != null) {
            nodeList.handleMouseInput();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, TextFormatting.GOLD + "Редактор дерева рецептов", width / 2, 10, 0xFFFFFF);
        drawString(fontRenderer, TextFormatting.YELLOW + "Узлы", 20, 25, 0xFFFFFF);
        if (nodeList != null) {
            nodeList.drawScreen(mouseX, mouseY, partialTicks);
        }

        int labelY = 35;
        drawSectionTitle("Основные настройки", labelY - 18);
        labelY = drawFieldWithLabel(labelY, "ID", idField);
        labelY = drawFieldWithLabel(labelY, "Tab ID", tabField);
        if (tabSelectButton != null) {
            tabSelectButton.x = FORM_LEFT + FIELD_WIDTH + 10;
            tabSelectButton.y = tabField.y - 2;
            tabSelectButton.enabled = !creationDialogVisible && RecipeTreeConfigManager.getInstance().getTabs().size() > 0;
            tabSelectButton.drawButton(mc, mouseX, mouseY, partialTicks);
        }
        labelY = drawFieldWithLabel(labelY, "Recipe ID", recipeIdField);
        if (recipeSelectButton != null) {
            recipeSelectButton.x = FORM_LEFT + FIELD_WIDTH + 10;
            recipeSelectButton.y = recipeIdField.y - 2;
            recipeSelectButton.enabled = !creationDialogVisible && !isCustomNodeMode();
            recipeSelectButton.drawButton(mc, mouseX, mouseY, partialTicks);
        }
        if (customNodeCheck != null) {
            customNodeCheck.x = FORM_LEFT;
            customNodeCheck.y = labelY + 2;
            customNodeCheck.drawButton(mc, mouseX, mouseY, partialTicks);
            labelY += FIELD_GAP;
        }

        labelY += SECTION_GAP;
        drawSectionTitle("Внешний вид", labelY - 18);
        labelY = drawFieldWithLabel(labelY, "Отображаемое имя", displayNameField);
        labelY = drawFieldWithLabel(labelY, "Заголовок узла", titleField);
        labelY = drawFieldWithLabel(labelY, "Custom icon", iconField);
        if (iconSelectButton != null) {
            iconSelectButton.x = FORM_LEFT + FIELD_WIDTH + 10;
            iconSelectButton.y = iconField.y - 2;
            iconSelectButton.enabled = !creationDialogVisible;
            iconSelectButton.drawButton(mc, mouseX, mouseY, partialTicks);
        }
        labelY = drawFieldWithLabel(labelY, "Стоимость", costField);
        labelY = drawFieldWithLabel(labelY, "Позиция X", posXField);
        labelY = drawFieldWithLabel(labelY, "Позиция Y", posYField);

        labelY += SECTION_GAP;
        drawSectionTitle("Доступ", labelY - 18);
        modeButton.x = FORM_LEFT;
        modeButton.y = labelY + 2;
        modeButton.drawButton(mc, mouseX, mouseY, partialTicks);
        labelY += FIELD_GAP;
        allowIndependentCheck.x = FORM_LEFT;
        allowIndependentCheck.y = labelY + 2;
        allowIndependentCheck.drawButton(mc, mouseX, mouseY, partialTicks);
        labelY += FIELD_GAP;
        grantAccessCheck.x = FORM_LEFT;
        grantAccessCheck.y = labelY + 2;
        grantAccessCheck.drawButton(mc, mouseX, mouseY, partialTicks);

        labelY += SECTION_GAP;
        drawSectionTitle("Связи и разблокировки", labelY - 18);
        labelY = drawFieldWithLabel(labelY, "Требуемые узлы", requiresField);
        labelY = drawFieldWithLabel(labelY, "Unlock nodes", unlockNodesField);
        labelY = drawFieldWithLabel(labelY, "Unlock tabs", unlockTabsField);
        drawFieldWithLabel(labelY, "Unlock permissions", unlockPermsField);

        drawString(fontRenderer, TextFormatting.GRAY + "Shift+ЛКМ по выходу → вход — создать связь; ПКМ — отмена", FORM_LEFT, height - 86, 0xFFFFFF);

        if (statusMessage != null) {
            drawCenteredString(fontRenderer, statusMessage, width / 2, height - 60, statusColor);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawPreviewPanel();

        if (creationDialogVisible) {
            drawRect(0, 0, width, height, 0xA0000000);
            int dialogWidth = 220;
            int dialogHeight = 130;
            int centerX = width / 2;
            int centerY = height / 2;
            int left = centerX - dialogWidth / 2;
            int top = centerY - dialogHeight / 2;
            drawGradientRect(left, top, left + dialogWidth, top + dialogHeight, 0xFF2A2A2A, 0xFF1E1E1E);
            drawRect(left, top, left + dialogWidth, top + 20, 0xFF444444);
            drawCenteredString(fontRenderer, "Создание узла", centerX, top + 6, 0xFFFFFF);

            drawString(fontRenderer, "ID узла", left + 12, centerY - 42, 0xFFFFFF);
            if (creationIdField != null) {
                creationIdField.drawTextBox();
            }
            drawString(fontRenderer, "Стоимость", left + 12, centerY - 8, 0xFFFFFF);
            if (creationCostField != null) {
                creationCostField.drawTextBox();
            }
            drawString(fontRenderer, TextFormatting.GRAY + "Вкладка: " + tabField.getText(), left + 12, centerY + 16, 0xFFFFFF);

            if (creationConfirmButton != null) {
                creationConfirmButton.drawButton(mc, mouseX, mouseY, partialTicks);
            }
            if (creationCancelButton != null) {
                creationCancelButton.drawButton(mc, mouseX, mouseY, partialTicks);
            }
        }
    }

    private class NodeList extends GuiSlot {
        NodeList() {
            super(RecipeTreeEditorScreen.this.mc, LIST_WIDTH, RecipeTreeEditorScreen.this.height,
                    40, RecipeTreeEditorScreen.this.height - 60, 20);
        }

        @Override
        protected int getSize() {
            return nodes.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
            if (index >= 0 && index < nodes.size()) {
                selectNode(nodes.get(index));
            }
        }

        @Override
        protected boolean isSelected(int index) {
            return index >= 0 && index < nodes.size() && Objects.equals(nodes.get(index).id, selectedNodeId);
        }

        @Override
        protected void drawBackground() {
        }

        @Override
        protected void drawSlot(int idx, int right, int top, int heightIn, int mouseXIn, int mouseYIn, float partialTicks) {
            if (idx < 0 || idx >= nodes.size()) {
                return;
            }
            NodeData node = nodes.get(idx);
            String title = node.displayName != null && !node.displayName.trim().isEmpty()
                ? node.displayName
                : (node.id != null ? node.id : "<без id>");
            RecipeTreeEditorScreen.this.fontRenderer.drawString(title, this.left + 3, top + 2, 0xFFFFFF);
            String tab = node.tab != null ? node.tab : "default";
            RecipeTreeEditorScreen.this.fontRenderer.drawString(TextFormatting.GRAY + tab, this.left + 3, top + 12, 0xAAAAAA);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.width = LIST_WIDTH;
            this.left = 10;
            this.right = this.left + LIST_WIDTH;
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected int getScrollBarX() {
            return this.right - 6;
        }
    }
}
