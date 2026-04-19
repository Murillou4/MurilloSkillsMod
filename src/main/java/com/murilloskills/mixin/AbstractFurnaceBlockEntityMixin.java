package com.murilloskills.mixin;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithXpGetter;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin for AbstractFurnaceBlockEntity to grant Blacksmith XP when smelting
 * ores and to speed up furnaces when a Blacksmith player is nearby.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Shadow
    int cookingTimeSpent;

    @Shadow
    int cookingTotalTime;

    @Shadow
    int litTimeRemaining;

    /**
     * Validates when a smelting operation completes.
     * Actual XP granting is handled in FurnaceOutputMixin when player takes output.
     */
    @Inject(method = "craftRecipe", at = @At("HEAD"))
    private static void onSmeltComplete(
            DynamicRegistryManager registryManager,
            RecipeEntry<?> recipe,
            SingleStackRecipeInput input,
            DefaultedList<ItemStack> slots,
            int count,
            CallbackInfoReturnable<Boolean> cir) {

        ItemStack inputStack = input.getStackInSlot(0);
        if (inputStack.isEmpty()) {
            return;
        }

        BlacksmithXpGetter.getSmeltingXp(inputStack.getItem());
    }

    /**
     * Blacksmith Furnace Mastery: speeds up nearby furnaces based on Blacksmith level.
     * At level 100, furnaces cook up to 4.5x faster.
     * Shows flame particles on the furnace as a visual indicator.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private static void onFurnaceTick(ServerWorld world, BlockPos pos, BlockState state,
            AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        AbstractFurnaceBlockEntityMixin self = (AbstractFurnaceBlockEntityMixin) (Object) blockEntity;

        // Only boost if furnace is actively cooking (has fuel and progress)
        if (self.litTimeRemaining <= 0 || self.cookingTimeSpent <= 0 || self.cookingTimeSpent >= self.cookingTotalTime)
            return;

        int radius = SkillConfig.getBlacksmithFurnaceSpeedRadius();
        Box searchBox = new Box(pos).expand(radius);
        List<ServerPlayerEntity> nearbyPlayers = world.getEntitiesByClass(
                ServerPlayerEntity.class, searchBox, p -> true);

        int bestLevel = 0;
        for (ServerPlayerEntity player : nearbyPlayers) {
            PlayerSkillData data = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
            if (data.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
                int level = data.getSkill(MurilloSkillsList.BLACKSMITH).level;
                bestLevel = Math.max(bestLevel, level);
            }
        }

        if (bestLevel <= 0)
            return;

        // Calculate extra cook ticks: scales from 0 at level 0 to (maxMultiplier-1) at level 100
        float maxExtra = SkillConfig.getBlacksmithFurnaceSpeedMaxMultiplier() - 1.0f;
        float extraFloat = (bestLevel / 100.0f) * maxExtra;
        int extraTicks = (int) extraFloat;

        // Probabilistic rounding for fractional part (smooth scaling)
        float fractional = extraFloat - extraTicks;
        if (fractional > 0 && world.random.nextFloat() < fractional) {
            extraTicks++;
        }

        if (extraTicks <= 0)
            return;

        // Add extra cook time, capped at cookingTotalTime - 1 to let normal crafting logic handle completion
        self.cookingTimeSpent = Math.min(self.cookingTimeSpent + extraTicks, self.cookingTotalTime - 1);

        // Visual indicator: spawn flame particles on the furnace every 10 ticks (0.5s)
        if (world.getTime() % 10 == 0) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5;
            world.spawnParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    x, y, z,
                    2, // count
                    0.2, 0.1, 0.2, // spread
                    0.01 // speed
            );
        }
    }
}
