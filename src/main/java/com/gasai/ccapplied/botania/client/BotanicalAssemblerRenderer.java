package com.gasai.ccapplied.botania.client;

import com.gasai.ccapplied.botania.BotanicalAssemblerBlockEntity;
import com.gasai.ccapplied.botania.MagicalStation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;

/** Render native Botania models at uniform scale; never remodel their textured parts. */
public final class BotanicalAssemblerRenderer implements BlockEntityRenderer<BotanicalAssemblerBlockEntity> {
    public static final ResourceLocation LIGHTS = new ResourceLocation("ae2", "block/molecular_assembler_lights");
    public static final ResourceLocation PORTAL = com.gasai.ccapplied.CCApplied.makeId("block/botanical_portal");
    public static final int FRAME_COLOR = 0xFF70BA83;

    public BotanicalAssemblerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BotanicalAssemblerBlockEntity machine, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        renderStation(machine.station(), pose, buffers, light, overlay);
        if (machine.isVisualPowered()) renderLights(pose, buffers, overlay);
    }

    public static void renderLights(PoseStack pose, MultiBufferSource buffers, int overlay) {
        var minecraft = Minecraft.getInstance();
        var model = minecraft.getModelManager().getModel(LIGHTS);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.tripwire()), null, model, 1, 1, 1,
                LightTexture.FULL_BRIGHT, overlay, net.minecraftforge.client.model.data.ModelData.EMPTY, null);
    }

    public static void renderStation(MagicalStation station, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        switch (station) {
            case POOL -> block("mana_pool", .19, .22, .19, .62f, pose, buffers, light, overlay);
            case RUNIC_ALTAR -> block("runic_altar", .19, .16, .19, .62f, pose, buffers, light, overlay);
            case APOTHECARY -> block("apothecary_default", .19, .16, .19, .62f, pose, buffers, light, overlay);
            case BREWERY -> block("brewery", .19, .16, .19, .62f, pose, buffers, light, overlay);
            case PURE_DAISY -> block("pure_daisy", .19, .16, .19, .62f, pose, buffers, light, overlay);
            case TERRA_PLATE -> {
                // Botania's 3x3 livingrock/lapis foundation, with its native plate on top.
                for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
                    var state = (x + z) % 2 == 0
                            ? state("livingrock") : net.minecraft.world.level.block.Blocks.LAPIS_BLOCK.defaultBlockState();
                    block(state, .2 + x * .2, .16, .2 + z * .2, .2f, pose, buffers, light, overlay);
                }
                block("terra_plate", .4, .36, .4, .2f, pose, buffers, light, overlay);
            }
            case ELVEN_TRADE -> {
                // The original five-block-high gateway outline, plus actual Natura pylons.
                for (int x = 0; x < 5; x++) for (int y = 0; y < 5; y++) {
                    boolean horizontal = y == 0 || y == 4;
                    if (horizontal ? x == 0 || x == 4 : x != 0 && x != 4) continue;
                    String id = x == 2 && y == 0 ? "alfheim_portal"
                            : x == 2 || y == 2 ? "glimmering_livingwood_log" : "livingwood_log";
                    var state = state(id);
                    if (state.hasProperty(BlockStateProperties.AXIS))
                        state = state.setValue(BlockStateProperties.AXIS, horizontal ? Direction.Axis.X : Direction.Axis.Y);
                    block(state, .2 + x * .12, .16 + y * .12, .52, .12f, pose, buffers, light, overlay);
                }
                block("natura_pylon", .23, .17, .23, .18f, pose, buffers, light, overlay);
                block("natura_pylon", .59, .17, .23, .18f, pose, buffers, light, overlay);
                var minecraft = Minecraft.getInstance();
                var portal = minecraft.getModelManager().getModel(PORTAL);
                minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                        buffers.getBuffer(net.minecraft.client.renderer.Sheets.translucentCullBlockSheet()),
                        null, portal, 1, 1, 1, LightTexture.FULL_BRIGHT, overlay,
                        net.minecraftforge.client.model.data.ModelData.EMPTY, null);
            }
        }
    }

    private static BlockState state(String id) {
        return BuiltInRegistries.BLOCK.get(new ResourceLocation("botania", id)).defaultBlockState();
    }

    private static void block(String id, double x, double y, double z, float scale,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        block(state(id), x, y, z, scale, pose, buffers, light, overlay);
    }

    private static void block(BlockState state, double x, double y, double z, float scale,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(scale, scale, scale);
        // renderSingleBlock also dispatches ENTITYBLOCK_ANIMATED (brewery and pylon) item renderers.
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, light, overlay);
        pose.popPose();
    }
}
