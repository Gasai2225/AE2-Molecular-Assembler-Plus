package com.gasai.ccapplied.patterns;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;
import com.gasai.ccapplied.core.registry.CCItems;

/**
 * Decoder for extreme 9x9 patterns
 */
public class ExtremePatternDecoder implements IPatternDetailsDecoder {

    public static final ExtremePatternDecoder INSTANCE = new ExtremePatternDecoder();

    private ExtremePatternDecoder() {}

    @Override
    public boolean isEncodedPattern(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == CCItems.EXTREME_CRAFTING_PATTERN.get();
    }

    @Override
    @Nullable
    public IPatternDetails decodePattern(AEItemKey what, Level level) {
        return null;
    }

    @Override
    @Nullable
    public IPatternDetails decodePattern(ItemStack stack, Level level, boolean tryRecovery) {
        if (!isEncodedPattern(stack)) {
            return null;
        }

        try {
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}