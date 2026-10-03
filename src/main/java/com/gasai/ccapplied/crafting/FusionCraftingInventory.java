package com.gasai.ccapplied.crafting;

import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.api.crafting.IFusionInjector;
import com.brandon3055.draconicevolution.api.crafting.IFusionInventory;
import java.util.List;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** A private copy: native fusion assembly may transfer modules from the catalyst. */
final class FusionCraftingInventory extends SimpleContainer implements IFusionInventory {
    FusionCraftingInventory(List<ItemStack> outer, ItemStack catalyst) {
        super(14);
        for (int i = 0; i < outer.size(); i++) setItem(i, outer.get(i).copy());
        setCatalystStack(catalyst.copy());
    }

    @Override public ItemStack getCatalystStack() { return getItem(12); }
    @Override public ItemStack getOutputStack() { return getItem(13); }
    @Override public void setCatalystStack(ItemStack stack) { setItem(12, stack); }
    @Override public void setOutputStack(ItemStack stack) { setItem(13, stack); }
    @Override public List<IFusionInjector> getInjectors() { return List.of(); }
    @Override public TechLevel getMinimumTier() { return TechLevel.CHAOTIC; }
    @Override public int size() { return getContainerSize(); }

}
