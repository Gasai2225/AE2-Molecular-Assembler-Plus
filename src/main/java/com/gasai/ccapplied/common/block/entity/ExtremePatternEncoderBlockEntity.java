package com.gasai.ccapplied.common.block.entity;

import com.gasai.ccapplied.common.core.ExtremePatternCodec;
import com.gasai.ccapplied.common.item.ExtremePatternEncodedItem;
import com.gasai.ccapplied.core.registry.ModBlockEntities;
import com.gasai.ccapplied.core.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

public class ExtremePatternEncoderBlockEntity extends BlockEntity {

    /** 9×9 входы, 9 выходов, 1 пустой шаблон, 1 закодированный шаблон */
    public final ItemStackHandler input       = new ItemStackHandler(81);
    public final ItemStackHandler output      = new ItemStackHandler(9);
    public final ItemStackHandler patternSlot = new ItemStackHandler(1); // пустой шаблон
    public final ItemStackHandler encodedSlot = new ItemStackHandler(1); // готовый шаблон

    public ExtremePatternEncoderBlockEntity(BlockPos pos, BlockState state) {
        // Временно используем тип ассемблера, пока свой не зарегистрирован
        super(ModBlockEntities.EXTREME_ASSEMBLER_BE.get(), pos, state);
    }

    /** Кодирует паттерн из сетки input/output. Поглощает 1 пустой шаблон и кладёт готовый в encodedSlot. */
    public void encode() {
        ItemStack blank = patternSlot.getStackInSlot(0);
        if (blank.isEmpty() || blank.getItem() != ModItems.EXTREME_PATTERN.get()) {
            return; // нет пустого шаблона
        }

        // Собираем входы/выходы (копиями, чтобы не шарить ссылки)
        ItemStack[] inputs = new ItemStack[81];
        for (int i = 0; i < 81; i++) {
            ItemStack st = input.getStackInSlot(i);
            inputs[i] = st.isEmpty() ? ItemStack.EMPTY : st.copy();
        }
        ItemStack[] outputsArr = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack st = output.getStackInSlot(i);
            outputsArr[i] = st.isEmpty() ? ItemStack.EMPTY : st.copy();
        }

        // Пишем NBT (без указания recipeId, без подстановок, время по умолчанию 200 тиков)
        CompoundTag tag = ExtremePatternCodec.write(null, inputs, outputsArr, false, 200);

        // Готовим результат
        ItemStack result = new ItemStack(ModItems.ENCODED_EXTREME_PATTERN.get());
        ExtremePatternEncodedItem.writePatternData(result, tag);

        // Если слот результата пуст — просто кладём и потребляем пустой шаблон
        ItemStack existing = encodedSlot.getStackInSlot(0);
        if (existing.isEmpty()) {
            encodedSlot.setStackInSlot(0, result);
            // потребляем один пустой шаблон
            blank = blank.copy();
            blank.shrink(1);
            patternSlot.setStackInSlot(0, blank.isEmpty() ? ItemStack.EMPTY : blank);
            return;
        }

        // Если там уже такой же закодированный — стакаем (если позволяет стак)
        if (ItemStack.isSameItemSameTags(existing, result)) {
            int space = Math.min(existing.getMaxStackSize(), 64) - existing.getCount();
            if (space > 0) {
                existing.grow(1);
                encodedSlot.setStackInSlot(0, existing);
                // потребляем один пустой шаблон
                blank = blank.copy();
                blank.shrink(1);
                patternSlot.setStackInSlot(0, blank.isEmpty() ? ItemStack.EMPTY : blank);
            }
            // иначе места нет — ничего не делаем и пустой шаблон не тратим
            return;
        }

        // В слоте другой предмет — не тратим пустой шаблон и не дропаем
        // (можно добавить логику сигнализировать игроку, если надо)
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("In", input.serializeNBT());
        tag.put("Out", output.serializeNBT());
        tag.put("Pattern", patternSlot.serializeNBT());
        tag.put("Encoded", encodedSlot.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        input.deserializeNBT(tag.getCompound("In"));
        output.deserializeNBT(tag.getCompound("Out"));
        patternSlot.deserializeNBT(tag.getCompound("Pattern"));
        encodedSlot.deserializeNBT(tag.getCompound("Encoded"));
    }
}
