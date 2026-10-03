package com.gasai.ccapplied.botania;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import vazkii.botania.api.brew.BrewContainer;
import vazkii.botania.api.recipe.*;

/** Resolves actual world recipes; encoded outputs and mana costs are never trusted. */
public final class MagicalRecipeResolver {
    public static final int MAX_INPUTS = 17;
    private MagicalRecipeResolver() {}

    public record Result(ResourceLocation recipeId, List<ItemStack> outputs, long mana, int ticks) {
        public Result {
            outputs = outputs.stream().filter(s -> !s.isEmpty()).map(ItemStack::copy).toList();
            if (outputs.isEmpty() || mana < 0 || ticks < 1) throw new IllegalArgumentException("Invalid magical recipe result");
        }
    }

    public static Result resolve(Level level, MagicalStation station, PoolCatalyst catalyst,
            ResourceLocation recipeId, List<ItemStack> supplied) {
        if (supplied.isEmpty() || supplied.size() > MAX_INPUTS
                || supplied.stream().anyMatch(s -> s.isEmpty() || s.getCount() != 1)) return null;
        if (station != MagicalStation.POOL && catalyst != PoolCatalyst.NONE) return null;
        var recipe = level.getRecipeManager().byKey(recipeId).orElse(null);
        if (recipe == null) return null;
        var inputs = supplied.stream().map(ItemStack::copy).toList();
        var inventory = new SimpleContainer(inputs.toArray(ItemStack[]::new));
        var outputs = new ArrayList<ItemStack>();
        long mana = 0;
        int ticks = 20;
        switch (station) {
            case POOL -> {
                if (!(recipe instanceof ManaInfusionRecipe pool) || inputs.size() != 1 || !pool.matches(inputs.get(0))) return null;
                var required = pool.getRecipeCatalyst();
                if (required == null ? catalyst != PoolCatalyst.NONE : !required.test(catalyst.state())) return null;
                outputs.add(pool.getRecipeOutput(level.registryAccess(), inputs.get(0)));
                mana = pool.getManaToConsume();
            }
            case APOTHECARY, RUNIC_ALTAR -> {
                if (station == MagicalStation.APOTHECARY && !(recipe instanceof PetalApothecaryRecipe)
                        || station == MagicalStation.RUNIC_ALTAR && !(recipe instanceof RunicAltarRecipe)) return null;
                var ritual = (RecipeWithReagent) recipe;
                // Last input is the seed/livingrock, never a free or implicitly generated reagent.
                var reagent = inputs.get(inputs.size() - 1);
                if (!ritual.getReagent().test(reagent)) return null;
                var ingredients = new SimpleContainer(inputs.subList(0, inputs.size() - 1).toArray(ItemStack[]::new));
                if (!ritual.matches(ingredients, level)) return null;
                outputs.add(ritual.assemble(ingredients, level.registryAccess()));
                if (recipe instanceof RunicAltarRecipe altar) {
                    mana = altar.getManaUsage();
                    ticks = 60;
                    for (var input : inputs.subList(0, inputs.size() - 1))
                        if (input.getItem() instanceof vazkii.botania.common.item.material.RuneItem) outputs.add(input.copy());
                }
            }
            case TERRA_PLATE -> {
                if (!(recipe instanceof TerrestrialAgglomerationRecipe plate) || !plate.matches(inventory, level)) return null;
                outputs.add(plate.assemble(inventory, level.registryAccess()));
                mana = plate.getMana();
                ticks = 100;
            }
            case ELVEN_TRADE -> {
                if (!(recipe instanceof ElvenTradeRecipe trade) || trade.isReturnRecipe()) return null;
                var matched = trade.match(inputs);
                if (matched.isEmpty() || matched.get().size() != inputs.size()) return null;
                outputs.addAll(trade.getOutputs(inputs));
                mana = 500;
            }
            case BREWERY -> {
                if (!(recipe instanceof BotanicalBreweryRecipe brew)
                        || !(inputs.get(0).getItem() instanceof BrewContainer container)
                        || inputs.size() != brew.getIngredients().size() + 1 || !brew.matches(inventory, level)) return null;
                mana = container.getManaCost(brew.getBrew(), inputs.get(0));
                if (mana < 0) return null;
                outputs.add(brew.getOutput(inputs.get(0)));
                ticks = 100;
            }
            case PURE_DAISY -> {
                if (!(recipe instanceof PureDaisyRecipe daisy) || inputs.size() != 1
                        || !(inputs.get(0).getItem() instanceof BlockItem block)
                        || !daisy.getInput().test(block.getBlock().defaultBlockState())) return null;
                // In-world functions and non-item block transformations need a world/fluid adapter.
                if (daisy.getSuccessFunction().getId() != null) return null;
                outputs.add(new ItemStack(daisy.getOutputState().getBlock()));
                ticks = Math.max(1, daisy.getTime() * 8);
            }
        }
        if (outputs.isEmpty() || outputs.get(0).isEmpty()) return null;
        return new Result(recipeId, outputs, mana, ticks);
    }

