package com.khimkhaosow.craftmastery.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.experience.PointsType;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;
import com.khimkhaosow.craftmastery.tabs.TabManager;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * Основная команда мода CraftMastery
 */
public class CommandCraftMastery extends CommandBase {

    private final List<String> aliases;

    public CommandCraftMastery() {
        aliases = Arrays.asList("craftmastery", "cm", "craftm");
    }

    @Override
    public String getName() {
        return "craftmastery";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/craftmastery <points|experience|tab|permission|reset|info> [параметры]";
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(0, "craftmastery");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            showHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "points":
                handlePointsCommand(server, sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "experience":
            case "exp":
                handleExperienceCommand(server, sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "tab":
                handleTabCommand(server, sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "permission":
            case "perm":
                handlePermissionCommand(server, sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "reset":
                handleResetCommand(server, sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "info":
                handleInfoCommand(server, sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "help":
                showHelp(sender);
                break;
            default:
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Неизвестная подкоманда: " + subCommand));
                showHelp(sender);
                break;
        }
    }

    private void handlePointsCommand(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 3) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Использование: /craftmastery points <give|take|set> <игрок> <тип> <количество>"));
            return;
        }

        String action = args[0].toLowerCase();
        String playerName = args[1];
        String pointsTypeStr = args[2];
        int amount;

        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Неверное количество: " + args[3]));
            return;
        }

        EntityPlayer player = server.getPlayerList().getPlayerByUsername(playerName);
        if (player == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Игрок не найден: " + playerName));
            return;
        }

        PointsType pointsType;
        try {
            pointsType = PointsType.valueOf(pointsTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Неверный тип очков: " + pointsTypeStr));
            return;
        }

        // Проверяем права
        if (!PermissionManager.getInstance().hasPermission(player, PermissionType.GIVE_POINTS)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "У вас нет прав на выдачу очков"));
            return;
        }

        PlayerExperienceData data = ExperienceManager.getInstance().getPlayerData(player);

        switch (action) {
            case "give":
                data.addPoints(pointsType, amount);
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN +
                    "Выдано " + amount + " " + pointsType.getDisplayName() + " игроку " + playerName));
                break;
            case "take":
                if (data.getPoints(pointsType) >= amount) {
                    data.spendPoints(pointsType, amount);
                    sender.sendMessage(new TextComponentString(TextFormatting.GREEN +
                        "Снято " + amount + " " + pointsType.getDisplayName() + " у игрока " + playerName));
                } else {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "У игрока недостаточно очков"));
                }
                break;
            case "set":
                // Сбрасываем до 0 и добавляем нужное количество
                int current = data.getPoints(pointsType);
                data.spendPoints(pointsType, current);
                data.addPoints(pointsType, amount);
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN +
                    "Установлено " + amount + " " + pointsType.getDisplayName() + " игроку " + playerName));
                break;
            default:
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Неверное действие: " + action));
                break;
        }
    }

    private void handleExperienceCommand(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 3) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Использование: /craftmastery experience <give|take|set> <игрок> <количество>"));
            return;
        }

        String action = args[0].toLowerCase();
        String playerName = args[1];
        float amount;

        try {
            amount = Float.parseFloat(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Неверное количество: " + args[2]));
            return;
        }

        EntityPlayer player = server.getPlayerList().getPlayerByUsername(playerName);
        if (player == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Игрок не найден: " + playerName));
            return;
        }

        // Проверяем права
        if (!PermissionManager.getInstance().hasPermission(player, PermissionType.GIVE_POINTS)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "У вас нет прав на выдачу опыта"));
            return;
        }

        PlayerExperienceData data = ExperienceManager.getInstance().getPlayerData(player);

        switch (action) {
            case "give":
                data.addExperience(com.khimkhaosow.craftmastery.experience.ExperienceType.BLOCK_MINING, amount);
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN +
                    "Выдано " + amount + " опыта игроку " + playerName));
                break;
            case "take":
                // Опыт нельзя отнимать напрямую, только через сброс
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Опыт можно только выдавать, для сброса используйте /craftmastery reset"));
                break;
            case "set":
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Опыт нельзя устанавливать напрямую"));
                break;
            default:
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Неверное действие: " + action));
                break;
        }
    }

    private void handleTabCommand(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Использование: /craftmastery tab <create|delete|info|list>"));
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "create":
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Использование: /craftmastery tab create <id> <название>"));
                    return;
                }
                handleTabCreate(sender, args[1], args[2]);
                break;
            case "delete":
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Использование: /craftmastery tab delete <id>"));
                    return;
                }
                handleTabDelete(sender, args[1]);
                break;
            case "info":
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Использование: /craftmastery tab info <id>"));
                    return;
                }
                handleTabInfo(sender, args[1]);
                break;
            case "list":
                handleTabList(sender);
                break;
            default:
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Неверное действие: " + action));
                break;
        }
    }

    private void handleTabCreate(ICommandSender sender, String id, String name) {
        TabManager tabManager = TabManager.getInstance();

        if (tabManager.hasTab(id)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Вкладка с ID '" + id + "' уже существует"));
            return;
        }

        com.khimkhaosow.craftmastery.tabs.Tab tab = tabManager.createTab(id, name);
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Создана вкладка: " + name + " (ID: " + id + ")"));
    }

    private void handleTabDelete(ICommandSender sender, String id) {
        TabManager tabManager = TabManager.getInstance();

        if (!tabManager.hasTab(id)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Вкладка с ID '" + id + "' не найдена"));
            return;
        }

        com.khimkhaosow.craftmastery.tabs.Tab tab = tabManager.getTab(id);
        if (tab == tabManager.getDefaultTab()) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Нельзя удалить базовую вкладку"));
            return;
        }

        boolean success = tabManager.removeTab(id);
        if (success) {
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Вкладка удалена: " + tab.getName()));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Ошибка при удалении вкладки"));
        }
    }

    private void handleTabInfo(ICommandSender sender, String id) {
        TabManager tabManager = TabManager.getInstance();

        if (!tabManager.hasTab(id)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Вкладка с ID '" + id + "' не найдена"));
            return;
        }

        com.khimkhaosow.craftmastery.tabs.Tab tab = tabManager.getTab(id);

        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Информация о вкладке ==="));
        sender.sendMessage(new TextComponentString("ID: " + TextFormatting.WHITE + tab.getId()));
        sender.sendMessage(new TextComponentString("Название: " + TextFormatting.WHITE + tab.getName()));
        sender.sendMessage(new TextComponentString("Описание: " + TextFormatting.WHITE + tab.getDescription()));
        sender.sendMessage(new TextComponentString("Рецептов: " + TextFormatting.WHITE + tab.getRecipeCount()));
        sender.sendMessage(new TextComponentString("Требуемых спец-очков: " + TextFormatting.WHITE + tab.getRequiredSpecialPoints()));
        sender.sendMessage(new TextComponentString("Стоимость сброса: " + TextFormatting.WHITE + tab.getResetCost()));
    }

    private void handleTabList(ICommandSender sender) {
        TabManager tabManager = TabManager.getInstance();
        List<com.khimkhaosow.craftmastery.tabs.Tab> tabs = tabManager.getAllTabs();

        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Список вкладок (" + tabs.size() + ") ==="));

        for (com.khimkhaosow.craftmastery.tabs.Tab tab : tabs) {
            TextFormatting color = tab == tabManager.getDefaultTab() ? TextFormatting.YELLOW : TextFormatting.WHITE;
            sender.sendMessage(new TextComponentString(color + tab.getId() + " - " + tab.getName() +
                " (" + tab.getRecipeCount() + " рецептов)"));
        }
    }

    private void handlePermissionCommand(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Команда permissions еще не реализована"));
    }

    private void handleResetCommand(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Использование: /craftmastery reset <player> <all|experience|points|tabs>"));
            return;
        }

        String playerName = args[0];
        String resetType = args[1].toLowerCase();

        EntityPlayer player = server.getPlayerList().getPlayerByUsername(playerName);
        if (player == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Игрок не найден: " + playerName));
            return;
        }

        switch (resetType) {
            case "all":
                ExperienceManager.getInstance().resetPlayerData(player);
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Сброс всех данных игрока " + playerName));
                break;
            case "experience":
            case "exp":
                ExperienceManager.getInstance().resetPlayerData(player);
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Сброс опыта игрока " + playerName));
                break;
            case "points":
                PlayerExperienceData data = ExperienceManager.getInstance().getPlayerData(player);
                for (PointsType type : PointsType.values()) {
                    int current = data.getPoints(type);
                    data.spendPoints(type, current);
                }
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Сброс очков игрока " + playerName));
                break;
            case "tabs":
                // Сброс всех вкладок игрока
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Сброс вкладок игрока " + playerName));
                break;
            default:
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Неверный тип сброса: " + resetType));
                break;
        }
    }

    private void handleInfoCommand(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            showGeneralInfo(sender);
            return;
        }

        String playerName = args[0];
        EntityPlayer player = server.getPlayerList().getPlayerByUsername(playerName);

        if (player == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Игрок не найден: " + playerName));
            return;
        }

        showPlayerInfo(sender, player);
    }

    private void showGeneralInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== CraftMastery ==="));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "Вкладок: " + TabManager.getInstance().getTabCount()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "Используйте /craftmastery help для справки"));
    }

    private void showPlayerInfo(ICommandSender sender, EntityPlayer player) {
        PlayerExperienceData data = ExperienceManager.getInstance().getPlayerData(player);

        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Информация об игроке " + player.getName() + " ==="));
        sender.sendMessage(new TextComponentString("Уровень: " + TextFormatting.WHITE + data.getLevel()));
        sender.sendMessage(new TextComponentString("Опыт: " + TextFormatting.WHITE + String.format("%.1f", data.getTotalExperience())));
        sender.sendMessage(new TextComponentString("Прогресс уровня: " + TextFormatting.WHITE + String.format("%.1f%%", data.getLevelProgress())));

        sender.sendMessage(new TextComponentString(""));
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Очки:"));
        for (PointsType type : PointsType.values()) {
            sender.sendMessage(new TextComponentString(type.getColor() + type.getDisplayName() + ": " + TextFormatting.WHITE + data.getPoints(type)));
        }
    }

    private void showHelp(ICommandSender sender) {
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== CraftMastery команды ==="));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "/craftmastery points <give|take|set> <игрок> <тип> <количество> - управление очками"));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "/craftmastery experience <give> <игрок> <количество> - управление опытом"));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "/craftmastery tab <create|delete|info|list> - управление вкладками"));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "/craftmastery reset <игрок> <all|experience|points|tabs> - сброс данных"));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "/craftmastery info [игрок] - информация"));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "/craftmastery permission - управление правами (скоро)"));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "/craftmastery help - эта справка"));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("points", "experience", "tab", "permission", "reset", "info", "help"));
        } else if (args.length == 2) {
            String command = args[0].toLowerCase();
            switch (command) {
                case "points":
                    completions.addAll(Arrays.asList("give", "take", "set"));
                    break;
                case "experience":
                case "exp":
                    completions.add("give");
                    break;
                case "tab":
                    completions.addAll(Arrays.asList("create", "delete", "info", "list"));
                    break;
                case "reset":
                    completions.addAll(Arrays.asList("all", "experience", "points", "tabs"));
                    break;
                case "info":
                    // Добавляем имена игроков
                    for (EntityPlayer player : server.getPlayerList().getPlayers()) {
                        completions.add(player.getName());
                    }
                    break;
            }
        } else if (args.length == 3) {
            String command = args[0].toLowerCase();
            if (command.equals("points")) {
                // Добавляем имена игроков
                for (EntityPlayer player : server.getPlayerList().getPlayers()) {
                    completions.add(player.getName());
                }
            } else if (command.equals("tab") && args[1].equals("info")) {
                // Добавляем ID вкладок
                for (com.khimkhaosow.craftmastery.tabs.Tab tab : TabManager.getInstance().getAllTabs()) {
                    completions.add(tab.getId());
                }
            }
        } else if (args.length == 4 && args[0].equals("points")) {
            // Добавляем типы очков
            for (PointsType type : PointsType.values()) {
                completions.add(type.name().toLowerCase());
            }
        }

        return completions;
    }
}
