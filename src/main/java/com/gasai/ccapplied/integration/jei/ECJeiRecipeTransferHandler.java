package com.gasai.ccapplied.integration.jei;

import com.blakebr0.extendedcrafting.api.crafting.ITableRecipe;
import com.gasai.ccapplied.integration.ae2.menu.ExtremePatternEncodingTermMenu;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ECJeiRecipeTransferHandler implements IRecipeTransferHandler<ExtremePatternEncodingTermMenu, ITableRecipe> {
    private final RecipeType<ITableRecipe> type;
    private final IRecipeTransferHandlerHelper helper;

    public ECJeiRecipeTransferHandler(RecipeType<ITableRecipe> type, IRecipeTransferHandlerHelper helper) {
        this.type = type;
        this.helper = helper;
    }

    @Override
    public Class<ExtremePatternEncodingTermMenu> getContainerClass() {
        return ExtremePatternEncodingTermMenu.class;
    }

    @Override
    public java.util.Optional<net.minecraft.world.inventory.MenuType<ExtremePatternEncodingTermMenu>> getMenuType() {
        return java.util.Optional.of(ExtremePatternEncodingTermMenu.TYPE);
    }

    @Override
    public RecipeType<ITableRecipe> getRecipeType() {
        return type;
    }

    @Override
    public IRecipeTransferError transferRecipe(ExtremePatternEncodingTermMenu menu, ITableRecipe recipe, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) return null;
        List<ItemStack> inputs = new ArrayList<>();

        int w;
        int h;
        try {
            if (recipe instanceof com.blakebr0.extendedcrafting.crafting.recipe.ShapedTableRecipe shaped) {
                w = Math.min(9, shaped.getWidth());
                h = Math.min(9, shaped.getHeight());
                var ings = shaped.getIngredients();
                inputs = new ArrayList<>(w * h);
                for (int i = 0; i < w * h; i++) {
                    var ing = i < ings.size() ? ings.get(i) : net.minecraft.world.item.crafting.Ingredient.EMPTY;
                    if (ing.isEmpty()) {
                        inputs.add(ItemStack.EMPTY);
                    } else {
                        var arr = ing.getItems();
                        inputs.add(arr.length > 0 ? arr[0].copy() : ItemStack.EMPTY);
                    }
                }
            } else {
                // shapeless (EC): используем ингредиенты рецепта, что реально участвуют в крафте
                var ings = recipe.getIngredients();
                inputs = new ArrayList<>(ings.size());
                for (var ing : ings) {
                    if (ing.isEmpty()) continue;
                    var arr = ing.getItems();
                    inputs.add(arr.length > 0 ? arr[0].copy() : ItemStack.EMPTY);
                }
                w = 1;
                h = inputs.size();
            }
        } catch (Throwable t) {
            // fallback: пусто
            w = 1;
            h = 0;
        }
        // Лог для диагностики shapeless/nbt
        try {
            com.gasai.ccapplied.CCApplied.LOG.info("[JEI EC Transfer] doTransfer={} type={} w={} h={} inputs={} first={}",
                    doTransfer,
                    (recipe instanceof com.blakebr0.extendedcrafting.crafting.recipe.ShapedTableRecipe) ? "shaped" : "shapeless",
                    w, h, inputs.size(), inputs.isEmpty() ? "empty" : inputs.get(0));
        } catch (Exception ignored) {}
        menu.requestApplyJeiGrid(w, h, inputs);
        return null;
    }
}


