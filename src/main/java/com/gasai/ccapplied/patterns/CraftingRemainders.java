package com.gasai.ccapplied.patterns;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Retained input and a crafting return may need two different inventory slots. */
public record CraftingRemainders(NonNullList<ItemStack> slots, List<ItemStack> overflow) {
    public static CraftingRemainders collect(Container container, int[] consumed) {
        var returned = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < consumed.length; slot++) {
            if (consumed[slot] > 0) returned.set(slot,
                    container.getItem(slot).copyWithCount(consumed[slot]).getCraftingRemainingItem());
        }
        return collect(container, consumed, returned);
    }

    public static CraftingRemainders collect(Container container, int[] consumed, List<ItemStack> recipeReturns) {
        var slots = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        var overflow = new ArrayList<ItemStack>();
        for (int slot = 0; slot < slots.size(); slot++) {
            var actual = container.getItem(slot);
            int count = slot < consumed.length ? consumed[slot] : 0;
            if (count < 0 || count > actual.getCount()) throw new IllegalArgumentException("Invalid consumed count");
            var retained = actual.copyWithCount(actual.getCount() - count);
            var returned = slot < recipeReturns.size() ? recipeReturns.get(slot).copy() : ItemStack.EMPTY;
            slots.set(slot, retained.isEmpty() ? returned : retained);
            if (!retained.isEmpty() && !returned.isEmpty()) overflow.add(returned);
        }
        return new CraftingRemainders(slots, List.copyOf(overflow));
    }
}
