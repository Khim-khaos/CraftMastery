package com.khimkhaosow.craftmastery.crafting;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

@SideOnly(Side.CLIENT)
public class CraftingGuiHandler {

    private static final String[] GUI_CONTAINER_INVENTORY_SLOTS = {"inventorySlots", "field_147002_h"};
    private static final String[] CONTAINER_WORKBENCH_WORLD = {"world", "worldObj", "field_75170_e"};
    private static final String[] CONTAINER_WORKBENCH_POS = {"pos", "field_178146_j"};

    public CraftingGuiHandler() {
        MinecraftForge.EVENT_BUS.register(this);
        CraftMastery.logger.info("CraftingGuiHandler registered on event bus");
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!(event.getGui() instanceof GuiCrafting)) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) {
            return;
        }

        GuiCrafting gui = (GuiCrafting) event.getGui();
        CraftMastery.logger.info("GuiCrafting opened, attempting to guard container");
        replaceContainer(gui, player);
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!(event.getGui() instanceof GuiCrafting) && !(event.getGui() instanceof GuiInventory)) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) {
            return;
        }

        if (event.getGui() instanceof GuiCrafting) {
            clearResultIfBlocked((GuiCrafting) event.getGui(), player);
        } else if (event.getGui() instanceof GuiInventory) {
            clearInventoryResultIfBlocked((GuiInventory) event.getGui(), player);
        }
    }

    private void replaceContainer(GuiCrafting gui, EntityPlayer player) {
        try {
            Container original = getContainer(gui);
            if (original == null) {
                CraftMastery.logger.info("GuiCrafting has no container instance, skipping guard");
                return;
            }
            if (original instanceof BlockedContainerWorkbench) {
                CraftMastery.logger.info("GuiCrafting container already guarded");
                return; // уже заменён
            }
            if (!(original instanceof ContainerWorkbench)) {
                CraftMastery.logger.info("GuiCrafting container is not a ContainerWorkbench ({}), skipping guard", original.getClass().getName());
                return;
            }

            ContainerWorkbench workbench = (ContainerWorkbench) original;
            World world = getWorkbenchWorld(workbench, player.world);
            BlockPos pos = getWorkbenchPos(workbench);

            BlockedContainerWorkbench blocked = new BlockedContainerWorkbench(player, world, pos);
            blocked.windowId = original.windowId;

            blocked.syncCraftMatrixFrom(workbench);

            setContainer(gui, blocked);
            if (player.openContainer == original) {
                player.openContainer = blocked;
            }

            blocked.updateResultVisibility();
            CraftMastery.logger.info("Replaced crafting container with guarded variant");
        } catch (Exception e) {
            CraftMastery.logger.warn("Could not replace crafting container", e);
        }
    }

    private Container getContainer(GuiContainer gui) throws Exception {
        Field field = findField(gui.getClass(), GUI_CONTAINER_INVENTORY_SLOTS);
        return (Container) field.get(gui);
    }

    private void setContainer(GuiCrafting gui, Container container) throws Exception {
        Field field = findField(gui.getClass(), GUI_CONTAINER_INVENTORY_SLOTS);
        field.set(gui, container);
    }

    private Field findField(Class<?> clazz, String[] names) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            for (String name : names) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {}
            }
            current = current.getSuperclass();
        }
        throw new NoSuchFieldException(names[0]);
    }

    private void clearResultIfBlocked(GuiCrafting gui, EntityPlayer player) {
        try {
            Container container = getContainer(gui);
            if (!(container instanceof BlockedContainerWorkbench)) {
                CraftMastery.logger.debug("GuiCrafting container is not guarded during draw ({}), skipping clear", container != null ? container.getClass().getName() : "null");
                return;
            }

            BlockedContainerWorkbench blocked = (BlockedContainerWorkbench) container;
            blocked.updateResultVisibility();
        } catch (Exception e) {
            CraftMastery.logger.warn("Could not clear crafting result", e);
        }
    }

    private void clearInventoryResultIfBlocked(GuiInventory gui, EntityPlayer player) {
        try {
            Container container = getContainer(gui);
            if (!(container instanceof ContainerPlayer)) {
                CraftMastery.logger.debug("GuiInventory container is not a ContainerPlayer ({}), skipping clear", container != null ? container.getClass().getName() : "null");
                return;
            }

            ContainerPlayer playerContainer = (ContainerPlayer) container;
            ItemStack result = playerContainer.craftResult.getStackInSlot(0);
            if (result.isEmpty() || isRecipeAllowed(player, playerContainer.craftMatrix)) {
                return;
            }

            playerContainer.craftResult.setInventorySlotContents(0, ItemStack.EMPTY);
            if (!playerContainer.inventorySlots.isEmpty()) {
                Slot resultSlot = playerContainer.inventorySlots.get(0);
                if (resultSlot != null) {
                    resultSlot.putStack(ItemStack.EMPTY);
                }
            }

            // Update slots to ensure GUI refreshes
            playerContainer.detectAndSendChanges();

            CraftMastery.logger.info("Cleared blocked player inventory crafting result on client");
        } catch (Exception e) {
            CraftMastery.logger.warn("Could not clear player inventory crafting result", e);
        }
    }

    private World getWorkbenchWorld(ContainerWorkbench workbench, World fallback) {
        try {
            World world = ObfuscationReflectionHelper.getPrivateValue(ContainerWorkbench.class, workbench, CONTAINER_WORKBENCH_WORLD);
            return world != null ? world : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private BlockPos getWorkbenchPos(ContainerWorkbench workbench) {
        try {
            BlockPos pos = ObfuscationReflectionHelper.getPrivateValue(ContainerWorkbench.class, workbench, CONTAINER_WORKBENCH_POS);
            return pos != null ? pos : BlockPos.ORIGIN;
        } catch (Exception ignored) {
            return BlockPos.ORIGIN;
        }
    }

    private static boolean isRecipeAllowed(EntityPlayer player, InventoryCrafting craftMatrix) {
        IRecipe recipe = CraftingManager.findMatchingRecipe(craftMatrix, player.world);
        if (recipe == null || recipe.getRegistryName() == null) {
            return false;
        }

        RecipeEntry entry = RecipeManager.getInstance().getRecipe(recipe.getRegistryName());
        if (entry == null) {
            return false;
        }

        return entry.isStudiedByPlayer(player.getUniqueID());
    }

    private static class BlockedContainerWorkbench extends ContainerWorkbench {
        private final EntityPlayer player;

        BlockedContainerWorkbench(EntityPlayer player, World world, BlockPos pos) {
            super(player.inventory, world, pos);
            this.player = player;
        }

        void syncCraftMatrixFrom(ContainerWorkbench original) {
            for (int i = 1; i <= 9 && i < original.inventorySlots.size() && i < this.inventorySlots.size(); i++) {
                ItemStack stack = original.inventorySlots.get(i).getStack();
                this.inventorySlots.get(i).putStack(stack.copy());
            }
            this.craftResult.setInventorySlotContents(0, ItemStack.EMPTY);
            if (!this.inventorySlots.isEmpty()) {
                this.inventorySlots.get(0).putStack(ItemStack.EMPTY);
            }
        }

        @Override
        public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
            if (index == 0) {
                ItemStack result = this.craftResult.getStackInSlot(0);
                if (!result.isEmpty() && !isRecipeAllowed(player, this.craftMatrix)) {
                    return ItemStack.EMPTY;
                }
            }

            return super.transferStackInSlot(playerIn, index);
        }

        @Override
        public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer playerIn) {
            if (slotId == 0) {
                ItemStack result = this.craftResult.getStackInSlot(0);
                if (!result.isEmpty() && !isRecipeAllowed(player, this.craftMatrix)) {
                    return ItemStack.EMPTY;
                }
            }

            ItemStack resultStack = super.slotClick(slotId, dragType, clickType, playerIn);
            updateResultVisibility();
            return resultStack;
        }

        @Override
        public void onCraftMatrixChanged(IInventory inventoryIn) {
            super.onCraftMatrixChanged(inventoryIn);
            updateResultVisibility();
        }

        void updateResultVisibility() {
            ItemStack result = this.craftResult.getStackInSlot(0);
            if (!result.isEmpty() && !isRecipeAllowed(player, this.craftMatrix)) {
                this.craftResult.setInventorySlotContents(0, ItemStack.EMPTY);
                if (!this.inventorySlots.isEmpty()) {
                    this.inventorySlots.get(0).putStack(ItemStack.EMPTY);
                }
                CraftMastery.logger.info("Cleared blocked crafting result on client");
            }
        }
    }
}