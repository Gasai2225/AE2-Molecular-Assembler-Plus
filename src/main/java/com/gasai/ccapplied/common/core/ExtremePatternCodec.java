package com.gasai.ccapplied.common.core;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Кодек NBT для наших 9x9 шаблонов.
 *
 * Формат:
 * - "recipe" : String (optional) id рецепта (в т.ч. extendedcrafting)
 * - "subs"   : boolean (разрешать подстановки/замены)
 * - "in0".."in80" : CompoundTag для ItemStack входов (пустые ячейки не пишем)
 * - "outCount" : int
 * - "out0"..   : CompoundTag для ItemStack выходов
 * - "time"     : int   // тики крафта (по умолчанию 200)
 */
public final class ExtremePatternCodec {
    private ExtremePatternCodec() {}

    public static final String KEY_RECIPE = "recipe";
    public static final String KEY_SUBS   = "subs";
    public static final String KEY_OUTC   = "outCount";
    public static final String KEY_TIME   = "time";

    /** Записать inputs/outputs/параметры в NBT для хранения в предмет-паттерн. */
    public static CompoundTag write(ResourceLocation recipeId,
                                    ItemStack[] inputs81,
                                    ItemStack[] outputs,
                                    boolean substitutes,
                                    int craftTime) {
        CompoundTag root = new CompoundTag();
        if (recipeId != null) root.putString(KEY_RECIPE, recipeId.toString());
        root.putBoolean(KEY_SUBS, substitutes);

        for (int i = 0; i < 81; i++) {
            ItemStack in = inputs81[i];
            if (in != null && !in.isEmpty()) {
                root.put("in" + i, in.save(new CompoundTag()));
            }
        }
        root.putInt(KEY_OUTC, outputs.length);
        for (int i = 0; i < outputs.length; i++) {
            if (!outputs[i].isEmpty()) {
                root.put("out" + i, outputs[i].save(new CompoundTag()));
            }
        }
        root.putInt(KEY_TIME, Math.max(1, craftTime));
        return root;
    }

    /** Прочитать inputs/outputs из NBT; возвращает детали для IPatternDetails. */
    public static Decoded read(CompoundTag root, AEItemKey definitionKey) {
        boolean subs = root.getBoolean(KEY_SUBS);
        ResourceLocation rid = root.contains(KEY_RECIPE, 8) ? new ResourceLocation(root.getString(KEY_RECIPE)) : null;
        int time = root.contains(KEY_TIME) ? Math.max(1, root.getInt(KEY_TIME)) : 200;

        ItemStack[] inputs = new ItemStack[81];
        for (int i = 0; i < 81; i++) {
            if (root.contains("in" + i, 10)) {
                inputs[i] = ItemStack.of(root.getCompound("in" + i));
            } else {
                inputs[i] = ItemStack.EMPTY;
            }
        }
        int outCount = root.getInt(KEY_OUTC);
        List<GenericStack> outs = new ArrayList<>(Math.max(1, outCount));
        for (int i = 0; i < outCount; i++) {
            if (root.contains("out" + i, 10)) {
                ItemStack is = ItemStack.of(root.getCompound("out" + i));
                if (!is.isEmpty()) outs.add(new GenericStack(AEItemKey.of(is), is.getCount()));
            }
        }
        GenericStack[] outputs = outs.toArray(GenericStack[]::new);
        return new Decoded(definitionKey, inputs, outputs, subs, rid, time);
    }

    public static final class Decoded {
        public final AEItemKey definition;
        public final ItemStack[] inputs81;
        public final GenericStack[] outputs;
        public final boolean substitutes;
        public final ResourceLocation recipeId;
        public final int craftTime;

        public Decoded(AEItemKey definition, ItemStack[] inputs81, GenericStack[] outputs,
                       boolean substitutes, ResourceLocation recipeId, int craftTime) {
            this.definition = definition;
            this.inputs81 = inputs81;
            this.outputs = outputs;
            this.substitutes = substitutes;
            this.recipeId = recipeId;
            this.craftTime = craftTime;
        }
    }
}
