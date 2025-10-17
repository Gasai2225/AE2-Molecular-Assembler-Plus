package com.gasai.ccapplied.integration.jei;

import com.gasai.ccapplied.screens.ExtremePatternEncodingTermScreen;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class ExtremeJeiGuiHandler implements IGuiContainerHandler<ExtremePatternEncodingTermScreen> {
    private final List<RecipeType<?>> types;

    public ExtremeJeiGuiHandler(List<RecipeType<?>> types) {
        this.types = types;
    }

    @Override
    public @NotNull List<Rect2i> getGuiExtraAreas(ExtremePatternEncodingTermScreen screen) {
        return List.of();
    }

    @Override
    public @NotNull Collection<IGuiClickableArea> getGuiClickableAreas(ExtremePatternEncodingTermScreen screen, double guiMouseX, double guiMouseY) {
        return List.of();
    }
}


