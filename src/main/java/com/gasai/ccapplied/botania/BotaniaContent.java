package com.gasai.ccapplied.botania;

import appeng.api.stacks.AEKeyTypes;
import com.gasai.ccapplied.core.registry.CCItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class BotaniaContent {
    public static final Supplier<Item> ASSEMBLER_FRAME = CCItems.ITEMS.register("botanical_assembler_frame", () -> new Item(new Item.Properties()));
    public static final Supplier<net.minecraft.world.inventory.MenuType<BotanicalAssemblerMenu>> ASSEMBLER_MENU =
            com.gasai.ccapplied.core.registry.CCMenuTypes.MENUS.register("botanical_assembler", () -> BotanicalAssemblerMenu.TYPE);
    public static final Supplier<Item> MAGICAL_TERMINAL = CCItems.ITEMS.register("magical_terminal",
            () -> new appeng.items.parts.PartItem<>(new Item.Properties(), MagicalTerminalPart.class, MagicalTerminalPart::new));
    public static final Supplier<net.minecraft.world.inventory.MenuType<MagicalTerminalMenu>> MAGICAL_TERMINAL_MENU =
            com.gasai.ccapplied.core.registry.CCMenuTypes.MENUS.register("magical_terminal", () -> MagicalTerminalMenu.TYPE);
    public static final Supplier<Item> MAGICAL_PATTERN = CCItems.ITEMS.register("magical_pattern", MagicalPatternItem::new);
    public static final Supplier<Item> MANA_IMPORT_BUS = bus("mana_import_bus");
    public static final Supplier<Item> MANA_EXPORT_BUS = bus("mana_export_bus");
    public static final Supplier<net.minecraft.world.inventory.MenuType<ManaBusMenu>> MANA_BUS_MENU =
            com.gasai.ccapplied.core.registry.CCMenuTypes.MENUS.register("mana_bus", () -> ManaBusMenu.TYPE);
    public static final Supplier<Item> MANA_CAPSULE = CCItems.ITEMS.register("mana_capsule", ManaCapsuleItem::new);
    public static final List<Supplier<Item>> MANA_CELLS = List.of(
            cell(1), cell(4), cell(16), cell(64), cell(256));

    private BotaniaContent() {}

    private static Supplier<Item> bus(String name) {
        return CCItems.ITEMS.register(name, () -> new appeng.items.parts.PartItem<>(
                new Item.Properties(), ManaBusPart.class, ManaBusPart::new));
    }

    private static Supplier<Item> cell(int kilobytes) {
        return CCItems.ITEMS.register("mana_cell_" + kilobytes + "k", () -> new ManaCellItem(kilobytes));
    }

    public static void register(IEventBus bus) {
        BotanicalAssemblers.init();
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            for (var entry : BotanicalAssemblers.ENTRIES.values())
                appeng.api.upgrades.Upgrades.add(appeng.core.definitions.AEItems.SPEED_CARD, entry.item().get(), 4);
        }));
        appeng.api.crafting.PatternDetailsHelper.registerDecoder(MagicalPatternItem.DECODER);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            for (var item : List.of(MANA_IMPORT_BUS.get(), MANA_EXPORT_BUS.get())) {
                appeng.api.upgrades.Upgrades.add(appeng.core.definitions.AEItems.SPEED_CARD, item, 4, "gui.ccapplied.mana_bus");
                appeng.api.upgrades.Upgrades.add(appeng.core.definitions.AEItems.REDSTONE_CARD, item, 1, "gui.ccapplied.mana_bus");
            }
        }));
        bus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK)) AEKeyTypes.register(ManaKeyType.INSTANCE);
        });
    }

    public static void addCreativeItems(net.minecraft.world.item.CreativeModeTab.Output output) {
        output.accept(ASSEMBLER_FRAME.get());
        BotanicalAssemblers.ENTRIES.values().forEach(entry -> output.accept(entry.item().get()));
        output.accept(MAGICAL_TERMINAL.get());
        output.accept(MAGICAL_PATTERN.get());
        MANA_CELLS.forEach(cell -> output.accept(cell.get()));
        output.accept(MANA_IMPORT_BUS.get());
        output.accept(MANA_EXPORT_BUS.get());
    }
}
