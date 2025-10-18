package com.gasai.ccapplied.parts;

import com.gasai.ccapplied.core.registry.CCItems;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;

import com.gasai.ccapplied.patterns.ExtremeCraftingPattern;
import com.gasai.ccapplied.items.ExtremeEncodedPatternItem;

/**
 * Encoding logic for extreme (9x9) template.
 * Behavior closely follows AE2 PatternEncodingLogic, but:
 *  - input grid 9x9 (81 slots)
 *  - 1 output
 *  - items only (no fluids), quantity in slots = 1
 */
public class ExtremePatternEncodingLogic implements InternalInventoryHost {

    public static final int EXTREME_GRID_SIZE = 9;
    public static final int MAX_INPUT_SLOTS = EXTREME_GRID_SIZE * EXTREME_GRID_SIZE; // 81
    public static final int MAX_OUTPUT_SLOTS = 1;

    private final com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost host;

    private final ConfigInventory encodedInputInv = ConfigInventory.configStacks(
            null, MAX_INPUT_SLOTS, this::onEncodedInputChanged, true);
    private final ConfigInventory encodedOutputInv = ConfigInventory.configStacks(
            null, MAX_OUTPUT_SLOTS, this::onEncodedOutputChanged, true);

    private final AppEngInternalInventory blankPatternInv = new AppEngInternalInventory(this, 1);
    private final AppEngInternalInventory encodedPatternInv = new AppEngInternalInventory(this, 1);

    private boolean isLoading = false;

    @Nullable
    private ResourceLocation recipeId;

    public ExtremePatternEncodingLogic(
        com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost host) {
    this.host = host;
}

    private static boolean isExtremeBlank(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == CCItems.EXTREME_BLANK_PATTERN.get();
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        if (inv == this.encodedPatternInv) {
            loadEncodedPattern(this.encodedPatternInv.getStackInSlot(0));
        }
        saveChanges();
    }

    @Override
    public void saveChanges() {
        if (!isLoading) {
            host.markForSave();
        }
    }

    @Override
    public boolean isClientSide() {
        return host.getLevel().isClientSide();
    }

    private void onEncodedInputChanged() {
        fixExtremeCraftingGrid();
        saveChanges();
    }

    private void onEncodedOutputChanged() {
        saveChanges();
    }

    private void loadEncodedPattern(net.minecraft.world.item.ItemStack patternStack) {
        if (patternStack.isEmpty()) {
            return;
        }
        var details = PatternDetailsHelper.decodePattern(patternStack, host.getLevel());

        if (details instanceof ExtremeCraftingPattern extreme) {
            loadExtremeCraftingPattern(extreme);
        }

        saveChanges();
    }



    private void loadExtremeCraftingPattern(ExtremeCraftingPattern pattern) {
        var dense = pattern.getDenseInputs81();
        fillInventoryFromSparseStacks(encodedInputInv, dense);

        var outs = pattern.getOutputs();
        encodedOutputInv.beginBatch();
        try {
            encodedOutputInv.clear();
            encodedOutputInv.setStack(0, (outs != null && outs.length > 0) ? outs[0] : null);
        } finally {
            encodedOutputInv.endBatch();
        }

        this.recipeId = pattern.getRecipeId();
    }

    private static void fillInventoryFromSparseStacks(ConfigInventory inv, GenericStack[] stacks) {
        inv.beginBatch();
        try {
            for (int i = 0; i < inv.size(); i++) {
                inv.setStack(i, i < stacks.length ? stacks[i] : null);
            }
        } finally {
            inv.endBatch();
        }
    }

    public ConfigInventory getEncodedInputInv() {
        return encodedInputInv;
    }

    public ConfigInventory getEncodedOutputInv() {
        return encodedOutputInv;
    }

    public InternalInventory getBlankPatternInv() {
        return blankPatternInv;
    }

    public InternalInventory getEncodedPatternInv() {
        return encodedPatternInv;
    }

    @Nullable
    public ResourceLocation getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(@Nullable ResourceLocation id) {
        this.recipeId = id;
        saveChanges();
    }

    /* ===================== NBT ===================== */

    public void readFromNBT(CompoundTag data) {
        isLoading = true;
        try {
            if (data.contains("ext_recipeId", Tag.TAG_STRING)) {
                this.recipeId = ResourceLocation.parse(data.getString("ext_recipeId"));
            } else {
                this.recipeId = null;
            }

            blankPatternInv.readFromNBT(data, "ext_blankPattern");
            encodedPatternInv.readFromNBT(data, "ext_encodedPattern");

            encodedInputInv.readFromChildTag(data, "ext_encodedInputs");
            encodedOutputInv.readFromChildTag(data, "ext_encodedOutputs");
        } finally {
            isLoading = false;
        }
    }

