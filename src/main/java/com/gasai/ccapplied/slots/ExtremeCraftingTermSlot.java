package com.gasai.ccapplied.slots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import appeng.helpers.IMenuCraftingPacket;
import appeng.menu.slot.FakeSlot;
import com.gasai.ccapplied.CCApplied;

/**
 * Слот для отображения результата экстремального крафта (9x9).
 * Аналогичен CraftingTermSlot из AE2, но адаптирован для ExtendedCrafting.
 */
public class ExtremeCraftingTermSlot extends FakeSlot {
    
    private final Player player;
    private final InternalInventory craftingMatrix;
    
    
    public ExtremeCraftingTermSlot(Player player, IActionSource actionSource, IEnergySource energySource,
                                  MEStorage storage, InternalInventory craftingMatrix, 
                                  IMenuCraftingPacket menu, InternalInventory inventory, int slot) {
        super(inventory, slot);
        this.player = player;
        this.craftingMatrix = craftingMatrix;
        this.setHideAmount(false);
        
        // Логируем для отладки
        CCApplied.LOG.debug("[ExtremeCraftingSlot] Created slot with player={}, matrix={}", 
            player != null ? player.getName().getString() : "null",
            craftingMatrix != null ? "valid" : "null");
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        // Блокируем ручной ввод - игрок не может сам положить предмет
        return false;
    }
    
    @Override
    public ItemStack getItem() {
        // Вычисляем результат крафта каждый раз (без кэширования)
        ItemStack result = getCraftingResult();
        
        // Гарантируем, что результат не null
        ItemStack finalResult = result != null ? result : ItemStack.EMPTY;
        
        // ВАЖНО: Сохраняем результат в базовом слоте для синхронизации с клиентом
        super.set(finalResult);
        
        return finalResult;
    }
    
    @Override
    public void set(ItemStack stack) {
        // Игнорируем попытки установить предмет вручную
        // Слот обновляется автоматически через getItem()
    }
    
    @Override
    public boolean hasItem() {
        return !getCraftingResult().isEmpty();
    }
    
    @Override
    public int getMaxStackSize() {
        return 64;
    }
    
    /**
     * Вычисляет результат крафта на основе текущей матрицы.
     */
    private ItemStack getCraftingResult() {
        try {
            // Проверяем, что игрок и мир доступны
            if (player == null || player.level() == null || craftingMatrix == null) {
                return ItemStack.EMPTY;
            }
            
            // Создаем массив для 9x9 сетки
            ItemStack[] craftingGrid = new ItemStack[81]; // 9x9 = 81 слот
            
            // Заполняем массив из crafting matrix
            int nonEmptySlots = 0;
            for (int i = 0; i < Math.min(craftingGrid.length, craftingMatrix.size()); i++) {
                ItemStack stack = craftingMatrix.getStackInSlot(i);
                craftingGrid[i] = stack != null ? stack : ItemStack.EMPTY;
                if (!craftingGrid[i].isEmpty()) {
                    nonEmptySlots++;
                }
            }
            
            // Получаем превью рецепта из ExtendedCrafting
            ItemStack result = com.gasai.ccapplied.crafting.ExtendedCraftingRecipeHelper.getRecipePreview(
                craftingGrid, player.level());
            
            // Гарантируем, что результат не null
            ItemStack finalResult = result != null ? result : ItemStack.EMPTY;
            return finalResult;
                
        } catch (Exception e) {
            CCApplied.LOG.warn("Error computing crafting result", e);
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * Устанавливает отображаемый результат (для совместимости).
     */
    public void setDisplayedCraftingOutput(ItemStack output) {
        // В этой реализации мы не храним результат, а вычисляем его динамически
        // Этот метод оставлен для совместимости
    }
    
    /**
     * Получает отображаемый результат.
     */
    public ItemStack getDisplayedCraftingOutput() {
        return getCraftingResult();
    }
    
    /**
     * Очищает отображаемый результат.
     */
    public void clearDisplayedOutput() {
        // В этой реализации мы не храним результат, а вычисляем его динамически
        // Этот метод оставлен для совместимости
    }
    
    /**
     * Инициализация слота (для совместимости с PatternTermSlot).
     */
    public void initialize(ItemStack stack) {
        // Игнорируем инициализацию от сервера, результат вычисляется на клиенте
    }
    
    
}
