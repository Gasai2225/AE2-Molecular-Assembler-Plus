package com.gasai.ccapplied;

import appeng.api.crafting.PatternDetailsHelper;
import com.gasai.ccapplied.client.CCPartModelRegistry;
import com.gasai.ccapplied.core.registry.CCItems;
import com.gasai.ccapplied.core.registry.CCMenuTypes;
import com.gasai.ccapplied.core.registry.CCBlocks;
import com.gasai.ccapplied.patterns.ExtremePatternDecoder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

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

    public CCApplied(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        
        if (com.gasai.ccapplied.core.registry.CCOptionalMods.isBotaniaLoaded()) {
            com.gasai.ccapplied.botania.BotaniaContent.register(modBus);
            container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                    com.gasai.ccapplied.botania.BotaniaConfig.SPEC, "ccapplied-botania.toml");
        }
        CCBlocks.BLOCKS.register(modBus);
        CCBlocks.ITEMS.register(modBus);
        CCBlocks.BLOCK_ENTITIES.register(modBus);
        CCItems.ITEMS.register(modBus);
        CCMenuTypes.MENUS.register(modBus);
        com.gasai.ccapplied.core.registry.CCCreativeTabs.CREATIVE_TABS.register(modBus);
        modBus.addListener(com.gasai.ccapplied.core.registry.CCCapabilities::register);

        CCPartModelRegistry.registerPartModels();
        PatternDetailsHelper.registerDecoder(ExtremePatternDecoder.INSTANCE);
    }
}
