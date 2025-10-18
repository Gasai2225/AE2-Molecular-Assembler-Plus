package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.items.ExtremeBlankPatternItem;
import com.gasai.ccapplied.items.ExtremeEncodedPatternItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CCItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CCApplied.MODID);

    public static final RegistryObject<Item> EXTREME_BLANK_PATTERN = ITEMS.register(
            "extreme_blank_pattern",
            () -> new ExtremeBlankPatternItem(new Item.Properties().stacksTo(64))
    );

    public static final RegistryObject<Item> EXTREME_CRAFTING_PATTERN = ITEMS.register(
            "extreme_crafting_pattern",
            () -> new ExtremeEncodedPatternItem(new Item.Properties().stacksTo(1))
    );

    // AE2 Part item — ставится на кабели
    public static final RegistryObject<Item> EXTREME_PATTERN_TERMINAL = ITEMS.register(
        "extreme_pattern_terminal",
        () -> new appeng.items.parts.PartItem<>(
                new Item.Properties(),
                com.gasai.ccapplied.parts.ExtremePatternEncodingTerminalPart.class,
                com.gasai.ccapplied.parts.ExtremePatternEncodingTerminalPart::new
        )
);


    private CCItems() {}
}
