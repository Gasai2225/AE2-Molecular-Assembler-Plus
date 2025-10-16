package com.gasai.ccapplied.integration.ae2.logic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import appeng.api.stacks.GenericStack;
import appeng.util.ConfigInventory;
import com.gasai.ccapplied.integration.extendedcrafting.ExtendedCraftingRecipeHelper;

/**
 * Утилиты для кодирования экстремальных паттернов
 * Использует существующую логику из ExtremePatternEncodingLogic
 */
public final class ExtremePatternEncoder {

    private ExtremePatternEncoder() {}

    /**
     * Результат кодирования паттерна
     */
    public static class EncodeResult {
        private final boolean success;
        private final ItemStack pattern;
        private final Component errorMessage;

        private EncodeResult(boolean success, ItemStack pattern, Component errorMessage) {
            this.success = success;
            this.pattern = pattern;
            this.errorMessage = errorMessage;
        }

        public static EncodeResult success(ItemStack pattern) {
            return new EncodeResult(true, pattern, null);
        }

        public static EncodeResult failure(Component error) {
            return new EncodeResult(false, ItemStack.EMPTY, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public ItemStack getPattern() {
            return pattern;
        }

        public Component getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Кодирует паттерн используя существующую логику ExtremePatternEncodingLogic
     */
    public static EncodeResult encodePattern(ConfigInventory inputs, ConfigInventory outputs,
                                           ItemStack blankPattern, ItemStack currentPattern, Level level) {
        
        // Проверяем, есть ли бланк для кодирования
        if (blankPattern.isEmpty()) {
            return EncodeResult.failure(Component.translatable("gui.ccapplied.extreme_no_blank_pattern"));
        }

        // Проверяем, что бланк - наш экстремальный бланк
        if (blankPattern.getItem() != com.gasai.ccapplied.core.registry.CCItems.EXTREME_BLANK_PATTERN.get()) {
            return EncodeResult.failure(Component.translatable("gui.ccapplied.extreme_wrong_blank_pattern"));
        }

        // Проверяем, есть ли входные данные
        boolean hasInputs = false;
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.getStack(i) != null) {
                hasInputs = true;
                break;
            }
        }
        
        if (!hasInputs) {
            return EncodeResult.failure(Component.translatable("gui.ccapplied.extreme_no_inputs"));
        }

        // Проверяем, есть ли выходные данные
        boolean hasOutputs = false;
        for (int i = 0; i < outputs.size(); i++) {
            if (outputs.getStack(i) != null) {
                hasOutputs = true;
                break;
            }
        }
        
        if (!hasOutputs) {
            return EncodeResult.failure(Component.translatable("gui.ccapplied.extreme_no_outputs"));
        }

        try {
            // Для экстремальных паттернов нужно создать специальную логику
            // так как стандартные crafting patterns работают только с 3x3 сеткой
            // Создаем custom encoded pattern с поддержкой 9x9 и ExtendedCrafting
            
            // Собираем входные и выходные данные для кодирования
            GenericStack[] inputStacks = collectInputStacks(inputs);
            GenericStack[] outputStacks = collectOutputStacks(outputs);
            
            // Проверяем, что у нас есть данные для кодирования
            if (inputStacks.length == 0) {
                return EncodeResult.failure(Component.translatable("gui.ccapplied.extreme_no_inputs"));
            }
            
            if (outputStacks.length == 0) {
                return EncodeResult.failure(Component.translatable("gui.ccapplied.extreme_no_outputs"));
            }
            
                   // Проверяем рецепт ExtendedCrafting
                   ItemStack[] craftingGrid = convertToCraftingGrid(inputStacks);
                   var recipe = ExtendedCraftingRecipeHelper.findRecipe(craftingGrid, level);
                   
                   if (recipe == null) {
                       return EncodeResult.failure(Component.translatable("gui.ccapplied.extreme_no_valid_recipe"));
                   }
                   
                   // Получаем превью результата
                   ItemStack preview = ExtendedCraftingRecipeHelper.getRecipePreview(craftingGrid, level);
                   if (preview == null) {
                       return EncodeResult.failure(Component.translatable("gui.ccapplied.extreme_recipe_preview_failed"));
                   }
                   
                   String recipeInfo = ExtendedCraftingRecipeHelper.getRecipeInfo(recipe);
                   com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeEncoder] Found valid recipe: {} ({}) -> {}", 
                       recipe.getId(), recipeInfo, preview.getDisplayName().getString());
            
