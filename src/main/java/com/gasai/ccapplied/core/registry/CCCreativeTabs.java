package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.core.registry.CCBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class CCCreativeTabs {
    
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CCApplied.MODID);

    public static final RegistryObject<CreativeModeTab> CCAPPLIED_TAB = CREATIVE_TABS.register("ccapplied_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ccapplied"))
                    .icon(() -> new ItemStack(CCBlocks.EXTREME_MOLECULAR_ASSEMBLER_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        // Добавляем предметы в креатив таб
                        output.accept(CCItems.EXTREME_BLANK_PATTERN.get());
                        output.accept(CCItems.EXTREME_PATTERN_TERMINAL.get());
                        output.accept(CCBlocks.EXTREME_MOLECULAR_ASSEMBLER_ITEM.get());
                        // EXTREME_CRAFTING_PATTERN не добавляем - это закодированный шаблон
                    })
                    .build());

    private CCCreativeTabs() {}
}
