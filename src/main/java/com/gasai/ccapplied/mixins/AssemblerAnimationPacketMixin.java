package com.gasai.ccapplied.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.client.render.crafting.AssemblerAnimationStatus;
import appeng.core.sync.packets.AssemblerAnimationPacket;

import com.gasai.ccapplied.tiles.ExtremeMolecularAssemblerTileEntity;

@Mixin(AssemblerAnimationPacket.class)
public class AssemblerAnimationPacketMixin {

    @Inject(method = "clientPacketData", at = @At("HEAD"), cancellable = true)
    @OnlyIn(Dist.CLIENT)
    private void onClientPacketData(Player player, CallbackInfo ci) {
        AssemblerAnimationPacket packet = (AssemblerAnimationPacket) (Object) this;
        try {
            java.lang.reflect.Field posField = AssemblerAnimationPacket.class.getDeclaredField("pos");
            posField.setAccessible(true);
            net.minecraft.core.BlockPos pos = (net.minecraft.core.BlockPos) posField.get(packet);
            
            BlockEntity te = player.getCommandSenderWorld().getBlockEntity(pos);
            
            if (te instanceof ExtremeMolecularAssemblerTileEntity extremeAssembler) {
                extremeAssembler.setAnimationStatus(new AssemblerAnimationStatus(packet.rate, packet.what.wrapForDisplayOrFilter()));
                ci.cancel();
            }
        } catch (Exception e) {
        }
    }
}
