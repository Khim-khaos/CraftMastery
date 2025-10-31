package com.khimkhaosow.craftmastery.gui;

import com.khimkhaosow.craftmastery.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiCheckBox;

import java.io.IOException;

/**
 * Simple screen for per-player client settings (HUD and notifications).
 */
public class GuiPlayerSettings extends GuiScreen {
    private static final int BTN_SAVE = 5010;
    private static final int BTN_CANCEL = 5011;

    private final GuiScreen parent;

    private GuiCheckBox showExpNotifications;
    private GuiCheckBox showPointsNotifications;
    private GuiCheckBox showExperienceHud;
    private GuiCheckBox showLevelHud;
    private GuiTextField hudScaleField;
    private GuiTextField hudXOffsetField;
    private GuiTextField hudYOffsetField;

    private String statusMessage;
    private int statusColor = 0xFFFFFFFF;

    public GuiPlayerSettings(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int left = width / 2 - 120;
        int top = 40;

        showExpNotifications = new GuiCheckBox(0, left, top, "Показывать уведомления опыта", ModConfig.showExperienceNotifications);
        buttonList.add(showExpNotifications);
        top += 22;

        showPointsNotifications = new GuiCheckBox(1, left, top, "Показывать уведомления очков", ModConfig.showPointsNotifications);
        buttonList.add(showPointsNotifications);
        top += 22;

        showExperienceHud = new GuiCheckBox(2, left, top, "Показывать полосу опыта", ModConfig.showExperience);
        buttonList.add(showExperienceHud);
        top += 22;

        showLevelHud = new GuiCheckBox(3, left, top, "Показывать уровень", ModConfig.showLevel);
        buttonList.add(showLevelHud);
        top += 30;

        hudScaleField = createField(left, top, String.valueOf(ModConfig.hudScale));
        top += 24;
        hudXOffsetField = createField(left, top, String.valueOf(ModConfig.hudXOffset));
        top += 24;
        hudYOffsetField = createField(left, top, String.valueOf(ModConfig.hudYOffset));

        buttonList.add(new GuiButton(BTN_SAVE, width / 2 - 110, height - 40, 100, 20, "Сохранить"));
        buttonList.add(new GuiButton(BTN_CANCEL, width / 2 + 10, height - 40, 100, 20, "Отмена"));
    }

    private GuiTextField createField(int x, int y, String value) {
        GuiTextField field = new GuiTextField(0, fontRenderer, x, y, 120, 18);
        field.setMaxStringLength(16);
        field.setText(value);
        return field;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (hudScaleField.textboxKeyTyped(typedChar, keyCode)) return;
        if (hudXOffsetField.textboxKeyTyped(typedChar, keyCode)) return;
        if (hudYOffsetField.textboxKeyTyped(typedChar, keyCode)) return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        hudScaleField.mouseClicked(mouseX, mouseY, mouseButton);
        hudXOffsetField.mouseClicked(mouseX, mouseY, mouseButton);
        hudYOffsetField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        hudScaleField.updateCursorCounter();
        hudXOffsetField.updateCursorCounter();
        hudYOffsetField.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) {
            return;
        }
        if (button.id == BTN_SAVE) {
            if (applySettings()) {
                Minecraft.getMinecraft().displayGuiScreen(parent);
            }
        } else if (button.id == BTN_CANCEL) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
        }
    }

    private boolean applySettings() {
        try {
            float scale = Float.parseFloat(hudScaleField.getText().trim());
            int xOffset = Integer.parseInt(hudXOffsetField.getText().trim());
            int yOffset = Integer.parseInt(hudYOffsetField.getText().trim());

            if (scale < 0.5f || scale > 2.0f) {
                setStatus("Масштаб HUD должен быть 0.5-2.0", true);
                return false;
            }

            ModConfig.applyGuiSettings(
                    showExpNotifications.isChecked(),
                    showPointsNotifications.isChecked(),
                    showExperienceHud.isChecked(),
                    showLevelHud.isChecked(),
                    scale,
                    xOffset,
                    yOffset
            );
            setStatus("Настройки сохранены", false);
            return true;
        } catch (NumberFormatException ex) {
            setStatus("Некорректные числовые значения", true);
            return false;
        }
    }

    private void setStatus(String message, boolean error) {
        statusMessage = message;
        statusColor = error ? 0xFFFF5555 : 0xFF55FF55;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, TextFormatting.GOLD + "Настройки игрока", width / 2, 15, 0xFFFFFF);

        showExpNotifications.drawButton(mc, mouseX, mouseY, partialTicks);
        showPointsNotifications.drawButton(mc, mouseX, mouseY, partialTicks);
        showExperienceHud.drawButton(mc, mouseX, mouseY, partialTicks);
        showLevelHud.drawButton(mc, mouseX, mouseY, partialTicks);

        drawString(fontRenderer, "Масштаб HUD", hudScaleField.x, hudScaleField.y - 12, 0xFFFFFF);
        hudScaleField.drawTextBox();
        drawString(fontRenderer, "Смещение X", hudXOffsetField.x, hudXOffsetField.y - 12, 0xFFFFFF);
        hudXOffsetField.drawTextBox();
        drawString(fontRenderer, "Смещение Y", hudYOffsetField.x, hudYOffsetField.y - 12, 0xFFFFFF);
        hudYOffsetField.drawTextBox();

        if (statusMessage != null) {
            drawCenteredString(fontRenderer, statusMessage, width / 2, height - 60, statusColor);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
