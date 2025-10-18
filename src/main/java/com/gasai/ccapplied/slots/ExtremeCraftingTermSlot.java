package com.gasai.ccapplied.slots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import appeng.helpers.IMenuCraftingPacket;
import appeng.menu.slot.FakeSlot;

/**
 * Slot for displaying extreme crafting result (9x9).
 * Similar to CraftingTermSlot from AE2, but adapted for ExtendedCrafting.
 */
public class ExtremeCraftingTermSlot extends FakeSlot {
    
    private final Player player;
    private final InternalInventory craftingMatrix;
    
    
    public ExtremeCraftingTermSlot(Player player, IActionSource actionSource, IEnergySource energySource,
                                  MEStorage storage, InternalInventory craftingMatrix, 
                                  IMenuCraftingPacket menu, InternalInventory inventory, int slot) {
        super(inventory, slot);
        this.player = player;
        this.craftingMatrix = craftingMatrix;
        this.setHideAmount(false);
        
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
    
    @Override
    public ItemStack getItem() {
        ItemStack result = getCraftingResult();
        
        ItemStack finalResult = result != null ? result : ItemStack.EMPTY;
        
        super.set(finalResult);
        
        return finalResult;
    }
    
    @Override
    public void set(ItemStack stack) {
    }
    
    @Override
    public boolean hasItem() {
        return !getCraftingResult().isEmpty();
    }
    
    @Override
    public int getMaxStackSize() {
        return 64;
    }
    
    /**
     * Computes crafting result based on current matrix.
     */
    private ItemStack getCraftingResult() {
        try {
            if (player == null || player.level() == null || craftingMatrix == null) {
                return ItemStack.EMPTY;
            }
            
            ItemStack[] craftingGrid = new ItemStack[81];
            
            int nonEmptySlots = 0;
            for (int i = 0; i < Math.min(craftingGrid.length, craftingMatrix.size()); i++) {
                ItemStack stack = craftingMatrix.getStackInSlot(i);
                craftingGrid[i] = stack != null ? stack : ItemStack.EMPTY;
                if (!craftingGrid[i].isEmpty()) {
                    nonEmptySlots++;
                }
            }
            
            ItemStack result = com.gasai.ccapplied.crafting.ExtendedCraftingRecipeHelper.getRecipePreview(
                craftingGrid, player.level());
            
            ItemStack finalResult = result != null ? result : ItemStack.EMPTY;
            return finalResult;
                
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * Sets displayed result (for compatibility).
     */
    public void setDisplayedCraftingOutput(ItemStack output) {
    }
    
    /**
     * Gets displayed result.
     */
    public ItemStack getDisplayedCraftingOutput() {
        return getCraftingResult();
    }
    
    /**
     * Clears displayed result.
     */
    public void clearDisplayedOutput() {
    }
    
    /**
     * Slot initialization (for compatibility with PatternTermSlot).
     */
    public void initialize(ItemStack stack) {
    }
    
    
}
