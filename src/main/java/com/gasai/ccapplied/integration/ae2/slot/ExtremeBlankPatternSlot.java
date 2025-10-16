package com.gasai.ccapplied.integration.ae2.slot;

import net.minecraft.world.item.ItemStack;
import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.RestrictedInputSlot;
import com.gasai.ccapplied.core.registry.CCItems;

/**
 * Слот для бланков экстремальных паттернов
 */
public class ExtremeBlankPatternSlot extends RestrictedInputSlot {
    
    public ExtremeBlankPatternSlot(InternalInventory inv, int slotIndex) {
        super(RestrictedInputSlot.PlacableItemType.BLANK_PATTERN, inv, slotIndex);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // Принимаем только наши экстремальные бланки
        return !stack.isEmpty() && stack.getItem() == CCItems.EXTREME_BLANK_PATTERN.get();
    }
}