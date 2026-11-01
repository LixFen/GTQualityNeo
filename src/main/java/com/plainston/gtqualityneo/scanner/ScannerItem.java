package com.plainston.gtqualityneo.scanner;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.item.IItemEnergy;
import gregapi.item.multiitem.energy.EnergyStat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import java.util.Collection;

/** Separate modern item IDs; charge is delegated to GT6's EU implementation. */
public final class ScannerItem extends Item implements IItemEnergy {
    public final int tier;
    public ScannerItem(int tier, Properties properties) { super(properties); this.tier = tier; }
    public long voltage() { return gregapi.data.CS.V[tier]; }
    private IItemEnergy energy(ItemStack stack) {
        // A real empty template keeps an exhausted scanner from being consumed by EnergyStat.
        var empty = new ItemStack(this);
        return EnergyStat.makeTool(TD.Energy.EU, voltage() * 8000, voltage(), 1, empty);
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) ScannerIntegration.openScreen.run();
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            net.minecraft.world.item.component.TooltipDisplay display,
            java.util.function.Consumer<net.minecraft.network.chat.Component> lines,
            net.minecraft.world.item.TooltipFlag flag) {
        lines.accept(net.minecraft.network.chat.Component.translatable("gtqualityneo.scanner.tooltip"));
        lines.accept(net.minecraft.network.chat.Component.translatable("gtqualityneo.scanner.energy", getEnergyStored(TD.Energy.EU, stack), getEnergyCapacity(TD.Energy.EU, stack)));
        int range = ScanConfig.SPEC.isLoaded() ? ScanConfig.range(tier) : tier * 2 + 5;
        int multiplier = ScanConfig.SPEC.isLoaded() ? ScanConfig.CONSUMPTION.get() : 50;
        lines.accept(net.minecraft.network.chat.Component.translatable("gtqualityneo.scanner.range", range, range, voltage() * multiplier));
    }
    @Override public boolean isEnergyType(TagData aEnergyType, ItemStack aStack, boolean aEmitting) { return energy(aStack).isEnergyType(aEnergyType, aStack, aEmitting); }
    @Override public Collection<TagData> getEnergyTypes(ItemStack aStack) { return energy(aStack).getEnergyTypes(aStack); }
    @Override public long doEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, Container aInventory, Level aWorld, int aX, int aY, int aZ, boolean aDoInject) { return energy(aStack).doEnergyInjection(aEnergyType, aStack, aSize, aAmount, aInventory, aWorld, aX, aY, aZ, aDoInject); }
    @Override public boolean canEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize) { return energy(aStack).canEnergyInjection(aEnergyType, aStack, aSize); }
    @Override public long doEnergyExtraction(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, Container aInventory, Level aWorld, int aX, int aY, int aZ, boolean aDoExtract) { return energy(aStack).doEnergyExtraction(aEnergyType, aStack, aSize, aAmount, aInventory, aWorld, aX, aY, aZ, aDoExtract); }
    @Override public boolean canEnergyExtraction(TagData aEnergyType, ItemStack aStack, long aSize) { return energy(aStack).canEnergyExtraction(aEnergyType, aStack, aSize); }
    @Override public boolean useEnergy(TagData aEnergyType, ItemStack aStack, long aEnergyAmount, LivingEntity aPlayer, Container aInventory, Level aWorld, int aX, int aY, int aZ, boolean aDoUse) { return energy(aStack).useEnergy(aEnergyType, aStack, aEnergyAmount, aPlayer, aInventory, aWorld, aX, aY, aZ, aDoUse); }
    @Override public ItemStack setEnergyStored(TagData aEnergyType, ItemStack aStack, long aAmount) { return energy(aStack).setEnergyStored(aEnergyType, aStack, aAmount); }
    @Override public long getEnergyStored(TagData aEnergyType, ItemStack aStack) { return energy(aStack).getEnergyStored(aEnergyType, aStack); }
    @Override public long getEnergyCapacity(TagData aEnergyType, ItemStack aStack) { return energy(aStack).getEnergyCapacity(aEnergyType, aStack); }
    @Override public long getEnergySizeInputMin(TagData aEnergyType, ItemStack aStack) { return energy(aStack).getEnergySizeInputMin(aEnergyType, aStack); }
    @Override public long getEnergySizeOutputMin(TagData aEnergyType, ItemStack aStack) { return energy(aStack).getEnergySizeOutputMin(aEnergyType, aStack); }
    @Override public long getEnergySizeInputRecommended(TagData aEnergyType, ItemStack aStack) { return energy(aStack).getEnergySizeInputRecommended(aEnergyType, aStack); }
    @Override public long getEnergySizeOutputRecommended(TagData aEnergyType, ItemStack aStack) { return energy(aStack).getEnergySizeOutputRecommended(aEnergyType, aStack); }
    @Override public long getEnergySizeInputMax(TagData aEnergyType, ItemStack aStack) { return energy(aStack).getEnergySizeInputMax(aEnergyType, aStack); }
    @Override public long getEnergySizeOutputMax(TagData aEnergyType, ItemStack aStack) { return energy(aStack).getEnergySizeOutputMax(aEnergyType, aStack); }
}
