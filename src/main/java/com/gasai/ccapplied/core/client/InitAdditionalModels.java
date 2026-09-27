package com.gasai.ccapplied.core.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;

import com.gasai.ccapplied.client.render.ExtremeMolecularAssemblerRenderer;

@OnlyIn(Dist.CLIENT)
public class InitAdditionalModels {

    public static void init(ModelEvent.RegisterAdditional event) {
        if (com.gasai.ccapplied.core.registry.CCOptionalMods.isBotaniaLoaded())
            event.register(com.gasai.ccapplied.botania.client.BotanicalAssemblerRenderer.PORTAL);
        if (com.gasai.ccapplied.core.registry.CCOptionalMods.isBotaniaLoaded())
            event.register(com.gasai.ccapplied.botania.client.BotanicalAssemblerRenderer.LIGHTS);
        event.register(ExtremeMolecularAssemblerRenderer.LIGHTS_MODEL);
    }
}
