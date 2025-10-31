package com.khimkhaosow.craftmastery;

import com.khimkhaosow.craftmastery.proxy.CommonProxy;
import com.khimkhaosow.craftmastery.util.Reference;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION, acceptedMinecraftVersions = Reference.MC_VERSIONS)
public class CraftMastery {

    @Mod.Instance(Reference.MOD_ID)
    public static CraftMastery instance;

    @SidedProxy(clientSide = Reference.CLIENT_PROXY_CLASS, serverSide = Reference.COMMON_PROXY_CLASS)
    public static CommonProxy proxy;

    public static Logger logger = LogManager.getLogger(Reference.MOD_ID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger.info("CraftMastery Pre-Initialization started");

        proxy.preInit(event);

        logger.info("CraftMastery Pre-Initialization completed");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("CraftMastery Initialization started");

        proxy.init(event);

        logger.info("CraftMastery Initialization completed");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        logger.info("CraftMastery Post-Initialization started");

        proxy.postInit(event);

        logger.info("CraftMastery Post-Initialization completed");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        logger.info("CraftMastery Server Starting");

        proxy.serverStarting(event);

        logger.info("CraftMastery Server Starting completed");
    }
}
