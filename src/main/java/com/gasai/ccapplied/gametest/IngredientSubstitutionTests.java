package com.gasai.ccapplied.gametest;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.botania.*;
import com.gasai.ccapplied.core.registry.CCItems;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
import com.gasai.ccapplied.crafting.DraconicFusionRecipeHelper;
import com.gasai.ccapplied.patterns.DraconicFusionPattern;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CCApplied.MODID)
@PrefixGameTestTemplate(false)
public final class IngredientSubstitutionTests {
    @GameTest(template = "empty")
    public static void apothecaryMixesPetalsAndMushrooms(GameTestHelper helper) {
        if (!CCOptionalMods.isBotaniaLoaded()) { helper.succeed(); return; }
        var level = helper.getLevel();
        var petal = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("botania", "white_mystical_petal")));
        var mushroom = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("botania", "white_shimmering_mushroom")));
        var ingredients = List.of(petal, petal.copy(), petal.copy(), petal.copy(), new ItemStack(Items.WHEAT_SEEDS));
        var result = MagicalRecipeResolver.find(level, MagicalStation.APOTHECARY, PoolCatalyst.NONE, ingredients);
        helper.assertTrue(result != null && !mushroom.isEmpty(), "Missing native pure daisy recipe or mushroom");
        var encoded = MagicalPatternItem.encode(level, MagicalStation.APOTHECARY, PoolCatalyst.NONE,
                result.recipeId(), ingredients, true);
        var flexible = MagicalPatternItem.readPattern(encoded, level);
        var exact = MagicalPatternItem.readPattern(MagicalPatternItem.encode(level, MagicalStation.APOTHECARY,
                PoolCatalyst.NONE, result.recipeId(), ingredients, false), level);
        helper.assertTrue(flexible.hasSlotInputs() && flexible.getInputs().length == ingredients.size(),
                "Repeated petals were condensed and cannot mix alternatives");
        helper.assertTrue(!exact.isItemValid(1, AEItemKey.of(mushroom), level), "Disabled substitutions accepted a mushroom");
        var table = new KeyCounter[ingredients.size()];
        for (int i = 0; i < table.length; i++) {
            table[i] = new KeyCounter();
            table[i].add(AEItemKey.of(i == 1 || i == 3 ? mushroom : ingredients.get(i)), 1);
        }
        var actual = BotanicalAssemblerBlockEntity.validateInputs(flexible, table, level);
        helper.assertTrue(actual != null && actual.get(0).is(petal.getItem()) && actual.get(1).is(mushroom.getItem()),
                "Mixed petals and mushrooms were rejected or replaced");
        helper.assertTrue(MagicalRecipeResolver.sameResult(result, MagicalRecipeResolver.resolve(level,
                MagicalStation.APOTHECARY, PoolCatalyst.NONE, result.recipeId(), actual)), "Mixed recipe changed its output");
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BotanicalAssemblers.ENTRIES.get(MagicalStation.APOTHECARY).block().get());
        var machine = (BotanicalAssemblerBlockEntity) helper.getBlockEntity(pos);
        machine.templateInventory().setItemDirect(0, encoded);
        helper.assertTrue(machine.isValidManualInput(1, mushroom), "Manual slot rejected a valid mushroom");
        helper.assertTrue(!machine.isValidManualInput(4, mushroom), "Reagent slot accepted a mushroom");
        table[4].clear();
        helper.assertTrue(BotanicalAssemblerBlockEntity.validateInputs(flexible, table, level) == null,
                "Missing reagent accepted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fusionAcceptsInitializedTools(GameTestHelper helper) {
        if (!CCOptionalMods.isDraconicEvolutionLoaded()) { helper.succeed(); return; }
        var level = helper.getLevel();
        int tested = 0;
        for (var holder : level.getRecipeManager().getRecipes()) {
            var recipe = holder.value();
            if (!(recipe instanceof com.brandon3055.draconicevolution.api.crafting.IFusionRecipe fusion)) continue;
            var catalysts = fusion.getCatalyst().getItems();
            if (catalysts.length == 0 || catalysts[0].getMaxStackSize() != 1) continue;
            var itemId = BuiltInRegistries.ITEM.getKey(catalysts[0].getItem());
            if (!itemId.getNamespace().equals("draconicevolution")
                    || !(itemId.getPath().startsWith("wyvern_") || itemId.getPath().startsWith("draconic_"))) continue;
            var outer = recipe.getIngredients();
            if (outer.isEmpty() || outer.size() > DraconicFusionPattern.OUTER_SLOTS) continue;
            var stacks = new ItemStack[DraconicFusionPattern.TOTAL_INPUT_SLOTS];
            Arrays.fill(stacks, ItemStack.EMPTY);
            for (int i = 0; i < outer.size(); i++) stacks[i] = outer.get(i).getItems()[0].copyWithCount(1);
            stacks[12] = catalysts[0].copyWithCount(1);
            var id = holder.id();
            var match = DraconicFusionRecipeHelper.matchRecipe(id, stacks, level);
            helper.assertTrue(match != null, "Native fusion recipe not matched: " + id);
            var sparse = new GenericStack[stacks.length];
            for (int i = 0; i < stacks.length; i++)
                if (!stacks[i].isEmpty()) sparse[i] = new GenericStack(AEItemKey.of(stacks[i]), stacks[i].getCount());
            var encoder = (com.gasai.ccapplied.items.DraconicEncodedPatternItem) CCItems.DRACONIC_FUSION_PATTERN.get();
            var encoded = encoder.encode(sparse, new GenericStack(AEItemKey.of(match.result()), match.result().getCount()),
                    match.tier(), match.totalEnergy(), id, level.registryAccess());
            var pattern = (DraconicFusionPattern) appeng.api.crafting.PatternDetailsHelper.decodePattern(encoded, level);
            var restored = (DraconicFusionPattern) appeng.api.crafting.PatternDetailsHelper.decodePattern(encoded.copy(), level);
            helper.assertTrue(pattern != null && restored != null, "Fusion pattern did not decode");
            helper.assertTrue(AEItemKey.of(pattern.getOutputStack()).equals(AEItemKey.of(restored.getOutputStack())),
                    "Decoding changed the promised output key: " + id);
            var initialized = stacks[12].copy();
            initialized.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Initialized tool"));
            var key = AEItemKey.of(initialized);
            helper.assertTrue(!key.equals(AEItemKey.of(stacks[12])), "Test did not change tool data");
            var trial = stacks.clone();
            trial[12] = initialized;
            var changed = DraconicFusionRecipeHelper.matchRecipe(id, trial, level);
            helper.assertTrue(pattern.isItemValid(12, key, level), "Tool variant rejected: " + id
                    + " expected=" + pattern.getOutputStack().save(level.registryAccess())
                    + " actual=" + (changed == null ? "no recipe" : changed.result().save(level.registryAccess())));
            helper.assertTrue(!pattern.isItemValid(12, AEItemKey.of(Items.IRON_PICKAXE), level), "Wrong tool accepted: " + id);
            var table = new KeyCounter[pattern.getInputs().length];
            for (int i = 0; i < table.length; i++) {
                table[i] = new KeyCounter();
                var input = pattern.getInputs()[i];
                var supplied = i == table.length - 1 ? key : input.getPossibleInputs()[0].what();
                helper.assertTrue(input.isValid(supplied, level), "CPU rejected tool variant: " + id);
                table[i].add(supplied, input.getMultiplier());
            }
            var grid = new SimpleContainer(13);
            pattern.fillCraftingGrid(table, grid::setItem);
            helper.assertTrue(key.equals(AEItemKey.of(grid.getItem(12))), "Grid discarded the actual tool data");
            helper.assertTrue(!pattern.assemble(grid, level).isEmpty(), "Initialized tool cannot craft: " + id);
            for (var counter : table) {
                counter.removeZeros();
                helper.assertTrue(counter.isEmpty(), "Fusion inputs were not consumed exactly once");
            }
            tested++;
        }
        helper.assertTrue(tested >= 5, "Too few native tool upgrades tested: " + tested);
        helper.succeed();
    }
}

