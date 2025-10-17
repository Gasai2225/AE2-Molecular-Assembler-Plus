package com.gasai.ccapplied.integration.ae2.menu;

import com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost;
import com.gasai.ccapplied.CCApplied;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

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

public class ExtremePatternEncodingTermMenu extends MEStorageMenu implements appeng.helpers.IMenuCraftingPacket, appeng.util.inv.InternalInventoryHost {

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
    
    // Реальная сетка крафта для хранения данных игрока
    private final appeng.util.inv.AppEngInternalInventory craftingMatrix;

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
        
        // Создаем реальную сетку крафта для хранения данных игрока
        this.craftingMatrix = new appeng.util.inv.AppEngInternalInventory(this, SLOTS);

        var inputsWrapper  = encodedInputsInv.createMenuWrapper();
        var outputsWrapper = encodedOutputsInv.createMenuWrapper();

        // 81 входной фейк-слот (9x9 сетка) - теперь ссылаются на реальную сетку крафта
        for (int i = 0; i < SLOTS; i++) {
            var fs = new FakeSlot(craftingMatrix, i);
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
            craftingMatrix, // Используем реальную сетку крафта
            this,
            outputsWrapper, 
            0
        );
        this.addSlot(this.outputSlot = out, SlotSemantics.CRAFTING_RESULT);
        
        // Логируем создание слота
        CCApplied.LOG.info("[ExtremeMenu] ExtremeCraftingTermSlot created and added to menu");

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
        // Приём сетки из JEI (центровка n×m во внутреннюю 9×9)
        registerClientAction("jei_apply_grid", JeiGridData.class, data -> {
            if (isClientSide()) return;
            applyCenteredGridFromClient(data.width, data.height, data.items);
        });
        registerClientAction("extremeEncodePattern", this::extremeEncodePattern);
        registerClientAction("extremeClearPattern", this::extremeClearPattern);
        registerClientAction("extremeCraftingClearPattern", this::extremeCraftingClearPattern);
        
        // Инициализируем поля @GuiSync
        this.uiActive = true;
        this.networkConnected = false;
        this.patternInputCount = 0;
        this.patternOutputCount = 0;

