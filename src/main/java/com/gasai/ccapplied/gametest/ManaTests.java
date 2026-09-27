package com.gasai.ccapplied.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageCells;
import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.botania.*;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CCApplied.MODID)
@PrefixGameTestTemplate(false)
public final class ManaTests {
    private ManaTests() {}

    @GameTest(template = "empty")
    public static void manaCellPersistence(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var stack = new ItemStack(BotaniaContent.MANA_CELLS.get(0).get());
        var cell = StorageCells.getCellInventory(stack, null);
        var action = IActionSource.empty();
        helper.assertTrue(cell != null, "AE2 does not recognize the mana cell");
        helper.assertTrue(cell.insert(ManaKey.INSTANCE, 1_000_000, Actionable.SIMULATE, action) == 1_000_000,
                "Cell cannot hold one pool");
        helper.assertTrue(cell.getAvailableStacks().isEmpty(), "Simulation inserted mana");
        helper.assertTrue(cell.insert(ManaKey.INSTANCE, 1_000_000, Actionable.MODULATE, action) == 1_000_000,
                "Insertion failed");
        cell.persist();
        var restored = StorageCells.getCellInventory(stack.copy(), null);
        helper.assertTrue(restored.extract(ManaKey.INSTANCE, Long.MAX_VALUE, Actionable.MODULATE, action) == 1_000_000,
                "Mana was lost on save");
        helper.assertTrue(restored.extract(ManaKey.INSTANCE, 1, Actionable.MODULATE, action) == 0,
                "Mana was duplicated");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void refusedManaTransferIsRecoverable(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var endpoint = new TestPool(1_000_000);
        var buffer = new ManaTransferBuffer();
        var refusing = new MEStorage() {
            @Override public long insert(AEKey key, long amount, Actionable mode, IActionSource action) {
                return mode == Actionable.SIMULATE ? amount : 0;
            }
            @Override public Component getDescription() { return Component.literal("refusing"); }
        };
        buffer.importTo(endpoint, refusing, IActionSource.empty(), 1_000_000);
        helper.assertTrue(endpoint.stored() == 0 && buffer.amount() == 1_000_000,
                "Refused insertion lost or duplicated mana");
        var capsule = ManaCapsuleItem.filled(buffer.amount());
        helper.assertTrue(ManaCapsuleItem.amount(capsule.copy()) == 1_000_000, "Recovery capsule lost mana");
        var restored = new ManaTransferBuffer();
        restored.restore(ManaCapsuleItem.amount(capsule));
        var cell = StorageCells.getCellInventory(new ItemStack(BotaniaContent.MANA_CELLS.get(0).get()), null);
        restored.flushTo(cell, IActionSource.empty(), 1_000_000);
        helper.assertTrue(restored.amount() == 0 && cell.getAvailableStacks().get(ManaKey.INSTANCE) == 1_000_000,
                "Buffer recovery failed");
        var full = new TestPool(1_000_000);
        restored.exportTo(cell, full, IActionSource.empty(), 1_000_000);
        helper.assertTrue(cell.getAvailableStacks().get(ManaKey.INSTANCE) == 1_000_000,
                "A full pool drained the cell");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void botaniaPoolRoundTrip(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("botania", "mana_pool")));
        helper.runAfterDelay(2, () -> {
            var pool = BotaniaManaAccess.pool(helper.getBlockEntity(pos));
            helper.assertTrue(pool != null, "Botania pool API is missing");
            helper.assertTrue(pool.insert(1_000_000) == 1_000_000, "Pool capacity mismatch");
            var cell = StorageCells.getCellInventory(new ItemStack(BotaniaContent.MANA_CELLS.get(0).get()), null);
            var buffer = new ManaTransferBuffer();
            helper.assertTrue(buffer.importTo(pool, cell, IActionSource.empty(), 1_000_000) == 1_000_000,
                    "One-operation pool import failed");
            helper.assertTrue(pool.stored() == 0, "Import duplicated mana");
            helper.assertTrue(buffer.exportTo(cell, pool, IActionSource.empty(), 1_000_000) == 1_000_000,
                    "One-operation pool export failed");
            helper.assertTrue(pool.stored() == 1_000_000 && cell.getAvailableStacks().isEmpty(),
                    "Round trip changed the total mana");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void manaBusMenuAndPersistence(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        helper.assertTrue(CCApplied.makeId("mana_bus").equals(BuiltInRegistries.MENU.getKey(ManaBusMenu.TYPE)),
                "Mana menu is not registered under our namespace");
        for (var item : java.util.List.of(BotaniaContent.MANA_IMPORT_BUS.get(), BotaniaContent.MANA_EXPORT_BUS.get())) {
            helper.setBlock(new BlockPos(1, 1, 1), net.minecraft.world.level.block.Blocks.AIR);
            var pos = helper.absolutePos(new BlockPos(1, 1, 1));
            var host = appeng.api.parts.PartHelper.placePartHost(null, helper.getLevel(), pos);
            helper.assertTrue(host != null, "Cannot place cable host");
            var bus = (ManaBusPart) host.addPart((appeng.api.parts.IPartItem<?>) item, net.minecraft.core.Direction.NORTH, null);
            helper.assertTrue(bus != null, "Mana bus is not an AE2 part");
            long baseLimit = bus.transferLimit();
            for (int slot = 0; slot < 4; slot++)
                bus.getUpgrades().setItemDirect(slot, appeng.core.definitions.AEItems.SPEED_CARD.stack());
            bus.getUpgrades().setItemDirect(4, appeng.core.definitions.AEItems.REDSTONE_CARD.stack());
            helper.assertTrue(bus.transferLimit() >= baseLimit, "Acceleration decreased throughput");
            helper.assertTrue(bus.getUpgrades().getInstalledUpgrades(appeng.core.definitions.AEItems.SPEED_CARD) == 4,
                    "Acceleration cards are not supported");
            bus.getConfigManager().putSetting(appeng.api.config.Settings.REDSTONE_CONTROLLED, appeng.api.config.RedstoneMode.LOW_SIGNAL);
            var tag = new net.minecraft.nbt.CompoundTag();
            bus.writeToNBT(tag, helper.getLevel().registryAccess());
            tag.putLong("ccManaBuffer", 12345);
            bus.readFromNBT(tag, helper.getLevel().registryAccess());
            helper.assertTrue(bus.bufferedMana() == 12345 && bus.getRSMode() == appeng.api.config.RedstoneMode.LOW_SIGNAL,
                    "Bus did not restore mana and redstone mode");
            var drops = new java.util.ArrayList<ItemStack>();
            bus.addAdditionalDrops(drops, true);
            helper.assertTrue(drops.stream().mapToLong(ManaCapsuleItem::amount).sum() == 12345,
                    "Wrench removal lost buffer contents");
            var menu = new ManaBusMenu(1, helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).getInventory(), bus);
            helper.assertTrue(menu.getSlots(appeng.menu.SlotSemantics.UPGRADE).size() == 5, "Missing upgrade slots");
            menu.broadcastChanges();
            bus.clearContent();
            helper.assertTrue(bus.bufferedMana() == 0 && bus.getUpgrades().isEmpty(), "Clearing the bus leaves duplicate drops");
        }
        for (String id : new String[] {"botania/mana_import_bus", "botania/mana_export_bus"})
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(CCApplied.makeId(id)).isPresent(), "Missing bus recipe: " + id);
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void creativePoolImport(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("botania", "creative_mana_pool")));
        helper.runAfterDelay(2, () -> {
            var pool = BotaniaManaAccess.pool(helper.getBlockEntity(pos));
            helper.assertTrue(pool != null && pool.stored() > 0, "Missing creative pool");
            long original = pool.stored();
            var cell = StorageCells.getCellInventory(new ItemStack(BotaniaContent.MANA_CELLS.get(1).get()), null);
            var buffer = new ManaTransferBuffer();
            for (int n = 0; n < 2; n++)
                helper.assertTrue(buffer.importTo(pool, cell, IActionSource.empty(), 62_500) == 62_500,
                        "Creative pool did not supply mana");
            helper.assertTrue(pool.stored() == original && cell.getAvailableStacks().get(ManaKey.INSTANCE) == 125_000,
                    "Creative pool transfer accounting is wrong");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void poweredNetworkImportsCreativeMana(GameTestHelper helper) {
        testPoweredTransfer(helper, true, false);
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void poweredNetworkImportsNormalMana(GameTestHelper helper) {
        testPoweredTransfer(helper, false, false);
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void poweredNetworkExportsMana(GameTestHelper helper) {
        testPoweredTransfer(helper, false, true);
    }

    private static void testPoweredTransfer(GameTestHelper helper, boolean creative, boolean exporting) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var pos = new BlockPos(1, 1, 1);
        var poolPos = new BlockPos(1, 1, 0);
        helper.setBlock(new BlockPos(1, 0, 1), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(poolPos, BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                "botania", creative ? "creative_mana_pool" : "mana_pool")));
        helper.setBlock(new BlockPos(0, 1, 1), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        var host = appeng.api.parts.PartHelper.placePartHost(null, helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(host != null, "Missing cable host");
        host.addPart(appeng.core.definitions.AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT), null, null);
        var item = exporting ? BotaniaContent.MANA_EXPORT_BUS.get() : BotaniaContent.MANA_IMPORT_BUS.get();
        var bus = (ManaBusPart) host.addPart((appeng.api.parts.IPartItem<?>) item,
                net.minecraft.core.Direction.NORTH, null);
        helper.assertTrue(bus != null, "Could not attach mana import bus");
        // Saving may notify this part again. An unchanged neighbor update must never save recursively.
        for (int n = 0; n < 10; n++)
            bus.onNeighborChanged(helper.getLevel(), helper.absolutePos(pos), helper.absolutePos(poolPos));
        helper.runAfterDelay(60, () -> {
            var pool = BotaniaManaAccess.pool(helper.getBlockEntity(poolPos));
            if (!creative && !exporting) pool.insert(1_000_000);
            helper.assertTrue(bus.getMainNode().isActive(), "Test network has no power/channel");
            var cell = StorageCells.getCellInventory(new ItemStack(BotaniaContent.MANA_CELLS.get(1).get()), null);
            var grid = bus.getMainNode().getNode().getGrid();
            if (exporting) cell.insert(ManaKey.INSTANCE, 1_000_000, Actionable.MODULATE, IActionSource.empty());
            appeng.api.storage.IStorageProvider provider = mounts -> mounts.mount(cell);
            grid.getStorageService().addGlobalStorageProvider(provider);
            helper.runAfterDelay(20, () -> {
                long stored = cell.getAvailableStacks().get(ManaKey.INSTANCE);
                helper.assertTrue(exporting ? pool.stored() > 0 : stored > 0, "Powered bus did not transfer mana");
                helper.assertTrue(creative || stored + pool.stored() + bus.bufferedMana() == 1_000_000,
                        "Network import lost or duplicated normal mana");
                grid.getStorageService().removeGlobalStorageProvider(provider);
                helper.succeed();
            });
        });
    }

    private static final class TestPool implements ManaEndpoint {
        private long mana;
        TestPool(long mana) { this.mana = mana; }
        @Override public long stored() { return mana; }
        @Override public long space() { return 1_000_000 - mana; }
        @Override public long extract(long amount) {
            long taken = Math.min(mana, Math.max(0, amount));
            mana -= taken;
            return taken;
        }
        @Override public long insert(long amount) {
            long added = Math.min(space(), Math.max(0, amount));
            mana += added;
            return added;
        }
    }
}
