package com.gasai.ccapplied.botania;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ManaCellItem extends Item implements IBasicCellItem {
    private final int kilobytes;

    public ManaCellItem(int kilobytes) {
        super(new Properties().stacksTo(1));
        this.kilobytes = kilobytes;
    }

    @Override public AEKeyType getKeyType() { return ManaKeyType.INSTANCE; }
    @Override public int getBytes(ItemStack stack) { return kilobytes * 1024; }
    @Override public int getBytesPerType(ItemStack stack) { return 8; }
    @Override public int getTotalTypes(ItemStack stack) { return 1; }
    @Override public double getIdleDrain() { return 0.5 + kilobytes / 64.0; }
    @Override public IUpgradeInventory getUpgrades(ItemStack stack) { return UpgradeInventories.forItem(stack, 0); }
    @Override public FuzzyMode getFuzzyMode(ItemStack stack) { return FuzzyMode.IGNORE_ALL; }
    @Override public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {}

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
            List<Component> lines, TooltipFlag flag) {
        addCellInformationToTooltip(stack, lines);
    }
}
