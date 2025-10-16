package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.integration.ae2.pattern.ExtremeBlankPatternItem;
import com.gasai.ccapplied.integration.ae2.pattern.ExtremeEncodedPatternItem;
import com.gasai.ccapplied.integration.ae2.items.ExtremePatternTerminalItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = CCApplied.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
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
                com.gasai.ccapplied.integration.ae2.part.ExtremePatternEncodingTerminalPart.class,
                com.gasai.ccapplied.integration.ae2.part.ExtremePatternEncodingTerminalPart::new
        )
);

    // Добавим в общий креатив таб для удобства (можно убрать)
    @SubscribeEvent
    public static void fillCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey().equals(CreativeModeTabs.REDSTONE_BLOCKS)) {
            e.accept(EXTREME_BLANK_PATTERN.get());
            e.accept(EXTREME_CRAFTING_PATTERN.get());
            e.accept(EXTREME_PATTERN_TERMINAL.get());
        }
    }

    private CCItems() {}
}
