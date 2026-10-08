package com.plainston.gtqualityneo.qol;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.fluid.CreativeTank;
import com.plainston.gtqualityneo.fluid.CreativeTankMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class QolNetwork {
    private QolNetwork() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GTQualityNeo.MOD_ID, path));
    }
    public record OpenMold(BlockPos pos, int shape, int lastShape) implements CustomPacketPayload {
        public static final Type<OpenMold> TYPE = QolNetwork.type("open_mold");
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenMold> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenMold::pos, ByteBufCodecs.INT, OpenMold::shape,
            ByteBufCodecs.INT, OpenMold::lastShape, OpenMold::new);
        @Override public Type<OpenMold> type() { return TYPE; }
    }
    public record OpenCircuit(int slot, int original) implements CustomPacketPayload {
        public static final Type<OpenCircuit> TYPE = QolNetwork.type("open_circuit");
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenCircuit> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenCircuit::slot, ByteBufCodecs.INT, OpenCircuit::original, OpenCircuit::new);
        @Override public Type<OpenCircuit> type() { return TYPE; }
    }
    public record ClickFluid(int window, int slot, int button, boolean batch) implements CustomPacketPayload {
        public static final Type<ClickFluid> TYPE = QolNetwork.type("click_fluid");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClickFluid> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ClickFluid::window, ByteBufCodecs.VAR_INT, ClickFluid::slot,
            ByteBufCodecs.VAR_INT, ClickFluid::button, ByteBufCodecs.BOOL, ClickFluid::batch, ClickFluid::new);
        @Override public Type<ClickFluid> type() { return TYPE; }
    }
    public record SelectCircuit(int slot, int original, int number) implements CustomPacketPayload {
        public static final Type<SelectCircuit> TYPE = QolNetwork.type("select_circuit");
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectCircuit> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectCircuit::slot, ByteBufCodecs.INT, SelectCircuit::original,
            ByteBufCodecs.VAR_INT, SelectCircuit::number, SelectCircuit::new);
        @Override public Type<SelectCircuit> type() { return TYPE; }
    }
    public record SelectFilter(int window, int slot, net.minecraft.world.item.ItemStack stack) implements CustomPacketPayload {
        public static final Type<SelectFilter> TYPE = QolNetwork.type("select_filter");
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectFilter> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectFilter::window, ByteBufCodecs.VAR_INT, SelectFilter::slot,
            net.minecraft.world.item.ItemStack.STREAM_CODEC, SelectFilter::stack, SelectFilter::new);
        @Override public Type<SelectFilter> type() { return TYPE; }
    }
    public record SelectMold(BlockPos pos, int shape) implements CustomPacketPayload {
        public static final Type<SelectMold> TYPE = QolNetwork.type("select_mold");
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectMold> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SelectMold::pos, ByteBufCodecs.INT, SelectMold::shape, SelectMold::new);
        @Override public Type<SelectMold> type() { return TYPE; }
    }
    public record ConfigureTank(int window, int action, int value, FluidStack fluid) implements CustomPacketPayload {
        public static final Type<ConfigureTank> TYPE = QolNetwork.type("configure_tank");
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureTank> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ConfigureTank::window, ByteBufCodecs.VAR_INT, ConfigureTank::action,
            ByteBufCodecs.INT, ConfigureTank::value, FluidStack.OPTIONAL_STREAM_CODEC, ConfigureTank::fluid, ConfigureTank::new);
        @Override public Type<ConfigureTank> type() { return TYPE; }
    }
    public record TankState(int window, CreativeTank.Settings settings) implements CustomPacketPayload {
        public static final Type<TankState> TYPE = QolNetwork.type("tank_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, TankState> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TankState::window, CreativeTank.Settings.STREAM_CODEC, TankState::settings, TankState::new);
        @Override public Type<TankState> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(OpenMold.TYPE, OpenMold.CODEC);
        registrar.playToClient(OpenCircuit.TYPE, OpenCircuit.CODEC);
        registrar.playToServer(ClickFluid.TYPE, ClickFluid.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player)
                com.plainston.gtqualityneo.fluid.GuiFluidInteraction.request(player, packet.window(), packet.slot(), packet.button(), packet.batch());
        });
        registrar.playToServer(SelectCircuit.TYPE, SelectCircuit.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player)
                CircuitInteraction.select(player, packet.slot(), packet.original(), packet.number());
        });
        registrar.playToServer(SelectFilter.TYPE, SelectFilter.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player)
                FilterGhost.select(player, packet.window(), packet.slot(), packet.stack());
        });
        registrar.playToClient(TankState.TYPE, TankState.CODEC);
        registrar.playToServer(SelectMold.TYPE, SelectMold.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player) MoldInteraction.select(player, packet.pos(), packet.shape());
        });
        registrar.playToServer(ConfigureTank.TYPE, ConfigureTank.CODEC, (packet, context) -> {
            if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof CreativeTankMenu menu)
                || menu.containerId != packet.window() || !menu.stillValid(player)
                || !player.level().mayInteract(player, menu.tank.getBlockPos())) return;
            var current = menu.tank.settings();
            switch (packet.action()) {
                case 0 -> { if (!packet.fluid().isEmpty()) menu.tank.configure(packet.fluid(), current.automatic(), current.rate()); }
                case 1 -> { if (packet.value() >= 0) menu.tank.configure(current.fluid(), current.automatic(), packet.value()); }
                case 2 -> menu.tank.configure(current.fluid(), !current.automatic(), current.rate());
                case 3 -> menu.tank.configure(FluidStack.EMPTY, current.automatic(), current.rate());
                default -> { return; }
            }
            menu.broadcastChanges();
        });
    }
}
