package com.gasai.ccapplied.botania;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Recovery container for mana left in a broken port. Not a source of mana. */
public final class ManaCapsuleItem extends Item {
    public ManaCapsuleItem() { super(new Properties().stacksTo(1)); }

    public static long amount(ItemStack stack) {
        if (!stack.is(BotaniaContent.MANA_CAPSULE.get())) return 0;
        var data = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return Math.max(0, data.getLong("mana"));
    }

    public static ItemStack filled(long amount) {
        var stack = new ItemStack(BotaniaContent.MANA_CAPSULE.get());
        setAmount(stack, amount);
        return stack;
    }

    public static void setAmount(ItemStack stack, long amount) {
        var data = new CompoundTag();
        data.putLong("mana", Math.max(0, amount));
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(data));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.ccapplied.mana_capsule", amount(stack)));
    }
}
