package com.khimkhaosow.craftmastery.gui;

import com.khimkhaosow.craftmastery.config.ModConfig;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.ExperienceType;
import com.khimkhaosow.craftmastery.experience.PointsType;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Admin screen for managing experience multipliers and granting rewards.
 */
public class GuiAdminSettingsScreen extends GuiScreen {
    private static final int BTN_SAVE = 5200;
    private static final int BTN_CANCEL = 5201;
    private static final int BTN_GRANT_EXP = 5202;
    private static final int BTN_GRANT_POINTS = 5203;
    private static final int BTN_LOAD = 5204;

    private final GuiScreen parent;
    private final EntityPlayer adminPlayer;

    private GuiTextField globalMultiplierField;
    private final Map<ExperienceType, GuiTextField> multiplierFields = new EnumMap<>(ExperienceType.class);
    private final Map<ExperienceType, GuiTextField> conversionFields = new EnumMap<>(ExperienceType.class);

    private GuiTextField grantTargetField;
    private GuiTextField grantAmountField;
    private GuiTextField grantPointsField;

    private GuiCheckBox permissionOpenInterface;
    private GuiCheckBox permissionLearnRecipes;
    private GuiCheckBox permissionResetTabs;
    private GuiCheckBox permissionManageRecipes;
    private GuiCheckBox permissionManageTabs;
    private GuiCheckBox permissionGivePoints;

    private String statusMessage;
    private int statusColor = 0xFFFFFFFF;

    private final List<GuiLabel> labels = new ArrayList<>();

    public GuiAdminSettingsScreen(GuiScreen parent, EntityPlayer adminPlayer) {
        this.parent = parent;
        this.adminPlayer = adminPlayer;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        labels.clear();

        int margin = 20;
        int labelLeft = width / 2 - 160;
        int fieldLeft = width / 2 + 20;
        int y = 40;

        globalMultiplierField = createField(fieldLeft, y, String.valueOf(ModConfig.globalExperienceMultiplier));
        addLabel("Глобальный множитель", labelLeft, y + 4);
        y += 28;

        for (ExperienceType type : ExperienceType.values()) {
            GuiTextField field = createField(fieldLeft, y, String.valueOf(getConfigMultiplier(type)));
            multiplierFields.put(type, field);
            addLabel("Множитель " + type.getDisplayName(), labelLeft, y + 4);
            y += 28;
        }

        for (ExperienceType type : ExperienceType.values()) {
            GuiTextField field = createField(fieldLeft, y, String.valueOf(getConfigConversion(type)));
            conversionFields.put(type, field);
            addLabel("Конверсия в очки " + type.getDisplayName(), labelLeft, y + 4);
            y += 28;
        }

        y += 8;
        addLabel("Права игроков по умолчанию", labelLeft, y);
        y += 18;
        permissionOpenInterface = addPermissionCheckbox(6000, labelLeft, y, "Открывать интерфейс", ModConfig.playersCanOpenInterface);
        y += 18;
        permissionLearnRecipes = addPermissionCheckbox(6001, labelLeft, y, "Изучать рецепты", ModConfig.playersCanLearnRecipes);
        y += 18;
        permissionResetTabs = addPermissionCheckbox(6002, labelLeft, y, "Сбрасывать вкладки", ModConfig.playersCanResetTabs);
        y += 18;
        permissionManageRecipes = addPermissionCheckbox(6003, labelLeft, y, "Управлять рецептами", ModConfig.playersCanManageRecipes);
        y += 18;
        permissionManageTabs = addPermissionCheckbox(6004, labelLeft, y, "Управлять вкладками", ModConfig.playersCanManageTabs);
        y += 18;
        permissionGivePoints = addPermissionCheckbox(6005, labelLeft, y, "Выдавать очки", ModConfig.playersCanGivePoints);
        y += 24;

        y += 10;
        addLabel("Выдать опыт игроку", labelLeft, y);
        y += 18;
        grantTargetField = createField(labelLeft, y, adminPlayer.getName());
        grantAmountField = createField(fieldLeft, y, "100");
        y += 24;
        grantPointsField = createField(fieldLeft, y, "10");
        addLabel("Очки (LEARNING)", labelLeft, y + 4);
        y += 30;

        buttonList.add(new GuiButton(BTN_LOAD, fieldLeft - 60, y, 120, 20, "Сбросить"));
        buttonList.add(new GuiButton(BTN_GRANT_EXP, fieldLeft + 70, y, 120, 20, "Выдать опыт"));
        y += 24;
        buttonList.add(new GuiButton(BTN_GRANT_POINTS, fieldLeft + 70, y, 120, 20, "Выдать очки"));

        buttonList.add(new GuiButton(BTN_SAVE, width / 2 - 110, height - 40, 100, 20, "Сохранить"));
        buttonList.add(new GuiButton(BTN_CANCEL, width / 2 + 10, height - 40, 100, 20, "Отмена"));
    }

