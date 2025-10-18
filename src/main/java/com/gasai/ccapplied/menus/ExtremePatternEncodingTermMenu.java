package com.gasai.ccapplied.menus;

import com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.slot.FakeSlot;
import appeng.menu.SlotSemantics;

import com.gasai.ccapplied.parts.ExtremePatternEncodingLogic;
import com.gasai.ccapplied.slots.ExtremeBlankPatternSlot;
import com.gasai.ccapplied.slots.ExtremeEncodedPatternSlot;
import com.gasai.ccapplied.slots.ExtremeCraftingTermSlot;
import com.gasai.ccapplied.core.registry.CCItems;

import java.util.List;

/**
 * Menu for extreme pattern encoding terminal (9×9).
 * Only "crafting" mode: inputs[81] + output[1].
 */

public class ExtremePatternEncodingTermMenu extends MEStorageMenu implements appeng.helpers.IMenuCraftingPacket, appeng.util.inv.InternalInventoryHost {

    public static final int GRID = 9;
    public static final int SLOTS = GRID * GRID;

    public static final String ACTION_ENCODE = "ext_encode";
    public static final String ACTION_CLEAR  = "ext_clear";

    public static final MenuType<ExtremePatternEncodingTermMenu> TYPE =
    MenuTypeBuilder.create(
        ExtremePatternEncodingTermMenu::new,
        IExtremePatternTerminalMenuHost.class
    ).build("extreme_patternterm");

    private final ExtremePatternEncodingLogic logic;

    private final FakeSlot[] inputSlots = new FakeSlot[SLOTS];
    private final ExtremeCraftingTermSlot outputSlot;
    
    private final appeng.util.inv.AppEngInternalInventory craftingMatrix;

    private final Slot blankPatternSlot;
    private final Slot encodedPatternSlot;

    private final appeng.util.ConfigInventory encodedInputsInv;
    private final appeng.util.ConfigInventory encodedOutputsInv;

    @GuiSync(10)
    public boolean uiActive = true;

    @GuiSync(11)
    public boolean networkConnected = false;

    @GuiSync(12)
    public int patternInputCount = 0;

    @GuiSync(13)
    public int patternOutputCount = 0;

    public ExtremePatternEncodingTermMenu(int id, Inventory inv, IExtremePatternTerminalMenuHost host) {
        this(TYPE, id, inv, host, true);}

    public ExtremePatternEncodingTermMenu(MenuType<?> type, int id, Inventory inv, IExtremePatternTerminalMenuHost host, boolean bindInv) {
        super(type, id, inv, host, bindInv);


        this.logic = host.getLogic();      

        this.encodedInputsInv  = logic.getEncodedInputInv();
        this.encodedOutputsInv = logic.getEncodedOutputInv();
        
        this.craftingMatrix = new appeng.util.inv.AppEngInternalInventory(this, SLOTS);

        var inputsWrapper  = encodedInputsInv.createMenuWrapper();
        var outputsWrapper = encodedOutputsInv.createMenuWrapper();

        for (int i = 0; i < SLOTS; i++) {
            var fs = new FakeSlot(craftingMatrix, i);
            fs.setHideAmount(true);
            this.inputSlots[i] = fs;
            this.addSlot(fs, SlotSemantics.CRAFTING_GRID);
        }

        var out = new ExtremeCraftingTermSlot(
            inv.player, 
            this.getActionSource(), 
            this.powerSource,
            host.getInventory(), 
            craftingMatrix,
            this,
            outputsWrapper, 
            0
        );
        this.addSlot(this.outputSlot = out, SlotSemantics.CRAFTING_RESULT);
        

        this.blankPatternSlot = this.addSlot(
                new ExtremeBlankPatternSlot(logic.getBlankPatternInv(), 0),
                SlotSemantics.BLANK_PATTERN
        );
        this.encodedPatternSlot = this.addSlot(
                new ExtremeEncodedPatternSlot(logic.getEncodedPatternInv(), 0),
                SlotSemantics.ENCODED_PATTERN
        );

        registerClientAction(ACTION_ENCODE, this::encode);
        registerClientAction(ACTION_CLEAR,  this::clearAll);
        registerClientAction("jei_apply_grid", JeiGridData.class, data -> {
            if (isClientSide()) return;
            applyCenteredGridFromClient(data.width, data.height, data.items);
        });
        registerClientAction("extremeEncodePattern", this::extremeEncodePattern);
        registerClientAction("extremeClearPattern", this::extremeClearPattern);
        registerClientAction("extremeCraftingClearPattern", this::extremeCraftingClearPattern);
        
        this.uiActive = true;
        this.networkConnected = false;
        this.patternInputCount = 0;
        this.patternOutputCount = 0;

        if (!isClientSide()) {
            loadCraftingMatrixFromLogic();
        }
    }

