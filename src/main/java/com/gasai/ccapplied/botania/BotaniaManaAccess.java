package com.gasai.ccapplied.botania;

import net.minecraft.world.level.block.entity.BlockEntity;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.common.block.mana.ManaPoolBlock;

/** Only pools expose both capacity and reversible withdrawal in Botania's API. */
public final class BotaniaManaAccess {
    private BotaniaManaAccess() {}

    public static ManaEndpoint pool(BlockEntity blockEntity) {
        if (!(blockEntity instanceof ManaPool pool)) return null;
        return new ManaEndpoint() {
            @Override public long stored() { return Math.max(0, pool.getCurrentMana()); }
            @Override public long space() { return Math.max(0L, (long) pool.getMaxMana() - stored()); }
            @Override public long extract(long offered) {
                int taken = (int) Math.min(Math.max(0, offered), Math.min(Integer.MAX_VALUE, stored()));
                // Creative pools intentionally keep their reported balance unchanged on withdrawal.
                if (blockEntity.getBlockState().getBlock() instanceof ManaPoolBlock block
                        && block.variant == ManaPoolBlock.Variant.CREATIVE) return taken;
                long before = stored();
                pool.receiveMana(-taken);
                return Math.max(0, Math.min(taken, before - stored()));
            }
            @Override public long insert(long offered) {
                int accepted = (int) Math.min(Math.max(0, offered), Math.min(Integer.MAX_VALUE, space()));
                long before = stored();
                pool.receiveMana(accepted);
                return Math.max(0, Math.min(accepted, stored() - before));
            }
        };
    }
}
