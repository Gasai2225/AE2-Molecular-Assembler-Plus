package com.gasai.ccapplied.client;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
import net.minecraft.resources.ResourceLocation;
import appeng.api.parts.PartModels;

public class CCPartModelRegistry {
    
    public static void registerPartModels() {
        if (CCOptionalMods.isBotaniaLoaded()) {
            PartModels.registerModels(
                    CCApplied.makeId("part/mana_import_bus"),
                    CCApplied.makeId("part/mana_export_bus"),
                    CCApplied.makeId("part/magical_terminal_off"),
                    CCApplied.makeId("part/magical_terminal_on"));
        }
        PartModels.registerModels(
            ResourceLocation.fromNamespaceAndPath(CCApplied.MODID, "part/extreme_pattern_encoding_terminal_off"),
            ResourceLocation.fromNamespaceAndPath(CCApplied.MODID, "part/extreme_pattern_encoding_terminal_on")
        );
        if (CCOptionalMods.isDraconicEvolutionLoaded()) {
            PartModels.registerModels(
                ResourceLocation.fromNamespaceAndPath(CCApplied.MODID, "part/draconic_pattern_encoding_terminal_off"),
                ResourceLocation.fromNamespaceAndPath(CCApplied.MODID, "part/draconic_pattern_encoding_terminal_on")
            );
        }
    }
}
