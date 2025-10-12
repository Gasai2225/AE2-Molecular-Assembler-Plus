package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.common.block.ExtremeMolecularAssemblerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CCApplied.MODID);

    public static final RegistryObject<Block> EXTREME_ASSEMBLER =
            BLOCKS.register("extreme_molecular_assembler",
                    () -> new ExtremeMolecularAssemblerBlock(
                            BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()
                    ));


    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
