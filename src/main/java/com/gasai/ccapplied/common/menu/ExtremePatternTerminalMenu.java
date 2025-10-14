package com.gasai.ccapplied.common.menu;

import com.gasai.ccapplied.common.logic.ExtremePatternEncodingLogic;
import com.gasai.ccapplied.common.parts.IExtremePatternTerminalMenuHost;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.AEBaseMenu;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.parts.encoding.EncodingMode;
import appeng.util.ConfigInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Меню для экстремального терминала кодирования паттернов
 * Поддерживает 9x9 сетку крафта для экстремальных рецептов
 */
public class ExtremePatternTerminalMenu extends AEBaseMenu {

    public static final MenuType<ExtremePatternTerminalMenu> TYPE = MenuTypeBuilder
            .create(ExtremePatternTerminalMenu::new, IExtremePatternTerminalMenuHost.class)
            .build("extreme_pattern_terminal");

    private final ExtremePatternEncodingLogic encodingLogic;
    
    private final FakeSlot[] craftingGridSlots = new FakeSlot[81]; // 9x9
    private final FakeSlot[] processingInputSlots = new FakeSlot[AEProcessingPattern.MAX_INPUT_SLOTS];
    private final FakeSlot[] processingOutputSlots = new FakeSlot[AEProcessingPattern.MAX_OUTPUT_SLOTS];
    private final RestrictedInputSlot blankPatternSlot;
    private final RestrictedInputSlot encodedPatternSlot;

    private final ConfigInventory encodedInputsInv;
    private final ConfigInventory encodedOutputsInv;

    @GuiSync(97)
    public EncodingMode mode = EncodingMode.CRAFTING;
    
    @GuiSync(96)
    public boolean substitute = false;

