package com.gasai.ccapplied.crafting;

import java.util.Set;

import appeng.menu.AutoCraftingMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * Helper for 9x9 crafting recipes from optional crafting-table mods.
 */
public class ExtendedCraftingRecipeHelper {

    private static final Set<ResourceLocation> GENERIC_EXTREME_RECIPE_TYPES = Set.of(
            ResourceLocation.fromNamespaceAndPath("avaritia", "shaped_extreme_craft"),
            ResourceLocation.fromNamespaceAndPath("avaritia", "shapeless_extreme_craft"),
            ResourceLocation.fromNamespaceAndPath("avaritia", "extreme_shaped"),
            ResourceLocation.fromNamespaceAndPath("avaritia", "extreme_shapeless"),
            ResourceLocation.fromNamespaceAndPath("avaritia", "extreme_craft"),
            ResourceLocation.fromNamespaceAndPath("avaritia", "extreme_crafting"),
            ResourceLocation.fromNamespaceAndPath("avaritia", "nether_craft"),
            ResourceLocation.fromNamespaceAndPath("avaritia", "end_craft"),
            ResourceLocation.fromNamespaceAndPath("avaritia", "sculk_craft"),
            ResourceLocation.fromNamespaceAndPath("avaritianeo", "shaped_extreme_craft"),
            ResourceLocation.fromNamespaceAndPath("avaritianeo", "shapeless_extreme_craft"),
            ResourceLocation.fromNamespaceAndPath("avaritianeo", "extreme_shaped"),
            ResourceLocation.fromNamespaceAndPath("avaritianeo", "extreme_shapeless"),
            ResourceLocation.fromNamespaceAndPath("avaritianeo", "extreme_crafting")
    );

    private ExtendedCraftingRecipeHelper() {
    }

    public static @Nullable ExtremeRecipeMatch findAnyRecipe(ItemStack[] craftingGrid, Level level) {
        return findRecipe(craftingGrid, level, null);
    }

    public static @Nullable ExtremeRecipeMatch findRecipe(ItemStack[] craftingGrid, Level level,
            @Nullable ResourceLocation recipeId) {
        if (level == null || craftingGrid == null || craftingGrid.length != 81 || isEmptyGrid(craftingGrid)) {
            return null;
        }

        if (ModList.get().isLoaded("extendedcrafting")) {
            ExtremeRecipeMatch extendedCraftingRecipe = ExtendedCraftingCompat.findRecipe(craftingGrid, level, recipeId);
            if (extendedCraftingRecipe != null) {
                return extendedCraftingRecipe;
            }
        }

        return findGenericExtremeRecipe(craftingGrid, level, recipeId);
    }

    public static @Nullable ItemStack getRecipePreview(ItemStack[] craftingGrid, Level level) {
        ExtremeRecipeMatch recipe = findAnyRecipe(craftingGrid, level);
        return recipe != null ? recipe.result() : null;
    }

    public static boolean isExtendedCraftingRecipe(CraftingRecipe recipe) {
        return false;
    }

    private static @Nullable ExtremeRecipeMatch findGenericExtremeRecipe(ItemStack[] craftingGrid, Level level, @Nullable ResourceLocation recipeId) {
        var container = toCraftingContainer(craftingGrid);

        try {
            for (var recipe : level.getRecipeManager().getRecipes()) {
                if (recipeId != null && !recipeId.equals(recipe.getId())) continue;
                ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
                if (!isSupportedGenericExtremeRecipeType(typeId)) {
                    continue;
                }

                if (matchesRecipe(recipe, container, level)) {
                    ItemStack result = getRecipeResult(recipe, container, level);
                    if (!result.isEmpty()) {
                        return new ExtremeRecipeMatch(result, recipe.getId(), typeId.getNamespace(),
                                mapRemainders(getRemainingItems(recipe, container),
                                        9, 0, 0));
                    }
                }
            }
        } catch (Exception e) {
            com.gasai.ccapplied.CCApplied.LOG.warn("Error matching generic extreme recipe", e);
        }

        return null;
    }

    private static boolean isSupportedGenericExtremeRecipeType(ResourceLocation typeId) {
        if (typeId == null) {
            return false;
        }

        if (GENERIC_EXTREME_RECIPE_TYPES.contains(typeId)) {
            return true;
        }

        String namespace = typeId.getNamespace();
        String path = typeId.getPath();
        return (namespace.equals("avaritia") || namespace.equals("avaritianeo"))
                && path.contains("craft")
                && !path.contains("smithing");
    }

    private static boolean isEmptyGrid(ItemStack[] craftingGrid) {
        for (ItemStack stack : craftingGrid) {
            if (stack != null && !stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static TransientCraftingContainer toCraftingContainer(ItemStack[] craftingGrid) {
        var container = new TransientCraftingContainer(new AutoCraftingMenu(), 9, 9);
        for (int i = 0; i < Math.min(craftingGrid.length, container.getContainerSize()); i++) {
            ItemStack stack = craftingGrid[i];
            container.setItem(i, stack == null ? ItemStack.EMPTY : stack.copy());
        }
        return container;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static boolean matchesRecipe(Recipe recipe, TransientCraftingContainer container, Level level) {
        try {
            return recipe.matches(container, level);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static ItemStack getRecipeResult(Recipe recipe, TransientCraftingContainer container, Level level) {
        try {
            ItemStack assembled = recipe.assemble(container, level.registryAccess());
            if (!assembled.isEmpty()) {
                return assembled;
            }
        } catch (Exception ignored) {
        }

        return ItemStack.EMPTY;
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static java.util.List<ItemStack> getRemainingItems(Recipe recipe,
            TransientCraftingContainer input) {
        return recipe.getRemainingItems(input);
    }

    static java.util.List<ItemStack> mapRemainders(java.util.List<ItemStack> nativeItems,
            int width, int left, int top) {
        var mapped = net.minecraft.core.NonNullList.withSize(81, ItemStack.EMPTY);
        for (int slot = 0; slot < nativeItems.size(); slot++) {
            var stack = nativeItems.get(slot);
            if (!stack.isEmpty()) mapped.set(left + slot % width + (top + slot / width) * 9, stack.copy());
        }
        return mapped;
    }
}
