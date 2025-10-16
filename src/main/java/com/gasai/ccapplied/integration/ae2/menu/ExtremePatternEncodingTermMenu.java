package com.gasai.ccapplied.integration.ae2.menu;

import com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost;
import com.gasai.ccapplied.CCApplied;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.slot.FakeSlot;
import appeng.menu.SlotSemantics;

import com.gasai.ccapplied.integration.ae2.part.ExtremePatternEncodingLogic;
import com.gasai.ccapplied.integration.ae2.slot.ExtremeBlankPatternSlot;
import com.gasai.ccapplied.integration.ae2.slot.ExtremeEncodedPatternSlot;
import com.gasai.ccapplied.integration.ae2.slot.ExtremeCraftingTermSlot;
import com.gasai.ccapplied.integration.ae2.logic.ExtremePatternEncoder;
import com.gasai.ccapplied.core.registry.CCItems;

import java.util.List;

/**
 * Меню терминала кодирования экстремальных паттернов (9×9).
 * Только режим "крафтинг": inputs[81] + output[1].
 */
public class ExtremePatternEncodingTermMenu extends MEStorageMenu implements appeng.helpers.IMenuCraftingPacket {

    public static final int GRID = 9;
    public static final int SLOTS = GRID * GRID;

    public static final String ACTION_ENCODE = "ext_encode";
    public static final String ACTION_CLEAR  = "ext_clear";

    public static final MenuType<ExtremePatternEncodingTermMenu> TYPE =
    MenuTypeBuilder.create(
        ExtremePatternEncodingTermMenu::new,
        IExtremePatternTerminalMenuHost.class
    ).build("extreme_patternterm");

    private final ExtremePatternEncodingLogic logic;

    private final FakeSlot[] inputSlots = new FakeSlot[SLOTS];
    private final ExtremeCraftingTermSlot outputSlot;

    private final Slot blankPatternSlot;
    private final Slot encodedPatternSlot;

    private final appeng.util.ConfigInventory encodedInputsInv;
    private final appeng.util.ConfigInventory encodedOutputsInv;

    @GuiSync(10)
    public boolean uiActive = true; // пример синка для клиента (на свой экран)

    @GuiSync(11)
    public boolean networkConnected = false; // статус сети

    @GuiSync(12)
    public int patternInputCount = 0; // количество входных слотов

    @GuiSync(13)
    public int patternOutputCount = 0; // количество выходных слотов

    public ExtremePatternEncodingTermMenu(int id, Inventory inv, IExtremePatternTerminalMenuHost host) {
        this(TYPE, id, inv, host, true);}

