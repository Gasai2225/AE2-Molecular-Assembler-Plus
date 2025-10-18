package com.gasai.ccapplied.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.gasai.ccapplied.patterns.ExtremeCraftingPattern;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.core.localization.GuiText;

/**
 * Item "Extreme Crafting Pattern".
 * Extends AE2 EncodedPatternItem for UX (Shift clearing, basic logic),
 * but overrides tooltip to show "Crafts" (like regular crafting template).
 */
public class ExtremeEncodedPatternItem extends EncodedPatternItem {

    public static final String NBT_ROOT = "ccapplied_extreme";
    private static final String NBT_SHAPED = "shaped";
    private static final String NBT_W = "w";
    private static final String NBT_H = "h";
    private static final String NBT_INPUTS = "inputs";
    private static final String NBT_OUTPUTS = "outputs";
    private static final String NBT_RECIPE_ID = "recipeId";

    public ExtremeEncodedPatternItem(Item.Properties props) {
        super(props);
    }

    @Override
    public void addToMainCreativeTab(CreativeModeTab.Output output) {
    }

    /* ===================== Encoding ===================== */

    public ItemStack encode(GenericStack[] inputs81, GenericStack primaryOutput, @Nullable ResourceLocation recipeId) {
        var out = new ItemStack(this);
        var tag = new CompoundTag();
        var root = new CompoundTag();

        root.putBoolean(NBT_SHAPED, true);
        root.putInt(NBT_W, 9);
        root.putInt(NBT_H, 9);

        var inList = new ListTag();
        for (int i = 0; i < ExtremeCraftingPattern.SLOTS; i++) {
            var gs = inputs81[i];
            if (gs == null) {
                inList.add(new CompoundTag());
                continue;
            }
            inList.add(writeGenericItem(gs));
        }
        root.put(NBT_INPUTS, inList);

        var outs = new ListTag();
        outs.add(writeGenericItem(primaryOutput));
        root.put(NBT_OUTPUTS, outs);

        if (recipeId != null) {
            root.putString(NBT_RECIPE_ID, recipeId.toString());
        }

        tag.put(NBT_ROOT, root);
        out.setTag(tag);
        return out;
    }

    private static CompoundTag writeGenericItem(GenericStack gs) {
        var t = new CompoundTag();
        var key = (AEItemKey) gs.what();
        t.putString("item", key.getId().toString());
        t.putLong("amount", gs.amount());
        try {
            var stack = key.toStack((int) Math.max(1, Math.min(gs.amount(), 64)));
            if (stack.hasTag()) {
                t.put("nbt", stack.getTag().copy());
            }
        } catch (Exception ignored) {
        }
        return t;
    }

    private static GenericStack readGenericItem(CompoundTag t) {
        if (t.isEmpty() || !t.contains("item", Tag.TAG_STRING)) return null;

        String itemId = t.getString("item");
        int firstColon = itemId.indexOf(":");
        if (firstColon > 0) {
            int secondColon = itemId.indexOf(":", firstColon + 1);
            if (secondColon > firstColon) {
                String ns1 = itemId.substring(0, firstColon);
                String ns2 = itemId.substring(firstColon + 1, secondColon);
                if (ns1.equals(ns2)) {
                    itemId = ns1 + ":" + itemId.substring(secondColon + 1);
                }
            }
        }

        ResourceLocation id;
        try {
            id = ResourceLocation.parse(itemId);
        } catch (Exception e) {
            return null;
        }

        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);

        var amount = t.contains("amount", Tag.TAG_LONG) ? t.getLong("amount") : 1L;
        var stack = new ItemStack(item, (int) Math.max(1, Math.min(amount, 64)));
        if (t.contains("nbt", Tag.TAG_COMPOUND)) {
            try {
                stack.setTag(t.getCompound("nbt").copy());
            } catch (Exception ignored) {}
        }
        var key = AEItemKey.of(stack);

