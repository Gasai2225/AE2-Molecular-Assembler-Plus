package com.gasai.ccapplied.botania;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import vazkii.botania.api.brew.BrewContainer;
import vazkii.botania.api.recipe.*;

/** Builds ghost inputs from a registered recipe, never from client-provided outputs or costs. */
public final class MagicalRecipeTransfer {
    private MagicalRecipeTransfer() {}

    public record Transfer(MagicalStation station, PoolCatalyst catalyst, List<ItemStack> inputs) {}

    public static Transfer prepare(Level level, ResourceLocation id) {
        if (level == null || id == null) return null;
        var holder = level.getRecipeManager().byKey(id).orElse(null);
        var recipe = holder == null ? null : holder.value();
        if (recipe == null) return null;
        MagicalStation station;
        if (recipe instanceof ManaInfusionRecipe) station = MagicalStation.POOL;
        else if (recipe instanceof RunicAltarRecipe) station = MagicalStation.RUNIC_ALTAR;
        else if (recipe instanceof PetalApothecaryRecipe) station = MagicalStation.APOTHECARY;
        else if (recipe instanceof TerrestrialAgglomerationRecipe) station = MagicalStation.TERRA_PLATE;
        else if (recipe instanceof ElvenTradeRecipe) station = MagicalStation.ELVEN_TRADE;
        else if (recipe instanceof BotanicalBreweryRecipe) station = MagicalStation.BREWERY;
        else if (recipe instanceof PureDaisyRecipe) station = MagicalStation.PURE_DAISY;
        else return null;
        var inputs = new ArrayList<ItemStack>();
        if (recipe instanceof PureDaisyRecipe daisy) {
            daisy.getInput().getDisplayedStacks().stream().filter(s -> s.getItem() instanceof BlockItem)
                    .findFirst().ifPresent(s -> inputs.add(s.copyWithCount(1)));
        } else {
            for (var ingredient : recipe.getIngredients()) if (!append(inputs, ingredient)) return null;
            if (recipe instanceof RecipeWithCatalysts<?> ritual) {
                for (var ingredient : ritual.getCatalysts()) if (!append(inputs, ingredient)) return null;
            }
            if (recipe instanceof RecipeWithReagent<?> ritual && !append(inputs, ritual.getReagent())) return null;
        }
        if (station == MagicalStation.BREWERY) {
            // Container is not in getIngredients(); select a real, supported Botania container.
            for (var item : BuiltInRegistries.ITEM) if (item instanceof BrewContainer) {
                var withContainer = new ArrayList<ItemStack>();
                withContainer.add(new ItemStack(item));
                withContainer.addAll(inputs);
                var transfer = validate(level, id, station, withContainer);
                if (transfer != null) return transfer;
            }
            return null;
        }
        return validate(level, id, station, inputs);
    }

    private static boolean append(List<ItemStack> inputs, Ingredient ingredient) {
        var choices = ingredient.getItems();
        if (choices.length == 0 || inputs.size() >= MagicalRecipeResolver.MAX_INPUTS) return false;
        inputs.add(choices[0].copyWithCount(1));
        return true;
    }

    private static Transfer validate(Level level, ResourceLocation id, MagicalStation station, List<ItemStack> inputs) {
        int capacity = station.inputSlots() + (station.hasReagent() ? 1 : 0);
        if (inputs.isEmpty() || inputs.size() > capacity) return null;
        for (var catalyst : PoolCatalyst.values()) {
            var result = MagicalRecipeResolver.resolve(level, station, catalyst, id, inputs);
            if (result != null) return new Transfer(station, catalyst, List.copyOf(inputs));
        }
        return null;
    }
}