        // При открытии меню загружаем сохранённое состояние фейк-слотов из логики в реальную матрицу
        if (!isClientSide()) {
            loadCraftingMatrixFromLogic();
        }
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

        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] encode() called");

        // Получаем превью результата рецепта
        ItemStack recipeResult = getRecipePreview();
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] Recipe result: {}", 
            recipeResult.isEmpty() ? "empty" : recipeResult.getDisplayName().getString());
        
        if (recipeResult.isEmpty()) {
            com.gasai.ccapplied.CCApplied.LOG.warn("[ExtremeMenu] No valid recipe found for current grid");
            return;
        }
        
        // Заполняем логику кодирования данными из craftingMatrix
        var encodingLogic = ((com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost) getHost()).getLogic();
        encodingLogic.fillFromCraftingMatrix(craftingMatrix, recipeResult);
        
        // Кодируем паттерн используя существующую логику
        boolean success = encodingLogic.encodePattern();
        
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] Encode result: {}", success ? "success" : "failure");
        
        if (!success) {
            com.gasai.ccapplied.CCApplied.LOG.warn("[ExtremeMenu] Failed to encode pattern using ExtremePatternEncodingLogic");
        }
    }

    public void clearAll() {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR);
            return;
        }
        
        // Очищаем все слоты
        for (int i = 0; i < SLOTS; i++) {
            inputSlots[i].set(ItemStack.EMPTY);
        }
        outputSlot.set(ItemStack.EMPTY);
        
        // Очищаем логику кодирования
        var encodingLogic = ((com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost) getHost()).getLogic();
        encodingLogic.clearCraftingGrid();
        
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] Grid cleared");
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        
        // Синхронизируем GUI поля с клиентом
        // broadcastFullUpdate() не существует, используем стандартный механизм
    }

    /* -------------------- утилиты -------------------- */

    private static boolean isOurBlank(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == CCItems.EXTREME_BLANK_PATTERN.get();
    }

    // Методы для поддержки виджетов
    public boolean canEncode() {
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] canEncode() called");
        
        // Проверяем наличие бланка паттерна
        if (this.blankPatternSlot.getItem().isEmpty()) {
            com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] canEncode: no blank pattern");
            return false;
        }
        
        // Проверяем, есть ли предметы в сетке крафта
        boolean hasInputItems = false;
        int inputItemCount = 0;
        if (inputSlots != null) {
            for (FakeSlot slot : inputSlots) {
                if (!slot.getItem().isEmpty()) {
                    hasInputItems = true;
                    inputItemCount++;
                }
            }
        }
        
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] canEncode: {} input items found", inputItemCount);
        
        if (!hasInputItems) {
            com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] canEncode: no input items");
            return false;
        }
        
        // На сервере дополнительно проверяем валидность рецепта
        if (!isClientSide()) {
            ItemStack recipeResult = getRecipePreview();
            boolean hasValidRecipe = !recipeResult.isEmpty();
            com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] canEncode: valid recipe = {}, result = {}", 
                hasValidRecipe, hasValidRecipe ? recipeResult.getDisplayName().getString() : "none");
            return hasValidRecipe; // Есть валидный рецепт
        }
        
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] canEncode: client side, returning true");
        return true; // На клиенте достаточно наличия предметов
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
        
        if (inputSlots == null || getPlayer() == null || getPlayer().level() == null) {
            com.gasai.ccapplied.CCApplied.LOG.debug("[ExtremeMenu] getRecipePreview: null check failed");
            return ItemStack.EMPTY;
        }
        
        try {
            // Попробуем использовать данные из outputSlot (ExtremeCraftingTermSlot)
            // который должен содержать актуальные данные сетки крафта
            if (outputSlot != null && outputSlot instanceof ExtremeCraftingTermSlot) {
                ItemStack result = outputSlot.getItem();
                if (!result.isEmpty()) {
                    com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeMenu] getRecipePreview: using data from outputSlot: {}", result.getDisplayName().getString());
                    return result;
                }
            }
            
            // Fallback: читаем из inputSlots как раньше
            ItemStack[] craftingGrid = new ItemStack[SLOTS];
            int nonEmptySlots = 0;
            for (int i = 0; i < SLOTS; i++) {
                craftingGrid[i] = inputSlots[i].getItem();
                if (!craftingGrid[i].isEmpty()) {
                    nonEmptySlots++;
                }
            }
            
            com.gasai.ccapplied.CCApplied.LOG.debug("[ExtremeMenu] getRecipePreview: {} non-empty slots out of {}", nonEmptySlots, SLOTS);
            
            ItemStack result = com.gasai.ccapplied.integration.extendedcrafting.ExtendedCraftingRecipeHelper.getRecipePreview(craftingGrid, getPlayer().level());
            com.gasai.ccapplied.CCApplied.LOG.debug("[ExtremeMenu] getRecipePreview: result = {}", 
                result != null ? result.getDisplayName().getString() : "null");
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
        // Если изменился слот в сетке крафта, обновляем результат
        if (slot instanceof FakeSlot && isInputSlot(slot)) {
            // Синхронизируем изменение с персистентной логикой (encodedInputsInv)
            if (!isClientSide()) {
                int slotIndex = slot.getContainerSlot();
                if (slotIndex >= 0 && slotIndex < SLOTS) {
                    var stack = craftingMatrix.getStackInSlot(slotIndex);
                    if (stack.isEmpty()) {
                        encodedInputsInv.setStack(slotIndex, null);
                    } else {
                        var key = appeng.api.stacks.AEItemKey.of(stack);
                        if (key != null) {
                            encodedInputsInv.setStack(slotIndex, new appeng.api.stacks.GenericStack(key, 1));
                        } else {
                            encodedInputsInv.setStack(slotIndex, null);
                        }
                    }
                }
            }
        }
        
        // Если изменился слот для закодированных паттернов
        if (slot == encodedPatternSlot && !isClientSide()) {
            var patternStack = slot.getItem();
            if (!patternStack.isEmpty()) {
                // Загружаем рецепт из паттерна в сетку крафта
                var encodingLogic = ((com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost) getHost()).getLogic();
                encodingLogic.loadPatternIntoMatrix(craftingMatrix, patternStack);
                CCApplied.LOG.info("[ExtremeMenu] Loaded pattern into crafting matrix");
            } else {
                // Если слот пустой (паттерн извлечен), очищаем сетку
                clearAll();
                CCApplied.LOG.info("[ExtremeMenu] Pattern removed, cleared crafting matrix");
            }
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

    /** Данные для JEI переноса (Gson-friendly DTO) */
    public static class JeiGridData {
        public final int width;
        public final int height;
        public final java.util.List<JeiItem> items;

        public JeiGridData(int width, int height, java.util.List<JeiItem> items) {
            this.width = width;
            this.height = height;
            this.items = items;
        }
    }

    /** Один предмет для JEI переноса */
    public static class JeiItem {
        public final String snbt; // полная SNBT ItemStack (с id/Count/nbt)

        public JeiItem(String snbt) {
            this.snbt = snbt;
        }
    }

    /** Применяет плоский список размера w*h, центрируя в нашей 9x9 матрице. */
    private void applyCenteredGridFromClient(int w, int h, java.util.List<JeiItem> items) {
        if (isClientSide()) return;
        if (w <= 0 || h <= 0) return;
        final boolean isShapeless = w == 1;
        // Для shaped ограничиваемся 9×9, для shapeless разрешаем любую высоту (обрежем до 81)
        if (!isShapeless && (w > GRID || h > GRID)) return;

        // Очистка матрицы (QoL: всегда сбрасываем старый рецепт перед записью нового)
        try {
            for (int i = 0; i < SLOTS; i++) {
                craftingMatrix.extractItem(i, Integer.MAX_VALUE, false);
                craftingMatrix.insertItem(i, ItemStack.EMPTY, false);
                encodedInputsInv.setStack(i, null);
            }
            // Сохраняем очистку немедленно, чтобы клиент увидел пустую сетку до заполнения
            logic.getEncodedInputInv().beginBatch();
            for (int i = 0; i < SLOTS; i++) logic.getEncodedInputInv().setStack(i, null);
            logic.getEncodedInputInv().endBatch();
        } catch (Exception e) {
            CCApplied.LOG.warn("[ExtremeMenu] Failed to clear grid before JEI apply", e);
        }

        // Декодируем все стаки заранее
        java.util.List<ItemStack> decoded = new java.util.ArrayList<>(items.size());
        for (var ji : items) {
            ItemStack st = ItemStack.EMPTY;
            try {
                var tag = net.minecraft.nbt.TagParser.parseTag(ji.snbt);
                if (tag instanceof net.minecraft.nbt.CompoundTag) {
                    st = ItemStack.of((net.minecraft.nbt.CompoundTag) tag);
                }
            } catch (Exception ignored) { }
            decoded.add(st);
        }

        if (isShapeless) {
            // Shapeless: заполняем по порядку с первого слота (0,0), слева-направо, сверху-вниз
            int limit = Math.min(decoded.size(), SLOTS);
            for (int i = 0; i < limit; i++) {
                ItemStack st = decoded.get(i);
                if (st == null || st.isEmpty()) continue;
                int x = i % GRID;
                int y = i / GRID;
                int cmIndex = x + y * GRID;
                craftingMatrix.insertItem(cmIndex, st.copy(), false);
                var key = appeng.api.stacks.AEItemKey.of(st);
                if (key != null) {
                    encodedInputsInv.setStack(cmIndex, new appeng.api.stacks.GenericStack(key, st.getCount()));
                }
            }
        } else {
            // Shaped: центрируем w×h во внутреннюю 9×9
            int startX = (GRID - w) / 2;
            int startY = (GRID - h) / 2;
            int idx = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (idx >= decoded.size()) break;
                    ItemStack st = decoded.get(idx++);
                    if (st != null && !st.isEmpty()) {
                        int cmIndex = (startX + x) + (startY + y) * GRID;
                        if (cmIndex >= 0 && cmIndex < SLOTS) {
                            craftingMatrix.insertItem(cmIndex, st.copy(), false);
                            var key = appeng.api.stacks.AEItemKey.of(st);
                            if (key != null) {
                                encodedInputsInv.setStack(cmIndex, new appeng.api.stacks.GenericStack(key, st.getCount()));
                            }
                        }
                    }
                }
            }
        }

        // Обновим превью и сохраним логику
        var encodingLogic = ((com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost) getHost()).getLogic();
        encodingLogic.fixExtremeCraftingGrid();
        CCApplied.LOG.info("[ExtremeMenu] JEI grid applied: {}x{} items={} (mode={})", w, h, items.size(), (w==1?"shapeless":"shaped"));
    }

    /** Публичный мост для клиентских интеграций (JEI) */
    public void requestApplyJeiGrid(int w, int h, java.util.List<ItemStack> items) {
        // Конвертируем ItemStack в полную SNBT-представление
        var list = new java.util.ArrayList<JeiItem>(items.size());
        for (var st : items) {
            if (st == null || st.isEmpty()) { list.add(new JeiItem("{}")); continue; }
            var tag = new net.minecraft.nbt.CompoundTag();
            st.save(tag); // включает id, Count и весь nbt
            list.add(new JeiItem(tag.toString()));
        }
        sendClientAction("jei_apply_grid", new JeiGridData(w, h, list));
    }

    /** Загружает сохранённые в логике (NBT) инпуты в реальную матрицу фейк-слотов. */
    private void loadCraftingMatrixFromLogic() {
        try {
            for (int i = 0; i < SLOTS; i++) {
                var gs = encodedInputsInv.getStack(i);
                if (gs == null || !appeng.api.stacks.AEItemKey.is(gs.what())) {
                    craftingMatrix.insertItem(i, ItemStack.EMPTY, false);
                    continue;
                }
                var key = (appeng.api.stacks.AEItemKey) gs.what();
                int amount = (int) Math.max(1, Math.min(gs.amount(), 64));
                ItemStack st = key.toStack(amount);
                craftingMatrix.insertItem(i, st, false);
            }
        } catch (Exception e) {
            CCApplied.LOG.warn("[ExtremeMenu] Failed to load crafting matrix from logic", e);
        }
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
    
    // Реализация InternalInventoryHost
    @Override
    public void onChangeInventory(appeng.api.inventories.InternalInventory inv, int slot) {
        // Обновляем превью при изменении сетки крафта
        if (inv == craftingMatrix) {
            com.gasai.ccapplied.CCApplied.LOG.debug("[ExtremeMenu] Crafting matrix changed at slot {}", slot);
            // Можно добавить логику для обновления превью
        }
    }
    
    @Override
    public void saveChanges() {
        // Сохраняем изменения в сетке крафта
        if (craftingMatrix != null) {
            // Логика сохранения может быть добавлена здесь
        }
    }

}