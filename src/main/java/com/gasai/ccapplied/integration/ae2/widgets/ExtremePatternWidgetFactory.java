package com.gasai.ccapplied.integration.ae2.widgets;

import appeng.client.gui.widgets.ActionButton;
import appeng.api.config.ActionItems;
import com.gasai.ccapplied.integration.ae2.menu.ExtremePatternEncodingTermMenu;

/**
 * Фабрика виджетов для экстремального терминала кодирования паттернов
 */
public class ExtremePatternWidgetFactory {

    /**
     * Создает кнопку кодирования экстремального паттерна
     */
    public static ActionButton createExtremeEncodeButton(ExtremePatternEncodingTermMenu menu) {
        return new ActionButton(ActionItems.ENCODE, () -> {
            if (menu != null) {
                menu.encode();
            }
        });
    }

    /**
     * Создает кнопку очистки экстремального паттерна
     */
    public static ActionButton createExtremeClearButton(ExtremePatternEncodingTermMenu menu) {
        return new ActionButton(ActionItems.CLOSE, () -> {
            if (menu != null) {
                menu.clearAll();
            }
        });
    }

    /**
     * Создает кнопку очистки сетки крафта
     */
    public static ActionButton createExtremeCraftingClearButton(ExtremePatternEncodingTermMenu menu) {
        return new ActionButton(ActionItems.CLOSE, () -> {
            if (menu != null) {
                menu.clearAll();
            }
        });
    }

    /**
     * Создает виджет подстановок для экстремального крафта
     */
    public static ActionButton createExtremeCraftingSubstitutionsButton(ExtremePatternEncodingTermMenu menu) {
        // TODO: Реализовать логику подстановок для 9x9 крафта
        return new ActionButton(ActionItems.CYCLE_PROCESSING_OUTPUT, () -> {
            // Пока пустая реализация
        });
    }

    /**
     * Создает виджет подстановок жидкостей для экстремального крафта
     */
    public static ActionButton createExtremeCraftingFluidSubstitutionsButton(ExtremePatternEncodingTermMenu menu) {
        // TODO: Реализовать логику подстановок жидкостей для 9x9 крафта
        return new ActionButton(ActionItems.CYCLE_PROCESSING_OUTPUT, () -> {
            // Пока пустая реализация
        });
    }
}

