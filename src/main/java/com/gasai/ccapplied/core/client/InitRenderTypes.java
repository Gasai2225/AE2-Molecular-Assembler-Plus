package com.gasai.ccapplied.core.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gasai.ccapplied.core.registry.CCBlocks;

/**
 * Инициализирует слои рендеринга для блоков
 */
@OnlyIn(Dist.CLIENT)
public final class InitRenderTypes {

    private InitRenderTypes() {
    }

    public static void init() {
        // Extreme Molecular Assembler должен рендериться в cutout слое как оригинальный
        ItemBlockRenderTypes.setRenderLayer(CCBlocks.EXTREME_MOLECULAR_ASSEMBLER.get(), RenderType.cutout());
    }
}

