package com.gasai.ccapplied.core.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.gasai.ccapplied.CCApplied;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;

/**
 * Система регистрации экранов CCApplied, совместимая с AE2
 */
public final class CCAppliedInitScreens {
    
    private static final Map<MenuType<?>, ScreenFactory<?, ?>> SCREEN_FACTORIES = new HashMap<>();
    
    private CCAppliedInitScreens() {}
    
    /**
     * Регистрирует экран для указанного типа меню
     */
    public static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(
            MenuType<M> type,
            ScreenFactory<M, U> factory,
            String stylePath) {
        
        SCREEN_FACTORIES.put(type, factory);
        
        // Регистрируем экран через стандартную систему Minecraft
        MenuScreens.<M, U>register(type, (menu, playerInv, title) -> {
            
            try {
                var style = StyleManager.loadStyleDoc(stylePath);
                
                @SuppressWarnings("unchecked")
                ScreenFactory<M, U> typedFactory = (ScreenFactory<M, U>) SCREEN_FACTORIES.get(type);
                if (typedFactory == null) {
                    throw new IllegalStateException("No factory registered for menu type: " + type);
                }
                
                var screen = typedFactory.create(menu, playerInv, title, style);
                return screen;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create screen", e);
            }
        });
        
    }
    
    /**
     * Фабрика экранов для CCApplied
     */
    @FunctionalInterface
    public interface ScreenFactory<T extends AbstractContainerMenu, U extends Screen & MenuAccess<T>> {
        U create(T menu, Inventory playerInv, Component title, ScreenStyle style);
    }
}

