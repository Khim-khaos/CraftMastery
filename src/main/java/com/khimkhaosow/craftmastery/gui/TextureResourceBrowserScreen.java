package com.khimkhaosow.craftmastery.gui;

import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.NodeData;
import com.khimkhaosow.craftmastery.util.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Простое окно выбора текстур для иконки узла.
 */
public class TextureResourceBrowserScreen extends GuiScreen {

    private static final String[] DEFAULT_TEXTURES = new String[] {
            "minecraft:textures/items/book.png",
            "minecraft:textures/items/diamond.png",
            "minecraft:textures/items/emerald.png",
            "minecraft:textures/items/iron_ingot.png",
            "minecraft:textures/gui/icons.png",
            Reference.MOD_ID + ":textures/gui/node_unlocked.png",
            Reference.MOD_ID + ":textures/gui/node_studied.png"
    };

    private final RecipeTreeEditorScreen parent;
    private final List<String> allResources;
    private List<String> filtered;
    private ResourceLocation previewTexture;
    private GuiTextField searchField;
    private IconList iconList;
    private GuiButton selectButton;
    private GuiButton cancelButton;
    private int selectedIndex = -1;

    public TextureResourceBrowserScreen(RecipeTreeEditorScreen parent) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.allResources = collectResources();
        this.filtered = new ArrayList<>(allResources);
    }

    private List<String> collectResources() {
        Set<String> resources = new LinkedHashSet<>();
        for (String preset : DEFAULT_TEXTURES) {
            resources.add(preset);
        }
        for (NodeData node : RecipeTreeConfigManager.getInstance().getNodes()) {
            if (node == null || node.customIcon == null || node.customIcon.trim().isEmpty()) {
                continue;
            }
            resources.add(node.customIcon.trim());
        }
        resources.add("minecraft:textures/items/paper.png");
        resources.add("minecraft:textures/items/experience_bottle.png");
        return new ArrayList<>(resources);
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int top = 32;
        searchField = new GuiTextField(0, fontRenderer, width / 2 - 120, top, 240, 18);
        searchField.setMaxStringLength(256);
        searchField.setFocused(true);

        iconList = new IconList();
        iconList.registerScrollButtons(7, 8);

        int buttonY = height - 40;
        selectButton = new GuiButton(1, width / 2 - 110, buttonY, 100, 20, "Выбрать");
        cancelButton = new GuiButton(2, width / 2 + 10, buttonY, 100, 20, "Отмена");
        buttonList.add(selectButton);
        buttonList.add(cancelButton);
        updateButtons();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (iconList != null) {
            iconList.handleMouseInput();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField != null && searchField.textboxKeyTyped(typedChar, keyCode)) {
            applyFilter();
            return;
        }
        switch (keyCode) {
            case 1: // ESC
                returnToParent();
                return;
            case 28:
            case 156:
                confirmSelection();
                return;
            default:
                break;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchField != null) {
            searchField.updateCursorCounter();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (searchField != null) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1:
                confirmSelection();
                break;
            case 2:
                returnToParent();
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, TextFormatting.GOLD + "Выбор иконки", width / 2, 12, 0xFFFFFF);
        if (searchField != null) {
            drawString(fontRenderer, "Поиск", width / 2 - 120, 22, 0xFFFFFF);
            searchField.drawTextBox();
        }
        if (iconList != null) {
            iconList.drawScreen(mouseX, mouseY, partialTicks);
        }
        drawPreview();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPreview() {
        if (selectedIndex < 0 || selectedIndex >= filtered.size()) {
            return;
        }
        String resource = filtered.get(selectedIndex);
        ResourceLocation location = parse(resource);
        if (location == null) {
            return;
        }
        previewTexture = location;

        int previewSize = 64;
        int centerX = width - 100;
        int centerY = 80;
        drawString(fontRenderer, TextFormatting.YELLOW + "Предпросмотр", centerX - previewSize / 2, centerY - previewSize - 12, 0xFFFFFF);

        TextureManager textureManager = mc.getTextureManager();
        try {
            textureManager.bindTexture(location);
            GlStateManager.color(1F, 1F, 1F, 1F);
            drawModalRectWithCustomSizedTexture(centerX - previewSize / 2, centerY - previewSize / 2,
                    0, 0, previewSize, previewSize, previewSize, previewSize);
        } catch (Exception ex) {
            drawCenteredString(fontRenderer, TextFormatting.RED + "Не удалось загрузить", centerX, centerY, 0xFFFFFF);
        }
        drawCenteredString(fontRenderer, TextFormatting.GRAY + resource, centerX, centerY + previewSize / 2 + 8, 0xFFFFFF);
    }

    private void confirmSelection() {
        if (selectedIndex < 0 || selectedIndex >= filtered.size()) {
            return;
        }
        ResourceLocation location = parse(filtered.get(selectedIndex));
        if (location == null) {
            return;
        }
        parent.onIconSelected(location);
        returnToParent();
    }

    private void returnToParent() {
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    private void applyFilter() {
        String query = searchField.getText().trim().toLowerCase();
        filtered = allResources.stream()
                .filter(path -> query.isEmpty() || path.toLowerCase().contains(query))
                .collect(Collectors.toList());
        selectedIndex = -1;
        updateButtons();
    }

    private void updateButtons() {
        if (selectButton != null) {
            selectButton.enabled = selectedIndex >= 0 && selectedIndex < filtered.size();
        }
    }

    private ResourceLocation parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return new ResourceLocation(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private class IconList extends GuiSlot {

        IconList() {
            super(TextureResourceBrowserScreen.this.mc, TextureResourceBrowserScreen.this.width - 40,
                    TextureResourceBrowserScreen.this.height - 100, 50,
                    TextureResourceBrowserScreen.this.height - 60, 22);
        }

        @Override
        protected int getSize() {
            return filtered.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
            if (index < 0 || index >= filtered.size()) {
                return;
            }
            selectedIndex = index;
            previewTexture = parse(filtered.get(index));
            updateButtons();
            if (doubleClick) {
                confirmSelection();
            }
        }

        @Override
        protected boolean isSelected(int index) {
            return index == selectedIndex;
        }

        @Override
        protected void drawBackground() {
            // already handled by parent background
        }

        @Override
        protected void drawSlot(int idx, int right, int top, int heightIn, int mouseXIn, int mouseYIn, float partialTicks) {
            if (idx < 0 || idx >= filtered.size()) {
                return;
            }
            String resource = filtered.get(idx);
            drawString(fontRenderer, resource, this.left + 4, top + 6, 0xFFFFFF);
        }
    }
}
