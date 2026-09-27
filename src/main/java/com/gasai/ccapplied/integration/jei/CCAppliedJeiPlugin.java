package com.gasai.ccapplied.integration.jei;

import com.gasai.ccapplied.CCApplied;
import com.brandon3055.draconicevolution.integration.jei.DEJEIPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class CCAppliedJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(CCApplied.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        var helper = registration.getTransferHelper();
        registration.addRecipeTransferHandler(new ExtremeJeiRecipeTransferHandler(helper), RecipeTypes.CRAFTING);
        registerDraconicFusionTransfer(registration);
        if (com.gasai.ccapplied.core.registry.CCOptionalMods.isBotaniaLoaded())
            MagicalJeiRecipeTransferHandler.register(registration);

        registerReflectedTransfer(registration, helper,
                "com.blakebr0.extendedcrafting.compat.jei.category.table.BasicTableCategory");
        registerReflectedTransfer(registration, helper,
                "com.blakebr0.extendedcrafting.compat.jei.category.table.AdvancedTableCategory");
        registerReflectedTransfer(registration, helper,
                "com.blakebr0.extendedcrafting.compat.jei.category.table.EliteTableCategory");
        registerReflectedTransfer(registration, helper,
                "com.blakebr0.extendedcrafting.compat.jei.category.table.UltimateTableCategory");

        registerReflectedTransfer(registration, helper,
                "committee.nova.mods.avaritia.init.compat.jei.category.tables.ExtremeCraftingTableCategory");
        registerReflectedTransfer(registration, helper,
                "committee.nova.mods.avaritia.init.compat.jei.category.tables.NetherCraftingTableCategory");
        registerReflectedTransfer(registration, helper,
                "committee.nova.mods.avaritia.init.compat.jei.category.tables.EndCraftingTableCategory");
        registerReflectedTransfer(registration, helper,
                "committee.nova.mods.avaritia.init.compat.jei.category.tables.SculkCraftingTableCategory");

        registerReflectedTransfer(registration, helper,
                "net.byAqua3.avaritia.compat.jei.AvaritiaJEIPlugin",
                "EXTREME_CRAFTING");
    }

    private static void registerDraconicFusionTransfer(IRecipeTransferRegistration registration) {
        try {
            var type = DEJEIPlugin.getFusionRecipeType();
            registration.addRecipeTransferHandler(new DraconicJeiRecipeTransferHandler(type), type);
        } catch (Throwable ignored) {
        }
    }

    private static void registerReflectedTransfer(
            IRecipeTransferRegistration registration,
            mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper helper,
            String categoryClassName) {
        registerReflectedTransfer(registration, helper, categoryClassName, "RECIPE_TYPE");
    }

    private static void registerReflectedTransfer(
            IRecipeTransferRegistration registration,
            mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper helper,
            String categoryClassName,
            String fieldName) {
        try {
            Class<?> categoryClass = Class.forName(categoryClassName);
            Object recipeType = categoryClass.getField(fieldName).get(null);
            if (recipeType instanceof RecipeType<?> type) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                RecipeType<Object> typedRecipeType = (RecipeType) type;
                registration.addRecipeTransferHandler(
                        new ECJeiRecipeTransferHandler(typedRecipeType, helper),
                        typedRecipeType);
            }
        } catch (Throwable ignored) {
        }
    }
}
