package com.gasai.ccapplied.gametest;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.botania.*;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import vazkii.botania.api.recipe.*;

@GameTestHolder(CCApplied.MODID)
@PrefixGameTestTemplate(false)
public final class MagicalPatternTests {
    @GameTest(template = "empty")
    public static void allStationsEncodeRealRecipes(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        for (var station : MagicalStation.values()) {
            boolean found = false;
            for (var holder : helper.getLevel().getRecipeManager().getRecipes()) {
                var recipe = holder.value();
                var inputs = sample(recipe);
                if (inputs.isEmpty()) continue;
                for (var catalyst : PoolCatalyst.values()) {
                    var result = MagicalRecipeResolver.resolve(helper.getLevel(), station, catalyst, holder.id(), inputs);
                    if (result == null) continue;
                    var encoded = MagicalPatternItem.encode(helper.getLevel(), station, catalyst, holder.id(), inputs);
                    var decoded = (MagicalPattern) appeng.api.crafting.PatternDetailsHelper.decodePattern(encoded.copy(), helper.getLevel());
                    helper.assertTrue(decoded != null, "AE2 cannot decode " + station);
                    helper.assertTrue(encoded.getMaxStackSize() == 1, "Encoded pattern is stackable");
                    helper.assertTrue(new ItemStack(BotaniaContent.MAGICAL_PATTERN.get()).getMaxStackSize() == 64,
                            "Blank patterns should stack");
                    helper.assertTrue(encoded.getItem() instanceof appeng.crafting.pattern.EncodedPatternItem,
                            "Pattern is not compatible with AE2's Shift preview");
                    helper.assertTrue(decoded.station() == station && decoded.catalyst() == catalyst, "Lost station/catalyst");
                    var table = new appeng.api.stacks.KeyCounter[decoded.getInputs().length];
                    for (int n = 0; n < table.length; n++) {
                        table[n] = new appeng.api.stacks.KeyCounter();
                        var input = decoded.getInputs()[n];
                        table[n].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
                    }
                    helper.assertTrue(BotanicalAssemblerBlockEntity.validateInputs(decoded, table, helper.getLevel()) != null,
                            "Assembler rejects native inputs: " + station);
                    table[0].add(decoded.getInputs()[0].getPossibleInputs()[0].what(), 1);
                    helper.assertTrue(BotanicalAssemblerBlockEntity.validateInputs(decoded, table, helper.getLevel()) == null,
                            "Assembler silently consumes excess inputs: " + station);
                    helper.assertTrue(helper.getLevel().getRecipeManager().byKey(
                            CCApplied.makeId("botania/" + station.id() + "_assembler")).isPresent(), "Missing assembler recipe: " + station);
                    helper.assertTrue(decoded.equals(MagicalPatternItem.readPattern(decoded.getDefinition().toStack(), helper.getLevel())),
                            "Pattern changed after save");
                    helper.assertTrue(!decoded.supportsPushInputsToExternalInventory(), "Magical pattern may be sent to an ordinary inventory");
                    long mana = 0;
                    for (var input : decoded.getInputs())
                        if (input.getPossibleInputs()[0].what() == ManaKey.INSTANCE)
                            mana += input.getMultiplier() * input.getPossibleInputs()[0].amount();
                    helper.assertTrue(mana == result.mana(), "Crafting CPU does not see the correct mana cost");
                    found = true;
                    break;
                }
                if (found) break;
            }
            helper.assertTrue(found, "No real recipe supported for " + station);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void poolCatalystAndInvalidPatterns(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        for (var mode : PoolCatalyst.values()) {
            boolean found = false;
            for (var holder : helper.getLevel().getRecipeManager().getRecipes()) {
                var recipe = holder.value();
                if (!(recipe instanceof ManaInfusionRecipe)) continue;
                var inputs = sample(recipe);
                var result = MagicalRecipeResolver.resolve(helper.getLevel(), MagicalStation.POOL, mode, holder.id(), inputs);
                if (result == null) continue;
                for (var wrong : PoolCatalyst.values()) if (wrong != mode)
                    helper.assertTrue(MagicalRecipeResolver.resolve(helper.getLevel(), MagicalStation.POOL, wrong, holder.id(), inputs) == null,
                            "Recipe accepted wrong pool catalyst");
                helper.assertTrue(MagicalPatternItem.encode(helper.getLevel(), MagicalStation.APOTHECARY, PoolCatalyst.NONE,
                        holder.id(), inputs).isEmpty(), "Recipe accepted wrong station");
                var encoded = MagicalPatternItem.encode(helper.getLevel(), MagicalStation.POOL, mode, holder.id(), inputs);
                var bad = encoded.copy();
                var root = MagicalPatternItem.data(bad);
                root.putInt("version", 999);
                var tag = new net.minecraft.nbt.CompoundTag();
                tag.put("ccMagicPattern", root);
                bad.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                helper.assertTrue(MagicalPatternItem.readPattern(bad, helper.getLevel()) == null, "Unknown pattern version accepted");
                helper.assertTrue(MagicalRecipeResolver.resolve(helper.getLevel(), MagicalStation.POOL, mode, holder.id(),
                        List.of(inputs.get(0).copyWithCount(64))) == null, "Oversized ingredient stack accepted");
                found = true;
                break;
            }
            helper.assertTrue(found, "Missing pool mode " + mode);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void encoderConsumesOneBlankAndPersists(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(new net.minecraft.core.BlockPos(0, 1, 1), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        var host = appeng.api.parts.PartHelper.placePartHost(null, helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(host != null, "Missing terminal host");
        host.addPart(appeng.core.definitions.AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT), null, null);
        var terminal = (MagicalTerminalPart) host.addPart((appeng.api.parts.IPartItem<?>) BotaniaContent.MAGICAL_TERMINAL.get(),
                net.minecraft.core.Direction.NORTH, null);
        terminal.ghosts.setItemDirect(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
        terminal.patterns.setItemDirect(0, new ItemStack(BotaniaContent.MAGICAL_PATTERN.get(), 2));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(terminal.isActive(), "Encoder has no channel/power");
            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var menu = new MagicalTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.getSlots(appeng.menu.SlotSemantics.PLAYER_INVENTORY).size() == 27
                    && menu.getSlots(appeng.menu.SlotSemantics.PLAYER_HOTBAR).size() == 9, "Duplicated player slots");
            helper.assertTrue(menu.getSlots(appeng.menu.SlotSemantics.VIEW_CELL).size() == 5, "Missing ME view cells");
            helper.assertTrue(terminal.getInventory() != null, "Terminal is not connected to ME storage");
            menu.selectStation(-1);
            helper.assertTrue(terminal.station() == MagicalStation.POOL, "Invalid station changed the menu");
            menu.broadcastChanges();
            helper.assertTrue(menu.valid && menu.mana > 0, "Encoder cannot preview manasteel");
            for (var slot : menu.resultSlots())
                helper.assertTrue(!slot.mayPickup(player), "Preview output can be taken for free");
            menu.encode();
            helper.assertTrue(terminal.patterns.getStackInSlot(0).getCount() == 1, "Encoder consumed wrong blank count");
            helper.assertTrue(MagicalPatternItem.readPattern(terminal.patterns.getStackInSlot(1), helper.getLevel()) != null,
                    "Encoder produced invalid pattern");
            menu.encode();
            helper.assertTrue(terminal.patterns.getStackInSlot(0).getCount() == 1, "Occupied output consumed another blank");
            var tag = new net.minecraft.nbt.CompoundTag();
            terminal.writeToNBT(tag, helper.getLevel().registryAccess());
            terminal.readFromNBT(tag, helper.getLevel().registryAccess());
            helper.assertTrue(terminal.patterns.getStackInSlot(0).getCount() == 1
                    && MagicalPatternItem.isEncoded(terminal.patterns.getStackInSlot(1)), "Pattern inventory lost on save");
            var drops = new ArrayList<ItemStack>();
            terminal.addAdditionalDrops(drops, true);
            helper.assertTrue(drops.stream().noneMatch(s -> s.is(net.minecraft.world.item.Items.IRON_INGOT)),
                    "Ghost ingredients became real drops");
            helper.assertTrue(drops.stream().mapToInt(ItemStack::getCount).sum() == 2, "Pattern drops lost or duplicated");
            for (var station : MagicalStation.values()) {
                menu.selectStation(station.ordinal());
                menu.broadcastChanges();
                helper.assertTrue(menu.ingredientSlots().stream().filter(net.minecraft.world.inventory.Slot::isActive).count()
                        == station.inputSlots(), "Wrong visible ingredient count for " + station);
                helper.assertTrue(menu.getSlots(MagicalTerminalMenu.REAGENT).get(0).isActive() == station.hasReagent(),
                        "Wrong reagent visibility for " + station);
            }
            menu.ingredientSlots().get(1).set(new ItemStack(net.minecraft.world.item.Items.DIAMOND));
            helper.assertTrue(terminal.ghosts.getStackInSlot(1).isEmpty(), "Hidden Daisy slot accepted an item");
            helper.assertTrue(terminal.inputs().isEmpty(), "Old ghost inputs leaked into a different station");
            menu.setSubstitutions(true);
            terminal.writeToNBT(tag, helper.getLevel().registryAccess());
            terminal.readFromNBT(tag, helper.getLevel().registryAccess());
            helper.assertTrue(terminal.substitutions(), "Lost substitution setting on save");
            menu.selectCatalyst(1);
            helper.assertTrue(terminal.catalyst() == PoolCatalyst.NONE, "Daisy accepted a pool catalyst");
            menu.selectStation(MagicalStation.POOL.ordinal());
            menu.selectCatalyst(2);
            helper.assertTrue(terminal.catalyst() == PoolCatalyst.CONJURATION, "Direct catalyst selection failed");
            helper.assertTrue(!terminal.substitutions(), "Daisy substitution setting leaked into another station");
            var visited = java.util.EnumSet.noneOf(MagicalStation.class);
            var catalysts = java.util.EnumSet.noneOf(PoolCatalyst.class);
            for (var registered : helper.getLevel().getRecipeManager().getRecipes()) {
                var id = registered.id();
                var transfer = MagicalRecipeTransfer.prepare(helper.getLevel(), id);
                if (transfer == null) continue;
                menu.applyJeiRecipe(id.toString());
                menu.broadcastChanges();
                helper.assertTrue(menu.valid && terminal.station() == transfer.station()
                        && terminal.catalyst() == transfer.catalyst(), "JEI transfer lost station/catalyst: " + id);
                var actual = terminal.inputs();
                helper.assertTrue(actual.size() == transfer.inputs().size(), "JEI transfer lost ingredient quantity");
                for (int i = 0; i < actual.size(); i++)
                    helper.assertTrue(ItemStack.matches(actual.get(i), transfer.inputs().get(i)),
                            "JEI transfer misplaced reagent/container: " + id);
                visited.add(transfer.station());
                if (transfer.station() == MagicalStation.POOL) catalysts.add(transfer.catalyst());
            }
            helper.assertTrue(visited.size() == MagicalStation.values().length, "Missing JEI station: " + visited);
            helper.assertTrue(catalysts.size() == PoolCatalyst.values().length, "Missing JEI catalyst: " + catalysts);
            var previous = terminal.inputs();
            var previousStation = terminal.station();
            menu.applyJeiRecipe("invalid recipe id");
            menu.applyJeiRecipe("ccapplied:missing_recipe");
            menu.applyJeiRecipe("minecraft:crafting_table");
            helper.assertTrue(terminal.station() == previousStation && terminal.inputs().size() == previous.size(),
                    "Invalid JEI transfer cleared the editor");
            for (int i = 0; i < previous.size(); i++)
                helper.assertTrue(ItemStack.matches(previous.get(i), terminal.inputs().get(i)), "Invalid transfer changed ghosts");
            helper.assertTrue(terminal.patterns.getStackInSlot(0).getCount() == 1,
                    "JEI transfer consumed a blank pattern");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void daisySubstitutionsUseTheSameRecipe(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        for (var holder : helper.getLevel().getRecipeManager().getRecipes()) {
            var recipe = holder.value();
            if (!(recipe instanceof PureDaisyRecipe)) continue;
            var ingredients = sample(recipe);
            if (ingredients.size() != 1) continue;
            var id = holder.id();
            var result = MagicalRecipeResolver.resolve(helper.getLevel(), MagicalStation.PURE_DAISY, PoolCatalyst.NONE, id, ingredients);
            if (result == null) continue;
            var candidates = MagicalRecipeResolver.daisyAlternatives(helper.getLevel(), result, ingredients.get(0));
            if (candidates.size() < 2) continue;
            var exact = MagicalPatternItem.readPattern(MagicalPatternItem.encode(helper.getLevel(),
                    MagicalStation.PURE_DAISY, PoolCatalyst.NONE, id, ingredients), helper.getLevel());
            var flexible = MagicalPatternItem.readPattern(MagicalPatternItem.encode(helper.getLevel(),
                    MagicalStation.PURE_DAISY, PoolCatalyst.NONE, id, ingredients, true), helper.getLevel());
            helper.assertTrue(exact.getInputs()[0].getPossibleInputs().length == 1, "Disabled substitutions are not exact");
            helper.assertTrue(flexible.getInputs()[0].getPossibleInputs().length == candidates.size(), "Missing safe substitutions");
            for (var candidate : candidates) {
                helper.assertTrue(flexible.getInputs()[0].isValid(appeng.api.stacks.AEItemKey.of(candidate), helper.getLevel()),
                        "Valid alternative rejected");
                helper.assertTrue(MagicalRecipeResolver.resolve(helper.getLevel(), MagicalStation.PURE_DAISY, PoolCatalyst.NONE,
                        id, List.of(candidate)) != null, "Alternative changed the recipe");
            }
            helper.assertTrue(!flexible.getInputs()[0].isValid(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.DIAMOND),
                    helper.getLevel()), "Unrelated item accepted as substitution");
            helper.succeed();
            return;
        }
        helper.fail("No native Daisy recipe with alternatives was tested");
    }

    @GameTest(template = "empty")
    public static void stationLayoutsStayInsidePanel(GameTestHelper helper) {
        for (var station : MagicalStation.values()) {
            var slots = new ArrayList<MagicalStationLayout.Point>();
            for (int index = 0; index < station.inputSlots(); index++) slots.add(MagicalStationLayout.input(station, index));
            if (station.hasReagent()) slots.add(MagicalStationLayout.reagent(station));
            for (int index = 0; index < MagicalStationLayout.outputsPerPage(station); index++)
                slots.add(MagicalStationLayout.output(station, index));
            for (int a = 0; a < slots.size(); a++) {
                var point = slots.get(a);
                helper.assertTrue(point.x() >= 1 && point.y() >= 1 && point.x() + 17 <= MagicalStationLayout.WIDTH
                        && point.y() + 17 <= MagicalStationLayout.HEIGHT, "Slot outside panel: " + station);
                for (int b = a + 1; b < slots.size(); b++) {
                    var other = slots.get(b);
                    helper.assertTrue(Math.abs(point.x() - other.x()) >= 18 || Math.abs(point.y() - other.y()) >= 18,
                            "Overlapping sockets: " + station);
                }
            }
            helper.assertTrue(MagicalStationLayout.input(station, station.inputSlots()) == null, "Extra input visible");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fiveElvenSocketsKeepRecipeQuantities(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var manasteel = new ItemStack(BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("botania", "manasteel_ingot")));
        var inputs = List.of(manasteel, manasteel.copy());
        helper.assertTrue(inputs.size() == 2, "Two input slots must preserve both ingots");
        var result = MagicalRecipeResolver.find(helper.getLevel(), MagicalStation.ELVEN_TRADE, PoolCatalyst.NONE, inputs);
        helper.assertTrue(result != null, "Two-ingot elven trade is invalid");
        var pattern = MagicalPatternItem.readPattern(MagicalPatternItem.encode(helper.getLevel(),
                MagicalStation.ELVEN_TRADE, PoolCatalyst.NONE, result.recipeId(), inputs), helper.getLevel());
        helper.assertTrue(pattern != null && pattern.ingredients().size() == 2, "Trade quantity lost on encoding");
        var block = new ItemStack(BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("botania", "manasteel_block")));
        var mixedInputs = List.of(manasteel, block);
        var mixed = MagicalRecipeResolver.find(helper.getLevel(), MagicalStation.ELVEN_TRADE,
                PoolCatalyst.NONE, mixedInputs);
        helper.assertTrue(mixed != null && mixed.outputs().getFirst().getCount() == 5,
                "Mixed ingot/block trade must produce five elementium ingots");
        var mixedPattern = MagicalPatternItem.readPattern(MagicalPatternItem.encode(helper.getLevel(),
                MagicalStation.ELVEN_TRADE, PoolCatalyst.NONE, mixed.recipeId(), mixedInputs), helper.getLevel());
        helper.assertTrue(mixedPattern != null && mixedPattern.ingredients().size() == 2
                && mixedPattern.ingredients().get(1).is(block.getItem()), "Mixed ingredients lost on encoding");
        helper.assertTrue(MagicalStation.ELVEN_TRADE.inputSlots() == 5
                && MagicalStationLayout.outputsPerPage(MagicalStation.ELVEN_TRADE) == 1, "Elven UI must have five inputs and one output");
        helper.assertTrue(MagicalStationLayout.output(MagicalStation.RUNIC_ALTAR, 0).x() == 48,
                "Runic primary output no longer aligns with the arrow");
        helper.succeed();
    }

    static List<ItemStack> sample(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        var result = new ArrayList<ItemStack>();
        if (recipe instanceof PureDaisyRecipe daisy) {
            daisy.getInput().getDisplayedStacks().stream().filter(s -> s.getItem() instanceof net.minecraft.world.item.BlockItem)
                    .findFirst().ifPresent(s -> result.add(s.copyWithCount(1)));
            return result;
        }
        if (recipe instanceof BotanicalBreweryRecipe) {
            BuiltInRegistries.ITEM.stream().filter(i -> i instanceof vazkii.botania.api.brew.BrewContainer)
                    .findFirst().ifPresent(i -> result.add(new ItemStack(i)));
        }
        for (var ingredient : recipe.getIngredients()) {
            var choices = ingredient.getItems();
            if (choices.length == 0) return List.of();
            result.add(choices[0].copyWithCount(1));
        }
        if (recipe instanceof RecipeWithCatalysts<?> ritual) {
            for (var ingredient : ritual.getCatalysts()) {
                if (ingredient.getItems().length == 0) return List.of();
                result.add(ingredient.getItems()[0].copyWithCount(1));
            }
        }
        if (recipe instanceof RecipeWithReagent<?> ritual) {
            var choices = ritual.getReagent().getItems();
            if (choices.length == 0) return List.of();
            result.add(choices[0].copyWithCount(1));
        }
        return result;
    }
}
