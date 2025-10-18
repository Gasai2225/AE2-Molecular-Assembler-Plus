package com.gasai.ccapplied.screens;

import com.gasai.ccapplied.menus.ExtremePatternEncodingTermMenu;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ExtremePatternEncodingTermScreen extends MEStorageScreen<ExtremePatternEncodingTermMenu> {
    // Текстура будет использоваться в будущем для кастомного рендеринга

    public ExtremePatternEncodingTermScreen(ExtremePatternEncodingTermMenu menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);
        
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeScreen] Constructor START for player: {}, title: {}, style: {}", 
            inv.player.getGameProfile().getName(), title.getString(), style != null ? "NOT NULL" : "NULL");
        
               try {
                   // Размеры экрана теперь определяются в JSON файле стиля
                   com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeScreen] After super() call, dimensions: {}x{}", imageWidth, imageHeight);
            
            // Инициализируем виджеты
            initializeWidgets();
            com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeScreen] Screen initialized successfully");
        } catch (Exception e) {
            com.gasai.ccapplied.CCApplied.LOG.error("[ExtremeScreen] Error in constructor", e);
            throw e;
        }
    }

    private void initializeWidgets() {
        // Инициализируем виджеты для экстремального терминала
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeScreen] Initializing extreme pattern terminal widgets");
        
        // Создаем кастомную кнопку кодирования паттерна
        var encodeBtn = new ExtremeEncodeButton(menu);
        widgets.add("extremeEncodePattern", encodeBtn);
        
        // Создаем кастомную кнопку очистки паттерна
        var clearBtn = new ExtremeClearButton(menu);
        clearBtn.setHalfSize(true);
        widgets.add("extremeClearPattern", clearBtn);
        
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeScreen] Extreme pattern terminal widgets initialized");
    }

    private static class ExtremeEncodeButton extends appeng.client.gui.widgets.IconButton {
        private final ExtremePatternEncodingTermMenu menu;

        public ExtremeEncodeButton(ExtremePatternEncodingTermMenu menu) {
            super(btn -> {
                com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeScreen] Encode button clicked");
                if (menu.canEncode()) {
                    menu.encode();
                } else {
                    com.gasai.ccapplied.CCApplied.LOG.warn("[ExtremeScreen] Cannot encode - conditions not met");
                }
            });
            this.menu = menu;
            this.setMessage(Component.translatable("gui.ccapplied.extreme_encode_pattern"));
        }

        @Override
        protected appeng.client.gui.Icon getIcon() {
            return appeng.client.gui.Icon.WHITE_ARROW_DOWN; // Используем иконку кодирования из AE2
        }
    }
    
    /**
     * Кастомная кнопка очистки экстремального паттерна
     */
    private static class ExtremeClearButton extends appeng.client.gui.widgets.IconButton {
        private final ExtremePatternEncodingTermMenu menu;

        public ExtremeClearButton(ExtremePatternEncodingTermMenu menu) {
            super(btn -> {
                com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeScreen] Clear button clicked");
                menu.clearAll();
            });
            this.menu = menu;
            this.setMessage(Component.translatable("gui.ccapplied.extreme_clear_pattern"));
        }

        @Override
        protected appeng.client.gui.Icon getIcon() {
            return appeng.client.gui.Icon.CLEAR; // Используем иконку очистки из AE2
        }
    }
}


