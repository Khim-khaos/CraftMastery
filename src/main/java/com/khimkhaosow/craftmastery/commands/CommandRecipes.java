package com.khimkhaosow.craftmastery.commands;

import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandRecipes extends CommandBase {
    
    @Override
    public String getName() {
        return "cmrecipes";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.cmrecipes.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // Уровень оператора
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException("commands.cmrecipes.usage");
        }

        RecipeManager recipeManager = RecipeManager.getInstance();

        switch (args[0].toLowerCase()) {
            case "export":
                if (args.length < 2) {
                    throw new WrongUsageException("commands.cmrecipes.export.usage");
                }
                String recipeId = args[1];
                String json = recipeManager.exportRecipeToJson(recipeId);
                if (json.equals("{}")) {
                    sender.sendMessage(new TextComponentString(
                        TextFormatting.RED + "Рецепт не найден: " + recipeId));
                } else {
                    sender.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "Рецепт экспортирован в JSON:"));
                    sender.sendMessage(new TextComponentString(json));
                }
                break;

            case "import":
                if (args.length < 2) {
                    throw new WrongUsageException("commands.cmrecipes.import.usage");
                }
                StringBuilder jsonBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    jsonBuilder.append(args[i]).append(" ");
                }
                if (recipeManager.importRecipeFromJson(jsonBuilder.toString())) {
                    sender.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "Рецепт успешно импортирован"));
                } else {
                    sender.sendMessage(new TextComponentString(
                        TextFormatting.RED + "Ошибка при импорте рецепта"));
                }
                break;

            case "reload":
                recipeManager.reloadRecipes();
                sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "Рецепты перезагружены"));
                break;

            case "save":
                recipeManager.saveAllRecipes();
                sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "Рецепты сохранены"));
                break;

            case "list":
                List<RecipeEntry> recipes = recipeManager.getAllRecipes();
                sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "Список рецептов (" + recipes.size() + "):"));
                for (RecipeEntry recipe : recipes) {
                    sender.sendMessage(new TextComponentString(
                        "- " + recipe.getRecipeId() + " (" + recipe.getDisplayName() + ")"));
                }
                break;

            case "info":
                if (args.length < 2) {
                    throw new WrongUsageException("commands.cmrecipes.info.usage");
                }
                RecipeEntry recipe = recipeManager.getRecipe(args[1]);
                if (recipe == null) {
                    sender.sendMessage(new TextComponentString(
                        TextFormatting.RED + "Рецепт не найден: " + args[1]));
                } else {
                    sender.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "Информация о рецепте:"));
                    sender.sendMessage(new TextComponentString("ID: " + recipe.getRecipeId()));
                    sender.sendMessage(new TextComponentString("Название: " + recipe.getDisplayName()));
                    sender.sendMessage(new TextComponentString("Категория: " + recipe.getCategory()));
                    sender.sendMessage(new TextComponentString("Сложность: " + recipe.getDifficulty()));
                    sender.sendMessage(new TextComponentString("Очки изучения: " + recipe.getRequiredLearningPoints()));
                    sender.sendMessage(new TextComponentString("Требуемый уровень: " + recipe.getRequiredLevel()));
                    sender.sendMessage(new TextComponentString("Описание: " + recipe.getDescription()));
                }
                break;

            default:
                throw new WrongUsageException("commands.cmrecipes.usage");
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "export", "import", "reload", "save", "list", "info");
        } else if (args.length == 2 && (args[0].equals("export") || args[0].equals("info"))) {
            return getListOfStringsMatchingLastWord(args,
                RecipeManager.getInstance().getAllRecipes().stream()
                    .map(RecipeEntry::getRecipeId)
                    .collect(Collectors.toList()));
        }
        return Collections.emptyList();
    }
}