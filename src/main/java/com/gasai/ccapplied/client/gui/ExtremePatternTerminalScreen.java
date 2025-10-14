package com.gasai.ccapplied.client.gui;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.common.menu.ExtremePatternTerminalMenu;
import appeng.parts.encoding.EncodingMode;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Клиентский экран для экстремального терминала кодирования паттернов
 * Предоставляет интерфейс для кодирования паттернов с сеткой 9x9
 */
public class ExtremePatternTerminalScreen extends AbstractContainerScreen<ExtremePatternTerminalMenu> {

    private static final ResourceLocation TEXTURE = 
        new ResourceLocation(CCApplied.MODID, "textures/gui/extreme_pattern_terminal.png");

    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 166;

    public ExtremePatternTerminalScreen(ExtremePatternTerminalMenu menu, 
                                       Inventory playerInventory, 
                                       Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // TODO: Добавить кнопки для переключения режимов, кодирования и т.д.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        
        // Отрисовка в зависимости от режима
        renderModeSpecificElements(graphics, x, y, partialTick);
    }

    private void renderModeSpecificElements(GuiGraphics graphics, int x, int y, float partialTick) {
        EncodingMode mode = menu.getMode();
        
        switch (mode) {
            case CRAFTING -> renderExtremeCraftingMode(graphics, x, y);
            case PROCESSING -> renderProcessingMode(graphics, x, y);
            default -> {}
        }
    }

    private void renderExtremeCraftingMode(GuiGraphics graphics, int x, int y) {
        // TODO: Отрисовка элементов для режима экстремального крафтинга
        // Сетка 9x9 для крафта
    }

    private void renderProcessingMode(GuiGraphics graphics, int x, int y) {
        // TODO: Отрисовка элементов для режима обработки
        // Входные и выходные слоты
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Заголовок
        graphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        // Инвентарь игрока
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}

