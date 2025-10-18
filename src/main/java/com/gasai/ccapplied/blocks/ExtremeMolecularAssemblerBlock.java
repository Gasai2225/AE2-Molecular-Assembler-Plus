package com.gasai.ccapplied.blocks;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import appeng.block.AEBaseEntityBlock;
import com.gasai.ccapplied.tiles.ExtremeMolecularAssemblerTileEntity;
import com.gasai.ccapplied.core.registry.CCMenuTypes;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;

/**
 * Блок Extreme Molecular Assembler для крафта 9x9 рецептов
 */
public class ExtremeMolecularAssemblerBlock extends AEBaseEntityBlock<ExtremeMolecularAssemblerTileEntity> {

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public ExtremeMolecularAssemblerBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    protected BlockState updateBlockStateFromBlockEntity(BlockState currentState, ExtremeMolecularAssemblerTileEntity be) {
        return currentState.setValue(POWERED, be.isPowered());
    }

    @Override
    public InteractionResult onActivated(Level level, BlockPos pos, Player player, InteractionHand hand,
            @Nullable ItemStack heldItem, BlockHitResult hit) {
        var be = this.getBlockEntity(level, pos);
        if (be != null) {

            if (!InteractionUtil.isInAlternateUseMode(player)) {
                if (!level.isClientSide()) {
                    MenuOpener.open(CCMenuTypes.EXTREME_MOLECULAR_ASSEMBLER.get(), player,
                            MenuLocators.forBlockEntity(be));
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return InteractionResult.PASS;
    }
    
}
