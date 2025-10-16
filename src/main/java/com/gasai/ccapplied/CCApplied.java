package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.integration.ae2.pattern.extreme.ExtremePatternItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;


@Mod(CCApplied.MODID)
public final class CCApplied {

    public static final String MODID = "ccapplied";
    public static final Logger LOG = LogUtils.getLogger();

    public CCApplied() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        CCMenus.MENUS.register(modBus);
        CCItems.ITEMS.register(modBus);
        ExtremePatternItems.ITEMS.register(modBus);
        // CCItems/CCBlocks и т.д. — тоже тут, если ещё не подключены
    }


    @Mod.EventBusSubscriber(modid = CCApplied.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class CCClient {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent e) {
        }
    }

    private static void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            appeng.api.crafting.PatternDetailsHelper.registerDecoder(
                    com.gasai.ccapplied.integration.ae2.pattern.extreme.ExtremePatternDecoder.INSTANCE
            );
        });
    }
}