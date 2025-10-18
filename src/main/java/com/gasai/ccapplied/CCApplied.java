package com.gasai.ccapplied;

import appeng.api.crafting.PatternDetailsHelper;
import com.gasai.ccapplied.core.registry.CCItems;
import com.gasai.ccapplied.core.registry.CCMenuTypes;
import com.gasai.ccapplied.core.registry.CCBlocks;
import com.gasai.ccapplied.patterns.ExtremePatternDecoder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.resources.ResourceLocation;

@Mod(CCApplied.MODID)
public final class CCApplied {
    public static final String MODID = "ccapplied";
    public static final Logger LOG = LogUtils.getLogger();
    
    /**
     * Creates ResourceLocation for given path
     */
    public static ResourceLocation makeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public CCApplied() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        CCBlocks.BLOCKS.register(modBus);
        CCBlocks.ITEMS.register(modBus);
        CCBlocks.BLOCK_ENTITIES.register(modBus);
        CCItems.ITEMS.register(modBus);
        CCMenuTypes.MENUS.register(modBus);
        com.gasai.ccapplied.core.registry.CCCreativeTabs.CREATIVE_TABS.register(modBus);

        PatternDetailsHelper.registerDecoder(ExtremePatternDecoder.INSTANCE);
        
        com.gasai.ccapplied.client.CCPartModelRegistry.registerPartModels();
    }
}