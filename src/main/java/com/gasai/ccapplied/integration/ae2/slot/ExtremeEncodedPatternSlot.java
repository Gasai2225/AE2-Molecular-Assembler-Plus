package com.gasai.ccapplied.integration.ae2.slot;

import net.minecraft.world.item.ItemStack;
import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.RestrictedInputSlot;
import com.gasai.ccapplied.core.registry.CCItems;

/**
 * Слот для закодированных экстремальных паттернов
 */
public class ExtremeEncodedPatternSlot extends RestrictedInputSlot {
    
    public ExtremeEncodedPatternSlot(InternalInventory inv, int slotIndex) {
        super(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, inv, slotIndex);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // Принимаем только наши экстремальные закодированные паттерны
        return !stack.isEmpty() && stack.getItem() == CCItems.EXTREME_CRAFTING_PATTERN.get();
    }
}