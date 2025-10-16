package com.gasai.ccapplied.integration.ae2.adapters;

import appeng.menu.slot.FakeSlot;
import appeng.util.ConfigInventory;

/**
 * Адаптер для экстремальной сетки крафта 9x9.
 * Преобразует ConfigInventory в FakeSlot массив для GUI.
 */
public class ExtremeCraftingGridAdapter {
    
    private final ConfigInventory configInventory;
    private final FakeSlot[] slots;
    private final int gridSize = 9;
    private final int totalSlots = gridSize * gridSize; // 81
    
    public ExtremeCraftingGridAdapter(ConfigInventory configInventory) {
        this.configInventory = configInventory;
        this.slots = new FakeSlot[totalSlots];
        initializeSlots();
    }
    
    private void initializeSlots() {
        var wrapper = configInventory.createMenuWrapper();
        for (int i = 0; i < totalSlots; i++) {
            slots[i] = new FakeSlot(wrapper, i);
            slots[i].setHideAmount(true); // Скрываем количество для визуального эффекта
        }
    }
    
    /**
     * Получить все слоты сетки
     */
    public FakeSlot[] getSlots() {
        return slots;
    }
    
    /**
     * Получить слот по координатам (x, y) в сетке 9x9
     */
    public FakeSlot getSlot(int x, int y) {
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
            return null;
        }
        int index = y * gridSize + x;
        return slots[index];
    }
    
    /**
     * Получить слот по индексу
     */
    public FakeSlot getSlot(int index) {
        if (index < 0 || index >= totalSlots) {
            return null;
        }
        return slots[index];
    }
    
    /**
     * Очистить всю сетку
     */
    public void clearGrid() {
        for (int i = 0; i < totalSlots; i++) {
            configInventory.setStack(i, null);
        }
    }
    
    /**
     * Проверить, есть ли хотя бы один предмет в сетке
     */
    public boolean hasAnyItems() {
        for (int i = 0; i < totalSlots; i++) {
            if (configInventory.getStack(i) != null) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Получить количество заполненных слотов
     */
    public int getFilledSlotCount() {
        int count = 0;
        for (int i = 0; i < totalSlots; i++) {
            if (configInventory.getStack(i) != null) {
                count++;
            }
        }
        return count;
    }
}
