package com.gasai.ccapplied.common.core;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;
import com.gasai.ccapplied.common.item.ExtremePatternEncodedItem;
import com.gasai.ccapplied.core.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ExtremePatternDecoder implements IPatternDetailsDecoder {

    @Override
    public boolean isEncodedPattern(ItemStack stack) {
        return stack.getItem() == ModItems.ENCODED_EXTREME_PATTERN.get()
                && ExtremePatternEncodedItem.isEncoded(stack);
    }

    @Override
    public @Nullable IPatternDetails decodePattern(AEItemKey what, Level level) {
        return decodePattern(what.toStack(), level, false);
    }

    @Override
    public @Nullable IPatternDetails decodePattern(ItemStack what, Level level, boolean tryRecovery) {
        if (!isEncodedPattern(what)) return null;

        var def = AEItemKey.of(what);
        var decoded = ExtremePatternCodec.read(what.getOrCreateTag(), def);
        return new ExtremePatternDetails(decoded.definition, decoded.inputs81, decoded.outputs,
                decoded.substitutes, decoded.craftTime);
    }
}
