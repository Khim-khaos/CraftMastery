package com.khimkhaosow.craftmastery.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public class GuiRecipeSelector extends GuiScreen {
    private static final ResourceLocation SELECTOR_TEXTURE = new ResourceLocation("craftmastery", "textures/gui/selector.png");
    private static final int SELECTOR_WIDTH = 256;
    private static final int SELECTOR_HEIGHT = 200;
    private static final int RECIPES_PER_PAGE = 6;
    
    private final GuiScreen parentScreen;
    private final List<RecipeEntry> selectedRecipes;
    private final Consumer<List<RecipeEntry>> onSelect;
    
    private List<RecipeEntry> availableRecipes;
    private int currentPage = 0;
    private GuiButton prevButton;
    private GuiButton nextButton;
    private GuiButton doneButton;
    
    public GuiRecipeSelector(GuiScreen parentScreen, List<RecipeEntry> selectedRecipes, Consumer<List<RecipeEntry>> onSelect) {
        this.parentScreen = parentScreen;
        this.selectedRecipes = new ArrayList<>(selectedRecipes != null ? selectedRecipes : new ArrayList<>());
        this.onSelect = onSelect;
        this.availableRecipes = RecipeManager.getInstance().getAllRecipes();
    }
    
    @Override
    public void initGui() {
        int centerX = width / 2;
        int centerY = height / 2;
        int left = centerX - SELECTOR_WIDTH / 2;
        int top = centerY - SELECTOR_HEIGHT / 2;
        
        buttonList.clear();
        
        // Кнопки рецептов
        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            int recipeIndex = currentPage * RECIPES_PER_PAGE + i;
            if (recipeIndex < availableRecipes.size()) {
                RecipeEntry recipe = availableRecipes.get(recipeIndex);
                GuiButton recipeButton = new GuiButton(
                    i,
                    left + 10,
                    top + 30 + i * 25,
                    236,
                    20,
                    recipe.getDisplayName()
                );
                buttonList.add(recipeButton);
            }
        }
        
        // Кнопки навигации
        buttonList.add(prevButton = new GuiButton(100, left + 10, top + 160, 73, 20, "Назад"));
        buttonList.add(nextButton = new GuiButton(101, left + 91, top + 160, 73, 20, "Далее"));
        buttonList.add(doneButton = new GuiButton(102, left + 173, top + 160, 73, 20, "Готово"));
        
        updateNavigationButtons();
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        int centerX = width / 2;
        int centerY = height / 2;
        int left = centerX - SELECTOR_WIDTH / 2;
        int top = centerY - SELECTOR_HEIGHT / 2;
        
        // Фон
        mc.getTextureManager().bindTexture(SELECTOR_TEXTURE);
        drawTexturedModalRect(left, top, 0, 0, SELECTOR_WIDTH, SELECTOR_HEIGHT);
        
        // Заголовок
        drawCenteredString(fontRenderer, "Выбор рецептов", centerX, top + 10, 0xFFFFFF);
        
        // Список рецептов
        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            int recipeIndex = currentPage * RECIPES_PER_PAGE + i;
            if (recipeIndex < availableRecipes.size()) {
                RecipeEntry recipe = availableRecipes.get(recipeIndex);
                boolean isSelected = selectedRecipes.contains(recipe);
                
                // Фон выбранного рецепта
                if (isSelected) {
                    drawRect(
                        left + 10,
                        top + 30 + i * 25,
                        left + 246,
                        top + 50 + i * 25,
                        0x3300FF00
                    );
                }
                
                // Иконка рецепта
                ItemStack result = recipe.getRecipeResult();
                if (!result.isEmpty()) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(left + 15, top + 32 + i * 25, 0);
                    GlStateManager.scale(0.8f, 0.8f, 0.8f);
                    mc.getRenderItem().renderItemAndEffectIntoGUI(result, 0, 0);
                    GlStateManager.popMatrix();
                }
            }
        }
        
        // Статистика
        String stats = String.format("Выбрано: %d", selectedRecipes.size());
        drawString(fontRenderer, stats, left + 10, top + 185, 0xFFFFFF);
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == prevButton && currentPage > 0) {
            currentPage--;
            initGui();
        } else if (button == nextButton && (currentPage + 1) * RECIPES_PER_PAGE < availableRecipes.size()) {
            currentPage++;
            initGui();
        } else if (button == doneButton) {
            onSelect.accept(selectedRecipes);
            mc.displayGuiScreen(parentScreen);
        } else if (button.id < RECIPES_PER_PAGE) {
            // Клик по рецепту
            int recipeIndex = currentPage * RECIPES_PER_PAGE + button.id;
            if (recipeIndex < availableRecipes.size()) {
                RecipeEntry recipe = availableRecipes.get(recipeIndex);
                if (selectedRecipes.contains(recipe)) {
                    selectedRecipes.remove(recipe);
                } else {
                    selectedRecipes.add(recipe);
                }
            }
        }
    }
    
    private void updateNavigationButtons() {
        prevButton.enabled = currentPage > 0;
        nextButton.enabled = (currentPage + 1) * RECIPES_PER_PAGE < availableRecipes.size();
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
            mc.displayGuiScreen(parentScreen);
        }
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}