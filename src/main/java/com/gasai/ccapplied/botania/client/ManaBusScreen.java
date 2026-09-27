package com.gasai.ccapplied.botania.client;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.core.definitions.AEItems;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import com.gasai.ccapplied.botania.ManaBusMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ManaBusScreen extends UpgradeableScreen<ManaBusMenu> {
    private final ServerSettingToggleButton<RedstoneMode> redstone =
            new ServerSettingToggleButton<>(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);

    public ManaBusScreen(ManaBusMenu menu, Inventory inventory, Component title, ScreenStyle style) {
        super(menu, inventory, title, style);
        addToLeftToolbar(redstone);
    }

    @Override public void drawBG(net.minecraft.client.gui.GuiGraphics graphics, int offsetX, int offsetY,
            int mouseX, int mouseY, float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        // Only real player slots belong on this panel; upgrades draw their own side panel.
        for (var semantics : java.util.List.of(appeng.menu.SlotSemantics.PLAYER_INVENTORY,
                appeng.menu.SlotSemantics.PLAYER_HOTBAR)) {
            for (var slot : menu.getSlots(semantics)) {
                appeng.client.gui.Icon.SLOT_BACKGROUND.getBlitter()
                        .dest(offsetX + slot.x - 1, offsetY + slot.y - 1).blit(graphics);
            }
        }
    }

    @Override protected void updateBeforeRender() {
        super.updateBeforeRender();
        redstone.set(menu.getRedStoneMode());
        redstone.setVisibility(menu.hasUpgrade(AEItems.REDSTONE_CARD));
        setTextContent("buffer", Component.translatable("gui.ccapplied.mana_buffer", menu.buffered));
        setTextContent("transfer", Component.translatable("gui.ccapplied.mana_transfer", menu.transferred));
        setTextContent("limit", Component.translatable("gui.ccapplied.mana_limit", menu.limit));
    }
}