    /* -------------------- update methods -------------------- */
    
    @Override
    public void setItem(int slotID, int stateId, ItemStack stack) {
        super.setItem(slotID, stateId, stack);
    }

    /* -------------------- actions -------------------- */

    public void encode() {
        if (isClientSide()) {
            sendClientAction(ACTION_ENCODE);
            return;
        }


        ItemStack recipeResult = getRecipePreview();
        
        if (recipeResult.isEmpty()) {
            return;
        }
        
        var encodingLogic = ((com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost) getHost()).getLogic();
        encodingLogic.fillFromCraftingMatrix(craftingMatrix, recipeResult);
        
        boolean success = encodingLogic.encodePattern();
        
        
    }

    public void clearAll() {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR);
            return;
        }
        
        for (int i = 0; i < SLOTS; i++) {
            inputSlots[i].set(ItemStack.EMPTY);
        }
        outputSlot.set(ItemStack.EMPTY);
        
        var encodingLogic = ((com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost) getHost()).getLogic();
        encodingLogic.clearCraftingGrid();
        
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        
    }

    /* -------------------- utilities -------------------- */

    private static boolean isOurBlank(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == CCItems.EXTREME_BLANK_PATTERN.get();
    }

    public boolean canEncode() {
        
        if (this.blankPatternSlot.getItem().isEmpty()) {
            return false;
        }
        
        boolean hasInputItems = false;
        int inputItemCount = 0;
        if (inputSlots != null) {
            for (FakeSlot slot : inputSlots) {
                if (!slot.getItem().isEmpty()) {
                    hasInputItems = true;
                    inputItemCount++;
                }
            }
        }
        
        
        if (!hasInputItems) {
            return false;
        }
        
        if (!isClientSide()) {
            ItemStack recipeResult = getRecipePreview();
            boolean hasValidRecipe = !recipeResult.isEmpty();
            return hasValidRecipe;
        }
        
        return true;
    }

    public boolean hasPattern() {
        return this.patternInputCount > 0 || this.patternOutputCount > 0;
    }

    public boolean isNetworkConnected() {
        return this.networkConnected;
    }

    public int getPatternInputCount() {
        return this.patternInputCount;
    }

    public int getPatternOutputCount() {
        return this.patternOutputCount;
    }
    
    /**
     * Gets and updates recipe result preview for current grid.
     * Similar to getAndUpdateOutput() from PatternEncodingTermMenu.
     */
    public ItemStack getAndUpdateOutput() {
        return this.outputSlot.getDisplayedCraftingOutput();
    }
    
    /**
     * Gets recipe result preview for current grid
     */
    public ItemStack getRecipePreview() {
        if (isClientSide()) {
            return ItemStack.EMPTY;
        }
        
        if (inputSlots == null || getPlayer() == null || getPlayer().level() == null) {
            return ItemStack.EMPTY;
        }
        
        try {
            if (outputSlot != null && outputSlot instanceof ExtremeCraftingTermSlot) {
                ItemStack result = outputSlot.getItem();
                if (!result.isEmpty()) {
                    return result;
                }
            }
            
            ItemStack[] craftingGrid = new ItemStack[SLOTS];
            int nonEmptySlots = 0;
            for (int i = 0; i < SLOTS; i++) {
                craftingGrid[i] = inputSlots[i].getItem();
                if (!craftingGrid[i].isEmpty()) {
                    nonEmptySlots++;
                }
            }
            
            
            ItemStack result = com.gasai.ccapplied.crafting.ExtendedCraftingRecipeHelper.getRecipePreview(craftingGrid, getPlayer().level());
            return result != null ? result : ItemStack.EMPTY;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
    
    


    @Override
    protected ItemStack transferStackToMenu(ItemStack input) {
        if (blankPatternSlot.mayPlace(input)) {
            input = blankPatternSlot.safeInsert(input);
            if (input.isEmpty()) return ItemStack.EMPTY;
        }
        if (encodedPatternSlot.mayPlace(input)) {
            input = encodedPatternSlot.safeInsert(input);
            if (input.isEmpty()) return ItemStack.EMPTY;
        }
        return super.transferStackToMenu(input);
    }

    @Override public InternalInventory getCraftingMatrix() {
        return encodedInputsInv.createMenuWrapper().getSubInventory(0, SLOTS);
    }

    @Override
    public boolean useRealItems() {
        return false;
    }

    public FakeSlot[] getInputSlots() { return inputSlots; }
    public ExtremeCraftingTermSlot getOutputSlot() { return outputSlot; }

    public boolean canEncodePattern() {
        boolean hasInputs = false;
        for (int i = 0; i < SLOTS; i++) {
            if (encodedInputsInv.getStack(i) != null) {
                hasInputs = true;
                break;
            }
        }
        
        if (!hasInputs) return false;
        
        if (isClientSide()) {
            return true;
        }
        
        return !getRecipePreview().isEmpty();
    }

    public boolean hasItemsInGrid() {
        return canEncodePattern();
    }

    public boolean hasNetworkConnection() {
        return getNetworkNode() != null;
    }

    public int getFilledSlotCount() {
        int count = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (encodedInputsInv.getStack(i) != null) {
                count++;
            }
        }
        return count;
    }

    public int getTotalSlotCount() {
        return SLOTS;
    }

    @Override
    public void onSlotChange(Slot slot) {
        if (slot instanceof FakeSlot && isInputSlot(slot)) {
            if (!isClientSide()) {
                int slotIndex = slot.getContainerSlot();
                if (slotIndex >= 0 && slotIndex < SLOTS) {
                    var stack = craftingMatrix.getStackInSlot(slotIndex);
                    if (stack.isEmpty()) {
                        encodedInputsInv.setStack(slotIndex, null);
                    } else {
                        var key = appeng.api.stacks.AEItemKey.of(stack);
                        if (key != null) {
                            encodedInputsInv.setStack(slotIndex, new appeng.api.stacks.GenericStack(key, 1));
                        } else {
                            encodedInputsInv.setStack(slotIndex, null);
                        }
                    }
                }
            }
        }
        
        if (slot == encodedPatternSlot && !isClientSide()) {
            var patternStack = slot.getItem();
            if (!patternStack.isEmpty()) {
                var encodingLogic = ((com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost) getHost()).getLogic();
                encodingLogic.loadPatternIntoMatrix(craftingMatrix, patternStack);
            } else {
                clearAll();
            }
        }
    }
    
    /**
     * Checks if slot is input slot of crafting grid
     */
    private boolean isInputSlot(Slot slot) {
        for (FakeSlot inputSlot : this.inputSlots) {
            if (inputSlot == slot) {
                return true;
            }
        }
        return false;
    }

    /** Data for JEI transfer (Gson-friendly DTO) */
    public static class JeiGridData {
        public final int width;
        public final int height;
        public final java.util.List<JeiItem> items;

        public JeiGridData(int width, int height, java.util.List<JeiItem> items) {
            this.width = width;
            this.height = height;
            this.items = items;
        }
    }

    /** One item for JEI transfer */
    public static class JeiItem {
        public final String snbt;

        public JeiItem(String snbt) {
            this.snbt = snbt;
        }
    }

    private void applyCenteredGridFromClient(int w, int h, java.util.List<JeiItem> items) {
        if (isClientSide()) return;
        if (w <= 0 || h <= 0) return;
        final boolean isShapeless = w == 1;
        if (!isShapeless && (w > GRID || h > GRID)) return;

        try {
            for (int i = 0; i < SLOTS; i++) {
                craftingMatrix.extractItem(i, Integer.MAX_VALUE, false);
                craftingMatrix.insertItem(i, ItemStack.EMPTY, false);
                encodedInputsInv.setStack(i, null);
            }
            logic.getEncodedInputInv().beginBatch();
            for (int i = 0; i < SLOTS; i++) logic.getEncodedInputInv().setStack(i, null);
            logic.getEncodedInputInv().endBatch();
        } catch (Exception e) {
        }

        java.util.List<ItemStack> decoded = new java.util.ArrayList<>(items.size());
        for (var ji : items) {
            ItemStack st = ItemStack.EMPTY;
            try {
                var tag = net.minecraft.nbt.TagParser.parseTag(ji.snbt);
                if (tag instanceof net.minecraft.nbt.CompoundTag) {
                    st = ItemStack.of((net.minecraft.nbt.CompoundTag) tag);
                }
            } catch (Exception ignored) { }
            decoded.add(st);
        }

        if (isShapeless) {
            int limit = Math.min(decoded.size(), SLOTS);
            for (int i = 0; i < limit; i++) {
                ItemStack st = decoded.get(i);
                if (st == null || st.isEmpty()) continue;
                int x = i % GRID;
                int y = i / GRID;
                int cmIndex = x + y * GRID;
                craftingMatrix.insertItem(cmIndex, st.copy(), false);
                var key = appeng.api.stacks.AEItemKey.of(st);
                if (key != null) {
                    encodedInputsInv.setStack(cmIndex, new appeng.api.stacks.GenericStack(key, st.getCount()));
                }
            }
        } else {
            int startX = (GRID - w) / 2;
            int startY = (GRID - h) / 2;
            int idx = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (idx >= decoded.size()) break;
                    ItemStack st = decoded.get(idx++);
                    if (st != null && !st.isEmpty()) {
                        int cmIndex = (startX + x) + (startY + y) * GRID;
                        if (cmIndex >= 0 && cmIndex < SLOTS) {
                            craftingMatrix.insertItem(cmIndex, st.copy(), false);
                            var key = appeng.api.stacks.AEItemKey.of(st);
                            if (key != null) {
                                encodedInputsInv.setStack(cmIndex, new appeng.api.stacks.GenericStack(key, st.getCount()));
                            }
                        }
                    }
                }
            }
        }

        var encodingLogic = ((com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost) getHost()).getLogic();
        encodingLogic.fixExtremeCraftingGrid();
    }

    public void requestApplyJeiGrid(int w, int h, java.util.List<ItemStack> items) {
        var list = new java.util.ArrayList<JeiItem>(items.size());
        for (var st : items) {
            if (st == null || st.isEmpty()) { list.add(new JeiItem("{}")); continue; }
            var tag = new net.minecraft.nbt.CompoundTag();
            st.save(tag);
            list.add(new JeiItem(tag.toString()));
        }
        sendClientAction("jei_apply_grid", new JeiGridData(w, h, list));
    }

    private void loadCraftingMatrixFromLogic() {
        try {
            for (int i = 0; i < SLOTS; i++) {
                var gs = encodedInputsInv.getStack(i);
                if (gs == null || !appeng.api.stacks.AEItemKey.is(gs.what())) {
                    craftingMatrix.insertItem(i, ItemStack.EMPTY, false);
                    continue;
                }
                var key = (appeng.api.stacks.AEItemKey) gs.what();
                int amount = (int) Math.max(1, Math.min(gs.amount(), 64));
                ItemStack st = key.toStack(amount);
                craftingMatrix.insertItem(i, st, false);
            }
        } catch (Exception e) {
        }
    }
    

    @Override
    public List<ItemStack> getViewCells() {
        return List.of();
    }

    @Override
    public appeng.api.networking.IGridNode getNetworkNode() {
        if (getTarget() instanceof appeng.api.parts.IPart part) {
            return part.getGridNode();
        }
        return null;
    }

    public void extremeEncodePattern() {
        if (isClientSide()) {
            sendClientAction("extremeEncodePattern");
            return;
        }
        encode();
    }

    public void extremeClearPattern() {
        if (isClientSide()) {
            sendClientAction("extremeClearPattern");
            return;
        }
        clearAll();
    }

    public void extremeCraftingClearPattern() {
        if (isClientSide()) {
            sendClientAction("extremeCraftingClearPattern");
            return;
        }
        clearAll();
    }
    
    @Override
    public void onChangeInventory(appeng.api.inventories.InternalInventory inv, int slot) {
        if (inv == craftingMatrix) {
        }
    }
    
    @Override
    public void saveChanges() {
        if (craftingMatrix != null) {
        }
    }

}