    public void writeToNBT(CompoundTag data) {
        if (this.recipeId != null) {
            data.putString("ext_recipeId", this.recipeId.toString());
        }
        blankPatternInv.writeToNBT(data, "ext_blankPattern");
        encodedPatternInv.writeToNBT(data, "ext_encodedPattern");
        encodedInputInv.writeToChildTag(data, "ext_encodedInputs");
        encodedOutputInv.writeToChildTag(data, "ext_encodedOutputs");
    }

    /* ===================== Grid validation ===================== */

    
    public void fixExtremeCraftingGrid() {
        if (host.getLevel() == null || host.getLevel().isClientSide()) {
            return;
        }

        var grid = getEncodedInputInv();
        for (int slot = 0; slot < grid.size(); slot++) {
            var stack = grid.getStack(slot);
            if (stack == null) {
                continue;
            }

            if (!AEItemKey.is(stack.what())) {
                grid.setStack(slot, null);
                continue;
            }

            if (stack.amount() != 1) {
                grid.setStack(slot, new GenericStack(stack.what(), 1));
            }
        }

        var out = getEncodedOutputInv();
        for (int slot = 0; slot < out.size(); slot++) {
            var s = out.getStack(slot);
            if (s == null) continue;
            if (!AEItemKey.is(s.what())) {
                out.setStack(slot, null);
                continue;
            }
        }
    }

    /* ===================== Encoding ===================== */

    
    public boolean encodePattern() {
        if (isClientSide()) {
            return false;
        }

        boolean hasInputs = false;
        for (int i = 0; i < encodedInputInv.size(); i++) {
            if (encodedInputInv.getStack(i) != null) {
                hasInputs = true;
                break;
            }
        }

        if (!hasInputs) {
            return false;
        }

        var output = encodedOutputInv.getStack(0);
        if (output == null) {
            return false;
        }

        var resultPattern = new ItemStack(CCItems.EXTREME_CRAFTING_PATTERN.get());
        var encodedPatternItem = (ExtremeEncodedPatternItem) resultPattern.getItem();

        GenericStack[] inputStacks = new GenericStack[MAX_INPUT_SLOTS];
        for (int i = 0; i < MAX_INPUT_SLOTS; i++) {
            inputStacks[i] = encodedInputInv.getStack(i);
        }

        resultPattern = encodedPatternItem.encode(inputStacks, output, recipeId);

        var existingPattern = encodedPatternInv.getStackInSlot(0);
        if (!existingPattern.isEmpty()) {
            encodedPatternInv.extractItem(0, existingPattern.getCount(), false);
            encodedPatternInv.insertItem(0, resultPattern, false);
        } else {
            var blankPattern = blankPatternInv.getStackInSlot(0);
            if (!isExtremeBlank(blankPattern)) {
                return false;
            }
            
            blankPatternInv.extractItem(0, 1, false);
            
            encodedPatternInv.insertItem(0, resultPattern, false);
        }

        return true;
    }

    
    public void clearCraftingGrid() {
        encodedInputInv.clear();
        encodedOutputInv.clear();
    }

    
    public void fillFromCraftingMatrix(InternalInventory craftingMatrix, ItemStack recipeResult) {
        encodedInputInv.clear();
        encodedOutputInv.clear();

        for (int i = 0; i < Math.min(craftingMatrix.size(), MAX_INPUT_SLOTS); i++) {
            ItemStack stack = craftingMatrix.getStackInSlot(i);
            if (!stack.isEmpty()) {
                var itemKey = AEItemKey.of(stack);
                if (itemKey != null) {
                    encodedInputInv.setStack(i, new GenericStack(itemKey, stack.getCount()));
                }
            }
        }

        if (!recipeResult.isEmpty()) {
            var outputItemKey = AEItemKey.of(recipeResult);
            if (outputItemKey != null) {
                encodedOutputInv.setStack(0, new GenericStack(outputItemKey, recipeResult.getCount()));
            }
        }
    }

    
    public void loadPatternIntoMatrix(InternalInventory craftingMatrix, ItemStack patternStack) {
        if (patternStack.isEmpty()) {
            return;
        }

        var details = PatternDetailsHelper.decodePattern(patternStack, host.getLevel());
        if (!(details instanceof ExtremeCraftingPattern extreme)) {
            return;
        }

        for (int i = 0; i < craftingMatrix.size(); i++) {
            craftingMatrix.insertItem(i, ItemStack.EMPTY, false);
        }

        var inputStacks = extreme.getInputStacks();
        for (int i = 0; i < Math.min(craftingMatrix.size(), inputStacks.length); i++) {
            if (inputStacks[i] != null && !inputStacks[i].isEmpty()) {
                craftingMatrix.insertItem(i, inputStacks[i].copy(), false);
            }
        }

        loadExtremeCraftingPattern(extreme);
    }
}
