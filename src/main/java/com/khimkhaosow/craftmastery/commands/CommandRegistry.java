package com.khimkhaosow.craftmastery.commands;

import com.khimkhaosow.craftmastery.CraftMastery;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * Регистратор команд мода
 */
public class CommandRegistry {

    /**
     * Регистрирует все команды мода
     */
    public static void registerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandCraftMastery());
        event.registerServerCommand(new CommandRecipes());

        CraftMastery.logger.info("Registered CraftMastery commands");
    }
}
