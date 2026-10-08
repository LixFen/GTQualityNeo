package com.plainston.gtqualityneo.qol;

import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.data.CS.ArmorsGT;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class UniversalHazmat {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Identifier BONUS = Identifier.fromNamespaceAndPath(GTQualityNeo.MOD_ID, "universal_hazmat");
    private UniversalHazmat() {}
    public static boolean isUniversal(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (var item : ArmorsGT.HAZMAT_UNIVERSAL) if (stack.getItem() == item) return true;
        return false;
    }
    public static boolean enhanced() {
        return GTQualityNeo.CONFIG.isLoaded() && GTQualityNeo.UNIVERSAL_HAZMAT.get();
    }
    public static void components(net.neoforged.neoforge.event.ModifyDefaultComponentsEvent event) {
        event.modifyMatching((item, components) -> {
            for (var armor : ArmorsGT.HAZMAT_UNIVERSAL) if (item == armor) return true;
            return false;
        }, (components, context, item) -> {
            if (enhanced()) components.set(net.minecraft.core.component.DataComponents.MAX_DAMAGE,
                components.getOrDefault(net.minecraft.core.component.DataComponents.MAX_DAMAGE, 128) * 4);
        });
    }
    public static boolean fullSet(LivingEntity wearer) {
        for (int i = 0; i < SLOTS.length; i++)
            if (ArmorsGT.HAZMAT_UNIVERSAL[i] == null || !wearer.getItemBySlot(SLOTS[i]).is(ArmorsGT.HAZMAT_UNIVERSAL[i])) return false;
        return true;
    }
    public static void update(LivingEntity wearer) {
        if (wearer.level().isClientSide()) return;
        var armor = wearer.getAttribute(Attributes.ARMOR);
        if (armor == null) return;
        if (enhanced() && fullSet(wearer)) {
            // GT6's four pieces already supply one armor point each.
            if (!armor.hasModifier(BONUS)) armor.addTransientModifier(new AttributeModifier(BONUS, 16, AttributeModifier.Operation.ADD_VALUE));
        } else armor.removeModifier(BONUS);
    }
    public static void tick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity wearer) update(wearer);
    }
    public static void equipmentChanged(LivingEquipmentChangeEvent event) { update(event.getEntity()); }
}
