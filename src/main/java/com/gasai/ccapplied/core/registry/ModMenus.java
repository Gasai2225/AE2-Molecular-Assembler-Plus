package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = 
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, CCApplied.MODID);

    // Меню регистрируются через MenuTypeBuilder в самих классах меню
    // Здесь регистрируем дополнительные меню при необходимости

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}

