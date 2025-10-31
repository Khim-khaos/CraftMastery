package com.khimkhaosow.craftmastery.proxy;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.crafting.CraftingGuiHandler;
import com.khimkhaosow.craftmastery.crafting.RecipeButtonHider;
import com.khimkhaosow.craftmastery.gui.GuiHandler;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        // Инициализация клиентских компонентов
        GuiHandler.init();

        // Инициализация обработчика GUI крафта
        CraftMastery.logger.info("ClientProxy preInit: creating CraftingGuiHandler");
        new CraftingGuiHandler();

        // Инициализация скрывателя кнопок рецептов
        CraftMastery.logger.info("ClientProxy preInit: creating RecipeButtonHider");
        new RecipeButtonHider();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // Дополнительная клиентская инициализация
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);

        // Клиентская пост-инициализация
    }
}
