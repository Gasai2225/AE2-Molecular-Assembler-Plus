package com.gasai.ccapplied.patterns;

import net.minecraft.core.NonNullList;
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
import com.gasai.ccapplied.core.registry.CCItems;

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
    private final int width;
    private final int height;
    @Nullable
    private final net.minecraft.resources.ResourceLocation recipeId;
    
    public ExtremeCraftingPattern(GenericStack[] sparseInputs, GenericStack[] sparseOutputs, 
                                ItemStack[] inputs, ItemStack output, boolean shaped, int width, int height,
                                @Nullable net.minecraft.resources.ResourceLocation recipeId) {
        this.definition = AEItemKey.of(CCItems.EXTREME_CRAFTING_PATTERN.get());
        this.inputs = createInputs(sparseInputs);
        this.outputs = sparseOutputs;
        this.inputStacks = inputs;
        this.outputStack = output;
        this.shaped = shaped;
        this.width = width;
        this.height = height;
        this.recipeId = recipeId;
    }
    
    public ExtremeCraftingPattern(GenericStack[] sparseInputs, GenericStack[] sparseOutputs, 
                                ItemStack[] inputs, ItemStack output, boolean shaped) {
        this(sparseInputs, sparseOutputs, inputs, output, shaped, 9, 9, null);
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
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
        
        for (GenericStack stack : sparseInputs) {
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
                        return null;
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
        
        return outputStack.copy();
    }
    
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                ItemStack patternStack = (i < inputStacks.length) ? inputStacks[i] : ItemStack.EMPTY;
                if (!patternStack.isEmpty()) {
                    int used = patternStack.getCount();
                    int remainingCount = stack.getCount() - used;
                    if (remainingCount > 0) {
                        ItemStack remainingStack = stack.copy();
                        remainingStack.setCount(remainingCount);
                        remaining.set(i, remainingStack);
                    } else {
                        remaining.set(i, ItemStack.EMPTY);
                    }
                } else {
                    remaining.set(i, ItemStack.EMPTY);
                }
            } else {
                remaining.set(i, ItemStack.EMPTY);
            }
        }
        return remaining;
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
        for (int i = 0; i < SLOTS; i++) {
            gridAccessor.set(i, ItemStack.EMPTY);
        }
        
        for (int i = 0; i < Math.min(SLOTS, inputStacks.length); i++) {
            ItemStack patternStack = inputStacks[i];
            if (!patternStack.isEmpty()) {
                AEItemKey patternKey = AEItemKey.of(patternStack);
                if (patternKey != null) {
                    for (KeyCounter counter : table) {
                        if (counter != null && !counter.isEmpty()) {
                            var entry = counter.iterator().next();
                            if (entry.getKey() instanceof AEItemKey itemKey && 
                                itemKey.equals(patternKey) && 
                                entry.getLongValue() >= patternStack.getCount()) {
                                
                                ItemStack stack = itemKey.toStack(patternStack.getCount());
                                gridAccessor.set(i, stack);
                                
                                counter.remove(itemKey, patternStack.getCount());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}