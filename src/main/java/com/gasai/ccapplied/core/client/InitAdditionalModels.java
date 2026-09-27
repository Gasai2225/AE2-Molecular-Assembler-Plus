package com.gasai.ccapplied.core.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.minecraft.client.resources.model.ModelResourceLocation;

import com.gasai.ccapplied.client.render.ExtremeMolecularAssemblerRenderer;

@OnlyIn(Dist.CLIENT)
public class InitAdditionalModels {

    public static void init(ModelEvent.RegisterAdditional event) {
        if (com.gasai.ccapplied.core.registry.CCOptionalMods.isBotaniaLoaded())
            event.register(ModelResourceLocation.standalone(com.gasai.ccapplied.botania.client.BotanicalAssemblerRenderer.PORTAL));
        if (com.gasai.ccapplied.core.registry.CCOptionalMods.isBotaniaLoaded())
            event.register(ModelResourceLocation.standalone(com.gasai.ccapplied.botania.client.BotanicalAssemblerRenderer.LIGHTS));
        event.register(ModelResourceLocation.standalone(ExtremeMolecularAssemblerRenderer.LIGHTS_MODEL));
    }
}
