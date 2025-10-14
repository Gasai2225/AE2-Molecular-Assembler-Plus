package com.gasai.ccapplied;

import com.gasai.ccapplied.core.registry.ModBlockEntities;
import com.gasai.ccapplied.core.registry.ModBlocks;
import com.gasai.ccapplied.core.registry.ModItems;
import com.gasai.ccapplied.core.registry.ModMenus;
import com.gasai.ccapplied.core.registry.ModParts;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CCApplied.MODID)
public final class CCApplied {

    public static final String MODID = "ccapplied";
    public static final Logger LOGGER = LoggerFactory.getLogger(CCApplied.class);

    public CCApplied() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Регистрация компонентов мода
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModParts.register(modEventBus); // Регистрация Part'ов

        LOGGER.info("CCApplied initialized!");
    }
}