package com.gasai.ccapplied.crafting;

import com.blakebr0.extendedcrafting.crafting.recipe.ShapedTableRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Optional;

/**
 * Адаптер для ShapedTableRecipe из ExtendedCrafting
 */
public class ShapedTableRecipeAdapter implements ITableRecipeAdapter {
    
    private final ShapedTableRecipe recipe;
    private final int tier;
    private final int width;
    private final int height;
    
    public ShapedTableRecipeAdapter(ShapedTableRecipe recipe) {
        this.recipe = recipe;
        this.tier = recipe.getTier();
        this.width = recipe.getWidth();
        this.height = recipe.getHeight();
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
    
    /**
     * Получает ширину рецепта
     */
    public int width() {
        return width;
    }
    
    /**
     * Получает высоту рецепта
     */
    public int height() {
        return height;
    }
}
