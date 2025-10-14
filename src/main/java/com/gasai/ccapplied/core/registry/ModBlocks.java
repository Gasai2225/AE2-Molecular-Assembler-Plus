package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = 
        DeferredRegister.create(ForgeRegistries.BLOCKS, CCApplied.MODID);

    // Блоки регистрируются здесь (пока нет)

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

