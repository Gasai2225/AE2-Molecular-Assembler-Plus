package com.gasai.ccapplied.tiles;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedInvBlockEntity;
import appeng.core.AppEng;
import appeng.core.network.clientbound.AssemblerAnimationPacket;
import appeng.client.render.crafting.AssemblerAnimationStatus;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import com.gasai.ccapplied.core.registry.CCBlocks;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
import appeng.crafting.CraftingEvent;
import appeng.menu.AutoCraftingMenu;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import appeng.util.inv.AppEngInternalInventory;
import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.patterns.IMolecularAssemblerSupportedPattern;
import com.gasai.ccapplied.patterns.ExtremeCraftingPattern;
import com.gasai.ccapplied.patterns.DraconicFusionPattern;

/**
 * TileEntity for Extreme Molecular Assembler - supports 9x9 recipes
 */
public class ExtremeMolecularAssemblerTileEntity extends AENetworkedInvBlockEntity
        implements IUpgradeableObject, IGridTickable, ICraftingMachine, IPowerChannelState {
    public enum AssemblerTier {
        WYVERN,
        DRACONIC,
        CHAOTIC;

        public boolean canHandle(DraconicFusionPattern.FusionTier required) {
            return this.ordinal() >= required.ordinal();
        }
    }

    /**
     * Identifies the sub-inventory used by extreme molecular assemblers to store the input items for the crafting process.
     */
    public static final ResourceLocation INV_MAIN = CCApplied.makeId("extreme_molecular_assembler");

    private final CraftingContainer craftingInv;
    private final AppEngInternalInventory gridInv = new AppEngInternalInventory(this, 81 + 1, 1); // 81 slots (9x9) + output
    private final AppEngInternalInventory patternInv = new AppEngInternalInventory(this, 1, 1);
    private final InternalInventory gridInvExt = new FilteredInternalInventory(this.gridInv, new CraftingGridFilter());
    private final InternalInventory internalInv = new CombinedInternalInventory(this.gridInv, this.patternInv);
    private final IUpgradeInventory upgrades;
    private boolean isPowered = false;
    private Direction pushDirection = null;
    private ItemStack myPattern = ItemStack.EMPTY;
    private IMolecularAssemblerSupportedPattern myPlan = null;
    private double progress = 0;
    private boolean isAwake = false;
    private boolean forcePlan = false;
    private boolean reboot = true;
    private AssemblerTier assemblerTier = AssemblerTier.CHAOTIC;
    private long fusionEnergyAccumulated = 0L;
    private int fusionCraftTicks = 0;
    private int fusionAnimationRefreshTicks = 0;

    @OnlyIn(Dist.CLIENT)
    private AssemblerAnimationStatus animationStatus;

    public ExtremeMolecularAssemblerTileEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        this(blockEntityType, pos, blockState, resolveTierByBlockState(blockState));
    }

    public ExtremeMolecularAssemblerTileEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState,
            AssemblerTier assemblerTier) {
        super(blockEntityType, pos, blockState);
        this.assemblerTier = assemblerTier;

        this.getMainNode()
                .setIdlePowerUsage(0.0)
                .addService(IGridTickable.class, this);
        this.upgrades = UpgradeInventories.forMachine(AEBlocks.MOLECULAR_ASSEMBLER, getUpgradeSlots(),
                this::saveChanges);
        this.craftingInv = new TransientCraftingContainer(new AutoCraftingMenu(), 9, 9); // 9x9 for extreme crafting

    }

    private int getUpgradeSlots() {
        return 5;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        Component name;
        if (hasCustomName()) {
            name = getCustomName();
        } else {
            name = CCBlocks.EXTREME_MOLECULAR_ASSEMBLER.get().asItem().getDescription();
        }
        var icon = AEItemKey.of(CCBlocks.EXTREME_MOLECULAR_ASSEMBLER.get());

        // List installed upgrades as the tooltip to differentiate assemblers by upgrade count
        List<Component> tooltip;
        var accelerationCards = getInstalledUpgrades(AEItems.SPEED_CARD);
        if (accelerationCards == 0) {
            tooltip = List.of();
        } else {
            tooltip = List.of(
                    GuiText.CompatibleUpgrade.text(
                            Tooltips.of(AEItems.SPEED_CARD.asItem().getDescription()),
                            Tooltips.ofUnformattedNumber(accelerationCards)));
        }

        return new PatternContainerGroup(icon, name, tooltip);
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] table,
            Direction where) {
        if (this.myPattern.isEmpty()) {
            boolean isEmpty = this.gridInv.isEmpty() && this.patternInv.isEmpty();

            if (isEmpty && patternDetails instanceof IMolecularAssemblerSupportedPattern pattern) {
                if (canCraftPattern(pattern) && this.fillGrid(table, pattern)) {
                    this.forcePlan = true;
                    this.myPlan = pattern;
                    this.pushDirection = where;


                    this.updateSleepiness();
                    this.saveChanges();
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean canCraftPattern(IMolecularAssemblerSupportedPattern pattern) {
        if (isTieredDraconicAssembler()) {
            if (pattern instanceof DraconicFusionPattern fusionPattern) {
                return assemblerTier.canHandle(fusionPattern.getTier());
            }
            return false;
        }

        if (pattern instanceof DraconicFusionPattern) {
            return false;
        }

        if (pattern instanceof ExtremeCraftingPattern) {
            return true;
        }

        return false;
    }

    private boolean isAllowedStoredPattern(IMolecularAssemblerSupportedPattern pattern) {
        if (!canCraftPattern(pattern)) {
            return false;
        }
        if (pattern instanceof DraconicFusionPattern fusionPattern) {
            return assemblerTier.canHandle(fusionPattern.getTier());
        }
        return true;
    }

    private static AssemblerTier resolveTierByBlockState(BlockState state) {
        if (CCOptionalMods.isDraconicEvolutionLoaded()) {
            try {
                var block = state.getBlock();
                if (CCBlocks.WYVERN_MOLECULAR_ASSEMBLER != null
                        && block == CCBlocks.WYVERN_MOLECULAR_ASSEMBLER.get()) {
                    return AssemblerTier.WYVERN;
                }
                if (CCBlocks.DRACONIC_MOLECULAR_ASSEMBLER != null
                        && block == CCBlocks.DRACONIC_MOLECULAR_ASSEMBLER.get()) {
                    return AssemblerTier.DRACONIC;
                }
            } catch (Exception ignored) {
            }
        }
        return AssemblerTier.CHAOTIC;
    }

    private boolean fillGrid(KeyCounter[] table, IMolecularAssemblerSupportedPattern pattern) {
        KeyCounter[] available = new KeyCounter[table.length];
        for (int i = 0; i < table.length; i++) {
            available[i] = new KeyCounter();
            if (table[i] != null) {
                available[i].addAll(table[i]);
            }
        }
        var candidate = new TransientCraftingContainer(new AutoCraftingMenu(), 9, 9);
        pattern.fillCraftingGrid(available, candidate::setItem);
        if (pattern.assemble(candidate, getLevel()).isEmpty()) {
            return false;
        }
        for (KeyCounter counter : available) {
            counter.removeZeros();
            if (!counter.isEmpty()) {
                return false;
            }
        }
        for (int i = 0; i < 81; i++) {
            this.gridInv.setItemDirect(i, candidate.getItem(i));
        }
        for (KeyCounter counter : table) {
            if (counter != null) {
                counter.clear();
            }
        }
        return true;
    }

    private void updateSleepiness() {
        final boolean wasEnabled = this.isAwake;
        this.isAwake = this.myPlan != null && this.hasMats() || this.canPush();
        if (wasEnabled != this.isAwake) {
            getMainNode().ifPresent((grid, node) -> {
                if (this.isAwake) {
                    grid.getTickManager().wakeDevice(node);
                } else {
                    grid.getTickManager().sleepDevice(node);
                }
            });
        }
    }

    private boolean canPush() {
        return !this.gridInv.getStackInSlot(81).isEmpty();
    }

    private boolean hasMats() {
        if (this.myPlan == null) {
            return false;
        }

        for (int x = 0; x < this.craftingInv.getContainerSize(); x++) {
            this.craftingInv.setItem(x, this.gridInv.getStackInSlot(x));
        }

        return !this.myPlan.assemble(this.craftingInv, this.getLevel()).isEmpty();
    }

    @Override
    public boolean acceptsPlans() {
        return this.patternInv.isEmpty();
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        final boolean c = super.readFromStream(data);
        final boolean oldPower = this.isPowered;
        this.isPowered = data.readBoolean();
        return this.isPowered != oldPower || c;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.isPowered);
    }

    @Override
    public void saveAdditional(CompoundTag data, Provider provider) {
        super.saveAdditional(data, provider);
        if (this.forcePlan) {
            // If the plan is null it means the pattern previously loaded from NBT hasn't been decoded yet
            var pattern = myPlan != null ? ((IPatternDetails) myPlan).getDefinition().toStack() : myPattern;
            if (!pattern.isEmpty()) {
                var compound = new CompoundTag();
                pattern.save(provider, compound);
                data.put("myPlan", compound);
                if (this.pushDirection != null) {
                    data.putInt("pushDirection", this.pushDirection.ordinal());
                }
            }
        }

        data.putLong("fusionEnergyAccumulated", this.fusionEnergyAccumulated);
        data.putInt("fusionCraftTicks", this.fusionCraftTicks);
        this.upgrades.writeToNBT(data, "upgrades", provider);
    }

    @Override
    public void loadTag(CompoundTag data, Provider provider) {
        super.loadTag(data, provider);

        this.forcePlan = false;
        this.myPattern = ItemStack.EMPTY;
        this.myPlan = null;
        this.fusionEnergyAccumulated = data.getLong("fusionEnergyAccumulated");
        this.fusionCraftTicks = data.getInt("fusionCraftTicks");
        this.fusionAnimationRefreshTicks = 0;

        if (data.contains("myPlan")) {
            var pattern = ItemStack.parseOptional(provider, data.getCompound("myPlan"));
            if (!pattern.isEmpty()) {
                this.forcePlan = true;
                this.myPattern = pattern;
                this.pushDirection = data.contains("pushDirection")
                        ? Direction.from3DDataValue(data.getInt("pushDirection")) : null;
            }
        }

        this.upgrades.readFromNBT(data, "upgrades", provider);
        this.recalculatePlan();
    }

    private void recalculatePlan() {
        this.reboot = true;

        if (this.forcePlan) {
            // If we're in forced mode, and myPattern is not empty, but the plan is null,
            // this indicates that we received an encoded pattern from NBT data, but
            // didn't have a chance to decode it yet
            if (getLevel() != null && myPlan == null) {
                if (!myPattern.isEmpty()) {
                    if (PatternDetailsHelper.decodePattern(myPattern, getLevel())
                            instanceof IMolecularAssemblerSupportedPattern supportedPlan) {
                        if (isAllowedStoredPattern(supportedPlan)) {
                            this.myPlan = supportedPlan;
                        }
                    }
                }

                this.myPattern = ItemStack.EMPTY;

                if (myPlan == null) {
                    this.forcePlan = false;
                }
            }

            return;
        }

        final ItemStack is = this.patternInv.getStackInSlot(0);

        boolean reset = true;

        if (!is.isEmpty()) {
            if (ItemStack.isSameItemSameComponents(is, this.myPattern)) {
                reset = this.myPlan == null || !isAllowedStoredPattern(this.myPlan);
            } else if (PatternDetailsHelper.decodePattern(is, getLevel())
                    instanceof IMolecularAssemblerSupportedPattern supportedPattern) {
                if (isAllowedStoredPattern(supportedPattern)) {
                    reset = false;
                    this.progress = 0;
                    this.myPattern = is;
                    this.myPlan = supportedPattern;
                }
            }
        }

        if (reset) {
            this.progress = 0;
            this.fusionEnergyAccumulated = 0L;
            this.fusionCraftTicks = 0;
            this.fusionAnimationRefreshTicks = 0;
            this.forcePlan = false;
            this.myPlan = null;
            this.myPattern = ItemStack.EMPTY;
            this.pushDirection = null;
        }

        this.updateSleepiness();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.COVERED;
    }

    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        } else if (id.equals(INV_MAIN)) {
            return this.internalInv;
        }

        return super.getSubInventory(id);
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.internalInv;
    }

    @Override
    protected InternalInventory getExposedInventoryForSide(Direction side) {
        return this.gridInvExt;
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        if (inv == this.gridInv || inv == this.patternInv) {
            this.recalculatePlan();
        }
    }

    public int getCraftingProgress() {
        return (int) this.progress;
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);

        for (var upgrade : upgrades) {
            drops.add(upgrade);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        upgrades.clear();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        this.recalculatePlan();
        this.updateSleepiness();
        return new TickingRequest(1, 1, !this.isAwake);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!this.gridInv.getStackInSlot(81).isEmpty()) {
            this.pushOut(this.gridInv.getStackInSlot(81));

            // did it eject?
            if (this.gridInv.getStackInSlot(81).isEmpty()) {
                this.saveChanges();
            }

            this.ejectHeldItems();
            this.updateSleepiness();
            this.progress = 0;
            this.fusionEnergyAccumulated = 0L;
            this.fusionCraftTicks = 0;
            this.fusionAnimationRefreshTicks = 0;
            return this.isAwake ? TickRateModulation.IDLE : TickRateModulation.SLEEP;
        }

        if (this.myPlan == null) {
            this.updateSleepiness();
            return TickRateModulation.SLEEP;
        }

        if (this.reboot) {
            ticksSinceLastCall = 1;
        }

        if (!this.isAwake) {
            return TickRateModulation.SLEEP;
        }

        this.reboot = false;
        boolean fusionCraftReady = false;
        if (this.myPlan instanceof DraconicFusionPattern fusionPattern) {
            TickRateModulation fusionResult = this.tickDraconicFusionCraft(fusionPattern, ticksSinceLastCall);
            if (fusionResult != null) {
                return fusionResult;
            }
            fusionCraftReady = true;
        }

        int speed = 10;
        if (!fusionCraftReady) {
            switch (this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD)) {
                case 0 -> this.progress += this.userPower(ticksSinceLastCall, speed = 10, 1.0);
                case 1 -> this.progress += this.userPower(ticksSinceLastCall, speed = 13, 1.3);
                case 2 -> this.progress += this.userPower(ticksSinceLastCall, speed = 17, 1.7);
                case 3 -> this.progress += this.userPower(ticksSinceLastCall, speed = 20, 2.0);
                case 4 -> this.progress += this.userPower(ticksSinceLastCall, speed = 25, 2.5);
                case 5 -> this.progress += this.userPower(ticksSinceLastCall, speed = 50, 5.0);
            }
        }

        if (this.progress >= 100) {
            for (int x = 0; x < this.craftingInv.getContainerSize(); x++) {
                this.craftingInv.setItem(x, this.gridInv.getStackInSlot(x));
            }

            this.progress = 0;
            final ItemStack output = this.myPlan.assemble(this.craftingInv, this.getLevel());
            if (!output.isEmpty()) {
                output.onCraftedBySystem(level);
                CraftingEvent.fireAutoCraftingEvent(getLevel(), (IPatternDetails) this.myPlan, output, this.craftingInv);

                var craftingRemainders = this.myPlan.getRemainingItems(this.craftingInv);

                // Consume inputs before external insertion can trigger inventory callbacks.
                for (int x = 0; x < this.craftingInv.getContainerSize(); x++) {
                    this.gridInv.setItemDirect(x, craftingRemainders.get(x));
                }
                this.pushOut(output.copy());

                if (this.patternInv.isEmpty()) {
                    this.forcePlan = false;
                    this.myPlan = null;
                    if (this.gridInv.isEmpty()) {
                        this.pushDirection = null;
                    }
                }
                this.fusionEnergyAccumulated = 0L;
                this.fusionCraftTicks = 0;
                this.fusionAnimationRefreshTicks = 0;

                this.ejectHeldItems();

                var item = AEItemKey.of(output);
                if (item != null) {
                    AppEng.instance().sendToAllNearExcept(null, this.worldPosition.getX(), this.worldPosition.getY(),
                            this.worldPosition.getZ(), 32, this.level, new AssemblerAnimationPacket(this.worldPosition,
                                    (byte) speed, item));
                }

                this.saveChanges();
                this.updateSleepiness();
                return this.isAwake ? TickRateModulation.IDLE : TickRateModulation.SLEEP;
            }
        }

        return TickRateModulation.FASTER;
    }

    @Nullable
    private TickRateModulation tickDraconicFusionCraft(DraconicFusionPattern fusionPattern, int ticksSinceLastCall) {
        long required = Math.max(0L, fusionPattern.getTotalEnergy());
        int chargeTicks = getFusionPhaseTicks();
        int craftTicks = getFusionPhaseTicks();

        this.refreshFusionAnimation(fusionPattern, ticksSinceLastCall);

        if (required > 0 && this.fusionEnergyAccumulated < required) {
            long remaining = required - this.fusionEnergyAccumulated;
            long chargePerTick = Math.max(1L, (required + chargeTicks - 1L) / chargeTicks);
            long wanted = Math.min(remaining, chargePerTick * Math.max(1, ticksSinceLastCall));
            long pulled = pullFusionEnergy(wanted);
            if (pulled <= 0) {
                return TickRateModulation.FASTER;
            }
            this.fusionEnergyAccumulated = Math.min(required, this.fusionEnergyAccumulated + pulled);
            this.saveChanges();
            this.progress = Math.min(50.0, (this.fusionEnergyAccumulated * 50.0) / required);
            return TickRateModulation.FASTER;
        }

        this.fusionCraftTicks += Math.max(1, ticksSinceLastCall);
        this.saveChanges();
        if (this.fusionCraftTicks < craftTicks) {
            this.progress = 50.0 + (this.fusionCraftTicks * 50.0) / craftTicks;
            return TickRateModulation.FASTER;
        }

        this.progress = 100;
        return null;
    }

    private int getFusionPhaseTicks() {
        int baseTicks = switch (this.assemblerTier) {
            case WYVERN -> 170;
            case DRACONIC -> 108;
            case CHAOTIC -> 47;
        };
        return Math.max(1, (int) Math.ceil(baseTicks / getSpeedCardMultiplier()));
    }

    private double getSpeedCardMultiplier() {
        return switch (this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD)) {
            case 1 -> 1.3;
            case 2 -> 1.7;
            case 3 -> 2.0;
            case 4 -> 2.5;
            case 5 -> 5.0;
            default -> 1.0;
        };
    }

    private void refreshFusionAnimation(DraconicFusionPattern fusionPattern, int ticksSinceLastCall) {
        this.fusionAnimationRefreshTicks -= Math.max(1, ticksSinceLastCall);
        if (this.fusionAnimationRefreshTicks > 0) {
            return;
        }

        this.fusionAnimationRefreshTicks = 80;
        this.sendCraftingAnimation(fusionPattern.getOutputStack(), (byte) 1);
    }

    private void sendCraftingAnimation(ItemStack stack, byte speed) {
        var item = AEItemKey.of(stack);
        if (item == null || this.level == null) {
            return;
        }
        AppEng.instance().sendToAllNearExcept(null, this.worldPosition.getX(), this.worldPosition.getY(),
                this.worldPosition.getZ(), 32, this.level, new AssemblerAnimationPacket(this.worldPosition, speed,
                        item));
    }

    private long pullFusionEnergy(long wanted) {
        if (wanted <= 0) {
            return 0;
        }
        long fromAdjacent = pullAdjacentForgeEnergy(wanted);
        if (fromAdjacent >= wanted) {
            return fromAdjacent;
        }
        var grid = getMainNode().getGrid();
        if (grid == null) {
            return fromAdjacent;
        }
        double extracted = grid.getEnergyService().extractAEPower(
                wanted - fromAdjacent,
                Actionable.MODULATE,
                PowerMultiplier.CONFIG);
        return fromAdjacent + Math.max(0L, (long) extracted);
    }

    private long pullAdjacentForgeEnergy(long wanted) {
        if (wanted <= 0 || level == null) {
            return 0;
        }
        long pulled = 0;
        for (Direction direction : Direction.values()) {
            if (pulled >= wanted) {
                break;
            }
            var be = level.getBlockEntity(worldPosition.relative(direction));
            if (be == null) {
                continue;
            }
            var cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, be.getBlockPos(), direction.getOpposite());
            if (cap == null) {
                continue;
            }
            long remaining = wanted - pulled;
            int request = (int) Math.min(Integer.MAX_VALUE, remaining);
            pulled += cap.extractEnergy(request, false);
        }
        return pulled;
    }

    private void ejectHeldItems() {
        if (this.gridInv.getStackInSlot(81).isEmpty()) {
            for (int x = 0; x < 81; x++) {
                final ItemStack is = this.gridInv.getStackInSlot(x);
                if (!is.isEmpty()
                        && (this.myPlan == null || !this.myPlan.isItemValid(x, AEItemKey.of(is), this.level))) {
                    this.gridInv.setItemDirect(81, is);
                    this.gridInv.setItemDirect(x, ItemStack.EMPTY);
                    this.saveChanges();
                    return;
                }
            }
        }
    }

    private int userPower(int ticksPassed, int bonusValue, double acceleratorTax) {
        var grid = getMainNode().getGrid();
        if (grid != null) {
            return (int) (grid.getEnergyService().extractAEPower(ticksPassed * bonusValue * acceleratorTax,
                    Actionable.MODULATE, PowerMultiplier.CONFIG) / acceleratorTax);
        } else {
            return 0;
        }
    }

    private void pushOut(ItemStack output) {
        if (this.pushDirection == null) {
            for (Direction d : Direction.values()) {
                output = this.pushTo(output, d);
            }
        } else {
            output = this.pushTo(output, this.pushDirection);
        }


        this.gridInv.setItemDirect(81, output);
    }

    private ItemStack pushTo(ItemStack output, Direction d) {
        if (output.isEmpty()) {
            return output;
        }

        final Level level = this.getLevel();
        if (level == null) {
            return output;
        }
        final BlockEntity te = level.getBlockEntity(this.worldPosition.relative(d));

        if (te == null) {
            return output;
        }

        var adaptor = InternalInventory.wrapExternal(level, te.getBlockPos(), d.getOpposite());
        if (adaptor == null) {
            return output;
        }

        final int size = output.getCount();
        output = adaptor.addItems(output);
        final int newSize = output.isEmpty() ? 0 : output.getCount();

        if (size != newSize) {
            this.saveChanges();
        }

        return output;
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        if (reason != IGridNodeListener.State.GRID_BOOT) {
            boolean newState = false;

            var grid = getMainNode().getGrid();
            if (grid != null) {
                newState = this.getMainNode().isPowered() && grid.getEnergyService().extractAEPower(1,
                        Actionable.SIMULATE, PowerMultiplier.CONFIG) > 0.0001;
            }

            if (newState != this.isPowered) {
                this.isPowered = newState;
                this.markForUpdate();
            }
        }
    }

    @Override
    public boolean isPowered() {
        return this.isPowered;
    }

    @Override
    public boolean isActive() {
        return this.isPowered;
    }

    @OnlyIn(Dist.CLIENT)
    public void setAnimationStatus(@Nullable AssemblerAnimationStatus status) {
        this.animationStatus = status;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public AssemblerAnimationStatus getAnimationStatus() {
        return this.animationStatus;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    @Nullable
    public IMolecularAssemblerSupportedPattern getCurrentPattern() {
        if (isClientSide()) {
            var patternItem = patternInv.getStackInSlot(0);
            var pattern = PatternDetailsHelper.decodePattern(patternItem, level);
            if (pattern instanceof IMolecularAssemblerSupportedPattern supportedPattern) {
                return isAllowedStoredPattern(supportedPattern) ? supportedPattern : null;
            }
            return null;
        } else {
            return myPlan != null && isAllowedStoredPattern(myPlan) ? myPlan : null;
        }
    }

    private class CraftingGridFilter implements IAEItemFilter {
        private boolean hasPattern() {
            return ExtremeMolecularAssemblerTileEntity.this.myPlan != null
                    && !ExtremeMolecularAssemblerTileEntity.this.patternInv.isEmpty();
        }

        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return slot == 81;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            if (slot >= 81) {
                return false;
            }

            if (this.hasPattern()) {
                if (!ExtremeMolecularAssemblerTileEntity.this.myPlan.isSlotEnabled(slot)) {
                    return false;
                }
                return ExtremeMolecularAssemblerTileEntity.this.myPlan.isItemValid(slot, AEItemKey.of(stack),
                        ExtremeMolecularAssemblerTileEntity.this.getLevel());
            }
            return false;
        }
    }

    public boolean isTieredDraconicAssembler() {
        return this.assemblerTier != AssemblerTier.CHAOTIC
                || (CCOptionalMods.isDraconicEvolutionLoaded()
                    && CCBlocks.CHAOTIC_MOLECULAR_ASSEMBLER != null
                    && getBlockState().getBlock() == CCBlocks.CHAOTIC_MOLECULAR_ASSEMBLER.get());
    }

    public String getAssemblerDisplayName() {
        return switch (assemblerTier) {
            case WYVERN -> "Wyvern Molecular Assembler";
            case DRACONIC -> "Draconic Molecular Assembler";
            case CHAOTIC -> !CCOptionalMods.isDraconicEvolutionLoaded()
                    || CCBlocks.CHAOTIC_MOLECULAR_ASSEMBLER == null
                    || getBlockState().getBlock() == CCBlocks.EXTREME_MOLECULAR_ASSEMBLER.get()
                    ? "Extreme Molecular Assembler"
                    : "Chaotic Molecular Assembler";
        };
    }
}
