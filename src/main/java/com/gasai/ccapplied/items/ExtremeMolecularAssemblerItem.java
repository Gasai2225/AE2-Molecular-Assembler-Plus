package com.gasai.ccapplied.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import com.gasai.ccapplied.blocks.ExtremeMolecularAssemblerBlock;
import javax.annotation.Nonnull;

/**
 * Предмет для размещения Extreme Molecular Assembler
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
        
        // Проверяем, что блок можно заменить
        if (state.is(Blocks.AIR) || state.canBeReplaced()) {
            // Устанавливаем блок Extreme Molecular Assembler
            BlockState newState = com.gasai.ccapplied.core.registry.CCBlocks.EXTREME_MOLECULAR_ASSEMBLER.get().defaultBlockState();
            level.setBlock(pos, newState, 3);
            
            // Уменьшаем количество предмета в руке
            if (!context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.FAIL;
    }
}
