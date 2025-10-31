package com.khimkhaosow.craftmastery.gui;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;
import com.khimkhaosow.craftmastery.util.Reference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Обработчик GUI и клавиш для мода CraftMastery
 */
@SideOnly(Side.CLIENT)
public class GuiHandler {

    private static KeyBinding openGuiKey;

    public static void init() {
        // Регистрируем клавишу для открытия GUI
        openGuiKey = new KeyBinding("key.craftmastery.open_gui", 34, // Клавиша G по умолчанию
            "key.categories.craftmastery");
        ClientRegistry.registerKeyBinding(openGuiKey);

        // Регистрируем обработчик событий
        MinecraftForge.EVENT_BUS.register(new GuiHandler());
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (openGuiKey.isPressed()) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player != null) {
                // Проверяем права доступа
                if (PermissionManager.getInstance().hasPermission(player, PermissionType.OPEN_INTERFACE)) {
                    Minecraft.getMinecraft().displayGuiScreen(new GuiCraftMastery(player));
                } else {
                    player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.RED + "У вас нет прав на открытие интерфейса CraftMastery"));
                }
            }
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // Здесь можно добавить дополнительную логику при открытии GUI
        // Например, проверку прав или инициализацию данных
    }
}
