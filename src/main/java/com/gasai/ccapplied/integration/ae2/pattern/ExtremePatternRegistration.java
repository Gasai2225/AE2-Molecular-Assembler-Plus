package com.gasai.ccapplied.integration.ae2.pattern;

import com.gasai.ccapplied.CCApplied;
import appeng.api.crafting.PatternDetailsHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Регистрация декодера для экстремальных паттернов
 */
@Mod.EventBusSubscriber(modid = CCApplied.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExtremePatternRegistration {

    private ExtremePatternRegistration() {}

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        CCApplied.LOG.info("[ExtremePatternRegistration] Registering extreme pattern decoder");
        
        event.enqueueWork(() -> {
            try {
                // Регистрируем наш декодер в системе AE2
                PatternDetailsHelper.registerDecoder(ExtremePatternDecoder.INSTANCE);
                CCApplied.LOG.info("[ExtremePatternRegistration] Extreme pattern decoder registered successfully");
            } catch (Exception e) {
                CCApplied.LOG.error("[ExtremePatternRegistration] Failed to register extreme pattern decoder", e);
            }
        });
    }
}

