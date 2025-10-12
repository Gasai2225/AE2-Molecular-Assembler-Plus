package com.gasai.ccapplied.hooks;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.gasai.ccapplied.common.item.ExtremePatternEncodedItem;

public final class GuiGraphicsHooks {
    private static final Logger LOG = LoggerFactory.getLogger("CCApplied/GuiHooks");
    private static final ThreadLocal<ItemStack> OVERRIDING_FOR = new ThreadLocal<>();
    private GuiGraphicsHooks() {}

    public static boolean onCtx(GuiGraphics gg, @Nullable LivingEntity living, @Nullable Level level,
                                ItemStack stack, int x, int y, int seed, int z) {
        return renderIfPattern(gg, stack, x, y, seed);
    }
    public static boolean onSimple(GuiGraphics gg, ItemStack stack, int x, int y) {
        return renderIfPattern(gg, stack, x, y, 0);
    }
    public static boolean onSimpleWithSeed(GuiGraphics gg, ItemStack stack, int x, int y, int seed) {
        return renderIfPattern(gg, stack, x, y, seed);
    }

    private static boolean renderIfPattern(GuiGraphics gg, ItemStack original, int x, int y, int seed) {
        if (!(original.getItem() instanceof ExtremePatternEncodedItem)
                || !ExtremePatternEncodedItem.isEncoded(original)
                || !Screen.hasShiftDown()) {
            return false;
        }
        if (OVERRIDING_FOR.get() == original) return false;

        var preview = ExtremePatternEncodedItem.getOutput(original);
        if (preview.isEmpty() || preview == original) return false;

        OVERRIDING_FOR.set(original);
        try {
            gg.renderItem(preview, x, y, seed);
            gg.renderItemDecorations(Minecraft.getInstance().font, preview, x, y);
            LOG.debug("[GuiHooks] replaced '{}' with preview '{}'",
                    original.getHoverName().getString(), preview.getHoverName().getString());
            return true;
        } finally {
            OVERRIDING_FOR.remove();
        }
    }
}