    public static List<List<ItemStack>> alternatives(Level level, MagicalStation station,
            Result expected, List<ItemStack> inputs) {
        if (station == MagicalStation.PURE_DAISY)
            return List.of(daisyAlternatives(level, expected, inputs.get(0)));
        var recipe = level.getRecipeManager().byKey(expected.recipeId()).orElse(null);
        if (station != MagicalStation.APOTHECARY || !(recipe instanceof PetalApothecaryRecipe petal))
            return List.of();
        var result = new ArrayList<List<ItemStack>>();
        var candidates = new ArrayList<ItemStack>();
        for (var ingredient : recipe.getIngredients()) candidates.addAll(List.of(ingredient.getItems()));
        for (int slot = 0; slot < inputs.size(); slot++) {
            var choices = new ArrayList<ItemStack>();
            choices.add(inputs.get(slot).copy());
            var available = slot == inputs.size() - 1 ? List.of(petal.getReagent().getItems()) : candidates;
            for (var candidate : available) {
                if (candidate.isEmpty()) continue;
                var replacement = candidate.copyWithCount(1);
                if (choices.stream().anyMatch(s -> ItemStack.matches(s, replacement))) continue;
                var trial = new ArrayList<>(inputs);
                trial.set(slot, replacement);
                var resolved = resolve(level, station, PoolCatalyst.NONE, expected.recipeId(), trial);
                if (sameResult(expected, resolved)) choices.add(replacement);
            }
            result.add(List.copyOf(choices));
        }
        return List.copyOf(result);
    }

    public static boolean sameResult(Result expected, Result actual) {
        if (actual == null || expected.mana() != actual.mana() || expected.ticks() != actual.ticks()
                || expected.outputs().size() != actual.outputs().size()) return false;
        for (int i = 0; i < expected.outputs().size(); i++)
            if (!ItemStack.matches(expected.outputs().get(i), actual.outputs().get(i))) return false;
        return true;
    }

    /** Only alternatives of the same Daisy recipe with identical output are safe substitutions. */
    public static List<ItemStack> daisyAlternatives(Level level, Result expected, ItemStack primary) {
        var result = new ArrayList<ItemStack>();
        result.add(primary.copyWithCount(1));
        var recipe = level.getRecipeManager().byKey(expected.recipeId()).orElse(null);

        if (!(recipe instanceof PureDaisyRecipe daisy)) return result;
        for (var candidate : daisy.getInput().getDisplayedStacks()) {
            if (candidate.isEmpty()) continue;
            var stack = candidate.copyWithCount(1);
            var resolved = resolve(level, MagicalStation.PURE_DAISY, PoolCatalyst.NONE, expected.recipeId(), List.of(stack));
            if (resolved == null || resolved.outputs().size() != expected.outputs().size()
                    || resolved.mana() != expected.mana() || resolved.ticks() != expected.ticks()) continue;
            boolean same = true;
            for (int index = 0; index < expected.outputs().size(); index++)
                same &= ItemStack.matches(resolved.outputs().get(index), expected.outputs().get(index));
            if (same && result.stream().noneMatch(existing -> ItemStack.matches(existing, stack))) result.add(stack);
        }
        return List.copyOf(result);
    }

    private static boolean belongsTo(net.minecraft.world.item.crafting.Recipe<?> recipe, MagicalStation station) {
        return switch (station) {
            case POOL -> recipe instanceof ManaInfusionRecipe;
            case RUNIC_ALTAR -> recipe instanceof RunicAltarRecipe;
            case APOTHECARY -> recipe instanceof PetalApothecaryRecipe;
            case TERRA_PLATE -> recipe instanceof TerrestrialAgglomerationRecipe;
            case ELVEN_TRADE -> recipe instanceof ElvenTradeRecipe;
            case BREWERY -> recipe instanceof BotanicalBreweryRecipe;
            case PURE_DAISY -> recipe instanceof PureDaisyRecipe;
        };
    }

    public static Result find(Level level, MagicalStation station, PoolCatalyst catalyst, List<ItemStack> inputs) {
        if (inputs.isEmpty()) return null;
        for (var recipe : level.getRecipeManager().getRecipes().stream()
                .filter(r -> belongsTo(r, station))
                .sorted(java.util.Comparator.comparing(r -> r.getId().toString())).toList()) {
            var result = resolve(level, station, catalyst, recipe.getId(), inputs);
            if (result != null) return result;
        }
        return null;
    }
}
