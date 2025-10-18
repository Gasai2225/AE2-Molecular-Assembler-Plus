package com.gasai.ccapplied.slots;

import net.minecraft.world.item.ItemStack;
import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.RestrictedInputSlot;
import com.gasai.ccapplied.core.registry.CCItems;

/**
 * Slot for encoded extreme patterns
 */
public class ExtremeEncodedPatternSlot extends RestrictedInputSlot {
    
    public ExtremeEncodedPatternSlot(InternalInventory inv, int slotIndex) {
        super(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, inv, slotIndex);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == CCItems.EXTREME_CRAFTING_PATTERN.get();
    }
}