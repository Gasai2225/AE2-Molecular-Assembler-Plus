package com.gasai.ccapplied.core.registry;

import appeng.api.AECapabilities;
import com.gasai.ccapplied.tiles.ExtremeMolecularAssemblerTileEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class CCCapabilities {
    private CCCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        register(event, CCBlocks.EXTREME_MOLECULAR_ASSEMBLER_TILE.get());
        if (CCBlocks.WYVERN_MOLECULAR_ASSEMBLER_TILE != null) {
            register(event, CCBlocks.WYVERN_MOLECULAR_ASSEMBLER_TILE.get());
        }
        if (CCBlocks.DRACONIC_MOLECULAR_ASSEMBLER_TILE != null) {
            register(event, CCBlocks.DRACONIC_MOLECULAR_ASSEMBLER_TILE.get());
        }
        if (CCBlocks.CHAOTIC_MOLECULAR_ASSEMBLER_TILE != null) {
            register(event, CCBlocks.CHAOTIC_MOLECULAR_ASSEMBLER_TILE.get());
        }
    }

    private static void register(RegisterCapabilitiesEvent event,
            BlockEntityType<ExtremeMolecularAssemblerTileEntity> type) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type, (machine, side) -> machine);
        event.registerBlockEntity(AECapabilities.CRAFTING_MACHINE, type, (machine, side) -> machine);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
                ExtremeMolecularAssemblerTileEntity::getExposedItemHandler);
    }
}
