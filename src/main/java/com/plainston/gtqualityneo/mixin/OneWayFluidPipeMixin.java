package com.plainston.gtqualityneo.mixin;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.client.PipeFlowTexture;

import gregapi.data.CS;
import gregapi.render.ITexture;
import gregapi.tileentity.connectors.MultiTileEntityPipeFluid;
import gregapi.tileentity.connectors.TileEntityBase10ConnectorRendered;
import gregapi.util.UT;
import gregapi.util.WD;

@Mixin(value = MultiTileEntityPipeFluid.class, remap = false)
public abstract class OneWayFluidPipeMixin extends TileEntityBase10ConnectorRendered {

    @Unique
    private byte gtquality$disabledInputs;

    @Unique
    private static final String GTQUALITY_INPUT_MASK = "gtquality.disabledFluidInputs";

    @Inject(method = "readFromNBT2", at = @At("TAIL"), remap = false)
    private void gtquality$readInputs(CompoundTag tag, CallbackInfo ci) {
        gtquality$disabledInputs = (byte) (tag.getByteOr(GTQUALITY_INPUT_MASK, (byte) 0) & 63);
    }

    @Inject(method = "writeToNBT2", at = @At("TAIL"), remap = false)
    private void gtquality$writeInputs(CompoundTag tag, CallbackInfo ci) {
        tag.putByte(GTQUALITY_INPUT_MASK, gtquality$disabledInputs);
    }

    @Inject(method = "onToolClick2", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtquality$toggleInput(String tool, long remainingDurability, long quality, Entity player,
        List<String> chat, Container playerInventory, boolean sneaking, ItemStack heldStack, byte side, float hitX,
        float hitY, float hitZ, CallbackInfoReturnable<Long> cir) {
        if (!GTQualityNeo.ONE_WAY_PIPES.get() || !sneaking || !CS.TOOL_wrench.equals(tool)) return;
        if (isClientSide()) {
            cir.setReturnValue(0L);
            return;
        }
        byte target = UT.Code.getSideWrenching(side, hitX, hitY, hitZ);
        gtquality$disabledInputs ^= CS.SBIT[target];
        WD.mark(this);
        updateClientData();
        if (chat != null) chat.add(
            Component.translatable(
                (gtquality$disabledInputs & CS.SBIT[target]) != 0 ? "gtqualityneo.pipe.output_only"
                    : "gtqualityneo.pipe.bidirectional").getString());
        cir.setReturnValue(10000L);
    }

    @Inject(method = "canAcceptFluidsFrom", at = @At("RETURN"), cancellable = true, remap = false)
    private void gtquality$blockInput(byte side, CallbackInfoReturnable<Boolean> cir) {
        if (GTQualityNeo.ONE_WAY_PIPES.get() && side >= 0 && side < 6 && (gtquality$disabledInputs & CS.SBIT[side]) != 0)
            cir.setReturnValue(false);
    }

    // Fluid pipes do not otherwise use GT6's visual byte. Both full chunk sync and incremental sync carry it.
    @Override
    public byte getVisualData() {
        return GTQualityNeo.ONE_WAY_PIPES.get() ? gtquality$disabledInputs : 0;
    }

    @Override
    public void setVisualData(byte data) {
        gtquality$disabledInputs = (byte) (data & 63);
    }

    @Inject(method = { "getTextureSide", "getTextureConnected" }, at = @At("RETURN"), cancellable = true, remap = false)
    private void gtquality$showArrows(byte side, byte connections, float diameter, int renderPass,
        CallbackInfoReturnable<ITexture> cir) {
        // Use the server-synchronised state even if the client has a different local configuration.
        int mask = gtquality$disabledInputs & connections;
        if (mask != 0) cir.setReturnValue(PipeFlowTexture.withArrows(cir.getReturnValue(), side, mask));
    }

    @Inject(method = "addToolTips", at = @At("TAIL"), remap = false)
    private void gtquality$explainFlow(List<String> tips, ItemStack stack, boolean advanced, CallbackInfo ci) {
        if (GTQualityNeo.ONE_WAY_PIPES.get())
            tips.add(Component.translatable("gtqualityneo.pipe.tip").getString());
    }
}
