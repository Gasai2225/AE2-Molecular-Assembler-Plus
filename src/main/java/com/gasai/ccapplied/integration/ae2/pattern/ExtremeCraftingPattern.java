package com.gasai.ccapplied.integration.ae2.pattern;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.gasai.ccapplied.core.registry.CCItems;

/**
 * Паттерн для экстремального крафта 9x9 - только предметы, shaped/unshaped
 */
public class ExtremeCraftingPattern implements IPatternDetails {
    
    public static final int SLOTS = 81; // 9x9 = 81 слот
    
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
        // Создаем IInput[] для 9x9 сетки - только предметы
        // Собираем только не-null элементы в компактный массив
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
                        return 1; // Всегда количество = 1 для крафта
                    }

                    @Override
                    public boolean isValid(AEKey input, Level level) {
                        if (!(input instanceof AEItemKey)) {
                            return false; // Только предметы
                        }
                        return input.equals(stack.what());
                    }

                    @Override
                    public @Nullable AEKey getRemainingKey(AEKey template) {
                        // Для экстремального крафта без остатков
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
     * Возвращает sparse inputs для загрузки в ConfigInventory
     * Теперь возвращаем только не-null элементы
     */
    public GenericStack[] getSparseInputs() {
        java.util.List<GenericStack> sparseList = new java.util.ArrayList<>();
        for (IInput input : inputs) {
            if (input != null) {
                sparseList.add(input.getPossibleInputs()[0]);
            }
        }
        return sparseList.toArray(new GenericStack[0]);
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
}