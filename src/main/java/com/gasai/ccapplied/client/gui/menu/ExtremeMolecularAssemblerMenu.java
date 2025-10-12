package com.gasai.ccapplied.client.gui.menu;

import appeng.api.stacks.AEItemKey;
import com.gasai.ccapplied.common.block.entity.ExtremeMolecularAssemblerBlockEntity;
import com.gasai.ccapplied.common.core.ExtremePatternDetails;
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

    public static final int GRID_START  = 0;
    public static final int GRID_COUNT  = 81;
    public static final int PATTERN_IDX = GRID_START + GRID_COUNT;   // 81
    public static final int OUTPUT_IDX  = PATTERN_IDX + 1;           // 82
    public static final int UPGR_START  = OUTPUT_IDX + 1;            // 83..87
    public static final int UPGR_COUNT  = 5;
    public static final int PLAYER_START= UPGR_START + UPGR_COUNT;   // 88..
    public static final int PLAYER_COUNT= 36;

    private final ExtremeMolecularAssemblerBlockEntity be;

    // === клиентский кэш маски активных слотов (81 бит = 3 * 27) ===
    private final int[] enabledMask = new int[3];

    public int  getPatternSlotIndex() { return PATTERN_IDX; }
    public int  getUpgradesCount()    { return be.getUpgradesInv().getSlots(); }

    /** Клиент спрашивает у меню, активен ли слот согласно текущему паттерну. */
    public boolean isGridSlotEnabledClient(int slot) {
        int part = slot / 27;
        int bit  = slot % 27;
        return (enabledMask[part] & (1 << bit)) != 0;
    }

    // 0: totalTicks, 1: ticksLeft
    private final ContainerData progressData = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> be.getCurrentTotalTicks();
                case 1 -> be.getCurrentTicksLeft();
                default -> 0;
            };
        }
        @Override public void set(int i, int v) { /* client side only */ }
        @Override public int getCount() { return 2; }
    };

    // 3 инт-а с битовой маской активных ячеек 9×9
    private final ContainerData maskData = new ContainerData() {
        @Override public int get(int i) { return be.getEnabledMaskPart(i); } // сервер -> клиент
        @Override public void set(int i, int v) { enabledMask[i] = v; }      // клиент принимает
        @Override public int getCount() { return 3; }
    };

    public ExtremeMolecularAssemblerMenu(int id, Inventory playerInv, ExtremeMolecularAssemblerBlockEntity be) {
        super(ModMenus.EXTREME_ASSEMBLER.get(), id);
        this.be = be;
        addDataSlots(progressData);
        addDataSlots(maskData);
        buildSlots(playerInv);
    }

    public ExtremeMolecularAssemblerMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.EXTREME_ASSEMBLER.get(), id);
        BlockPos pos = buf.readBlockPos();
        var be0 = playerInv.player.level().getBlockEntity(pos);
        if (!(be0 instanceof ExtremeMolecularAssemblerBlockEntity ema)) {
            throw new IllegalStateException("ExtremeMolecularAssembler BE not found at " + pos);
        }
        this.be = ema;
        addDataSlots(progressData);
        addDataSlots(maskData);
        buildSlots(playerInv);
    }

    private void buildSlots(Inventory playerInv) {
        // == 9x9 (+1,+1) ==
        final int gx0 = 9, gy0 = 16, dx = 18, dy = 18;
        IItemHandler grid = be.getGridInv();
        for (int r = 0; r < 9; r++) for (int c = 0; c < 9; c++) {
            int slotIndex = r * 9 + c;
            addSlot(new PatternGridSlot(be, grid, slotIndex, gx0 + c * dx, gy0 + r * dy));
        }

        // == Pattern (+1,+1) ==
        IItemHandler pat = be.getPatternInv();
        addSlot(new SlotItemHandler(pat, 0, 223, 55) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() == ModItems.EXTREME_PATTERN.get(); }
            @Override public int getMaxStackSize() { return 1; }
        });

        // == Output ==
        IItemHandler out = be.getOutputInv();
        addSlot(new SlotItemHandler(out, 0, 223, 88) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player p) { return !getItem().isEmpty(); }
        });

        // == Upgrades (панель 8/8, шаг 18) ==
        final int ux = 275 + 8;
        final int uy0 = 8;
        IItemHandler ups = be.getUpgradesInv();
        for (int i = 0; i < ups.getSlots(); i++) {
            addSlot(new SlotItemHandler(ups, i, ux, uy0 + i * 18) {
                @Override public int getMaxStackSize() { return 1; }
            });
        }

        // == Player inv (+1,+1) ==
        final int invX0 = 55, invY0 = 193;
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(playerInv, c + r * 9 + 9, invX0 + c * 18, invY0 + r * 18));
        final int hotY = 251;
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(playerInv, i, invX0 + i * 18, hotY));
    }

    @Override public boolean stillValid(Player p) {
        return p.distanceToSqr(be.getBlockPos().getCenter()) <= 64.0;
    }

    public int getProgressPercent() {
        int total = progressData.get(0), left = progressData.get(1);
        if (total <= 0) return 0;
        int done = Math.max(0, total - Math.max(0, left));
        return Math.min(100, (done * 100) / total);
    }

    // ==== КЛЮЧЕВОЕ: безопасная раскладка стека по 9×9 ====
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot s = this.slots.get(index);
        if (s == null || !s.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = s.getItem();
        ItemStack original = stack.copy();

        // === из инвентаря игрока ===
        if (index >= PLAYER_START && index < PLAYER_START + PLAYER_COUNT) {

            // Паттерн в свой слот
            if (stack.getItem() == ModItems.EXTREME_PATTERN.get()) {
                if (moveItemStackTo(stack, PATTERN_IDX, PATTERN_IDX + 1, false))
                    return original;
            }

            // Апгрейды
            if (moveItemStackTo(stack, UPGR_START, UPGR_START + UPGR_COUNT, false))
                return original;

            // В 9×9: только по шаблону, по 1 шт., в первую подходящую пустую ячейку.
            boolean movedAny = false;
            while (!stack.isEmpty() && insertStackToGridAccordingToPattern(stack)) {
                movedAny = true;
            }
            if (movedAny) return original;

            // Переброс между рюкзаком/хотбаром
            if (index < PLAYER_START + 27) {
                if (moveItemStackTo(stack, PLAYER_START + 27, PLAYER_START + 36, false))
                    return original;
            } else {
                if (moveItemStackTo(stack, PLAYER_START, PLAYER_START + 27, false))
                    return original;
            }
            return ItemStack.EMPTY;
        }

        // === из машины в игрока ===
        if (index == PATTERN_IDX
                || index == OUTPUT_IDX
                || (index >= UPGR_START && index < UPGR_START + UPGR_COUNT)
                || (index >= GRID_START && index < GRID_START + GRID_COUNT)) {
            if (moveItemStackTo(stack, PLAYER_START, PLAYER_START + PLAYER_COUNT, false))
                return original;
            return ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }


    private boolean insertStackToGridAccordingToPattern(ItemStack stack) {
        if (stack.isEmpty()) return false;

        ExtremePatternDetails epd = be.getCurrentPatternDetails();
        if (epd == null) return false;

        ItemStack[] layout = epd.getLayout81();
        var level = be.getLevel();
        var key = appeng.api.stacks.AEItemKey.of(stack); // для валидации substitutes

        for (int i = 0; i < GRID_COUNT; i++) {
            ItemStack tmpl = layout[i];
            if (tmpl == null || tmpl.isEmpty()) continue;       // этот слот паттерном не используется

            Slot dst = this.slots.get(GRID_START + i);
            if (dst == null || dst.hasItem()) continue;         // занято — дальше

            // Жёсткий матч (Item+NBT) ИЛИ (если substitutes) через AEKey
            boolean matches = ItemStack.isSameItemSameTags(tmpl, stack)
                    || (key != null && epd.isValidForSlot(i, key, level));

            if (!matches) continue;

            ItemStack one = stack.split(1);
            dst.set(one);
            dst.setChanged();
            this.broadcastChanges();
            return true;
        }
        return false;
    }

    /** Кладём по 1 штуке в пустые и «разрешённые» слоты сетки. */

    /** 9×9: активность и валидация зависят от текущего паттерна. */
    private static class PatternGridSlot extends SlotItemHandler {
        private final ExtremeMolecularAssemblerBlockEntity be;

        public PatternGridSlot(ExtremeMolecularAssemblerBlockEntity be, IItemHandler inv, int index, int x, int y) {
            super(inv, index, x, y);
            this.be = be;
        }
        @Override public boolean mayPlace(ItemStack stack) { return be.isItemValidForGrid(getSlotIndex(), stack); }
        @Override public boolean mayPickup(Player player)  { return !getItem().isEmpty(); }
        @Override public int getMaxStackSize()             { return 1; }
        @Override public boolean isActive() {
            // не подсвечиваем пустые неактивные (но если предмет лежит — позволим забрать)
            ExtremePatternDetails epd = be.getCurrentPatternDetails();
            boolean enabled = epd != null && epd.isSlotEnabled(getSlotIndex());
            return enabled || !getItem().isEmpty();
        }
    }
}
