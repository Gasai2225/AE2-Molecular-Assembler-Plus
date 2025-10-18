package com.gasai.ccapplied.crafting;

import com.blakebr0.extendedcrafting.crafting.recipe.ShapelessTableRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Optional;


public class ShapelessTableRecipeAdapter implements ITableRecipeAdapter {
    
    private final ShapelessTableRecipe recipe;
    private final int tier;
    
    public ShapelessTableRecipeAdapter(ShapelessTableRecipe recipe) {
        this.recipe = recipe;
        this.tier = recipe.getTier();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <R extends Recipe<?>> R recipe() {
        return (R) this.recipe;
    }
    
    @Override
    public <R extends Recipe<?>> Optional<R> unwrap(Class<R> recipeClass) {
        if (recipeClass.isInstance(this.recipe)) {
            return Optional.of(recipeClass.cast(this.recipe));
        }
        return Optional.empty();
    }
    
    @Override
    public ResourceLocation recipeId() {
        return recipe.getId();
    }
    
    @Override
    public int tier() {
        return tier;
    }
}
