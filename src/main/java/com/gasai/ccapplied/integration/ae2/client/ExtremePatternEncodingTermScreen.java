package com.gasai.ccapplied.integration.ae2.client;

import com.gasai.ccapplied.integration.ae2.menu.ExtremePatternEncodingTermMenu;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ExtremePatternEncodingTermScreen extends MEStorageScreen<ExtremePatternEncodingTermMenu> {
    // Текстура будет использоваться в будущем для кастомного рендеринга

    public ExtremePatternEncodingTermScreen(ExtremePatternEncodingTermMenu menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);
        
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeScreen] Constructor START for player: {}, title: {}, style: {}", 
            inv.player.getGameProfile().getName(), title.getString(), style != null ? "NOT NULL" : "NULL");
        
               try {
                   // Размеры экрана теперь определяются в JSON файле стиля
                   com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeScreen] After super() call, dimensions: {}x{}", imageWidth, imageHeight);
            
            // Инициализируем виджеты
            initializeWidgets();
            com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeScreen] Screen initialized successfully");
        } catch (Exception e) {
            com.gasai.ccapplied.CCApplied.LOG.error("[ExtremeScreen] Error in constructor", e);
            throw e;
        }
    }

    private void initializeWidgets() {
        // Инициализируем виджеты для экстремального терминала
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeScreen] Initializing extreme pattern terminal widgets");
        
        // Виджеты определяются в JSON файле и автоматически создаются AE2
        
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremeScreen] Extreme pattern terminal widgets initialized");
    }
    
    @Override
    public void drawFG(net.minecraft.client.gui.GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        // Рисуем заголовок экстремального терминала
        guiGraphics.drawString(font, "Extreme Pattern Terminal", offsetX + 8, offsetY + 6, 0x404040, false);
        guiGraphics.drawString(font, "9x9 Crafting Grid", offsetX + 8, offsetY + imageHeight - 20, 0x404040, false);
    }

    // Убираем переопределение renderBg, так как это final метод в AEBaseScreen

    // Метод рендеринга сетки будет использоваться в будущем для кастомного отображения

    // Убираем переопределение render, так как это может конфликтовать с AEBaseScreen
}


