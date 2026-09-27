package com.gasai.ccapplied.botania.client;

import com.gasai.ccapplied.botania.BotanicalAssemblerBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Same native station and shell in inventories, hands and dropped items. */
public final class BotanicalAssemblerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public BotanicalAssemblerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof BotanicalAssemblerBlock block)) return;
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(), pose, buffers, light, overlay);
        BotanicalAssemblerRenderer.renderStation(block.station(), pose, buffers, light, overlay);
        BotanicalAssemblerRenderer.renderLights(pose, buffers, overlay);
    }
}
