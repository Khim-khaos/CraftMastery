package com.khimkhaosow.craftmastery.integration.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * Интеграция с Just Enough Items (JEI)
 */
@JEIPlugin
public class JEIIntegration implements IModPlugin {

    private static boolean jeiLoaded = false;
    // Сохраняем реестр JEI для последующего обновления по требованию
    private static IModRegistry savedRegistry = null;
    private static int tickCounter = 0;

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        // Регистрация пользовательских категорий рецептов
        // В будущем можно добавить специальные категории для CraftMastery
    }

    @Override
    public void register(IModRegistry registry) {
        jeiLoaded = true;

        // Сохраняем реестр для обновлений в рантайме
        savedRegistry = registry;

        // Попытка скрыть рецепты сразу (если игрок уже в мире)
        hideUnstudiedRecipes(registry);

        // Регистрация клиентского тик-хендлера для периодического обновления видимости
        try {
            MinecraftForge.EVENT_BUS.register(new Object() {
                @SubscribeEvent
                public void onClientTick(ClientTickEvent event) {
                    // Обновляем примерно каждую секунду
                    if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) return;
                    if (++tickCounter < 20) return;
                    tickCounter = 0;
                    try {
                        if (savedRegistry != null) hideUnstudiedRecipes(savedRegistry);
                    } catch (Exception e) {
                        CraftMastery.logger.warn("JEI refresh failed: {}", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            CraftMastery.logger.warn("Could not register JEI refresh tick: {}", e.getMessage());
        }

        CraftMastery.logger.info("JEI integration initialized");
    }

    /**
     * Скрывает неизученные рецепты в JEI
     */
    private void hideUnstudiedRecipes(IModRegistry registry) {
        // Клиент должен быть в мире
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;
        if (player == null) return;

        List<ItemStack> outputsToHide = new ArrayList<>();

        // Получаем все рецепты из регистра Minecraft
        for (IRecipe recipe : net.minecraft.item.crafting.CraftingManager.REGISTRY) {
            if (recipe == null || recipe.getRecipeOutput() == null) continue;
            RecipeEntry entry = RecipeManager.getInstance().getRecipe(recipe.getRegistryName());
            
            // Если это рецепт CraftMastery и он не изучен - скрываем
            if (entry != null && !entry.isStudiedByPlayer(player.getUniqueID())) {
                if (!entry.canPlayerStudy(player, ExperienceManager.getInstance().getPlayerData(player))) {
                    outputsToHide.add(recipe.getRecipeOutput().copy());
                }
            }
        }

        if (!outputsToHide.isEmpty()) {
            try {
                // Используем JEI API для скрытия результатов (через IngredientBlacklist)
                for (ItemStack output : outputsToHide) {
                    try {
                        registry.getJeiHelpers().getIngredientBlacklist().addIngredientToBlacklist(output);
                    } catch (Exception ex) {
                        // Игнорируем отдельные ошибки для конкретных предметов
                        CraftMastery.logger.debug("Failed to blacklist {}: {}", output, ex.getMessage());
                    }
                }
                
                CraftMastery.logger.info("Hidden {} recipe outputs in JEI for player {}", outputsToHide.size(), player.getName());
            } catch (Exception e) {
                CraftMastery.logger.error("Could not hide recipes in JEI: {}", e.getMessage());
            }
        }
    }

    /**
     * Определяет, должен ли рецепт быть скрыт
     */
    private boolean shouldHideRecipe(RecipeEntry entry) {
        // Показываем только базовые minecraft рецепты
        if (entry.hasTag(com.khimkhaosow.craftmastery.recipe.RecipeTag.COMMON)) {
            return false;
        }

        // Проверяем требования для изучения
        return true;
    }

    /**
     * Показывает изученные рецепты в JEI
     */
    public static void showStudiedRecipes(EntityPlayer player) {
        if (!jeiLoaded || !Loader.isModLoaded("jei")) return;

        // В будущем: обновить отображение рецептов для конкретного игрока
        // CraftMastery.logger.debug("Updating JEI recipe visibility for player: {}", player.getName());
    }

    /**
     * Скрывает рецепты, которые игрок не может изучить
     */
    public static void hideUnavailableRecipes(EntityPlayer player) {
        if (!jeiLoaded || !Loader.isModLoaded("jei")) return;

        // В будущем: скрыть рецепты, которые игрок не может изучить
        // CraftMastery.logger.debug("Hiding unavailable recipes in JEI for player: {}", player.getName());
    }

    /**
     * Обновляет видимость рецептов для игрока
     */
    public static void updateRecipeVisibility(EntityPlayer player) {
        if (!jeiLoaded || !Loader.isModLoaded("jei")) return;

        showStudiedRecipes(player);
        hideUnavailableRecipes(player);
    }

    /**
     * Проверяет, загружен ли JEI
     */
    public static boolean isJEILoaded() {
        return jeiLoaded && Loader.isModLoaded("jei");
    }

    /**
     * Добавляет пользовательский рецепт в JEI
     */
    public static void addCustomRecipe(IRecipe recipe) {
        if (!jeiLoaded || !Loader.isModLoaded("jei")) return;

        // В будущем: добавить рецепт в JEI
        // CraftMastery.logger.debug("Adding custom recipe to JEI: {}", recipe.getRegistryName());
        // } catch (Exception e) {
        //     CraftMastery.logger.error("Error adding recipe to JEI: ", e);
        // }
    }

    /**
     * Удаляет рецепт из JEI
     */
    public static void removeRecipe(IRecipe recipe) {
        if (!jeiLoaded || !Loader.isModLoaded("jei")) return;

        // В будущем: удалить рецепт из JEI
        // CraftMastery.logger.debug("Removing recipe from JEI: {}", recipe.getRegistryName());
        // } catch (Exception e) {
        //     CraftMastery.logger.error("Error removing recipe from JEI: ", e);
        // }
    }
}