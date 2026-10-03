package com.gasai.ccapplied.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record ExtremeRecipeMatch(ItemStack result, @Nullable ResourceLocation recipeId, String backend,
        java.util.List<ItemStack> remainingItems) {
}
