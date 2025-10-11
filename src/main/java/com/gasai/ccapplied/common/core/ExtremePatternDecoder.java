package com.gasai.ccapplied.common.core;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.gasai.ccapplied.common.item.ExtremePatternItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ExtremePatternDecoder implements IPatternDetailsDecoder {

    @Override
    public boolean isEncodedPattern(ItemStack stack) {
        return stack.getItem() instanceof ExtremePatternItem && ExtremePatternItem.isEncoded(stack);
    }

    @Override
    public @Nullable IPatternDetails decodePattern(AEItemKey what, Level level) {
        AEKey k = what; // just alias
        ItemStack asItem = what.toStack(); // нужен сам ItemStack паттерна
        return decodePattern(asItem, level, false);
    }

    @Override
    public @Nullable IPatternDetails decodePattern(ItemStack what, Level level, boolean tryRecovery) {
        if (!(what.getItem() instanceof ExtremePatternItem) || !ExtremePatternItem.isEncoded(what)) {
            return null;
        }
        var root = ExtremePatternItem.readPatternData(what);
        if (root == null) return null;

        var def = AEItemKey.of(what);
        var decoded = ExtremePatternCodec.read(root, def);
        return new ExtremePatternDetails(decoded.definition, decoded.inputs81, decoded.outputs,
                decoded.substitutes, decoded.craftTime);
    }
}
