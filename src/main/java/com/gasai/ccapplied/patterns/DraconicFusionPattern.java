package com.gasai.ccapplied.patterns;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

public class DraconicFusionPattern implements IMolecularAssemblerSupportedPattern {
    public static final int OUTER_SLOTS = 12;
    public static final int TOTAL_INPUT_SLOTS = 13; // 12 outer + 1 catalyst(center)

    public enum FusionTier {
        WYVERN,
        DRACONIC,
        CHAOTIC
    }

    private final AEItemKey definition;
    private final IInput[] inputs;
    private final GenericStack[] outputs;
    private final ItemStack[] inputStacks;
    private final ItemStack outputStack;
    private final FusionTier tier;
    private final long totalEnergy;
    @Nullable
    private final ResourceLocation recipeId;
    private final Level level;

    public DraconicFusionPattern(
            AEItemKey definition,
            GenericStack[] sparseInputs,
            GenericStack[] sparseOutputs,
            ItemStack[] inputs,
            ItemStack output,
            FusionTier tier,
            long totalEnergy,
            @Nullable ResourceLocation recipeId, Level level) {
        this.definition = java.util.Objects.requireNonNull(definition);
        this.inputStacks = inputs;
        var resolved = com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper.matchRecipe(recipeId, inputs, level);
        this.outputStack = resolved == null ? output
                : com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper.withOutputIdentity(resolved.result(), output);
        this.outputs = resolved == null ? sparseOutputs
                : new GenericStack[] {new GenericStack(AEItemKey.of(outputStack), outputStack.getCount())};
        this.tier = tier;
        this.totalEnergy = totalEnergy;
        this.recipeId = recipeId;
        this.level = level;
        this.inputs = createInputs(sparseInputs);
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DraconicFusionPattern pattern && definition.equals(pattern.definition);
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public List<GenericStack> getOutputs() {
        return Arrays.asList(outputs);
    }

    public ItemStack[] getInputStacks() {
        return inputStacks;
    }

    public ItemStack getOutputStack() {
        return outputStack;
    }

    public FusionTier getTier() {
        var recipe = com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper.currentRecipe(recipeId, level);
        return recipe == null ? tier : com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper.tier(recipe);
    }

    public long getTotalEnergy() {
        var recipe = com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper.currentRecipe(recipeId, level);
        return recipe == null ? totalEnergy : recipe.getEnergyCost();
    }

    @Nullable
    public ResourceLocation getRecipeId() {
        return recipeId;
    }

    private IInput[] createInputs(GenericStack[] sparseInputs) {
        java.util.List<IInput> inputList = new java.util.ArrayList<>();
        for (int slot = 0; slot < sparseInputs.length; slot++) {
            GenericStack stack = sparseInputs[slot];
            if (stack == null || !(stack.what() instanceof AEItemKey item)) {
                continue;
            }
            int inputSlot = slot;
            var choices = new java.util.ArrayList<GenericStack>();
            choices.add(new GenericStack(item, 1));
            var clean = AEItemKey.of(new ItemStack(item.getItem()));
            if (!item.equals(clean) && isItemValid(slot, clean, level))
                choices.add(new GenericStack(clean, 1));
            inputList.add(new IInput() {
                @Override
                public GenericStack[] getPossibleInputs() {
                    return choices.toArray(GenericStack[]::new);
                }

                @Override
                public long getMultiplier() {
                    return stack.amount();
                }

                @Override
                public boolean isValid(AEKey input, Level level) {
                    return input instanceof AEItemKey key && isItemValid(inputSlot, key, level);
                }

                @Override
                public @Nullable AEKey getRemainingKey(AEKey template) {
                    if (!consumesSlot(inputSlot, template)) return template;
                    return template instanceof AEItemKey item ? AEItemKey.of(item.toStack().getCraftingRemainingItem()) : null;
                }
            });
        }
        return inputList.toArray(new IInput[0]);
    }

    @Override
    public ItemStack assemble(Container container, Level level) {
        for (int i = 0; i < Math.min(TOTAL_INPUT_SLOTS, inputStacks.length); i++) {
            ItemStack expected = inputStacks[i];
            if (expected.isEmpty()) {
                continue;
            }
            ItemStack actual = container.getItem(i);
            if (actual.isEmpty() || !isItemValid(i, AEItemKey.of(actual), level) || actual.getCount() < expected.getCount()) {
                return ItemStack.EMPTY;
            }
        }
        var actual = new ItemStack[TOTAL_INPUT_SLOTS];
        for (int i = 0; i < actual.length; i++)
            actual[i] = inputStacks[i].isEmpty() ? ItemStack.EMPTY
                    : container.getItem(i).copyWithCount(inputStacks[i].getCount());
        var match = com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper.matchRecipe(recipeId, actual, level);
        return match != null && ItemStack.matches(outputStack,
                com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper.withOutputIdentity(match.result(), outputStack))
                ? match.result().copy() : ItemStack.EMPTY;
    }

    @Override
    public CraftingRemainders getRemainingItems(CraftingContainer container) {
        var actual = inputStacks.clone();
        for (int slot = 0; slot < actual.length; slot++) {
            if (!actual[slot].isEmpty()) actual[slot] = container.getItem(slot).copyWithCount(actual[slot].getCount());
        }
        var match = com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper.matchRecipe(recipeId, actual, level);
        int[] consumed = new int[inputStacks.length];
        if (match != null) {
            int ingredient = 0;
            for (int slot = 0; slot < OUTER_SLOTS; slot++) {
                if (!actual[slot].isEmpty() && match.consumedIngredients().get(ingredient++)) {
                    consumed[slot] = inputStacks[slot].getCount();
                }
            }
            consumed[OUTER_SLOTS] = inputStacks[OUTER_SLOTS].getCount();
        }
        return CraftingRemainders.collect(container, consumed);
    }

    private boolean consumesSlot(int slot, AEKey template) {
        if (inputStacks[slot].isEmpty()) return false;
        if (slot == OUTER_SLOTS) return true;
        var trial = inputStacks.clone();
        if (template instanceof AEItemKey item) trial[slot] = item.toStack(inputStacks[slot].getCount());
        var match = com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper.matchRecipe(recipeId, trial, level);
        if (match == null) return false;
        int index = 0;
        for (int i = 0; i < slot; i++) if (!inputStacks[i].isEmpty()) index++;
        return match.consumedIngredients().get(index);
    }

    @Override
    public boolean isItemValid(int slot, AEItemKey key, Level level) {
        if (slot < 0 || slot >= TOTAL_INPUT_SLOTS) {
            return false;
        }
        if (slot >= inputStacks.length || inputStacks[slot].isEmpty()) {
            return key == null;
        }
        AEItemKey expected = AEItemKey.of(inputStacks[slot]);
        if (expected == null || key == null || expected.getItem() != key.getItem()) return false;
        if (expected.equals(key)) return true;
        // Respect the recipe's NBT/component predicate instead of rejecting every initialized tool.
        var trial = inputStacks.clone();
        trial[slot] = key.toStack(inputStacks[slot].getCount());
        var match = com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper.matchRecipe(recipeId, trial, level);
        // Substitutions must not silently discard modules/enchantments or change the CPU's promised output.
        return match != null && ItemStack.matches(outputStack,
                com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper.withOutputIdentity(match.result(), outputStack));
    }

    @Override
    public boolean isSlotEnabled(int slot) {
        return slot >= 0 && slot < TOTAL_INPUT_SLOTS && slot < inputStacks.length && !inputStacks[slot].isEmpty();
    }

    @Override
    public void fillCraftingGrid(KeyCounter[] table, IMolecularAssemblerSupportedPattern.CraftingGridAccessor gridAccessor) {
        int inputIndex = 0;
        for (int slot = 0; slot < inputStacks.length; slot++) {
            var expected = inputStacks[slot];
            ItemStack actual = ItemStack.EMPTY;
            if (!expected.isEmpty()) {
                if (inputIndex < table.length && table[inputIndex] != null) {
                    var counter = table[inputIndex];
                    AEItemKey selected = null;
                    for (var entry : counter) {
                        if (entry.getKey() instanceof AEItemKey key && entry.getLongValue() >= expected.getCount()
                                && isItemValid(slot, key, level)) {
                            selected = key;
                            break;
                        }
                    }
                    if (selected != null) {
                        actual = selected.toStack(expected.getCount());
                        counter.remove(selected, expected.getCount());
                    }
                }
                inputIndex++;
            }
            gridAccessor.set(slot, actual);
        }
    }
}
