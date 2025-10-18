package com.gasai.ccapplied.client;

import com.gasai.ccapplied.CCApplied;
import net.minecraft.resources.ResourceLocation;
import appeng.api.parts.PartModels;

public class CCPartModelRegistry {
    
    public static void registerPartModels() {
        PartModels.registerModels(
            ResourceLocation.fromNamespaceAndPath(CCApplied.MODID, "part/extreme_pattern_encoding_terminal_off"),
            ResourceLocation.fromNamespaceAndPath(CCApplied.MODID, "part/extreme_pattern_encoding_terminal_on")
        );
        
    }
}
