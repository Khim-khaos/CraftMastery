package com.khimkhaosow.craftmastery.crafting;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Фильтр для скрытия рецептов из интерфейса крафта
 */
public class RecipeFilter {

    private static RecipeFilter instance;

    public RecipeFilter() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static RecipeFilter getInstance() {
        if (instance == null) {
            instance = new RecipeFilter();
        }
        return instance;
    }

    /**
     * Проверяет, должен ли быть скрыт рецепт для игрока
     */
    public boolean shouldHideRecipe(EntityPlayer player, IRecipe recipe) {
        if (player == null || recipe == null) return false;

        // Проверяем права доступа
        if (!PermissionManager.getInstance().hasPermission(player, PermissionType.LEARN_RECIPES)) {
            return true; // Скрываем все рецепты если нет прав
        }

        // Получаем RecipeEntry для этого рецепта
        RecipeEntry recipeEntry = RecipeManager.getInstance().getRecipe(recipe.getRegistryName());
        if (recipeEntry == null) {
            // Если рецепт не найден в системе CraftMastery, показываем его
            return false;
        }

        // Скрываем, если рецепт не изучен и не доступен для изучения
        if (!recipeEntry.isStudiedByPlayer(player.getUniqueID())) {
            PlayerExperienceData expData = ExperienceManager.getInstance().getPlayerData(player);
            return !recipeEntry.canPlayerStudy(player, expData);
        }

        return false;
    }

    /**
     * Фильтрует список рецептов для игрока
     */
    @SideOnly(Side.CLIENT)
    public java.util.List<IRecipe> filterRecipesForPlayer(EntityPlayer player, java.util.List<IRecipe> recipes) {
        java.util.List<IRecipe> filtered = new java.util.ArrayList<>();

        for (IRecipe recipe : recipes) {
            if (!shouldHideRecipe(player, recipe)) {
                filtered.add(recipe);
            }
        }

        return filtered;
    }
}
