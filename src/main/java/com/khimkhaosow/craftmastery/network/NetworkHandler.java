package com.khimkhaosow.craftmastery.network;

import com.khimkhaosow.craftmastery.util.Reference;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
// Добавлен импорт Side
import net.minecraftforge.fml.relauncher.Side; // Добавлен
// --- Добавлен импорт MessageExperienceSync ---
import com.khimkhaosow.craftmastery.network.messages.MessageExperienceSync;

/**
 * Обработчик сетевых сообщений
 */
public class NetworkHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID);
    private static int discriminator = 0;

    public static void init() {
        // Регистрируем сообщения
        // Теперь MessageExperienceSync.Handler.class и MessageExperienceSync.class должны быть найдены
        INSTANCE.registerMessage(MessageExperienceSync.Handler.class, MessageExperienceSync.class, discriminator++, Side.CLIENT);
    }
}