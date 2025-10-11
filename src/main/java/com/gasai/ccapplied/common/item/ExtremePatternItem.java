package com.gasai.ccapplied.common.item;

import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.crafting.PatternDetailsHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ExtremePatternItem extends Item {
    public static final String KEY = "ccapplied_extreme_pattern";

    public ExtremePatternItem(Properties props) { super(props); }

    public static boolean isEncoded(ItemStack stack) {
        return stack.hasTag() && stack.getOrCreateTag().contains(KEY);
    }

    public static void writePatternData(ItemStack stack, CompoundTag data) {
        stack.getOrCreateTag().put(KEY, data);
    }

    public static CompoundTag readPatternData(ItemStack stack) {
        return stack.getOrCreateTagElement(KEY);
    }

    public static void registerDecoder(IPatternDetailsDecoder decoder) {
        PatternDetailsHelper.registerDecoder(decoder);
    }
}
