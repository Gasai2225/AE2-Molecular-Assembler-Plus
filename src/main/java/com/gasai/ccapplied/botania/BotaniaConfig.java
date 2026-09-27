package com.gasai.ccapplied.botania;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BotaniaConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MANA_PER_TICK;

    static {
        var builder = new ModConfigSpec.Builder();
        MANA_PER_TICK = builder.comment("Maximum mana transferred per bus per tick. Base rate is 62,500; each acceleration card doubles it. One pool is 1,000,000.")
                .defineInRange("manaPerTick", 1_000_000, 1, 1_000_000_000);
        SPEC = builder.build();
    }

    private BotaniaConfig() {}
}
