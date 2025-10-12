package com.gasai.ccapplied.common.item;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.core.AppEng;
import appeng.items.misc.WrappedGenericStack;
import com.gasai.ccapplied.common.core.ExtremePatternCodec;
import com.gasai.ccapplied.common.core.ExtremePatternDetails;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public class ExtremePatternEncodedItem extends Item {

    private static final Logger LOG = LoggerFactory.getLogger("CCApplied/ExtremePatternItem");
    /** Клиентский кэш превью, как в AE2. */
    private static final Map<ItemStack, ItemStack> SIMPLE_CACHE = new WeakHashMap<>();

    public ExtremePatternEncodedItem(Properties props) {
        super(props.stacksTo(1));
    }

    /* ==== NBT ==== */

    public static boolean isEncoded(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(ExtremePatternCodec.KEY_OUTC);
    }

    public static void writePatternData(ItemStack stack, CompoundTag tag) {
        stack.setTag(tag.copy());
        SIMPLE_CACHE.remove(stack); // сброс кэша при изменении
        LOG.debug("[writePatternData] keys={}", tag.getAllKeys());
    }

    /* ==== Превью результата (как EncodedPatternItem#getOutput в AE2) ==== */

    /** Возвращает стек, который показывается при удержании Shift (в GUI). */
    @OnlyIn(Dist.CLIENT)
    public static ItemStack getOutput(ItemStack item) {
        var cached = SIMPLE_CACHE.get(item);
        if (cached != null) return cached;

        var level = AppEng.instance().getClientLevel();
        if (level == null || !isEncoded(item)) return ItemStack.EMPTY;

        IPatternDetails details = appeng.api.crafting.PatternDetailsHelper.decodePattern(item, level, false);
        ItemStack out = ItemStack.EMPTY;

        if (details instanceof ExtremePatternDetails epd && epd.getOutputs().length > 0) {
            var primary = epd.getOutputs()[0];
            if (primary.what() instanceof AEItemKey itemKey) {
                out = itemKey.toStack((int) primary.amount());
            } else {
                out = WrappedGenericStack.wrap(primary.what(), 0);
            }
        } else if (details != null) {
            var primary = details.getPrimaryOutput();
            if (primary != null) {
                if (primary.what() instanceof AEItemKey itemKey) {
                    out = itemKey.toStack();
                } else {
                    out = WrappedGenericStack.wrap(primary.what(), 0);
                }
            }
        }

        LOG.debug("[getOutput] -> {}", out.isEmpty() ? "EMPTY" : out.getHoverName().getString());
        SIMPLE_CACHE.put(item, out);
        return out;
    }

    /* ==== Клиентский renderer (BEWLR) ==== */
    // В этой сборке НЕ ставим @Override на initializeClient (сигнатуры могут отличаться)


    /* ==== Creative / Tooltip ==== */

    /** Как в AE2: закодированный паттерн не добавляется в креатив. */
    public void addToMainCreativeTab(CreativeModeTab.Output output) {
        // don't show encoded pattern in creative
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {
        if (!isEncoded(stack)) {
            lines.add(Component.translatable("tooltip.ccapplied.pattern.not_encoded")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        var preview = getOutput(stack);
        if (!preview.isEmpty()) {
            lines.add(Component.literal("Craft: ")
                    .append(preview.getHoverName().copy().withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
        lines.add(Component.translatable("tooltip.ccapplied.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
    }
}
