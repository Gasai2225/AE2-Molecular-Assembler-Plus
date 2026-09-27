package com.gasai.ccapplied.botania;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A single item represents both blank and encoded magical patterns. */
public final class MagicalPatternItem extends appeng.crafting.pattern.EncodedPatternItem {
    private static final String ROOT = "ccMagicPattern";
    public MagicalPatternItem() { super(new Properties()); }
    @Override public IPatternDetails decode(ItemStack stack, Level level, boolean recover) { return readPattern(stack, level); }
    @Override public IPatternDetails decode(AEItemKey key, Level level) { return readPattern(key.toStack(), level); }

    @Override public int getMaxStackSize(ItemStack stack) { return isEncoded(stack) ? 1 : 64; }

    @Override public void appendHoverText(ItemStack stack, Level level, List<net.minecraft.network.chat.Component> lines,
            net.minecraft.world.item.TooltipFlag flags) {
        if (isEncoded(stack)) super.appendHoverText(stack, level, lines, flags);
        if (isEncoded(stack) && data(stack).getString("station").equals(MagicalStation.PURE_DAISY.id())) {
            lines.add(net.minecraft.network.chat.Component.translatable("gui.ccapplied.magic.substitutions").append(" ")
                    .append((data(stack).getBoolean("substitutions") ? appeng.core.localization.GuiText.Yes
                            : appeng.core.localization.GuiText.No).text()));
        }
        if (isEncoded(stack) && data(stack).getString("station").equals(MagicalStation.POOL.id())) {
            try {
                var catalyst = PoolCatalyst.fromId(data(stack).getString("catalyst"));
                lines.add(net.minecraft.network.chat.Component.translatable("gui.ccapplied.magic.catalyst." + catalyst.id())
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            } catch (IllegalArgumentException ignored) {
                // The native tooltip already marks invalid patterns.
            }
        }
    }

    // AE2's default reset returns its own blank pattern. Keep our single magical item instead.
    private boolean clearMagicPattern(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (player == null || !player.isShiftKeyDown() || !isEncoded(stack)) return false;
        if (!player.level().isClientSide()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (player.getInventory().getItem(slot) == stack) {
                    player.getInventory().setItem(slot, new ItemStack(this, stack.getCount()));
                    return true;
                }
            }
        }
        return false;
    }
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        clearMagicPattern(player.getItemInHand(hand), player);
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    @Override public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack,
            net.minecraft.world.item.context.UseOnContext context) {
        return clearMagicPattern(stack, context.getPlayer())
                ? net.minecraft.world.InteractionResult.SUCCESS : net.minecraft.world.InteractionResult.PASS;
    }

    @Override public boolean isFoil(ItemStack stack) { return isEncoded(stack); }
    @Override public net.minecraft.network.chat.Component getName(ItemStack stack) {
        if (!isEncoded(stack)) return super.getName(stack);
        try {
            var station = MagicalStation.fromId(data(stack).getString("station"));
            return super.getName(stack).copy().append(" — ")
                    .append(net.minecraft.network.chat.Component.translatable("gui.ccapplied.magic.station." + station.id()));
        } catch (IllegalArgumentException ignored) {
            return super.getName(stack);
        }
    }

    public static CompoundTag data(ItemStack stack) {
        var tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.getCompound(ROOT);
    }
    public static boolean isEncoded(ItemStack stack) {
        return stack.is(BotaniaContent.MAGICAL_PATTERN.get()) && !data(stack).isEmpty();
    }
    public static ItemStack encode(Level level, MagicalStation station, PoolCatalyst catalyst,
            ResourceLocation recipeId, List<ItemStack> inputs) {
        return encode(level, station, catalyst, recipeId, inputs, false);
    }
    public static ItemStack encode(Level level, MagicalStation station, PoolCatalyst catalyst,
            ResourceLocation recipeId, List<ItemStack> inputs, boolean substitutions) {
        if (MagicalRecipeResolver.resolve(level, station, catalyst, recipeId, inputs) == null)
            return ItemStack.EMPTY;
        var root = new CompoundTag();
        root.putInt("version", 1);
        root.putString("station", station.id());
        root.putString("catalyst", catalyst.id());
        root.putString("recipe", recipeId.toString());
        root.putBoolean("substitutions", station == MagicalStation.PURE_DAISY && substitutions);
        var list = new ListTag();
        for (var input : inputs) list.add(input.save(new CompoundTag()));
        root.put("inputs", list);
        var result = new ItemStack(BotaniaContent.MAGICAL_PATTERN.get());
        var tag = new CompoundTag();
        tag.put(ROOT, root);
        result.setTag(tag);
        return result;
    }

    public static MagicalPattern readPattern(ItemStack stack, Level level) {
        if (level == null || !isEncoded(stack)) return null;
        try {
            var root = data(stack);
            if (root.getInt("version") != 1) return null;
            var station = MagicalStation.fromId(root.getString("station"));
            var catalyst = PoolCatalyst.fromId(root.getString("catalyst"));
            var id = ResourceLocation.tryParse(root.getString("recipe"));
            if (id == null) return null;
            var list = root.getList("inputs", Tag.TAG_COMPOUND);
            if (list.isEmpty() || list.size() > MagicalRecipeResolver.MAX_INPUTS) return null;
            var inputs = new ArrayList<ItemStack>();
            for (int i = 0; i < list.size(); i++) inputs.add(ItemStack.of(list.getCompound(i)));
            var recipe = MagicalRecipeResolver.resolve(level, station, catalyst, id, inputs);
            if (recipe == null) return null;
            var alternatives = station == MagicalStation.PURE_DAISY && root.getBoolean("substitutions")
                    ? MagicalRecipeResolver.daisyAlternatives(level, recipe, inputs.get(0)) : List.<ItemStack>of();
            return new MagicalPattern(stack, station, catalyst, inputs, recipe, alternatives);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static final IPatternDetailsDecoder DECODER = new IPatternDetailsDecoder() {
        @Override public boolean isEncodedPattern(ItemStack stack) { return isEncoded(stack); }
        @Override public IPatternDetails decodePattern(ItemStack stack, Level level, boolean recover) { return readPattern(stack, level); }
        @Override public IPatternDetails decodePattern(AEItemKey key, Level level) { return readPattern(key.toStack(), level); }
    };
}
