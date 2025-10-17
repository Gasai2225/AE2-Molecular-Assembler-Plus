package com.gasai.ccapplied.core.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.gasai.ccapplied.CCApplied;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientInit {
    private ClientInit() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            // Инициализируем рендер типы
            InitRenderTypes.init();
            
            // Инициализируем рендереры блоков
            InitBlockEntityRenderers.init();
        });
        
        // Клиентская инициализация CCApplied
        CCApplied.LOG.info("[ClientInit] CCApplied client initialization complete");
    }
    
    @SubscribeEvent
    public static void onModelRegistry(ModelEvent.RegisterAdditional event) {
        // Регистрируем дополнительные модели
        InitAdditionalModels.init(event);
    }
}


