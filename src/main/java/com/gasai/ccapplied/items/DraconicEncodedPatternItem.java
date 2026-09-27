package com.gasai.ccapplied.items;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.EncodedPatternItem;
import com.gasai.ccapplied.core.registry.CCItems;
import com.gasai.ccapplied.core.registry.CCOptionalMods;
import com.gasai.ccapplied.patterns.DraconicFusionPattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DraconicEncodedPatternItem extends EncodedPatternItem {
    public static final String NBT_ROOT = "ccapplied_draconic";
    private static final String NBT_INPUTS = "inputs";
    private static final String NBT_OUTPUTS = "outputs";
    private static final String NBT_TIER = "tier";
    private static final String NBT_TOTAL_ENERGY = "total_energy";
    private static final String NBT_RECIPE_ID = "recipeId";

    public DraconicEncodedPatternItem(Properties props) {
        super(props);
    }

    @Override
    public void addToMainCreativeTab(CreativeModeTab.Output output) {
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        var root = getRoot(stack);
        if (root == null) {
            return;
        }

        String tier = root.contains(NBT_TIER, Tag.TAG_STRING) ? root.getString(NBT_TIER) : "WYVERN";
        long energy = root.contains(NBT_TOTAL_ENERGY, Tag.TAG_LONG) ? root.getLong(NBT_TOTAL_ENERGY) : 0L;

        tooltip.add(Component.literal("Fusion Tier: " + prettyTier(tier)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Energy Cost: " + NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0L, energy)) + " OP")
                .withStyle(ChatFormatting.GOLD));
        if (root.contains(NBT_RECIPE_ID, Tag.TAG_STRING)) {
            tooltip.add(Component.literal("Recipe: " + root.getString(NBT_RECIPE_ID)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public ItemStack encode(
            GenericStack[] inputs13,
            GenericStack output,
            DraconicFusionPattern.FusionTier tier,
            long totalEnergy,
            @Nullable ResourceLocation recipeId) {
        var out = new ItemStack(this);
        var tag = new CompoundTag();
        var root = new CompoundTag();

        var inList = new ListTag();
        for (int i = 0; i < DraconicFusionPattern.TOTAL_INPUT_SLOTS; i++) {
            var gs = i < inputs13.length ? inputs13[i] : null;
            inList.add(gs == null ? new CompoundTag() : writeGenericItem(gs));
        }
        root.put(NBT_INPUTS, inList);

        var outList = new ListTag();
        outList.add(writeGenericItem(output));
        root.put(NBT_OUTPUTS, outList);

        root.putString(NBT_TIER, tier.name());
        root.putLong(NBT_TOTAL_ENERGY, Math.max(0, totalEnergy));
        if (recipeId != null) {
            root.putString(NBT_RECIPE_ID, recipeId.toString());
        }

        tag.put(NBT_ROOT, root);
        out.setTag(tag);
        return out;
    }

    @Override
    public @Nullable IPatternDetails decode(ItemStack stack, Level level, boolean tryRecovery) {
        if (!stack.hasTag()) {
            return null;
        }
        var tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_ROOT, Tag.TAG_COMPOUND)) {
            return null;
        }

        var root = tag.getCompound(NBT_ROOT);
        var inList = root.getList(NBT_INPUTS, Tag.TAG_COMPOUND);
        var outList = root.getList(NBT_OUTPUTS, Tag.TAG_COMPOUND);
        if (outList.isEmpty()) {
            return null;
        }

        var inputs = new GenericStack[DraconicFusionPattern.TOTAL_INPUT_SLOTS];
        for (int i = 0; i < Math.min(inList.size(), inputs.length); i++) {
            inputs[i] = readGenericItem(inList.getCompound(i));
        }

        var outputs = new ArrayList<GenericStack>();
        for (int i = 0; i < outList.size(); i++) {
            var gs = readGenericItem(outList.getCompound(i));
            if (gs != null) {
                outputs.add(gs);
            }
        }
        if (outputs.isEmpty()) {
            return null;
        }

        var tierName = root.contains(NBT_TIER, Tag.TAG_STRING) ? root.getString(NBT_TIER) : "WYVERN";
        DraconicFusionPattern.FusionTier tier;
        try {
            tier = DraconicFusionPattern.FusionTier.valueOf(tierName);
        } catch (IllegalArgumentException e) {
            tier = DraconicFusionPattern.FusionTier.WYVERN;
        }

        long totalEnergy = root.contains(NBT_TOTAL_ENERGY, Tag.TAG_LONG) ? root.getLong(NBT_TOTAL_ENERGY) : 0L;
        ResourceLocation recipeId = null;
        if (root.contains(NBT_RECIPE_ID, Tag.TAG_STRING)) {
            recipeId = ResourceLocation.parse(root.getString(NBT_RECIPE_ID));
        }

        var inputStacks = new ItemStack[DraconicFusionPattern.TOTAL_INPUT_SLOTS];
        for (int i = 0; i < inputStacks.length; i++) {
            if (inputs[i] != null && inputs[i].what() instanceof AEItemKey k) {
                inputStacks[i] = k.toStack((int) Math.max(1, Math.min(inputs[i].amount(), 64)));
            } else {
                inputStacks[i] = ItemStack.EMPTY;
            }
        }

        ItemStack outStack = ItemStack.EMPTY;
        if (outputs.get(0).what() instanceof AEItemKey outKey) {
            outStack = outKey.toStack((int) Math.max(1, Math.min(outputs.get(0).amount(), 64)));
        }

        return new DraconicFusionPattern(
                AEItemKey.of(stack),
                inputs,
                outputs.toArray(GenericStack[]::new),
                inputStacks,
                outStack,
                tier,
                totalEnergy,
                recipeId);
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
                if (CCOptionalMods.isDraconicEvolutionLoaded() && CCItems.DRACONIC_BLANK_PATTERN != null) {
                    var blank = new ItemStack(CCItems.DRACONIC_BLANK_PATTERN.get());
                    player.setItemInHand(hand, blank);
                }
            }
            return InteractionResultHolder.success(stack);
        }

        return super.use(level, player, hand);
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
        if (t.isEmpty() || !t.contains("item", Tag.TAG_STRING)) {
            return null;
        }

        ResourceLocation id;
        try {
            id = ResourceLocation.parse(t.getString("item"));
        } catch (Exception e) {
            return null;
        }

        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        var amount = t.contains("amount", Tag.TAG_LONG) ? t.getLong("amount") : 1L;
        var stack = new ItemStack(item, (int) Math.max(1, Math.min(amount, 64)));
        if (t.contains("nbt", Tag.TAG_COMPOUND)) {
            try {
                stack.setTag(t.getCompound("nbt").copy());
            } catch (Exception ignored) {
            }
        }
        var key = AEItemKey.of(stack);
        return key == null ? null : new GenericStack(key, amount);
    }

    private static @Nullable CompoundTag getRoot(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_ROOT, Tag.TAG_COMPOUND)) {
            return null;
        }
        return tag.getCompound(NBT_ROOT);
    }

    private static String prettyTier(String tier) {
        return switch (tier.toUpperCase(Locale.ROOT)) {
            case "CHAOTIC" -> "Chaotic";
            case "DRACONIC" -> "Draconic";
            default -> "Wyvern";
        };
    }
}
