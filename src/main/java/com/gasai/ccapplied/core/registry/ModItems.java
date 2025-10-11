package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.common.item.ExtremePatternItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CCApplied.MODID);

    public static final RegistryObject<Item> EXTREME_PATTERN = ITEMS.register(
            "extreme_pattern",
            () -> new ExtremePatternItem(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.RARE)));

    public static final RegistryObject<Item> EXTREME_ASSEMBLER_ITEM = ITEMS.register(
            "extreme_molecular_assembler",
            () -> new BlockItem(ModBlocks.EXTREME_ASSEMBLER.get(), new Item.Properties())
    );

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
