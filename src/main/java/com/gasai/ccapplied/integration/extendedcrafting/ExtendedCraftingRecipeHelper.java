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
            
            // Получаем менеджер рецептов
            var recipeManager = level.getRecipeManager();
            if (recipeManager == null) {
                return null;
            }
            
            // Ищем ExtendedCrafting рецепты через специальные категории
            try {
                // Получаем все рецепты из менеджера
                var allRecipes = recipeManager.getRecipes();
                
                // Ищем рецепты
                for (var recipe : allRecipes) {
                    // Проверяем, является ли рецепт ITableRecipe
                    if (recipe instanceof ITableRecipe tableRecipe) {
                        // Проверяем, подходит ли этот рецепт для нашей сетки
                        if (matchesExtendedCraftingRecipe(tableRecipe, craftingGrid, level)) {
                            return tableRecipe;
                        }
                    }
                }
                
                return null;
                
            } catch (Exception e) {
                com.gasai.ccapplied.CCApplied.LOG.warn("Error searching ExtendedCrafting recipes", e);
                return null;
            }
            
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
            
            // Получаем ингредиенты рецепта
            var ingredients = recipe.getIngredients();
            
            // Ищем рецепт во всех возможных позициях в сетке 9x9
            for (int startY = 0; startY <= 9 - height; startY++) {
                for (int startX = 0; startX <= 9 - width; startX++) {
                    if (matchesRecipeAtPosition(recipe, ingredients, craftingGrid, startX, startY, width, height)) {
                        return true; // Найден рецепт в этой позиции
                    }
                }
            }
            
            return false; // Рецепт не найден ни в одной позиции
            
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
            // Проверяем соответствие ингредиентов в области рецепта
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int recipeIndex = x + y * width;
                    int gridIndex = (startX + x) + (startY + y) * 9; // 9x9 сетка
                    
                    if (recipeIndex < ingredients.size() && gridIndex < craftingGrid.length) {
                        var ingredient = ingredients.get(recipeIndex);
                        var gridStack = craftingGrid[gridIndex];
                        
                        // Если ингредиент не пустой, проверяем соответствие
                        if (!ingredient.isEmpty()) {
                            if (gridStack.isEmpty() || !ingredient.test(gridStack)) {
                                return false; // Не соответствует
                            }
                        } else {
                            // Если ингредиент пустой, в сетке тоже должно быть пусто
                            if (!gridStack.isEmpty()) {
                                return false;
                            }
                        }
                    }
                }
            }
            
            // Проверяем, что ВНЕ области рецепта нет лишних предметов
            // Но только для позиций, которые не заняты рецептом
            for (int y = 0; y < 9; y++) {
                for (int x = 0; x < 9; x++) {
                    int gridIndex = x + y * 9;
                    
                    // Если позиция ВНЕ области рецепта (учитываем startX и startY)
                    if (x < startX || x >= startX + width || y < startY || y >= startY + height) {
                        if (!craftingGrid[gridIndex].isEmpty()) {
                            return false; // Есть лишний предмет
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
            // Получаем ингредиенты рецепта
            var ingredients = recipe.getIngredients();
            
            // Создаем копию сетки для проверки (чтобы не изменять оригинал)
            var availableItems = new java.util.ArrayList<ItemStack>();
            for (ItemStack stack : craftingGrid) {
                if (!stack.isEmpty()) {
                    availableItems.add(stack.copy());
                }
            }
            
            // Проверяем каждый ингредиент рецепта
            for (var ingredient : ingredients) {
                if (ingredient.isEmpty()) {
                    continue; // Пропускаем пустые ингредиенты
                }
                
                boolean found = false;
                // Ищем подходящий предмет в доступных
                for (int i = 0; i < availableItems.size(); i++) {
                    var item = availableItems.get(i);
                    if (ingredient.test(item)) {
                        // Найден подходящий предмет, убираем его из доступных
                        availableItems.remove(i);
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    return false; // Не найден подходящий ингредиент
                }
            }
            
            // ВАЖНО: Проверяем, что не осталось лишних предметов
            // Если все ингредиенты найдены, но остались предметы - рецепт не подходит
            if (!availableItems.isEmpty()) {
                return false; // Есть лишние предметы
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
