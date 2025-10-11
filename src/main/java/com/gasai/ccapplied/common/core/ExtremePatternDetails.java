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

    public ExtremePatternDetails(AEItemKey definition, ItemStack[] inputs81, GenericStack[] outputs,
                                 boolean substitutes, int craftTime) {
        this.definition = definition;
        this.outputs = outputs;
        this.substitutes = substitutes;
        this.craftTime = Math.max(1, craftTime);

        // Строим массив IInput: по одному IInput на каждую непустую ячейку
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

    /** Необязательная помощь сборщику — время крафта (используем в нашей машине). */
    public int getCraftTime() {
        return craftTime;
    }

    public boolean canSubstitute() {
        return substitutes;
    }

    /** Базовый IInput: один возможный вариант без остатков/подстановок (MVP). */
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
                // MVP: базовая проверка по типу ключа
                for (GenericStack s : possibleInputs) {
                    if (s.what().equals(input)) return true;
                }
                return false;
            }

            @Override
            public AEKey getRemainingKey(AEKey template) {
                // Нет остатков (например, вёдра) — добавим позже при необходимости
                return null;
            }
        };
    }
}
