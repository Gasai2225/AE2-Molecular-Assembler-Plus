package com.gasai.ccapplied.botania;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.gasai.ccapplied.CCApplied;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;

public final class ManaKeyType extends AEKeyType {
    public static final ManaKeyType INSTANCE = new ManaKeyType();

    private ManaKeyType() {
        super(CCApplied.makeId("mana"), ManaKey.class, Component.translatable("resource.ccapplied.mana"));
    }

    @Override public int getAmountPerOperation() { return 1000; }
    @Override public int getAmountPerByte() { return 1000; }
    @Override public int getAmountPerUnit() { return 1_000_000; }
    @Override public String getUnitSymbol() { return "pool"; }
    @Override public AEKey readFromPacket(RegistryFriendlyByteBuf data) { return ManaKey.INSTANCE; }
    @Override public com.mojang.serialization.MapCodec<? extends AEKey> codec() {
        return com.mojang.serialization.MapCodec.unit(ManaKey.INSTANCE);
    }
}
