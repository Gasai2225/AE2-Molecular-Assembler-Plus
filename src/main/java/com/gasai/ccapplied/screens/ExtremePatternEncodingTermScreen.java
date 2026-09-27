package com.gasai.ccapplied.screens;

import appeng.client.gui.Icon;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import com.gasai.ccapplied.menus.ExtremePatternEncodingTermMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ExtremePatternEncodingTermScreen extends MEStorageScreen<ExtremePatternEncodingTermMenu> {
    public ExtremePatternEncodingTermScreen(ExtremePatternEncodingTermMenu menu, Inventory inventory,
            Component title, ScreenStyle style) {
        super(menu, inventory, title, style);
        var encode = new IconButton(button -> {
            if (menu.canEncode()) {
                menu.encode();
            }
        }) {
            @Override
            protected Icon getIcon() {
                return Icon.WHITE_ARROW_DOWN;
            }
        };
        encode.setMessage(Component.translatable("gui.ccapplied.extreme_encode_pattern"));
        widgets.add("extremeEncodePattern", encode);

        var clear = new IconButton(button -> menu.clearAll()) {
            @Override
            protected Icon getIcon() {
                return Icon.CLEAR;
            }
        };
        clear.setMessage(Component.translatable("gui.ccapplied.extreme_clear_pattern"));
        clear.setHalfSize(true);
        widgets.add("extremeClearPattern", clear);
    }
}


