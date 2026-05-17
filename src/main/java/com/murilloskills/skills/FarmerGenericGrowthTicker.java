package com.murilloskills.skills;

import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.utils.CrossModMinecraftCompat;
import com.murilloskills.utils.MinecraftVersionCompat;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class FarmerGenericGrowthTicker {
    private static final int SAMPLES_PER_SECOND = 24;

    private FarmerGenericGrowthTicker() {
    }

    public static void tick(ServerPlayerEntity player, int farmerLevel) {
        if (farmerLevel < SkillConfig.getFarmerFertileGroundLevel() || player.age % 20 != 0) {
            return;
        }

        ServerWorld world = MinecraftVersionCompat.serverWorld(player);
        int radius = SkillConfig.getFarmerFertileGroundRadius();
        int verticalRadius = 4;
        int growth = rollGrowth(world, farmerLevel);
        if (growth <= 0) {
            return;
        }

        BlockPos origin = player.getBlockPos();
        for (int i = 0; i < SAMPLES_PER_SECOND; i++) {
            int dx = world.random.nextInt(radius * 2 + 1) - radius;
            int dy = world.random.nextInt(verticalRadius * 2 + 1) - verticalRadius;
            int dz = world.random.nextInt(radius * 2 + 1) - radius;
            BlockPos pos = origin.add(dx, dy, dz);
            BlockState current = world.getBlockState(pos);
            if (!CrossModMinecraftCompat.isPlantLike(current) || CrossModMinecraftCompat.isMaturePlant(current)) {
                continue;
            }

            BlockState grown = CrossModMinecraftCompat.growBy(current, growth);
            if (grown != current) {
                world.setBlockState(pos, grown, 2);
                if (world.getTime() % 10 == 0) {
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            2, 0.25, 0.2, 0.25, 0.0);
                }
            }
        }
    }

    private static int rollGrowth(ServerWorld world, int farmerLevel) {
        float growthBoost = FarmerSkill.getFertileGroundGrowthBoost(farmerLevel);
        int growth = (int) growthBoost;
        float fractional = growthBoost - growth;
        if (fractional > 0.0f && world.random.nextFloat() < fractional) {
            growth++;
        }
        return growth;
    }
}
