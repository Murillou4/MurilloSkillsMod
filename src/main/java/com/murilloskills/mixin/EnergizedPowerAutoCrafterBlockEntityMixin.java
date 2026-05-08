package com.murilloskills.mixin;

import com.murilloskills.utils.BlacksmithMachineSpeedHelper;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.jddev0.ep.block.entity.AutoCrafterBlockEntity", remap = false)
public abstract class EnergizedPowerAutoCrafterBlockEntityMixin {
    @Inject(
            method = "tick(Lnet/minecraft/class_1937;Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;Lme/jddev0/ep/block/entity/AutoCrafterBlockEntity;)V",
            at = @At("TAIL"),
            remap = false)
    private static void murilloSkills$applyBlacksmithAutoCrafterSpeed(
            World world,
            BlockPos pos,
            BlockState state,
            @Coerce Object blockEntity,
            CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        BlacksmithMachineSpeedHelper.tryBoostEnergizedPowerWorker(blockEntity, serverWorld, pos);
    }
}
