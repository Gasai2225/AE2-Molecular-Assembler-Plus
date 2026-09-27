package com.gasai.ccapplied.core.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gasai.ccapplied.core.registry.CCBlocks;
import com.gasai.ccapplied.core.registry.CCOptionalMods;

/**
 * Initializes render layers for blocks
 */
@OnlyIn(Dist.CLIENT)
public final class InitRenderTypes {

    private InitRenderTypes() {
    }

    public static void init() {
        if (CCOptionalMods.isBotaniaLoaded()) {
            for (var entry : com.gasai.ccapplied.botania.BotanicalAssemblers.ENTRIES.values())
                ItemBlockRenderTypes.setRenderLayer(entry.block().get(), RenderType.cutout());
        }
        ItemBlockRenderTypes.setRenderLayer(CCBlocks.EXTREME_MOLECULAR_ASSEMBLER.get(), RenderType.cutout());
        if (CCOptionalMods.isDraconicEvolutionLoaded()) {
            ItemBlockRenderTypes.setRenderLayer(CCBlocks.WYVERN_MOLECULAR_ASSEMBLER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(CCBlocks.DRACONIC_MOLECULAR_ASSEMBLER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(CCBlocks.CHAOTIC_MOLECULAR_ASSEMBLER.get(), RenderType.cutout());
        }
    }
}
