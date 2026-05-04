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
@Mixin(targets = "techreborn.blockentity.machine.iron.AbstractIronMachineBlockEntity", remap = false)
public abstract class TechRebornIronMachineBlockEntityMixin {
    @Shadow(remap = false)
    public int progress;

    @Shadow(remap = false)
    protected abstract int cookingTime();

    @Shadow(remap = false)
    public abstract boolean isBurning();

    @Inject(
            method = "tick(Lnet/minecraft/class_1937;Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;Lreborncore/common/blockentity/MachineBaseBlockEntity;)V",
            at = @At("TAIL"),
            remap = false)
    private void murilloSkills$applyBlacksmithIronMachineSpeed(
            World world,
            BlockPos pos,
            BlockState state,
            @Coerce Object blockEntity,
            CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        int totalTime = cookingTime();
        if (!isBurning() || progress <= 0 || totalTime <= 0 || progress >= totalTime) {
            return;
        }

        int bestLevel = BlacksmithMachineSpeedHelper.getBestNearbyBlacksmithLevel(serverWorld, pos);
        int extraTicks = BlacksmithMachineSpeedHelper.getExtraProgressTicks(serverWorld, bestLevel);
        if (extraTicks <= 0) {
            return;
        }

        progress = Math.min(progress + extraTicks, totalTime - 1);
        BlacksmithMachineSpeedHelper.spawnSpeedParticles(serverWorld, pos);
    }
}
