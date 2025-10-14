package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(ForgeRegistries.ITEMS, CCApplied.MODID);

    // Обычные предметы регистрируются здесь (пока нет)
    // Part'ы регистрируются в ModParts

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

