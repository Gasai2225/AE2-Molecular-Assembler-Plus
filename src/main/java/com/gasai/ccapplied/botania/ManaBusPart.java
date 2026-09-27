package com.gasai.ccapplied.botania;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.*;
import appeng.api.parts.*;
import appeng.api.util.AECableType;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.UpgradeablePart;
import com.gasai.ccapplied.CCApplied;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

public final class ManaBusPart extends UpgradeablePart implements IGridTickable {
    private final ManaTransferBuffer buffer = new ManaTransferBuffer();
    private final boolean importing;
    private final IPartModel off, on, active;
    private boolean lastSignal, pendingPulse;
    private long lastTransfer;

    public ManaBusPart(IPartItem<?> item) {
        super(item);
        importing = item.asItem() == BotaniaContent.MANA_IMPORT_BUS.get();
        String kind = importing ? "import" : "export";
        off = model(kind, "off");
        on = model(kind, "on");
        active = model(kind, "has_channel");
        getMainNode().setIdlePowerUsage(8).addService(IGridTickable.class, this);
    }

    @Override protected void registerSettings(appeng.api.util.IConfigManagerBuilder builder) {
        super.registerSettings(builder);
        builder.registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
    }

    private static IPartModel model(String kind, String state) {
        return new PartModel(CCApplied.makeId("part/mana_" + kind + "_bus"),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ae2", "part/" + kind + "_bus_" + state));
    }

    @Override protected int getUpgradeSlots() { return 5; }
    @Override public RedstoneMode getRSMode() { return getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED); }
    @Override public float getCableConnectionLength(AECableType cable) { return 5; }
    @Override public IPartModel getStaticModels() { return isActive() ? active : isPowered() ? on : off; }
    @Override public void getBoxes(IPartCollisionHelper boxes) {
        if (!importing) {
            boxes.addBox(4, 4, 12, 12, 12, 14);
            boxes.addBox(5, 5, 14, 11, 11, 15);
            boxes.addBox(6, 6, 15, 10, 10, 16);
            boxes.addBox(6, 6, 11, 10, 10, 12);
            return;
        }
        boxes.addBox(6, 6, 11, 10, 10, 13);
        boxes.addBox(5, 5, 13, 11, 11, 14);
        boxes.addBox(4, 4, 14, 12, 12, 16);
    }
    public long bufferedMana() { return buffer.amount(); }
    public long lastTransfer() { return lastTransfer; }
    public long transferLimit() {
        return Math.min(BotaniaConfig.MANA_PER_TICK.get(),
                62_500L << getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD));
    }

    @Override public void upgradesChanged() {
        pendingPulse = false;
    }
    @Override protected void onSettingChanged(appeng.api.util.IConfigManager manager, appeng.api.config.Setting<?> setting) {
        super.onSettingChanged(manager, setting);
        pendingPulse = false;
        if (getHost() != null) lastSignal = getHost().hasRedstone();
    }

    @Override public void addToWorld() {
        super.addToWorld();
        lastSignal = getHost().hasRedstone();
    }
    @Override public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor) {
        if (isClientSide()) return;
        boolean signal = getHost().hasRedstone();
        boolean newPulse = !pendingPulse && !lastSignal && signal
                && getRSMode() == RedstoneMode.SIGNAL_PULSE
                && getUpgrades().isInstalled(AEItems.REDSTONE_CARD);
        lastSignal = signal;
        if (newPulse) {
            // Update state before saving: save notifications can re-enter this callback.
            pendingPulse = true;
            getHost().markForSave();
        }
    }
    @Override public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false);
    }
    @Override public TickRateModulation tickingRequest(IGridNode node, int elapsed) {
        lastTransfer = 0;
        if (!getMainNode().isActive()) return TickRateModulation.SAME;
        boolean pulseMode = getUpgrades().isInstalled(AEItems.REDSTONE_CARD)
                && getRSMode() == RedstoneMode.SIGNAL_PULSE;
        if (pulseMode ? !pendingPulse : isSleeping()) return TickRateModulation.SAME;
        if (pendingPulse) {
            pendingPulse = false;
            getHost().markForSave();
        }
        var pos = getBlockEntity().getBlockPos().relative(getSide());
        if (!getLevel().hasChunkAt(pos)) return TickRateModulation.SAME;
        var pool = BotaniaManaAccess.pool(getLevel().getBlockEntity(pos));
        var inventory = node.getGrid().getStorageService().getInventory();
        var source = IActionSource.ofMachine(this);
        long before = buffer.amount();
        if (importing) {
            lastTransfer = buffer.flushTo(inventory, source, transferLimit());
            if (pool != null && lastTransfer < transferLimit())
                lastTransfer += buffer.importTo(pool, inventory, source, transferLimit() - lastTransfer);
        } else if (pool != null) {
            lastTransfer = buffer.exportTo(inventory, pool, source, transferLimit());
        }
        if (lastTransfer > 0 || before != buffer.amount()) getHost().markForSave();
        return TickRateModulation.SAME;
    }

    @Override public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!isClientSide()) MenuOpener.open(ManaBusMenu.TYPE, player, MenuLocators.forPart(this));
        return true;
    }
    @Override public boolean onUseItemOn(ItemStack heldItem, Player player, InteractionHand hand, Vec3 pos) {
        var stack = player.getItemInHand(hand);
        long stored = ManaCapsuleItem.amount(stack);
        if (stored <= 0 || !player.isShiftKeyDown()) return super.onUseItemOn(heldItem, player, hand, pos);
        if (!isClientSide()) {
            long accepted = buffer.deposit(stored);
            if (accepted == stored) stack.shrink(1);
            else ManaCapsuleItem.setAmount(stack, stored - accepted);
            getHost().markForSave();
        }
        return true;
    }
    @Override public void writeToNBT(CompoundTag data, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeToNBT(data, registries);
        data.putLong("ccManaBuffer", buffer.amount());
        data.putBoolean("ccManaPulse", pendingPulse);
    }
    @Override public void readFromNBT(CompoundTag data, net.minecraft.core.HolderLookup.Provider registries) {
        super.readFromNBT(data, registries);
        buffer.restore(data.getLong("ccManaBuffer"));
        pendingPulse = data.getBoolean("ccManaPulse");
    }
    @Override public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        ManaKey.INSTANCE.addDrops(buffer.amount(), drops, getLevel(), getBlockEntity().getBlockPos());
    }
    @Override public void clearContent() {
        super.clearContent();
        buffer.restore(0);
    }
}
