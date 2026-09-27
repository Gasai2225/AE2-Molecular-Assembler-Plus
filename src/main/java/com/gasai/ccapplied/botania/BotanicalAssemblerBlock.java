package com.gasai.ccapplied.botania;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class BotanicalAssemblerBlock extends AEBaseEntityBlock<BotanicalAssemblerBlockEntity> {
    private final MagicalStation station;
    public BotanicalAssemblerBlock(MagicalStation station) {
        super(BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> 7));
        this.station = station;
    }
    public MagicalStation station() { return station; }
    @Override public InteractionResult onActivated(Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.item.ItemStack heldItem, BlockHitResult hit) {
        if (appeng.util.InteractionUtil.isInAlternateUseMode(player)) return InteractionResult.PASS;
        if (getBlockEntity(level, pos) == null) return InteractionResult.PASS;
        if (!level.isClientSide()) MenuOpener.open(BotanicalAssemblerMenu.TYPE, player, MenuLocators.forBlockEntity(getBlockEntity(level, pos)));
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
