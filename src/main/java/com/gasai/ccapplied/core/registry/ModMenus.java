package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.client.gui.menu.ExtremeMolecularAssemblerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CCApplied.MODID);

    public static final RegistryObject<MenuType<ExtremeMolecularAssemblerMenu>> EXTREME_ASSEMBLER =
            MENUS.register("extreme_molecular_assembler",
                    () -> IForgeMenuType.create((windowId, inv, buf) -> new ExtremeMolecularAssemblerMenu(windowId, inv, buf)));
}
