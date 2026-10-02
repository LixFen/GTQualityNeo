package com.plainston.gtqualityneo.qol;

import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.util.UT;
import gregtech.tileentity.tools.MultiTileEntityMold;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class QolEvents {
    private QolEvents() {}

    public static void damage(LivingIncomingDamageEvent event) {
        if (GTQualityNeo.HEAT_HAZMAT_IMMUNITY.get() && event.getSource().is(DamageTypeTags.IS_FIRE)
            && UT.Entities.isWearingFullHeatHazmat(event.getEntity())) event.setCanceled(true);
    }

    public static void moldClick(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()
            || !MoldInteraction.isChisel(player.getMainHandItem()) || !event.getLevel().mayInteract(player, event.getPos())) return;
        var tile = event.getLevel().getBlockEntity(event.getPos());
        if (tile == null || tile.getClass() != MultiTileEntityMold.class) return;
        var mold = (MultiTileEntityMold) tile;
        if (!MoldInteraction.canEdit(mold)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new QolNetwork.OpenMold(event.getPos(),
                MoldInteraction.shape(mold), player.getPersistentData()
                    .getCompoundOrEmpty(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG)
                    .getIntOr("gtqualityneo.lastMoldShape", 0)));
        }
    }
}
