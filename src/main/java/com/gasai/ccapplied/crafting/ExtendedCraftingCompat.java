package com.gasai.ccapplied.crafting;

import com.blakebr0.extendedcrafting.api.crafting.ITableRecipe;
import com.blakebr0.extendedcrafting.api.TableCraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Uses the integration's own matching rules, including mirrors and overlapping ingredients. */
final class ExtendedCraftingCompat {
    private ExtendedCraftingCompat() {
    }

    static @Nullable ExtremeRecipeMatch findRecipe(ItemStack[] grid, Level level) {
        if (level == null || grid == null || grid.length != 81) {
            return null;
        }
        int left = 9, top = 9, right = -1, bottom = -1;
        for (int slot = 0; slot < grid.length; slot++) {
            if (grid[slot] != null && !grid[slot].isEmpty()) {
                left = Math.min(left, slot % 9);
                top = Math.min(top, slot / 9);
                right = Math.max(right, slot % 9);
                bottom = Math.max(bottom, slot / 9);
            }
        }
        if (right < 0) {
            return null;
        }
        for (var entry : level.getRecipeManager().getRecipes()) {
            if (!(entry.value() instanceof ITableRecipe recipe)) {
                continue;
            }
            int tier = recipe.hasRequiredTier() ? recipe.getTier() : 4;
            int size = tier * 2 + 1;
            if (tier < 1 || tier > 4 || right - left >= size || bottom - top >= size) {
                continue;
            }
            var items = new java.util.ArrayList<ItemStack>(size * size);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    ItemStack stack = x + left < 9 && y + top < 9 ? grid[x + left + (y + top) * 9] : ItemStack.EMPTY;
                    items.add(stack == null ? ItemStack.EMPTY : stack.copy());
                }
            }
            var input = TableCraftingInput.of(size, size, items, tier);
            if (recipe.matches(input, level)) {
                ItemStack result = recipe.assemble(input, level.registryAccess());
                if (!result.isEmpty()) {
                    return new ExtremeRecipeMatch(result, entry.id(), "extendedcrafting");
                }
            }
        }
        return null;
    }
}
