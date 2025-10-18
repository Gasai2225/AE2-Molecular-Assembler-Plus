package com.gasai.ccapplied.menus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import appeng.api.stacks.AEItemKey;
import com.gasai.ccapplied.tiles.ExtremeMolecularAssemblerTileEntity;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.OutputSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.api.inventories.InternalInventory;
import appeng.client.Point;
import appeng.menu.slot.IOptionalSlot;

/**
 * Menu for Extreme Molecular Assembler - supports 9x9 recipes
 */
public class ExtremeMolecularAssemblerMenu extends UpgradeableMenu<ExtremeMolecularAssemblerTileEntity>
        implements IProgressProvider {

    public static final MenuType<ExtremeMolecularAssemblerMenu> TYPE = MenuTypeBuilder
            .create(ExtremeMolecularAssemblerMenu::new, ExtremeMolecularAssemblerTileEntity.class)
            .build("extreme_molecular_assembler");

    private static final int MAX_CRAFT_PROGRESS = 100;
    private final ExtremeMolecularAssemblerTileEntity molecularAssembler;
    @GuiSync(4)
    public int craftProgress = 0;

    private Slot encodedPatternSlot;

    public ExtremeMolecularAssemblerMenu(int id, Inventory playerInv, ExtremeMolecularAssemblerTileEntity be) {
        super(TYPE, id, playerInv, be);
        this.molecularAssembler = be;
    }

    public boolean isValidItemForSlot(int slotIndex, ItemStack i) {
        var details = molecularAssembler.getCurrentPattern();
        if (details != null) {
            return details.isItemValid(slotIndex, AEItemKey.of(i), molecularAssembler.getLevel());
        }

        return false;
    }

    @Override
    protected void setupConfig() {
        var mac = this.getHost().getSubInventory(ExtremeMolecularAssemblerTileEntity.INV_MAIN);

        for (int i = 0; i < 81; i++) {
            this.addSlot(new ExtremeMolecularAssemblerPatternSlot(this, mac, i), SlotSemantics.MACHINE_CRAFTING_GRID);
        }

        this.addSlot(new OutputSlot(mac, 81, null), SlotSemantics.MACHINE_OUTPUT);

        encodedPatternSlot = this.addSlot(
                new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, mac, 82),
                SlotSemantics.ENCODED_PATTERN);
    }

    @Override
    public void broadcastChanges() {
        this.craftProgress = this.molecularAssembler.getCraftingProgress();

        this.standardDetectAndSendChanges();
    }

    @Override
    public int getCurrentProgress() {
        return this.craftProgress;
    }

    @Override
    public int getMaxProgress() {
        return MAX_CRAFT_PROGRESS;
    }

    @Override
    public void onSlotChange(Slot s) {

        if (s == encodedPatternSlot) {
            for (Slot otherSlot : slots) {
                if (otherSlot != s && otherSlot instanceof AppEngSlot) {
                    ((AppEngSlot) otherSlot).resetCachedValidation();
                }
            }
        }

    }

    /**
     * Slot for pattern in Extreme Molecular Assembler
     */
    public static class ExtremeMolecularAssemblerPatternSlot extends AppEngSlot implements IOptionalSlot {

        private final ExtremeMolecularAssemblerMenu mac;

        public ExtremeMolecularAssemblerPatternSlot(ExtremeMolecularAssemblerMenu mac, InternalInventory inv,
                int invSlot) {
            super(inv, invSlot);
            this.mac = mac;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return super.mayPlace(stack) && this.mac.isValidItemForSlot(this.getSlotIndex(), stack);
        }

        @Override
        protected boolean getCurrentValidationState() {
            ItemStack stack = getItem();
            return stack.isEmpty() || mayPlace(stack);
        }

        @Override
        public boolean isRenderDisabled() {
            return true;
        }

        @Override
        public boolean isSlotEnabled() {
            int slotIndex = getSlotIndex();
            if (!getInventory().getStackInSlot(slotIndex).isEmpty()) {
                return true;
            }

            var pattern = mac.getHost().getCurrentPattern();
            return slotIndex >= 0 && slotIndex < 81 && pattern != null && pattern.isSlotEnabled(slotIndex);
        }

        @Override
        public Point getBackgroundPos() {
            return new Point(x - 1, y - 1);
        }
    }
}
