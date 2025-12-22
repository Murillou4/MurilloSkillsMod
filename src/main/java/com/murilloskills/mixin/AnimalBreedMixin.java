package com.murilloskills.mixin;

import com.murilloskills.events.ChallengeEventsHandler;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to track when players breed animals for daily challenges.
 * Targets AnimalEntity.breed method which is called when animals successfully
 * breed.
 */
@Mixin(AnimalEntity.class)
public abstract class AnimalBreedMixin {

    /**
     * Track when animals are bred by a player.
     */
    @Inject(method = "breed(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;)V", at = @At("HEAD"))
    private void onBreed(ServerWorld world, AnimalEntity other, CallbackInfo ci) {
        AnimalEntity self = (AnimalEntity) (Object) this;

        // Get the player who caused the breeding (the one who fed this animal)
        if (self.getLovingPlayer() instanceof ServerPlayerEntity serverPlayer) {
            ChallengeEventsHandler.onAnimalBred(serverPlayer);

            // Track animal breeding for Animal Breeder achievement
            com.murilloskills.utils.AchievementTracker.incrementAndCheck(
                    serverPlayer, com.murilloskills.skills.MurilloSkillsList.FARMER,
                    com.murilloskills.utils.AchievementTracker.KEY_ANIMALS_BRED, 1);
        }
    }
}
