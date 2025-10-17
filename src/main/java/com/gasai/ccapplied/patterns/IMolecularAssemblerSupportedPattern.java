package com.gasai.ccapplied.patterns;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;

/**
 * Интерфейс для паттернов, поддерживающих сборку в Extreme Molecular Assembler
 */
public interface IMolecularAssemblerSupportedPattern extends IPatternDetails {
    ItemStack assemble(Container container, Level level);

    boolean isItemValid(int slot, AEItemKey key, Level level);

    boolean isSlotEnabled(int slot);

    void fillCraftingGrid(KeyCounter[] table, CraftingGridAccessor gridAccessor);

    @Override
    default boolean supportsPushInputsToExternalInventory() {
        // Patterns crafted in a molecular assembler are usually pointless to craft in anything else
        return false;
    }

    @FunctionalInterface
    interface CraftingGridAccessor {
        void set(int slot, ItemStack stack);
    }

    NonNullList<ItemStack> getRemainingItems(CraftingContainer container);
}
