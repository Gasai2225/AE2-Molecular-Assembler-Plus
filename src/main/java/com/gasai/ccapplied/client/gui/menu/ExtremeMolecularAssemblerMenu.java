package com.gasai.ccapplied.client.gui.menu;

import com.gasai.ccapplied.common.block.entity.ExtremeMolecularAssemblerBlockEntity;
import com.gasai.ccapplied.core.registry.ModItems;
import com.gasai.ccapplied.core.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ExtremeMolecularAssemblerMenu extends AbstractContainerMenu {

    // индексация слотов
    private static final int GRID_START  = 0;    // 81 read-only (0..80)
    private static final int GRID_COUNT  = 81;
    private static final int PATTERN_IDX = GRID_START + GRID_COUNT;           // 81
    private static final int UPGR_START  = PATTERN_IDX + 1;                   // 82..86
    private static final int UPGR_COUNT  = 5;
    private static final int PLAYER_START= UPGR_START + UPGR_COUNT;           // 87..
    private static final int PLAYER_COUNT= 36; // 27+9

    private final ExtremeMolecularAssemblerBlockEntity be;

    /** data[0] = totalTicks, data[1] = ticksLeft (для синка прогресса на клиент) */
    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> be.getCurrentTotalTicks();
                case 1 -> be.getCurrentTicksLeft();
                default -> 0;
            };
        }
        @Override public void set(int i, int v) { /* сервер только читает */ }
        @Override public int getCount() { return 2; }
    };

    /* ---------- SERVER ctor (с BE) ---------- */
    public ExtremeMolecularAssemblerMenu(int id, Inventory playerInv, ExtremeMolecularAssemblerBlockEntity be) {
        super(ModMenus.EXTREME_ASSEMBLER.get(), id);
        this.be = be;
        addDataSlots(this.data);
        buildSlots(playerInv);
    }

    /* ---------- CLIENT ctor (из FriendlyByteBuf) ---------- */
    public ExtremeMolecularAssemblerMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.EXTREME_ASSEMBLER.get(), id);
        BlockPos pos = buf.readBlockPos();
        var be = playerInv.player.level().getBlockEntity(pos);
        if (!(be instanceof ExtremeMolecularAssemblerBlockEntity ema)) {
            throw new IllegalStateException("ExtremeMolecularAssembler BE not found at " + pos);
        }
        this.be = ema;
        addDataSlots(this.data);
        buildSlots(playerInv);
    }

    private void buildSlots(Inventory playerInv) {
        // 9x9 read-only grid
        IItemHandler grid = be.getGridInv();
        int x0 = 18, y0 = 18, dx = 18, dy = 18;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int slotIndex = r * 9 + c;
                addSlot(new ReadonlySlot(grid, slotIndex, x0 + c * dx, y0 + r * dy));
            }
        }

        // pattern
        IItemHandler pat = be.getPatternInv();
        addSlot(new SlotItemHandler(pat, 0, x0 + 9 * dx + 26, y0) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ModItems.EXTREME_PATTERN.get();
            }
        });

        // upgrades (5)
        IItemHandler ups = be.getUpgradesInv();
        int ux = x0 + 9 * dx + 26;
        int uy = y0 + 24;
        for (int i = 0; i < ups.getSlots(); i++) {
            addSlot(new SlotItemHandler(ups, i, ux, uy + i * 18));
        }

        // player inv 27 + hotbar 9
        int baseY = y0 + 9 * dy + 14;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, baseY + r * 18));
            }
        }
        int hotbarY = baseY + 3 * 18 + 4;
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInv, i, 8 + i * 18, hotbarY));
        }
    }

    @Override
    public boolean stillValid(Player p) {
        return p.distanceToSqr(be.getBlockPos().getCenter()) <= 64.0;
    }

    /** для экрана — 0..100% */
    public int getProgressPercent() {
        int total = data.get(0);
        int left  = data.get(1);
        if (total <= 0) return 0;
        int done = Math.max(0, total - Math.max(0, left));
        return Math.min(100, (done * 100) / total);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem().copy();

        // из player -> pattern/upgrades
        if (index >= PLAYER_START && index < PLAYER_START + PLAYER_COUNT) {
            if (stack.getItem() == ModItems.EXTREME_PATTERN.get()) {
                if (moveItemStackTo(stack, PATTERN_IDX, PATTERN_IDX + 1, false)) {
                    slot.setByPlayer(ItemStack.EMPTY);
                    return stack;
                }
            }
            if (moveItemStackTo(stack, UPGR_START, UPGR_START + UPGR_COUNT, false)) {
                slot.setByPlayer(ItemStack.EMPTY);
                return stack;
            }
            // рюкзак<->хотбар
            if (index < PLAYER_START + 27) {
                if (!moveItemStackTo(stack, PLAYER_START + 27, PLAYER_START + 36, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, PLAYER_START, PLAYER_START + 27, false)) return ItemStack.EMPTY;
            }
            slot.setByPlayer(ItemStack.EMPTY);
            return stack;
        }

        // из pattern/upgrades -> player
        if ((index == PATTERN_IDX) || (index >= UPGR_START && index < UPGR_START + UPGR_COUNT)) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_START + PLAYER_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            slot.setByPlayer(ItemStack.EMPTY);
            return stack;
        }

        // grid 0..80 — read-only
        return ItemStack.EMPTY;
    }

    /** read-only слот (для 9×9 отображения) */
    private static class ReadonlySlot extends SlotItemHandler {
        public ReadonlySlot(IItemHandler handler, int index, int x, int y) { super(handler, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }
}
