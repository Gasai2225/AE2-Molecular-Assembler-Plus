package com.gasai.ccapplied.slots;

import net.minecraft.world.item.ItemStack;
import appeng.menu.slot.FakeSlot;

/**
 * Special slot for displaying extreme crafting result (9x9).
 * Similar to PatternTermSlot from AE2, but adapted for ExtendedCrafting.
 */
public class ExtremePatternTermSlot extends FakeSlot {
    
    private ItemStack displayedOutput = ItemStack.EMPTY;
    
    public ExtremePatternTermSlot(appeng.api.inventories.InternalInventory inventory, int slot) {
        super(inventory, slot);
        this.setHideAmount(false);
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
    
    @Override
    public ItemStack getItem() {
        return displayedOutput;
    }
    
    @Override
    public void set(ItemStack stack) {
    }
    
    @Override
    public boolean hasItem() {
        return !displayedOutput.isEmpty();
    }
    
    @Override
    public int getMaxStackSize() {
        return 64;
    }
    
    /**
     * Sets the displayed crafting result.
     * Similar to setDisplayedCraftingOutput() from PatternTermSlot.
     */
    public void setDisplayedCraftingOutput(ItemStack output) {
        if (output == null) {
            this.displayedOutput = ItemStack.EMPTY;
            return;
        }
        
        this.displayedOutput = output.copy();
        
    }
    
    /**
     * Gets the current displayed result.
     */
    public ItemStack getDisplayedCraftingOutput() {
        return displayedOutput.copy();
    }
    
    /**
     * Clears the displayed result.
     */
    public void clearDisplayedOutput() {
        this.displayedOutput = ItemStack.EMPTY;
    }
}
