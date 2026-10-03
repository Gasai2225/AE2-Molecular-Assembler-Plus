package com.gasai.ccapplied.botania;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.*;
import appeng.api.stacks.*;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.me.helpers.MachineSource;
import appeng.util.inv.AppEngInternalInventory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** One atomic job: retain ingredients until completion, then retain outputs until accepted by ME. */
public final class BotanicalAssemblerBlockEntity extends AENetworkInvBlockEntity
        implements ICraftingMachine, IGridTickable, appeng.api.upgrades.IUpgradeableObject {
    private final appeng.api.upgrades.IUpgradeInventory upgrades;
    private final AppEngInternalInventory inventory = new AppEngInternalInventory(this, 18);
    private ItemStack plan = ItemStack.EMPTY;
    private long reservedMana;
    private final AppEngInternalInventory jobOutputs = new AppEngInternalInventory(null, 18);
    private int jobTicks;
    private int progress;
    private boolean visualPowered;
    private final AppEngInternalInventory template = new AppEngInternalInventory(this, 1, 1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return canChangeTemplate() && decodeTemplate(stack) != null;
        }
    };
    private final AppEngInternalInventory manualInputs = new AppEngInternalInventory(this, 18, 1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return isValidManualInput(slot, stack); }
    };
    private final InternalInventory automation = new appeng.util.inv.FilteredInternalInventory(
            new appeng.util.inv.CombinedInternalInventory(manualInputs, inventory),
            new appeng.util.inv.filter.IAEItemFilter() {
                @Override public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                    return slot < 18 && isValidManualInput(slot, stack);
                }
                @Override public boolean allowExtract(InternalInventory inv, int slot, int amount) {
                    return slot >= 18 && plan.isEmpty();
                }
            });

    public InternalInventory templateInventory() { return template; }
    public InternalInventory manualInventory() { return manualInputs; }
    public boolean canChangeTemplate() { return plan.isEmpty() && inventory.isEmpty() && manualInputs.isEmpty(); }
    public boolean manualEditable() { return plan.isEmpty() && inventory.isEmpty() && configuredPattern() != null; }
    public ItemStack activePattern() { return plan.copy(); }
    public MagicalPattern configuredPattern() { return decodeTemplate(template.getStackInSlot(0)); }
    public MagicalPattern displayedPattern() { return plan.isEmpty() ? configuredPattern() : decodeTemplate(plan); }

    public MagicalPattern decodeTemplate(ItemStack stack) {
        if (getLevel() == null || stack.isEmpty()) return null;
        var pattern = MagicalPatternItem.readPattern(stack, getLevel());
        return pattern != null && pattern.station() == station() ? pattern : null;
    }

    public boolean isValidManualInput(int slot, ItemStack stack) {
        if (!plan.isEmpty() || !inventory.isEmpty() || stack.isEmpty()) return false;
        var pattern = configuredPattern();
        if (pattern == null) return false;
        var ingredients = pattern.ingredients();
        if (slot < 0 || slot >= ingredients.size()) return false;
        return pattern.isItemValid(slot, AEItemKey.of(stack), getLevel());
    }

    public InternalInventory menuInputs() {
        return new appeng.api.inventories.BaseInternalInventory() {
            @Override public int size() { return 18; }
            @Override public int getSlotLimit(int slot) { return 1; }
            @Override public ItemStack getStackInSlot(int slot) {
                return getLevel().isClientSide() || plan.isEmpty()
                        ? manualInputs.getStackInSlot(slot) : inventory.getStackInSlot(slot);
            }
            @Override public void setItemDirect(int slot, ItemStack stack) {
                if (getLevel().isClientSide() || manualEditable()) manualInputs.setItemDirect(slot, stack);
            }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return isValidManualInput(slot, stack); }
        };
    }

    public InternalInventory menuOutputs() {
        return new appeng.api.inventories.BaseInternalInventory() {
            @Override public int size() { return 18; }
            @Override public ItemStack getStackInSlot(int slot) {
                return plan.isEmpty() ? inventory.getStackInSlot(slot) : ItemStack.EMPTY;
            }
            @Override public void setItemDirect(int slot, ItemStack stack) {
                if (plan.isEmpty()) inventory.setItemDirect(slot, stack);
            }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
        };
    }

    private boolean startManualJob() {
        var pattern = configuredPattern();
        if (pattern == null || !plan.isEmpty() || !inventory.isEmpty()) return false;
        var ingredients = pattern.ingredients();
        var actual = new ArrayList<ItemStack>();
        for (int i = 0; i < manualInputs.size(); i++) {
            var stack = manualInputs.getStackInSlot(i);
            if (i >= ingredients.size()) {
                if (!stack.isEmpty()) return false;
            } else {
                if (stack.getCount() != 1 || !isValidManualInput(i, stack)) return false;
                actual.add(stack.copy());
            }
        }
        var recipe = MagicalRecipeResolver.resolve(getLevel(), station(), pattern.catalyst(), pattern.recipe().recipeId(), actual);
        if (!MagicalRecipeResolver.sameResult(pattern.recipe(), recipe) || recipe.outputs().size() > inventory.size()) return false;
        var storage = getMainNode().getGrid().getStorageService().getInventory();
        var source = new MachineSource(this);
        long cost = recipe.mana();
        if (cost > 0) {
            if (storage.extract(ManaKey.INSTANCE, cost, Actionable.SIMULATE, source) != cost) return false;
            long extracted = storage.extract(ManaKey.INSTANCE, cost, Actionable.MODULATE, source);
            if (extracted != cost) {
                // Preserve a partial extraction even if a third-party storage changes after simulation.
                if (extracted > 0) appeng.util.Platform.spawnDrops(getLevel(), getBlockPos(),
                        List.of(ManaCapsuleItem.filled(extracted)));
                return false;
            }
        }
        for (int i = 0; i < actual.size(); i++) inventory.setItemDirect(i, actual.get(i));
        plan = pattern.getDefinition().toStack();
        reservedMana = cost;
        rememberResult(recipe);
        progress = 0;
        manualInputs.clear();
        saveChanges();
        return true;
    }

    public boolean isVisualPowered() { return visualPowered; }

    @Override
    public void onMainNodeStateChanged(appeng.api.networking.IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        boolean powered = getMainNode().isPowered();
        if (visualPowered != powered) {
            visualPowered = powered;
            markForUpdate();
        }
    }

    @Override
    protected void writeToStream(net.minecraft.network.FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(getMainNode().isPowered());
    }

    @Override
    protected boolean readFromStream(net.minecraft.network.FriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        boolean previous = visualPowered;
        visualPowered = data.readBoolean();
        return changed || previous != visualPowered;
    }
    public BotanicalAssemblerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        upgrades = appeng.api.upgrades.UpgradeInventories.forMachine(state.getBlock(), 4, this::saveChanges);
        getMainNode().setIdlePowerUsage(1).addService(IGridTickable.class, this);
    }
    public MagicalStation station() { return ((BotanicalAssemblerBlock) getBlockState().getBlock()).station(); }
    @Override public appeng.api.upgrades.IUpgradeInventory getUpgrades() { return upgrades; }
    public long reservedMana() { return reservedMana; }
    public int status() { return !getMainNode().isActive() ? 0 : !plan.isEmpty() ? 2
            : !inventory.isEmpty() ? 3 : !template.isEmpty() ? 4 : 1; }
    public int craftProgress() {
        var pattern = plan.isEmpty() ? null : MagicalPatternItem.readPattern(plan, getLevel());
        return pattern == null ? 0 : Math.min(100, (int) (100L * progress / pattern.recipe().ticks()));
    }
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, Direction side) {
        if (cap == appeng.capabilities.Capabilities.CRAFTING_MACHINE)
            return appeng.capabilities.Capabilities.CRAFTING_MACHINE.orEmpty(cap,
                    net.minecraftforge.common.util.LazyOptional.of(() -> this));
        return super.getCapability(cap, side);
    }
    @Override public InternalInventory getInternalInventory() { return inventory; }
    @Override protected InternalInventory getExposedInventoryForSide(Direction side) { return automation; }
    @Override public void onChangeInventory(InternalInventory inv, int slot) {
        if (getLevel() != null && !getLevel().isClientSide()) {
            saveChanges();
            getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
        }
    }
    @Override public PatternContainerGroup getCraftingMachineInfo() {
        var item = getBlockState().getBlock().asItem();
        return new PatternContainerGroup(AEItemKey.of(item), item.getDescription(), List.of());
    }
    @Override public boolean acceptsPlans() {
        // Claim dispatch while configured so providers cannot fall back to an untyped item-handler push.
        return !template.isEmpty() || (manualInputs.isEmpty() && plan.isEmpty() && inventory.isEmpty() && getMainNode().isActive());
    }
    @Override public boolean pushPattern(IPatternDetails details, KeyCounter[] table, Direction direction) {
        if (!template.isEmpty() || !acceptsPlans()
                || !(details instanceof MagicalPattern offered) || offered.station() != station()) return false;
        // Decode again: never trust cached outputs or externally supplied pattern implementations.
        var pattern = MagicalPatternItem.readPattern(offered.getDefinition().toStack(), getLevel());
        if (pattern == null || pattern.station() != station()
                || !MagicalRecipeResolver.sameResult(offered.recipe(), pattern.recipe())) return false;
        var actual = validateInputs(pattern, table, getLevel());
        if (actual == null) return false;
        var recipe = MagicalRecipeResolver.resolve(getLevel(), station(), pattern.catalyst(), pattern.recipe().recipeId(), actual);
        if (!MagicalRecipeResolver.sameResult(pattern.recipe(), recipe) || recipe.outputs().size() > inventory.size()) return false;
        for (int i = 0; i < actual.size(); i++) inventory.setItemDirect(i, actual.get(i));
        plan = pattern.getDefinition().toStack();
        reservedMana = recipe.mana();
        rememberResult(recipe);
        progress = 0;
        for (var counter : table) counter.clear();
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
        saveChanges();
        return true;
    }
    private void rememberResult(MagicalRecipeResolver.Result recipe) {
        jobOutputs.clear();
        for (int slot = 0; slot < recipe.outputs().size(); slot++) {
            jobOutputs.setItemDirect(slot, recipe.outputs().get(slot).copy());
        }
        jobTicks = recipe.ticks();
    }

    private boolean matchesJobResult(MagicalRecipeResolver.Result recipe) {
        if (recipe == null || jobOutputs.isEmpty() || recipe.mana() != reservedMana
                || recipe.ticks() != jobTicks || recipe.outputs().size() > jobOutputs.size()) return false;
        for (int slot = 0; slot < jobOutputs.size(); slot++) {
            var output = slot < recipe.outputs().size() ? recipe.outputs().get(slot) : ItemStack.EMPTY;
            if (!ItemStack.matches(jobOutputs.getStackInSlot(slot), output)) return false;
        }
        return true;
    }

    /** Validate first without mutating CPU input counters; reconstruct ordered recipe inputs afterwards. */
    public static List<ItemStack> validateInputs(MagicalPattern pattern, KeyCounter[] table, Level level) {
        var required = pattern.getInputs();
        if (table.length != required.length) return null;
        var items = new KeyCounter();
        for (int i = 0; i < table.length; i++) {
            if (table[i] == null) return null;
            long remaining = required[i].getMultiplier();
            for (var entry : table[i]) {
                long amount = entry.getLongValue();
                if (amount <= 0 || amount > remaining || !required[i].isValid(entry.getKey(), level)) return null;
                remaining -= amount;
                if (entry.getKey() instanceof AEItemKey) items.add(entry.getKey(), amount);
                else if (entry.getKey() != ManaKey.INSTANCE) return null;
            }
            if (remaining != 0) return null;
        }
        var actual = new ArrayList<ItemStack>();
        if (pattern.hasSlotInputs()) {
            // Each counter belongs to its recipe slot, including the final reagent.
            for (var counter : table) {
                var key = counter.getFirstKey(AEItemKey.class);
                if (key == null) return null;
                actual.add(key.toStack(1));
            }
            return actual;
        }
        for (var ingredient : pattern.ingredients()) {
            AEItemKey key = AEItemKey.of(ingredient);
            if (pattern.station() == MagicalStation.PURE_DAISY && items.get(key) == 0)
                key = items.getFirstKey(AEItemKey.class);
            if (key == null || items.get(key) < 1) return null;
            actual.add(key.toStack(1));
            items.remove(key, 1);
        }
        items.removeZeros();
        return items.isEmpty() ? actual : null;
    }
    @Override public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, plan.isEmpty() && inventory.isEmpty() && template.isEmpty(), true);
    }
    @Override public TickRateModulation tickingRequest(IGridNode node, int elapsed) {
        var grid = getMainNode().getGrid();
        if (grid == null || !getMainNode().isActive()) return TickRateModulation.IDLE;
        if (plan.isEmpty() && inventory.isEmpty() && !template.isEmpty()) startManualJob();
        if (!plan.isEmpty()) {
            var pattern = MagicalPatternItem.readPattern(plan, getLevel());
            if (pattern == null || pattern.station() != station()) return TickRateModulation.IDLE;
            var actual = new ArrayList<ItemStack>();
            for (var stack : inventory) if (!stack.isEmpty()) actual.add(stack.copy());
            var recipe = MagicalRecipeResolver.resolve(getLevel(), station(), pattern.catalyst(), pattern.recipe().recipeId(), actual);
            if (!matchesJobResult(recipe))
                return TickRateModulation.IDLE; // Keep resources recoverable if a datapack changes the recipe.
            int speed = 1 << getInstalledUpgrades(appeng.core.definitions.AEItems.SPEED_CARD);
            // The first scheduler interval includes idle time before the job existed.
            int activeTicks = progress == 0 ? 1 : Math.max(1, elapsed);
            int work = (int) Math.min((long) activeTicks * speed, Math.max(1, recipe.ticks() - progress));
            double energy = work * 2.0;
            if (grid.getEnergyService().extractAEPower(energy, Actionable.SIMULATE, PowerMultiplier.CONFIG) < energy)
                return TickRateModulation.IDLE;
            grid.getEnergyService().extractAEPower(energy, Actionable.MODULATE, PowerMultiplier.CONFIG);
            progress += work;
            if (progress >= recipe.ticks()) {
                inventory.clear();
                for (int i = 0; i < recipe.outputs().size(); i++) inventory.setItemDirect(i, recipe.outputs().get(i).copy());
                plan = ItemStack.EMPTY;
                reservedMana = 0;
                jobOutputs.clear();
                jobTicks = 0;
                progress = 0;
            }
            saveChanges();
        }
        if (plan.isEmpty()) {
            var storage = grid.getStorageService().getInventory();
            var source = new MachineSource(this);
            for (int i = 0; i < inventory.size(); i++) {
                var stack = inventory.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                long inserted = storage.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, source);
                if (inserted > 0) {
                    inventory.setItemDirect(i, stack.copyWithCount(stack.getCount() - (int) inserted));
                    saveChanges();
                }
            }
        }
        return plan.isEmpty() && inventory.isEmpty()
                ? template.isEmpty() ? TickRateModulation.SLEEP : TickRateModulation.SLOWER
                : TickRateModulation.URGENT;
    }
    @Override public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (var upgrade : upgrades) if (!upgrade.isEmpty()) drops.add(upgrade.copy());
        for (var stack : template) if (!stack.isEmpty()) drops.add(stack.copy());
        for (var stack : manualInputs) if (!stack.isEmpty()) drops.add(stack.copy());
        if (reservedMana > 0) drops.add(ManaCapsuleItem.filled(reservedMana));
    }
    @Override public void clearContent() {
        super.clearContent();
        upgrades.clear();
        template.clear();
        manualInputs.clear();
        plan = ItemStack.EMPTY; reservedMana = 0; progress = 0;
        jobOutputs.clear(); jobTicks = 0;
    }
    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        upgrades.writeToNBT(tag, "upgrades");
        template.writeToNBT(tag, "botanicalTemplate");
        manualInputs.writeToNBT(tag, "botanicalManualInputs");
        tag.put("botanicalPlan", plan.isEmpty() ? new CompoundTag() : plan.save(new CompoundTag()));
        jobOutputs.writeToNBT(tag, "botanicalJobOutputs");
        tag.putInt("botanicalJobTicks", jobTicks);
        tag.putLong("reservedMana", reservedMana);
        tag.putInt("botanicalProgress", progress);
    }
    @Override public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        upgrades.readFromNBT(tag, "upgrades");
        template.clear();
        manualInputs.clear();
        template.readFromNBT(tag, "botanicalTemplate");
        manualInputs.readFromNBT(tag, "botanicalManualInputs");
        plan = ItemStack.of(tag.getCompound("botanicalPlan"));
        jobOutputs.clear();
        jobOutputs.readFromNBT(tag, "botanicalJobOutputs");
        jobTicks = Math.max(0, tag.getInt("botanicalJobTicks"));
        reservedMana = Math.max(0, tag.getLong("reservedMana"));
        progress = Math.max(0, tag.getInt("botanicalProgress"));
    }
}
