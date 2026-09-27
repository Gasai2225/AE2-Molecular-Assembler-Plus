package com.gasai.ccapplied.botania;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BotaniaConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MANA_PER_TICK;

    static {
        var builder = new ForgeConfigSpec.Builder();
        MANA_PER_TICK = builder.comment("Maximum mana transferred per bus per tick. Base rate is 62,500; each acceleration card doubles it. One pool is 1,000,000.")
                .defineInRange("manaPerTick", 1_000_000, 1, 1_000_000_000);
        SPEC = builder.build();
    }

    private BotaniaConfig() {}
}
