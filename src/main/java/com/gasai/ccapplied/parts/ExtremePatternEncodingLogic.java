package com.gasai.ccapplied.parts;

import com.gasai.ccapplied.core.registry.CCItems;
import com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalLogicHost;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;

import com.gasai.ccapplied.items.ExtremeBlankPatternItem;
import com.gasai.ccapplied.patterns.ExtremeCraftingPattern;
import com.gasai.ccapplied.items.ExtremeEncodedPatternItem;

/**
 * Логика кодирования для экстремального (9x9) шаблона.
 * Поведение максимально повторяет AE2 PatternEncodingLogic, но:
 *  - входная сетка 9x9 (81 слот)
 *  - выходов 1
 *  - только предметы (без жидкостей), количество в слотах = 1
 */
public class ExtremePatternEncodingLogic implements InternalInventoryHost {

    public static final int EXTREME_GRID_SIZE = 9;
    public static final int MAX_INPUT_SLOTS = EXTREME_GRID_SIZE * EXTREME_GRID_SIZE; // 81
    public static final int MAX_OUTPUT_SLOTS = 1; // для ExtremeCrafting обычно 1 результат

    private final com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost host;

    // «виртуальные» инвентари содержат GenericStack, как в AE2
    private final ConfigInventory encodedInputInv = ConfigInventory.configStacks(
            null, MAX_INPUT_SLOTS, this::onEncodedInputChanged, true);
    private final ConfigInventory encodedOutputInv = ConfigInventory.configStacks(
            null, MAX_OUTPUT_SLOTS, this::onEncodedOutputChanged, true);

    private final AppEngInternalInventory blankPatternInv = new AppEngInternalInventory(this, 1);
    private final AppEngInternalInventory encodedPatternInv = new AppEngInternalInventory(this, 1);

    private boolean isLoading = false;

    // Доп. поле на будущее: если решите поддержать связку с конкретным recipeId (как у камнерезки)
    @Nullable
    private ResourceLocation recipeId;

    public ExtremePatternEncodingLogic(
        com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost host) {
    this.host = host;
}

    private static boolean isExtremeBlank(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == CCItems.EXTREME_BLANK_PATTERN.get();
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        // Если в правый слот положили закодированный — загрузим его в «виртуальные» инвентари
        if (inv == this.encodedPatternInv) {
            loadEncodedPattern(this.encodedPatternInv.getStackInSlot(0));
        }
        saveChanges();
    }

    @Override
    public void saveChanges() {
        if (!isLoading) {
            host.markForSave();
        }
    }

    @Override
    public boolean isClientSide() {
        return host.getLevel().isClientSide();
    }

    private void onEncodedInputChanged() {
        fixExtremeCraftingGrid();
        saveChanges();
    }

    private void onEncodedOutputChanged() {
        saveChanges();
    }

    private void loadEncodedPattern(net.minecraft.world.item.ItemStack patternStack) {
        if (patternStack.isEmpty()) {
            return;
        }
        // Используем общий AE2 хелпер — ваш декодер (ExtremePatternDecoder) уже зарегистрирован на предмет
        var details = PatternDetailsHelper.decodePattern(patternStack, host.getLevel());

        if (details instanceof ExtremeCraftingPattern extreme) {
            loadExtremeCraftingPattern(extreme);
        }
        // Если в будущем добавите ещё разновидности — подключите их здесь.

        saveChanges();
    }



    private void loadExtremeCraftingPattern(ExtremeCraftingPattern pattern) {
        // входы 9×9: используем плотное представление 81 слота, чтобы сохранить позиции
        var dense = pattern.getDenseInputs81();
        fillInventoryFromSparseStacks(encodedInputInv, dense);

        // выход: у нас MAX_OUTPUT_SLOTS = 1, берём первичный output
        var outs = pattern.getOutputs();
        encodedOutputInv.beginBatch();
        try {
            encodedOutputInv.clear();
            encodedOutputInv.setStack(0, (outs != null && outs.length > 0) ? outs[0] : null);
        } finally {
            encodedOutputInv.endBatch();
        }

        // recipeId может быть null — это ок
        this.recipeId = pattern.getRecipeId();
    }

