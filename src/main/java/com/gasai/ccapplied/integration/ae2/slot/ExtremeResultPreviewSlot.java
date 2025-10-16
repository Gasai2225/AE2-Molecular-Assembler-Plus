package com.gasai.ccapplied.integration.ae2.slot;

import net.minecraft.world.item.ItemStack;
import appeng.menu.slot.FakeSlot;
import com.gasai.ccapplied.integration.ae2.menu.ExtremePatternEncodingTermMenu;

/**
 * Специальный слот для отображения превью результата рецепта.
 * Блокирует ручной ввод и автоматически показывает результат крафта.
 */
public class ExtremeResultPreviewSlot extends FakeSlot {
    
    private final ExtremePatternEncodingTermMenu menu;
    
    public ExtremeResultPreviewSlot(appeng.api.inventories.InternalInventory inventory, int slot, ExtremePatternEncodingTermMenu menu) {
        super(inventory, slot);
        this.menu = menu;
        this.setHideAmount(false); // Показываем количество
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        // Блокируем ручной ввод - игрок не может сам положить предмет
        return false;
    }
    
    @Override
    public ItemStack getItem() {
        // Возвращаем превью результата рецепта
        if (menu != null && !menu.isClientSide()) {
            try {
                return menu.getRecipePreview();
            } catch (Exception e) {
                // Логируем ошибку и возвращаем пустой стек
                com.gasai.ccapplied.CCApplied.LOG.warn("Error getting recipe preview in slot", e);
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }
    
    @Override
    public void set(ItemStack stack) {
        // Игнорируем попытки установить предмет вручную
        // Слот обновляется автоматически через getItem()
    }
    
    @Override
    public boolean hasItem() {
        return !getItem().isEmpty();
    }
    
    @Override
    public int getMaxStackSize() {
        return 1; // Результат рецепта обычно один предмет
    }
}
