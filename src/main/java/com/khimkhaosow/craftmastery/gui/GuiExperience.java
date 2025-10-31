package com.khimkhaosow.craftmastery.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.khimkhaosow.craftmastery.experience.ExperienceType;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.util.RenderHelper;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

public class GuiExperience extends GuiScreen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("craftmastery:textures/gui/experience.png");
    private static final int GUI_WIDTH = 248;
    private static final int GUI_HEIGHT = 166;
    
    private PlayerExperienceData experienceData;
    private float levelProgress;
    private int guiLeft;
    private int guiTop;
    private List<ExperienceType> displayedTypes;
    
    public GuiExperience(PlayerExperienceData data, float progress) {
        this.experienceData = data;
        this.levelProgress = progress;
        this.displayedTypes = new ArrayList<>(ExperienceType.values().length);
        for (ExperienceType type : ExperienceType.values()) {
            this.displayedTypes.add(type);
        }
    }
    
    @Override
    public void initGui() {
        super.initGui();
        
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Добавляем кнопки для сортировки и фильтрации
        this.buttonList.add(new GuiButton(0, guiLeft + 10, guiTop + GUI_HEIGHT - 25, 70, 20, "Сортировка"));
        this.buttonList.add(new GuiButton(1, guiLeft + 90, guiTop + GUI_HEIGHT - 25, 70, 20, "Фильтр"));
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        
        // Отрисовка фона
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        
        // Отрисовка заголовка
        String title = TextFormatting.GOLD + "Опыт и Навыки";
        fontRenderer.drawString(title, guiLeft + (GUI_WIDTH - fontRenderer.getStringWidth(title)) / 2, guiTop + 6, 0x404040);
        
        // Отрисовка общего уровня
        String levelText = "Уровень " + experienceData.getLevel();
        fontRenderer.drawString(levelText, guiLeft + 10, guiTop + 20, 0x404040);
        
        // Отрисовка прогресс-бара
        drawExperienceBar(guiLeft + 10, guiTop + 32, GUI_WIDTH - 20, 8);
        
        // Отрисовка статистики по типам опыта
        int y = guiTop + 50;
        for (ExperienceType type : displayedTypes) {
            drawExperienceType(type, guiLeft + 10, y);
            y += 20;
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        // Отрисовка тултипов
        drawTooltips(mouseX, mouseY);
    }
    
    private void drawExperienceBar(int x, int y, int width, int height) {
        // Фон прогресс-бара
        drawRect(x, y, x + width, y + height, 0xFF555555);
        
        // Заполненная часть
        int filledWidth = (int)(width * levelProgress);
        int color = 0xFF00FF00; // Зеленый цвет
        drawRect(x, y, x + filledWidth, y + height, color);
        
        // Процент прогресса
        String progress = String.format("%.1f%%", levelProgress * 100);
        fontRenderer.drawString(progress, 
            x + (width - fontRenderer.getStringWidth(progress)) / 2, 
            y + (height - fontRenderer.FONT_HEIGHT) / 2, 
            0xFFFFFF);
    }
    
    private void drawExperienceType(ExperienceType type, int x, int y) {
        // Название типа опыта
        fontRenderer.drawString(type.getDisplayName(), x, y, 0x404040);
        
        // Количество опыта
        float experience = experienceData.getExperience(type);
        String expText = String.format("%.1f", experience);
        fontRenderer.drawString(expText, 
            x + 150 - fontRenderer.getStringWidth(expText), 
            y, 
            0x404040);
    }
    
    private void drawTooltips(int mouseX, int mouseY) {
        // Проверяем, находится ли курсор над типом опыта
        int typeY = guiTop + 50;
        for (ExperienceType type : displayedTypes) {
            if (mouseX >= guiLeft + 10 && mouseX <= guiLeft + GUI_WIDTH - 10 &&
                mouseY >= typeY && mouseY < typeY + 16) {
                
                List<String> tooltip = new ArrayList<>();
                tooltip.add(TextFormatting.YELLOW + type.getDisplayName());
                tooltip.add(TextFormatting.GRAY + type.getDescription());
                tooltip.add("");
                tooltip.add(TextFormatting.GREEN + "Опыт: " + String.format("%.1f", experienceData.getExperience(type)));
                
                drawHoveringText(tooltip, mouseX, mouseY);
                break;
            }
            typeY += 20;
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: // Сортировка
                sortExperienceTypes();
                break;
            case 1: // Фильтр
                filterExperienceTypes();
                break;
        }
    }
    
    private void sortExperienceTypes() {
        displayedTypes.sort((a, b) -> {
            float expA = experienceData.getExperience(a);
            float expB = experienceData.getExperience(b);
            return Float.compare(expB, expA); // Сортировка по убыванию
        });
    }
    
    private void filterExperienceTypes() {
        // TODO: Добавить диалог фильтрации
    }
    
    public void updateData(PlayerExperienceData data, float progress) {
        this.experienceData = data;
        this.levelProgress = progress;
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}