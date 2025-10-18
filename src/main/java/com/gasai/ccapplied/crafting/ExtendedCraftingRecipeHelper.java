package com.gasai.ccapplied.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import com.blakebr0.extendedcrafting.api.crafting.ITableRecipe;
import com.blakebr0.extendedcrafting.crafting.recipe.ShapedTableRecipe;
import com.blakebr0.extendedcrafting.crafting.recipe.ShapelessTableRecipe;

/**
 * Helper for working with ExtendedCrafting recipes
 */
public class ExtendedCraftingRecipeHelper {
    
    private ExtendedCraftingRecipeHelper() {}
    
    /**
     * Checks if there is a valid ExtendedCrafting recipe for the given 9x9 crafting grid
     */
    public static @Nullable ITableRecipe findRecipe(ItemStack[] craftingGrid, Level level) {
        if (level == null || craftingGrid == null || craftingGrid.length != 81) {
            return null;
        }
        
        try {
            boolean hasItems = false;
            for (ItemStack stack : craftingGrid) {
                if (!stack.isEmpty()) {
                    hasItems = true;
                    break;
                }
            }
            
            if (!hasItems) {
                return null;
            }
            
            var recipeManager = level.getRecipeManager();
            if (recipeManager == null) {
                return null;
            }
            
            try {
                var allRecipes = recipeManager.getRecipes();
                
                for (var recipe : allRecipes) {
                    if (recipe instanceof ITableRecipe tableRecipe) {
                        if (matchesExtendedCraftingRecipe(tableRecipe, craftingGrid, level)) {
                            return tableRecipe;
                        }
                    }
                }
                
                return null;
                
            } catch (Exception e) {
                return null;
            }
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Проверяет, соответствует ли ExtendedCrafting рецепт заданной сетке крафта
     */
    private static boolean matchesExtendedCraftingRecipe(ITableRecipe recipe, ItemStack[] craftingGrid, Level level) {
        try {
            if (recipe instanceof ShapedTableRecipe shaped) {
                return matchesShapedTableRecipe(shaped, craftingGrid, level);
            } else if (recipe instanceof ShapelessTableRecipe shapeless) {
                return matchesShapelessTableRecipe(shapeless, craftingGrid, level);
            }
            return false;
        } catch (Exception e) {
            com.gasai.ccapplied.CCApplied.LOG.warn("Error matching ExtendedCrafting recipe", e);
            return false;
        }
    }
    
    /**
     * Проверяет ShapedTableRecipe
     */
    private static boolean matchesShapedTableRecipe(ShapedTableRecipe recipe, ItemStack[] craftingGrid, Level level) {
        try {
            int width = recipe.getWidth();
            int height = recipe.getHeight();
            
            if (width > 9 || height > 9) {
                return false;
            }
            
            var ingredients = recipe.getIngredients();
            
            for (int startY = 0; startY <= 9 - height; startY++) {
                for (int startX = 0; startX <= 9 - width; startX++) {
                    if (matchesRecipeAtPosition(recipe, ingredients, craftingGrid, startX, startY, width, height)) {
                        return true;
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            com.gasai.ccapplied.CCApplied.LOG.warn("Error matching ShapedTableRecipe", e);
            return false;
        }
    }
    
    /**
     * Проверяет, соответствует ли рецепт сетке в конкретной позиции
     */
    private static boolean matchesRecipeAtPosition(ShapedTableRecipe recipe, 
                                                  java.util.List<net.minecraft.world.item.crafting.Ingredient> ingredients,
                                                  ItemStack[] craftingGrid, 
                                                  int startX, int startY, int width, int height) {
        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int recipeIndex = x + y * width;
                    int gridIndex = (startX + x) + (startY + y) * 9;
                    
                    if (recipeIndex < ingredients.size() && gridIndex < craftingGrid.length) {
                        var ingredient = ingredients.get(recipeIndex);
                        var gridStack = craftingGrid[gridIndex];
                        
                        if (!ingredient.isEmpty()) {
                            if (gridStack.isEmpty() || !ingredient.test(gridStack)) {
                                return false;
                            }
                        } else {
                            if (!gridStack.isEmpty()) {
                                return false;
                            }
                        }
                    }
                }
            }
            
            for (int y = 0; y < 9; y++) {
                for (int x = 0; x < 9; x++) {
                    int gridIndex = x + y * 9;
                    
                    if (x < startX || x >= startX + width || y < startY || y >= startY + height) {
                        if (!craftingGrid[gridIndex].isEmpty()) {
                            return false;
                        }
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            com.gasai.ccapplied.CCApplied.LOG.warn("Error matching recipe at position", e);
            return false;
        }
    }
    
    /**
     * Проверяет ShapelessTableRecipe
     */
    private static boolean matchesShapelessTableRecipe(ShapelessTableRecipe recipe, ItemStack[] craftingGrid, Level level) {
        try {
            var ingredients = recipe.getIngredients();
            
            var availableItems = new java.util.ArrayList<ItemStack>();
            for (ItemStack stack : craftingGrid) {
                if (!stack.isEmpty()) {
                    availableItems.add(stack.copy());
                }
            }
            
            for (var ingredient : ingredients) {
                if (ingredient.isEmpty()) {
                    continue;
                }
                
                boolean found = false;
                for (int i = 0; i < availableItems.size(); i++) {
                    var item = availableItems.get(i);
                    if (ingredient.test(item)) {
                        availableItems.remove(i);
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    return false;
                }
            }
            
            if (!availableItems.isEmpty()) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            com.gasai.ccapplied.CCApplied.LOG.warn("Error matching ShapelessTableRecipe", e);
            return false;
        }
    }
    
    
    /**
     * Проверяет, является ли рецепт ExtendedCrafting рецептом
     */
    public static boolean isExtendedCraftingRecipe(CraftingRecipe recipe) {
        return false;
    }
    
    /**
     * Получает превью результата рецепта
     */
    public static @Nullable ItemStack getRecipePreview(ItemStack[] craftingGrid, Level level) {
        ITableRecipe recipe = findRecipe(craftingGrid, level);
        if (recipe != null) {
            try {
                return recipe.getResultItem(level.registryAccess());
            } catch (Exception e) {
                com.gasai.ccapplied.CCApplied.LOG.warn("Error getting recipe preview", e);
            }
        }
        return null;
    }
    
    /**
     * Получает информацию о рецепте ExtendedCrafting
     */
    public static @Nullable String getRecipeInfo(ITableRecipe recipe) {
        if (recipe instanceof ShapedTableRecipe shaped) {
            return String.format("ShapedTable %dx%d (Tier %d)", 
                shaped.getWidth(), shaped.getHeight(), shaped.getTier());
        } else if (recipe instanceof ShapelessTableRecipe shapeless) {
            return String.format("ShapelessTable (Tier %d)", shapeless.getTier());
        }
        return null;
    }
}
