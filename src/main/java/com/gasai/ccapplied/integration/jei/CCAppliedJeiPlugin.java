package com.gasai.ccapplied.integration.jei;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.integration.ae2.client.ExtremePatternEncodingTermScreen;
import com.gasai.ccapplied.integration.ae2.menu.ExtremePatternEncodingTermMenu;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class CCAppliedJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = new ResourceLocation(CCApplied.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // на первом этапе без кликабельных зон; не требуется для transfer
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        var helper = registration.getTransferHelper();
        registration.addRecipeTransferHandler(new ExtremeJeiRecipeTransferHandler(helper), RecipeTypes.CRAFTING);
        try {
            var basic = com.blakebr0.extendedcrafting.compat.jei.category.table.BasicTableCategory.RECIPE_TYPE;
            var advanced = com.blakebr0.extendedcrafting.compat.jei.category.table.AdvancedTableCategory.RECIPE_TYPE;
            var elite = com.blakebr0.extendedcrafting.compat.jei.category.table.EliteTableCategory.RECIPE_TYPE;
            var ultimate = com.blakebr0.extendedcrafting.compat.jei.category.table.UltimateTableCategory.RECIPE_TYPE;
            registration.addRecipeTransferHandler(new ECJeiRecipeTransferHandler(basic, helper), basic);
            registration.addRecipeTransferHandler(new ECJeiRecipeTransferHandler(advanced, helper), advanced);
            registration.addRecipeTransferHandler(new ECJeiRecipeTransferHandler(elite, helper), elite);
            registration.addRecipeTransferHandler(new ECJeiRecipeTransferHandler(ultimate, helper), ultimate);
        } catch (Throwable ignored) {}
    }
}


