package com.gasai.ccapplied.core.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientInit {
    private ClientInit() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        // Регистрация экранов теперь происходит в соответствующих хелперах
        // Например, ExtremePatternTerminalClientHelper для экстремального терминала
    }
}


