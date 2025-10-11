package com.gasai.ccapplied.client.gui.menu;

import com.gasai.ccapplied.common.block.entity.ExtremePatternEncoderBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ExtremePatternEncoderMenu extends AbstractContainerMenu {

    public final ExtremePatternEncoderBlockEntity be;

    public ExtremePatternEncoderMenu(int id, Inventory inv, ExtremePatternEncoderBlockEntity be) {
        super(null, id); // TODO: добавить свой MenuType
        this.be = be;

        // 9x9 входы
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new SlotItemHandler(be.input, y * 9 + x, 8 + x * 18, 18 + y * 18));
            }
        }

        // 3x3 выходы (в центре справа)
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new SlotItemHandler(be.output, y * 3 + x, 200 + x * 18, 90 + y * 18));
            }
        }

        // шаблоны
        addSlot(new SlotItemHandler(be.patternSlot, 0, 200, 30));
        addSlot(new SlotItemHandler(be.encodedSlot, 0, 200, 60));

        // инвентарь игрока
        int startY = 200;
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 9; x++)
                addSlot(new Slot(inv, x + y * 9 + 9, 8 + x * 18, startY + y * 18));
        for (int x = 0; x < 9; x++)
            addSlot(new Slot(inv, x, 8 + x * 18, startY + 58));
    }

    @Override
    public boolean stillValid(Player p) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
