package com.gasai.ccapplied.screens;

import com.gasai.ccapplied.core.client.CCAppliedInitScreens;
import com.gasai.ccapplied.core.registry.CCMenuTypes;
import com.gasai.ccapplied.menus.ExtremeMolecularAssemblerMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client helper that registers CCApplied screens
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ExtremePatternTerminalClientHelper {

    private ExtremePatternTerminalClientHelper() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {

        event.enqueueWork(() -> {
            try {
                CCAppliedInitScreens.register(
                    CCMenuTypes.EXTREME_PATTERN_TERM.get(),
                    ExtremePatternEncodingTermScreen::new,
                    "/screens/ccterminal/extreme_pattern_encoding_terminal.json"
                );
                
                MenuScreens.<ExtremeMolecularAssemblerMenu, ExtremeMolecularAssemblerScreen>register(
                    CCMenuTypes.EXTREME_MOLECULAR_ASSEMBLER.get(), 
                    (menu, playerInv, title) -> {
                               try {
                                   var style = appeng.client.gui.style.StyleManager.loadStyleDoc("/screens/extreme_molecular_assembler.json");
                                   return new ExtremeMolecularAssemblerScreen(menu, playerInv, title, style);
                               } catch (Exception e) {
                            throw new RuntimeException("Failed to create screen", e);
                        }
                    });
                
            } catch (Exception e) {
            }
        });
    }
}
