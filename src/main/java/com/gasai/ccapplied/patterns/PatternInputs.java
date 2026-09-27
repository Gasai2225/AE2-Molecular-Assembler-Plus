package com.gasai.ccapplied.patterns;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import net.minecraft.world.item.ItemStack;

/** Consumes exact keys, including inputs split between several AE2 counters. */
final class PatternInputs {
    private PatternInputs() {
    }

    static void fill(ItemStack[] inputs, KeyCounter[] table,
            IMolecularAssemblerSupportedPattern.CraftingGridAccessor target) {
        for (int slot = 0; slot < inputs.length; slot++) {
            ItemStack expected = inputs[slot];
            if (expected.isEmpty()) {
                target.set(slot, ItemStack.EMPTY);
                continue;
            }
            AEItemKey key = AEItemKey.of(expected);
            int missing = expected.getCount();
            for (KeyCounter counter : table) {
                if (counter != null) {
                    long taken = Math.min(missing, Math.max(0, counter.get(key)));
                    counter.remove(key, taken);
                    missing -= (int) taken;
                }
                if (missing == 0) {
                    break;
                }
            }
            target.set(slot, missing == 0 ? expected.copy() : ItemStack.EMPTY);
        }
    }
}
