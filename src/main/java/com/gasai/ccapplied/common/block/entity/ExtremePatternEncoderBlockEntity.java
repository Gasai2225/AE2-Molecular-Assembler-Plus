package com.gasai.ccapplied.common.block.entity;

import com.gasai.ccapplied.common.core.ExtremePatternCodec;
import com.gasai.ccapplied.common.item.ExtremePatternItem;
import com.gasai.ccapplied.core.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

public class ExtremePatternEncoderBlockEntity extends BlockEntity {

    public final ItemStackHandler input = new ItemStackHandler(81);
    public final ItemStackHandler output = new ItemStackHandler(9);
    public final ItemStackHandler patternSlot = new ItemStackHandler(1); // пустой шаблон
    public final ItemStackHandler encodedSlot = new ItemStackHandler(1); // готовый шаблон

    public ExtremePatternEncoderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTREME_ASSEMBLER_BE.get(), pos, state); // временно, позже зарегистрируем свой тип
    }

    public void encode() {
        ItemStack blank = patternSlot.getStackInSlot(0);
        if (!(blank.getItem() instanceof ExtremePatternItem)) return;

        // Собираем входы/выходы
        ItemStack[] inputs = new ItemStack[81];
        ItemStack[] outputs = new ItemStack[9];
        for (int i = 0; i < 81; i++) inputs[i] = input.getStackInSlot(i);
        for (int i = 0; i < 9; i++) outputs[i] = output.getStackInSlot(i);

        CompoundTag tag = ExtremePatternCodec.write(null, inputs, outputs, false, 200);
        ItemStack encoded = blank.copy();
        ExtremePatternItem.writePatternData(encoded, tag);
        encodedSlot.setStackInSlot(0, encoded);
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
