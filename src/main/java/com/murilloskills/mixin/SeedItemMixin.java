package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.FarmerXpGetter;
import com.murilloskills.utils.SkillConfig;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for BlockItem (used by seeds) to implement 3x3 area planting.
 * Applies to wheat_seeds, beetroot_seeds, carrot, potato, nether_wart.
 */
@Mixin(BlockItem.class)
public class SeedItemMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void onUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();

        if (world.isClient() || player == null || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        ItemStack stack = context.getStack();
        Item seedItem = stack.getItem();

        // Check if this is a plantable seed we support
        Block cropToPlant = FarmerSkill.getCropForSeed(seedItem);
        if (cropToPlant == null) {
            return; // Not a seed we handle, let vanilla process it
        }

        // Check if player has Farmer skill selected and area planting enabled
        SkillGlobalState state = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
        var playerData = state.getPlayerData(serverPlayer);

        if (!playerData.isSkillSelected(MurilloSkillsList.FARMER)) {
            return; // Let vanilla handle it
        }

        var farmerStats = playerData.getSkill(MurilloSkillsList.FARMER);
        if (farmerStats.level < SkillConfig.FARMER_AREA_PLANTING_LEVEL) {
            return; // Level too low, let vanilla handle it
        }

        if (!FarmerSkill.isAreaPlantingEnabled(serverPlayer)) {
            return; // Toggle is off, let vanilla handle it
        }

        // Area planting is enabled - intercept and plant in 3x3
        BlockPos clickedPos = context.getBlockPos();
        int radius = SkillConfig.FARMER_AREA_PLANTING_RADIUS;
        int planted = 0;
        int seedsNeeded = 0;

        // First pass: count how many valid positions
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos basePos = clickedPos.add(dx, 0, dz);
                if (canPlantAt(world, basePos, cropToPlant, seedItem)) {
                    seedsNeeded++;
                }
            }
        }

        // Check if player has enough seeds
        if (seedsNeeded == 0) {
            return; // No valid positions, let vanilla try
        }

        int availableSeeds = stack.getCount();
        int seedsToUse = Math.min(seedsNeeded, availableSeeds);

        if (seedsToUse == 0) {
            return; // No seeds available
        }

        // Second pass: plant seeds
        for (int dx = -radius; dx <= radius && planted < seedsToUse; dx++) {
            for (int dz = -radius; dz <= radius && planted < seedsToUse; dz++) {
                BlockPos basePos = clickedPos.add(dx, 0, dz);
                BlockPos plantPos = basePos.up();

                if (canPlantAt(world, basePos, cropToPlant, seedItem)) {
                    // Plant the crop
                    world.setBlockState(plantPos, cropToPlant.getDefaultState());
                    planted++;

                    // Give XP for planting
                    SkillReceptorResult xpResult = FarmerXpGetter.getPlantingXp(cropToPlant);
                    if (xpResult.didGainXp()) {
                        playerData.addXpToSkill(MurilloSkillsList.FARMER, xpResult.getXpAmount());
                    }
                }
            }
        }

        if (planted > 0) {
            // Consume seeds
            stack.decrement(planted);

            // Play sound
            world.playSound(null, clickedPos, SoundEvents.ITEM_CROP_PLANT, SoundCategory.BLOCKS, 1.0f, 1.0f);

            // Track planting for daily challenges
            for (int i = 0; i < planted; i++) {
                com.murilloskills.events.ChallengeEventsHandler.onSeedsPlanted(serverPlayer);
            }

            // Mark state as dirty and sync
            state.markDirty();
            SkillsNetworkUtils.syncSkills(serverPlayer);

            // Cancel vanilla behavior
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    /**
     * Check if we can plant at this position
     */
    private static boolean canPlantAt(World world, BlockPos basePos, Block cropToPlant, Item seedItem) {
        BlockPos plantPos = basePos.up();
        BlockState baseState = world.getBlockState(basePos);
        BlockState plantState = world.getBlockState(plantPos);

        // Plant position must be air
        if (!plantState.isAir()) {
            return false;
        }

        // Check base block based on crop type
        if (seedItem == Items.NETHER_WART) {
            // Nether wart needs soul sand
            return baseState.isOf(Blocks.SOUL_SAND);
        } else {
            // Other crops need farmland
            return baseState.getBlock() instanceof FarmlandBlock || baseState.isOf(Blocks.FARMLAND);
        }
    }
}
