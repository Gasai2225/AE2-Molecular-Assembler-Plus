package com.gasai.ccapplied.core.client;

import appeng.api.util.AEColor;
import com.gasai.ccapplied.core.registry.CCItems;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
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
            if (CCOptionalMods.isBotaniaLoaded()) {
                appeng.api.client.AEKeyRendering.register(
                        com.gasai.ccapplied.botania.ManaKeyType.INSTANCE,
                        com.gasai.ccapplied.botania.ManaKey.class,
                        new com.gasai.ccapplied.botania.client.ManaKeyRenderer());
            }
            InitRenderTypes.init();
            
            InitBlockEntityRenderers.init();
        });
        
    }
    
    @SubscribeEvent
    public static void onModelRegistry(ModelEvent.RegisterAdditional event) {
        InitAdditionalModels.init(event);
    }

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        if (CCOptionalMods.isBotaniaLoaded()) {
            for (var entry : com.gasai.ccapplied.botania.BotanicalAssemblers.ENTRIES.values())
                event.register((state, level, pos, tint) ->
                        com.gasai.ccapplied.botania.client.BotanicalAssemblerRenderer.FRAME_COLOR, entry.block().get());
        }
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        if (CCOptionalMods.isBotaniaLoaded())
            event.register((stack, tint) -> com.gasai.ccapplied.botania.client.BotanicalAssemblerRenderer.FRAME_COLOR,
                    com.gasai.ccapplied.botania.BotaniaContent.ASSEMBLER_FRAME.get());
        if (CCOptionalMods.isBotaniaLoaded()) {
            event.register(
                    (stack, tintIndex) -> AEColor.TRANSPARENT.getVariantByTintIndex(tintIndex),
                    com.gasai.ccapplied.botania.BotaniaContent.MAGICAL_TERMINAL.get());
        }
        event.register(
                (stack, tintIndex) -> AEColor.TRANSPARENT.getVariantByTintIndex(tintIndex),
                CCItems.EXTREME_PATTERN_TERMINAL.get());
        if (CCOptionalMods.isDraconicEvolutionLoaded()) {
            event.register(
                    (stack, tintIndex) -> AEColor.TRANSPARENT.getVariantByTintIndex(tintIndex),
                    CCItems.DRACONIC_PATTERN_TERMINAL.get());
        }
    }
}

