package com.murilloskills.mixin;

import com.murilloskills.utils.BlacksmithMachineSpeedHelper;
import net.minecraft.block.BlockState;
import net.minecraft.recipe.SmeltingRecipe;
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
@Mixin(targets = "techreborn.blockentity.machine.tier1.ElectricFurnaceBlockEntity", remap = false)
public abstract class TechRebornElectricFurnaceBlockEntityMixin {
    @Shadow(remap = false)
    private SmeltingRecipe currentRecipe;

    @Shadow(remap = false)
    private int cookTime;

    @Shadow(remap = false)
    private int cookTimeTotal;

    @Shadow(remap = false)
    public abstract long getStored();

    @Shadow(remap = false)
    public abstract long getEuPerTick(long ticks);

    @Inject(
            method = "tick(Lnet/minecraft/class_1937;Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;Lreborncore/common/blockentity/MachineBaseBlockEntity;)V",
            at = @At("TAIL"),
            remap = false)
    private void murilloSkills$applyBlacksmithElectricFurnaceSpeed(
            World world,
            BlockPos pos,
            BlockState state,
            @Coerce Object blockEntity,
            CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (currentRecipe == null || cookTime <= 0 || cookTimeTotal <= 0 || cookTime >= cookTimeTotal) {
            return;
        }

        if (getStored() <= getEuPerTick(1L)) {
            return;
        }

        int bestLevel = BlacksmithMachineSpeedHelper.getBestNearbyBlacksmithLevel(serverWorld, pos);
        int extraTicks = BlacksmithMachineSpeedHelper.getExtraProgressTicks(serverWorld, bestLevel);
        if (extraTicks <= 0) {
            return;
        }

        cookTime = Math.min(cookTime + extraTicks, cookTimeTotal - 1);
        BlacksmithMachineSpeedHelper.spawnSpeedParticles(serverWorld, pos);
    }
}
