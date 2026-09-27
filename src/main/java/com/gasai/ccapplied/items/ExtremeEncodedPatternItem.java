package com.gasai.ccapplied.items;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.gasai.ccapplied.core.registry.CCItems;
import com.gasai.ccapplied.patterns.ExtremeCraftingPattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.core.localization.GuiText;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class ExtremeEncodedPatternItem extends EncodedPatternItem<IPatternDetails> {

    public static final String NBT_ROOT = "ccapplied_extreme";
    private static final String NBT_SHAPED = "shaped";
    private static final String NBT_W = "w";
    private static final String NBT_H = "h";
    private static final String NBT_INPUTS = "inputs";
    private static final String NBT_OUTPUTS = "outputs";
    private static final String NBT_RECIPE_ID = "recipeId";

    public ExtremeEncodedPatternItem(Item.Properties props) {
        super(props, ExtremeEncodedPatternItem::decodeFromKey,
                (stack, level, exception, flag) -> new appeng.api.crafting.PatternDetailsTooltip(Component.empty()));
    }

    public void addToMainCreativeTab(CreativeModeTab.Output output) {
    }

    public ItemStack encode(GenericStack[] inputs81, GenericStack primaryOutput, @Nullable ResourceLocation recipeId,
            HolderLookup.Provider registries) {
        var out = new ItemStack(this);
        var tag = new CompoundTag();
        var root = new CompoundTag();

        root.putBoolean(NBT_SHAPED, true);
        root.putInt(NBT_W, 9);
        root.putInt(NBT_H, 9);

        var inList = new ListTag();
        for (int i = 0; i < ExtremeCraftingPattern.SLOTS; i++) {
            var gs = inputs81[i];
            inList.add(writeGenericItem(gs, registries));
        }
        root.put(NBT_INPUTS, inList);

        var outs = new ListTag();
        outs.add(writeGenericItem(primaryOutput, registries));
        root.put(NBT_OUTPUTS, outs);

        if (recipeId != null) {
            root.putString(NBT_RECIPE_ID, recipeId.toString());
        }

        tag.put(NBT_ROOT, root);
        CustomData.set(DataComponents.CUSTOM_DATA, out, tag);
        return out;
    }

    private static @Nullable IPatternDetails decodeFromKey(AEItemKey what, Level level) {
        return decodeStack(what.toStack(), level);
    }

    public static @Nullable IPatternDetails decodeStack(ItemStack stack, Level level) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(NBT_ROOT, Tag.TAG_COMPOUND)) {
            return null;
        }

        var root = tag.getCompound(NBT_ROOT);
        var shaped = root.getBoolean(NBT_SHAPED);
        var w = root.getInt(NBT_W);
        var h = root.getInt(NBT_H);

        var in = new GenericStack[ExtremeCraftingPattern.SLOTS];
        var inList = root.getList(NBT_INPUTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(inList.size(), in.length); i++) {
            in[i] = readGenericItem(inList.getCompound(i), level);
        }

        var outputs = new ArrayList<GenericStack>();
        var outList = root.getList(NBT_OUTPUTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < outList.size(); i++) {
            var gs = readGenericItem(outList.getCompound(i), level);
            if (gs != null) {
                outputs.add(gs);
            }
        }
        if (outputs.isEmpty()) {
            return null;
        }

        ResourceLocation rid = null;
        if (root.contains(NBT_RECIPE_ID, Tag.TAG_STRING)) {
            rid = ResourceLocation.parse(root.getString(NBT_RECIPE_ID));
        }

        ItemStack[] itemInputs = new ItemStack[ExtremeCraftingPattern.SLOTS];
        for (int i = 0; i < ExtremeCraftingPattern.SLOTS; i++) {
            if (in[i] != null && in[i].what() instanceof AEItemKey itemKey) {
                itemInputs[i] = itemKey.toStack((int) in[i].amount());
            } else {
                itemInputs[i] = ItemStack.EMPTY;
            }
        }

        ItemStack itemOutput = ItemStack.EMPTY;
        if (outputs.get(0).what() instanceof AEItemKey outputKey) {
            itemOutput = outputKey.toStack((int) outputs.get(0).amount());
        }

        return new ExtremeCraftingPattern(AEItemKey.of(stack), in, outputs.toArray(GenericStack[]::new), itemInputs, itemOutput, shaped, w, h, rid);
    }

    public @Nullable IPatternDetails decode(AEItemKey what, Level level) {
        return decodeStack(what.toStack(), level);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                var blank = new ItemStack(CCItems.EXTREME_BLANK_PATTERN.get());
                player.setItemInHand(hand, blank);
            }
            return InteractionResultHolder.success(stack);
        }
        return super.use(level, player, hand);
    }

    private static CompoundTag writeGenericItem(@Nullable GenericStack gs, HolderLookup.Provider registries) {
        return GenericStack.writeTag(registries, gs);
    }

    private static GenericStack readGenericItem(CompoundTag t, @Nullable Level level) {
        if (level != null) {
            try {
                return GenericStack.readTag(level.registryAccess(), t);
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }

    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag advancedTooltips) {
        IPatternDetails details = decodeStack(stack, null);
        if (details == null) {
            return;
        }

        var label = GuiText.Crafts.text().copy().append(": ").withStyle(ChatFormatting.GRAY);
        var and = Component.literal(" ").copy().append(GuiText.And.text()).append(" ").withStyle(ChatFormatting.GRAY);

        boolean first = true;
        for (var anOut : details.getOutputs()) {
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
            lines.add(Component.empty().append(first ? Component.literal("Substitute: No") : and).append(getStackComponent(gs)));
            first = false;
        }
    }

    protected static Component getStackComponent(GenericStack stack) {
        var amountInfo = stack.what().formatAmount(stack.amount(), appeng.api.stacks.AmountFormat.FULL);
        var displayName = stack.what().getDisplayName();
        return Component.literal(amountInfo + " x ").append(displayName);
    }
}
