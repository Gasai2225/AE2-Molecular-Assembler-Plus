package com.gasai.ccapplied.integration.ae2.api;

import appeng.api.storage.ITerminalHost;
import net.minecraft.world.level.Level;

import com.gasai.ccapplied.parts.ExtremePatternEncodingLogic;

public interface IExtremePatternTerminalMenuHost extends ITerminalHost {
    ExtremePatternEncodingLogic getLogic();
    Level getLevel();
    void markForSave();
}
