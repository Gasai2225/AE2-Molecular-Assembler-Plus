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
            if (CCOptionalMods.isBotaniaLoaded()) {
                CCAppliedInitScreens.register(com.gasai.ccapplied.botania.BotaniaContent.ASSEMBLER_MENU.get(),
                        com.gasai.ccapplied.botania.client.BotanicalAssemblerScreen::new, "/screens/botanical_assembler.json");
                CCAppliedInitScreens.register(com.gasai.ccapplied.botania.BotaniaContent.MAGICAL_TERMINAL_MENU.get(),
                        com.gasai.ccapplied.botania.client.MagicalTerminalScreen::new, "/screens/magical_terminal.json");
                CCAppliedInitScreens.register(com.gasai.ccapplied.botania.BotaniaContent.MANA_BUS_MENU.get(),
                        com.gasai.ccapplied.botania.client.ManaBusScreen::new, "/screens/mana_bus.json");
            }
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
