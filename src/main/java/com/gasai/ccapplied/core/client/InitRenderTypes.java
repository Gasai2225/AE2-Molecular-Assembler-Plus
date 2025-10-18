package com.gasai.ccapplied.core.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gasai.ccapplied.core.registry.CCBlocks;

/**
 * Initializes render layers for blocks
 */
@OnlyIn(Dist.CLIENT)
public final class InitRenderTypes {

    private InitRenderTypes() {
    }

    public static void init() {
        ItemBlockRenderTypes.setRenderLayer(CCBlocks.EXTREME_MOLECULAR_ASSEMBLER.get(), RenderType.cutout());
    }
}

