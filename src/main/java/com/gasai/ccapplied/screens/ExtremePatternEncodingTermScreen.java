package com.gasai.ccapplied.screens;

import com.gasai.ccapplied.menus.ExtremePatternEncodingTermMenu;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ExtremePatternEncodingTermScreen extends MEStorageScreen<ExtremePatternEncodingTermMenu> {

    public ExtremePatternEncodingTermScreen(ExtremePatternEncodingTermMenu menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);
        
        
               try {
            
            initializeWidgets();
        } catch (Exception e) {
            throw e;
        }
    }

    private void initializeWidgets() {
        
        var encodeBtn = new ExtremeEncodeButton(menu);
        widgets.add("extremeEncodePattern", encodeBtn);
        
        var clearBtn = new ExtremeClearButton(menu);
        clearBtn.setHalfSize(true);
        widgets.add("extremeClearPattern", clearBtn);
        
    }

    private static class ExtremeEncodeButton extends appeng.client.gui.widgets.IconButton {
        private final ExtremePatternEncodingTermMenu menu;

        public ExtremeEncodeButton(ExtremePatternEncodingTermMenu menu) {
            super(btn -> {
                if (menu.canEncode()) {
                    menu.encode();
                } else {
                }
            });
            this.menu = menu;
            this.setMessage(Component.translatable("gui.ccapplied.extreme_encode_pattern"));
        }

        @Override
        protected appeng.client.gui.Icon getIcon() {
            return appeng.client.gui.Icon.WHITE_ARROW_DOWN;
        }
    }
    
    /**
     * Custom button for clearing extreme pattern
     */
    private static class ExtremeClearButton extends appeng.client.gui.widgets.IconButton {
        private final ExtremePatternEncodingTermMenu menu;

        public ExtremeClearButton(ExtremePatternEncodingTermMenu menu) {
            super(btn -> {
                menu.clearAll();
            });
            this.menu = menu;
            this.setMessage(Component.translatable("gui.ccapplied.extreme_clear_pattern"));
        }

        @Override
        protected appeng.client.gui.Icon getIcon() {
            return appeng.client.gui.Icon.CLEAR;
        }
    }
}


