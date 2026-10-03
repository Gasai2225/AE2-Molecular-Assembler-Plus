package com.gasai.ccapplied.gametest;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.menu.AutoCraftingMenu;
import com.blakebr0.extendedcrafting.crafting.recipe.ShapelessTableRecipe;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.api.crafting.FusionRecipe;
import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.core.registry.CCBlocks;
import com.gasai.ccapplied.core.registry.CCItems;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
import com.gasai.ccapplied.items.DraconicEncodedPatternItem;
import com.gasai.ccapplied.patterns.DraconicFusionPattern;
import com.gasai.ccapplied.patterns.ExtremeCraftingPattern;
import com.gasai.ccapplied.tiles.ExtremeMolecularAssemblerTileEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CCApplied.MODID)
@PrefixGameTestTemplate(false)
public final class RecipeReloadTests {
    static final ResourceLocation IRON_ID = CCApplied.makeId("gametest/iron");
    private static final ResourceLocation FUSION_ID = CCApplied.makeId("gametest/fusion");

    static void withIronRecipe(GameTestHelper helper, Runnable test) {
        if (!net.neoforged.fml.ModList.get().isLoaded("extendedcrafting")) {
            helper.succeed();
            return;
        }
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        try {
            replace(helper, IRON_ID, ironRecipe(Items.IRON_INGOT, Items.GOLD_INGOT));
            test.run();
        } finally {
            manager.replaceRecipes(original);
        }
    }

    private static RecipeHolder<?> ironRecipe(net.minecraft.world.item.Item input, net.minecraft.world.item.Item output) {
        var recipe = new ShapelessTableRecipe(NonNullList.of(Ingredient.EMPTY, Ingredient.of(input)),
                new ItemStack(output), 4);
        return new RecipeHolder<>(IRON_ID, recipe);
    }

    private static RecipeHolder<?> fusionRecipe(TechLevel tier, long energy) {
        return fusionRecipe(FUSION_ID, tier, energy);
    }

    private static RecipeHolder<?> fusionRecipe(ResourceLocation id, TechLevel tier, long energy) {
        var recipe = new FusionRecipe(new ItemStack(Items.GOLD_INGOT), Ingredient.of(Items.IRON_INGOT),
                energy, tier, List.of(
                        new FusionRecipe.FusionIngredient(Ingredient.of(Items.DIAMOND), false),
                        new FusionRecipe.FusionIngredient(Ingredient.of(Items.HONEY_BOTTLE), true)));
        return new RecipeHolder<>(id, recipe);
    }

    private static void replace(GameTestHelper helper, ResourceLocation id, RecipeHolder<?> replacement) {
        var recipes = new ArrayList<>(helper.getLevel().getRecipeManager().getRecipes());
        recipes.removeIf(recipe -> recipe.id().equals(id));
        if (replacement != null) recipes.add(replacement);
        helper.getLevel().getRecipeManager().replaceRecipes(recipes);
    }

    private static DraconicFusionPattern fusionPattern(GameTestHelper helper) {
        return fusionPattern(helper, FUSION_ID);
    }

