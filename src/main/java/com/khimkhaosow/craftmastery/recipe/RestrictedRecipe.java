package com.khimkhaosow.craftmastery.recipe;

import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;
import com.khimkhaosow.craftmastery.crafting.CraftingHandler;

/**
 * Обёртка для рецепта, которая проверяет, может ли игрок использовать рецепт
 */
public class RestrictedRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    private final IRecipe original;
    private final ResourceLocation originalId;

    public RestrictedRecipe(IRecipe original) {
        this.original = original;
        this.originalId = original.getRegistryName();
        setRegistryName(originalId);
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        // Проверяем доступ игрока к рецепту
        EntityPlayer player = findPlayer(inv);
        if (player != null) {
            RecipeEntry entry = RecipeManager.getInstance().getRecipe(originalId);
            if (entry != null && !entry.isStudiedByPlayer(player.getUniqueID())) {
                return false;
            }
        }
        return original.matches(inv, world);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        EntityPlayer player = findPlayer(inv);
        if (player != null) {
            RecipeEntry entry = RecipeManager.getInstance().getRecipe(originalId);
            if (entry != null && !entry.isStudiedByPlayer(player.getUniqueID())) {
                return ItemStack.EMPTY;
            }
        }
        return original.getCraftingResult(inv);
    }

    @Override
    public boolean canFit(int width, int height) {
        return original.canFit(width, height);
    }

    @Override
    public ItemStack getRecipeOutput() {
        return original.getRecipeOutput();
    }

    private EntityPlayer findPlayer(InventoryCrafting inv) {
        try {
            // Получаем игрока через CraftingHandler
            return CraftingHandler.getInstance().getCurrentPlayer();
        } catch (Exception e) {
            return null;
        }
    }

    public IRecipe getOriginal() {
        return original;
    }
}