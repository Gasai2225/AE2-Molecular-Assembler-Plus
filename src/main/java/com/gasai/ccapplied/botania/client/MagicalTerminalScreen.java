package com.gasai.ccapplied.botania.client;

import appeng.client.gui.Icon;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.TabButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.core.localization.ButtonToolTips;
import appeng.menu.SlotSemantics;
import com.gasai.ccapplied.botania.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MagicalTerminalScreen extends MEStorageScreen<MagicalTerminalMenu> {
    private static final int PANEL_X = 9, PANEL_BOTTOM = 252;
    private final Map<MagicalStation, TabButton> stationTabs = new EnumMap<>(MagicalStation.class);
    private final TabButton catalystButton;
    private final ToggleButton substitutions;
    private final ActionButton encode, nextOutputs;
    private int outputPage;
    private MagicalStation previousStation;

    public MagicalTerminalScreen(MagicalTerminalMenu menu, Inventory inventory, Component title, ScreenStyle style) {
        super(menu, inventory, title, style);
        ((appeng.menu.slot.AppEngSlot) menu.getSlots(SlotSemantics.BLANK_PATTERN).get(0)).setIcon(Icon.BACKGROUND_BLANK_PATTERN);
        ((appeng.menu.slot.AppEngSlot) menu.getSlots(SlotSemantics.ENCODED_PATTERN).get(0)).setIcon(Icon.BACKGROUND_ENCODED_PATTERN);
        for (var station : MagicalStation.values()) {
            var tab = new TabButton(stationIcon(station), stationName(station), button -> menu.selectStation(station.ordinal()));
            tab.setStyle(TabButton.Style.HORIZONTAL);
            widgets.add("station" + station.ordinal(), tab);
            stationTabs.put(station, tab);
        }
        catalystButton = new TabButton(Icon.CLEAR, Component.empty(), button -> menu.cycleCatalyst()) {
            @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                if (!visible) return;
                Icon.SLOT_BACKGROUND.getBlitter().dest(getX(), getY()).blit(graphics);
                var catalyst = PoolCatalyst.values()[Math.floorMod(menu.catalyst, PoolCatalyst.values().length)];
                if (catalyst == PoolCatalyst.NONE)
                    Icon.CLEAR.getBlitter().dest(getX() + 1, getY() + 1).blit(graphics);
                else graphics.renderItem(new ItemStack(catalyst.state().getBlock()), getX() + 1, getY() + 1);
            }
        };
        widgets.add("catalyst", catalystButton);
        substitutions = new ToggleButton(Icon.SUBSTITUTION_ENABLED, Icon.SUBSTITUTION_DISABLED, menu::setSubstitutions);
        substitutions.setHalfSize(true);
        substitutions.setTooltipOn(List.of(ButtonToolTips.SubstitutionsOn.text(), ButtonToolTips.SubstitutionsDescEnabled.text()));
        substitutions.setTooltipOff(List.of(ButtonToolTips.SubstitutionsOff.text(), ButtonToolTips.SubstitutionsDescDisabled.text()));
        widgets.add("substitutions", substitutions);
        encode = new ActionButton(Icon.WHITE_ARROW_DOWN, "gui.ccapplied.magic.encode", menu::encode);
        widgets.add("encode", encode);
        var clear = new ActionButton(Icon.CLEAR, "gui.ccapplied.magic.clear", menu::clearInputs);
        clear.setHalfSize(true);
        widgets.add("clear", clear);
        nextOutputs = new ActionButton(Icon.ARROW_RIGHT, "gui.ccapplied.magic.next_outputs", () -> outputPage++);
        nextOutputs.setHalfSize(true);
        widgets.add("nextOutputs", nextOutputs);
    }

    private static Component stationName(MagicalStation station) {
        return Component.translatable("gui.ccapplied.magic.station." + station.id());
    }

    private static ItemStack stationIcon(MagicalStation station) {
        String id = switch (station) {
            case POOL -> "mana_pool";
            case RUNIC_ALTAR -> "runic_altar";
            case APOTHECARY -> "apothecary_default";
            case TERRA_PLATE -> "terra_plate";
            case ELVEN_TRADE -> "alfheim_portal";
            case BREWERY -> "brewery";
            case PURE_DAISY -> "pure_daisy";
        };
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("botania", id)));
    }

    @Override protected void updateBeforeRender() {
        super.updateBeforeRender();
        var selected = menu.selectedStation();
        if (selected != previousStation) {
            outputPage = 0;
            previousStation = selected;
        }
        setTextContent("station", stationName(selected));
        getStyle().getText().get("station").setScale(Math.min(1.0f, 160.0f / Math.max(1, font.width(stationName(selected)))));
        stationTabs.forEach((station, tab) -> tab.setSelected(station == selected));
        catalystButton.visible = selected == MagicalStation.POOL;
        catalystButton.active = catalystButton.visible;
        catalystButton.setMessage(Component.translatable("gui.ccapplied.magic.catalyst." +
                PoolCatalyst.values()[Math.floorMod(menu.catalyst, PoolCatalyst.values().length)].id())
                .append("\n").append(Component.translatable("gui.ccapplied.magic.cycle_catalyst")));
        setTextContent("catalyst", catalystButton.visible
                ? Component.translatable("gui.ccapplied.magic.catalyst_label") : Component.empty());
        substitutions.setVisibility(selected == MagicalStation.PURE_DAISY);
        substitutions.setState(menu.substitutions);
        encode.active = menu.valid;
        encode.setMessage(Component.translatable(menu.valid ? "gui.ccapplied.magic.encode" : "gui.ccapplied.magic.no_recipe"));
        encode.tooltip = menu.valid
                ? List.of(encode.getMessage(), Component.translatable("gui.ccapplied.magic.mana", menu.mana))
                : List.of(encode.getMessage());
        int perPage = MagicalStationLayout.outputsPerPage(selected);
        int pages = Math.max(1, (menu.resultCount + perPage - 1) / perPage);
        outputPage = Math.floorMod(outputPage, pages);
        nextOutputs.setVisibility(MagicalStationLayout.multipleOutputs(selected) && pages > 1);
        layoutSlots(selected);
    }

    private void position(Slot slot, MagicalStationLayout.Point point) {
        var semantic = menu.getSlotSemantic(slot);
        var position = getStyle().getSlots().computeIfAbsent(semantic.id(), ignored -> new appeng.client.gui.style.SlotPosition());
        position.setLeft(point == null ? -9999 : PANEL_X + point.x());
        position.setTop(point == null ? -9999 : imageHeight - PANEL_BOTTOM + point.y());
        // Let AE2 move its slots; no reflection or access-transformer changes are needed.
        repositionSlots(semantic);
    }

    private void layoutSlots(MagicalStation station) {
        var inputs = menu.ingredientSlots();
        for (int i = 0; i < inputs.size(); i++) position(inputs.get(i), MagicalStationLayout.input(station, i));
        position(menu.getSlots(MagicalTerminalMenu.REAGENT).get(0), MagicalStationLayout.reagent(station));
        var outputs = menu.resultSlots();
        int perPage = MagicalStationLayout.outputsPerPage(station);
        for (int i = 0; i < outputs.size(); i++) {
            int pageIndex = i - outputPage * perPage;
            boolean shown = i < Math.max(1, menu.resultCount) && pageIndex >= 0 && pageIndex < perPage;
            position(outputs.get(i), shown ? MagicalStationLayout.output(station, pageIndex) : null);
        }
    }

    @Override public void drawBG(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(graphics, x, y, mouseX, mouseY, partialTicks);
        var station = menu.selectedStation();
        int px = x + PANEL_X, py = y + imageHeight - PANEL_BOTTOM;
        var emblem = MagicalStationLayout.emblem(station);
        graphics.renderItem(stationIcon(station), px + emblem.x(), py + emblem.y());
        switch (station) {
            case POOL, PURE_DAISY -> arrow(graphics, px + 60, py + 72);
            case ELVEN_TRADE -> arrow(graphics, px + 76, py + 72);
            case BREWERY -> arrow(graphics, px + 80, py + 58);
            case TERRA_PLATE -> Icon.ARROW_UP.getBlitter().dest(px + 46, py + 56).blit(graphics);
            case RUNIC_ALTAR -> Icon.ARROW_DOWN.getBlitter().dest(px + 48, py + 92).blit(graphics);
            case APOTHECARY -> { }
        }
        var sockets = new java.util.ArrayList<Slot>(menu.ingredientSlots());
        sockets.addAll(menu.resultSlots());
        for (var semantic : List.of(MagicalTerminalMenu.REAGENT, SlotSemantics.BLANK_PATTERN, SlotSemantics.ENCODED_PATTERN))
            sockets.addAll(menu.getSlots(semantic));
        for (var slot : sockets)
            if (slot.x >= 0 && slot.isActive())
                Icon.SLOT_BACKGROUND.getBlitter().dest(x + slot.x - 1, y + slot.y - 1).blit(graphics);
    }

    private static void arrow(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y + 4, x + 12, y + 8, 0xFF8B8B8B);
        for (int column = 0; column < 7; column++)
            graphics.fill(x + 12 + column, y + column, x + 13 + column, y + 12 - column, 0xFF8B8B8B);
    }

    @Override protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        var reagent = menu.getSlots(MagicalTerminalMenu.REAGENT).get(0);
        if (hoveredSlot == reagent && reagent.isActive() && !reagent.hasItem())
            graphics.renderTooltip(font, Component.translatable("gui.ccapplied.magic.reagent." + menu.selectedStation().id()), x, y);
        else super.renderTooltip(graphics, x, y);
    }

    private static final class ActionButton extends IconButton {
        private final Icon icon;
        private List<Component> tooltip = List.of();
        ActionButton(Icon icon, String label, Runnable action) {
            super(button -> action.run());
            this.icon = icon;
            setMessage(Component.translatable(label));
        }
        @Override protected Icon getIcon() { return icon; }
        @Override public List<Component> getTooltipMessage() { return tooltip.isEmpty() ? super.getTooltipMessage() : tooltip; }
    }
}