    private static void fillInventoryFromSparseStacks(ConfigInventory inv, GenericStack[] stacks) {
        inv.beginBatch();
        try {
            for (int i = 0; i < inv.size(); i++) {
                inv.setStack(i, i < stacks.length ? stacks[i] : null);
            }
        } finally {
            inv.endBatch();
        }
    }

    /** Входная «виртуальная» сетка 9×9 (GenericStack). */
    public ConfigInventory getEncodedInputInv() {
        return encodedInputInv;
    }

    /** Выходной «виртуальный» слот(ы) (GenericStack). */
    public ConfigInventory getEncodedOutputInv() {
        return encodedOutputInv;
    }

    /** Инвентарь на 1 слот для «пустых» экстремальных паттернов. */
    public InternalInventory getBlankPatternInv() {
        return blankPatternInv;
    }

    /** Инвентарь на 1 слот для уже закодированного паттерна (и приём готового при кодировании). */
    public InternalInventory getEncodedPatternInv() {
        return encodedPatternInv;
    }

    @Nullable
    public ResourceLocation getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(@Nullable ResourceLocation id) {
        this.recipeId = id;
        saveChanges();
    }

    /* ===================== NBT ===================== */

    public void readFromNBT(CompoundTag data) {
        isLoading = true;
        try {
            if (data.contains("ext_recipeId", Tag.TAG_STRING)) {
                this.recipeId = ResourceLocation.parse(data.getString("ext_recipeId"));
            } else {
                this.recipeId = null;
            }

            blankPatternInv.readFromNBT(data, "ext_blankPattern");
            encodedPatternInv.readFromNBT(data, "ext_encodedPattern");

            encodedInputInv.readFromChildTag(data, "ext_encodedInputs");
            encodedOutputInv.readFromChildTag(data, "ext_encodedOutputs");
        } finally {
            isLoading = false;
        }
    }

    public void writeToNBT(CompoundTag data) {
        if (this.recipeId != null) {
            data.putString("ext_recipeId", this.recipeId.toString());
        }
        blankPatternInv.writeToNBT(data, "ext_blankPattern");
        encodedPatternInv.writeToNBT(data, "ext_encodedPattern");
        encodedInputInv.writeToChildTag(data, "ext_encodedInputs");
        encodedOutputInv.writeToChildTag(data, "ext_encodedOutputs");
    }

    /* ===================== Валидация сетки ===================== */

    /**
     * Для экстремального крафта:
     *  - только предметные ключи (никаких жидкостей/нестандартных ключей)
     *  - количество в ячейке = 1
     */
    public void fixExtremeCraftingGrid() {
        if (host.getLevel() == null || host.getLevel().isClientSide()) {
            return;
        }

        var grid = getEncodedInputInv();
        for (int slot = 0; slot < grid.size(); slot++) {
            var stack = grid.getStack(slot);
            if (stack == null) {
                continue;
            }

            // только предметы
            if (!AEItemKey.is(stack.what())) {
                grid.setStack(slot, null);
                continue;
            }

            // количество = 1
            if (stack.amount() != 1) {
                grid.setStack(slot, new GenericStack(stack.what(), 1));
            }
        }

        // выход тоже приводим к «1 предмет», если вдруг накинули больше
        var out = getEncodedOutputInv();
        for (int slot = 0; slot < out.size(); slot++) {
            var s = out.getStack(slot);
            if (s == null) continue;
            if (!AEItemKey.is(s.what())) {
                out.setStack(slot, null);
                continue;
            }
            // выходное количество может быть >1 у рецепта — это допустимо.
            // Если хотите всегда 1 — раскомментируйте:
            // if (s.amount() != 1) out.setStack(slot, new GenericStack(s.what(), 1));
        }
    }

    /* ===================== Кодирование ===================== */

