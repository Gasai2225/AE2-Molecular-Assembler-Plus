package com.gasai.ccapplied.integration.jei;

import com.gasai.ccapplied.integration.ae2.menu.ExtremePatternEncodingTermMenu;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;

public class ExtremeJeiRecipeTransferHandler implements IRecipeTransferHandler<ExtremePatternEncodingTermMenu, CraftingRecipe> {
    private final IRecipeTransferHandlerHelper helper;

    public ExtremeJeiRecipeTransferHandler(IRecipeTransferHandlerHelper helper) {
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
    public mezz.jei.api.recipe.RecipeType<CraftingRecipe> getRecipeType() {
        return mezz.jei.api.constants.RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(ExtremePatternEncodingTermMenu menu, CraftingRecipe recipe, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) return null;
        List<ItemStack> inputs;
        int w;
        int h;
        if (recipe instanceof ShapedRecipe shaped) {
            w = Math.min(3, shaped.getWidth());
            h = Math.min(3, shaped.getHeight());
            var ings = shaped.getIngredients();
            inputs = new ArrayList<>(w * h);
            for (int i = 0; i < w * h; i++) {
                var ing = i < ings.size() ? ings.get(i) : net.minecraft.world.item.crafting.Ingredient.EMPTY;
                if (ing.isEmpty()) inputs.add(ItemStack.EMPTY);
                else {
                    var arr = ing.getItems();
                    inputs.add(arr.length > 0 ? arr[0].copy() : ItemStack.EMPTY);
                }
            }
        } else {
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

        menu.requestApplyJeiGrid(w, h, inputs);
        return null;
    }
}