    public ExtremePatternEncodingTermMenu(MenuType<?> type, int id, Inventory inv, IExtremePatternTerminalMenuHost host, boolean bindInv) {
        super(type, id, inv, host, bindInv);

        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] ctor id={} player={} host={} bindInv={} on {}", 
            id, inv.player.getGameProfile().getName(), host, bindInv, 
            inv.player.level().isClientSide() ? "CLIENT" : "SERVER");

        this.logic = host.getLogic();      

        this.encodedInputsInv  = logic.getEncodedInputInv();
        this.encodedOutputsInv = logic.getEncodedOutputInv();

        var inputsWrapper  = encodedInputsInv.createMenuWrapper();
        var outputsWrapper = encodedOutputsInv.createMenuWrapper();

        // 81 входной фейк-слот (9x9 сетка)
        for (int i = 0; i < SLOTS; i++) {
            var fs = new FakeSlot(inputsWrapper, i);
            fs.setHideAmount(true);
            // Располагаем слоты в правильном порядке для сетки 9x9
            this.inputSlots[i] = fs;
            this.addSlot(fs, SlotSemantics.CRAFTING_GRID);
        }

        // 1 выходной слот с превью результата экстремального крафта
        var out = new ExtremeCraftingTermSlot(
            inv.player, 
            this.getActionSource(), 
            this.powerSource,
            host.getInventory(), 
            inputsWrapper, 
            this,
            outputsWrapper, 
            0
        );
        this.addSlot(this.outputSlot = out, SlotSemantics.CRAFTING_RESULT);
        
        // Логируем создание слота
        CCApplied.LOG.info("[ExtremeMenu] ExtremeCraftingTermSlot created and added to menu");
        
        // ТЕСТ: Принудительно обновляем слот результата для проверки
        CCApplied.LOG.info("[ExtremeMenu] Testing output slot - getItem(): {}", 
            this.outputSlot.getItem().isEmpty() ? "empty" : this.outputSlot.getItem().getDisplayName().getString());

        // реальные слоты бланка/энкодед
        this.blankPatternSlot = this.addSlot(
                new ExtremeBlankPatternSlot(logic.getBlankPatternInv(), 0),
                SlotSemantics.BLANK_PATTERN
        );
        this.encodedPatternSlot = this.addSlot(
                new ExtremeEncodedPatternSlot(logic.getEncodedPatternInv(), 0),
                SlotSemantics.ENCODED_PATTERN
        );

        registerClientAction(ACTION_ENCODE, this::encode);
        registerClientAction(ACTION_CLEAR,  this::clearAll);
        registerClientAction("extremeEncodePattern", this::extremeEncodePattern);
        registerClientAction("extremeClearPattern", this::extremeClearPattern);
        registerClientAction("extremeCraftingClearPattern", this::extremeCraftingClearPattern);
        
        // Инициализируем поля @GuiSync
        this.uiActive = true;
        this.networkConnected = false;
        this.patternInputCount = 0;
        this.patternOutputCount = 0;
        
        // ТЕСТ: Принудительно обновляем меню для проверки работы слота
        CCApplied.LOG.info("[ExtremeMenu] Forcing broadcastChanges() to test slot");
        this.broadcastChanges();
    }

    /* -------------------- методы обновления -------------------- */
    
    @Override
    public void setItem(int slotID, int stateId, ItemStack stack) {
        super.setItem(slotID, stateId, stack);
        // Слот результата обновляется автоматически
    }

    /* -------------------- действия -------------------- */

    public void encode() {
        if (isClientSide()) {
            sendClientAction(ACTION_ENCODE);
            return;
        }

        // Временно отключено для диагностики краша
        // Получаем превью результата рецепта
        // ItemStack recipeResult = getRecipePreview();
        // if (recipeResult.isEmpty()) {
        //     com.gasai.ccapplied.CCApplied.LOG.warn("[ExtremeMenu] No valid recipe found for current grid");
        //     return;
        // }
        
        // // Устанавливаем результат рецепта в выходной слот для кодирования
        // var recipeOutput = AEItemKey.of(recipeResult);
        // if (recipeOutput != null) {
        //     encodedOutputsInv.setStack(0, new GenericStack(recipeOutput, recipeResult.getCount()));
        // }

        // Используем новую логику кодирования
        var result = ExtremePatternEncoder.encodePattern(
            encodedInputsInv, 
            encodedOutputsInv,
            blankPatternSlot.getItem(),
            encodedPatternSlot.getItem(),
            getPlayer().level()
        );
        
        if (result.isSuccess()) {
            // Успешно закодировали паттерн
            ItemStack encodedPattern = result.getPattern();
            
            // Проверяем, нужно ли использовать бланк
            var currentPattern = encodedPatternSlot.getItem();
            if (currentPattern.isEmpty()) {
                // Используем бланк
                var blank = blankPatternSlot.getItem();
                if (isOurBlank(blank)) {
                    blank.shrink(1);
                    if (blank.isEmpty()) {
                        blankPatternSlot.set(ItemStack.EMPTY);
                    }
                }
            }
            
            // Устанавливаем закодированный паттерн
            encodedPatternSlot.set(encodedPattern);
            broadcastChanges();
            
            com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] Pattern encoded successfully");
        } else {
            // Ошибка кодирования
            com.gasai.ccapplied.CCApplied.LOG.warn("[ExtremeMenu] Failed to encode pattern: {}", 
                result.getErrorMessage().getString());
        }
    }

    public void clearAll() {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR);
            return;
        }
        
        // Используем новую логику очистки
        ExtremePatternEncoder.clearInputSlots(encodedInputsInv);
        ExtremePatternEncoder.clearOutputSlots(encodedOutputsInv);
        
        // Слот результата обновляется автоматически
        broadcastChanges();
        
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] Grid cleared");
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        
        // Логируем вызов broadcastChanges
        CCApplied.LOG.debug("[ExtremeMenu] broadcastChanges called, outputSlot item: {}", 
            this.outputSlot.getItem().isEmpty() ? "empty" : this.outputSlot.getItem().getDisplayName().getString());
        
        // Синхронизируем GUI поля с клиентом
        // broadcastFullUpdate() не существует, используем стандартный механизм
    }

    /* -------------------- утилиты -------------------- */

    private static boolean isOurBlank(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == CCItems.EXTREME_BLANK_PATTERN.get();
    }

    // Методы для поддержки виджетов
    public boolean canEncode() {
        if (isClientSide()) {
            return !this.blankPatternSlot.getItem().isEmpty() &&
                   this.patternInputCount > 0 &&
                   this.patternOutputCount > 0;
        }
        
        // На сервере дополнительно проверяем валидность рецепта
        return !this.blankPatternSlot.getItem().isEmpty() &&
               this.patternInputCount > 0 &&
               this.patternOutputCount > 0 &&
               !getRecipePreview().isEmpty(); // Есть валидный рецепт
    }

    public boolean hasPattern() {
        return this.patternInputCount > 0 || this.patternOutputCount > 0;
    }

    public boolean isNetworkConnected() {
        return this.networkConnected;
    }

    public int getPatternInputCount() {
        return this.patternInputCount;
    }

    public int getPatternOutputCount() {
        return this.patternOutputCount;
    }
    
    /**
     * Получает и обновляет превью результата рецепта для текущей сетки.
     * Аналогично getAndUpdateOutput() из PatternEncodingTermMenu.
     */
    public ItemStack getAndUpdateOutput() {
        // Теперь результат вычисляется динамически в слоте
        return this.outputSlot.getDisplayedCraftingOutput();
    }
    
    /**
     * Получает превью результата рецепта для текущей сетки
     */
    public ItemStack getRecipePreview() {
        if (isClientSide()) {
            return ItemStack.EMPTY;
        }
        
        if (encodedInputsInv == null || getPlayer() == null || getPlayer().level() == null) {
            return ItemStack.EMPTY;
        }
        
        try {
            ItemStack[] craftingGrid = new ItemStack[SLOTS];
            for (int i = 0; i < SLOTS; i++) {
                var stack = encodedInputsInv.getStack(i);
                if (stack != null && stack.what() instanceof appeng.api.stacks.AEItemKey itemKey) {
                    craftingGrid[i] = itemKey.toStack((int) stack.amount());
                } else {
                    craftingGrid[i] = ItemStack.EMPTY;
                }
            }
            
            ItemStack result = com.gasai.ccapplied.integration.extendedcrafting.ExtendedCraftingRecipeHelper.getRecipePreview(craftingGrid, getPlayer().level());
            return result != null ? result : ItemStack.EMPTY;
        } catch (Exception e) {
            com.gasai.ccapplied.CCApplied.LOG.warn("Error getting recipe preview", e);
            return ItemStack.EMPTY;
        }
    }


    @Override
    protected ItemStack transferStackToMenu(ItemStack input) {
        // пополнить бланк
        if (blankPatternSlot.mayPlace(input)) {
            input = blankPatternSlot.safeInsert(input);
            if (input.isEmpty()) return ItemStack.EMPTY;
        }
        // пополнить encoded
        if (encodedPatternSlot.mayPlace(input)) {
            input = encodedPatternSlot.safeInsert(input);
            if (input.isEmpty()) return ItemStack.EMPTY;
        }
        return super.transferStackToMenu(input);
    }

    /** Для «крафтинг-пакета» AE2: отдаём первые 81 слота как матрицу. */
    @Override public InternalInventory getCraftingMatrix() {
        return encodedInputsInv.createMenuWrapper().getSubInventory(0, SLOTS);
    }

    @Override
    public boolean useRealItems() {
        return false;
    }

    public FakeSlot[] getInputSlots() { return inputSlots; }
    public ExtremeCraftingTermSlot getOutputSlot() { return outputSlot; }

    // Методы для виджетов
    public boolean canEncodePattern() {
        // Проверяем наличие входных предметов и валидного рецепта
        boolean hasInputs = false;
        for (int i = 0; i < SLOTS; i++) {
            if (encodedInputsInv.getStack(i) != null) {
                hasInputs = true;
                break;
            }
        }
        
        if (!hasInputs) return false;
        
        // Проверяем, есть ли валидный рецепт для текущей сетки
        if (isClientSide()) {
            return true; // На клиенте просто проверяем входы
        }
        
        return !getRecipePreview().isEmpty(); // На сервере проверяем валидность рецепта
    }

    public boolean hasItemsInGrid() {
        return canEncodePattern();
    }

    public boolean hasNetworkConnection() {
        return getNetworkNode() != null;
    }

    public int getFilledSlotCount() {
        int count = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (encodedInputsInv.getStack(i) != null) {
                count++;
            }
        }
        return count;
    }

    public int getTotalSlotCount() {
        return SLOTS;
    }

    @Override
    public void onSlotChange(Slot slot) {
        // Логируем изменение слота
        CCApplied.LOG.debug("[ExtremeMenu] onSlotChange called for slot: {}", slot.getClass().getSimpleName());
        
        // Если изменился слот в сетке крафта, принудительно обновляем результат
        if (slot instanceof FakeSlot && isInputSlot(slot)) {
            CCApplied.LOG.debug("[ExtremeMenu] Input slot changed, forcing output update");
            // Принудительно обновляем слот результата
            this.outputSlot.setChanged();
            broadcastChanges();
        }
    }
    
    /**
     * Проверяет, является ли слот входным слотом сетки крафта
     */
    private boolean isInputSlot(Slot slot) {
        for (FakeSlot inputSlot : this.inputSlots) {
            if (inputSlot == slot) {
                return true;
            }
        }
        return false;
    }
    

    // Реализация методов из IMenuCraftingPacket
    @Override
    public List<ItemStack> getViewCells() {
        return List.of(); // Для экстремального терминала view cells не используются
    }

    @Override
    public appeng.api.networking.IGridNode getNetworkNode() {
        if (getTarget() instanceof appeng.api.parts.IPart part) {
            return part.getGridNode();
        }
        return null;
    }

    // Методы для обработки действий виджетов
    public void extremeEncodePattern() {
        if (isClientSide()) {
            sendClientAction("extremeEncodePattern");
            return;
        }
        encode();
    }

    public void extremeClearPattern() {
        if (isClientSide()) {
            sendClientAction("extremeClearPattern");
            return;
        }
        clearAll();
    }

    public void extremeCraftingClearPattern() {
        if (isClientSide()) {
            sendClientAction("extremeCraftingClearPattern");
            return;
        }
        clearAll();
    }

}

