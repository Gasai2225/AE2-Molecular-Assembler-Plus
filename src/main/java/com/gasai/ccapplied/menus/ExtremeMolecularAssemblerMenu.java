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
 * Меню для Extreme Molecular Assembler - поддерживает 9x9 рецепты
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

        // Создаем слоты для сетки 9x9 (81 слот)
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

        // If the pattern changes, the crafting grid slots have to be revalidated
        if (s == encodedPatternSlot) {
            for (Slot otherSlot : slots) {
                if (otherSlot != s && otherSlot instanceof AppEngSlot) {
                    ((AppEngSlot) otherSlot).resetCachedValidation();
                }
            }
        }

    }

    /**
     * Слот для паттерна в Extreme Molecular Assembler
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
            return true; // The background image does not include a slot background
        }

        @Override
        public boolean isSlotEnabled() {
            int slotIndex = getSlotIndex();
            // Always enabled when there's an item in the inventory (otherwise you can't take it out...)
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
