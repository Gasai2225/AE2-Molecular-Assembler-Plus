package com.gasai.ccapplied.client.gui;

import com.gasai.ccapplied.client.gui.menu.ExtremePatternEncoderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ExtremePatternEncoderScreen extends AbstractContainerScreen<ExtremePatternEncoderMenu> {

    public ExtremePatternEncoderScreen(ExtremePatternEncoderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        // TODO: фон GUI (позже добавим)
    }
}
