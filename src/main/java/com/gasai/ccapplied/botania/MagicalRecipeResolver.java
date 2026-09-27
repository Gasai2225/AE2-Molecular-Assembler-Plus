package com.gasai.ccapplied.botania;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import vazkii.botania.common.crafting.recipe.StacksProcessingRecipeInput;
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
        var holder = level.getRecipeManager().byKey(recipeId).orElse(null);
        var recipe = holder == null ? null : holder.value();
        if (recipe == null) return null;
        var inputs = supplied.stream().map(ItemStack::copy).toList();
        var inventory = new StacksProcessingRecipeInput(inputs.toArray(ItemStack[]::new));
        var outputs = new ArrayList<ItemStack>();
        long mana = 0;
        int ticks = 20;
        switch (station) {
            case POOL -> {
                if (!(recipe instanceof ManaInfusionRecipe pool) || inputs.size() != 1 || !pool.matches(inputs.get(0))) return null;
                var required = pool.getRecipeCatalyst();
                boolean noCatalyst = required == null || required.getType() == vazkii.botania.common.crafting.StateIngredients.NONE_TYPE;
                if (noCatalyst ? catalyst != PoolCatalyst.NONE : !required.test(catalyst.state())) return null;
                outputs.add(pool.getRecipeOutput(level.registryAccess(), inputs.get(0)));
                mana = pool.getManaToConsume();
            }
            case APOTHECARY, RUNIC_ALTAR -> {
                if (station == MagicalStation.APOTHECARY && !(recipe instanceof PetalApothecaryRecipe)
                        || station == MagicalStation.RUNIC_ALTAR && !(recipe instanceof RunicAltarRecipe)) return null;
                var ritual = (RecipeWithReagent<ProcessingRecipeInput>) recipe;
                // Last input is the seed/livingrock, never a free or implicitly generated reagent.
                var reagent = inputs.get(inputs.size() - 1);
                if (!ritual.getReagent().test(reagent)) return null;
                var ingredients = new StacksProcessingRecipeInput(inputs.subList(0, inputs.size() - 1).toArray(ItemStack[]::new));
                if (!ritual.matches(ingredients, level)) return null;
                outputs.add(ritual.assemble(ingredients, level.registryAccess()));
                if (recipe instanceof RunicAltarRecipe altar) {
                    mana = altar.getMana();
                    ticks = 60;
                    outputs.addAll(altar.getRemainingItems(ingredients));
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
                var matched = trade.tryAssemble(inventory, level.registryAccess());
                if (matched.isEmpty() || matched.get().matchedInputSlots().size() != inputs.size()
                        || matched.get().matchedInputSlots().values().intStream().anyMatch(n -> n != 1)) return null;
                outputs.addAll(matched.get().outputs());
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
                if (daisy.getSuccessFunction().isPresent() || daisy.getPreUpdateFunction().isPresent()) return null;
                var states = daisy.getOutput().getDisplayed();
                if (states.size() != 1) return null;
                outputs.add(new ItemStack(states.get(0).getBlock()));
                ticks = Math.max(1, daisy.getTime() * 8);
            }
        }
        if (outputs.isEmpty() || outputs.get(0).isEmpty()) return null;
        return new Result(recipeId, outputs, mana, ticks);
    }

    /** Only alternatives of the same Daisy recipe with identical output are safe substitutions. */
    public static List<ItemStack> daisyAlternatives(Level level, Result expected, ItemStack primary) {
        var result = new ArrayList<ItemStack>();
        result.add(primary.copyWithCount(1));
        var holder = level.getRecipeManager().byKey(expected.recipeId()).orElse(null);
        var recipe = holder == null ? null : holder.value();
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
                .filter(r -> belongsTo(r.value(), station))
                .sorted(java.util.Comparator.comparing(r -> r.id().toString())).toList()) {
            var result = resolve(level, station, catalyst, recipe.id(), inputs);
            if (result != null) return result;
        }
        return null;
    }
}
