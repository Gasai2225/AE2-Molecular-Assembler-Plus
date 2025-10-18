package com.gasai.ccapplied.core.registry;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.blocks.ExtremeMolecularAssemblerBlock;
import com.gasai.ccapplied.tiles.ExtremeMolecularAssemblerTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.concurrent.atomic.AtomicReference;

@Mod.EventBusSubscriber(modid = CCApplied.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCBlocks {
    
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CCApplied.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CCApplied.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CCApplied.MODID);
    
    public static final RegistryObject<Block> EXTREME_MOLECULAR_ASSEMBLER = BLOCKS.register(
        "extreme_molecular_assembler",
        () -> new ExtremeMolecularAssemblerBlock(Block.Properties.of()
                .strength(3.5f)
                .requiresCorrectToolForDrops()
                .noOcclusion())
    );
    
    public static final RegistryObject<Item> EXTREME_MOLECULAR_ASSEMBLER_ITEM = ITEMS.register(
        "extreme_molecular_assembler",
        () -> new BlockItem(EXTREME_MOLECULAR_ASSEMBLER.get(), new Item.Properties())
    );
    
    public static final RegistryObject<BlockEntityType<ExtremeMolecularAssemblerTileEntity>> EXTREME_MOLECULAR_ASSEMBLER_TILE = BLOCK_ENTITIES.register(
        "extreme_molecular_assembler",
        () -> {
            var typeHolder = new AtomicReference<BlockEntityType<ExtremeMolecularAssemblerTileEntity>>();
            BlockEntityType.BlockEntitySupplier<ExtremeMolecularAssemblerTileEntity> supplier = (BlockPos pos, BlockState state) -> new ExtremeMolecularAssemblerTileEntity(typeHolder.get(), pos, state);
            var type = BlockEntityType.Builder.of(supplier, EXTREME_MOLECULAR_ASSEMBLER.get()).build(null);
            typeHolder.set(type);
            
            ((ExtremeMolecularAssemblerBlock) EXTREME_MOLECULAR_ASSEMBLER.get()).setBlockEntity(ExtremeMolecularAssemblerTileEntity.class, type, null, null);
            
            return type;
        }
    );
}
