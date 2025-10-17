package com.gasai.ccapplied.core.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gasai.ccapplied.client.render.ExtremeMolecularAssemblerRenderer;
import com.gasai.ccapplied.core.registry.CCBlocks;

@OnlyIn(Dist.CLIENT)
public final class InitBlockEntityRenderers {

    private InitBlockEntityRenderers() {
    }

    public static void init() {
        register(CCBlocks.EXTREME_MOLECULAR_ASSEMBLER_TILE.get(), ExtremeMolecularAssemblerRenderer::new);
    }

    private static <T extends BlockEntity> void register(BlockEntityType<T> type,
            BlockEntityRendererProvider<T> factory) {
        BlockEntityRenderers.register(type, factory);
    }
}

