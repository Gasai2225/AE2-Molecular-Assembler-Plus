package com.gasai.ccapplied.items;

import com.gasai.ccapplied.parts.ExtremePatternEncodingTerminalPart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import javax.annotation.Nonnull;
import net.minecraft.world.InteractionResult;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;

public class ExtremePatternTerminalItem extends Item implements IPartItem<ExtremePatternEncodingTerminalPart> {
    public ExtremePatternTerminalItem(Properties props) {
        super(props);
    }

    @Override
    public Class<ExtremePatternEncodingTerminalPart> getPartClass() {
        return ExtremePatternEncodingTerminalPart.class;
    }

    @Override
    public ExtremePatternEncodingTerminalPart createPart() {
        return new ExtremePatternEncodingTerminalPart(this);
    }

    @Override
    public InteractionResult useOn(@Nonnull UseOnContext ctx) {
        return PartHelper.usePartItem(ctx);
    }
}


