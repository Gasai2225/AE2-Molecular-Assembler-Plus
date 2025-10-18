package com.gasai.ccapplied.screens;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import appeng.client.gui.widgets.ProgressBar.Direction;
import appeng.client.gui.implementations.UpgradeableScreen;
import com.gasai.ccapplied.menus.ExtremeMolecularAssemblerMenu;

public class ExtremeMolecularAssemblerScreen extends UpgradeableScreen<ExtremeMolecularAssemblerMenu> {

    private final ProgressBar pb;

    public ExtremeMolecularAssemblerScreen(ExtremeMolecularAssemblerMenu menu, Inventory playerInventory,
            Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.pb = new ProgressBar(this.menu, style.getImage("progressBar"), Direction.VERTICAL);
        widgets.add("progressBar", this.pb);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.pb.setFullMsg(Component.literal(this.menu.getCurrentProgress() + "%"));
    }
}
