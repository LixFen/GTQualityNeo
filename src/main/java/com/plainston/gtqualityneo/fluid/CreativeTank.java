package com.plainston.gtqualityneo.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.plainston.gtqualityneo.GTQualityNeo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import java.util.Set;
import java.util.function.Supplier;

public final class CreativeTank extends BlockEntity {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GTQualityNeo.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GTQualityNeo.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GTQualityNeo.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GTQualityNeo.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GTQualityNeo.MOD_ID);
    public static final Supplier<CreativeTankBlock> BLOCK = BLOCKS.registerBlock("creative_tank", CreativeTankBlock::new,
        properties -> properties.strength(5, 10).noOcclusion());
    public static final Supplier<net.minecraft.world.item.BlockItem> ITEM = ITEMS.registerSimpleBlockItem("creative_tank", BLOCK);
    public static final Supplier<BlockEntityType<CreativeTank>> TYPE = TYPES.register("creative_tank", TankType::new);
    public static final Supplier<DataComponentType<Settings>> SETTINGS = COMPONENTS.register("tank_settings", () ->
        DataComponentType.<Settings>builder().persistent(Settings.CODEC).networkSynchronized(Settings.STREAM_CODEC).build());
    static {
        TABS.register("main", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.gtqualityneo"))
            .icon(() -> new ItemStack(ITEM.get())).displayItems((parameters, output) -> output.accept(ITEM.get())).build());
    }

    private static final class TankType extends BlockEntityType<CreativeTank> {
        TankType() { super(CreativeTank::new, Set.of(BLOCK.get())); }
    }

    public record Settings(FluidStack fluid, boolean automatic, int rate) {
        public static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(Settings::fluid),
            Codec.BOOL.fieldOf("automatic").forGetter(Settings::automatic),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("rate").forGetter(Settings::rate)).apply(instance, Settings::new));
        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Settings> STREAM_CODEC = StreamCodec.composite(
            FluidStack.OPTIONAL_STREAM_CODEC, Settings::fluid, ByteBufCodecs.BOOL, Settings::automatic,
            ByteBufCodecs.VAR_INT, Settings::rate, Settings::new);
        public static final Settings DEFAULT = new Settings(FluidStack.EMPTY, false, 1000);
    }

    private Settings settings = Settings.DEFAULT;
    private long outputTick = Long.MIN_VALUE;
    private int emitted;
    private final SnapshotJournal<Integer> budget = new SnapshotJournal<>() {
        @Override protected Integer createSnapshot() { return emitted; }
        @Override protected void revertToSnapshot(Integer previous) { emitted = previous; }
    };

    public CreativeTank(BlockPos pos, BlockState state) { super(TYPE.get(), pos, state); }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); TYPES.register(bus); COMPONENTS.register(bus); TABS.register(bus);
        CreativeTankMenu.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, TYPE.get(), (tank, side) -> tank.handler));
    }

    public Settings settings() { return settings; }

    public void configure(FluidStack fluid, boolean automatic, int rate) {
        settings = new Settings(fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1), automatic, Math.max(0, rate));
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        settings = input.read("settings", Settings.CODEC).orElse(Settings.DEFAULT);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("settings", Settings.CODEC, settings);
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(SETTINGS.get(), settings);
    }
    @Override protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        settings = components.getOrDefault(SETTINGS.get(), Settings.DEFAULT);
    }

    private int available() {
        if (level == null || level.isClientSide()) return 0;
        if (outputTick != level.getGameTime()) { outputTick = level.getGameTime(); emitted = 0; }
        return Math.max(0, settings.rate() - emitted);
    }

    public final ResourceHandler<FluidResource> handler = new ResourceHandler<>() {
        @Override public int size() { return 1; }
        @Override public FluidResource getResource(int index) { java.util.Objects.checkIndex(index, 1); return FluidResource.of(settings.fluid()); }
        @Override public long getAmountAsLong(int index) { return getResource(index).isEmpty() ? 0 : Integer.MAX_VALUE; }
        @Override public long getCapacityAsLong(int index, FluidResource resource) { return isValid(index, resource) ? Integer.MAX_VALUE : 0; }
        @Override public boolean isValid(int index, FluidResource resource) { java.util.Objects.checkIndex(index, 1); return resource.isEmpty() || resource.equals(getResource(index)); }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) { java.util.Objects.checkIndex(index, 1); return 0; }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            java.util.Objects.checkIndex(index, 1);
            if (resource.isEmpty() || !resource.equals(getResource(index)) || amount <= 0) return 0;
            int moved = Math.min(amount, available());
            if (moved > 0) { budget.updateSnapshots(transaction); emitted += moved; }
            return moved;
        }
    };

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, CreativeTank tank) {
        if (level.isClientSide() || !tank.settings.automatic() || tank.settings.fluid().isEmpty()) return;
        FluidResource resource = tank.handler.getResource(0);
        for (Direction side : Direction.values()) {
            int available = tank.available();
            if (available == 0) break;
            BlockPos neighbor = pos.relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            var target = level.getCapability(Capabilities.Fluid.BLOCK, neighbor, side.getOpposite());
            if (target == null) continue;
            try (var transaction = Transaction.openRoot()) {
                int reserved = tank.handler.extract(resource, available, transaction);
                int accepted = target.insert(resource, reserved, transaction);
                tank.emitted -= reserved - accepted;
                transaction.commit();
            }
        }
    }
}
