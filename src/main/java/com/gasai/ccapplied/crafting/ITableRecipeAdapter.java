package com.gasai.ccapplied.crafting;

import com.blakebr0.extendedcrafting.api.crafting.ITableRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Optional;

public interface ITableRecipeAdapter {
    <R extends Recipe<?>> R recipe();
    <R extends Recipe<?>> Optional<R> unwrap(Class<R> recipeClass);
    ResourceLocation recipeId();
    int tier();
    static ITableRecipeAdapter of(ITableRecipe recipe) {
        if (recipe instanceof com.blakebr0.extendedcrafting.crafting.recipe.ShapedTableRecipe shaped) {
            return new ShapedTableRecipeAdapter(shaped);
        } else if (recipe instanceof com.blakebr0.extendedcrafting.crafting.recipe.ShapelessTableRecipe shapeless) {
            return new ShapelessTableRecipeAdapter(shapeless);
        }
        throw new IllegalArgumentException("Unknown ITableRecipe implementation: " + recipe.getClass().getName());
    }
}
