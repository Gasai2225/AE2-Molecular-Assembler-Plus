package com.gasai.ccapplied.common.core;

import appeng.api.crafting.IPatternDetailsDecoder;

public final class PatternInit {
    private PatternInit() {}

    public static void registerDecoders() {
        try {
            Class<?> helper = Class.forName("appeng.api.crafting.PatternDetailsHelper");
            Class<?> decoderIface = IPatternDetailsDecoder.class;
            Object decoder = new ExtremePatternDecoder();

            // AE2 1.20+: addDecoder(IPatternDetailsDecoder)
            try {
                var m = helper.getMethod("addDecoder", decoderIface);
                m.invoke(null, decoder);
                return;
            } catch (NoSuchMethodException ignored) {}

            // альтернативное имя
            var m2 = helper.getMethod("registerDecoder", decoderIface);
            m2.invoke(null, decoder);
        } catch (Throwable t) {
            // лог по желанию
        }
    }
}
