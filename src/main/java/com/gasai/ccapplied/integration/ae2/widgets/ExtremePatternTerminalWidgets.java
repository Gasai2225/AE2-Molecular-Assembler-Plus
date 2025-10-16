package com.gasai.ccapplied.integration.ae2.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import appeng.api.config.ActionItems;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.IconButton;
import com.gasai.ccapplied.integration.ae2.menu.ExtremePatternEncodingTermMenu;

/**
 * Виджеты для экстремального терминала кодирования паттернов
 */
public final class ExtremePatternTerminalWidgets {

    private ExtremePatternTerminalWidgets() {}

    /**
     * Создает кнопку кодирования экстремального паттерна
     */
    public static ExtremeEncodeButton createExtremeEncodeButton(ExtremePatternEncodingTermMenu menu) {
        return new ExtremeEncodeButton(menu);
    }

    /**
     * Создает кнопку очистки экстремального паттерна
     */
    public static ExtremeClearButton createExtremeClearButton(ExtremePatternEncodingTermMenu menu) {
        return new ExtremeClearButton(menu);
    }

    /**
     * Кнопка кодирования экстремального паттерна
     */
    public static class ExtremeEncodeButton extends ActionButton {
        private final ExtremePatternEncodingTermMenu menu;

        public ExtremeEncodeButton(ExtremePatternEncodingTermMenu menu) {
            super(ActionItems.ENCODE, () -> {
                if (menu != null) {
                    menu.encode();
                }
            });
            this.menu = menu;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Показываем кнопку только если можно кодировать
            if (menu != null && menu.canEncode()) {
                this.active = true;
                this.visible = true;
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            } else {
                this.active = false;
                this.visible = false;
            }
        }

        @Override
        public void onPress() {
            if (menu != null && menu.canEncode()) {
                menu.encode();
            }
        }
    }

    /**
     * Кнопка очистки экстремального паттерна
     */
    public static class ExtremeClearButton extends ActionButton {
        private final ExtremePatternEncodingTermMenu menu;

        public ExtremeClearButton(ExtremePatternEncodingTermMenu menu) {
            super(ActionItems.CLOSE, () -> {
                if (menu != null) {
                    menu.clearAll();
                }
            });
            this.menu = menu;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Показываем кнопку только если есть что очищать
            if (menu != null && menu.hasPattern()) {
                this.active = true;
                this.visible = true;
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            } else {
                this.active = false;
                this.visible = false;
            }
        }

        @Override
        public void onPress() {
            if (menu != null && menu.hasPattern()) {
                menu.clearAll();
            }
        }
    }

    /**
     * Кнопка очистки сетки крафта
     */
    public static class ExtremeClearCraftingButton extends IconButton {
        private final ExtremePatternEncodingTermMenu menu;

        public ExtremeClearCraftingButton(ExtremePatternEncodingTermMenu menu) {
            super(btn -> {
                if (menu != null) {
                    menu.clearAll();
                }
            });
            this.menu = menu;
            this.setMessage(Component.translatable("gui.ccapplied.extreme_clear_crafting"));
        }

        @Override
        protected Icon getIcon() {
            return Icon.CLEAR;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Показываем кнопку только если есть что очищать
            if (menu != null && menu.hasPattern()) {
                this.active = true;
                this.visible = true;
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            } else {
                this.active = false;
                this.visible = false;
            }
        }

        @Override
        public void onPress() {
            if (menu != null && menu.hasPattern()) {
                menu.clearAll();
            }
        }
    }
}