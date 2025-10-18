package com.gasai.ccapplied.client;

import com.gasai.ccapplied.CCApplied;
import net.minecraft.resources.ResourceLocation;
import appeng.api.parts.PartModels;

public class CCPartModelRegistry {
    
    public static void registerPartModels() {
        // Регистрируем модели частей для AE2 используя правильный API
        PartModels.registerModels(
            ResourceLocation.fromNamespaceAndPath(CCApplied.MODID, "part/extreme_pattern_encoding_terminal_off"),
            ResourceLocation.fromNamespaceAndPath(CCApplied.MODID, "part/extreme_pattern_encoding_terminal_on")
        );
        
        CCApplied.LOG.info("Registered part models: extreme_pattern_encoding_terminal_off, extreme_pattern_encoding_terminal_on");
    }
}
