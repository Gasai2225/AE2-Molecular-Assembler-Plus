package com.gasai.ccapplied.common.util;

import com.blakebr0.extendedcrafting.api.crafting.ITableRecipe;
import com.gasai.ccapplied.common.core.ExtremePatternCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

public final class RecipeEncodingUtil {
    private RecipeEncodingUtil() {}

    /**
     * Кодирует рецепт в NBT шаблона (включая AE2 и Extended Crafting).
     */
    public static CompoundTag encodeRecipeToTag(ResourceLocation id, Recipe<?> recipe, Level level,
                                                boolean substitutes, int time) {
        ItemStack[] inputs = buildInputs81(recipe);
        ItemStack[] outputs = buildOutputs(recipe, level);
        return ExtremePatternCodec.write(id, inputs, outputs, substitutes, time);
    }

    /**
     * Создаёт 9×9 массив входов из рецепта AE2 или Extended Crafting.
     */
    public static ItemStack[] buildInputs81(Recipe<?> recipe) {
        ItemStack[] inputs81 = new ItemStack[81];
        for (int i = 0; i < 81; i++) inputs81[i] = ItemStack.EMPTY;

        // ================= Extended Crafting =================
        if (recipe instanceof ITableRecipe table) {
            int tier = table.getTier(); // 1=3x3, 2=5x5, 3=7x7, 4=9x9
            NonNullList<Ingredient> ings = table.getIngredients();

            int size = switch (tier) {
                case 2 -> 5;
                case 3 -> 7;
                case 4 -> 9;
                default -> 3;
            };

            // Центрируем EC крафт в 9x9 сетке
            int offset = (9 - size) / 2;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int localIndex = y * size + x;
                    if (localIndex >= ings.size()) continue;
                    ItemStack pick = pickFirst(ings.get(localIndex));
                    inputs81[(y + offset) * 9 + (x + offset)] = pick;
                }
            }
            return inputs81;
        }

        // ================= Ванильные Shaped =================
        if (recipe instanceof ShapedRecipe sr) {
            int w = sr.getWidth();
            int h = sr.getHeight();
            NonNullList<Ingredient> ings = sr.getIngredients();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int idx = y * w + x;
                    if (idx < ings.size()) {
                        ItemStack pick = pickFirst(ings.get(idx));
                        inputs81[y * 9 + x] = pick;
                    }
                }
            }
            return inputs81;
        }

        // ================= Ванильные Shapeless =================
        if (recipe instanceof ShapelessRecipe sl) {
            NonNullList<Ingredient> ings = sl.getIngredients();
            for (int i = 0; i < ings.size() && i < 81; i++) {
                inputs81[i] = pickFirst(ings.get(i));
            }
            return inputs81;
        }

        // ================= Фолбэк =================
        NonNullList<Ingredient> ings = recipe.getIngredients();
        for (int i = 0; i < ings.size() && i < 81; i++) {
            inputs81[i] = pickFirst(ings.get(i));
        }
        return inputs81;
    }

    public static ItemStack[] buildOutputs(Recipe<?> recipe, Level level) {
        ItemStack out = recipe.getResultItem(level.registryAccess());
        if (out.isEmpty()) out = ItemStack.EMPTY;
        return new ItemStack[]{ out.copy() };
    }

    private static ItemStack pickFirst(Ingredient ing) {
        ItemStack[] examples = ing.getItems();
        return (examples.length > 0) ? examples[0].copy() : ItemStack.EMPTY;
    }
}
