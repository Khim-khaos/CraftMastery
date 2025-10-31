package com.khimkhaosow.craftmastery.crafting;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Обработчик крафта для блокировки неизученных рецептов
 */
public class CraftingHandler {

    private static CraftingHandler instance;
    private ThreadLocal<net.minecraft.entity.player.EntityPlayer> currentPlayer = new ThreadLocal<>();

    public CraftingHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    /**
     * Получает текущего игрока из контекста крафта
     */
    public net.minecraft.entity.player.EntityPlayer getCurrentPlayer() {
        return currentPlayer.get();
    }

    /**
     * Устанавливает текущего игрока в контекст крафта
     */
    public void setCurrentPlayer(net.minecraft.entity.player.EntityPlayer player) {
        if (player == null) {
            currentPlayer.remove();
        } else {
            currentPlayer.set(player);
        }
    }

    public static CraftingHandler getInstance() {
        if (instance == null) {
            instance = new CraftingHandler();
        }
        return instance;
    }

    /**
     * Проверяет, может ли игрок использовать рецепт для крафта
     */
    public boolean canPlayerCraftRecipe(net.minecraft.entity.player.EntityPlayer player, IRecipe recipe) {
        if (player == null || recipe == null) return true;

        // Проверяем права доступа
        if (!PermissionManager.getInstance().hasPermission(player, PermissionType.LEARN_RECIPES)) {
            return false;
        }

        // Получаем RecipeEntry для этого рецепта
        RecipeEntry recipeEntry = RecipeManager.getInstance().getRecipe(recipe.getRegistryName());
        if (recipeEntry == null) {
            // Если рецепт не найден в системе CraftMastery, блокируем крафт
            CraftMastery.logger.info("Блокировка крафта - рецепт не в системе: {}", recipe.getRegistryName());
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "Этот рецепт не добавлен в систему CraftMastery и заблокирован."));
            return false;
        }

        // Проверяем, изучен ли рецепт
        if (recipeEntry.isStudiedByPlayer(player.getUniqueID())) {
            CraftMastery.logger.info("Крафт разрешен - рецепт изучен: {}", recipe.getRegistryName());
            return true;
        }

        // Проверяем, может ли игрок изучить рецепт
        PlayerExperienceData expData = ExperienceManager.getInstance().getPlayerData(player);
        if (recipeEntry.canPlayerStudy(player, expData)) {
            // Предлагаем изучить рецепт
            suggestRecipeStudy(player, recipeEntry);
            CraftMastery.logger.info("Крафт заблокирован - рецепт доступен для изучения: {}", recipe.getRegistryName());
            return false;
        }

        // Рецепт недоступен
        CraftMastery.logger.info("Крафт заблокирован - рецепт недоступен: {}", recipe.getRegistryName());
        player.sendMessage(new TextComponentString(
            TextFormatting.RED + "Рецепт '" + recipe.getRecipeOutput().getDisplayName() +
            "' недоступен. Изучите его в интерфейсе CraftMastery (клавиша G)"));

        return false;
    }

    /**
     * Предлагает игроку изучить рецепт
     */
    private void suggestRecipeStudy(net.minecraft.entity.player.EntityPlayer player, RecipeEntry recipeEntry) {
        String recipeName = recipeEntry.getRecipeResult().getDisplayName();
        int cost = recipeEntry.getRequiredLearningPoints();

        player.sendMessage(new TextComponentString(
            TextFormatting.YELLOW + "Рецепт '" + recipeName + "' доступен для изучения!"));
        player.sendMessage(new TextComponentString(
            TextFormatting.WHITE + "Стоимость: " + cost + " очков изучения"));
        player.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "Нажмите G для открытия интерфейса CraftMastery"));
    }

    /**
     * Обработчик крафта предмета
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemCrafted(ItemCraftedEvent event) {
        if (event.player == null || event.crafting.isEmpty()) return;
        
        // Устанавливаем текущего игрока для контекста крафта
        setCurrentPlayer(event.player);

        net.minecraft.entity.player.EntityPlayer player = event.player;

        // Проверяем права доступа
        if (!PermissionManager.getInstance().hasPermission(player, PermissionType.LEARN_RECIPES)) {
            cancelCrafting(event, player, "У вас нет прав на крафт");
            return;
        }

        // Находим рецепт, который использовался для крафта
        IRecipe usedRecipe = findRecipeFromOutput(event.craftMatrix, event.crafting);
        if (usedRecipe == null) {
            if (!PermissionManager.getInstance().hasPermission(player, PermissionType.ADMIN_SETTINGS)) {
                cancelCrafting(event, player, "Неизвестный рецепт");
            }
            return;
        }

        // Получаем RecipeEntry
        RecipeEntry recipeEntry = RecipeManager.getInstance().getRecipe(usedRecipe.getRegistryName());
        if (recipeEntry == null) {
            // Это рецепт не в системе CraftMastery - блокируем
            CraftMastery.logger.info("Блокировка крафта в onItemCrafted - рецепт не в системе: {}", usedRecipe.getRegistryName());
            cancelCrafting(event, player, "Этот рецепт не добавлен в систему CraftMastery");
            return;
        }

        // Проверяем изучен ли рецепт
        if (!recipeEntry.isStudiedByPlayer(player.getUniqueID())) {
            // Отменяем крафт до того, как предмет появится в инвентаре
            CraftMastery.logger.info("Блокировка крафта в onItemCrafted - рецепт не изучен: {}", usedRecipe.getRegistryName());
            cancelCrafting(event, player, "Рецепт '" + usedRecipe.getRecipeOutput().getDisplayName() + "' не изучен");
            
            // Показываем информацию о рецепте
            suggestRecipeStudy(player, recipeEntry);
            return;
        }

        CraftMastery.logger.info("Крафт разрешен в onItemCrafted: {}", usedRecipe.getRegistryName());

        // Рецепт изучен - начисляем опыт за крафт
        awardCraftingExperience(player, usedRecipe);
    }

    /**
     * Отменяет крафт и возвращает материалы
     */
    private void cancelCrafting(ItemCraftedEvent event, net.minecraft.entity.player.EntityPlayer player, String reason) {
        CraftMastery.logger.info("Отмена крафта: {}", reason);

        // Делаем копию результата до того, как обнулим стек
        ItemStack craftedCopy = ItemStack.EMPTY;
        if (event.crafting != null && !event.crafting.isEmpty()) {
            craftedCopy = event.crafting.copy();
        }

        // Обнуляем предмет, который должен был выдаться игроку
        event.crafting.setCount(0);

        // Очищаем курсор игрока, если там результат крафта
        try {
            net.minecraft.item.ItemStack cursorStack = player.inventory.getItemStack();
            if (!cursorStack.isEmpty() && !craftedCopy.isEmpty()
                && ItemStack.areItemsEqual(cursorStack, craftedCopy)
                && ItemStack.areItemStackTagsEqual(cursorStack, craftedCopy)) {
                CraftMastery.logger.info("Очищен курсор от заблокированного рецепта: {}", cursorStack.getItem().getRegistryName());
                player.inventory.setItemStack(net.minecraft.item.ItemStack.EMPTY);
            }
        } catch (Exception e) {
            CraftMastery.logger.warn("Could not clear cursor: {}", e.getMessage());
        }

        // Для shift-клика предмет, возможно, уже добавлен в инвентарь
        if (!craftedCopy.isEmpty()) {
            int amountToRemove = craftedCopy.getCount();
            CraftMastery.logger.info("Удаляем {} предмет(ов) из инвентаря игрока", amountToRemove);

            for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
                ItemStack invStack = player.inventory.getStackInSlot(slot);
                if (!invStack.isEmpty()
                    && ItemStack.areItemsEqual(invStack, craftedCopy)
                    && ItemStack.areItemStackTagsEqual(invStack, craftedCopy)) {
                    int remove = Math.min(amountToRemove, invStack.getCount());
                    invStack.shrink(remove);
                    amountToRemove -= remove;

                    if (invStack.getCount() <= 0) {
                        player.inventory.setInventorySlotContents(slot, ItemStack.EMPTY);
                    }

                    if (amountToRemove <= 0) {
                        break;
                    }
                }
            }

            if (amountToRemove > 0) {
                CraftMastery.logger.info("Осталось удалить {} предмет(ов) — возможно, они уже отсутствовали", amountToRemove);
            }
        }

        // Возвращаем материалы из сетки крафта
        for (int i = 0; i < event.craftMatrix.getSizeInventory(); i++) {
            ItemStack stack = event.craftMatrix.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ItemStack returnStack = stack.copy();
                if (!player.inventory.addItemStackToInventory(returnStack)) {
                    player.dropItem(returnStack, false);
                }
            }
        }

        // Очищаем сетку крафта
        try {
            event.craftMatrix.clear();
        } catch (Exception ignore) { }

        // Уведомляем игрока
        player.sendMessage(new TextComponentString(TextFormatting.RED + "Крафт отменен! " + reason));
    }

    /**
     * Возвращает ресурсы игроку при отмене крафта
     */
    private void refundCraftingMaterials(net.minecraft.entity.player.EntityPlayer player, IInventory craftMatrix) {
        // Возвращаем предметы из сетки крафта обратно в инвентарь игрока
        for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
            ItemStack stack = craftMatrix.getStackInSlot(i);
            if (!stack.isEmpty()) {
                // Пытаемся добавить предмет в инвентарь игрока
                if (!player.inventory.addItemStackToInventory(stack)) {
                    // Если инвентарь полон, выбрасываем предмет
                    player.dropItem(stack, false);
                }
            }
        }
    }

    /**
     * Находит рецепт по результату крафта
     */
    private IRecipe findRecipeFromOutput(IInventory craftMatrix, ItemStack output) {
        if (output.isEmpty()) return null;

        // Проверяем все рецепты
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            if (recipe.getRecipeOutput().isEmpty()) continue;

            // Сравниваем результаты
            if (ItemStack.areItemsEqual(recipe.getRecipeOutput(), output) &&
                ItemStack.areItemStackTagsEqual(recipe.getRecipeOutput(), output)) {

                // Проверяем, может ли этот рецепт быть использован с текущими ингредиентами
                // Временно упрощаем проверку - просто проверяем, что результат совпадает
                if (recipe.getRecipeOutput().getCount() <= output.getCount()) {
                    return recipe;
                }
            }
        }

        return null;
    }

    /**
     * Начисляет опыт за крафт
     */
    private void awardCraftingExperience(net.minecraft.entity.player.EntityPlayer player, IRecipe recipe) {
        if (!com.khimkhaosow.craftmastery.config.ModConfig.enableCraftingExperience) {
            return;
        }

        PlayerExperienceData expData = ExperienceManager.getInstance().getPlayerData(player);

        // Базовый опыт за крафт
        float baseExperience = 1.0f;

        // Множитель из конфигурации
        float multiplier = com.khimkhaosow.craftmastery.config.ModConfig.craftingMultiplier *
                          com.khimkhaosow.craftmastery.config.ModConfig.globalExperienceMultiplier;

        // Добавляем опыт
        expData.addExperience(com.khimkhaosow.craftmastery.experience.ExperienceType.CRAFTING,
                             baseExperience * multiplier);

        // Показываем уведомление если включено
        if (com.khimkhaosow.craftmastery.config.ModConfig.showExperienceNotifications) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "+ " + String.format("%.1f", baseExperience * multiplier) +
                " опыта за крафт"));
        }
    }

    /**
     * Обработчик входа игрока в мир
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        net.minecraft.entity.player.EntityPlayer player = event.player;

        // Показываем приветственное сообщение
        if (PermissionManager.getInstance().hasPermission(player, PermissionType.OPEN_INTERFACE)) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "Добро пожаловать в CraftMastery!"));
            player.sendMessage(new TextComponentString(
                TextFormatting.WHITE + "Нажмите " + TextFormatting.YELLOW + "G" +
                TextFormatting.WHITE + " для открытия интерфейса крафта"));
        }

        // Проверяем и обновляем данные игрока
        ExperienceManager.getInstance().getPlayerData(player);
    }

    /**
     * Проверяет, нужно ли скрывать рецепт в JEI для игрока
     */
    @SideOnly(Side.CLIENT)
    public boolean shouldHideRecipeInJEI(net.minecraft.entity.player.EntityPlayer player, IRecipe recipe) {
        if (player == null || recipe == null) return false;

        // Проверяем права
        if (!PermissionManager.getInstance().hasPermission(player, PermissionType.LEARN_RECIPES)) {
            return true; // Скрываем все рецепты если нет прав
        }

        // Получаем RecipeEntry
        RecipeEntry recipeEntry = RecipeManager.getInstance().getRecipe(recipe.getRegistryName());
        if (recipeEntry == null) return false;

        // Скрываем, если рецепт не изучен и не доступен для изучения
        if (!recipeEntry.isStudiedByPlayer(player.getUniqueID())) {
            PlayerExperienceData expData = ExperienceManager.getInstance().getPlayerData(player);
            return !recipeEntry.canPlayerStudy(player, expData);
        }

        return false;
    }
}