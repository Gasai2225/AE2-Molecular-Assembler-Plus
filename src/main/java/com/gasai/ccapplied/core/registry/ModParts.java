package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.common.parts.ExtremePatternTerminalPart;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

/**
 * Регистрация Part'ов для CCApplied
 */
public class ModParts {
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(ForgeRegistries.ITEMS, CCApplied.MODID);

    public static final RegistryObject<PartItem<ExtremePatternTerminalPart>> EXTREME_PATTERN_TERMINAL =
        register("extreme_pattern_terminal", ExtremePatternTerminalPart.class, ExtremePatternTerminalPart::new);

    private static <T extends IPart> RegistryObject<PartItem<T>> register(
            String id,
            Class<T> partClass,
            Function<IPartItem<T>, T> factory) {
        // Регистрируем модели Part'а
        PartModels.registerModels(PartModelsHelper.createModels(partClass));
        return ITEMS.register(id, () -> new PartItem<>(new Item.Properties(), partClass, factory));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

