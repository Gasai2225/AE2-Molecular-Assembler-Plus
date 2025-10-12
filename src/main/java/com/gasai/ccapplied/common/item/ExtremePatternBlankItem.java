package com.gasai.ccapplied.common.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;

public class ExtremePatternBlankItem extends Item {
    public ExtremePatternBlankItem(Properties props) { super(props); }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.ccapplied.extreme_pattern");
    }
}
