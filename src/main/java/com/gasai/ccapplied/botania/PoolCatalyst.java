package com.gasai.ccapplied.botania;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum PoolCatalyst {
    NONE("none", null), ALCHEMY("alchemy", "alchemy_catalyst"), CONJURATION("conjuration", "conjuration_catalyst");
    private final String id;
    private final String block;
    PoolCatalyst(String id, String block) { this.id = id; this.block = block; }
    public String id() { return id; }
    public BlockState state() {
        return block == null ? Blocks.AIR.defaultBlockState() : BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("botania", block)).defaultBlockState();
    }
    public static PoolCatalyst fromId(String id) {
        for (var catalyst : values()) if (catalyst.id.equals(id)) return catalyst;
        throw new IllegalArgumentException("Unknown pool catalyst: " + id);
    }
}
