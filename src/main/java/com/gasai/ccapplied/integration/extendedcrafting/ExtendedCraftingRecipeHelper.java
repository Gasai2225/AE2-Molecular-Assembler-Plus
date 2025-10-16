package com.gasai.ccapplied.integration.extendedcrafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

// ExtendedCrafting imports
import com.blakebr0.extendedcrafting.api.crafting.ITableRecipe;
import com.blakebr0.extendedcrafting.crafting.recipe.ShapedTableRecipe;
import com.blakebr0.extendedcrafting.crafting.recipe.ShapelessTableRecipe;

import java.util.List;

/**
 * Хелпер для работы с рецептами ExtendedCrafting
 */
public class ExtendedCraftingRecipeHelper {
    
    private ExtendedCraftingRecipeHelper() {}
    
    /**
     * Проверяет, есть ли валидный рецепт ExtendedCrafting для заданной сетки крафта 9x9
     * Пока упрощенная версия - просто проверяем наличие предметов в сетке
     */
    public static @Nullable ITableRecipe findRecipe(ItemStack[] craftingGrid, Level level) {
        if (level == null || craftingGrid == null || craftingGrid.length != 81) {
            return null;
        }
        
        try {
            // Проверяем, есть ли предметы в сетке
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
            
            // TODO: В будущем добавить реальную проверку ExtendedCrafting рецептов
            // Пока возвращаем null, чтобы система работала без крашей
            return null;
            
        } catch (Exception e) {
            com.gasai.ccapplied.CCApplied.LOG.warn("Error checking ExtendedCrafting recipes", e);
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
            // Проверяем размеры рецепта
            int width = recipe.getWidth();
            int height = recipe.getHeight();
            
            if (width > 9 || height > 9) {
                return false; // Слишком большой рецепт
            }
            
            // TODO: Добавить более точную проверку соответствия ингредиентов
            // Пока просто проверяем, что в сетке есть предметы
            boolean hasItems = false;
            for (ItemStack stack : craftingGrid) {
                if (!stack.isEmpty()) {
                    hasItems = true;
                    break;
                }
            }
            
            return hasItems;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Проверяет ShapelessTableRecipe
     */
    private static boolean matchesShapelessTableRecipe(ShapelessTableRecipe recipe, ItemStack[] craftingGrid, Level level) {
        try {
            // Для shapeless рецептов просто проверяем, что есть предметы
            boolean hasItems = false;
            for (ItemStack stack : craftingGrid) {
                if (!stack.isEmpty()) {
                    hasItems = true;
                    break;
                }
            }
            
            return hasItems;
        } catch (Exception e) {
            return false;
        }
    }
    
    
    /**
     * Проверяет, является ли рецепт ExtendedCrafting рецептом
     */
    public static boolean isExtendedCraftingRecipe(CraftingRecipe recipe) {
        // Пока возвращаем false, так как ITableRecipe не наследуется от CraftingRecipe
        // TODO: В будущем добавить правильную проверку
        return false;
    }
    
    /**
     * Получает превью результата рецепта
     */
    public static @Nullable ItemStack getRecipePreview(ItemStack[] craftingGrid, Level level) {
        ITableRecipe recipe = findRecipe(craftingGrid, level);
        if (recipe != null) {
            try {
                // Для ExtendedCrafting рецептов получаем результат через getResultItem
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
