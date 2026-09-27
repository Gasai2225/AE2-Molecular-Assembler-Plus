package com.gasai.ccapplied.botania;

import appeng.api.inventories.InternalInventory;
import appeng.api.parts.*;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractTerminalPart;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class MagicalTerminalPart extends AbstractTerminalPart implements InternalInventoryHost {
    public final AppEngInternalInventory ghosts = new AppEngInternalInventory(this, 17, 1);
    public final AppEngInternalInventory patterns = new AppEngInternalInventory(this, 2);
    private MagicalStation station = MagicalStation.POOL;
    private PoolCatalyst catalyst = PoolCatalyst.NONE;
    private boolean substitutions;
    private int revision;
    private static final IPartModel OFF = model("off"), ON = model("on"), ACTIVE = model("has_channel");
    public MagicalTerminalPart(IPartItem<?> item) { super(item); }
    private static IPartModel model(String state) {
        return new PartModel(MODEL_BASE,
                com.gasai.ccapplied.CCApplied.makeId("part/magical_terminal_" + (state.equals("off") ? "off" : "on")),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ae2", "part/display_status_" + state));
    }
    @Override public IPartModel getStaticModels() { return isActive() ? ACTIVE : isPowered() ? ON : OFF; }
    public MagicalStation station() { return station; }
    public PoolCatalyst catalyst() { return catalyst; }
    public boolean substitutions() { return station == MagicalStation.PURE_DAISY && substitutions; }
    public void setSubstitutions(boolean enabled) {
        substitutions = station == MagicalStation.PURE_DAISY && enabled;
        changed();
    }
    public int revision() { return revision; }
    public void cycleStation() {
        selectStation(MagicalStation.values()[(station.ordinal() + 1) % MagicalStation.values().length]);
    }
    public void selectStation(MagicalStation selected) {
        if (station == selected) return;
        station = selected;
        catalyst = PoolCatalyst.NONE;
        substitutions = false;
        ghosts.clear();
        changed();
    }
    public void cycleCatalyst() {
        selectCatalyst(PoolCatalyst.values()[(catalyst.ordinal() + 1) % PoolCatalyst.values().length]);
    }
    public void selectCatalyst(PoolCatalyst selected) {
        if (station != MagicalStation.POOL || catalyst == selected) return;
        catalyst = selected;
        changed();
    }
    public List<ItemStack> inputs() {
        var result = new ArrayList<ItemStack>();
        if (station == MagicalStation.BREWERY && !ghosts.getStackInSlot(16).isEmpty())
            result.add(ghosts.getStackInSlot(16).copyWithCount(1));
        for (int i = 0; i < station.inputSlots(); i++) if (!ghosts.getStackInSlot(i).isEmpty())
            result.add(ghosts.getStackInSlot(i).copyWithCount(1));
        if (station.hasReagent() && station != MagicalStation.BREWERY && !ghosts.getStackInSlot(16).isEmpty())
            result.add(ghosts.getStackInSlot(16).copyWithCount(1));
        return result;
    }
    private void changed() { revision++; saveChanges(); }
    @Override public void saveChanges() { if (getHost() != null && !isClientSide()) getHost().markForSave(); }
    @Override public void onChangeInventory(InternalInventory inventory, int slot) { changed(); }
    @Override public net.minecraft.world.inventory.MenuType<?> getMenuType(Player player) {
        return MagicalTerminalMenu.TYPE;
    }
    @Override public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        ghosts.writeToNBT(tag, "magicGhosts");
        patterns.writeToNBT(tag, "magicPatterns");
        tag.putString("magicStation", station.id());
        tag.putString("magicCatalyst", catalyst.id());
        tag.putBoolean("magicSubstitutions", substitutions());
    }
    @Override public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        ghosts.readFromNBT(tag, "magicGhosts");
        patterns.readFromNBT(tag, "magicPatterns");
        try {
            station = MagicalStation.fromId(tag.getString("magicStation"));
            catalyst = PoolCatalyst.fromId(tag.getString("magicCatalyst"));
        } catch (IllegalArgumentException ignored) {
            station = MagicalStation.POOL;
            catalyst = PoolCatalyst.NONE;
        }
        substitutions = station == MagicalStation.PURE_DAISY && tag.getBoolean("magicSubstitutions");
        revision++;
    }
    @Override public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        for (var stack : patterns) if (!stack.isEmpty()) drops.add(stack.copy());
    }
    @Override public void clearContent() {
        super.clearContent();
        patterns.clear();
        ghosts.clear();
    }
}
