package com.plainston.gtqualityneo.integration.jade;

import gregapi.data.CS;
import gregapi.data.FM;
import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.recipes.Recipe;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import gregapi.data.FL;
import gregapi.util.ST;
import gregapi.code.ItemNBT;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.Element;
import snownee.jade.api.view.ProgressView;

/** Jade presentation matching GTQuality's special-device WDMla information. */
public enum GT6DetailsComponentProvider implements IBlockComponentProvider {
    INSTANCE;
    private static final String PARAMS = "gtquality.parameters";
    private static final String STATE_SUPPORTED = "gtquality.state.supported";
    private static final String STATE_ON = "gtquality.state.on";
    private static final String STATE_PASSIVE = "gtquality.state.passive";
    private static final String STATE_ACTIVE = "gtquality.state.active";

    @Override public Identifier getUid() { return GT6DetailsProvider.UID; }
    @Override public int getDefaultPriority() { return 1100; }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains(GT6DetailsProvider.DATA)) return;
        CompoundTag data = accessor.getServerData().getCompoundOrEmpty(GT6DetailsProvider.DATA);
        String kind = data.getStringOr("kind", "");
        boolean throughMultiblockPart = data.getBooleanOr("through_part", false);
        if (throughMultiblockPart) tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
        if (throughMultiblockPart) tooltip.add(Component.translatable("gtqualityneo.jade.controller"));
        if (data.getBooleanOr(STATE_SUPPORTED, false)) appendInterfaceMachineState(tooltip, data);
        if (kind.equals("MultiTileEntityGeneratorSolid") || kind.equals("MultiTileEntityGeneratorFluidBed")
            || kind.equals("MultiTileEntityMixingBowl") || kind.equals("MultiTileEntitySiftingTable")
            || kind.equals("MultiTileEntityReactorCore")) tooltip.remove(JadeIds.UNIVERSAL_ITEM_STORAGE);
        appendMachineInfo(tooltip, kind, data, accessor, throughMultiblockPart);
    }

    private static void appendMachineInfo(ITooltip tooltip, String kind, CompoundTag data, BlockAccessor accessor, boolean throughMultiblockPart) {
        if (kind.equals("MultiTileEntitySmeltery") || kind.equals("MultiTileEntityCrucible")) {
            addLine(
                tooltip,
                "temperature",
                data.getShortOr("gt.temperature", (short) 0) + " / " + data.getLongOr("gtquality.max_temperature", 0L) + " K");
            appendMaterials(tooltip, data.getCompoundOrEmpty("gt.materials"));
        } else if (kind.equals("MultiTileEntityMold")) {
            appendMoldInfo(tooltip, data);
        } else if (kind.equals("MultiTileEntityBoilerTank")) {
            appendBoiler(tooltip, data, throughMultiblockPart);
        } else if (kind.equals("MultiTileEntityGeneratorSolid")) {
            appendSolidGenerator(tooltip, accessor, data);
        } else if (kind.equals("MultiTileEntityGeneratorFluidBed")) {
            appendFluidBed(tooltip, accessor, data, throughMultiblockPart);
        } else if (kind.equals("MultiTileEntityGeneratorLiquid")) {
            appendLiquidGenerator(tooltip, data, FM.Burn.mRecipeFluidMap, throughMultiblockPart);
        } else if (kind.equals("MultiTileEntityRock")) {
            addItem(tooltip, "rock_content", getItem(data, "gt.value"));
        } else if (kind.equals("TileEntityBase08FluidContainer")) {
            if (throughMultiblockPart) {
                addStoredTank(tooltip, "tank", data);
            }
        } else if (kind.equals("MultiTileEntityMultiBlockPart")) {
            return;
        } else if (kind.equals("MultiTileEntityAnvil")) {
            appendAnvil(tooltip, data, accessor);
        } else if (kind.equals("MultiTileEntityEngineSteam")) {
            appendSteamEngine(tooltip, data);
        } else if (kind.equals("MultiTileEntityTurbineSteam")) {
            appendSteamTurbine(tooltip, data);
        } else if (kind.equals("MultiTileEntityAxle")) {
            addLine(tooltip, "transfer_ru", data.getLongOr("gt.transfer.ru", 0L) + " RU/t");
        } else if (kind.equals("MultiTileEntityWireElectric")) {
            addLine(tooltip, "transfer_eu", data.getLongOr("gt.transfer.eu", 0L) + " EU/t");
        } else if (kind.equals("MultiTileEntityFluidSpring")) {
            FluidStack fluid = getFluid(data, "gt.spring");
            if (fluid != null) {
                addLine(tooltip, "spring_fluid", FL.name(fluid, true));
                addLine(tooltip, "spring_rate", formatTime(fluid.getAmount()) + " / bucket");
            }
        } else if (kind.equals("MultiTileEntityMixingBowl")) {
            appendMixingBowl(tooltip, accessor, data, throughMultiblockPart);
        } else if (kind.equals("TileEntityBase08Barrel") || kind.equals("MultiTileEntityTank")) {
            if (throughMultiblockPart) {
                addStoredTank(tooltip, "tank", data);
            }
        } else if (kind.equals("MultiTileEntityPipeFluid")) {
            if (throughMultiblockPart) appendPipe(tooltip, data);
        } else if (kind.equals("MultiTileEntitySiftingTable")) {
            appendSifter(tooltip, accessor, data);
        } else if (kind.equals("MultiTileEntityMotorLiquid")) {
            appendLiquidMotor(tooltip, data, throughMultiblockPart);
        } else if (kind.equals("MultiTileEntityReactorCore")) {
            appendReactor(tooltip, accessor, data, throughMultiblockPart);
        } else if (kind.equals("MultiTileEntityBush")) {
            appendBush(tooltip, data);
        }
    }

    private static void appendMoldInfo(ITooltip tooltip, CompoundTag data) {
        addLine(
            tooltip,
            "temperature",
            data.getLongOr("gt.temperature", 0L) + "K/" + data.getLongOr("gtquality.max_temperature", 0L) + "K");

        OreDictPrefix prefix = OreDictPrefix.get(data.getStringOr("mold_prefix", ""));
        addLine(tooltip, "mold_produces", prefix == null ? tr("mold_unselected") : prefix.mNameLocal);

        byte directions = data.getByteOr("gt.connection", (byte) 0);
        StringBuilder autoInputDirections = new StringBuilder();
        for (byte side : CS.COMPASS_DIRECTIONS) {
            if (!CS.SIDES_HORIZONTAL[side] || (directions & CS.SBIT[side]) == 0) continue;
            if (autoInputDirections.length() > 0) autoInputDirections.append('&');
            autoInputDirections.append(tr(cardinalDirectionKey(side)));
        }
        addLine(
            tooltip,
            "mold_auto_input_directions",
            autoInputDirections.length() == 0 ? tr("mold_no_directions") : autoInputDirections.toString());
        addLine(
            tooltip,
            "mold_redstone_mode",
            data.getBooleanOr("gt.mode", false) ? tr("mold_redstone_requires_signal") : tr("mold_redstone_uncontrolled"));
    }

    private static String cardinalDirectionKey(byte side) {
        if (side == CS.SIDE_NORTH) return "direction.north";
        if (side == CS.SIDE_EAST) return "direction.east";
        if (side == CS.SIDE_SOUTH) return "direction.south";
        return "direction.west";
    }

    private static void appendBoiler(ITooltip tooltip, CompoundTag data, boolean throughMultiblockPart) {
        CompoundTag params = data.getCompoundOrEmpty(PARAMS);
        FluidStack fuel = getFluid(data, "gt.tank.0");
        FluidStack steam = getFluid(data, "gt.tank.1");
        long steamCapacity = Math.max(1, params.getLongOr("gt.capacity.su", 0L));
        if (throughMultiblockPart) {
            addFluidGauge(tooltip, "fuel", fuel, 4000);
            addProgressGauge(tooltip, "steam", steam == null ? 0 : steam.getAmount(), steamCapacity, steam);
        }
        addLine(tooltip, "heat", data.getLongOr("gt.energy", 0L) + " HU");

        long steamAmount = steam == null ? 0 : steam.getAmount();
        long outFactor = Math.max(0, Math.min(3, 4 * steamAmount / steamCapacity - 1));
        int pressure = Math.max(0, Math.min(31, data.getIntOr("gt.boiler.pressure", 0)));
        addLine(tooltip, "boiler_pressure", pressure + " / 31 (" + formatPercent(pressure * 100.0 / 31) + ")");
        long efficiency = data.contains("gt.eff") ? data.getLongOr("gt.eff", 0L) : 10000;
        addLine(tooltip, "efficiency", formatPercent(efficiency / 100.0));
        addLine(tooltip, "steam_output", params.getLongOr("gt.output.su", 0L) * outFactor + " L/t");
    }

    private static void appendSolidGenerator(ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
        ItemStack fuel = getInventory(accessor, data).get(0);
        ItemStack ash = getInventory(accessor, data).get(1);
        if (ash == null && fuel != null && FM.Furnace.mRecipeItemMap.get(fuel) == null) {
            ash = fuel;
            fuel = null;
        }
        addItem(tooltip, "fuel", fuel);
        addItem(tooltip, "ash", ash);
        if (data.contains("gt.active")) {
            long output = data.getCompoundOrEmpty(PARAMS)
                .getLongOr("gt.output", 0L);
            long ticks = output <= 0 ? data.getLongOr("gt.energy", 0L) : data.getLongOr("gt.energy", 0L) / output;
            addLine(tooltip, "burn_time", formatTime(ticks));
            addLine(tooltip, "heat_output", output + " HU/t");
        }
    }

    private static void appendFluidBed(ITooltip tooltip, BlockAccessor accessor, CompoundTag data, boolean throughMultiblockPart) {
        ItemStack fuel = getInventory(accessor, data).get(0);
        ItemStack ash = getInventory(accessor, data).get(1);
        if (ash == null && fuel != null && FM.FluidBed.mRecipeItemMap.get(fuel) == null) {
            ash = fuel;
            fuel = null;
        }
        if (throughMultiblockPart) addFluidGauge(tooltip, "calcite", getFluid(data, "gt.tank"), 1000);
        addItem(tooltip, "fuel", fuel);
        addItem(tooltip, "ash", ash);
        if (data.contains("gt.active")) {
            long output = data.getCompoundOrEmpty(PARAMS)
                .getLongOr("gt.output", 0L);
            long ticks = output <= 0 ? data.getLongOr("gt.energy", 0L) : data.getLongOr("gt.energy", 0L) / output;
            addLine(tooltip, "burn_time", formatTime(ticks));
            addLine(tooltip, "heat_output", output + " HU/t");
        }
    }

    private static void appendLiquidGenerator(ITooltip tooltip, CompoundTag data, Map<String, ?> recipeMap,
        boolean throughMultiblockPart) {
        FluidStack fuel = getFluid(data, "gt.tank");
        long output = Math.max(
            1,
            data.getCompoundOrEmpty(PARAMS)
                .getLongOr("gt.output", 0L));
        if (throughMultiblockPart) addFluidGauge(tooltip, "fuel", fuel, output * 10);
        if (data.contains("gt.active")) {
            double rate = getFuelConsumption(recipeMap, fuel, output);
            addLine(tooltip, "fuel_rate", String.format(Locale.ROOT, "%.2f L/t", rate));
            addLine(tooltip, "heat_output", output + " HU/t");
        }
    }

    private static double getFuelConsumption(Map<String, ?> recipeMap, FluidStack fuel, long output) {
        if (fuel == null) return 0;
        Object recipesObject = recipeMap.get(
            FL.regName(fuel.getFluid()));
        if (!(recipesObject instanceof Iterable<?>recipes)) return 0;
        for (Object entry : recipes) {
            if (!(entry instanceof Recipe recipe)) continue;
            long fuelHeat = -recipe.mEUt * recipe.mDuration;
            if (fuelHeat <= 0) return 0;
            if (recipe.mFluidInputs != null) {
                for (FluidStack input : recipe.mFluidInputs) {
                    if (input != null && FL.equal(input, fuel) && input.getAmount() > 0) {
                        fuelHeat /= input.getAmount();
                        break;
                    }
                }
            }
            return fuelHeat <= 0 ? 0 : output / (double) fuelHeat;
        }
        return 0;
    }

    private static void appendAnvil(ITooltip tooltip, CompoundTag data, BlockAccessor accessor) {
        long durability = data.getLongOr("gt.durability", 0L) / 1000;
        addLine(tooltip, "anvil_durability", (durability / 10) + "." + (durability % 10));
        int side = accessor.getHitResult() == null ? -1 : accessor.getHitResult().getDirection().get3DDataValue();
        int facing = data.getIntOr("gt.facing", 0);
        boolean endFace = side >= 0
            && ((facing == 2 || facing == 3) ? side >= 4 : (facing == 4 || facing == 5) && side > 1 && side < 4);
        if (endFace) {
            boolean bigEnd = facing == 2 || facing == 3 ? (side - facing) % 2 == 1 : (facing - side) % 2 == 1;
            addLine(tooltip, "anvil_end", tr(bigEnd ? "anvil_big" : "anvil_small"));
        }
    }

    private static void appendSteamEngine(ITooltip tooltip, CompoundTag data) {
        CompoundTag params = data.getCompoundOrEmpty(PARAMS);
        long capacity = Math.max(1, params.getLongOr(CS.NBT_CAPACITY, 0L));
        long energy = data.getLongOr(CS.NBT_ENERGY, 0L);
        addProgressGauge(tooltip, "stored_ku", energy, capacity, null);
        long output = params.getLongOr("gt.output", 0L);
        long state = data.getLongOr(CS.NBT_VISUAL, 0L);
        long efficiency = data.getLongOr(CS.NBT_EFFICIENCY, 0L);
        long ku = output * (state + 1) / 16;
        long steam = efficiency <= 0 ? 0 : ku * 2 * 10000 / efficiency;
        if (!data.contains(CS.NBT_ACTIVE)) {
            ku = 0;
            steam = 0;
        }
        addLine(tooltip, "energy_output", ku + " KU/t");
        addLine(tooltip, "steam_input", steam + " L/t");
    }

    private static void appendSteamTurbine(ITooltip tooltip, CompoundTag data) {
        long output = data.contains("gt.output.su") ? data.getLongOr("gt.output.su", 0L) : amount(getFluid(data, "gt.tank.0"));
        long input = data.getCompoundOrEmpty(PARAMS)
            .getLongOr("gt.input", 0L);
        if (output > input * 2) {
            addLine(tooltip, "state", tr("overpowered"));
        } else {
            addLine(tooltip, "steam_input", output + " L/t");
            addLine(tooltip, "energy_output", output / 3 + " RU/t");
        }
    }

    private static void appendLiquidMotor(ITooltip tooltip, CompoundTag data, boolean throughMultiblockPart) {
        long output = data.getCompoundOrEmpty(PARAMS)
            .getLongOr("gt.output", 0L);
        FluidStack fuel = getFluid(data, "gt.tank.0");
        if (throughMultiblockPart) {
            long capacity = Math.max(1, output * 10);
            addFluidGauge(tooltip, "fuel", fuel, capacity);
            addFluidGauge(tooltip, "exhaust", getFluid(data, "gt.tank.1"), capacity);
        }
        if (data.contains("gt.energy")) {
            addLine(
                tooltip,
                "fuel_rate",
                String.format(Locale.ROOT, "%.2f L/t", getFuelConsumption(FM.Engine.mRecipeFluidMap, fuel, output)));
            addLine(tooltip, "energy_output", output + " RU/t");
        }
    }

    private static void appendReactor(ITooltip tooltip, BlockAccessor accessor, CompoundTag data, boolean throughMultiblockPart) {
        FluidStack coolant = getFluid(data, "gt.tank.0");
        if (throughMultiblockPart) {
            addFluidGauge(tooltip, "coolant", coolant, 64000);
            addFluidGauge(tooltip, "hot_fluid", getFluid(data, "gt.tank.1"), 64000);
        }
        Map<Integer, ItemStack> rods = getInventory(accessor, data);
        long heat = 0;
        for (Map.Entry<Integer, ItemStack> entry : rods.entrySet()) {
            ItemStack rod = entry.getValue();
            if (rod == null) continue;
            long neutron = data.getLongOr("gt.value.o." + entry.getKey(), 0L);
            switch (getRodType(ST.meta(rod))) {
                case 1 -> heat += neutron * 2;
                case 2 -> heat += neutron / 2;
                case 3 -> heat += neutron;
                default -> {}
            }
            String durability = ItemNBT.has(rod) ? " · " + formatTime(
                ItemNBT.get(rod)
                    .getLongOr(CS.NBT_DURABILITY, 0L) / 100)
                : "";
            addLine(tooltip, "reactor_rod", entry.getKey() + ": " + rod.getHoverName().getString() + durability);
        }
        if (coolant != null && FL.equal(MT.Sn.mLiquid, coolant)) heat = (long) Math.ceil(heat / 3.0);
        else if (coolant != null && FL.equal(MT.Na.mLiquid, coolant)) heat = (long) Math.ceil(heat / 6.0);
        if (heat != 0) addLine(tooltip, "heat_output", heat + " HU/t");

        StringBuilder neutrons = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (!data.contains("gt.value.o." + i)) continue;
            if (neutrons.length() != 0) neutrons.append(" | ");
            neutrons.append(i)
                .append(": ")
                .append(data.getLongOr("gt.value.o." + i, 0L));
        }
        if (neutrons.length() != 0) addLine(tooltip, "neutron_count", neutrons.toString());
    }

    private static void appendBush(ITooltip tooltip, CompoundTag data) {
        long state = data.getLongOr("gt.state", 0L);
        long progress = data.getLongOr("gt.progress", 0L);
        if (state == 3) {
            addProgressGauge(tooltip, "growth", 1, 1, null, tr("mature"));
        } else {
            long current = state * 256 + progress + (progress < 0 ? 256 : 0);
            addProgressGauge(tooltip, "growth", Math.max(0, current), 768, null);
        }
        addItem(tooltip, "crop_output", getItem(data, "gt.value"));
    }

    private static void appendInterfaceMachineState(ITooltip tooltip, CompoundTag data) {
        String state = !data.getBooleanOr(STATE_ON, false) || !data.getBooleanOr(STATE_PASSIVE, false) ? "state.stopped"
            : data.getBooleanOr(STATE_ACTIVE, false) ? "state.running" : "state.idle";
        addStateLine(tooltip, state);
    }

    private static int getRodType(int meta) {
        if (meta > 9400) return 1;
        if (meta > 9300) return 0;
        if (meta > 9209) return 3;
        if (meta == 9202) return 2;
        return 0;
    }

    private static String formatTime(long ticks) {
        if (ticks < 20) return ticks + " t";
        if (ticks < 1200) return (ticks / 20) + " s";
        if (ticks < 72000) return (ticks / 1200) + " min " + ((ticks % 1200) / 20) + " s";
        if (ticks < 1728000) return (ticks / 72000) + " h " + ((ticks % 72000) / 1200) + " min";
        return (ticks / 1728000) + " d " + ((ticks % 1728000) / 72000) + " h";
    }

    private static String formatPercent(double percent) {
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }

    private static String tr(String key) {
        return I18n.get("gtqualityneo.jade." + key);
    }

    private static void appendMixingBowl(ITooltip tooltip, BlockAccessor accessor, CompoundTag data, boolean throughPart) {
        Map<Integer, ItemStack> inventory = getInventory(accessor, data);
        addItems(tooltip, "item_inputs", inventory, 0, 6);
        addItem(tooltip, "item_output", inventory.get(6));
        if (throughPart) {
            for (int i = 0; i < 6; i++) addFluidSlot(tooltip, "fluid_input", i, getFluid(data, "gt.tank.in." + i));
            for (int i = 0; i < 2; i++) addFluidSlot(tooltip, "fluid_output", i, getFluid(data, "gt.tank.out." + i));
        }
    }

    private static void appendPipe(ITooltip tooltip, CompoundTag data) {
        int columns = data.contains("gt.mlast.8") ? 3 : 2;
        List<Element> row = new ArrayList<>();
        for (int i = 0; i < columns * columns; i++) {
            FluidStack fluid = getFluid(data, "gt.tank." + i);
            if (i % columns != 0) row.add(JadeUI.spacer(8, 0));
            if (fluid == null) row.add(JadeUI.text(Component.literal(tr("empty"))));
            else {
                row.add(JadeUI.fluid(JadeFluidObject.of(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch())));
                row.add(JadeUI.text(Component.literal(fluid.getAmount() + " mB " + FL.name(fluid, true))));
            }
            if (i % columns == columns - 1) {
                tooltip.add(row);
                row = new ArrayList<>();
            }
        }
    }

    private static void appendSifter(ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
        Map<Integer, ItemStack> inventory = getInventory(accessor, data);
        addItem(tooltip, "item_input", inventory.get(0));
        addItems(tooltip, "item_output", inventory, 1, inventory.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1);
    }

    private static void addStateLine(ITooltip tooltip, String state) {
        int color = state.equals("state.running") ? 0x55FF55 : state.equals("state.idle") ? 0xAAAAAA : 0xFF5555;
        tooltip.add(Component.literal(tr("state") + ": ").append(Component.literal(tr(state)).withColor(color)));
    }

    private static void appendMaterials(ITooltip tooltip, CompoundTag materials) {
        StringBuilder row = new StringBuilder();
        int first = 0;
        for (int i = 0; i < 32; i++) {
            String slot = Integer.toString(i);
            if (!materials.contains(slot)) break;
            CompoundTag materialTag = materials.getCompoundOrEmpty(slot);
            OreDictMaterial material = OreDictMaterial.get(materialTag.getShortOr("i", (short) 0));
            if (material != null) {
                long amount = materialTag.getLongOr("a", 0L) / 648648;
                if (row.isEmpty()) first = i + 1;
                else row.append(" | ");
                row.append(String.format(Locale.ROOT, "%.3f %s", amount / 1000.0,
                    material.getLocal()));
            }
            if (i % 4 == 3 || i == 31 || !materials.contains(Integer.toString(i + 1))) {
                if (!row.isEmpty()) tooltip.add(Component.literal(tr("materials") + " " + first + "–" + (i + 1) + ": " + row));
                row.setLength(0);
            }
        }
    }

    private static void addFluidSlot(ITooltip tooltip, String key, int slot, FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return;
        tooltip.add(Component.literal(tr(key) + " " + (slot + 1) + ": "));
        tooltip.append(JadeUI.fluid(JadeFluidObject.of(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch())));
        tooltip.append(Component.literal(fluid.getAmount() + " mB " + FL.name(fluid, true)));
    }

    private static void addFluidGauge(ITooltip tooltip, String key, FluidStack fluid, long capacity) {
        addProgressGauge(tooltip, key, amount(fluid), capacity, fluid);
    }

    private static void addStoredTank(ITooltip tooltip, String key, CompoundTag data) {
        CompoundTag tank = data.getCompoundOrEmpty("gt.tank");
        long amount = tank.getLongOr("LAmount", tank.getIntOr("Amount", 0));
        addProgressGauge(tooltip, key, amount, data.getLongOr("gtquality.capacity", 0L), getFluid(data, "gt.tank"));
    }

    private static void addProgressGauge(ITooltip tooltip, String key, long current, long max, FluidStack fluid) {
        addProgressGauge(tooltip, key, current, max, fluid, null);
    }

    private static void addProgressGauge(ITooltip tooltip, String key, long current, long max, FluidStack fluid, String override) {
        float ratio = GT6MachineComponentProvider.progressRatio(current, max);
        String value = override == null ? Math.max(0, current) + " / " + Math.max(0, max)
            + " (" + formatPercent(current * 100.0 / Math.max(1, max)) + ")" : override;
        ProgressView.Part part = fluid == null ? ProgressView.Part.of(ratio)
            : ProgressView.Part.of(ratio, JadeUI.fluid(JadeFluidObject.of(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch())));
        tooltip.add(JadeUI.progress(new ProgressView(part, Component.literal(tr(key) + ": " + value),
            JadeUI.progressStyle(), BoxStyle.nestedBox())));
    }

    private static void addLine(ITooltip tooltip, String key, String value) {
        tooltip.add(Component.literal(tr(key) + ": " + value));
    }

    private static void addItem(ITooltip tooltip, String key, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        tooltip.add(Component.literal(tr(key) + ": "));
        tooltip.append(JadeUI.smallItem(stack));
        tooltip.append(Component.literal(stack.getCount() + " × ").append(stack.getHoverName()));
    }

    private static void addItems(ITooltip tooltip, String key, Map<Integer, ItemStack> items, int from, int to) {
        for (int i = from; i < to; i++) addItem(tooltip, key, items.get(i));
    }

    private static Map<Integer, ItemStack> getInventory(BlockAccessor accessor, CompoundTag data) {
        Map<Integer, ItemStack> items = new HashMap<>();
        ListTag inventory = data.getListOrEmpty("gt.invlist");
        for (int i = 0; i < inventory.size(); i++) {
            CompoundTag item = inventory.getCompoundOrEmpty(i);
            if (!item.contains("stack")) continue;
            ItemStack stack = accessor.decodeFromNbt(ItemStack.OPTIONAL_STREAM_CODEC, item.get("stack")).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) items.put(item.getIntOr("s", 0), stack);
        }
        return items;
    }

    private static ItemStack getItem(CompoundTag data, String key) {
        return data.contains(key) ? ST.load(data.getCompoundOrEmpty(key)) : null;
    }

    private static FluidStack getFluid(CompoundTag data, String key) {
        return data.contains(key) ? FL.load(data.getCompoundOrEmpty(key)) : null;
    }

    private static long amount(FluidStack fluid) {
        return fluid == null ? 0 : Math.max(0, fluid.getAmount());
    }
}
