package com.khimkhaosow.craftmastery.crafting;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;

/**
 * Handler for blocking usage of items/blocks if related recipes are not learned
 */
public class ItemUsageHandler {

    public ItemUsageHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world.isRemote) return;

        Block block = event.getWorld().getBlockState(event.getPos()).getBlock();

        // Block workbench usage
        if (block instanceof BlockWorkbench) {
            if (!canUseWorkbench(player)) {
                event.setCanceled(true);
                event.setResult(Result.DENY);
                player.sendMessage(new TextComponentString("Я не знаю, как этим пользоваться!"));
                CraftMastery.logger.info("Blocked workbench usage for player: {}", player.getName());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world.isRemote) return;

        ItemStack item = event.getItemStack();

        // Block usage of any item if related recipe not learned
        if (!canUseItem(player, item)) {
            event.setCanceled(true);
            event.setResult(Result.DENY);
            player.sendMessage(new TextComponentString("Я не умею этим пользоваться!"));
            CraftMastery.logger.info("Blocked item usage for player: {}, item: {}", player.getName(), item.getDisplayName());
        }
    }

    private boolean canUseWorkbench(EntityPlayer player) {
        // Check if player has learned basic crafting recipes or workbench-related
        // For simplicity, check if any plank recipe is learned (since workbench crafts planks)
        RecipeEntry plankRecipe = RecipeManager.getInstance().getRecipe("minecraft:oak_planks");
        if (plankRecipe != null && plankRecipe.isStudiedByPlayer(player.getUniqueID())) {
            return true;
        }
        return false; // Block if no basic recipes learned
    }

    private boolean canUseItem(EntityPlayer player, ItemStack item) {
        // Check if the recipe for this item is learned
        RecipeEntry itemRecipe = RecipeManager.getInstance().getRecipe(item.getItem().getRegistryName());
        if (itemRecipe == null || itemRecipe.isStudiedByPlayer(player.getUniqueID())) {
            return true;
        }
        return false;
    }
}
