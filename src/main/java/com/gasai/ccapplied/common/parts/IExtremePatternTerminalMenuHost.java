package com.gasai.ccapplied.common.parts;

import com.gasai.ccapplied.common.logic.ExtremePatternEncodingLogic;
import appeng.helpers.IPatternTerminalMenuHost;

/**
 * Интерфейс хоста для меню экстремального терминала паттернов
 */
public interface IExtremePatternTerminalMenuHost extends IPatternTerminalMenuHost {
    
    @Override
    ExtremePatternEncodingLogic getLogic();
}

