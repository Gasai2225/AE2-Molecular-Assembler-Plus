package com.gasai.ccapplied.core.client;

import appeng.api.util.AEColor;
import com.gasai.ccapplied.core.registry.CCItems;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientInit {
    private ClientInit() {}

    @SubscribeEvent
    public static void onClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        if (!CCOptionalMods.isBotaniaLoaded()) return;
        var extension = new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private com.gasai.ccapplied.botania.client.BotanicalAssemblerItemRenderer renderer;
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new com.gasai.ccapplied.botania.client.BotanicalAssemblerItemRenderer();
                return renderer;
            }
        };
        for (var entry : com.gasai.ccapplied.botania.BotanicalAssemblers.ENTRIES.values())
            event.registerItem(extension, entry.item().get());
    }

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
                    (stack, tintIndex) -> FastColor.ARGB32.opaque(AEColor.TRANSPARENT.getVariantByTintIndex(tintIndex)),
                    com.gasai.ccapplied.botania.BotaniaContent.MAGICAL_TERMINAL.get());
        }
        event.register(
                (stack, tintIndex) -> FastColor.ARGB32.opaque(AEColor.TRANSPARENT.getVariantByTintIndex(tintIndex)),
                CCItems.EXTREME_PATTERN_TERMINAL.get());
        if (CCOptionalMods.isDraconicEvolutionLoaded()) {
            event.register(
                    (stack, tintIndex) -> FastColor.ARGB32.opaque(AEColor.TRANSPARENT.getVariantByTintIndex(tintIndex)),
                    CCItems.DRACONIC_PATTERN_TERMINAL.get());
        }
    }
}

