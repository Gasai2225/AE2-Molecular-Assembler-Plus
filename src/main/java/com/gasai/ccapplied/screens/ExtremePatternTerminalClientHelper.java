package com.gasai.ccapplied.screens;

import com.gasai.ccapplied.core.client.CCAppliedInitScreens;
import com.gasai.ccapplied.core.registry.CCMenuTypes;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
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
            CCAppliedInitScreens.register(CCMenuTypes.EXTREME_PATTERN_TERM.get(),
                    ExtremePatternEncodingTermScreen::new, "/screens/ccterminal/extreme_pattern_encoding_terminal.json");
            if (CCOptionalMods.isDraconicEvolutionLoaded()) {
                CCAppliedInitScreens.register(CCMenuTypes.DRACONIC_PATTERN_TERM.get(),
                        DraconicPatternEncodingTermScreen::new, "/screens/ccterminal/draconic_pattern_encoding_terminal.json");
            }
            CCAppliedInitScreens.register(CCMenuTypes.EXTREME_MOLECULAR_ASSEMBLER.get(),
                    ExtremeMolecularAssemblerScreen::new, "/screens/extreme_molecular_assembler.json");
        });
    }
}
