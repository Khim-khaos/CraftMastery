package com.khimkhaosow.craftmastery.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;

/**
 * Guard for crafting checks using a ThreadLocal current player.
 */
public final class CraftingGuard {
    private static final ThreadLocal<EntityPlayer> currentPlayer = new ThreadLocal<>();

    private CraftingGuard() {}

    public static void setCurrentPlayer(EntityPlayer player) {
        currentPlayer.set(player);
    }

    public static void clear() {
        currentPlayer.remove();
    }

    public static EntityPlayer getCurrentPlayer() {
        return currentPlayer.get();
    }

    /**
     * Проверяет, разрешено ли использовать рецепт текущим игроком.
     * Если нет текущего игрока, возвращает true (поведение ванили).
     */
    public static boolean isRecipeAllowed(IRecipe recipe, InventoryCrafting craftMatrix, World world) {
        EntityPlayer player = getCurrentPlayer();
        if (player == null) return true;

        try {
            if (recipe == null) return true;
            if (recipe.getRegistryName() == null) return true;

            RecipeEntry entry = RecipeManager.getInstance().getRecipe(recipe.getRegistryName());
            if (entry == null) return true; // не наш рецепт

            // если изучен — разрешаем
            if (entry.isStudiedByPlayer(player.getUniqueID())) return true;

            // если игрок может изучить — считаем недоступным (предложим изучить)
            if (entry.canPlayerStudy(player, ExperienceManager.getInstance().getPlayerData(player))) {
                return false;
            }

            // иначе недоступен
            return false;
        } catch (Throwable t) {
            // На случай ошибок — не ломаем ваниль
            return true;
        }
    }
}
