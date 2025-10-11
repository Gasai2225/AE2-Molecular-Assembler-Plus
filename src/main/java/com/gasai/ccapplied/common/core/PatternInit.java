package com.gasai.ccapplied.common.core;

import com.gasai.ccapplied.common.item.ExtremePatternItem;

public final class PatternInit {
    private static boolean REG = false;
    private PatternInit() {}

    public static void registerDecoders() {
        if (!REG) {
            ExtremePatternItem.registerDecoder(new ExtremePatternDecoder());
            REG = true;
        }
    }
}
