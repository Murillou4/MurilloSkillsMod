package com.murilloskills.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents Explorer speed modifiers from affecting FOV.
 * Temporarily removes our movement speed attribute modifiers during FOV calculation
 * so the player doesn't get jarring FOV changes from Explorer passives.
 */
@Mixin(AbstractClientPlayerEntity.class)
public class ExplorerFovMixin {

    @Unique
    private static final Identifier EXPLORER_SPEED_ID = Identifier.of("murilloskills", "explorer_speed_bonus");
    @Unique
    private static final Identifier EXPLORER_PATHFINDER_SPEED_ID = Identifier.of("murilloskills", "explorer_pathfinder_speed");

    @Unique
    private EntityAttributeModifier murilloskills$savedSpeedMod;
    @Unique
    private EntityAttributeModifier murilloskills$savedPathfinderMod;

    @Inject(method = "getFovMultiplier", at = @At("HEAD"))
    private void murilloskills$beforeFovCalc(boolean firstPerson, float fovEffectScale, CallbackInfoReturnable<Float> cir) {
        var self = (AbstractClientPlayerEntity) (Object) this;
        var speedAttr = self.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            murilloskills$savedSpeedMod = speedAttr.getModifier(EXPLORER_SPEED_ID);
            murilloskills$savedPathfinderMod = speedAttr.getModifier(EXPLORER_PATHFINDER_SPEED_ID);
            if (murilloskills$savedSpeedMod != null) speedAttr.removeModifier(EXPLORER_SPEED_ID);
            if (murilloskills$savedPathfinderMod != null) speedAttr.removeModifier(EXPLORER_PATHFINDER_SPEED_ID);
        }
    }

    @Inject(method = "getFovMultiplier", at = @At("RETURN"))
    private void murilloskills$afterFovCalc(boolean firstPerson, float fovEffectScale, CallbackInfoReturnable<Float> cir) {
        var self = (AbstractClientPlayerEntity) (Object) this;
        var speedAttr = self.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            if (murilloskills$savedSpeedMod != null) {
                speedAttr.addTemporaryModifier(murilloskills$savedSpeedMod);
                murilloskills$savedSpeedMod = null;
            }
            if (murilloskills$savedPathfinderMod != null) {
                speedAttr.addTemporaryModifier(murilloskills$savedPathfinderMod);
                murilloskills$savedPathfinderMod = null;
            }
        }
    }
}
