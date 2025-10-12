package com.gasai.ccapplied.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.gasai.ccapplied.hooks.GuiGraphicsHooks;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    // Вариант 7 аргументов (x,y,seed,z)
    @Inject(method =
            "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("HEAD"), cancellable = true)
    private void ccapplied$renderCtx7(@Nullable LivingEntity living, @Nullable Level level,
                                      ItemStack stack, int x, int y, int seed, int z,
                                      CallbackInfo ci) {
        if (GuiGraphicsHooks.onCtx((GuiGraphics)(Object)this, living, level, stack, x, y, seed, z)) {
            ci.cancel();
        }
    }

    // Вариант 6 аргументов (x,y,seed) — у части сборок Mojmap 1.20.1 z нет
    @Inject(method =
            "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD"), cancellable = true)
    private void ccapplied$renderCtx6(@Nullable LivingEntity living, @Nullable Level level,
                                      ItemStack stack, int x, int y, int seed,
                                      CallbackInfo ci) {
        if (GuiGraphicsHooks.onCtx((GuiGraphics)(Object)this, living, level, stack, x, y, seed, 0)) {
            ci.cancel();
        }
    }

    // Простая перегрузка
    @Inject(method =
            "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("HEAD"), cancellable = true)
    private void ccapplied$renderSimple(ItemStack stack, int x, int y, CallbackInfo ci) {
        if (GuiGraphicsHooks.onSimple((GuiGraphics)(Object)this, stack, x, y)) {
            ci.cancel();
        }
    }

    // Перегрузка с seed
    @Inject(method =
            "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD"), cancellable = true)
    private void ccapplied$renderSimpleSeed(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        if (GuiGraphicsHooks.onSimpleWithSeed((GuiGraphics)(Object)this, stack, x, y, seed)) {
            ci.cancel();
        }
    }
}
