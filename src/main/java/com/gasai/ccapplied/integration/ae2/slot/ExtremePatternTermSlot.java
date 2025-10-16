package com.gasai.ccapplied.integration.ae2.slot;

import net.minecraft.world.item.ItemStack;
import appeng.menu.slot.FakeSlot;
import com.gasai.ccapplied.CCApplied;

/**
 * Специальный слот для отображения результата экстремального крафта (9x9).
 * Аналогичен PatternTermSlot из AE2, но адаптирован для ExtendedCrafting.
 */
public class ExtremePatternTermSlot extends FakeSlot {
    
    private ItemStack displayedOutput = ItemStack.EMPTY;
    
    public ExtremePatternTermSlot(appeng.api.inventories.InternalInventory inventory, int slot) {
        super(inventory, slot);
        this.setHideAmount(false); // Показываем количество
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        // Блокируем ручной ввод - игрок не может сам положить предмет
        return false;
    }
    
    @Override
    public ItemStack getItem() {
        // Возвращаем отображаемый результат
        return displayedOutput;
    }
    
    @Override
    public void set(ItemStack stack) {
        // Игнорируем попытки установить предмет вручную
        // Слот обновляется автоматически через setDisplayedCraftingOutput()
    }
    
    @Override
    public boolean hasItem() {
        return !displayedOutput.isEmpty();
    }
    
    @Override
    public int getMaxStackSize() {
        return 64; // Максимальный размер стека
    }
    
    /**
     * Устанавливает отображаемый результат крафта.
     * Аналогично setDisplayedCraftingOutput() из PatternTermSlot.
     */
    public void setDisplayedCraftingOutput(ItemStack output) {
        if (output == null) {
            this.displayedOutput = ItemStack.EMPTY;
            return;
        }
        
        this.displayedOutput = output.copy();
        
        // Логируем для отладки
        if (!output.isEmpty()) {
            CCApplied.LOG.debug("[ExtremePatternSlot] Set displayed output: {} x{}", 
                output.getDisplayName().getString(), output.getCount());
        }
    }
    
    /**
     * Получает текущий отображаемый результат.
     */
    public ItemStack getDisplayedCraftingOutput() {
        return displayedOutput.copy();
    }
    
    /**
     * Очищает отображаемый результат.
     */
    public void clearDisplayedOutput() {
        this.displayedOutput = ItemStack.EMPTY;
    }
}
