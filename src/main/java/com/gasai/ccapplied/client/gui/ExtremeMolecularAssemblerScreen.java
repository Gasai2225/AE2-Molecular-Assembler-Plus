package com.gasai.ccapplied.client.gui;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.client.gui.menu.ExtremeMolecularAssemblerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ExtremeMolecularAssemblerScreen extends AbstractContainerScreen<ExtremeMolecularAssemblerMenu> {

    // ОДИН спрайт-лист: и фон, и полоска на нём же.
    private static final ResourceLocation SHEET =
            new ResourceLocation(CCApplied.MODID, "textures/gui/ext_assembler_sheet.png");

    // Твой лист 320×310:
    private static final int SHEET_W = 320;
    private static final int SHEET_H = 310;

    public ExtremeMolecularAssemblerScreen(ExtremeMolecularAssemblerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = SHEET_W;
        this.imageHeight = SHEET_H;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width  - this.imageWidth)  / 2;
        this.topPos  = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // ФОН: весь лист 1:1
        g.blit(SHEET, leftPos, topPos, 0, 0, imageWidth, imageHeight, SHEET_W, SHEET_H);

        // === ПРОГРЕСС-БАР (вырезаем из ЭТОГО ЖЕ PNG) ===
        // Экранные координаты области у выхода (как на твоём макете):
        final int barX = leftPos + 242;   // место возле выхода по экрану
        final int barY = topPos  + 116;   // верх области
        final int barW = 10;              // ширина индикатора
        final int barH = 22;              // высота индикатора

        // Координаты "кусочка" (UV) В САМОМ PNG-ЛИСТЕ,
        // где лежит цветная вертикальная полоска (нижняя радужная метка).
        // !!! Если промахнётся на пару пикселей — поправь эти два числа !!!
        final int PROG_U = 236;   // <— X в листе (подгони по своему PNG)
        final int PROG_V = 290;   // <— Y в листе (подгони по своему PNG)
        final int PROG_W = barW;  // 1:1 по размерам
        final int PROG_H = barH;

        int pct = menu.getProgressPercent(); // 0..100 (берётся из ContainerData)
        if (pct > 0) {
            int h = (pct * PROG_H) / 100;          // сколько пикселей рисуем
            int srcV = PROG_V + (PROG_H - h);      // режем снизу вверх
            int dstY = barY  + (PROG_H - h);

            g.blit(SHEET, barX, dstY, PROG_U, srcV, PROG_W, h, SHEET_W, SHEET_H);
        }
    }
}
