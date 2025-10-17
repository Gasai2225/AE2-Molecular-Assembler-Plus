package com.gasai.ccapplied.client.render;

import com.gasai.ccapplied.tiles.ExtremeMolecularAssemblerTileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import appeng.client.render.crafting.AssemblerAnimationStatus;
import com.gasai.ccapplied.CCApplied;

/**
 * Рендерер для Extreme Molecular Assembler
 */
@OnlyIn(Dist.CLIENT)
public class ExtremeMolecularAssemblerRenderer implements BlockEntityRenderer<ExtremeMolecularAssemblerTileEntity> {
    
    public static final ResourceLocation LIGHTS_MODEL = CCApplied.makeId("block/extreme_molecular_assembler_lights");
    
    private final RandomSource particleRandom = RandomSource.create();
    
    public ExtremeMolecularAssemblerRenderer(BlockEntityRendererProvider.Context context) {
        // Инициализация рендерера
    }
    
    @Override
    public void render(ExtremeMolecularAssemblerTileEntity blockEntity, float partialTick, 
                      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        
        AssemblerAnimationStatus status = blockEntity.getAnimationStatus();
        if (status != null) {
            if (!Minecraft.getInstance().isPaused()) {
                if (status.isExpired()) {
                    blockEntity.setAnimationStatus(null);
                }
                
                status.setAccumulatedTicks(status.getAccumulatedTicks() + partialTick);
                status.setTicksUntilParticles(status.getTicksUntilParticles() - partialTick);
            }
            
            renderStatus(blockEntity, poseStack, bufferSource, packedLight, status);
        }
        
        if (blockEntity.isPowered()) {
            renderPowerLight(poseStack, bufferSource, packedLight, packedOverlay);
        }
    }
    
    private void renderPowerLight(PoseStack ms, MultiBufferSource bufferIn, int combinedLightIn,
            int combinedOverlayIn) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel lightsModel = minecraft.getModelManager().getModel(LIGHTS_MODEL);
        // tripwire layer имеет нужные нам свойства шейдера:
        // alpha testing
        // translucency
        VertexConsumer buffer = bufferIn.getBuffer(RenderType.tripwire());
        
        // certainly doesn't use alpha testing, making it look like it will not work.
        minecraft.getBlockRenderer().getModelRenderer().renderModel(ms.last(), buffer, null,
                lightsModel, 1, 1, 1, combinedLightIn, combinedOverlayIn, ModelData.EMPTY, null);
    }
    
    private void renderStatus(ExtremeMolecularAssemblerTileEntity blockEntity, PoseStack ms,
            MultiBufferSource bufferIn, int combinedLightIn, AssemblerAnimationStatus status) {
        double centerX = blockEntity.getBlockPos().getX() + 0.5f;
        double centerY = blockEntity.getBlockPos().getY() + 0.5f;
        double centerZ = blockEntity.getBlockPos().getZ() + 0.5f;
        
        // Spawn crafting FX that fly towards the block's center
        Minecraft minecraft = Minecraft.getInstance();
        if (status.getTicksUntilParticles() <= 0) {
            status.setTicksUntilParticles(4);
            
            // Пока не добавляем частицы, так как у нас нет ParticleTypes.CRAFTING
            // Можно добавить позже если нужно
        }
        
        ItemStack is = status.getIs();
        if (!is.isEmpty()) {
            System.out.println("[CCApplied] ExtremeMolecularAssemblerRenderer: Rendering item " + is.getDisplayName().getString() + " at " + blockEntity.getBlockPos());
            ItemRenderer itemRenderer = minecraft.getItemRenderer();
            ms.pushPose();
            ms.translate(0.5, 0.5, 0.5); // Translate to center of block
            
            if (!(is.getItem() instanceof BlockItem)) {
                ms.translate(0, -0.3f, 0);
            } else {
                ms.translate(0, -0.2f, 0);
            }
            
            itemRenderer.renderStatic(is, ItemDisplayContext.GROUND, combinedLightIn,
                    OverlayTexture.NO_OVERLAY, ms, bufferIn, blockEntity.getLevel(), 0);
            ms.popPose();
        } else {
            System.out.println("[CCApplied] ExtremeMolecularAssemblerRenderer: No item to render at " + blockEntity.getBlockPos());
        }
    }
}
