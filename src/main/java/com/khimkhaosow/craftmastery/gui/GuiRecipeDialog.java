package com.khimkhaosow.craftmastery.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import java.io.IOException;
import java.util.List;

public class GuiRecipeDialog extends GuiScreen {
    private static final ResourceLocation DIALOG_TEXTURE = new ResourceLocation("craftmastery", "textures/gui/dialog.png");
    private static final int DIALOG_WIDTH = 256;
    private static final int DIALOG_HEIGHT = 200;
    
    private final GuiScreen parentScreen;
    private final RecipeEntry editingRecipe;
    private final boolean isNewRecipe;
    
    private GuiTextField nameField;
    private GuiTextField requiredPointsField;
    private GuiTextField requiredLevelField;
    private GuiButton requirementsButton;
    private GuiButton unlockablesButton;
    private GuiButton saveButton;
    private GuiButton cancelButton;
    
    private List<RecipeEntry> requirements;
    private List<RecipeEntry> unlockables;
    private IRecipe selectedRecipe;
    
    public GuiRecipeDialog(GuiScreen parentScreen, RecipeEntry recipe) {
        this.parentScreen = parentScreen;
        this.editingRecipe = recipe;
        this.isNewRecipe = (recipe == null);
    }
    
    @Override
    public void initGui() {
        int centerX = width / 2;
        int centerY = height / 2;
        int left = centerX - DIALOG_WIDTH / 2;
        int top = centerY - DIALOG_HEIGHT / 2;
        
        // Поля ввода
        nameField = new GuiTextField(0, fontRenderer, left + 10, top + 30, 236, 20);
        requiredPointsField = new GuiTextField(1, fontRenderer, left + 10, top + 70, 116, 20);
        requiredLevelField = new GuiTextField(2, fontRenderer, left + 130, top + 70, 116, 20);
        
        if (!isNewRecipe) {
            nameField.setText(editingRecipe.getDisplayName());
            requiredPointsField.setText(String.valueOf(editingRecipe.getRequiredLearningPoints()));
            requiredLevelField.setText(String.valueOf(editingRecipe.getRequiredLevel()));
            requirements = editingRecipe.getRecipeRequirements();
            unlockables = editingRecipe.getRecipeUnlockables();
        }
        
        // Кнопки
        buttonList.clear();
        buttonList.add(requirementsButton = new GuiButton(3, left + 10, top + 100, 236, 20, "Требования"));
        buttonList.add(unlockablesButton = new GuiButton(4, left + 10, top + 125, 236, 20, "Что разблокирует"));
        buttonList.add(saveButton = new GuiButton(5, left + 10, top + 160, 116, 20, "Сохранить"));
        buttonList.add(cancelButton = new GuiButton(6, left + 130, top + 160, 116, 20, "Отмена"));
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        int centerX = width / 2;
        int centerY = height / 2;
        int left = centerX - DIALOG_WIDTH / 2;
        int top = centerY - DIALOG_HEIGHT / 2;
        
        // Фон диалога
        mc.getTextureManager().bindTexture(DIALOG_TEXTURE);
        drawTexturedModalRect(left, top, 0, 0, DIALOG_WIDTH, DIALOG_HEIGHT);
        
        // Заголовок
        String title = isNewRecipe ? "Новый рецепт" : "Редактирование рецепта";
        drawCenteredString(fontRenderer, title, centerX, top + 10, 0xFFFFFF);
        
        // Метки полей
        drawString(fontRenderer, "Название:", left + 10, top + 20, 0xFFFFFF);
        drawString(fontRenderer, "Очки:", left + 10, top + 60, 0xFFFFFF);
        drawString(fontRenderer, "Уровень:", left + 130, top + 60, 0xFFFFFF);
        
        // Поля ввода
        nameField.drawTextBox();
        requiredPointsField.drawTextBox();
        requiredLevelField.drawTextBox();
        
        // Кнопки
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        // Отображаем выбранный рецепт
        if (selectedRecipe != null) {
            ItemStack result = selectedRecipe.getRecipeOutput();
            if (!result.isEmpty()) {
                mc.getRenderItem().renderItemAndEffectIntoGUI(
                    result,
                    left + DIALOG_WIDTH - 26,
                    top + 32
                );
            }
        }
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        nameField.mouseClicked(mouseX, mouseY, mouseButton);
        requiredPointsField.mouseClicked(mouseX, mouseY, mouseButton);
        requiredLevelField.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
            mc.displayGuiScreen(parentScreen);
            return;
        }
        
        if (nameField.isFocused()) {
            nameField.textboxKeyTyped(typedChar, keyCode);
        }
        if (requiredPointsField.isFocused()) {
            if (Character.isDigit(typedChar) || keyCode == 14) { // Цифры или backspace
                requiredPointsField.textboxKeyTyped(typedChar, keyCode);
            }
        }
        if (requiredLevelField.isFocused()) {
            if (Character.isDigit(typedChar) || keyCode == 14) {
                requiredLevelField.textboxKeyTyped(typedChar, keyCode);
            }
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == requirementsButton) {
            openRequirementsDialog();
        } else if (button == unlockablesButton) {
            openUnlockablesDialog();
        } else if (button == saveButton) {
            if (validateAndSave()) {
                mc.displayGuiScreen(parentScreen);
            }
        } else if (button == cancelButton) {
            mc.displayGuiScreen(parentScreen);
        }
    }
    
    private void openRequirementsDialog() {
        // Открываем диалог выбора требуемых рецептов
        mc.displayGuiScreen(new GuiRecipeSelector(
            this,
            requirements,
            selected -> requirements = selected
        ));
    }
    
    private void openUnlockablesDialog() {
        // Открываем диалог выбора разблокируемых рецептов
        mc.displayGuiScreen(new GuiRecipeSelector(
            this,
            unlockables,
            selected -> unlockables = selected
        ));
    }
    
    private boolean validateAndSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            // Показываем ошибку
            return false;
        }
        
        int points;
        try {
            points = Integer.parseInt(requiredPointsField.getText().trim());
        } catch (NumberFormatException e) {
            // Показываем ошибку
            return false;
        }
        
        int level;
        try {
            level = Integer.parseInt(requiredLevelField.getText().trim());
        } catch (NumberFormatException e) {
            // Показываем ошибку
            return false;
        }
        
        if (isNewRecipe) {
            RecipeEntry newRecipe = new RecipeEntry(
                new ResourceLocation("craftmastery", name.toLowerCase().replace(" ", "_"))
            );
            newRecipe.setRecipe(selectedRecipe);
            newRecipe.setDisplayName(name);
            newRecipe.setRequiredLearningPoints(points);
            newRecipe.setRequiredLevel(level);
            newRecipe.setRecipeRequirements(requirements);
            newRecipe.setRecipeUnlockables(unlockables);
            RecipeManager.getInstance().registerRecipe(newRecipe);
        } else {
            editingRecipe.setDisplayName(name);
            editingRecipe.setRequiredLearningPoints(points);
            editingRecipe.setRequiredLevel(level);
            editingRecipe.setRecipeRequirements(requirements);
            editingRecipe.setRecipeUnlockables(unlockables);
        }
        
        return true;
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}