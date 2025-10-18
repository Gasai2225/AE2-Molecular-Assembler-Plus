package com.gasai.ccapplied.patterns;

import com.gasai.ccapplied.CCApplied;
import appeng.api.crafting.PatternDetailsHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Registration of decoder for extreme patterns
 */
@Mod.EventBusSubscriber(modid = CCApplied.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExtremePatternRegistration {

    private ExtremePatternRegistration() {}

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        
        event.enqueueWork(() -> {
            try {
                PatternDetailsHelper.registerDecoder(ExtremePatternDecoder.INSTANCE);
            } catch (Exception e) {
            }
        });
    }
}

