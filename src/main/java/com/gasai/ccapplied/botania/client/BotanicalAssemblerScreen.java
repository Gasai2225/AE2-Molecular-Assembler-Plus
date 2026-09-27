package com.gasai.ccapplied.botania.client;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import com.gasai.ccapplied.botania.*;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class BotanicalAssemblerScreen extends UpgradeableScreen<BotanicalAssemblerMenu> {
    private static final int PANEL_X = 8, PANEL_Y = 32, BAR_X = 10, BAR_WIDTH = 124;
    private final IconButton nextOutputs;
    private int outputPage;

    public BotanicalAssemblerScreen(BotanicalAssemblerMenu menu, Inventory inventory, Component title, ScreenStyle style) {
        super(menu, inventory, title, style);
        ((AppEngSlot) menu.getSlots(SlotSemantics.ENCODED_PATTERN).get(0)).setIcon(Icon.BACKGROUND_ENCODED_PATTERN);
        nextOutputs = new IconButton(button -> outputPage++) {
            @Override protected Icon getIcon() { return Icon.S_ARROW_DOWN; }
            @Override public List<Component> getTooltipMessage() {
                return List.of(Component.translatable("gui.ccapplied.magic.next_outputs"));
            }
        };
        nextOutputs.setHalfSize(true);
        nextOutputs.setDisableBackground(true);
        widgets.add("nextOutputs", nextOutputs);
    }

    private ItemStack stationIcon() {
        String id = switch (menu.station()) {
            case POOL -> "mana_pool";
            case RUNIC_ALTAR -> "runic_altar";
            case APOTHECARY -> "petal_apothecary";
            case TERRA_PLATE -> "terrestrial_agglomeration_plate";
            case ELVEN_TRADE -> "elven_gateway_core";
            case BREWERY -> "botanical_brewery";
            case PURE_DAISY -> "pure_daisy";
        };
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("botania", id)));
    }

    private MagicalStationLayout.Point inputPoint(int index) {
        var station = menu.station();
        if (station == MagicalStation.BREWERY) {
            if (index == 0) return MagicalStationLayout.reagent(station);
            return MagicalStationLayout.input(station, index - 1);
        }
        if (station.hasReagent()) {
            int reagent = menu.ingredientCount > 0 ? menu.ingredientCount - 1 : 16;
            if (index == reagent) return MagicalStationLayout.reagent(station);
            if (index == 16) return null;
        }
        return MagicalStationLayout.input(station, index);
    }

    private void position(Slot slot, MagicalStationLayout.Point point) {
        var semantic = menu.getSlotSemantic(slot);
        var position = getStyle().getSlots().computeIfAbsent(semantic.id(),
                ignored -> new appeng.client.gui.style.SlotPosition());
        position.setLeft(point == null ? -9999 : PANEL_X + point.x());
        position.setTop(point == null ? -9999 : PANEL_Y + point.y());
        repositionSlots(semantic);
    }

    @Override protected void updateBeforeRender() {
        super.updateBeforeRender();
        var title = Component.translatable("block.ccapplied." + menu.station().id() + "_assembler");
        setTextContent("dialog_title", title);
        getStyle().getText().get("dialog_title").setScale(Math.min(1f, 160f / Math.max(1, font.width(title))));
        var state = Component.translatable("gui.ccapplied.assembler.state." + menu.status);
        setTextContent("state", state);
        getStyle().getText().get("state").setScale(Math.min(1f, 160f / Math.max(1, font.width(state))));
        for (int i = 0; i < 18; i++) position(menu.inputSlots().get(i), inputPoint(i));
        int perPage = MagicalStationLayout.outputsPerPage(menu.station());
        int pages = Math.max(1, (menu.resultCount + perPage - 1) / perPage);
        outputPage = Math.floorMod(outputPage, pages);
        for (int i = 0; i < 18; i++) {
            int index = i - outputPage * perPage;
            boolean shown = i < Math.max(1, menu.resultCount) && index >= 0 && index < perPage;
            position(menu.outputSlots().get(i), shown ? MagicalStationLayout.output(menu.station(), index) : null);
        }
        nextOutputs.setVisibility(pages > 1);
    }

    @Override public void drawBG(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(graphics, x, y, mouseX, mouseY, partialTicks);
        for (var semantic : List.of(SlotSemantics.PLAYER_INVENTORY, SlotSemantics.PLAYER_HOTBAR))
            for (var slot : menu.getSlots(semantic))
                socket(graphics, x + slot.x - 1, y + slot.y - 1);
        int px = x + PANEL_X, py = y + PANEL_Y;
        var emblem = MagicalStationLayout.emblem(menu.station());
        graphics.renderItem(stationIcon(), px + emblem.x(), py + emblem.y());
        switch (menu.station()) {
            case POOL, PURE_DAISY -> arrow(graphics, px + 60, py + 72);
            case ELVEN_TRADE -> arrow(graphics, px + 76, py + 72);
            case BREWERY -> arrow(graphics, px + 80, py + 58);
            case TERRA_PLATE -> Icon.ARROW_UP.getBlitter().dest(px + 46, py + 56).blit(graphics);
            case RUNIC_ALTAR -> Icon.ARROW_DOWN.getBlitter().dest(px + 48, py + 92).blit(graphics);
            case APOTHECARY -> { }
        }
        // Keep the full disabled layout visible even when a recipe uses only some sockets.
        for (int i = 0; i < menu.station().inputSlots(); i++) {
            var point = MagicalStationLayout.input(menu.station(), i);
            graphics.fill(px + point.x(), py + point.y(), px + point.x() + 16, py + point.y() + 16, 0xFFACAFC4);
        }
        for (int i = 0; i < 18; i++) {
            var slot = menu.inputSlots().get(i);
            if (slot.x < 0) continue;
            if (menu.inputEnabled(i) && menu.manualMode() && menu.status != 2 && menu.status != 3)
                socket(graphics, x + slot.x - 1, y + slot.y - 1);
            else graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFFACAFC4);
        }
        for (var slot : menu.outputSlots())
            if (slot.x >= 0) socket(graphics, x + slot.x - 1, y + slot.y - 1);
        var patternSlot = menu.getSlots(SlotSemantics.ENCODED_PATTERN).get(0);
        socket(graphics, x + patternSlot.x - 1, y + patternSlot.y - 1);
        bar(graphics, x + BAR_X, y + 191, menu.progress / 100.0, 0xFF77BC83);
        bar(graphics, x + BAR_X, y + 200, menu.requiredMana == 0 ? 0 : (double) menu.mana / menu.requiredMana, 0xFF56BFE6);
        var pattern = menu.displayedPattern();
        if (menu.station() == MagicalStation.POOL && pattern != null && pattern.catalyst() != PoolCatalyst.NONE)
            graphics.renderItem(new ItemStack(pattern.catalyst().state().getBlock()), x + 66, y + 152);
    }

    private static void bar(GuiGraphics graphics, int x, int y, double fraction, int color) {
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + 6, 0xFF55586D);
        graphics.fill(x, y, x + BAR_WIDTH, y + 5, 0xFF292C3B);
        graphics.fill(x, y, x + (int) (BAR_WIDTH * Math.max(0, Math.min(1, fraction))), y + 5, color);
    }
    private static void socket(GuiGraphics graphics, int x, int y) {
        Blitter.texture("guis/pattern_modes.png").src(6, 164, 18, 18).dest(x, y).blit(graphics);
    }
    private static void arrow(GuiGraphics graphics, int x, int y) {
        Blitter.texture("guis/pattern_modes.png").src(64, 24, 26, 18).dest(x, y, 19, 12).blit(graphics);
    }

    @Override protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        int rx = x - leftPos, ry = y - topPos;
        if (rx >= BAR_X - 1 && rx <= BAR_X + BAR_WIDTH + 1 && ry >= 190 && ry <= 206) {
            graphics.renderTooltip(font, ry < 199
                    ? Component.translatable("gui.ccapplied.assembler.progress", menu.progress)
                    : Component.translatable("gui.ccapplied.assembler.mana_bar", menu.mana, menu.requiredMana), x, y);
        } else if (hoveredSlot == menu.getSlots(SlotSemantics.ENCODED_PATTERN).get(0) && !hoveredSlot.hasItem()) {
            var pattern = menu.displayedPattern();
            if (pattern == null) graphics.renderTooltip(font,
                    Component.translatable("gui.ccapplied.assembler.pattern_hint"), x, y);
            else graphics.renderTooltip(font, pattern.getDefinition().toStack(), x, y);
        } else super.renderTooltip(graphics, x, y);
    }
}
