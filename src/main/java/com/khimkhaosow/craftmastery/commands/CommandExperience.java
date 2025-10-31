package com.khimkhaosow.craftmastery.commands;

import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.ExperienceType;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.experience.PointsType;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;
import com.khimkhaosow.craftmastery.util.NotificationHelper;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

public class CommandExperience extends CommandBase {

    @Override
    public String getName() {
        return "craftexp";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.craftexp.usage";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("cexp", "craftmastery_exp");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // Уровень оператора
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException(getUsage(sender));
        }

        String subCommand = args[0].toLowerCase();
        EntityPlayer targetPlayer = args.length > 1 ? getPlayer(server, sender, args[1]) : getCommandSenderAsPlayer(sender);

        // Проверка прав
        if (sender instanceof EntityPlayer && !PermissionManager.getInstance().hasPermission((EntityPlayer)sender, PermissionType.GIVE_POINTS)) {
            NotificationHelper.sendErrorMessage((EntityPlayer)sender, "У вас нет прав для управления опытом");
            return;
        }

        switch (subCommand) {
            case "add":
                handleAdd(sender, targetPlayer, args);
                break;
            case "set":
                handleSet(sender, targetPlayer, args);
                break;
            case "reset":
                handleReset(sender, targetPlayer);
                break;
            case "info":
                handleInfo(sender, targetPlayer);
                break;
            case "help":
                handleHelp(sender);
                break;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private void handleAdd(ICommandSender sender, EntityPlayer target, String[] args) throws CommandException {
        if (args.length < 4) {
            throw new WrongUsageException("commands.craftexp.add.usage");
        }

        String type = args[2].toUpperCase();
        float amount = (float) parseDouble(args[3]);

        if (type.equals("POINTS")) {
            if (args.length < 5) {
                throw new WrongUsageException("commands.craftexp.add.points.usage");
            }
            PointsType pointsType = PointsType.valueOf(args[4].toUpperCase());
            ExperienceManager.getInstance().addPoints(target, pointsType, (int)amount);
            NotificationHelper.sendSuccessMessage(target, String.format("Добавлено %d очков типа %s", (int)amount, pointsType.getDisplayName()));
        } else {
            ExperienceType expType = ExperienceType.valueOf(type);
            ExperienceManager.getInstance().addExperience(target, expType, amount);
            NotificationHelper.sendSuccessMessage(target, String.format("Добавлено %.1f опыта типа %s", amount, expType.getDisplayName()));
        }
    }

    private void handleSet(ICommandSender sender, EntityPlayer target, String[] args) throws CommandException {
        if (args.length < 4) {
            throw new WrongUsageException("commands.craftexp.set.usage");
        }

        String type = args[2].toUpperCase();
        float amount = (float) parseDouble(args[3]);

        PlayerExperienceData data = ExperienceManager.getInstance().getPlayerData(target);
        if (type.equals("LEVEL")) {
            data.setLevel((int)amount);
            NotificationHelper.sendSuccessMessage(target, String.format("Установлен уровень %d", (int)amount));
        } else if (type.equals("POINTS")) {
            if (args.length < 5) {
                throw new WrongUsageException("commands.craftexp.set.points.usage");
            }
            PointsType pointsType = PointsType.valueOf(args[4].toUpperCase());
            data.setPoints(pointsType, (int)amount);
            NotificationHelper.sendSuccessMessage(target, String.format("Установлено %d очков типа %s", (int)amount, pointsType.getDisplayName()));
        } else {
            ExperienceType expType = ExperienceType.valueOf(type);
            data.setExperience(expType, amount);
            NotificationHelper.sendSuccessMessage(target, String.format("Установлено %.1f опыта типа %s", amount, expType.getDisplayName()));
        }
    }

    private void handleReset(ICommandSender sender, EntityPlayer target) {
        ExperienceManager.getInstance().resetPlayerData(target);
        NotificationHelper.sendSuccessMessage(target, "Данные опыта сброшены");
    }

    private void handleInfo(ICommandSender sender, EntityPlayer target) {
        PlayerExperienceData data = ExperienceManager.getInstance().getPlayerData(target);

        NotificationHelper.sendMessage(target, "=== Информация об опыте ===", TextFormatting.GOLD);
        NotificationHelper.sendMessage(target, String.format("Уровень: %d (%.1f%%)", 
            data.getLevel(), data.getLevelProgress()), TextFormatting.GREEN);
        NotificationHelper.sendMessage(target, String.format("Опыт уровня: %.1f / %.1f", 
            data.getCurrentLevelExperience(), data.getExperienceForNextLevel()), TextFormatting.GREEN);
        NotificationHelper.sendMessage(target, "Опыт по типам:", TextFormatting.AQUA);

        for (ExperienceType type : ExperienceType.values()) {
            float exp = data.getExperience(type);
            if (exp > 0) {
                NotificationHelper.sendMessage(target, String.format("- %s: %.1f", 
                    type.getDisplayName(), exp), TextFormatting.GRAY);
            }
        }

        NotificationHelper.sendMessage(target, "Очки по типам:", TextFormatting.LIGHT_PURPLE);
        for (PointsType type : PointsType.values()) {
            int points = data.getPoints(type);
            if (points > 0) {
                NotificationHelper.sendMessage(target, String.format("- %s: %d", 
                    type.getDisplayName(), points), TextFormatting.GRAY);
            }
        }
    }

    private void handleHelp(ICommandSender sender) {
        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            NotificationHelper.sendMessage(player, "=== Помощь по команде /craftexp ===", TextFormatting.GOLD);
            NotificationHelper.sendMessage(player, "/craftexp add <игрок> <тип> <количество> - Добавить опыт/очки", TextFormatting.YELLOW);
            NotificationHelper.sendMessage(player, "/craftexp set <игрок> <тип> <количество> - Установить опыт/очки", TextFormatting.YELLOW);
            NotificationHelper.sendMessage(player, "/craftexp reset <игрок> - Сбросить все данные", TextFormatting.YELLOW);
            NotificationHelper.sendMessage(player, "/craftexp info <игрок> - Показать информацию", TextFormatting.YELLOW);
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "add", "set", "reset", "info", "help");
        } else if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("add")) {
                List<String> types = new ArrayList<>();
                for (ExperienceType type : ExperienceType.values()) {
                    types.add(type.name());
                }
                types.add("POINTS");
                return getListOfStringsMatchingLastWord(args, types);
            } else if (args[0].equalsIgnoreCase("set")) {
                List<String> types = new ArrayList<>();
                for (ExperienceType type : ExperienceType.values()) {
                    types.add(type.name());
                }
                types.add("POINTS");
                types.add("LEVEL");
                return getListOfStringsMatchingLastWord(args, types);
            }
        } else if (args.length == 5) {
            if ((args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("set")) 
                && args[2].equalsIgnoreCase("POINTS")) {
                List<String> types = new ArrayList<>();
                for (PointsType type : PointsType.values()) {
                    types.add(type.name());
                }
                return getListOfStringsMatchingLastWord(args, types);
            }
        }
        return Collections.emptyList();
    }
}