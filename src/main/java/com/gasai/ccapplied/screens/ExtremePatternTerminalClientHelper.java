package com.gasai.ccapplied.screens;

import com.gasai.ccapplied.core.client.CCAppliedInitScreens;
import com.gasai.ccapplied.core.registry.CCMenuTypes;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
import com.gasai.ccapplied.menus.ExtremePatternEncodingTermMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client helper that registers CCApplied screens
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ExtremePatternTerminalClientHelper {

    private ExtremePatternTerminalClientHelper() {}

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
            CCAppliedInitScreens.register(event, CCMenuTypes.EXTREME_PATTERN_TERM.get(),
                    ExtremePatternEncodingTermScreen::new, "/screens/ccterminal/extreme_pattern_encoding_terminal.json");
            if (CCOptionalMods.isDraconicEvolutionLoaded()) {
                CCAppliedInitScreens.register(event, CCMenuTypes.DRACONIC_PATTERN_TERM.get(),
                        DraconicPatternEncodingTermScreen::new, "/screens/ccterminal/draconic_pattern_encoding_terminal.json");
            }
            CCAppliedInitScreens.register(event, CCMenuTypes.EXTREME_MOLECULAR_ASSEMBLER.get(),
                    ExtremeMolecularAssemblerScreen::new, "/screens/extreme_molecular_assembler.json");
    }
}
