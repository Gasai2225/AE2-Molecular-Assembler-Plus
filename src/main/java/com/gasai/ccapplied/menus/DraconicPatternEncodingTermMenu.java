package com.gasai.ccapplied.menus;

import appeng.api.inventories.InternalInventory;
import appeng.menu.guisync.GuiSync;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;
import com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper;
import com.gasai.ccapplied.crafting.DraconicFusionRecipeMatch;
import com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost;
import com.gasai.ccapplied.parts.ExtremePatternEncodingLogic;
import com.gasai.ccapplied.patterns.DraconicFusionPattern;
import com.gasai.ccapplied.slots.DraconicBlankPatternSlot;
import com.gasai.ccapplied.slots.DraconicEncodedPatternSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DraconicPatternEncodingTermMenu extends MEStorageMenu {
    public static final int OUTER_SLOTS = 12;
    public static final int TOTAL_SLOTS = 13;
    public static final int CENTER_SLOT = 12;

    public static final String ACTION_ENCODE = "draconic_encode";
    public static final String ACTION_CLEAR = "draconic_clear";
    public static final appeng.menu.SlotSemantic[] OUTER_SLOT_SEMANTICS = {
            SlotSemantics.register("DRACONIC_SLOT_0", false),
            SlotSemantics.register("DRACONIC_SLOT_1", false),
            SlotSemantics.register("DRACONIC_SLOT_2", false),
            SlotSemantics.register("DRACONIC_SLOT_3", false),
            SlotSemantics.register("DRACONIC_SLOT_4", false),
            SlotSemantics.register("DRACONIC_SLOT_5", false),
            SlotSemantics.register("DRACONIC_SLOT_6", false),
            SlotSemantics.register("DRACONIC_SLOT_7", false),
            SlotSemantics.register("DRACONIC_SLOT_8", false),
            SlotSemantics.register("DRACONIC_SLOT_9", false),
            SlotSemantics.register("DRACONIC_SLOT_10", false),
            SlotSemantics.register("DRACONIC_SLOT_11", false)
    };
    public static final appeng.menu.SlotSemantic CATALYST_SLOT_SEMANTIC = SlotSemantics.register("DRACONIC_CATALYST", false);

    public static final MenuType<DraconicPatternEncodingTermMenu> TYPE = MenuTypeBuilder.create(
            DraconicPatternEncodingTermMenu::new,
            IExtremePatternTerminalMenuHost.class).buildUnregistered(com.gasai.ccapplied.CCApplied.makeId("draconic_patternterm"));

    private final ExtremePatternEncodingLogic logic;
    private final InternalInventory craftingMatrix;
    private final appeng.util.inv.AppEngInternalInventory resultPreviewInv = new appeng.util.inv.AppEngInternalInventory(null, 1);
    private final FakeSlot[] inputSlots = new FakeSlot[OUTER_SLOTS];
    private final FakeSlot catalystSlot;
    private final PreviewOnlySlot resultPreviewSlot;
    private final Slot blankPatternSlot;
    private final Slot encodedPatternSlot;

    @GuiSync(40)
    public int uiTierOrdinal = -1;
    @GuiSync(41)
    public long uiEnergyCost = 0L;

    public DraconicPatternEncodingTermMenu(int id, Inventory inv, IExtremePatternTerminalMenuHost host) {
        this(TYPE, id, inv, host, true);
    }

    public DraconicPatternEncodingTermMenu(MenuType<?> type, int id, Inventory inv, IExtremePatternTerminalMenuHost host, boolean bindInv) {
        super(type, id, inv, host, bindInv);
        this.logic = host.getLogic();
        var inputsWrapper = logic.getEncodedInputInv().createMenuWrapper();
        this.craftingMatrix = inputsWrapper.getSubInventory(0, TOTAL_SLOTS);

        for (int i = 0; i < OUTER_SLOTS; i++) {
            var slot = new FakeSlot(inputsWrapper, i);
            slot.setHideAmount(true);
            inputSlots[i] = slot;
            this.addSlot(slot, OUTER_SLOT_SEMANTICS[i]);
        }
        this.catalystSlot = new FakeSlot(inputsWrapper, CENTER_SLOT);
        this.catalystSlot.setHideAmount(false);
        this.addSlot(this.catalystSlot, CATALYST_SLOT_SEMANTIC);
        this.resultPreviewSlot = new PreviewOnlySlot(resultPreviewInv, 0);
        this.resultPreviewSlot.setHideAmount(false);
        this.addSlot(this.resultPreviewSlot, SlotSemantics.CRAFTING_RESULT);

        this.blankPatternSlot = this.addSlot(new DraconicBlankPatternSlot(logic.getBlankPatternInv(), 0), SlotSemantics.BLANK_PATTERN);
        this.encodedPatternSlot = this.addSlot(new DraconicEncodedPatternSlot(logic.getEncodedPatternInv(), 0), SlotSemantics.ENCODED_PATTERN);

        registerClientAction(ACTION_ENCODE, this::encode);
        registerClientAction(ACTION_CLEAR, this::clearAll);

        if (!isClientSide()) {
            logic.loadDraconicMatrixInto(craftingMatrix);
            findFusionRecipe();
        }
    }

    public void encode() {
        if (isClientSide()) {
            sendClientAction(ACTION_ENCODE);
            return;
        }
        var match = findFusionRecipe();
        if (match == null) {
            return;
        }

        var inputs = new appeng.api.stacks.GenericStack[DraconicFusionPattern.TOTAL_INPUT_SLOTS];
        int idx = 0;
        for (int i = 0; i < OUTER_SLOTS; i++) {
            var stack = craftingMatrix.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            var key = appeng.api.stacks.AEItemKey.of(stack);
            if (key != null && idx < OUTER_SLOTS) {
                inputs[idx++] = new appeng.api.stacks.GenericStack(key, Math.max(1, stack.getCount()));
            }
        }
        var center = craftingMatrix.getStackInSlot(CENTER_SLOT);
        var centerKey = appeng.api.stacks.AEItemKey.of(center);
        var outKey = appeng.api.stacks.AEItemKey.of(match.result());
        if (centerKey == null || outKey == null) {
            return;
        }
        inputs[OUTER_SLOTS] = new appeng.api.stacks.GenericStack(centerKey, Math.max(1, center.getCount()));
        var output = new appeng.api.stacks.GenericStack(outKey, Math.max(1, match.result().getCount()));
        logic.encodeDraconicPattern(inputs, output, match.tier(), match.totalEnergy(), match.recipeId());
    }

    @Override
    public void setItem(int slotID, int stateId, ItemStack stack) {
        super.setItem(slotID, stateId, stack);
        findFusionRecipe();
    }

    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        super.initializeContents(stateId, items, carried);
        findFusionRecipe();
    }

    public void clearAll() {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR);
            return;
        }
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            setFilterSlot(i, ItemStack.EMPTY);
        }
        clearRecipePreview();
        logic.setRecipeId(null);
        logic.clearCraftingGrid();
    }

    public boolean canEncode() {
        if (blankPatternSlot.getItem().isEmpty() && encodedPatternSlot.getItem().isEmpty()) {
            return false;
        }
        return findFusionRecipe() != null;
    }

    private DraconicFusionRecipeMatch findFusionRecipe() {
        if (getPlayer() == null || getPlayer().level() == null) {
            clearRecipePreview();
            return null;
        }
        var center = craftingMatrix.getStackInSlot(CENTER_SLOT);
        if (center.isEmpty()) {
            clearRecipePreview();
            if (!isClientSide()) {
                logic.setRecipeId(null);
                logic.saveDraconicMatrix(craftingMatrix, ItemStack.EMPTY);
            }
            return null;
        }
        List<ItemStack> outer = new ArrayList<>();
        for (int i = 0; i < OUTER_SLOTS; i++) {
            var st = craftingMatrix.getStackInSlot(i);
            if (!st.isEmpty()) {
                outer.add(st.copy());
            }
        }
        var match = DraconicFusionRecipeHelper.findRecipe(outer, center, getPlayer().level());
        if (match != null) {
            uiTierOrdinal = match.tier().ordinal();
            uiEnergyCost = Math.max(0L, match.totalEnergy());
            resultPreviewInv.setItemDirect(0, match.result().copy());
            if (!isClientSide()) {
                logic.setRecipeId(match.recipeId());
                logic.saveDraconicMatrix(craftingMatrix, match.result());
            }
        } else {
            clearRecipePreview();
            if (!isClientSide()) {
                logic.setRecipeId(null);
                logic.saveDraconicMatrix(craftingMatrix, ItemStack.EMPTY);
            }
        }
        return match;
    }

    private void clearRecipePreview() {
        uiTierOrdinal = -1;
        uiEnergyCost = 0L;
        resultPreviewInv.setItemDirect(0, ItemStack.EMPTY);
    }

    public void onSlotChange(Slot slot) {
        if (!isClientSide()) {
            if (slot == encodedPatternSlot) {
                var patternStack = slot.getItem();
                if (!patternStack.isEmpty()) {
                    logic.loadDraconicPatternIntoMatrix(craftingMatrix, patternStack);
                    broadcastChanges();
                }
            }
            findFusionRecipe();
        }
    }

    public String getTierText() {
        return switch (uiTierOrdinal) {
            case 2 -> "Chaotic";
            case 1 -> "Draconic";
            case 0 -> "Wyvern";
            default -> "";
        };
    }

    public int getTierOrdinal() {
        return uiTierOrdinal;
    }

    public long getEnergyCost() {
        return uiEnergyCost;
    }

    public void requestApplyJeiFusion(ItemStack catalyst, List<ItemStack> outer) {
        sendFusionFilters(catalyst, outer);
    }

    private void applyJeiFusionRecipe(JeiFusionData data) {
        List<ItemStack> outer = new ArrayList<>(data.outer.size());
        for (var item : data.outer) {
            outer.add(fromSnbt(item.snbt));
        }
        applyJeiFusionStacks(fromSnbt(data.catalyst), outer, true);
    }

    private void applyJeiFusionStacks(ItemStack catalyst, List<ItemStack> outer, boolean updateRecipe) {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            setFilterSlot(i, ItemStack.EMPTY);
        }

        int limit = Math.min(OUTER_SLOTS, outer.size());
        for (int i = 0; i < limit; i++) {
            var stack = outer.get(i);
            setFilterSlot(i, stack == null ? ItemStack.EMPTY : stack.copy());
        }
        if (catalyst != null) {
            setFilterSlot(CENTER_SLOT, catalyst.copy());
        }
        if (updateRecipe) {
            findFusionRecipe();
            broadcastChanges();
        }
    }

    private String toSnbt(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "{}";
        }
        var tag = new net.minecraft.nbt.CompoundTag();
        stack.save(getPlayer().registryAccess(), tag);
        return tag.toString();
    }

    private ItemStack fromSnbt(String snbt) {
        try {
            return ItemStack.parseOptional(getPlayer().registryAccess(), net.minecraft.nbt.TagParser.parseTag(snbt));
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    private void setFilterSlot(int slot, ItemStack stack) {
        craftingMatrix.extractItem(slot, Integer.MAX_VALUE, false);
        if (stack != null && !stack.isEmpty()) {
            craftingMatrix.insertItem(slot, stack.copy(), false);
        }
    }

    private void sendFusionFilters(ItemStack catalyst, List<ItemStack> outer) {
        for (int i = 0; i < OUTER_SLOTS; i++) {
            sendSetFilter(inputSlots[i], ItemStack.EMPTY);
        }
        sendSetFilter(catalystSlot, ItemStack.EMPTY);

        int limit = Math.min(OUTER_SLOTS, outer.size());
        for (int i = 0; i < limit; i++) {
            var stack = outer.get(i);
            if (stack != null && !stack.isEmpty()) {
                sendSetFilter(inputSlots[i], stack.copy());
            }
        }
        if (catalyst != null && !catalyst.isEmpty()) {
            sendSetFilter(catalystSlot, catalyst.copy());
        }
    }

    private static void sendSetFilter(FakeSlot slot, ItemStack stack) {
        appeng.core.network.ServerboundPacket message = new appeng.core.network.serverbound.InventoryActionPacket(
                appeng.helpers.InventoryAction.SET_FILTER, slot.index, stack);
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(message);
    }

    public static class JeiFusionData {
        private static final com.google.gson.Gson GSON = new com.google.gson.Gson();
        public final String catalyst;
        public final List<JeiItem> outer;

        public JeiFusionData(String catalyst, List<JeiItem> outer) {
            this.catalyst = catalyst;
            this.outer = outer;
        }

        public String toPayload() {
            return GSON.toJson(this);
        }

        public static JeiFusionData fromPayload(String payload) {
            try {
                return GSON.fromJson(payload, JeiFusionData.class);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    public static class JeiItem {
        public final String snbt;

        public JeiItem(String snbt) {
            this.snbt = snbt;
        }
    }

    private static class PreviewOnlySlot extends AppEngSlot {
        public PreviewOnlySlot(InternalInventory inv, int idx) {
            super(inv, idx);
            setNotDraggable();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
            return false;
        }

    }
}
