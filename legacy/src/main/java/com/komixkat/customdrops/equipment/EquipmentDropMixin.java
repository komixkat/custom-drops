package com.komixkat.customdrops.equipment;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.config.schema.EquipmentOverrideEntry;
import com.komixkat.customdrops.registry.IdentifierResolver;
import com.komixkat.customdrops.registry.TagResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class EquipmentDropMixin {

    @Inject(method = "setDropChance", at = @At("HEAD"), cancellable = true)
    private void customdrops$forceAlwaysDropIfConfigured(EquipmentSlot slot, float chance, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (self.level().isClientSide()) return;
        if (!CustomDropsMod.config().equipmentOverridesEnabled()) return;

        EquipmentOverrideEntry.EquipmentSlotGroup group = toGroup(slot);
        if (group == null) return;

        for (EquipmentOverrideEntry entry : CustomDropsMod.config().equipmentOverrides()) {
            if (entry.slot() != group) continue;
            if (matches(self, entry)) {
                self.setDropChance(slot, entry.dropChance());
                ci.cancel();
                return;
            }
        }
    }

    private static boolean matches(Mob self, EquipmentOverrideEntry entry) {
        if (entry.isTag()) {
            return TagResolver.resolveEntityTag(entry.targetEntityId())
                .map(self::is)
                .orElse(false);
        }
        return IdentifierResolver.resolve(entry.targetEntityId())
            .map(id -> id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(self.getType())))
            .orElse(false);
    }

    private static EquipmentOverrideEntry.EquipmentSlotGroup toGroup(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> EquipmentOverrideEntry.EquipmentSlotGroup.MAIN_HAND;
            case OFFHAND -> EquipmentOverrideEntry.EquipmentSlotGroup.OFF_HAND;
            case HEAD -> EquipmentOverrideEntry.EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentOverrideEntry.EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentOverrideEntry.EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentOverrideEntry.EquipmentSlotGroup.FEET;
            default -> null;
        };
    }
}
