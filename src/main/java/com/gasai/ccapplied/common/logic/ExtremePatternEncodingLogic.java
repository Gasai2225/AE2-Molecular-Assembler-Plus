package com.gasai.ccapplied.common.logic;

import appeng.parts.encoding.PatternEncodingLogic;

/**
 * Логика кодирования экстремальных паттернов
 * Расширяет PatternEncodingLogic из AE2 для поддержки сетки 9x9
 */
public class ExtremePatternEncodingLogic extends PatternEncodingLogic {

    public ExtremePatternEncodingLogic(IExtremePatternTerminalLogicHost host) {
        super(host);
    }

    /**
     * Интерфейс хоста для логики экстремального терминала паттернов
     */
    public interface IExtremePatternTerminalLogicHost extends appeng.helpers.IPatternTerminalLogicHost {
    }
}

