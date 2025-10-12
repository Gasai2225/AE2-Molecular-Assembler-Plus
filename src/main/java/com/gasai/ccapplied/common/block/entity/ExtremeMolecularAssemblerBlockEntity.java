package com.gasai.ccapplied.common.block.entity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.util.AECableType;
import appeng.capabilities.Capabilities;
import com.gasai.ccapplied.common.core.ExtremePatternDetails;
import com.gasai.ccapplied.core.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Queue;

public class ExtremeMolecularAssemblerBlockEntity extends BlockEntity
        implements ICraftingMachine, IGridTickable, MenuProvider, IInWorldGridNodeHost {

    public static net.minecraft.world.level.block.entity.BlockEntityType<ExtremeMolecularAssemblerBlockEntity> TYPE;

    public int getCurrentTotalTicks() { return currentTotalTicks; }
    public int getCurrentTicksLeft()  { return currentTicksLeft; }

    private static final IGridNodeListener<ExtremeMolecularAssemblerBlockEntity> LISTENER =
            new IGridNodeListener<>() {
                @Override
                public void onSaveChanges(ExtremeMolecularAssemblerBlockEntity owner, IGridNode node) {
                    if (owner != null) owner.setChanged();
                }
                @Override
                public void onStateChanged(ExtremeMolecularAssemblerBlockEntity owner, IGridNode node, State state) {
                    if (owner == null || owner.level == null || owner.level.isClientSide) return;

                    boolean newPowered = false;
                    var grid = node.getGrid();
                    if (grid != null && node.isPowered()) {
                        newPowered = grid.getEnergyService().extractAEPower(
                                1, appeng.api.config.Actionable.SIMULATE, appeng.api.config.PowerMultiplier.CONFIG
                        ) > 0.0001;
                    }

                    if (newPowered != owner.isPowered) {
                        owner.isPowered = newPowered;
                        var st = owner.level.getBlockState(owner.worldPosition);
                        if (st.hasProperty(com.gasai.ccapplied.common.block.ExtremeMolecularAssemblerBlock.POWERED)) {
                            owner.level.setBlock(owner.worldPosition,
                                    st.setValue(com.gasai.ccapplied.common.block.ExtremeMolecularAssemblerBlock.POWERED, newPowered),
                                    3);
                        }
                    }
                }
            };

    private final IManagedGridNode node = GridHelper.createManagedNode(this, LISTENER);

    /* ======================= Inventories ======================= */
    private final ItemStackHandler gridInv = new ItemStackHandler(81) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
    };

    private final ItemStackHandler outputInv = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public ItemStackHandler getOutputInv() { return outputInv; }

    private final ItemStackHandler patternInv = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() == ModItems.EXTREME_PATTERN.get();
        }
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            recalcCurrentPattern();
            if (getStackInSlot(0).isEmpty()) {
                cancelAllAndRefund();
            }
        }
    };

    // кэш плана
    private ItemStack lastPatternStack = ItemStack.EMPTY;
    private ExtremePatternDetails currentPattern = null;

    private ItemStack addToOutputBuffer(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        return ItemHandlerHelper.insertItem(outputInv, stack, false);
    }

    private void recalcCurrentPattern() {
        var st = patternInv.getStackInSlot(0);
        if (ItemStack.isSameItemSameTags(st, lastPatternStack)) return;

        lastPatternStack = st.copy();
        currentPattern = null;

        if (!st.isEmpty() && level != null) {
            var d = appeng.api.crafting.PatternDetailsHelper.decodePattern(st, level, false);
            if (d instanceof com.gasai.ccapplied.common.core.ExtremePatternDetails epd) {
                currentPattern = epd;
            }
        }
    }

    public ExtremePatternDetails getCurrentPattern() {
        if (currentPattern == null) recalcCurrentPattern();
        return currentPattern;
    }
    public ExtremePatternDetails getCurrentPatternDetails() { return getCurrentPattern(); }

    private final int[] enabledMask = new int[3];

    public int getEnabledMaskPart(int i) {
        return (i >= 0 && i < 3) ? enabledMask[i] : 0;
    }

    private void updateEnabledMask() {
        enabledMask[0] = enabledMask[1] = enabledMask[2] = 0;
        ExtremePatternDetails plan = getCurrentPattern();
        if (plan == null) return;

        for (int i = 0; i < 81; i++) {
            if (plan.isSlotEnabled(i)) {
                int part = i / 27;
                int bit  = i % 27;
                enabledMask[part] |= (1 << bit);
            }
        }
    }
    // 0..4 — апгрейды
    private final ItemStackHandler upgrades = new ItemStackHandler(5) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) { return isSpeedCard(stack); }
        @Override
        public int getSlotLimit(int slot) { return 1; }
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    public boolean isItemValidForGrid(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= 81 || level == null) return false;
        var epd = getCurrentPatternDetails();
        if (epd == null) return false;
        var key = AEItemKey.of(stack);
        return key != null && epd.isSlotEnabled(slot) && epd.isValidForSlot(slot, key, level);
    }

    private static boolean isSpeedCard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return false;
        var ns = id.getNamespace();
        var path = id.getPath();
        return path.equals("speed_card") && (ns.equals("appliedenergistics2") || ns.equals("ae2"));
    }

    /* ======================= Job Queue & Progress ======================= */

    private int currentTotalTicks = 0;
    private int currentTicksLeft = 0;

    private static final class Task {
        final GenericStack[] outputs;
        final ItemStack[] reservedInputs;
        final Direction eject;
        int ticks;
        Task(GenericStack[] outputs, ItemStack[] reservedInputs, Direction eject, int time) {
            this.outputs = outputs;
            this.reservedInputs = reservedInputs;
            this.eject = eject;
            this.ticks = Math.max(1, time);
        }
    }

    private final Queue<Task> queue = new ArrayDeque<>();

    public ExtremeMolecularAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        node.setVisualRepresentation(state.getBlock());
        node.setExposedOnSides(EnumSet.allOf(Direction.class));
        node.setIdlePowerUsage(15.0);
        node.setInWorldNode(true);
        node.addService(IGridTickable.class, this);
    }

    /* ======================= Lifecycle ======================= */

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide && node.getNode() == null) {
            node.create(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        node.destroy();
    }

    /* ======================= IInWorldGridNodeHost ======================= */

    @Override public IGridNode getGridNode(Direction dir) { return node.getNode(); }
    @Override public AECableType getCableConnectionType(Direction dir) { return AECableType.COVERED; }

    /* ======================= MenuProvider (GUI) ======================= */

    @Override public Component getDisplayName() { return Component.literal("Extreme Molecular Assembler"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new com.gasai.ccapplied.client.gui.menu.ExtremeMolecularAssemblerMenu(id, inv, this);
    }

    /* ======================= ICraftingMachine ======================= */

    @Override public PatternContainerGroup getCraftingMachineInfo() { return PatternContainerGroup.nothing(); }

    @Override
    public boolean pushPattern(IPatternDetails pd, KeyCounter[] table, Direction where) {
        if (!(pd instanceof ExtremePatternDetails epd)) return false;

        // Визуально заполняем сетку из раскладки 9×9 по 1 предмету в ячейку.
        for (int i = 0; i < gridInv.getSlots(); i++) gridInv.setStackInSlot(i, ItemStack.EMPTY);
        var layout = epd.getLayout81();
        var reserved = new ArrayList<ItemStack>(layout.length);
        for (int i = 0; i < Math.min(layout.length, 81); i++) {
            var is = layout[i];
            if (is != null && !is.isEmpty()) {
                ItemStack one = is.copy(); one.setCount(1);
                gridInv.setStackInSlot(i, one.copy());
                reserved.add(one.copy());
            }
        }

        int time = Math.max(1, epd.getCraftTime());
        if (queue.isEmpty()) {
            currentTotalTicks = time;
            currentTicksLeft  = time;
        }
        queue.add(new Task(epd.getOutputs(), reserved.toArray(ItemStack[]::new), where, time));
        setChanged();
        wakeIfPossible();
        return true;
    }

    private double getSpeedMultiplier() {
        return switch (getInstalledSpeedCards()) {
            case 0 -> 1.0;
            case 1 -> 1.3;
            case 2 -> 1.7;
            case 3 -> 2.0;
            case 4 -> 2.5;
            default -> 5.0;
        };
    }

    private int getInstalledSpeedCards() {
        int count = 0;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (isSpeedCard(upgrades.getStackInSlot(i))) count++;
        }
        return count;
    }

    private void wakeIfPossible() {
        var n = node.getNode();
        if (n != null && n.getGrid() != null) {
            n.getGrid().getTickManager().wakeDevice(n);
        }
    }

    private boolean isPowered = false;

    @Override
    public boolean acceptsPlans() {
        boolean gridEmpty = true;
        for (int i = 0; i < gridInv.getSlots(); i++) {
            if (!gridInv.getStackInSlot(i).isEmpty()) { gridEmpty = false; break; }
        }
        var it = patternInv.getStackInSlot(0);
        boolean patternOk = it.isEmpty() || it.getItem() == ModItems.EXTREME_PATTERN.get();
        return patternOk && gridEmpty && queue.isEmpty();
    }

    /* ======================= IGridTickable ======================= */

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        boolean hasWork = !queue.isEmpty() || canStartManualJob();
        return new TickingRequest(1, 40, !hasWork, false);
    }

    private boolean canStartManualJob() {
        var plan = getCurrentPatternDetails();
        if (plan == null) return false;

        for (int slot = 0; slot < 81; slot++) {
            if (!plan.isSlotEnabled(slot)) continue;
            var st  = gridInv.getStackInSlot(slot);
            var key = AEItemKey.of(st);
            if (st.isEmpty() || key == null || !plan.isValidForSlot(slot, key, getLevel())) {
                return false;
            }
        }
        return true;
    }

    private void tryStartManualJob() {
        var plan = getCurrentPatternDetails();
        if (plan == null || !canStartManualJob()) return;

        var reserved = new java.util.ArrayList<ItemStack>(81);
        for (int slot = 0; slot < 81; slot++) {
            if (!plan.isSlotEnabled(slot)) continue;
            var st = gridInv.getStackInSlot(slot);
            var one = st.split(1);
            reserved.add(one);
            gridInv.setStackInSlot(slot, st.isEmpty() ? ItemStack.EMPTY : st);
        }
        setChanged();

        int time = Math.max(1, plan.getCraftTime());
        queue.add(new Task(plan.getOutputs(), reserved.toArray(ItemStack[]::new), null, time));
        wakeIfPossible();
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode gridNode, int ticksSinceLastCall) {
        if (getLevel() == null || getLevel().isClientSide) {
            return TickRateModulation.SAME;
        }

        Task t = queue.peek();

        if (t == null) {
            tryStartManualJob();
            t = queue.peek();

            if (t == null) {
                currentTotalTicks = 0;
                currentTicksLeft  = 0;
                return TickRateModulation.SLEEP;
            }

            currentTotalTicks = Math.max(1, t.ticks);
            currentTicksLeft  = t.ticks;
            return TickRateModulation.SAME;
        }

        int delta = Math.max(1, ticksSinceLastCall);
        int work  = (int) Math.ceil(delta * getSpeedMultiplier());
        t.ticks -= work;
        if (t.ticks < 0) t.ticks = 0;
        currentTicksLeft = t.ticks;

        if (t.ticks > 0) {
            return TickRateModulation.SAME;
        }

        pushOutputsOrBuffer(t.outputs, t.eject);

        for (int i = 0; i < gridInv.getSlots(); i++) {
            gridInv.setStackInSlot(i, ItemStack.EMPTY);
        }
        queue.poll();

        if (!queue.isEmpty()) {
            Task nxt = queue.peek();
            currentTotalTicks = Math.max(1, nxt.ticks);
            currentTicksLeft  = nxt.ticks;
            setChanged();
            return TickRateModulation.SAME;
        } else {
            currentTotalTicks = 0;
            currentTicksLeft  = 0;
            setChanged();
            return TickRateModulation.SLEEP;
        }
    }

    /* ======================= Выдача результата ======================= */

    private void pushOutputsOrBuffer(GenericStack[] outputs, Direction dir) {
        if (outputs == null || outputs.length == 0) return;

        for (GenericStack gs : outputs) {
            if (!(gs.what() instanceof AEItemKey itemKey)) continue;

            ItemStack todo = itemKey.toStack((int) gs.amount());
            if (todo.isEmpty()) continue;

            todo = tryPushToNeighbor(todo, dir);
            if (!todo.isEmpty()) todo = addToOutputBuffer(todo);
            if (!todo.isEmpty()) {
                todo = tryPushToNeighbor(todo, null);
                if (!todo.isEmpty()) addToOutputBuffer(todo);
            }
        }

        setChanged();
    }

    private ItemStack tryPushToNeighbor(ItemStack stack, Direction preferred) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        if (preferred != null) {
            stack = pushToSide(stack, preferred);
            if (stack.isEmpty()) return ItemStack.EMPTY;
        }

        for (Direction d : Direction.values()) {
            stack = pushToSide(stack, d);
            if (stack.isEmpty()) break;
        }
        return stack;
    }

    private ItemStack pushToSide(ItemStack stack, Direction side) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        BlockPos targetPos = worldPosition.relative(side);
        BlockEntity be = getLevel().getBlockEntity(targetPos);
        if (be == null) return stack;

        var cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side.getOpposite());
        if (!cap.isPresent()) return stack;

        IItemHandler inv = cap.orElse(null);
        if (inv == null) return stack;

        return ItemHandlerHelper.insertItem(inv, stack, false);
    }

    /* ======================= Cancel & Refund ======================= */

    private void cancelAllAndRefund() {
        while (!queue.isEmpty()) {
            Task t = queue.poll();
            if (t != null && t.reservedInputs != null) {
                refundInputs(t.reservedInputs);
            }
        }
        for (int i = 0; i < gridInv.getSlots(); i++) gridInv.setStackInSlot(i, ItemStack.EMPTY);
        currentTotalTicks = 0;
        currentTicksLeft = 0;
        setChanged();
    }

    private void refundInputs(ItemStack[] inputs) {
        if (inputs == null || inputs.length == 0) return;

        // сначала к соседям, затем — в буфер вывода, без дропов
        for (var st : inputs) {
            if (st == null || st.isEmpty()) continue;
            ItemStack rest = tryPushToNeighbor(st.copy(), null);
            if (!rest.isEmpty()) {
                addToOutputBuffer(rest);
            }
        }
    }

    /* ======================= Save / Load ======================= */

    @Override protected void saveAdditional(CompoundTag tag) {
        tag.put("GridInv", gridInv.serializeNBT());
        tag.put("Pattern", patternInv.serializeNBT());
        tag.put("Upgrades", upgrades.serializeNBT());
        tag.put("Output",  outputInv.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("GridInv")) gridInv.deserializeNBT(tag.getCompound("GridInv"));
        if (tag.contains("Pattern")) patternInv.deserializeNBT(tag.getCompound("Pattern"));
        if (tag.contains("Upgrades")) upgrades.deserializeNBT(tag.getCompound("Upgrades"));
        if (tag.contains("Output"))  outputInv.deserializeNBT(tag.getCompound("Output"));
    }

    /* ======================= Exposed getters ======================= */

    public ItemStackHandler getGridInv() { return gridInv; }
    public ItemStackHandler getPatternInv() { return patternInv; }
    public ItemStackHandler getUpgradesInv() { return upgrades; }

    public int getCraftingProgress() {
        if (currentTotalTicks <= 0) return 0;
        int done = currentTotalTicks - Math.max(0, currentTicksLeft);
        int pct = (int) (done * 100L / currentTotalTicks);
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        return pct;
    }

    /* ======================= Capabilities ======================= */

    private final LazyOptional<ICraftingMachine> craftingMachineCap = LazyOptional.of(() -> this);
    private final LazyOptional<IInWorldGridNodeHost> hostCap = LazyOptional.of(() -> this);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
        if (capability == Capabilities.CRAFTING_MACHINE) {
            return Capabilities.CRAFTING_MACHINE.orEmpty(capability, craftingMachineCap);
        }
        if (capability == Capabilities.IN_WORLD_GRID_NODE_HOST) {
            return Capabilities.IN_WORLD_GRID_NODE_HOST.orEmpty(capability, hostCap);
        }
        return super.getCapability(capability, facing);
    }

    /* ======================= Fallback tick (вне сети) ======================= */

    public void serverTick() {
        var n = node.getNode();
        if (n != null && n.getGrid() != null) return;

        if (queue.peek() == null) {
            tryStartManualJob();
            if (queue.peek() == null) {
                currentTotalTicks = 0;
                currentTicksLeft  = 0;
                return;
            }
            currentTotalTicks = Math.max(1, queue.peek().ticks);
            currentTicksLeft  = queue.peek().ticks;
        }

        Task t = queue.peek();
        int work = (int) Math.ceil(getSpeedMultiplier());
        t.ticks -= work;
        if (t.ticks < 0) t.ticks = 0;
        currentTicksLeft = t.ticks;

        if (t.ticks > 0) return;

        pushOutputsOrBuffer(t.outputs, t.eject);

        for (int i = 0; i < gridInv.getSlots(); i++) gridInv.setStackInSlot(i, ItemStack.EMPTY);

        queue.poll();
        if (!queue.isEmpty()) {
            Task nxt = queue.peek();
            currentTotalTicks = Math.max(1, nxt.ticks);
            currentTicksLeft  = nxt.ticks;
        } else {
            currentTotalTicks = 0;
            currentTicksLeft  = 0;
        }

        setChanged();
    }
}