        return new GenericStack(key, amount);
    }

    /* ===================== Decoding ===================== */

    @Override
    public @Nullable IPatternDetails decode(ItemStack stack, Level level, boolean tryRecovery) {
        if (!stack.hasTag()) return null;
        var tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_ROOT, Tag.TAG_COMPOUND)) return null;

        var root = tag.getCompound(NBT_ROOT);

        var shaped = root.getBoolean(NBT_SHAPED);
        var w = root.getInt(NBT_W);
        var h = root.getInt(NBT_H);

        var in = new GenericStack[ExtremeCraftingPattern.SLOTS];
        var inList = root.getList(NBT_INPUTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(inList.size(), in.length); i++) {
            var gs = readGenericItem(inList.getCompound(i));
            in[i] = gs;
        }

        var outputs = new ArrayList<GenericStack>();
        var outList = root.getList(NBT_OUTPUTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < outList.size(); i++) {
            var gs = readGenericItem(outList.getCompound(i));
            if (gs != null) outputs.add(gs);
        }
        if (outputs.isEmpty()) return null;

        ResourceLocation rid = null;
        if (root.contains(NBT_RECIPE_ID, Tag.TAG_STRING)) {
            rid = ResourceLocation.parse(root.getString(NBT_RECIPE_ID));
        }

        ItemStack[] itemInputs = new ItemStack[ExtremeCraftingPattern.SLOTS];
        for (int i = 0; i < ExtremeCraftingPattern.SLOTS && i < in.length; i++) {
            if (in[i] != null && in[i].what() instanceof AEItemKey itemKey) {
                itemInputs[i] = itemKey.toStack((int) in[i].amount());
            } else {
                itemInputs[i] = ItemStack.EMPTY;
            }
        }
        
        ItemStack itemOutput = ItemStack.EMPTY;
        if (outputs.size() > 0 && outputs.get(0) != null && outputs.get(0).what() instanceof AEItemKey outputKey) {
            itemOutput = outputKey.toStack((int) outputs.get(0).amount());
        }

        return new ExtremeCraftingPattern(in, outputs.toArray(GenericStack[]::new), itemInputs, itemOutput, shaped, w, h, rid);
    }

    @Override
    public @Nullable IPatternDetails decode(AEItemKey what, Level level) {
        return decode(what.toStack(), level, false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                var blank = new ItemStack(com.gasai.ccapplied.core.registry.CCItems.EXTREME_BLANK_PATTERN.get());
                player.setItemInHand(hand, blank);
            }
            return InteractionResultHolder.success(stack);
        }

        return super.use(level, player, hand);
    }

    /* ===================== Tooltip ===================== */

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag advancedTooltips) {
        if (!stack.hasTag()) return;

        IPatternDetails details = decode(stack, level, false);
        if (details == null) {
            stack.setHoverName(GuiText.InvalidPattern.text().copy().withStyle(ChatFormatting.RED));
            return;
        }
        if (stack.hasCustomHoverName()) stack.resetHoverName();

        var label = GuiText.Crafts.text().copy().append(": ").withStyle(ChatFormatting.GRAY);
        var and = Component.literal(" ").copy().append(GuiText.And.text()).append(" ").withStyle(ChatFormatting.GRAY);
        var with = GuiText.With.text().copy().append(": ").withStyle(ChatFormatting.GRAY);

        var out = details.getOutputs();
        boolean first = true;
        for (var anOut : out) {
            if (anOut == null) continue;
            lines.add(Component.empty().append(first ? label : and).append(getStackComponent(anOut)));
            first = false;
        }

        java.util.Map<appeng.api.stacks.AEKey, Long> totals = new java.util.LinkedHashMap<>();
        for (var input : details.getInputs()) {
            var primaryInputTemplate = input.getPossibleInputs()[0];
            var key = primaryInputTemplate.what();
            long add = primaryInputTemplate.amount() * input.getMultiplier();
            totals.merge(key, add, Long::sum);
        }

        first = true;
        for (var entry : totals.entrySet()) {
            var key = entry.getKey();
            long amt = entry.getValue();
            var gs = new GenericStack(key, amt);
            lines.add(Component.empty().append(first ? with : and).append(getStackComponent(gs)));
            first = false;
        }

        var substitutionLabel = GuiText.Substitute.text().copy().append(" ");
        lines.add(substitutionLabel.copy().append(GuiText.No.text()));
    }

    @org.jetbrains.annotations.NotNull
    protected static Component getStackComponent(GenericStack stack) {
        var amountInfo = stack.what().formatAmount(stack.amount(), appeng.api.stacks.AmountFormat.FULL);
        var displayName = stack.what().getDisplayName();
        return net.minecraft.network.chat.Component.literal(amountInfo + " x ").append(displayName);
    }
}
