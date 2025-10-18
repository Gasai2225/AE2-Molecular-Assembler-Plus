package com.gasai.ccapplied.core.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientInit {
    private ClientInit() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            InitRenderTypes.init();
            
            InitBlockEntityRenderers.init();
        });
        
    }
    
    @SubscribeEvent
    public static void onModelRegistry(ModelEvent.RegisterAdditional event) {
        InitAdditionalModels.init(event);
    }
}


