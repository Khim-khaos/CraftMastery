package com.khimkhaosow.craftmastery.proxy;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.config.ModConfig;
import com.khimkhaosow.craftmastery.crafting.ItemUsageHandler;
import com.khimkhaosow.craftmastery.crafting.CraftingHandler;
import com.khimkhaosow.craftmastery.crafting.RecipeFilter;
import com.khimkhaosow.craftmastery.commands.CommandRegistry;
import com.khimkhaosow.craftmastery.network.NetworkHandler;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.tabs.TabManager;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        // Инициализация конфигурации
        ModConfig.init(event);

        // Инициализация сети
        NetworkHandler.init();

        // Регистрация обработчиков событий
        MinecraftForge.EVENT_BUS.register(ExperienceManager.getInstance());
        MinecraftForge.EVENT_BUS.register(PermissionManager.getInstance());
        MinecraftForge.EVENT_BUS.register(CraftingHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(RecipeFilter.getInstance());
        MinecraftForge.EVENT_BUS.register(new ItemUsageHandler());

        CraftMastery.logger.info("CommonProxy Pre-Init completed");
    }

    public void init(FMLInitializationEvent event) {
        // Инициализация менеджеров
        PermissionManager.getInstance();
        ExperienceManager.getInstance();
        TabManager.getInstance();
        
        // Инициализируем RecipeManager для оборачивания рецептов
        RecipeManager.getInstance();

        CraftMastery.logger.info("CommonProxy Init completed");
    }

    public void postInit(FMLPostInitializationEvent event) {
        // Синхронизация конфигурации с менеджерами
        ModConfig.syncWithExperienceManager(ExperienceManager.getInstance());

        CraftMastery.logger.info("CommonProxy Post-Init completed");
    }

    public void serverStarting(FMLServerStartingEvent event) {
        // Регистрация команд
        CommandRegistry.registerCommands(event);

        CraftMastery.logger.info("CommonProxy Server Starting completed");
    }
}
