package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.common.block.entity.ExtremeMolecularAssemblerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CCApplied.MODID);

    public static final RegistryObject<BlockEntityType<ExtremeMolecularAssemblerBlockEntity>> EXTREME_ASSEMBLER_BE =
            BLOCK_ENTITIES.register("extreme_molecular_assembler",
                    () -> BlockEntityType.Builder.of(
                            ExtremeMolecularAssemblerBlockEntity::new,
                            ModBlocks.EXTREME_ASSEMBLER.get()
                    ).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