    private static DraconicFusionPattern fusionPattern(GameTestHelper helper, ResourceLocation id) {
        var inputs = new GenericStack[13];
        inputs[0] = new GenericStack(AEItemKey.of(Items.DIAMOND), 1);
        inputs[1] = new GenericStack(AEItemKey.of(Items.HONEY_BOTTLE), 1);
        inputs[12] = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1);
        var encoder = (DraconicEncodedPatternItem) CCItems.DRACONIC_FUSION_PATTERN.get();
        var encoded = encoder.encode(inputs, new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1),
                DraconicFusionPattern.FusionTier.WYVERN, 0, id, helper.getLevel().registryAccess());
        return (DraconicFusionPattern) PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
    }

    @GameTest(template = "empty")
    public static void extremeUsesCurrentRecipe(GameTestHelper helper) {
        withIronRecipe(helper, () -> {
            var pattern = (ExtremeCraftingPattern) PatternDetailsHelper.decodePattern(
                    RegressionTests.encodedIronPattern(helper), helper.getLevel());
            var grid = new TransientCraftingContainer(new AutoCraftingMenu(), 9, 9);
            grid.setItem(0, new ItemStack(Items.IRON_INGOT));
            helper.assertTrue(pattern.assemble(grid, helper.getLevel()).is(Items.GOLD_INGOT), "Current recipe rejected");
            replace(helper, IRON_ID, null);
            helper.assertTrue(pattern.assemble(grid, helper.getLevel()).isEmpty(), "Deleted recipe still crafts");
            replace(helper, IRON_ID, ironRecipe(Items.IRON_INGOT, Items.DIAMOND));
            helper.assertTrue(pattern.assemble(grid, helper.getLevel()).isEmpty(), "Changed output still crafts old result");
            replace(helper, IRON_ID, ironRecipe(Items.GOLD_INGOT, Items.GOLD_INGOT));
            helper.assertTrue(pattern.assemble(grid, helper.getLevel()).isEmpty(), "Changed ingredient ignored");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void fusionUsesCurrentTierAndEnergy(GameTestHelper helper) {
        if (!CCOptionalMods.isDraconicEvolutionLoaded()) { helper.succeed(); return; }
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        try {
            replace(helper, FUSION_ID, fusionRecipe(TechLevel.WYVERN, 100));
            var pattern = fusionPattern(helper);
            helper.assertTrue(pattern.getTotalEnergy() == 100, "Encoded energy overrides the recipe");
            replace(helper, FUSION_ID, fusionRecipe(TechLevel.CHAOTIC, 900));
            helper.assertTrue(pattern.getTier() == DraconicFusionPattern.FusionTier.CHAOTIC
                    && pattern.getTotalEnergy() == 900, "Cached pattern ignored changed tier/energy");
            var pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, CCBlocks.WYVERN_MOLECULAR_ASSEMBLER.get());
            var machine = (ExtremeMolecularAssemblerTileEntity) helper.getBlockEntity(pos);
            helper.assertTrue(!machine.pushPattern(pattern, inputs(pattern), Direction.NORTH), "Wyvern accepted chaotic recipe");
            var grid = new TransientCraftingContainer(new AutoCraftingMenu(), 9, 9);
            pattern.fillCraftingGrid(inputs(pattern), grid::setItem);
            helper.assertTrue(!pattern.assemble(grid, helper.getLevel()).isEmpty(), "Valid fusion rejected");
            replace(helper, FUSION_ID, null);
            helper.assertTrue(pattern.assemble(grid, helper.getLevel()).isEmpty(), "Deleted fusion still crafts");
            helper.succeed();
        } finally {
            manager.replaceRecipes(original);
        }
    }

    @GameTest(template = "empty")
    public static void reusableInputsAndOverflowSurviveSave(GameTestHelper helper) {
        if (!CCOptionalMods.isDraconicEvolutionLoaded()) { helper.succeed(); return; }
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        try {
            replace(helper, FUSION_ID, fusionRecipe(TechLevel.WYVERN, 0));
            var pattern = fusionPattern(helper);
            helper.assertTrue(AEItemKey.of(Items.DIAMOND).equals(
                    pattern.getInputs()[0].getRemainingKey(AEItemKey.of(Items.DIAMOND))), "CPU will consume reusable input");
            var pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, CCBlocks.WYVERN_MOLECULAR_ASSEMBLER.get());
            var machine = (ExtremeMolecularAssemblerTileEntity) helper.getBlockEntity(pos);
            helper.assertTrue(machine.pushPattern(pattern, inputs(pattern), Direction.NORTH), "Fusion inputs rejected");
            machine.getInternalInventory().setItemDirect(1, new ItemStack(Items.HONEY_BOTTLE, 2));
            machine.tickingRequest(null, 1);
            machine.tickingRequest(null, 200);
            var inventory = machine.getInternalInventory();
            helper.assertTrue(inventory.getStackInSlot(81).is(Items.GOLD_INGOT), "Fusion did not complete");
            helper.assertTrue(inventory.getStackInSlot(0).is(Items.DIAMOND), "Reusable ingredient was consumed");
            helper.assertTrue(inventory.getStackInSlot(1).is(Items.HONEY_BOTTLE)
                    && inventory.getStackInSlot(1).getCount() == 1, "Excess ingredient was lost");
            helper.assertTrue(inventory.getStackInSlot(83).is(Items.GLASS_BOTTLE), "Crafting return was lost");
            machine.tickingRequest(null, 200);
            helper.assertTrue(inventory.getStackInSlot(83).is(Items.GLASS_BOTTLE), "Blocked output overwrote overflow");
            var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
            machine.clearContent();
            machine.loadTag(saved, helper.getLevel().registryAccess());
            helper.assertTrue(inventory.getStackInSlot(83).is(Items.GLASS_BOTTLE), "Overflow did not survive save/load");
            var drops = new ArrayList<ItemStack>();
            machine.addAdditionalDrops(helper.getLevel(), helper.absolutePos(pos), drops);
            helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(Items.GLASS_BOTTLE)), "Breaking machine loses overflow");
            inventory.setItemDirect(81, ItemStack.EMPTY);
            machine.tickingRequest(null, 1);
            helper.assertTrue(inventory.getStackInSlot(81).is(Items.GLASS_BOTTLE)
                    && inventory.getStackInSlot(83).isEmpty(), "Buffered return cannot reach output");
            helper.succeed();
        } finally {
            manager.replaceRecipes(original);
        }
    }

    @GameTest(template = "empty")
    public static void extremeKeepsExcessAndContainer(GameTestHelper helper) {
        withIronRecipe(helper, () -> {
            replace(helper, IRON_ID, ironRecipe(Items.HONEY_BOTTLE, Items.GOLD_INGOT));
            var inputs = new GenericStack[81];
            inputs[0] = new GenericStack(AEItemKey.of(Items.HONEY_BOTTLE), 1);
            var encoder = (com.gasai.ccapplied.items.ExtremeEncodedPatternItem) CCItems.EXTREME_CRAFTING_PATTERN.get();
            var encoded = encoder.encode(inputs, new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1),
                    IRON_ID, helper.getLevel().registryAccess());
            var pattern = (ExtremeCraftingPattern) PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
            var grid = new TransientCraftingContainer(new AutoCraftingMenu(), 9, 9);
            grid.setItem(0, new ItemStack(Items.HONEY_BOTTLE, 2));
            var remaining = pattern.getRemainingItems(grid);
            helper.assertTrue(remaining.slots().get(0).is(Items.HONEY_BOTTLE)
                    && remaining.slots().get(0).getCount() == 1, "Excess input lost");
            helper.assertTrue(remaining.overflow().size() == 1 && remaining.overflow().get(0).is(Items.GLASS_BOTTLE),
                    "Crafting return lost with excess input");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void overlappingFusionIngredientsKeepTheirConsumptionFlags(GameTestHelper helper) {
        if (!CCOptionalMods.isDraconicEvolutionLoaded()) { helper.succeed(); return; }
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        try {
            var recipe = new FusionRecipe(new ItemStack(Items.EMERALD), Ingredient.of(Items.DIAMOND),
                    0, TechLevel.WYVERN, List.of(
                            new FusionRecipe.FusionIngredient(Ingredient.of(Items.IRON_INGOT, Items.GOLD_INGOT), false),
                            new FusionRecipe.FusionIngredient(Ingredient.of(Items.IRON_INGOT), true)));
            replace(helper, FUSION_ID, new RecipeHolder<>(FUSION_ID, recipe));
            var stacks = new ItemStack[13];
            java.util.Arrays.fill(stacks, ItemStack.EMPTY);
            stacks[0] = new ItemStack(Items.IRON_INGOT);
            stacks[1] = new ItemStack(Items.GOLD_INGOT);
            stacks[12] = new ItemStack(Items.DIAMOND);
            var match = com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper.matchRecipe(
                    FUSION_ID, stacks, helper.getLevel());
            helper.assertTrue(match != null, "Overlapping ingredients rejected a valid assignment");
            helper.assertTrue(match.consumedIngredients().equals(List.of(true, false)),
                    "Reassigned ingredients lost their consumption flags");
            helper.succeed();
        } finally {
            manager.replaceRecipes(original);
        }
    }


    @GameTest(template = "empty")
    public static void pipesDrainAllCraftingRemainders(GameTestHelper helper) {
        if (!CCOptionalMods.isDraconicEvolutionLoaded()) { helper.succeed(); return; }
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        try {
            replace(helper, FUSION_ID, fusionRecipe(TechLevel.WYVERN, 0));
            var pattern = fusionPattern(helper);
            var pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, CCBlocks.WYVERN_MOLECULAR_ASSEMBLER.get());
            var machine = (ExtremeMolecularAssemblerTileEntity) helper.getBlockEntity(pos);
            helper.assertTrue(machine.pushPattern(pattern, inputs(pattern), Direction.NORTH), "Initial job rejected");
            machine.tickingRequest(null, 1);
            machine.tickingRequest(null, 200);
            var handler = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(pos), Direction.UP);
            var extracted = new ArrayList<ItemStack>();
            for (int i = 0; i < 3; i++) {
                extracted.add(handler.extractItem(81, 64, false));
                if (!machine.getInternalInventory().isEmpty()) {
                    helper.assertTrue(!machine.getTickingRequest(null).isSleeping(),
                            "Assembler slept before the pipe drained all remainders");
                }
                machine.tickingRequest(null, 1);
            }
            helper.assertTrue(extracted.stream().anyMatch(stack -> stack.is(Items.GOLD_INGOT)), "Output lost");
            helper.assertTrue(extracted.stream().anyMatch(stack -> stack.is(Items.DIAMOND)), "Reusable ingredient stuck");
            helper.assertTrue(extracted.stream().anyMatch(stack -> stack.is(Items.GLASS_BOTTLE)), "Bottle stuck");
            helper.assertTrue(machine.getInternalInventory().isEmpty(), "Pipe did not drain inventory");
            helper.assertTrue(machine.pushPattern(pattern, inputs(pattern), Direction.NORTH), "Next job rejected after extraction");
            helper.succeed();
        } finally {
            manager.replaceRecipes(original);
        }
    }

    @GameTest(template = "empty")
    public static void replacingPatternResetsFusionProgress(GameTestHelper helper) {
        if (!CCOptionalMods.isDraconicEvolutionLoaded()) { helper.succeed(); return; }
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        try {
            var secondId = CCApplied.makeId("gametest/second_fusion");
            replace(helper, FUSION_ID, fusionRecipe(TechLevel.WYVERN, 100));
            replace(helper, secondId, fusionRecipe(secondId, TechLevel.WYVERN, 300));
            var pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, CCBlocks.WYVERN_MOLECULAR_ASSEMBLER.get());
            var machine = (ExtremeMolecularAssemblerTileEntity) helper.getBlockEntity(pos);
            machine.getInternalInventory().setItemDirect(82, fusionPattern(helper).getDefinition().toStack());
            var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
            saved.putLong("fusionEnergyAccumulated", 100);
            saved.putInt("fusionCraftTicks", 40);
            machine.loadTag(saved, helper.getLevel().registryAccess());
            var restored = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
            helper.assertTrue(restored.getLong("fusionEnergyAccumulated") == 100
                    && restored.getInt("fusionCraftTicks") == 40, "Loading the same pattern erased progress");
            machine.getInternalInventory().setItemDirect(82, fusionPattern(helper, secondId).getDefinition().toStack());
            var changed = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
            helper.assertTrue(changed.getLong("fusionEnergyAccumulated") == 0
                    && changed.getInt("fusionCraftTicks") == 0, "Replacement inherited old fusion progress");
            helper.succeed();
        } finally {
            manager.replaceRecipes(original);
        }
    }

    @GameTest(template = "empty")
    public static void extremeUsesRecipeSpecificReturns(GameTestHelper helper) {
        withIronRecipe(helper, () -> {
            var recipe = new ShapelessTableRecipe(NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.IRON_INGOT)),
                    new ItemStack(Items.GOLD_INGOT), 1);
            var specialReturn = new ItemStack(Items.DIAMOND_PICKAXE);
            specialReturn.setDamageValue(17);
            recipe.setTransformer((slot, stack) -> specialReturn.copy());
            replace(helper, IRON_ID, new RecipeHolder<>(IRON_ID, recipe));
            var encoder = (com.gasai.ccapplied.items.ExtremeEncodedPatternItem) CCItems.EXTREME_CRAFTING_PATTERN.get();
            for (int slot : new int[] {0, 40, 80}) {
                var inputs = new GenericStack[81];
                inputs[slot] = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1);
                var encoded = encoder.encode(inputs, new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1),
                        IRON_ID, helper.getLevel().registryAccess());
                var pattern = (ExtremeCraftingPattern) PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
                helper.assertTrue(AEItemKey.of(specialReturn).equals(pattern.getInputs()[0].getRemainingKey(AEItemKey.of(Items.IRON_INGOT))),
                        "CPU ignored recipe-specific return");
                var grid = new TransientCraftingContainer(new AutoCraftingMenu(), 9, 9);
                grid.setItem(slot, new ItemStack(Items.IRON_INGOT));
                helper.assertTrue(!pattern.assemble(grid, helper.getLevel()).isEmpty(), "Shifted recipe rejected");
                var remaining = pattern.getRemainingItems(grid);
                helper.assertTrue(ItemStack.matches(remaining.slots().get(slot), specialReturn),
                        "Native return was lost or moved to another slot");
                helper.assertTrue(remaining.slots().stream().filter(stack -> !stack.isEmpty()).count() == 1,
                        "Native return duplicated");
                grid.setItem(slot, new ItemStack(Items.IRON_INGOT, 2));
                remaining = pattern.getRemainingItems(grid);
                helper.assertTrue(remaining.slots().get(slot).is(Items.IRON_INGOT)
                        && remaining.slots().get(slot).getCount() == 1
                        && remaining.overflow().size() == 1
                        && ItemStack.matches(remaining.overflow().get(0), specialReturn),
                        "Native return or surplus lost when both need the same slot");
            }
            helper.succeed();
        });
    }


    @GameTest(template = "empty", timeoutTicks = 200)
    public static void nativeAwakeningKeepsFourCatalystBlocks(GameTestHelper helper) {
        if (!CCOptionalMods.isDraconicEvolutionLoaded()) { helper.succeed(); return; }
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(new BlockPos(0, 1, 1), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        var host = appeng.api.parts.PartHelper.placePartHost(null, helper.getLevel(), helper.absolutePos(pos));
        host.addPart(appeng.core.definitions.AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT), null, null);
        var terminal = (com.gasai.ccapplied.parts.DraconicPatternEncodingTerminalPart) host.addPart(
                (appeng.api.parts.IPartItem<?>) CCItems.DRACONIC_PATTERN_TERMINAL.get(), Direction.NORTH, null);
        helper.runAfterDelay(60, () -> {
            com.brandon3055.draconicevolution.api.crafting.IFusionRecipe nativeRecipe = null;
            for (var entry : helper.getLevel().getRecipeManager().getRecipes()) {
                if (entry.value() instanceof com.brandon3055.draconicevolution.api.crafting.IFusionRecipe fusion
                        && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                                fusion.getResultItem(helper.getLevel().registryAccess()).getItem())
                                .getPath().equals("awakened_draconium_block")) {
                    nativeRecipe = fusion;
                    break;
                }
            }
            helper.assertTrue(nativeRecipe != null, "Native awakening recipe missing");
            var logic = terminal.getLogic();
            var catalyst = nativeRecipe.getCatalyst().getItems()[0].copyWithCount(4);
            for (int slot = 0; slot < nativeRecipe.fusionIngredients().size(); slot++) {
                logic.getEncodedInputInv().setStack(slot,
                        new GenericStack(AEItemKey.of(nativeRecipe.fusionIngredients().get(slot).get().getItems()[0]), 1));
            }
            logic.getEncodedInputInv().setStack(12, new GenericStack(AEItemKey.of(catalyst), 4));
            helper.assertTrue(logic.getEncodedInputInv().getStack(12).amount() == 4, "Terminal truncated catalyst");
            logic.getBlankPatternInv().setItemDirect(0, new ItemStack(CCItems.DRACONIC_BLANK_PATTERN.get()));
            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var menu = new com.gasai.ccapplied.menus.DraconicPatternEncodingTermMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.canEncode(), "Awakening cannot be previewed");
            menu.encode();
            helper.assertTrue(logic.getBlankPatternInv().getStackInSlot(0).isEmpty(), "Blank was not consumed");
            helper.assertTrue(menu.canEncode(), "Existing fusion pattern requires a blank");
            menu.encode();
            helper.assertTrue(logic.getEncodedPatternInv().getStackInSlot(0).getCount() == 1
                    && logic.getBlankPatternInv().getStackInSlot(0).isEmpty(), "Reencoding changed pattern counts");
            var encoded = logic.getEncodedPatternInv().getStackInSlot(0);
            var pattern = (DraconicFusionPattern) PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
            helper.assertTrue(pattern != null && pattern.getInputStacks()[12].getCount() == 4
                    && pattern.getOutputStack().getCount() == 4, "Encoded awakening has wrong amounts");
            var machinePos = new BlockPos(2, 1, 1);
            helper.setBlock(machinePos, CCBlocks.WYVERN_MOLECULAR_ASSEMBLER.get());
            var machine = (ExtremeMolecularAssemblerTileEntity) helper.getBlockEntity(machinePos);
            machine.getInternalInventory().setItemDirect(82, encoded.copy());
            var handler = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(machinePos), Direction.UP);
            helper.assertTrue(handler.insertItem(12, catalyst.copy(), false).isEmpty(),
                    "Assembler cannot insert four catalyst blocks");
            var grid = new TransientCraftingContainer(new AutoCraftingMenu(), 9, 9);
            pattern.fillCraftingGrid(inputs(pattern), grid::setItem);
            helper.assertTrue(pattern.assemble(grid, helper.getLevel()).getCount() == 4, "Awakening craft rejected");
            grid.setItem(12, catalyst.copyWithCount(3));
            helper.assertTrue(pattern.assemble(grid, helper.getLevel()).isEmpty(), "Three catalysts accepted");
            grid.setItem(12, catalyst.copyWithCount(5));
            var remainder = pattern.getRemainingItems(grid);
            helper.assertTrue(remainder.slots().get(12).getCount() == 1, "Craft did not consume exactly four catalysts");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void extremeReencodesWithoutBlankPattern(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(new BlockPos(0, 1, 1), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        var host = appeng.api.parts.PartHelper.placePartHost(null, helper.getLevel(), helper.absolutePos(pos));
        host.addPart(appeng.core.definitions.AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT), null, null);
        var terminal = (com.gasai.ccapplied.parts.ExtremePatternEncodingTerminalPart) host.addPart(
                (appeng.api.parts.IPartItem<?>) CCItems.EXTREME_PATTERN_TERMINAL.get(), Direction.NORTH, null);
        helper.runAfterDelay(60, () -> withIronRecipe(helper, () -> {
            var logic = terminal.getLogic();
            logic.getEncodedPatternInv().setItemDirect(0, RegressionTests.encodedIronPattern(helper));
            replace(helper, IRON_ID, ironRecipe(Items.IRON_INGOT, Items.DIAMOND));
            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var menu = new com.gasai.ccapplied.menus.ExtremePatternEncodingTermMenu(1, player.getInventory(), terminal);
            menu.getInputSlots()[0].set(new ItemStack(Items.IRON_INGOT));
            helper.assertTrue(menu.canEncode(), "Existing pattern requires a blank");
            menu.encode();
            var encoded = logic.getEncodedPatternInv().getStackInSlot(0);
            var pattern = (ExtremeCraftingPattern) PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
            helper.assertTrue(pattern != null && pattern.getOutputs().get(0).what().equals(AEItemKey.of(Items.DIAMOND)),
                    "Existing pattern was not rewritten");
            helper.assertTrue(encoded.getCount() == 1 && logic.getBlankPatternInv().getStackInSlot(0).isEmpty(),
                    "Reencoding changed pattern counts");
            logic.getEncodedPatternInv().setItemDirect(0, ItemStack.EMPTY);
            helper.assertTrue(!menu.canEncode(), "Encoding allowed without any pattern");
            helper.succeed();
        }));
    }

    private static KeyCounter[] inputs(DraconicFusionPattern pattern) {
        var table = new KeyCounter[pattern.getInputs().length];
        for (int i = 0; i < table.length; i++) {
            table[i] = new KeyCounter();
            table[i].add(pattern.getInputs()[i].getPossibleInputs()[0].what(), pattern.getInputs()[i].getMultiplier());
        }
        return table;
    }
}
