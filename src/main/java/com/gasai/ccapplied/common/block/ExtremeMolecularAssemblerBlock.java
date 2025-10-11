package com.gasai.ccapplied.common.block;

import com.gasai.ccapplied.common.block.entity.ExtremeMolecularAssemblerBlockEntity;
import com.gasai.ccapplied.core.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class ExtremeMolecularAssemblerBlock extends BaseEntityBlock {



    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public ExtremeMolecularAssemblerBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(POWERED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtremeMolecularAssemblerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == ModBlockEntities.EXTREME_ASSEMBLER_BE.get()
                ? (lvl, pos, st, be) -> ((ExtremeMolecularAssemblerBlockEntity) be).serverTick()
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            var be = level.getBlockEntity(pos);
            if (be instanceof ExtremeMolecularAssemblerBlockEntity ema) {
                net.minecraftforge.network.NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, ema,
                        buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
