package com.murilloskills.mixin;

import com.murilloskills.utils.BlacksmithMachineSpeedHelper;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "reborncore.common.blockentity.MachineBaseBlockEntity", remap = false)
public abstract class RebornCoreMachineBaseBlockEntityMixin {
    @Shadow(remap = false)
    public abstract void addSpeedMultiplier(double speedMultiplier);

    @Inject(
            method = "tick(Lnet/minecraft/class_1937;Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;Lreborncore/common/blockentity/MachineBaseBlockEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lreborncore/common/blockentity/MachineBaseBlockEntity;afterUpgradesApplication()V",
                    shift = At.Shift.AFTER,
                    remap = false),
            remap = false)
    private void murilloSkills$applyBlacksmithMachineSpeed(
            World world,
            BlockPos pos,
            BlockState state,
            @Coerce Object blockEntity,
            CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        int bestLevel = BlacksmithMachineSpeedHelper.getBestNearbyBlacksmithLevel(serverWorld, pos);
        double speedBonus = BlacksmithMachineSpeedHelper.getRebornCoreSpeedBonus(bestLevel);
        if (speedBonus <= 0.0D) {
            return;
        }

        addSpeedMultiplier(speedBonus);
        BlacksmithMachineSpeedHelper.spawnSpeedParticles(serverWorld, pos);
    }
}
