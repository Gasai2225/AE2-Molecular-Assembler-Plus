package com.gasai.ccapplied.screens;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.core.client.CCAppliedInitScreens;
import com.gasai.ccapplied.core.registry.CCMenuTypes;
import com.gasai.ccapplied.screens.ExtremePatternEncodingTermScreen;
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
                
                CCApplied.LOG.info("[ExtremeClientHelper] CCApplied screens initialized successfully");
            } catch (Exception e) {
                CCApplied.LOG.error("[ExtremeClientHelper] Failed to initialize screens", e);
            }
        });
    }
}
