package com.gasai.ccapplied.common.core;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class ExtremePatternDetails implements IPatternDetails {

    private final AEItemKey definition;
    private final IInput[] inputs;
    private final GenericStack[] outputs;
    private final boolean substitutes;
    private final int craftTime; // тики

    /** Полная карта 9×9 для GUI/валидации (индексы 0..80). */
    private final ItemStack[] inputs81;

    public ExtremePatternDetails(AEItemKey definition, ItemStack[] inputs81, GenericStack[] outputs,
                                 boolean substitutes, int craftTime) {
        this.definition = definition;
        this.outputs = outputs;
        this.substitutes = substitutes;
        this.craftTime = Math.max(1, craftTime);
        this.inputs81 = inputs81.clone(); // сохраняем послойно 0..80

        // Строим «сжатый» список IInput для авто-крафта
        List<IInput> list = new ArrayList<>();
        for (ItemStack is : inputs81) {
            if (is != null && !is.isEmpty()) {
                GenericStack gs = new GenericStack(AEItemKey.of(is), is.getCount());
                list.add(simpleInput(gs));
            }
        }
        this.inputs = list.toArray(IInput[]::new);
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public GenericStack[] getOutputs() {
        return outputs;
    }

    /** Время крафта из паттерна (в тиках). */
    public int getCraftTime() {
        return craftTime;
    }

    public boolean canSubstitute() {
        return substitutes;
    }

    /* ================= Доп. API для машины/меню ================= */

    /** Слот включен, если в паттерне в этой ячейке есть предмет. */
    public boolean isSlotEnabled(int slot) {
        return slot >= 0 && slot < inputs81.length
                && inputs81[slot] != null
                && !inputs81[slot].isEmpty();
    }

    /**
     * Предмет подходит для данной ячейки по шаблону.
     * Сначала проверка точного совпадения (NBT/без NBT), затем — при разрешённых подстановках — по AEKey.
     */
    public boolean isValidForSlot(int slot, AEItemKey key, Level level) {
        if (key == null || !isSlotEnabled(slot)) return false;

        ItemStack tmpl = inputs81[slot];

        // Строгое равенство предмета с NBT
        ItemStack candidate = key.toStack(1);
        if (ItemStack.isSameItemSameTags(tmpl, candidate)) {
            return true;
        }

        // Разрешены «подстановки» — допускаем тот же AEKey (AEKey включает NBT)
        if (substitutes) {
            var tmplKey = AEItemKey.of(tmpl);
            return tmplKey != null && tmplKey.equals(key);
        }

        return false;
    }

    /** Для подсказок/отрисовки — копия 9×9 раскладки. */
    public ItemStack[] getLayout81() {
        return inputs81.clone();
    }

    /** Базовый IInput. */
    public static IPatternDetails.IInput simpleInput(GenericStack... possibleInputs) {
        return new IPatternDetails.IInput() {
            @Override
            public GenericStack[] getPossibleInputs() {
                return possibleInputs;
            }

            @Override
            public long getMultiplier() {
                return 1;
            }

            @Override
            public boolean isValid(AEKey input, Level level) {
                for (GenericStack s : possibleInputs) {
                    if (s.what().equals(input)) return true;
                }
                return false;
            }

            @Override
            public AEKey getRemainingKey(AEKey template) {
                return null;
            }
        };
    }
}
