package com.gasai.ccapplied.mixins;

import appeng.client.render.crafting.AssemblerAnimationStatus;
import appeng.core.sync.packets.AssemblerAnimationPacket;
import com.gasai.ccapplied.tiles.ExtremeMolecularAssemblerTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AssemblerAnimationPacket.class, remap = false)
public class AssemblerAnimationPacketMixin {
    @Shadow
    @Final
    private BlockPos pos;

    @Inject(method = "clientPacketData", at = @At("HEAD"), cancellable = true, remap = false)
    private void onClientPacketData(Player player, CallbackInfo ci) {
        if (player.getCommandSenderWorld().getBlockEntity(pos) instanceof ExtremeMolecularAssemblerTileEntity assembler) {
            var packet = (AssemblerAnimationPacket) (Object) this;
            assembler.setAnimationStatus(new AssemblerAnimationStatus(packet.rate, packet.what.wrapForDisplayOrFilter()));
            ci.cancel();
        }
    }
}
