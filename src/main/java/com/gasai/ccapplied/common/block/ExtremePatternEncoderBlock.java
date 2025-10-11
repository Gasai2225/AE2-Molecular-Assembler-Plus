package com.gasai.ccapplied.common.block;

import com.gasai.ccapplied.common.block.entity.ExtremePatternEncoderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ExtremePatternEncoderBlock extends BaseEntityBlock {
    public ExtremePatternEncoderBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtremePatternEncoderBlockEntity(pos, state);
    }
}
