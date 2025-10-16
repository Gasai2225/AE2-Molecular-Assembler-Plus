package com.gasai.ccapplied.integration.extendedcrafting;

import com.blakebr0.extendedcrafting.api.crafting.ITableRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Optional;

/**
 * Адаптер для ExtendedCrafting рецептов
 * Аналогичен ITableRecipeAdapter из ExtendedTerminal
 */
public interface ITableRecipeAdapter {
    
    /**
     * Получает базовый рецепт
     */
    <R extends Recipe<?>> R recipe();
    
    /**
     * Получает рецепт определенного типа
     */
    <R extends Recipe<?>> Optional<R> unwrap(Class<R> recipeClass);
    
    /**
     * Получает ID рецепта
     */
    ResourceLocation recipeId();
    
    /**
     * Получает тир рецепта (размер стола)
     */
    int tier();
    
    /**
     * Создает адаптер из ITableRecipe
     */
    static ITableRecipeAdapter of(ITableRecipe recipe) {
        if (recipe instanceof com.blakebr0.extendedcrafting.crafting.recipe.ShapedTableRecipe shaped) {
            return new ShapedTableRecipeAdapter(shaped);
        } else if (recipe instanceof com.blakebr0.extendedcrafting.crafting.recipe.ShapelessTableRecipe shapeless) {
            return new ShapelessTableRecipeAdapter(shapeless);
        }
        throw new IllegalArgumentException("Unknown ITableRecipe implementation: " + recipe.getClass().getName());
    }
}
