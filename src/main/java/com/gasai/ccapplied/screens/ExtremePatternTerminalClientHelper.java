package com.gasai.ccapplied.screens;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.core.client.CCAppliedInitScreens;
import com.gasai.ccapplied.core.registry.CCMenuTypes;
import com.gasai.ccapplied.screens.ExtremePatternEncodingTermScreen;
import com.gasai.ccapplied.screens.ExtremeMolecularAssemblerScreen;
import com.gasai.ccapplied.menus.ExtremeMolecularAssemblerMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Хелпер для клиента, который регистрирует экраны CCApplied
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ExtremePatternTerminalClientHelper {

    private ExtremePatternTerminalClientHelper() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        CCApplied.LOG.info("[ExtremeClientHelper] Initializing CCApplied client screens");

        event.enqueueWork(() -> {
            try {
                // Регистрируем экран экстремального терминала кодирования паттернов
                CCAppliedInitScreens.register(
                    CCMenuTypes.EXTREME_PATTERN_TERM.get(),
                    ExtremePatternEncodingTermScreen::new,
                    "/screens/ccterminal/extreme_pattern_encoding_terminal.json"
                );
                
                // Регистрируем экран экстремального молекулярного ассемблера через стандартную систему Minecraft
                MenuScreens.<ExtremeMolecularAssemblerMenu, ExtremeMolecularAssemblerScreen>register(
                    CCMenuTypes.EXTREME_MOLECULAR_ASSEMBLER.get(), 
                    (menu, playerInv, title) -> {
                               try {
                                   var style = appeng.client.gui.style.StyleManager.loadStyleDoc("/screens/extreme_molecular_assembler.json");
                                   return new ExtremeMolecularAssemblerScreen(menu, playerInv, title, style);
                               } catch (Exception e) {
                            CCApplied.LOG.error("[ExtremeClientHelper] Failed to create ExtremeMolecularAssemblerScreen", e);
                            throw new RuntimeException("Failed to create screen", e);
                        }
                    });
                
                CCApplied.LOG.info("[ExtremeClientHelper] CCApplied screens initialized successfully");
            } catch (Exception e) {
                CCApplied.LOG.error("[ExtremeClientHelper] Failed to initialize screens", e);
            }
        });
    }
}
