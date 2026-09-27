package com.gasai.ccapplied.screens;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.ScreenStyle;
import com.gasai.ccapplied.menus.DraconicPatternEncodingTermMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.text.NumberFormat;
import java.util.Locale;

public class DraconicPatternEncodingTermScreen extends MEStorageScreen<DraconicPatternEncodingTermMenu> {

    public DraconicPatternEncodingTermScreen(DraconicPatternEncodingTermMenu menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);
        widgets.add("draconicEncodePattern", new DraconicEncodeButton(menu));
        var clear = new DraconicClearButton(menu);
        clear.setHalfSize(true);
        widgets.add("draconicClearPattern", clear);
    }

    @Override
    protected void updateBeforeRender() {
        setTextContent("draconic_tier_label", Component.literal("Tier: " + menu.getTierText()).withStyle(getTierColor()));
        setTextContent("draconic_energy_label", Component.literal("Energy Cost: " + NumberFormat.getIntegerInstance(Locale.US).format(menu.getEnergyCost()) + " OP"));
        super.updateBeforeRender();
    }

    private ChatFormatting getTierColor() {
        return switch (menu.getTierOrdinal()) {
            case 2 -> ChatFormatting.RED;
            case 1 -> ChatFormatting.GOLD;
            case 0 -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GRAY;
        };
    }

    private static class DraconicEncodeButton extends appeng.client.gui.widgets.IconButton {
        private final DraconicPatternEncodingTermMenu menu;
        public DraconicEncodeButton(DraconicPatternEncodingTermMenu menu) {
            super(btn -> {
                if (menu.canEncode()) menu.encode();
            });
            this.menu = menu;
            this.setMessage(Component.translatable("gui.ccapplied.draconic_encode_pattern"));
        }
        @Override
        protected appeng.client.gui.Icon getIcon() {
            return appeng.client.gui.Icon.WHITE_ARROW_DOWN;
        }
    }

    private static class DraconicClearButton extends appeng.client.gui.widgets.IconButton {
        public DraconicClearButton(DraconicPatternEncodingTermMenu menu) {
            super(btn -> menu.clearAll());
            this.setMessage(Component.translatable("gui.ccapplied.draconic_clear_pattern"));
        }
        @Override
        protected appeng.client.gui.Icon getIcon() {
            return appeng.client.gui.Icon.CLEAR;
        }
    }
}
