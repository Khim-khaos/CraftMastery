package com.khimkhaosow.craftmastery.gui.widgets;

import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * Виджет панели администратора
 */
public class AdminPanelWidget extends Gui {
    private static final ResourceLocation ADMIN_TEXTURES = new ResourceLocation("craftmastery", "textures/gui/admin.png");
    private static final int BUTTON_SIZE = 25;
    private static final int BUTTON_SPACING = 35;

    private final Minecraft minecraft;
    private final FontRenderer fontRenderer;
    private final EntityPlayer player;
    private final int x;
    private final int y;
    private boolean isSettingsPage;

    public interface AdminButtonCallback {
        void onSettingsClicked();
        void onBackClicked();
    }

    private final AdminButtonCallback callback;

    public AdminPanelWidget(Minecraft minecraft, EntityPlayer player, 
                          int x, int y, AdminButtonCallback callback) {
        this.minecraft = minecraft;
        this.fontRenderer = minecraft.fontRenderer;
        this.player = player;
        this.x = x;
        this.y = y;
        this.callback = callback;
        this.isSettingsPage = false;
    }

    public void draw(int mouseX, int mouseY) {
        minecraft.getTextureManager().bindTexture(ADMIN_TEXTURES);

        // Кнопка настроек (если есть права)
        if (PermissionManager.getInstance().hasPermission(player, PermissionType.ADMIN_SETTINGS)) {
            int color = isSettingsPage ? 0xFF4CAF50 : 
                       isMouseOverSettings(mouseX, mouseY) ? 0xFF666666 : 0xFF555555;
            drawRect(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, color);
            drawCenteredString(fontRenderer, "\u2699", 
                             x + BUTTON_SIZE/2, y + 5, 0xFFFFFF);
        }

        // Кнопка "назад" (если не на главной)
        if (!isSettingsPage) {
            int backX = x + BUTTON_SPACING;
            int color = isMouseOverBack(mouseX, mouseY) ? 0xFF666666 : 0xFF555555;
            drawRect(backX, y, backX + BUTTON_SIZE, y + BUTTON_SIZE, color);
            drawCenteredString(fontRenderer, "\u2190", 
                             backX + BUTTON_SIZE/2, y + 5, 0xFFFFFF);
        }
    }

    public boolean handleMouseClick(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return false; // Только левый клик
        if (isMouseOverSettings(mouseX, mouseY)) {
            if (PermissionManager.getInstance().hasPermission(player, PermissionType.ADMIN_SETTINGS)) {
                isSettingsPage = true;
                if (callback != null) {
                    callback.onSettingsClicked();
                }
                return true;
            }
        }
        
        if (!isSettingsPage && isMouseOverBack(mouseX, mouseY)) {
            if (callback != null) {
                callback.onBackClicked();
            }
            return true;
        }

        return false;
    }

    private boolean isMouseOverSettings(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + BUTTON_SIZE &&
               mouseY >= y && mouseY <= y + BUTTON_SIZE;
    }

    private boolean isMouseOverBack(int mouseX, int mouseY) {
        int backX = x + BUTTON_SPACING;
        return mouseX >= backX && mouseX <= backX + BUTTON_SIZE &&
               mouseY >= y && mouseY <= y + BUTTON_SIZE;
    }

    public void setSettingsPage(boolean isSettingsPage) {
        this.isSettingsPage = isSettingsPage;
    }
}