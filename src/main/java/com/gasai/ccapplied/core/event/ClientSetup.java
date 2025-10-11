package com.gasai.ccapplied.core.event;

import com.gasai.ccapplied.client.gui.ExtremeMolecularAssemblerScreen;
import com.gasai.ccapplied.core.registry.ModBlocks;
import com.gasai.ccapplied.core.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.EXTREME_ASSEMBLER.get(), RenderType.cutout());
            net.minecraft.client.gui.screens.MenuScreens.register(
                    ModMenus.EXTREME_ASSEMBLER.get(),
                    com.gasai.ccapplied.client.gui.ExtremeMolecularAssemblerScreen::new
            );
        });
    }
}
