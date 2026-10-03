package com.gasai.ccapplied.crafting;

import com.gasai.ccapplied.patterns.DraconicFusionPattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record DraconicFusionRecipeMatch(
        ItemStack result,
        ItemStack catalyst,
        List<ItemStack> ingredients,
        DraconicFusionPattern.FusionTier tier,
        long totalEnergy,
        @Nullable ResourceLocation recipeId,
        List<Boolean> consumedIngredients) {
}
