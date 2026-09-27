package com.gasai.ccapplied.botania;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.gasai.ccapplied.CCApplied;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** One resource type; mana is never represented as a craftable item or fluid. */
public final class ManaKey extends AEKey {
    public static final ManaKey INSTANCE = new ManaKey();
    private ManaKey() {}

    @Override public AEKeyType getType() { return ManaKeyType.INSTANCE; }
    @Override public AEKey dropSecondary() { return this; }
    @Override public Object getPrimaryKey() { return this; }
    @Override public ResourceLocation getId() { return CCApplied.makeId("mana"); }
    @Override public CompoundTag toTag(net.minecraft.core.HolderLookup.Provider registries) { return new CompoundTag(); }
    @Override public void writeToPacket(RegistryFriendlyByteBuf data) {}
    @Override protected Component computeDisplayName() { return Component.translatable("resource.ccapplied.mana"); }
    @Override public boolean hasComponents() { return false; }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
        if (amount > 0) drops.add(ManaCapsuleItem.filled(amount));
    }
}
