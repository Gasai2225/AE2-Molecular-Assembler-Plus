package com.gasai.ccapplied.integration.ae2.api;

import net.minecraft.world.level.Level;

public interface IExtremePatternTerminalLogicHost {
    Level getLevel();
    void markForSave();
}
