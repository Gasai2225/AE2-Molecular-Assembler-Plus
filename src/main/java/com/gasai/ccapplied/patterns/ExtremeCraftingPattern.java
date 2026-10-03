package com.gasai.ccapplied.patterns;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;

/**
 * Pattern for extreme crafting 9x9 - items only, shaped/unshaped
 */
public class ExtremeCraftingPattern implements IPatternDetails, IMolecularAssemblerSupportedPattern {
    
    public static final int SLOTS = 81;
    
    private final AEItemKey definition;
    private final IInput[] inputs;
    private final GenericStack[] outputs;
    private final ItemStack[] inputStacks;
    private final ItemStack outputStack;
    private final boolean shaped;
    private final Level level;
    private final int width;
    private final int height;
    @Nullable
    private final net.minecraft.resources.ResourceLocation recipeId;
    
    public ExtremeCraftingPattern(AEItemKey definition, GenericStack[] sparseInputs, GenericStack[] sparseOutputs,
                                ItemStack[] inputs, ItemStack output, boolean shaped, int width, int height,
                                @Nullable net.minecraft.resources.ResourceLocation recipeId, Level level) {
        this.definition = java.util.Objects.requireNonNull(definition);
        this.outputs = sparseOutputs;
        this.inputStacks = inputs;
        this.outputStack = output;
        this.shaped = shaped;
        this.width = width;
        this.height = height;
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
        return other instanceof ExtremeCraftingPattern pattern && definition.equals(pattern.definition);
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
    public GenericStack[] getOutputs() {
        return outputs;
    }
    
    private IInput[] createInputs(GenericStack[] sparseInputs) {
        java.util.List<IInput> inputsList = new java.util.ArrayList<>();
        
        for (int slot = 0; slot < sparseInputs.length; slot++) {
            GenericStack stack = sparseInputs[slot];
            int inputSlot = slot;
            if (stack != null && stack.what() instanceof AEItemKey) {
                inputsList.add(new IInput() {
                    @Override
                    public GenericStack[] getPossibleInputs() {
                        return new GenericStack[]{stack};
                    }

                    @Override
                    public long getMultiplier() {
                        return 1;
                    }

                    @Override
                    public boolean isValid(AEKey input, Level level) {
                        if (!(input instanceof AEItemKey)) {
                            return false;
                        }
                        return input.equals(stack.what());
                    }

                    @Override
                    public @Nullable AEKey getRemainingKey(AEKey template) {
                        var match = com.gasai.ccapplied.crafting.ExtendedCraftingRecipeHelper.findRecipe(inputStacks, level, recipeId);
                        return match == null ? null : AEItemKey.of(match.remainingItems().get(inputSlot));
                    }
                });
            }
        }
        
        return inputsList.toArray(new IInput[0]);
    }
    
    public ItemStack[] getInputStacks() {
        return inputStacks;
    }

    public ItemStack getOutputStack() {
        return outputStack;
    }

    public boolean isShaped() {
        return shaped;
    }
    
    /**
     * Dense representation of 9x9 inputs: array of length 81, indices match grid positions.
     * Based on original ItemStack[] during encoding to preserve placement.
     */
    public GenericStack[] getDenseInputs81() {
        GenericStack[] dense = new GenericStack[SLOTS];
        for (int i = 0; i < Math.min(SLOTS, inputStacks.length); i++) {
            ItemStack st = inputStacks[i];
            if (st != null && !st.isEmpty()) {
                AEItemKey key = AEItemKey.of(st);
                if (key != null) {
                    dense[i] = new GenericStack(key, st.getCount());
                }
            }
        }
        return dense;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    @Nullable
    public net.minecraft.resources.ResourceLocation getRecipeId() {
        return recipeId;
    }
    
    
    @Override
    public ItemStack assemble(Container container, Level level) {
        for (int i = 0; i < Math.min(SLOTS, inputStacks.length); i++) {
            ItemStack patternStack = inputStacks[i];
            if (!patternStack.isEmpty()) {
                ItemStack containerStack = container.getItem(i);
                if (containerStack.isEmpty() || 
                    !ItemStack.isSameItemSameTags(patternStack, containerStack) ||
                    containerStack.getCount() < patternStack.getCount()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        
        var grid = new ItemStack[SLOTS];
        for (int slot = 0; slot < SLOTS; slot++) {
            var expected = slot < inputStacks.length ? inputStacks[slot] : ItemStack.EMPTY;
            if (expected.isEmpty() && !container.getItem(slot).isEmpty()) return ItemStack.EMPTY;
            grid[slot] = expected.isEmpty() ? ItemStack.EMPTY : container.getItem(slot).copyWithCount(expected.getCount());
        }
        var match = com.gasai.ccapplied.crafting.ExtendedCraftingRecipeHelper.findRecipe(grid, level, recipeId);
        return match != null && ItemStack.matches(outputStack, match.result()) ? match.result().copy() : ItemStack.EMPTY;
    }
    
    @Override
    public CraftingRemainders getRemainingItems(CraftingContainer container) {
        int[] consumed = new int[inputStacks.length];
        for (int slot = 0; slot < consumed.length; slot++) consumed[slot] = inputStacks[slot].getCount();
        var grid = new ItemStack[SLOTS];
        for (int slot = 0; slot < grid.length; slot++) {
            grid[slot] = consumed[slot] == 0 ? ItemStack.EMPTY : container.getItem(slot).copyWithCount(consumed[slot]);
        }
        var match = com.gasai.ccapplied.crafting.ExtendedCraftingRecipeHelper.findRecipe(grid, level, recipeId);
        if (match == null) throw new IllegalStateException("Crafting recipe is no longer available");
        return CraftingRemainders.collect(container, consumed, match.remainingItems());
    }

    @Override
    public boolean isItemValid(int slot, AEItemKey key, Level level) {
        if (slot < 0 || slot >= SLOTS) {
            return false;
        }
        
        if (slot >= inputStacks.length || inputStacks[slot].isEmpty()) {
            return key == null;
        }
        
        ItemStack patternStack = inputStacks[slot];
        AEItemKey patternKey = AEItemKey.of(patternStack);
        return patternKey != null && patternKey.equals(key);
    }
    
    @Override
    public boolean isSlotEnabled(int slot) {
        if (slot < 0 || slot >= SLOTS) {
            return false;
        }
        
        if (slot < inputStacks.length) {
            return !inputStacks[slot].isEmpty();
        }
        
        return false;
    }
    
    @Override
    public void fillCraftingGrid(KeyCounter[] table, IMolecularAssemblerSupportedPattern.CraftingGridAccessor gridAccessor) {
        PatternInputs.fill(inputStacks, table, gridAccessor);
    }
}