                   // Создаем encoded pattern используя наш custom item
                   ItemStack resultPattern = new ItemStack(com.gasai.ccapplied.core.registry.CCItems.EXTREME_CRAFTING_PATTERN.get());
                   
                   // Используем ExtremeEncodedPatternItem для кодирования
                   var encodedPatternItem = (com.gasai.ccapplied.integration.ae2.pattern.ExtremeEncodedPatternItem) resultPattern.getItem();
                   
                   // Берем первый выход как primary output
                   GenericStack primaryOutput = outputStacks.length > 0 ? outputStacks[0] : null;
                   if (primaryOutput == null) {
                       return EncodeResult.failure(Component.translatable("gui.ccapplied.extreme_no_outputs"));
                   }
                   
                   resultPattern = encodedPatternItem.encode(
                       inputStacks, 
                       primaryOutput, 
                       null // recipeId пока null
                   );
                   
                   return EncodeResult.success(resultPattern);
        } catch (Exception e) {
            return EncodeResult.failure(Component.translatable("gui.ccapplied.extreme_encode_error"));
        }
    }

    /**
     * Очищает входные слоты
     */
    public static void clearInputSlots(ConfigInventory inventory) {
        inventory.clear();
    }

    /**
     * Очищает выходные слоты
     */
    public static void clearOutputSlots(ConfigInventory inventory) {
        inventory.clear();
    }

    /**
     * Собирает входные стеки из ConfigInventory
     */
    private static GenericStack[] collectInputStacks(ConfigInventory inventory) {
        var stacks = new java.util.ArrayList<GenericStack>();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (stack != null && stack.amount() > 0) {
                stacks.add(stack);
            }
        }
        return stacks.toArray(new GenericStack[0]);
    }

    /**
     * Собирает выходные стеки из ConfigInventory
     */
    private static GenericStack[] collectOutputStacks(ConfigInventory inventory) {
        var stacks = new java.util.ArrayList<GenericStack>();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (stack != null && stack.amount() > 0) {
                stacks.add(stack);
            }
        }
        return stacks.toArray(new GenericStack[0]);
    }

    /**
     * Конвертирует GenericStack[] в ItemStack[] для 9x9 сетки крафта
     */
    private static ItemStack[] convertToCraftingGrid(GenericStack[] stacks) {
        ItemStack[] grid = new ItemStack[81]; // 9x9 = 81 слот
        
        for (int i = 0; i < Math.min(stacks.length, 81); i++) {
            GenericStack stack = stacks[i];
            if (stack != null && stack.what() instanceof appeng.api.stacks.AEItemKey itemKey) {
                grid[i] = itemKey.toStack((int) Math.min(stack.amount(), Integer.MAX_VALUE));
            } else {
                grid[i] = ItemStack.EMPTY;
            }
        }
        
        // Заполняем оставшиеся слоты пустыми стеками
        for (int i = stacks.length; i < 81; i++) {
            grid[i] = ItemStack.EMPTY;
        }
        
        return grid;
    }

    /**
     * Конвертирует GenericStack в ItemStack
     */
    private static ItemStack convertToItemStack(GenericStack stack) {
        if (stack != null && stack.what() instanceof appeng.api.stacks.AEItemKey itemKey) {
            return itemKey.toStack((int) Math.min(stack.amount(), Integer.MAX_VALUE));
        }
        return ItemStack.EMPTY;
    }
}