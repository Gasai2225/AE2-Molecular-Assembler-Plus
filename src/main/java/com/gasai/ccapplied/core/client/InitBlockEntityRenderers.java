package com.gasai.ccapplied.core.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.gasai.ccapplied.client.render.ExtremeMolecularAssemblerRenderer;
import com.gasai.ccapplied.core.registry.CCBlocks;
import com.gasai.ccapplied.core.registry.CCOptionalMods;

@OnlyIn(Dist.CLIENT)
public final class InitBlockEntityRenderers {

    private InitBlockEntityRenderers() {
    }

    public static void init() {
        if (CCOptionalMods.isBotaniaLoaded()) {
            for (var entry : com.gasai.ccapplied.botania.BotanicalAssemblers.ENTRIES.values())
                register(entry.type().get(), com.gasai.ccapplied.botania.client.BotanicalAssemblerRenderer::new);
        }
        register(CCBlocks.EXTREME_MOLECULAR_ASSEMBLER_TILE.get(), ExtremeMolecularAssemblerRenderer::new);
        if (CCOptionalMods.isDraconicEvolutionLoaded()) {
            register(CCBlocks.WYVERN_MOLECULAR_ASSEMBLER_TILE.get(), ExtremeMolecularAssemblerRenderer::new);
            register(CCBlocks.DRACONIC_MOLECULAR_ASSEMBLER_TILE.get(), ExtremeMolecularAssemblerRenderer::new);
            register(CCBlocks.CHAOTIC_MOLECULAR_ASSEMBLER_TILE.get(), ExtremeMolecularAssemblerRenderer::new);
        }
    }

    private static <T extends BlockEntity> void register(BlockEntityType<T> type,
            BlockEntityRendererProvider<T> factory) {
        BlockEntityRenderers.register(type, factory);
    }
}

