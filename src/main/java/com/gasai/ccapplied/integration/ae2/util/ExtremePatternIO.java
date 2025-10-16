// ./src/main/java/com/gasai/ccapplied/integration/ae2/util/ExtremePatternIO.java
package com.gasai.ccapplied.integration.ae2.util;

import net.minecraft.world.item.ItemStack;
import appeng.api.stacks.GenericStack;
import com.gasai.ccapplied.core.registry.CCItems;
import com.gasai.ccapplied.integration.ae2.pattern.ExtremeEncodedPatternItem;

public final class ExtremePatternIO {
    private ExtremePatternIO() {}

    public static ItemStack encode9x9(GenericStack[] inputs81, GenericStack primaryOutput) {
        ExtremeEncodedPatternItem item =
                (ExtremeEncodedPatternItem) CCItems.EXTREME_CRAFTING_PATTERN.get();
        // recipeId можно передавать null (или свой, если выбираете по GUI)
        return item.encode(inputs81, primaryOutput, /*recipeId*/ null);
    }
}
