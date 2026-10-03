package com.gasai.ccapplied.crafting;

import com.gasai.ccapplied.patterns.DraconicFusionPattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import java.util.ArrayList;
import java.util.List;

public final class DraconicFusionRecipeHelper {
    private static final ResourceLocation FUSION_RECIPE_TYPE =
            ResourceLocation.fromNamespaceAndPath("draconicevolution", "fusion_crafting");

    private DraconicFusionRecipeHelper() {
    }

    /** Keep AE2's promised output key stable; native assembly creates a fresh settings-provider UUID. */
    public static ItemStack withOutputIdentity(ItemStack result, ItemStack template) {
        result.getCapability(com.brandon3055.draconicevolution.api.capability.DECapabilities.MODULE_HOST_CAPABILITY)
                .ifPresent(host -> {
                    if (host instanceof com.brandon3055.draconicevolution.api.modules.lib.ModuleHostImpl modules) {
                        var reference = template.serializeNBT().getCompound("ForgeCaps").getCompound("Parent")
                                .getCompound("module_host");
                        var data = modules.serializeNBT();
                        data.putUUID("provider_id", reference.hasUUID("provider_id")
                                ? reference.getUUID("provider_id") : new java.util.UUID(0, 0));
                        modules.deserializeNBT(data);
                    }
                });
        return result;
    }

    public static @Nullable DraconicFusionRecipeMatch matchRecipe(ResourceLocation id, ItemStack[] inputs, Level level) {
        if (id == null || level == null || inputs.length != DraconicFusionPattern.TOTAL_INPUT_SLOTS) return null;
        var recipe = level.getRecipeManager().byKey(id).orElse(null);
        if (recipe == null || !FUSION_RECIPE_TYPE.equals(
                net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()))) return null;
        var outer = new ArrayList<ItemStack>();
        for (int i = 0; i < DraconicFusionPattern.OUTER_SLOTS; i++)
            if (!inputs[i].isEmpty()) outer.add(inputs[i]);
        return tryMatchFusionRecipe(recipe, outer, inputs[DraconicFusionPattern.OUTER_SLOTS], level);
    }

    public static @Nullable DraconicFusionRecipeMatch findRecipe(List<ItemStack> outerInputs, ItemStack catalyst, Level level) {
        if (level == null || catalyst.isEmpty() || outerInputs == null || outerInputs.isEmpty() || outerInputs.size() > 12) {
            return null;
        }
        if (!ModList.get().isLoaded("draconicevolution")) {
            return null;
        }

        try {
            for (var recipe : level.getRecipeManager().getRecipes()) {
                var typeId = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
                if (!FUSION_RECIPE_TYPE.equals(typeId)) {
                    continue;
                }
                var match = tryMatchFusionRecipe(recipe, outerInputs, catalyst, level);
                if (match != null) {
                    return match;
                }
            }
        } catch (Exception e) {
            com.gasai.ccapplied.CCApplied.LOG.warn("Error matching Draconic Evolution fusion recipe", e);
        }

        return null;
    }

    public static @Nullable IFusionRecipe currentRecipe(ResourceLocation id, Level level) {
        if (id == null || level == null || !ModList.get().isLoaded("draconicevolution")) return null;
        var entry = level.getRecipeManager().byKey(id).orElse(null);
        return entry instanceof IFusionRecipe recipe ? recipe : null;
    }

    public static DraconicFusionPattern.FusionTier tier(IFusionRecipe recipe) {
        return switch (recipe.getRecipeTier()) {
            case CHAOTIC -> DraconicFusionPattern.FusionTier.CHAOTIC;
            case DRACONIC -> DraconicFusionPattern.FusionTier.DRACONIC;
            default -> DraconicFusionPattern.FusionTier.WYVERN;
        };
    }

    private static @Nullable DraconicFusionRecipeMatch tryMatchFusionRecipe(
            Recipe<?> entry, List<ItemStack> outerInputs, ItemStack catalyst, Level level) {
        if (!(entry instanceof IFusionRecipe recipe)
                || !recipe.getCatalyst().test(catalyst)) return null;
        var ingredients = recipe.fusionIngredients();
        if (ingredients.size() != outerInputs.size()) return null;
        int[] assigned = new int[outerInputs.size()];
        java.util.Arrays.fill(assigned, -1);
        for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
            if (!assign(recipe, ingredient, outerInputs, assigned, new boolean[assigned.length])) return null;
        }
        var consumed = new ArrayList<Boolean>();
        for (int ingredient : assigned) consumed.add(ingredients.get(ingredient).consume());
        var result = recipe.assemble(new FusionCraftingInventory(outerInputs, catalyst), level.registryAccess());
        return result.isEmpty() ? null : new DraconicFusionRecipeMatch(result, catalyst.copy(), outerInputs,
                tier(recipe), recipe.getEnergyCost(), entry.getId(), List.copyOf(consumed));
    }

    // Reassign previous matches when ingredients overlap, rather than greedily rejecting valid inputs.
    private static boolean assign(IFusionRecipe recipe, int ingredient, List<ItemStack> stacks,
            int[] assigned, boolean[] visited) {
        for (int slot = 0; slot < stacks.size(); slot++) {
            if (visited[slot] || !recipe.fusionIngredients().get(ingredient).get().test(stacks.get(slot))) continue;
            visited[slot] = true;
            if (assigned[slot] == -1 || assign(recipe, assigned[slot], stacks, assigned, visited)) {
                assigned[slot] = ingredient;
                return true;
            }
        }
        return false;
    }
}