    private float getConfigMultiplier(ExperienceType type) {
        switch (type) {
            case BLOCK_MINING:
                return ModConfig.blockMiningMultiplier;
            case CRAFTING:
                return ModConfig.craftingMultiplier;
            case MOB_KILL:
                return ModConfig.mobKillMultiplier;
            case PLAYER_KILL:
                return ModConfig.playerKillMultiplier;
            default:
                return 1.0f;
        }
    }

    private float getConfigConversion(ExperienceType type) {
        switch (type) {
            case BLOCK_MINING:
                return ModConfig.blockMiningToLearningRatio;
            case CRAFTING:
                return ModConfig.craftingToLearningRatio;
            case MOB_KILL:
                return ModConfig.mobKillToLearningRatio;
            case PLAYER_KILL:
                return ModConfig.playerKillToLearningRatio;
            default:
                return 0.0f;
        }
    }

    private GuiTextField createField(int x, int y, String value) {
        GuiTextField field = new GuiTextField(0, fontRenderer, x, y, 120, 18);
        field.setMaxStringLength(32);
        field.setText(value);
        return field;
    }

    private void addLabel(String text, int x, int y) {
        GuiLabel label = new GuiLabel(fontRenderer, labels.size(), x, y, 180, 12, 0xFFFFFF);
        label.addLine(text);
        labels.add(label);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (globalMultiplierField.textboxKeyTyped(typedChar, keyCode)) return;
        for (GuiTextField field : multiplierFields.values()) {
            if (field.textboxKeyTyped(typedChar, keyCode)) return;
        }
        for (GuiTextField field : conversionFields.values()) {
            if (field.textboxKeyTyped(typedChar, keyCode)) return;
        }
        if (grantTargetField.textboxKeyTyped(typedChar, keyCode)) return;
        if (grantAmountField.textboxKeyTyped(typedChar, keyCode)) return;
        if (grantPointsField.textboxKeyTyped(typedChar, keyCode)) return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        globalMultiplierField.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : multiplierFields.values()) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
        for (GuiTextField field : conversionFields.values()) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
        grantTargetField.mouseClicked(mouseX, mouseY, mouseButton);
        grantAmountField.mouseClicked(mouseX, mouseY, mouseButton);
        grantPointsField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        globalMultiplierField.updateCursorCounter();
        for (GuiTextField field : multiplierFields.values()) {
            field.updateCursorCounter();
        }
        for (GuiTextField field : conversionFields.values()) {
            field.updateCursorCounter();
        }
        grantTargetField.updateCursorCounter();
        grantAmountField.updateCursorCounter();
        grantPointsField.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) {
            return;
        }

        switch (button.id) {
            case BTN_SAVE:
                if (applySettings()) {
                    Minecraft.getMinecraft().displayGuiScreen(parent);
                }
                break;
            case BTN_CANCEL:
                Minecraft.getMinecraft().displayGuiScreen(parent);
                break;
            case BTN_GRANT_EXP:
                grantExperience();
                break;
            case BTN_GRANT_POINTS:
                grantPoints();
                break;
            case BTN_LOAD:
                initGui();
                setStatus("Значения сброшены", false);
                break;
            default:
                break;
        }
    }

    private boolean applySettings() {
        try {
            float global = Float.parseFloat(globalMultiplierField.getText().trim());
            if (global < 0.1f || global > 10.0f) {
                setStatus("Глобальный множитель 0.1-10.0", true);
                return false;
            }
            float blockMultiplier = parseNonNegative(multiplierFields.get(ExperienceType.BLOCK_MINING).getText().trim());
            float craftingMultiplier = parseNonNegative(multiplierFields.get(ExperienceType.CRAFTING).getText().trim());
            float mobMultiplier = parseNonNegative(multiplierFields.get(ExperienceType.MOB_KILL).getText().trim());
            float playerMultiplier = parseNonNegative(multiplierFields.get(ExperienceType.PLAYER_KILL).getText().trim());

            float blockConversion = parseNonNegative(conversionFields.get(ExperienceType.BLOCK_MINING).getText().trim());
            float craftingConversion = parseNonNegative(conversionFields.get(ExperienceType.CRAFTING).getText().trim());
            float mobConversion = parseNonNegative(conversionFields.get(ExperienceType.MOB_KILL).getText().trim());
            float playerConversion = parseNonNegative(conversionFields.get(ExperienceType.PLAYER_KILL).getText().trim());

            if (blockMultiplier < 0 || craftingMultiplier < 0 || mobMultiplier < 0 || playerMultiplier < 0
                || blockConversion < 0 || craftingConversion < 0 || mobConversion < 0 || playerConversion < 0) {
                setStatus("Значения не могут быть отрицательными", true);
                return false;
            }

            ModConfig.applyExperienceSettings(
                    global,
                    blockMultiplier,
                    craftingMultiplier,
                    mobMultiplier,
                    playerMultiplier,
                    blockConversion,
                    craftingConversion,
                    mobConversion,
                    playerConversion
            );

            ModConfig.applyPermissionSettings(
                    permissionOpenInterface.isChecked(),
                    permissionLearnRecipes.isChecked(),
                    permissionResetTabs.isChecked(),
                    permissionManageRecipes.isChecked(),
                    permissionManageTabs.isChecked(),
                    permissionGivePoints.isChecked()
            );

            PermissionManager permissionManager = PermissionManager.getInstance();
            permissionManager.setDefaultPlayerPermission(PermissionType.OPEN_INTERFACE, permissionOpenInterface.isChecked());
            permissionManager.setDefaultPlayerPermission(PermissionType.LEARN_RECIPES, permissionLearnRecipes.isChecked());
            permissionManager.setDefaultPlayerPermission(PermissionType.RESET_TABS, permissionResetTabs.isChecked());
            permissionManager.setDefaultPlayerPermission(PermissionType.MANAGE_RECIPES, permissionManageRecipes.isChecked());
            permissionManager.setDefaultPlayerPermission(PermissionType.MANAGE_TABS, permissionManageTabs.isChecked());
            permissionManager.setDefaultPlayerPermission(PermissionType.GIVE_POINTS, permissionGivePoints.isChecked());

            ExperienceManager.getInstance().syncWithConfig();
            setStatus("Настройки сохранены", false);
            return true;
        } catch (NumberFormatException ex) {
            setStatus("Некорректные числовые значения", true);
            return false;
        }
    }

    private void grantExperience() {
        String targetName = grantTargetField.getText().trim();
        EntityPlayer target = getPlayerByName(targetName);
        if (target == null) {
            setStatus("Игрок не найден", true);
            return;
        }
        try {
            float amount = Float.parseFloat(grantAmountField.getText().trim());
            ExperienceManager.getInstance().addExperience(target, ExperienceType.BLOCK_MINING, amount);
            setStatus("Выдано " + amount + " опыта", false);
        } catch (NumberFormatException ex) {
            setStatus("Некорректное значение опыта", true);
        }
    }

    private void grantPoints() {
        String targetName = grantTargetField.getText().trim();
        EntityPlayer target = getPlayerByName(targetName);
        if (target == null) {
            setStatus("Игрок не найден", true);
            return;
        }
        try {
            int amount = Integer.parseInt(grantPointsField.getText().trim());
            ExperienceManager.getInstance().addPoints(target, PointsType.LEARNING, amount);
            setStatus("Выдано " + amount + " очков", false);
        } catch (NumberFormatException ex) {
            setStatus("Некорректное значение очков", true);
        }
    }

    private float parseNonNegative(String value) {
        try {
            float parsed = Float.parseFloat(value);
            return parsed < 0 ? -1f : parsed;
        } catch (NumberFormatException ex) {
            return -1f;
        }
    }

    private EntityPlayer getPlayerByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return Minecraft.getMinecraft().world != null ? Minecraft.getMinecraft().world.getPlayerEntityByName(name) : null;
    }

    private GuiCheckBox addPermissionCheckbox(int id, int x, int y, String label, boolean checked) {
        GuiCheckBox checkBox = new GuiCheckBox(id, x, y, label, checked);
        buttonList.add(checkBox);
        return checkBox;
    }

    private void setStatus(String message, boolean error) {
        statusMessage = message;
        statusColor = error ? 0xFFFF5555 : 0xFF55FF55;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, TextFormatting.GOLD + "Админ-настройки", width / 2, 15, 0xFFFFFF);

        globalMultiplierField.drawTextBox();
        for (GuiTextField field : multiplierFields.values()) {
            field.drawTextBox();
        }
        for (GuiTextField field : conversionFields.values()) {
            field.drawTextBox();
        }
        grantTargetField.drawTextBox();
        grantAmountField.drawTextBox();
        grantPointsField.drawTextBox();

        for (GuiLabel label : labels) {
            label.drawLabel(mc, mouseX, mouseY);
        }

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
