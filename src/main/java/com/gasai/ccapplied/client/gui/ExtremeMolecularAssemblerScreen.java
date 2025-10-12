package com.gasai.ccapplied.client.gui;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.client.gui.menu.ExtremeMolecularAssemblerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ExtremeMolecularAssemblerScreen extends AbstractContainerScreen<ExtremeMolecularAssemblerMenu> {

    private static final ResourceLocation BG =
            new ResourceLocation(CCApplied.MODID, "textures/gui/extreme_molecular_assembler.png");
    private static final ResourceLocation STATES =
            new ResourceLocation(CCApplied.MODID, "textures/gui/states.png");
    private static final ResourceLocation UPGRADE_PANEL =
            new ResourceLocation(CCApplied.MODID, "textures/gui/upgrade_slots.png");

    private static final int BG_TEX_W = 320, BG_TEX_H = 310;
    private static final int WIN_W = 269, WIN_H = 275;

    public ExtremeMolecularAssemblerScreen(ExtremeMolecularAssemblerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = WIN_W;
        this.imageHeight = WIN_H;
    }

    @Override protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
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
        final int x = leftPos, y = topPos;

        // Фон 0..269 x 0..274
        g.blit(BG, x, y, 0, 0, WIN_W, WIN_H, BG_TEX_W, BG_TEX_H);

        // 9×9 плашки (смещение +1,+1)
        final int SLOT_U = 192, SLOT_V = 192, SLOT_W = 18, SLOT_H = 18;
        final int gx0 = x + 9, gy0 = y + 16, step = 18;

        boolean hasPattern = this.menu.slots.get(ExtremeMolecularAssemblerMenu.PATTERN_IDX).hasItem();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int sx = gx0 + c * step;
                int sy = gy0 + r * step;
                g.blit(STATES, sx - 1, sy - 1, SLOT_U, SLOT_V, SLOT_W, SLOT_H, 256, 256);

                // серим неактивные (по маске с сервера). если паттерна нет — серим все.
                boolean enabled = hasPattern && menu.isGridSlotEnabledClient(r * 9 + c);
                if (!enabled) {
                    g.fill(sx, sy, sx + 16, sy + 16, 0x88FFFFFF); // сильнее белая вуаль
                }
            }
        }

        // Прогресс: dst 245/88..250/104
        final int DST_X0 = x + 245, DST_Y0 = y + 88, DST_W = 6, DST_H = 16;
        final int SRC_U0 = 279, SRC_V0 = 195, SRC_W = 18, SRC_H = 6;

        int pct = menu.getProgressPercent();
        if (pct > 0) {
            int h = Math.max(1, (pct * DST_H) / 100);
            for (int dy = 0; dy < h; dy++) {
                int u = SRC_U0 + (SRC_W - 1) - (dy * SRC_W) / Math.max(1, (DST_H - 1));
                for (int dx = 0; dx < DST_W; dx++) {
                    g.blit(BG, DST_X0 + dx, (DST_Y0 + (DST_H - h)) + dy, u, SRC_V0, 1, SRC_H, BG_TEX_W, BG_TEX_H);
                }
            }
        }

        // Панель апгрейдов (полотно 32x104). Слоты начинаются с 8/8, шаг 18 вниз.
        final int panelX = x + 275, panelY = y;
        g.blit(UPGRADE_PANEL, panelX, panelY, 0, 0, 32, 104, 32, 104);

        // Внутренний state-оверлей (states 240/208..256/224)
        final int STATE_U = 240, STATE_V = 208, STATE_S = 16;
        final int overlayX = panelX + 8;
        int slots = Math.min(menu.getUpgradesCount(), 5);
        for (int i = 0; i < slots; i++) {
            int sy = panelY + 8 + i * 18;
            g.blit(STATES, overlayX, sy, STATE_U, STATE_V, STATE_S, STATE_S, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, 8, 6, 0x404040, false);
        g.drawString(this.font, this.playerInventoryTitle, 55, 183, 0x404040, false);
    }
}