    public ExtremePatternTerminalMenu(int id, Inventory playerInventory, IExtremePatternTerminalMenuHost host) {
        super(TYPE, id, playerInventory, host);
        this.encodingLogic = host.getLogic();
        this.encodedInputsInv = encodingLogic.getEncodedInputInv();
        this.encodedOutputsInv = encodingLogic.getEncodedOutputInv();

        // Обертки для использования со слотами
        var encodedInputs = encodedInputsInv.createMenuWrapper();
        var encodedOutputs = encodedOutputsInv.createMenuWrapper();

        // Создаем сетку крафта 9x9 для экстремального крафтинга
        int index = 0;
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                var slot = new FakeSlot(encodedInputs, index);
                slot.setHideAmount(true);
                this.addSlot(this.craftingGridSlots[index] = slot, SlotSemantics.CRAFTING_GRID);
                index++;
            }
        }

        // Создаем слот вывода для режима крафта
        this.addSlot(new FakeSlot(encodedOutputs, 0), SlotSemantics.CRAFTING_RESULT);

        // Создаем слоты для обработки (processing)
        for (int i = 0; i < processingInputSlots.length; i++) {
            this.addSlot(this.processingInputSlots[i] = new FakeSlot(encodedInputs, i),
                    SlotSemantics.PROCESSING_INPUTS);
        }
        for (int i = 0; i < this.processingOutputSlots.length; i++) {
            this.addSlot(this.processingOutputSlots[i] = new FakeSlot(encodedOutputs, i),
                    SlotSemantics.PROCESSING_OUTPUTS);
        }

        // Слоты для паттернов
        this.addSlot(this.blankPatternSlot = new RestrictedInputSlot(
                RestrictedInputSlot.PlacableItemType.BLANK_PATTERN,
                encodingLogic.getBlankPatternInv(), 0), SlotSemantics.BLANK_PATTERN);
        
        this.addSlot(this.encodedPatternSlot = new RestrictedInputSlot(
                RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN,
                encodingLogic.getEncodedPatternInv(), 0), SlotSemantics.ENCODED_PATTERN);

        this.encodedPatternSlot.setStackLimit(1);
        
        this.createPlayerInventorySlots(playerInventory);
    }

    public void encode() {
        if (isClientSide()) {
            // TODO: отправка пакета на сервер
            return;
        }

        ItemStack encodedPattern = encodePattern();
        if (encodedPattern != null) {
            var encodeOutput = this.encodedPatternSlot.getItem();

            if (!encodeOutput.isEmpty()
                    && !PatternDetailsHelper.isEncodedPattern(encodeOutput)
                    && !AEItems.BLANK_PATTERN.isSameAs(encodeOutput)) {
                return;
            } else if (encodeOutput.isEmpty()) {
                var blankPattern = this.blankPatternSlot.getItem();
                if (!isPattern(blankPattern)) {
                    return;
                }

                blankPattern.shrink(1);
                if (blankPattern.getCount() <= 0) {
                    this.blankPatternSlot.set(ItemStack.EMPTY);
                }
            }

            this.encodedPatternSlot.set(encodedPattern);
        } else {
            clearPattern();
        }
    }

    private void clearPattern() {
        var encodedPattern = this.encodedPatternSlot.getItem();
        if (PatternDetailsHelper.isEncodedPattern(encodedPattern)) {
            this.encodedPatternSlot.set(
                    AEItems.BLANK_PATTERN.stack(encodedPattern.getCount()));
        }
    }

    @Nullable
    private ItemStack encodePattern() {
        return switch (this.mode) {
            case CRAFTING -> encodeExtremeCraftingPattern();
            case PROCESSING -> encodeProcessingPattern();
            default -> null;
        };
    }

    @Nullable
    private ItemStack encodeExtremeCraftingPattern() {
        // TODO: реализация кодирования экстремального крафт-паттерна
        return null;
    }

    @Nullable
    private ItemStack encodeProcessingPattern() {
        var inputs = new GenericStack[encodedInputsInv.size()];
        boolean valid = false;
        for (int slot = 0; slot < encodedInputsInv.size(); slot++) {
            inputs[slot] = encodedInputsInv.getStack(slot);
            if (inputs[slot] != null) {
                valid = true;
            }
        }
        if (!valid) {
            return null;
        }

        var outputs = new GenericStack[encodedOutputsInv.size()];
        for (int slot = 0; slot < encodedOutputsInv.size(); slot++) {
            outputs[slot] = encodedOutputsInv.getStack(slot);
        }
        if (outputs[0] == null) {
            return null;
        }

        return PatternDetailsHelper.encodeProcessingPattern(inputs, outputs);
    }

    @Nullable
    private ItemStack getEncodedCraftingIngredient(int slot) {
        var what = encodedInputsInv.getKey(slot);
        if (what == null) {
            return ItemStack.EMPTY;
        } else if (what instanceof AEItemKey itemKey) {
            return itemKey.toStack(1);
        } else {
            return null;
        }
    }

    private boolean isPattern(ItemStack output) {
        if (output.isEmpty()) {
            return false;
        }
        return AEItems.BLANK_PATTERN.isSameAs(output);
    }

    public void clear() {
        if (isClientSide()) {
            // TODO: отправка пакета на сервер
            return;
        }

        encodedInputsInv.clear();
        encodedOutputsInv.clear();
    }

    public EncodingMode getMode() {
        return this.mode;
    }

    public void setMode(EncodingMode mode) {
        if (isClientSide()) {
            // TODO: отправка пакета на сервер
        } else {
            this.mode = mode;
        }
    }

    public boolean isSubstitute() {
        return this.substitute;
    }

    public void setSubstitute(boolean substitute) {
        if (isClientSide()) {
            // TODO: отправка пакета на сервер
        } else {
            this.substitute = substitute;
        }
    }

    public FakeSlot[] getCraftingGridSlots() {
        return craftingGridSlots;
    }

    public FakeSlot[] getProcessingInputSlots() {
        return processingInputSlots;
    }

    public FakeSlot[] getProcessingOutputSlots() {
        return processingOutputSlots;
    }

    public boolean isClientSide() {
        return getPlayerInventory().player.level().isClientSide();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (isServerSide()) {
            if (this.mode != encodingLogic.getMode()) {
                this.mode = encodingLogic.getMode();
            }
            this.substitute = encodingLogic.isSubstitution();
        }
    }
}

