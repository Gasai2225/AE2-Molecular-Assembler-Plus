package com.gasai.ccapplied.botania;

import com.gasai.ccapplied.core.registry.CCBlocks;
import appeng.blockentity.AEBaseBlockEntity;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class BotanicalAssemblers {
    public record Entry(Supplier<Block> block, Supplier<Item> item,
            Supplier<BlockEntityType<BotanicalAssemblerBlockEntity>> type) {}
    public static final Map<MagicalStation, Entry> ENTRIES = new EnumMap<>(MagicalStation.class);
    static {
        for (var station : MagicalStation.values()) {
            String id = station.id() + "_assembler";
            Supplier<Block> block = CCBlocks.BLOCKS.register(id, () -> new BotanicalAssemblerBlock(station));
            Supplier<Item> item = CCBlocks.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
            Supplier<BlockEntityType<BotanicalAssemblerBlockEntity>> type = CCBlocks.BLOCK_ENTITIES.register(id, () -> {
                var ref = new java.util.concurrent.atomic.AtomicReference<BlockEntityType<BotanicalAssemblerBlockEntity>>();
                var result = BlockEntityType.Builder.of(
                        (net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) ->
                                new BotanicalAssemblerBlockEntity(ref.get(), pos, state), block.get()).build(null);
                ref.set(result);
                AEBaseBlockEntity.registerBlockEntityItem(result, item.get());
                ((BotanicalAssemblerBlock) block.get()).setBlockEntity(BotanicalAssemblerBlockEntity.class, result, null, null);
                return result;
            });
            ENTRIES.put(station, new Entry(block, item, type));
        }
    }
    private BotanicalAssemblers() {}
    public static void init() {}
}
