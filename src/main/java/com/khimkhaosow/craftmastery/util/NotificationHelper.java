package com.khimkhaosow.craftmastery.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/**
 * Вспомогательный класс для отправки уведомлений игрокам
 */
public class NotificationHelper {

    /**
     * Отправляет сообщение игроку
     */
    public static void sendMessage(EntityPlayer player, String message, TextFormatting color) {
        if (player == null) return;

        ITextComponent text = new TextComponentString(message);
        text.getStyle().setColor(color);
        player.sendMessage(text);
    }

    /**
     * Отправляет локализованное сообщение игроку
     */
    public static void sendTranslatedMessage(EntityPlayer player, String translationKey, Object... args) {
        if (player == null) return;

        ITextComponent text = new TextComponentTranslation(translationKey, args);
        player.sendMessage(text);
    }

    /**
     * Отправляет уведомление об опыте
     */
    public static void sendExperienceNotification(EntityPlayer player, float amount, String type) {
        String message = String.format("§a+%.1f опыта §7(%s)", amount, type);
        sendMessage(player, message, TextFormatting.GREEN);
    }

    /**
     * Отправляет уведомление об очках
     */
    public static void sendPointsNotification(EntityPlayer player, int amount, String type) {
        String message = String.format("§b+%d очков §7(%s)", amount, type);
        sendMessage(player, message, TextFormatting.AQUA);
    }

    /**
     * Отправляет уведомление о новом уровне
     */
    public static void sendLevelUpNotification(EntityPlayer player, int newLevel) {
        String message = String.format("§6Поздравляем! Вы достигли §e%d§6 уровня!", newLevel);
        sendMessage(player, message, TextFormatting.GOLD);
        
        // Добавляем эффекты
        if (player instanceof EntityPlayerMP) {
            // Звук левел-апа
            player.world.playSound(null, player.posX, player.posY, player.posZ, 
                net.minecraft.init.SoundEvents.ENTITY_PLAYER_LEVELUP, 
                net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    /**
     * Отправляет уведомление об ошибке
     */
    public static void sendErrorMessage(EntityPlayer player, String message) {
        sendMessage(player, "§c" + message, TextFormatting.RED);
    }

    /**
     * Отправляет уведомление об успехе
     */
    public static void sendSuccessMessage(EntityPlayer player, String message) {
        sendMessage(player, "§a" + message, TextFormatting.GREEN);
    }
}