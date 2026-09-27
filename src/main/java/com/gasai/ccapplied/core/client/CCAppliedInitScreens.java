package com.gasai.ccapplied.core.client;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class CCAppliedInitScreens {
    private CCAppliedInitScreens() {
    }

    public static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(
            MenuType<M> type, ScreenFactory<M, U> factory, String stylePath) {
        MenuScreens.<M, U>register(type, (menu, inventory, title) ->
                factory.create(menu, inventory, title, StyleManager.loadStyleDoc(stylePath)));
    }

    @FunctionalInterface
    public interface ScreenFactory<M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> {
        U create(M menu, Inventory inventory, Component title, ScreenStyle style);
    }
}

