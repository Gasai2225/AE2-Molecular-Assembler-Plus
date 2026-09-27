package com.gasai.ccapplied.botania.client;

import appeng.api.client.AEKeyRenderHandler;
import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEItemKey;
import com.gasai.ccapplied.botania.BotaniaContent;
import com.gasai.ccapplied.botania.ManaKey;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public final class ManaKeyRenderer implements AEKeyRenderHandler<ManaKey> {
    @Override public void drawInGui(Minecraft minecraft, GuiGraphics graphics, int x, int y, ManaKey key) {
        AEKeyRendering.drawInGui(minecraft, graphics, x, y, AEItemKey.of(BotaniaContent.MANA_CAPSULE.get()));
    }

    @Override public void drawOnBlockFace(PoseStack pose, MultiBufferSource buffers, ManaKey key,
            float scale, int light, Level level) {
        AEKeyRendering.drawOnBlockFace(pose, buffers, AEItemKey.of(BotaniaContent.MANA_CAPSULE.get()), scale, light, level);
    }

    @Override public Component getDisplayName(ManaKey key) { return key.getDisplayName(); }
}
