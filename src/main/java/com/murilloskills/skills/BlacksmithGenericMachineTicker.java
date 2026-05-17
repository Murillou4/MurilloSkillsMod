package com.murilloskills.skills;

import com.murilloskills.utils.BlacksmithMachineSpeedHelper;
import com.murilloskills.utils.MinecraftVersionCompat;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class BlacksmithGenericMachineTicker {
    private BlacksmithGenericMachineTicker() {
    }

    public static void tick(ServerPlayerEntity player, int blacksmithLevel) {
        if (blacksmithLevel <= 0 || player.age % 10 != 0) {
            return;
        }

        ServerWorld world = MinecraftVersionCompat.serverWorld(player);
        int radius = SkillConfig.getBlacksmithFurnaceSpeedRadius();
        BlockPos origin = player.getBlockPos();
        for (BlockPos mutable : BlockPos.iterate(origin.add(-radius, -radius, -radius),
                origin.add(radius, radius, radius))) {
            BlockPos pos = mutable.toImmutable();
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity != null) {
                BlacksmithMachineSpeedHelper.tryBoostGenericMachine(blockEntity, world, pos);
            }
        }
    }
}
