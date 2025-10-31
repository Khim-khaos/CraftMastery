package com.khimkhaosow.craftmastery.crafting;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * Обертка для рецептов, которая проверяет права игрока
 */
public class CraftMasteryRecipeWrapper extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    private final IRecipe originalRecipe;
    private final RecipeEntry recipeEntry;

    public CraftMasteryRecipeWrapper(IRecipe originalRecipe) {
        this.originalRecipe = originalRecipe;

        // Копируем оригинальные данные
        this.setRegistryName(originalRecipe.getRegistryName());

        // Получаем RecipeEntry
        this.recipeEntry = RecipeManager.getInstance().getRecipe(originalRecipe.getRegistryName());
    }

    @Override
    public boolean matches(InventoryCrafting inv, net.minecraft.world.World worldIn) {
        return originalRecipe.matches(inv, worldIn);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return originalRecipe.getCraftingResult(inv);
    }

    @Override
    public boolean canFit(int width, int height) {
        return originalRecipe.canFit(width, height);
    }

    @Override
    public ItemStack getRecipeOutput() {
        return originalRecipe.getRecipeOutput();
    }

    /**
     * Проверяет, может ли игрок использовать этот рецепт
     */
    public boolean canPlayerUseRecipe(EntityPlayer player) {
        if (player == null) return true;

        // Проверяем права доступа
        if (!PermissionManager.getInstance().hasPermission(player, PermissionType.LEARN_RECIPES)) {
            return false;
        }

        // Если рецепт не в системе CraftMastery, разрешаем
        if (recipeEntry == null) {
            return true;
        }

        // Проверяем, изучен ли рецепт
        if (recipeEntry.isStudiedByPlayer(player.getUniqueID())) {
            return true;
        }

        // Проверяем, может ли игрок изучить рецепт
        PlayerExperienceData expData = ExperienceManager.getInstance().getPlayerData(player);
        if (recipeEntry.canPlayerStudy(player, expData)) {
            // Показываем рецепт, но крафт будет заблокирован CraftingHandler'ом
            return true;
        }

        // Рецепт недоступен
        return false;
    }

    /**
     * Получает оригинальный рецепт
     */
    public IRecipe getOriginalRecipe() {
        return originalRecipe;
    }

    /**
     * Получает RecipeEntry
     */
    public RecipeEntry getRecipeEntry() {
        return recipeEntry;
    }
}
