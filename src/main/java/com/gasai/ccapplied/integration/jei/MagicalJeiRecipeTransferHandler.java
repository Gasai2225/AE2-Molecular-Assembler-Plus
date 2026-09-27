package com.gasai.ccapplied.integration.jei;

import com.gasai.ccapplied.botania.MagicalRecipeTransfer;
import com.gasai.ccapplied.botania.MagicalTerminalMenu;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.Recipe;
import vazkii.botania.client.integration.jei.*;

/** Recipe IDs only cross the network; both sides validate against Botania's recipe manager. */
public final class MagicalJeiRecipeTransferHandler<R extends Recipe<?>>
        implements IRecipeTransferHandler<MagicalTerminalMenu, R> {
    private final RecipeType<R> type;
    private final IRecipeTransferHandlerHelper helper;

    private MagicalJeiRecipeTransferHandler(RecipeType<R> type, IRecipeTransferHandlerHelper helper) {
        this.type = type;
        this.helper = helper;
    }

    public static void register(IRecipeTransferRegistration registration) {
        register(registration, ManaPoolRecipeCategory.TYPE);
        register(registration, RunicAltarRecipeCategory.TYPE);
        register(registration, PetalApothecaryRecipeCategory.TYPE);
        register(registration, TerrestrialAgglomerationRecipeCategory.TYPE);
        register(registration, ElvenTradeRecipeCategory.TYPE);
        register(registration, BreweryRecipeCategory.TYPE);
        register(registration, PureDaisyRecipeCategory.TYPE);
    }

    private static <R extends Recipe<?>> void register(IRecipeTransferRegistration registration, RecipeType<R> type) {
        registration.addRecipeTransferHandler(new MagicalJeiRecipeTransferHandler<>(type, registration.getTransferHelper()), type);
    }

    @Override public Class<MagicalTerminalMenu> getContainerClass() { return MagicalTerminalMenu.class; }
    @Override public Optional<MenuType<MagicalTerminalMenu>> getMenuType() { return Optional.of(MagicalTerminalMenu.TYPE); }
    @Override public RecipeType<R> getRecipeType() { return type; }

    @Override public IRecipeTransferError transferRecipe(MagicalTerminalMenu menu, R recipe,
            IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        var id = recipe.getId();
        if (MagicalRecipeTransfer.prepare(player.level(), id) == null)
            return helper.createUserErrorWithTooltip(Component.translatable("gui.ccapplied.magical_transfer_unsupported"));
        if (doTransfer) menu.applyJeiRecipe(id.toString());
        return null;
    }
}
