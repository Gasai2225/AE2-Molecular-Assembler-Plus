package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CCApplied.MODID);

    // BlockEntity регистрируются здесь (пока нет)

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

