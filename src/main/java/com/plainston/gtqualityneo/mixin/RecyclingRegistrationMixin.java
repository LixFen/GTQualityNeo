// Compatibility: GT6w alpha.17 owns all recycling-set additions/removals in these two methods.
package com.plainston.gtqualityneo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.plainston.gtqualityneo.integration.loading.RecyclingRegistrationIndex;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.event.IOreDictListenerRecyclable.OreDictRecyclingContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

@Mixin(value = OreDictManager.class, remap = false)
public abstract class RecyclingRegistrationMixin {
    @Shadow @Final private Set<OreDictRecyclingContainer> mRecyclableRegistrations;
    @Unique private RecyclingRegistrationIndex gtqualityneo$recycling;

    @Unique private RecyclingRegistrationIndex gtqualityneo$recyclingIndex() {
        if (gtqualityneo$recycling == null) gtqualityneo$recycling = new RecyclingRegistrationIndex(mRecyclableRegistrations);
        return gtqualityneo$recycling;
    }

    @WrapOperation(method = "setItemData_", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean gtqualityneo$addRegistration(Set<OreDictRecyclingContainer> registrations, Object value, Operation<Boolean> original) {
        var index = gtqualityneo$recyclingIndex();
        boolean added = original.call(registrations, value);
        if (added) index.add((OreDictRecyclingContainer) value);
        return added;
    }

    @WrapOperation(method = "removeItemData_", at = @At(value = "INVOKE", target = "Ljava/util/Set;remove(Ljava/lang/Object;)Z"))
    private boolean gtqualityneo$removeRegistration(Set<OreDictRecyclingContainer> registrations, Object value, Operation<Boolean> original) {
        var index = gtqualityneo$recyclingIndex();
        boolean removed = original.call(registrations, value);
        if (removed) index.remove((OreDictRecyclingContainer) value);
        return removed;
    }

    @WrapOperation(method = "addAssociation_", at = @At(value = "NEW", target = "(Ljava/util/Collection;)Ljava/util/ArrayList;"))
    private ArrayList<OreDictRecyclingContainer> gtqualityneo$matchingStack(Collection<OreDictRecyclingContainer> registrations,
        Operation<ArrayList<OreDictRecyclingContainer>> original, @Local(argsOnly = true) ItemStack stack) {
        // The original loop still checks prefix, exact item and exact subtype, then retracts the original recipes.
        return gtqualityneo$recyclingIndex().forStack(stack);
    }

    @WrapOperation(method = "removeItemData_", at = @At(value = "NEW", target = "(Ljava/util/Collection;)Ljava/util/ArrayList;"))
    private ArrayList<OreDictRecyclingContainer> gtqualityneo$matchingData(Collection<OreDictRecyclingContainer> registrations,
        Operation<ArrayList<OreDictRecyclingContainer>> original, @Local(argsOnly = true) OreDictItemData itemData) {
        // Identity selects candidates; GT6 retains its wildcard/NBT matching and listener replay behavior.
        return gtqualityneo$recyclingIndex().forData(itemData);
    }
}
