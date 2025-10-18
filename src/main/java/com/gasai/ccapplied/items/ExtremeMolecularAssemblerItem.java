package com.gasai.ccapplied.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import javax.annotation.Nonnull;

/**
 * Item for placing Extreme Molecular Assembler
 */
public class ExtremeMolecularAssemblerItem extends Item {
    
    public ExtremeMolecularAssemblerItem(Properties properties) {
        super(properties);
    }
    
    @Nonnull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        
        if (state.is(Blocks.AIR) || state.canBeReplaced()) {
            BlockState newState = com.gasai.ccapplied.core.registry.CCBlocks.EXTREME_MOLECULAR_ASSEMBLER.get().defaultBlockState();
            level.setBlock(pos, newState, 3);
            
            if (!context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.FAIL;
    }
}
