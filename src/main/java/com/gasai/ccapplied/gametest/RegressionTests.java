package com.gasai.ccapplied.gametest;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.core.registry.CCBlocks;
import com.gasai.ccapplied.core.registry.CCItems;
import com.gasai.ccapplied.core.registry.CCMenuTypes;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
import com.gasai.ccapplied.items.ExtremeEncodedPatternItem;
import com.gasai.ccapplied.patterns.ExtremeCraftingPattern;
import com.gasai.ccapplied.tiles.ExtremeMolecularAssemblerTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Regression tests run only by the development GameTest server. */
@GameTestHolder(CCApplied.MODID)
@PrefixGameTestTemplate(false)
public final class RegressionTests {
    private RegressionTests() {
    }

    @GameTest(template = "empty")
    public static void menuRegistration(GameTestHelper helper) {
        helper.assertTrue(CCApplied.makeId("extreme_patternterm").equals(
                BuiltInRegistries.MENU.getKey(CCMenuTypes.EXTREME_PATTERN_TERM.get())), "Terminal menu has the wrong registry id");
        helper.assertTrue(CCApplied.makeId("extreme_molecular_assembler").equals(
                BuiltInRegistries.MENU.getKey(CCMenuTypes.EXTREME_MOLECULAR_ASSEMBLER.get())), "Assembler menu has the wrong registry id");
        if (CCOptionalMods.isDraconicEvolutionLoaded()) {
            helper.assertTrue(CCApplied.makeId("draconic_patternterm").equals(
                    BuiltInRegistries.MENU.getKey(CCMenuTypes.DRACONIC_PATTERN_TERM.get())), "Fusion menu has the wrong registry id");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void craftingRecipesLoad(GameTestHelper helper) {
        for (String id : new String[] {
                "blocks/extreme_molecular_assembler", "parts/extreme_pattern_terminal", "patterns/extreme_blank_pattern"
        }) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(CCApplied.makeId(id)).isPresent(),
                    "Missing recipe: " + id);
        }
        if (CCOptionalMods.isDraconicEvolutionLoaded()) {
            for (String id : new String[] {
                    "blocks/wyvern_molecular_assembler", "blocks/draconic_molecular_assembler",
                    "blocks/chaotic_molecular_assembler", "parts/draconic_pattern_terminal", "patterns/draconic_blank_pattern"
            }) {
                helper.assertTrue(helper.getLevel().getRecipeManager().byKey(CCApplied.makeId(id)).isPresent(),
                        "Missing optional recipe: " + id);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void encodedPatternSurvivesSave(GameTestHelper helper) {
        ItemStack encoded = encodedIronPattern(helper);
        var pattern = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(pattern != null, "Encoded pattern cannot be decoded");
        helper.assertTrue(AEItemKey.of(encoded).equals(pattern.getDefinition()), "Pattern definition lost its encoded data");
        var restored = PatternDetailsHelper.decodePattern(pattern.getDefinition().toStack(), helper.getLevel());
        helper.assertTrue(pattern.equals(restored), "Saved pattern does not decode to the same pattern");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectedPushKeepsInputs(GameTestHelper helper) {
        RecipeReloadTests.withIronRecipe(helper, () -> {
            var pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, CCBlocks.EXTREME_MOLECULAR_ASSEMBLER.get());
            var assembler = (ExtremeMolecularAssemblerTileEntity) helper.getBlockEntity(pos);
            var pattern = PatternDetailsHelper.decodePattern(encodedIronPattern(helper), helper.getLevel());
            var key = AEItemKey.of(Items.IRON_INGOT);
            var wrong = new KeyCounter();
            wrong.add(AEItemKey.of(Items.GOLD_INGOT), 1);
            helper.assertTrue(!assembler.pushPattern(pattern, new KeyCounter[] {wrong}, Direction.NORTH),
                    "Wrong ingredients accepted");
            helper.assertTrue(wrong.get(AEItemKey.of(Items.GOLD_INGOT)) == 1, "Rejected input was lost");
            helper.assertTrue(assembler.getInternalInventory().isEmpty(), "Rejected push changed the inventory");

            var excess = new KeyCounter();
            excess.add(key, 2);
            helper.assertTrue(!assembler.pushPattern(pattern, new KeyCounter[] {excess}, Direction.NORTH),
                    "Excess ingredients accepted");
            helper.assertTrue(excess.get(key) == 2, "Excess input was discarded");

            var valid = new KeyCounter();
            valid.add(AEItemKey.of(Items.GOLD_INGOT), 0);
            valid.add(key, 1);
            helper.assertTrue(assembler.pushPattern(pattern, new KeyCounter[] {valid}, Direction.NORTH),
                    "Valid ingredients rejected");
            helper.assertTrue(valid.isEmpty(), "Accepted input was not consumed");
            helper.assertTrue(assembler.getInternalInventory().getStackInSlot(0).is(Items.IRON_INGOT),
                    "Accepted input did not reach the grid");
            var saved = assembler.saveWithFullMetadata(helper.getLevel().registryAccess());
            helper.assertTrue(saved.contains("myPlan"), "In-flight crafting plan was not saved");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void itemTransport(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CCBlocks.EXTREME_MOLECULAR_ASSEMBLER.get());
        var assembler = (ExtremeMolecularAssemblerTileEntity) helper.getBlockEntity(pos);
        var handler = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(handler != null, "Assembler inventory is unavailable to pipes");
        var rejected = handler.insertItem(0, new ItemStack(Items.IRON_INGOT), false);
        helper.assertTrue(rejected.getCount() == 1, "Unconfigured assembler accepted ingredients");
        assembler.getInternalInventory().setItemDirect(81, new ItemStack(Items.GOLD_INGOT, 2));
        var extracted = handler.extractItem(81, 1, false);
        helper.assertTrue(extracted.is(Items.GOLD_INGOT) && extracted.getCount() == 1,
                "Pipe cannot extract crafting output");
        helper.assertTrue(assembler.getInternalInventory().getStackInSlot(81).getCount() == 1,
                "Extraction changed the wrong amount");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void assemblerDrops(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        for (String tier : new String[] {"extreme", "wyvern", "draconic", "chaotic"}) {
            if (!tier.equals("extreme") && !CCOptionalMods.isDraconicEvolutionLoaded()) {
                continue;
            }
            var block = BuiltInRegistries.BLOCK.get(CCApplied.makeId(tier + "_molecular_assembler"));
            helper.setBlock(pos, block);
            var state = block.defaultBlockState();
            var tool = new ItemStack(Items.DIAMOND_PICKAXE);
            helper.assertTrue(tool.isCorrectToolForDrops(state), "Pickaxe cannot harvest " + tier);
            var drops = net.minecraft.world.level.block.Block.getDrops(state, helper.getLevel(),
                    helper.absolutePos(pos), helper.getBlockEntity(pos), null, tool);
            helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(block.asItem()) && stack.getCount() == 1),
                    "Assembler does not drop itself: " + tier);
        }
        helper.succeed();
    }

    static ItemStack encodedIronPattern(GameTestHelper helper) {
        GenericStack[] inputs = new GenericStack[ExtremeCraftingPattern.SLOTS];
        inputs[0] = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1);
        return ((ExtremeEncodedPatternItem) CCItems.EXTREME_CRAFTING_PATTERN.get()).encode(
                inputs, new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1), RecipeReloadTests.IRON_ID, helper.getLevel().registryAccess());
    }
}
