package com.gasai.ccapplied.gametest;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.botania.*;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
import appeng.api.stacks.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CCApplied.MODID)
@PrefixGameTestTemplate(false)
public final class BotanicalAssemblerTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void speedCardsReduceNaturalCraftTime(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var machines = new BotanicalAssemblerBlockEntity[5];
        var completed = new int[5];
        java.util.Arrays.fill(completed, -1);
        helper.setBlock(new BlockPos(1, 1, 1), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        var positions = List.of(new BlockPos(0, 1, 1), new BlockPos(0, 1, 0), new BlockPos(1, 1, 0),
                new BlockPos(2, 1, 0), new BlockPos(2, 1, 1));
        for (int cards = 0; cards <= 4; cards++) {
            var pos = positions.get(cards);
            helper.setBlock(pos, BotanicalAssemblers.ENTRIES.get(MagicalStation.POOL).block().get());
            machines[cards] = (BotanicalAssemblerBlockEntity) helper.getBlockEntity(pos);
            for (int slot = 0; slot < cards; slot++)
                machines[cards].getUpgrades().setItemDirect(slot, appeng.core.definitions.AEItems.SPEED_CARD.stack());
        }
        helper.runAfterDelay(60, () -> {
            var inputs = List.of(new ItemStack(Items.IRON_INGOT));
            var recipe = MagicalRecipeResolver.find(helper.getLevel(), MagicalStation.POOL, PoolCatalyst.NONE, inputs);
            var pattern = MagicalPatternItem.readPattern(MagicalPatternItem.encode(helper.getLevel(),
                    MagicalStation.POOL, PoolCatalyst.NONE, recipe.recipeId(), inputs), helper.getLevel());
            for (var machine : machines)
                helper.assertTrue(machine.pushPattern(pattern, table(pattern), Direction.WEST), "Craft rejected");
        });
        // Schedule before execution: adding to GameTest's task map inside a running task can rehash its iterator.
        for (int tick = 1; tick <= 30; tick++) {
            final int time = tick;
            helper.runAfterDelay(60 + tick, () -> {
                for (int cards = 0; cards <= 4; cards++)
                    if (completed[cards] < 0 && machines[cards].activePattern().isEmpty()) completed[cards] = time;
            });
        }
        helper.runAfterDelay(91, () -> {
            for (int cards = 0; cards <= 4; cards++) {
                helper.assertTrue(completed[cards] > 0, "Craft never finished with " + cards + " cards");
                if (cards > 0) helper.assertTrue(completed[cards] < completed[cards - 1],
                        "Speed card has no effect: " + java.util.Arrays.toString(completed));
                helper.assertTrue(machines[cards].reservedMana() == 0
                        && machines[cards].getInternalInventory().getStackInSlot(0).getCount() == 1,
                        "Speed cards changed output/mana accounting");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void networkTicksUpdateOpenMenuProgress(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BotanicalAssemblers.ENTRIES.get(MagicalStation.POOL).block().get());
        helper.setBlock(new BlockPos(0, 1, 1), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        var machine = (BotanicalAssemblerBlockEntity) helper.getBlockEntity(pos);
        var menus = new BotanicalAssemblerMenu[1];
        helper.runAfterDelay(60, () -> {
            menus[0] = new BotanicalAssemblerMenu(1, helper.makeMockPlayer().getInventory(), machine);
            var inputs = List.of(new ItemStack(Items.IRON_INGOT));
            var recipe = MagicalRecipeResolver.find(helper.getLevel(), MagicalStation.POOL, PoolCatalyst.NONE, inputs);
            var pattern = MagicalPatternItem.readPattern(MagicalPatternItem.encode(helper.getLevel(),
                    MagicalStation.POOL, PoolCatalyst.NONE, recipe.recipeId(), inputs), helper.getLevel());
            helper.assertTrue(machine.pushPattern(pattern, table(pattern), Direction.WEST), "Craft rejected");
        });
        for (int tick = 1; tick <= 25; tick++)
            helper.runAfterDelay(60 + tick, () -> menus[0].broadcastChanges());
        helper.runAfterDelay(65, () -> {
            var menu = menus[0];
            menu.broadcastChanges();
            helper.assertTrue(menu.status == 2 && menu.progress > 0 && menu.progress < 100,
                    "Open menu never receives intermediate progress: " + menu.progress);
            helper.assertTrue(menu.progress == machine.craftProgress(), "Menu progress is stale");
        });
        helper.runAfterDelay(85, () -> {
            var menu = menus[0];
            menu.broadcastChanges();
            helper.assertTrue(machine.activePattern().isEmpty() && menu.status == 3,
                    "Natural network ticks did not complete the craft");
            helper.assertTrue(!menu.outputSlots().get(0).getItem().isEmpty(), "Completed output lost");
            helper.succeed();
        });
    }
    @GameTest(template = "empty")
    public static void modelTexturesAndMenuRegistration(GameTestHelper helper) throws Exception {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        helper.assertTrue(CCApplied.makeId("botanical_assembler").equals(
                net.minecraft.core.registries.BuiltInRegistries.MENU.getKey(BotanicalAssemblerMenu.TYPE)), "Wrong menu registry");
        for (var station : MagicalStation.values()) {
            var model = modelJson("block/" + station.id() + "_assembler");
            var entry = BotanicalAssemblers.ENTRIES.get(station);
            var drops = net.minecraft.world.level.block.Block.getDrops(entry.block().get().defaultBlockState(),
                    helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), null);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(entry.item().get()) && drops.get(0).getCount() == 1,
                    "Optional drop tag must drop exactly one assembler: " + station);
            helper.assertTrue(model.get("parent").getAsString().equals("ccapplied:block/botanical_assembler_frame"),
                    "Assembler does not share native shell: " + station);
            helper.assertTrue(!model.has("elements"), "Station must use native runtime models, not fabricated cubes");
            var item = modelJson("item/" + station.id() + "_assembler");
            helper.assertTrue(item.get("parent").getAsString().equals("builtin/entity"), "Missing station item renderer");
        }
        var shell = modelJson("block/botanical_assembler_frame");
        helper.assertTrue(shell.getAsJsonObject("textures").get("outside").getAsString().equals("ae2:block/molecular_assembler"),
                "Shell must retain AE2 texture and its UV layout");
        var elements = shell.getAsJsonArray("elements");
        helper.assertTrue(elements.size() == 42, "Expected 12 native beams and 6 faces split into frame/window regions");
        for (int i = 0; i < elements.size(); i++) {
            for (var face : elements.get(i).getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                var data = face.getValue().getAsJsonObject();
                // AE2 15.x intentionally uses geometry-derived UVs on some faces.
                helper.assertTrue(data.get("texture").getAsString().equals("#outside")
                                && (!data.has("uv") || data.getAsJsonArray("uv").size() == 4),
                        "Invalid native UV/texture");
                helper.assertTrue(data.has("tintindex") == (i >= 30 || i % 5 != 4), "Tint only frame, not glass");
            }
        }
        for (String id : List.of("mana_pool", "runic_altar", "apothecary_default",
                "brewery", "terra_plate",
                "alfheim_portal", "natura_pylon", "pure_daisy", "livingrock",
                "livingwood_log", "glimmering_livingwood_log")) {
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(
                    new net.minecraft.resources.ResourceLocation("botania", id)),
                    "Missing native miniature model block: " + id);
        }
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(CCApplied.makeId("botania/botanical_assembler_frame")).isPresent(),
                "Missing terrasteel frame recipe");
        helper.succeed();
    }
    private static com.google.gson.JsonObject modelJson(String id) throws java.io.IOException {
        try (var stream = BotanicalAssemblerTests.class.getResourceAsStream("/assets/ccapplied/models/" + id + ".json")) {
            if (stream == null) throw new java.io.IOException("Missing model " + id);
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void atomicCraftRecoveryAndBlockedOutput(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var entry = BotanicalAssemblers.ENTRIES.get(MagicalStation.POOL);
        var pos = new BlockPos(1,1,1);
        helper.setBlock(pos, entry.block().get());
        helper.setBlock(new BlockPos(0,1,1), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.runAfterDelay(60, () -> {
            var machine = (BotanicalAssemblerBlockEntity) helper.getBlockEntity(pos);
            helper.assertTrue(machine.acceptsPlans(), "Assembler did not connect to powered ME");
            for (int i = 0; i < 4; i++) machine.getUpgrades().setItemDirect(i, appeng.core.definitions.AEItems.SPEED_CARD.stack());
            helper.assertTrue(machine.getInstalledUpgrades(appeng.core.definitions.AEItems.SPEED_CARD) == 4, "Speed cards not accepted");
            var menu = new BotanicalAssemblerMenu(1, helper.makeMockPlayer().getInventory(), machine);
            helper.assertTrue(menu.getSlots(appeng.menu.SlotSemantics.UPGRADE).size() == 4, "Missing menu upgrades");
            helper.assertTrue(menu.inputSlots().size() == 18 && menu.outputSlots().size() == 18, "Missing input/output slots");
            for (var slot : menu.inputSlots())
                helper.assertTrue(!slot.mayPlace(new ItemStack(Items.IRON_INGOT)) && !slot.mayPickup(helper.makeMockPlayer()),
                        "Job inventory can be changed through menu");
            menu.broadcastChanges();
            helper.assertTrue(appeng.api.implementations.blockentities.ICraftingMachine.of(machine, Direction.WEST) == machine,
                    "Pattern provider cannot discover crafting machine");
            var inputs = List.of(new ItemStack(Items.IRON_INGOT));
            var recipe = MagicalRecipeResolver.find(helper.getLevel(), MagicalStation.POOL, PoolCatalyst.NONE, inputs);
            helper.assertTrue(recipe != null, "Missing manasteel recipe");
            var pattern = MagicalPatternItem.readPattern(MagicalPatternItem.encode(helper.getLevel(),
                    MagicalStation.POOL, PoolCatalyst.NONE, recipe.recipeId(), inputs), helper.getLevel());
            var missingMana = table(pattern);
            for (var counter : missingMana) counter.remove(ManaKey.INSTANCE);
            helper.assertTrue(!machine.pushPattern(pattern, missingMana, Direction.WEST), "Craft accepted without mana");
            helper.assertTrue(!missingMana[0].isEmpty() && machine.getInternalInventory().isEmpty(),
                    "Rejected push consumed resources");
            var supplied = table(pattern);
            helper.assertTrue(machine.pushPattern(pattern, supplied, Direction.WEST), "Valid craft rejected");
            for (var counter : supplied) helper.assertTrue(counter.isEmpty(), "Accepted inputs were not consumed");
            var extra = table(pattern);
            helper.assertTrue(!machine.pushPattern(pattern, extra, Direction.WEST) && !extra[0].isEmpty(),
                    "Busy assembler accepted a second job");
            var saved = new net.minecraft.nbt.CompoundTag();
            machine.saveAdditional(saved);
            var restored = new BotanicalAssemblerBlockEntity(entry.type().get(), helper.absolutePos(pos), entry.block().get().defaultBlockState());
            restored.setLevel(helper.getLevel());
            restored.loadTag(saved);
            var drops = new java.util.ArrayList<ItemStack>();
            restored.addAdditionalDrops(helper.getLevel(), helper.absolutePos(pos), drops);
            helper.assertTrue(drops.stream().filter(s -> s.is(Items.IRON_INGOT)).mapToInt(ItemStack::getCount).sum() == 1,
                    "Saving unfinished work lost its input");
            helper.assertTrue(drops.stream().mapToLong(ManaCapsuleItem::amount).sum() == recipe.mana(),
                    "Saving unfinished work lost reserved mana");
            restored.clearContent();
            drops.clear();
            restored.addAdditionalDrops(helper.getLevel(), helper.absolutePos(pos), drops);
            helper.assertTrue(drops.stream().allMatch(ItemStack::isEmpty), "Clearing leaves duplicate drops");
            machine.tickingRequest(machine.getMainNode().getNode(), 1);
            machine.tickingRequest(machine.getMainNode().getNode(), Math.max(1, (recipe.ticks() + 15) / 16));
            var output = recipe.outputs().get(0);
            helper.assertTrue(machine.getInternalInventory().getStackInSlot(0).is(output.getItem()), "Craft produced wrong output");
            helper.assertTrue(!machine.acceptsPlans(), "Blocked output was overwritten");
            drops.clear();
            machine.addAdditionalDrops(helper.getLevel(), helper.absolutePos(pos), drops);
            helper.assertTrue(drops.stream().mapToLong(ManaCapsuleItem::amount).sum() == 0, "Completed craft refunds mana");
            var received = new KeyCounter();
            appeng.api.storage.MEStorage sink = new appeng.api.storage.MEStorage() {
                @Override public net.minecraft.network.chat.Component getDescription() { return net.minecraft.network.chat.Component.literal("Test"); }
                @Override public long insert(AEKey key, long amount, appeng.api.config.Actionable mode,
                        appeng.api.networking.security.IActionSource source) {
                    if (mode == appeng.api.config.Actionable.MODULATE) received.add(key, amount);
                    return amount;
                }
            };
            var grid = machine.getMainNode().getGrid();
            appeng.api.storage.IStorageProvider provider = mounts -> mounts.mount(sink);
            grid.getStorageService().addGlobalStorageProvider(provider);
            machine.tickingRequest(machine.getMainNode().getNode(), 1);
            helper.assertTrue(received.get(AEItemKey.of(output)) == output.getCount() && machine.acceptsPlans(),
                    "Output did not reach ME exactly once");
            machine.tickingRequest(machine.getMainNode().getNode(), 1);
            helper.assertTrue(received.get(AEItemKey.of(output)) == output.getCount(), "Repeated tick duplicated output");
            grid.getStorageService().removeGlobalStorageProvider(provider);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void stationRoutingRejectsForeignPatternsWithoutConsuming(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var machines = new java.util.EnumMap<MagicalStation, BotanicalAssemblerBlockEntity>(MagicalStation.class);
        int[][] positions = {{0,0},{1,0},{2,0},{0,1},{2,1},{0,2},{1,2}};
        helper.setBlock(new BlockPos(1,1,1), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        for (var station : MagicalStation.values()) {
            int[] p = positions[station.ordinal()];
            var pos = new BlockPos(p[0],1,p[1]);
            helper.setBlock(pos, BotanicalAssemblers.ENTRIES.get(station).block().get());
            machines.put(station, (BotanicalAssemblerBlockEntity) helper.getBlockEntity(pos));
        }
        helper.runAfterDelay(60, () -> {
            for (var source : MagicalStation.values()) {
                var pattern = samplePattern(helper, source);
                for (var target : MagicalStation.values()) {
                    var machine = machines.get(target);
                    helper.assertTrue(machine.acceptsPlans(), "Station not ready: " + target);
                    var menu = new BotanicalAssemblerMenu(1, helper.makeMockPlayer().getInventory(), machine);
                    var slot = menu.getSlots(appeng.menu.SlotSemantics.ENCODED_PATTERN).get(0);
                    helper.assertTrue(slot.mayPlace(pattern.getDefinition().toStack()) == (source == target),
                            "Wrong manual template filter: " + source + " -> " + target);
                    if (source == target) continue;
                    var supplied = table(pattern);
                    helper.assertTrue(!machine.pushPattern(pattern, supplied, Direction.WEST),
                            "Foreign job accepted: " + source + " -> " + target);
                    assertUnchanged(helper, pattern, supplied);
                    helper.assertTrue(machine.getInternalInventory().isEmpty() && machine.reservedMana() == 0,
                            "Rejected job modified machine");
                }
                var own = machines.get(source);
                var supplied = table(pattern);
                helper.assertTrue(own.pushPattern(pattern, supplied, Direction.WEST), "Own recipe rejected: " + source);
                for (var counter : supplied) helper.assertTrue(counter.isEmpty(), "Accepted input not transferred");
                own.tickingRequest(own.getMainNode().getNode(), 1);
                own.tickingRequest(own.getMainNode().getNode(), pattern.recipe().ticks());
                for (int i = 0; i < pattern.recipe().outputs().size(); i++) {
                    var expected = pattern.recipe().outputs().get(i);
                    var actual = own.getInternalInventory().getStackInSlot(i);
                    helper.assertTrue(AEItemKey.of(expected).equals(AEItemKey.of(actual))
                                    && actual.getCount() == expected.getCount(), "Wrong output: " + source);
                }
                own.clearContent();
            }
            helper.succeed();
        });
    }

    private static void assertUnchanged(GameTestHelper helper, MagicalPattern pattern, KeyCounter[] supplied) {
        for (int i = 0; i < supplied.length; i++) {
            var expected = pattern.getInputs()[i];
            helper.assertTrue(supplied[i].get(expected.getPossibleInputs()[0].what()) == expected.getMultiplier(),
                    "Rejected pattern consumed resources");
        }
    }

    private static MagicalPattern samplePattern(GameTestHelper helper, MagicalStation station) {
        for (var holder : helper.getLevel().getRecipeManager().getRecipes()) {
            var recipe = holder;
            var inputs = MagicalPatternTests.sample(recipe);
            if (inputs.isEmpty()) continue;
            for (var catalyst : PoolCatalyst.values()) {
                var encoded = MagicalPatternItem.encode(helper.getLevel(), station, catalyst, holder.getId(), inputs);
                var pattern = MagicalPatternItem.readPattern(encoded, helper.getLevel());
                if (pattern != null) return pattern;
            }
        }
        throw new IllegalStateException("Missing test recipe: " + station);
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void manualTemplateReservesManaAndPersists(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var entry = BotanicalAssemblers.ENTRIES.get(MagicalStation.POOL);
        var pos = new BlockPos(1,1,1);
        helper.setBlock(pos, entry.block().get());
        helper.setBlock(new BlockPos(0,1,1), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.runAfterDelay(60, () -> {
            var machine = (BotanicalAssemblerBlockEntity) helper.getBlockEntity(pos);
            var inputs = List.of(new ItemStack(Items.IRON_INGOT));
            var recipe = MagicalRecipeResolver.find(helper.getLevel(), MagicalStation.POOL, PoolCatalyst.NONE, inputs);
            helper.assertTrue(recipe != null && recipe.mana() > 0, "Missing manasteel recipe");
            var pattern = MagicalPatternItem.readPattern(MagicalPatternItem.encode(helper.getLevel(),
                    MagicalStation.POOL, PoolCatalyst.NONE, recipe.recipeId(), inputs), helper.getLevel());
            var player = helper.makeMockPlayer();
            var menu = new BotanicalAssemblerMenu(1, player.getInventory(), machine);
            var patternSlot = menu.getSlots(appeng.menu.SlotSemantics.ENCODED_PATTERN).get(0);
            helper.assertTrue(!patternSlot.mayPlace(new ItemStack(BotaniaContent.MAGICAL_PATTERN.get())),
                    "Blank pattern accepted");
            helper.assertTrue(machine.templateInventory().insertItem(0, pattern.getDefinition().toStack(), false).isEmpty(),
                    "Own encoded template refused");
            menu.broadcastChanges();
            helper.assertTrue(menu.requiredMana == recipe.mana() && menu.station() == MagicalStation.POOL,
                    "Missing station/mana menu sync");
            helper.assertTrue(menu.inputSlots().get(0).mayPlace(inputs.get(0))
                            && !menu.inputSlots().get(1).mayPlace(inputs.get(0)), "Manual slot filter broken: "
                            + menu.status + " / " + machine.manualEditable() + " / "
                            + machine.isValidManualInput(0, inputs.get(0)) + " / "
                            + menu.inputSlots().get(0).getSlotIndex() + " / "
                            + menu.inputSlots().get(1).getSlotIndex());
            helper.assertTrue(!menu.inputSlots().get(0).mayPlace(new ItemStack(Items.DIAMOND)), "Unrelated ingredient accepted");
            var automation = machine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(IllegalStateException::new);
            helper.assertTrue(!automation.insertItem(0, new ItemStack(Items.DIAMOND), false).isEmpty()
                    && automation.insertItem(0, inputs.get(0).copy(), true).isEmpty(), "Automation bypasses input filter");
            helper.assertTrue(machine.manualInventory().insertItem(0, inputs.get(0).copy(), false).isEmpty(),
                    "Valid manual ingredient refused");
            helper.assertTrue(!patternSlot.mayPickup(player), "Template removable with pending ingredients");
            var supplied = table(pattern);
            helper.assertTrue(machine.acceptsPlans() && !machine.pushPattern(pattern, supplied, Direction.WEST),
                    "Configured assembler must reject provider dispatch and prevent inventory fallback");
            assertUnchanged(helper, pattern, supplied);
            machine.tickingRequest(machine.getMainNode().getNode(), 1);
            helper.assertTrue(machine.activePattern().isEmpty() && machine.manualInventory().getStackInSlot(0).is(Items.IRON_INGOT),
                    "No-mana attempt consumed ingredients");
            var tag = new net.minecraft.nbt.CompoundTag();
            machine.saveAdditional(tag);
            var restored = new BotanicalAssemblerBlockEntity(entry.type().get(), helper.absolutePos(pos), entry.block().get().defaultBlockState());
            restored.setLevel(helper.getLevel());
            restored.loadTag(tag);
            helper.assertTrue(restored.configuredPattern() != null
                    && restored.manualInventory().getStackInSlot(0).is(Items.IRON_INGOT), "Lost pending manual inputs/template");
            var drops = new java.util.ArrayList<ItemStack>();
            restored.addAdditionalDrops(helper.getLevel(), helper.absolutePos(pos), drops);
            helper.assertTrue(drops.stream().filter(s -> s.is(BotaniaContent.MAGICAL_PATTERN.get())).mapToInt(ItemStack::getCount).sum() == 1,
                    "Template drops duplicated/lost");
            helper.assertTrue(drops.stream().filter(s -> s.is(Items.IRON_INGOT)).mapToInt(ItemStack::getCount).sum() == 1,
                    "Manual ingredient drops duplicated/lost");

            long[] balance = {recipe.mana() * 2};
            appeng.api.storage.MEStorage manaSource = new appeng.api.storage.MEStorage() {
                @Override public net.minecraft.network.chat.Component getDescription() {
                    return net.minecraft.network.chat.Component.literal("Mana test storage");
                }
                @Override public void getAvailableStacks(KeyCounter out) { out.add(ManaKey.INSTANCE, balance[0]); }
                @Override public long extract(AEKey key, long amount, appeng.api.config.Actionable mode,
                        appeng.api.networking.security.IActionSource source) {
                    if (key != ManaKey.INSTANCE) return 0;
                    long taken = Math.min(balance[0], amount);
                    if (mode == appeng.api.config.Actionable.MODULATE) balance[0] -= taken;
                    return taken;
                }
            };
            var storageService = machine.getMainNode().getGrid().getStorageService();
            appeng.api.storage.IStorageProvider provider = mounts -> mounts.mount(manaSource);
            storageService.addGlobalStorageProvider(provider);
            machine.tickingRequest(machine.getMainNode().getNode(), 1);
            menu.broadcastChanges();
            helper.assertTrue(balance[0] == recipe.mana() && menu.mana == recipe.mana()
                    && machine.manualInventory().isEmpty() && !machine.activePattern().isEmpty(), "Mana was not reserved atomically");
            helper.assertTrue(!patternSlot.mayPickup(player) && !menu.inputSlots().get(0).mayPickup(player)
                    && !menu.inputSlots().get(0).mayPlace(inputs.get(0)), "Active job can be modified");
            helper.assertTrue(automation.extractItem(18, 1, false).isEmpty()
                    && !automation.insertItem(0, inputs.get(0).copy(), false).isEmpty(), "Automation can modify active job");
            machine.saveAdditional(tag);
            restored.loadTag(tag);
            drops.clear();
            restored.addAdditionalDrops(helper.getLevel(), helper.absolutePos(pos), drops);
            helper.assertTrue(drops.stream().mapToLong(ManaCapsuleItem::amount).sum() == recipe.mana(),
                    "Saved manual job lost its reserved mana");
            helper.assertTrue(drops.stream().filter(s -> s.is(BotaniaContent.MAGICAL_PATTERN.get())).mapToInt(ItemStack::getCount).sum() == 1,
                    "Active job duplicated template");
            helper.assertTrue(drops.stream().filter(s -> s.is(Items.IRON_INGOT)).mapToInt(ItemStack::getCount).sum() == 1,
                    "Reload duplicated manual and active-job ingredients");
            machine.tickingRequest(machine.getMainNode().getNode(), recipe.ticks());
            helper.assertTrue(balance[0] == recipe.mana()
                    && machine.getInternalInventory().getStackInSlot(0).is(recipe.outputs().get(0).getItem()),
                    "Manual recipe failed or consumed mana twice");
            menu.broadcastChanges();
            helper.assertTrue(menu.outputSlots().get(0).mayPickup(player)
                    && !menu.outputSlots().get(0).mayPlace(inputs.get(0)), "Output slot permissions wrong");
            machine.tickingRequest(machine.getMainNode().getNode(), recipe.ticks());
            helper.assertTrue(balance[0] == recipe.mana(), "Blocked output consumed more mana");
            storageService.removeGlobalStorageProvider(provider);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void changedRecipeCannotReplacePromisedOutput(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BotanicalAssemblers.ENTRIES.get(MagicalStation.POOL).block().get());
        helper.setBlock(new BlockPos(0, 1, 1), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.runAfterDelay(60, () -> {
            var machine = (BotanicalAssemblerBlockEntity) helper.getBlockEntity(pos);
            var inputs = List.of(new ItemStack(Items.IRON_INGOT));
            var recipe = MagicalRecipeResolver.find(helper.getLevel(), MagicalStation.POOL, PoolCatalyst.NONE, inputs);
            var pattern = MagicalPatternItem.readPattern(MagicalPatternItem.encode(helper.getLevel(),
                    MagicalStation.POOL, PoolCatalyst.NONE, recipe.recipeId(), inputs), helper.getLevel());
            var manager = helper.getLevel().getRecipeManager();
            var original = new java.util.ArrayList<>(manager.getRecipes());
            var nativeRecipe = (vazkii.botania.api.recipe.ManaInfusionRecipe) manager.byKey(recipe.recipeId()).orElseThrow();
            var changedOutput = recipe.outputs().get(0).copyWithCount(recipe.outputs().get(0).getCount() + 1);
            var changed = new vazkii.botania.common.crafting.ManaInfusionRecipe(recipe.recipeId(), changedOutput,
                    net.minecraft.world.item.crafting.Ingredient.of(Items.IRON_INGOT), (int) recipe.mana(), "",
                    nativeRecipe.getRecipeCatalyst());
            var modified = new java.util.ArrayList<>(original);
            modified.removeIf(entry -> entry.getId().equals(recipe.recipeId()));
            modified.add(changed);
            try {
                manager.replaceRecipes(modified);
                var offered = table(pattern);
                helper.assertTrue(!machine.pushPattern(pattern, offered, Direction.WEST)
                        && !offered[0].isEmpty(), "Stale CPU plan accepted or its inputs consumed");
                manager.replaceRecipes(original);
                helper.assertTrue(machine.pushPattern(pattern, table(pattern), Direction.WEST), "Valid job rejected");
                machine.tickingRequest(machine.getMainNode().getNode(), 1);
                manager.replaceRecipes(modified);
                var saved = new net.minecraft.nbt.CompoundTag();
                machine.saveAdditional(saved);
                machine.loadTag(saved);
                machine.tickingRequest(machine.getMainNode().getNode(), 200);
                helper.assertTrue(!machine.activePattern().isEmpty()
                        && machine.getInternalInventory().getStackInSlot(0).is(Items.IRON_INGOT)
                        && machine.reservedMana() == recipe.mana(), "Reloaded job crafted changed output or lost resources");
                var drops = new java.util.ArrayList<ItemStack>();
                machine.addAdditionalDrops(helper.getLevel(), helper.absolutePos(pos), drops);
                helper.assertTrue(drops.stream().noneMatch(stack -> stack.is(changedOutput.getItem())),
                        "Promised output snapshot became a physical item");
                manager.replaceRecipes(original);
                machine.tickingRequest(machine.getMainNode().getNode(), 200);
                helper.assertTrue(machine.activePattern().isEmpty()
                        && ItemStack.matches(machine.getInternalInventory().getStackInSlot(0), recipe.outputs().get(0)),
                        "Restoring recipe did not resume the original job");
                helper.succeed();
            } finally {
                manager.replaceRecipes(original);
            }
        });
    }

    private static KeyCounter[] table(MagicalPattern pattern) {
        var inputs = pattern.getInputs();
        var result = new KeyCounter[inputs.length];
        for (int i=0; i<inputs.length; i++) {
            result[i] = new KeyCounter();
            result[i].add(inputs[i].getPossibleInputs()[0].what(), inputs[i].getMultiplier());
        }
        return result;
    }
}
