package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.menus.ExtremePatternEncodingTermMenu;
import com.gasai.ccapplied.menus.ExtremeMolecularAssemblerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CCMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CCApplied.MODID);

    // Регистрируем именно ТЕКУЩИЙ экземпляр TYPE из класса меню
    public static final RegistryObject<MenuType<ExtremePatternEncodingTermMenu>> EXTREME_PATTERN_TERM =
            MENUS.register("extreme_patternterm", () -> ExtremePatternEncodingTermMenu.TYPE);
    
    public static final RegistryObject<MenuType<ExtremeMolecularAssemblerMenu>> EXTREME_MOLECULAR_ASSEMBLER =
            MENUS.register("extreme_molecular_assembler", () -> ExtremeMolecularAssemblerMenu.TYPE);

    private CCMenuTypes() {}
}