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
import com.gasai.ccapplied.client.gui.menu.ExtremeMolecularAssemblerMenu;
import com.gasai.ccapplied.common.core.ExtremePatternDetails;
import com.gasai.ccapplied.core.registry.ModBlockEntities;
import com.gasai.ccapplied.core.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

    /* ======================= AE2 Integration ======================= */

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
                        // как в MA: симулируем маленький дрен
                        newPowered = grid.getEnergyService().extractAEPower(
                                1, appeng.api.config.Actionable.SIMULATE, appeng.api.config.PowerMultiplier.CONFIG
                        ) > 0.0001;
                    }

                    if (newPowered != owner.isPowered) {
                        owner.isPowered = newPowered;
                        // обновим блокстейт (POWERED)
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
    // 0..80 — входная 9×9 сетка (визуально/остатки)
    private final ItemStackHandler gridInv = new ItemStackHandler(81) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    // 0 — слот шаблона (наш extreme pattern)
    private final ItemStackHandler patternInv = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() == ModItems.EXTREME_PATTERN.get();
        }
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // убрали шаблон — отменяем все активные задачи и рефандим входы
            if (getStackInSlot(0).isEmpty()) {
                cancelAllAndRefund();
            }
        }
    };

    // 0..4 — слоты апгрейдов (Speed Card)
    private final ItemStackHandler upgrades = new ItemStackHandler(5) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isSpeedCard(stack);
        }
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    private static boolean isSpeedCard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return false;
        var ns = id.getNamespace();
        var path = id.getPath();
        return path.equals("speed_card") && (ns.equals("appliedenergistics2") || ns.equals("ae2"));
    }

    /* ======================= Job Queue & Progress ======================= */

    // прогресс текущей головы очереди
    private int currentTotalTicks = 0;
    private int currentTicksLeft = 0;

    private static final class Task {
        final GenericStack[] outputs;
        final ItemStack[] reservedInputs; // на случай отмены — возвращаем
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
        super(ModBlockEntities.EXTREME_ASSEMBLER_BE.get(), pos, state);

        // Конфиг ноды — ДО create()
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

    @Override
    public IGridNode getGridNode(Direction dir) {
        return node.getNode();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.COVERED;
    }

    /* ======================= MenuProvider (GUI) ======================= */

    @Override
    public Component getDisplayName() {
        return Component.literal("Extreme Molecular Assembler");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ExtremeMolecularAssemblerMenu(id, inv, this);
    }

    /* ======================= ICraftingMachine ======================= */

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return PatternContainerGroup.nothing();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] table, Direction where) {
        if (!(patternDetails instanceof ExtremePatternDetails epd)) return false;

        // Визуальная раскладка + сбор входов для возможного рефанда
        ItemStack[] reserved = fillGridFromKeyCountersAndCollect(table);

        // Время с учётом ускорителей (простая шкала): base / (1 + cards)
        int time = Math.max(1, epd.getCraftTime() / Math.max(1, 1 + getInstalledSpeedCards()));

        // если это первая задача — устанавливаем базовый прогресс
        if (queue.isEmpty()) {
            currentTotalTicks = time;
            currentTicksLeft = time;
        }

        queue.add(new Task(epd.getOutputs(), reserved, where, time));
        setChanged();
        wakeIfPossible();
        return true;
    }

    private ItemStack[] fillGridFromKeyCountersAndCollect(KeyCounter[] table) {
        // Очистим визуальную сетку
        for (int i = 0; i < gridInv.getSlots(); i++) gridInv.setStackInSlot(i, ItemStack.EMPTY);

        if (table == null) return new ItemStack[0];

        var reserved = new ArrayList<ItemStack>();

        // Берём ровно по ОДНОМУ предмету с каждого занятого слота (0..80)
        int maxSlots = Math.min(81, table.length);
        for (int slot = 0; slot < maxSlots; slot++) {
            var kc = table[slot];
            if (kc == null || kc.isEmpty()) continue;

            // Выбираем первый доступный ключ (AEItemKey) и берём 1 шт. (для 1 крафта)
            var it = kc.iterator();
            while (it.hasNext()) {
                var entry = it.next();
                var key = entry.getKey();
                if (key instanceof AEItemKey itemKey) {
                    // 1 штука на слот
                    ItemStack one = itemKey.toStack(1);
                    if (!one.isEmpty()) {
                        // визуально положим 1 предмет
                        gridInv.setStackInSlot(slot, one.copy());
                        // и зарезервируем 1 предмет для возможного рефанда
                        reserved.add(one.copy());
                    }
                    break; // НЕ забираем больше из этого KeyCounter — только 1 на слот!
                }
            }
        }

        return reserved.toArray(ItemStack[]::new);
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
        // только если нет задач и сетка пуста
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
    public TickingRequest getTickingRequest(IGridNode gridNode) {
        boolean hasWork = !queue.isEmpty();
        return new TickingRequest(hasWork ? 1 : 20, 40, !hasWork, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode gridNode, int ticksSinceLastCall) {
        if (level == null || level.isClientSide) return TickRateModulation.SAME;

        Task t = queue.peek();
        if (t == null) {
            currentTotalTicks = 0;
            currentTicksLeft = 0;
            return TickRateModulation.SLEEP;
        }

        int delta = Math.max(1, ticksSinceLastCall);
        t.ticks -= delta;

        if (currentTotalTicks <= 0) currentTotalTicks = Math.max(1, t.ticks + delta);
        currentTicksLeft = Math.max(0, t.ticks);

        if (t.ticks > 0) return TickRateModulation.SAME;

        // Выпуск результата
        ejectOutputs(t.outputs, t.eject);

        // Очистим визуальную сетку
        for (int i = 0; i < gridInv.getSlots(); i++) gridInv.setStackInSlot(i, ItemStack.EMPTY);

        // задача выполнена — входы не рефандим
        queue.poll();

        // Переключим прогресс на следующую задачу (если есть)
        if (!queue.isEmpty()) {
            var nxt = queue.peek();
            currentTotalTicks = Math.max(1, nxt.ticks);
            currentTicksLeft = nxt.ticks;
        } else {
            currentTotalTicks = 0;
            currentTicksLeft = 0;
        }

        setChanged();
        return queue.isEmpty() ? TickRateModulation.SLEEP : TickRateModulation.SAME;
    }

    /* ======================= Выдача результата ======================= */

    private void ejectOutputs(GenericStack[] outputs, Direction dir) {
        if (outputs == null || outputs.length == 0) return;
        Direction d = (dir == null) ? Direction.UP : dir;
        BlockPos targetPos = worldPosition.relative(d);
        var be = level.getBlockEntity(targetPos);

        // 1) Пытаемся положить в соседний инвентарь
        if (be != null) {
            var cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, d.getOpposite());
            if (cap.isPresent()) {
                IItemHandler inv = cap.orElse(null);
                if (inv != null) {
                    for (GenericStack gs : outputs) {
                        if (gs.what() instanceof AEItemKey itemKey) {
                            var stack = itemKey.toStack((int) gs.amount());
                            if (stack.isEmpty()) continue;
                            stack = ItemHandlerHelper.insertItem(inv, stack, false);
                            if (!stack.isEmpty()) {
                                Containers.dropItemStack(level, targetPos.getX() + 0.5,
                                        targetPos.getY() + 0.5, targetPos.getZ() + 0.5, stack);
                            }
                        }
                    }
                    return;
                }
            }
        }

        // 2) Иначе — дропаем в мир
        for (GenericStack gs : outputs) {
            if (gs.what() instanceof AEItemKey itemKey) {
                var stack = itemKey.toStack((int) gs.amount());
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level,
                            targetPos.getX() + 0.5,
                            targetPos.getY() + 0.5,
                            targetPos.getZ() + 0.5,
                            stack);
                }
            }
        }
    }

    /* ======================= Cancel & Refund ======================= */

    private void cancelAllAndRefund() {
        // вернём входы всех НЕвыполненных задач
        while (!queue.isEmpty()) {
            Task t = queue.poll();
            if (t != null && t.reservedInputs != null) {
                refundInputs(t.reservedInputs);
            }
        }
        // чистим визуальную сетку
        for (int i = 0; i < gridInv.getSlots(); i++) gridInv.setStackInSlot(i, ItemStack.EMPTY);
        // сброс прогресса
        currentTotalTicks = 0;
        currentTicksLeft = 0;
        setChanged();
    }

    private void refundInputs(ItemStack[] inputs) {
        if (inputs == null || inputs.length == 0) return;
        Direction d = Direction.UP;
        BlockPos targetPos = worldPosition.relative(d);
        var be = level.getBlockEntity(targetPos);

        if (be != null) {
            var cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, d.getOpposite());
            if (cap.isPresent()) {
                var inv = cap.orElse(null);
                if (inv != null) {
                    for (var st : inputs) {
                        if (st.isEmpty()) continue;
                        var rem = ItemHandlerHelper.insertItem(inv, st.copy(), false);
                        if (!rem.isEmpty()) {
                            Containers.dropItemStack(level,
                                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, rem);
                        }
                    }
                    return;
                }
            }
        }
        // иначе — в мир
        for (var st : inputs) {
            if (!st.isEmpty()) {
                Containers.dropItemStack(level,
                        targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, st.copy());
            }
        }
    }

    /* ======================= Save / Load ======================= */

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("GridInv", gridInv.serializeNBT());
        tag.put("Pattern", patternInv.serializeNBT());
        tag.put("Upgrades", upgrades.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("GridInv")) gridInv.deserializeNBT(tag.getCompound("GridInv"));
        if (tag.contains("Pattern")) patternInv.deserializeNBT(tag.getCompound("Pattern"));
        if (tag.contains("Upgrades")) upgrades.deserializeNBT(tag.getCompound("Upgrades"));
    }

    /* ======================= Exposed getters for menu ======================= */

    public ItemStackHandler getGridInv() { return gridInv; }
    public ItemStackHandler getPatternInv() { return patternInv; }
    public ItemStackHandler getUpgradesInv() { return upgrades; }

    // для GUI: 0..100
    public int getCraftingProgress() {
        if (currentTotalTicks <= 0) return 0;
        int done = currentTotalTicks - Math.max(0, currentTicksLeft);
        int pct = (int) (done * 100L / currentTotalTicks);
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        return pct;
    }

    /* ======================= AE2 Capabilities ======================= */

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

        var t = queue.peek();
        if (t == null) return;

        if (--t.ticks <= 0) {
            ejectOutputs(t.outputs, t.eject);
            for (int i = 0; i < gridInv.getSlots(); i++) gridInv.setStackInSlot(i, ItemStack.EMPTY);
            queue.poll();
            setChanged();
        }
    }
}
