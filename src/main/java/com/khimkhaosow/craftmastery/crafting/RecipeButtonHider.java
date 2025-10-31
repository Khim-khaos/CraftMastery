package com.khimkhaosow.craftmastery.crafting;

// 1. Импорты для Forge SideOnly
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

// 3. Импорты для других компонентов мода
import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry; // Добавлен импорт RecipeEntry
import com.khimkhaosow.craftmastery.recipe.RecipeManager; // Добавлен импорт RecipeManager
import com.khimkhaosow.craftmastery.permissions.PermissionManager; // Добавлен импорт PermissionManager
import com.khimkhaosow.craftmastery.permissions.PermissionType; // Добавлен импорт PermissionType
import com.khimkhaosow.craftmastery.util.Reference; // Добавлен импорт Reference

// 4. Импорты для Minecraft/Forge
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.item.crafting.IRecipe;

// 5. Импорты для Java
import java.lang.reflect.Field;

/**
 * Обработчик событий GUI для скрытия кнопок рецептов
 */
@SideOnly(Side.CLIENT) // Side.CLIENT должен быть найден
public class RecipeButtonHider {

    private static final String[] GUI_SCREEN_BUTTON_LIST = {"buttonList", "field_146292_n"};
    private static final String[] GUI_CRAFTING_MATRIX = {"craftMatrix", "field_147003_i"};

    public RecipeButtonHider() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Обрабатывает инициализацию GUI
     */
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent // Аннотация SubscribeEvent должна быть найдена
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiCrafting) {
            GuiCrafting craftingGui = (GuiCrafting) event.getGui();
            EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player; // EntityPlayer должен быть найден

            if (player != null) {
                // Скрываем неизученные рецепты после инициализации GUI
                hideUnstudiedRecipeButtons(craftingGui, player);
            }
        }
    }

    /**
     * Обрабатывает клик по кнопке
     */
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent // Аннотация SubscribeEvent должна быть найдена
    public void onButtonClick(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getGui() instanceof GuiCrafting) {
            EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player; // EntityPlayer должен быть найден

            if (player != null && isRecipeButton(event.getButton())) {
                // Проверяем, является ли рецепт изученным
                IRecipe recipe = getRecipeFromButton(event.getButton());
                if (recipe != null) {
                    RecipeEntry recipeEntry = RecipeManager.getInstance().getRecipe(recipe.getRegistryName()); // RecipeEntry и RecipeManager должны быть найдены
                    if (recipeEntry != null && !recipeEntry.isStudiedByPlayer(player.getUniqueID())) {
                        // Отменяем клик по неизученному рецепту
                        event.setCanceled(true);

                        // Показываем сообщение игроку
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            net.minecraft.util.text.TextFormatting.RED + "Рецепт '" +
                            recipe.getRecipeOutput().getDisplayName() + "' не изучен!"));

                        // Показываем информацию о рецепте
                        suggestRecipeStudy(player, recipeEntry);
                    }
                }
            }
        }
    }

    /**
     * Скрывает кнопки неизученных рецептов
     */
    private void hideUnstudiedRecipeButtons(GuiCrafting craftingGui, EntityPlayer player) {
        // Проверяем права доступа
        if (!PermissionManager.getInstance().hasPermission(player, PermissionType.LEARN_RECIPES)) {
            return;
        }

        try {
            // --- Исправлено: используем GuiScreen для доступа к buttonList ---
            // Container container = craftingGui; // НЕПРАВИЛЬНО
            @SuppressWarnings("unchecked")
            java.util.List<GuiButton> buttonList = (java.util.List<GuiButton>) ObfuscationReflectionHelper.getPrivateValue(
                net.minecraft.client.gui.GuiScreen.class,
                craftingGui,
                GUI_SCREEN_BUTTON_LIST
            );

            net.minecraft.inventory.InventoryCrafting craftMatrix = ObfuscationReflectionHelper.getPrivateValue(
                GuiCrafting.class,
                craftingGui,
                GUI_CRAFTING_MATRIX
            );

            // Проходим по всем кнопкам
            for (GuiButton button : buttonList) {
                // Проверяем является ли это кнопкой рецепта
                if (isRecipeButton(button)) {
                    // Получаем рецепт через reflection
                    IRecipe recipe = getRecipeFromButton(button);
                    if (recipe != null) {
                        RecipeEntry recipeEntry = RecipeManager.getInstance().getRecipe(recipe.getRegistryName());
                        if (recipeEntry != null) {
                            // Проверяем изучен ли рецепт
                            if (!recipeEntry.isStudiedByPlayer(player.getUniqueID())) {
                                // Если рецепт не изучен:
                                button.visible = false; // Скрываем кнопку
                                button.enabled = false; // Отключаем кнопку
                                
                                // Очищаем результат крафта если это тот же рецепт
                                if (recipe.matches(craftMatrix, player.world)) {
                                    craftMatrix.clear();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            CraftMastery.logger.error("Failed to hide recipe buttons: {}", e.getMessage());
        }
    }

    /**
     * Получает рецепт из кнопки рецепта
     */
    private IRecipe getRecipeFromButton(GuiButton button) {
        // Список возможных имен полей для рецепта
        String[] recipeFieldNames = {"recipe", "field_146140_h", "craftingRecipe"};
        
        for (String fieldName : recipeFieldNames) {
            try {
                java.lang.reflect.Field field = button.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(button);
                if (value instanceof IRecipe) {
                    return (IRecipe) value;
                }
            } catch (Exception ignored) {
                // Продолжаем поиск
            }
        }

        // Если не нашли по известным именам, ищем по типу
        try {
            for (java.lang.reflect.Field field : button.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(button);
                if (value instanceof IRecipe) {
                    return (IRecipe) value;
                }
            }
        } catch (Exception ignored) {
            // Игнорируем ошибки
        }

        return null;
    }

    /**
     * Показывает информацию о том, как изучить рецепт
     */
    private void suggestRecipeStudy(EntityPlayer player, RecipeEntry recipeEntry) {
        PlayerExperienceData expData = ExperienceManager.getInstance().getPlayerData(player);

        player.sendMessage(new net.minecraft.util.text.TextComponentString(
            net.minecraft.util.text.TextFormatting.YELLOW + "Рецепт '" +
            recipeEntry.getRecipeId() + "' доступен для изучения!"));

        if (recipeEntry.canPlayerStudy(player, expData)) {
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                net.minecraft.util.text.TextFormatting.GREEN + "Стоимость: " +
                recipeEntry.getRequiredLearningPoints() + " очков изучения"));

            if (recipeEntry.getRequiredLevel() > 1) {
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.GREEN + "Требуемый уровень: " +
                    recipeEntry.getRequiredLevel()));
            }

            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                net.minecraft.util.text.TextFormatting.WHITE + "Нажмите G для открытия интерфейса CraftMastery"));
        } else {
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                net.minecraft.util.text.TextFormatting.RED + "Недостаточно очков изучения или уровня"));
        }
    }

    /**
     * Проверяет, является ли кнопка кнопкой рецепта
     */
    private boolean isRecipeButton(GuiButton button) {
        // Проверяем по имени класса, так как GuiRecipeButton может называться по-другому
        return button.getClass().getSimpleName().contains("Recipe");
    }
}