    /**
     * Кодирует паттерн из текущего состояния encodedInputInv и encodedOutputInv
     */
    public boolean encodePattern() {
        if (isClientSide()) {
            return false;
        }

        // Проверяем что есть входные данные
        boolean hasInputs = false;
        for (int i = 0; i < encodedInputInv.size(); i++) {
            if (encodedInputInv.getStack(i) != null) {
                hasInputs = true;
                break;
            }
        }

        if (!hasInputs) {
            return false;
        }

        // Проверяем что есть выходные данные
        var output = encodedOutputInv.getStack(0);
        if (output == null) {
            return false;
        }

        // Создаем закодированный паттерн
        var resultPattern = new ItemStack(CCItems.EXTREME_CRAFTING_PATTERN.get());
        var encodedPatternItem = (ExtremeEncodedPatternItem) resultPattern.getItem();

        // Создаем массив GenericStack[] длиной 81 для входных данных
        GenericStack[] inputStacks = new GenericStack[MAX_INPUT_SLOTS];
        for (int i = 0; i < MAX_INPUT_SLOTS; i++) {
            inputStacks[i] = encodedInputInv.getStack(i);
        }

        // Кодируем паттерн
        resultPattern = encodedPatternItem.encode(inputStacks, output, recipeId);

        // Проверяем, есть ли уже закодированный паттерн в слоте
        var existingPattern = encodedPatternInv.getStackInSlot(0);
        if (!existingPattern.isEmpty()) {
            // Если есть закодированный паттерн, полностью очищаем слот и ставим новый
            encodedPatternInv.extractItem(0, existingPattern.getCount(), false);
            encodedPatternInv.insertItem(0, resultPattern, false);
        } else {
            // Если нет закодированного паттерна, проверяем бланк паттерн
            var blankPattern = blankPatternInv.getStackInSlot(0);
            if (!isExtremeBlank(blankPattern)) {
                return false;
            }
            
            // Убираем бланк паттерн (уменьшаем количество на 1)
            blankPatternInv.extractItem(0, 1, false);
            
            // Ставим закодированный паттерн
            encodedPatternInv.insertItem(0, resultPattern, false);
        }

        return true;
    }

    /**
     * Очищает сетку крафта
     */
    public void clearCraftingGrid() {
        encodedInputInv.clear();
        encodedOutputInv.clear();
    }

    /**
     * Заполняет encodedInputInv и encodedOutputInv из реальной сетки крафта
     */
    public void fillFromCraftingMatrix(InternalInventory craftingMatrix, ItemStack recipeResult) {
        // Очищаем текущие данные
        encodedInputInv.clear();
        encodedOutputInv.clear();

        // Заполняем входные данные из craftingMatrix
        for (int i = 0; i < Math.min(craftingMatrix.size(), MAX_INPUT_SLOTS); i++) {
            ItemStack stack = craftingMatrix.getStackInSlot(i);
            if (!stack.isEmpty()) {
                var itemKey = AEItemKey.of(stack);
                if (itemKey != null) {
                    encodedInputInv.setStack(i, new GenericStack(itemKey, stack.getCount()));
                }
            }
        }

        // Заполняем выходные данные
        if (!recipeResult.isEmpty()) {
            var outputItemKey = AEItemKey.of(recipeResult);
            if (outputItemKey != null) {
                encodedOutputInv.setStack(0, new GenericStack(outputItemKey, recipeResult.getCount()));
            }
        }
    }

    /**
     * Загружает рецепт из закодированного паттерна в реальную сетку крафта
     */
    public void loadPatternIntoMatrix(InternalInventory craftingMatrix, ItemStack patternStack) {
        if (patternStack.isEmpty()) {
            return;
        }

        // Декодируем паттерн
        var details = PatternDetailsHelper.decodePattern(patternStack, host.getLevel());
        if (!(details instanceof ExtremeCraftingPattern extreme)) {
            return;
        }

        // Очищаем сетку крафта
        for (int i = 0; i < craftingMatrix.size(); i++) {
            craftingMatrix.insertItem(i, ItemStack.EMPTY, false);
        }

        // Загружаем данные из паттерна в сетку крафта
        var inputStacks = extreme.getInputStacks();
        for (int i = 0; i < Math.min(craftingMatrix.size(), inputStacks.length); i++) {
            if (inputStacks[i] != null && !inputStacks[i].isEmpty()) {
                craftingMatrix.insertItem(i, inputStacks[i].copy(), false);
            }
        }

        // Также обновляем виртуальные инвентари для синхронизации
        loadExtremeCraftingPattern(extreme);
    }
}
