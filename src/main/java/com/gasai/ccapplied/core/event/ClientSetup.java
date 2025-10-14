package com.gasai.ccapplied.core.event;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.client.gui.ExtremePatternTerminalScreen;
import com.gasai.ccapplied.core.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Обработчик событий клиентской стороны
 */
@Mod.EventBusSubscriber(modid = CCApplied.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Регистрация экранов
            MenuScreens.register(com.gasai.ccapplied.common.menu.ExtremePatternTerminalMenu.TYPE, 
                                ExtremePatternTerminalScreen::new);
        });
    }
}

