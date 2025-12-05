package com.murilloskills.skills;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.utils.FarmerXpGetter;
import com.murilloskills.utils.SkillConfig;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Handles crop harvesting events for the Farmer skill.
 * Integrates with PlayerBlockBreakEvents to give XP and apply bonuses.
 */
public class CropHarvestHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-Farmer");
    private static final Random random = new Random();

    /**
     * Main handler called when a block is broken.
     * Checks if it's a mature crop and gives XP + applies bonuses.
     */
    public static void handle(PlayerEntity player, World world, BlockPos pos, BlockState state) {
        if (world.isClient())
            return;

        Block block = state.getBlock();

        // Check if it's a crop block
        if (!FarmerXpGetter.isCropBlock(block)) {
            return;
        }

        // Check if crop is mature
        boolean isMature = isCropMature(state, block);
        SkillReceptorResult result = FarmerXpGetter.getCropHarvestXp(block, isMature);

        if (!result.didGainXp()) {
            return;
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        SkillGlobalState skillState = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
        var playerData = skillState.getPlayerData(serverPlayer);

        // Check if Farmer is a selected skill
        if (!playerData.isSkillSelected(MurilloSkillsList.FARMER)) {
            return;
        }

        var stats = playerData.getSkill(MurilloSkillsList.FARMER);
        int level = stats.level;

        // Add XP
        if (playerData.addXpToSkill(MurilloSkillsList.FARMER, result.getXpAmount())) {
            SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.FARMER, stats.level);
        }

        // Apply harvest bonuses
        applyHarvestBonuses(serverPlayer, (ServerWorld) world, pos, state, block, level);

        skillState.markDirty();
        SkillsNetworkUtils.syncSkills(serverPlayer);
    }

    /**
     * Checks if a crop is mature based on its block state.
     */
    private static boolean isCropMature(BlockState state, Block block) {
        if (block instanceof CropBlock cropBlock) {
            return cropBlock.isMature(state);
        }

        // Nether Wart
        if (block == Blocks.NETHER_WART) {
            int age = state.get(net.minecraft.block.NetherWartBlock.AGE);
            return age >= 3;
        }

        // Sweet Berries
        if (block == Blocks.SWEET_BERRY_BUSH) {
            int age = state.get(net.minecraft.block.SweetBerryBushBlock.AGE);
            return age >= 2;
        }

        // Cocoa
        if (block == Blocks.COCOA) {
            int age = state.get(net.minecraft.block.CocoaBlock.AGE);
            return age >= 2;
        }

        // Melon and Pumpkin are always "mature"
        if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            return true;
        }

        return false;
    }

    /**
     * Applies harvest bonuses based on farmer level.
     */
    private static void applyHarvestBonuses(ServerPlayerEntity player, ServerWorld world,
            BlockPos pos, BlockState state, Block block, int level) {
        // Calculate double harvest chance
        float doubleChance = FarmerSkill.getDoubleHarvestChance(level);

        // Roll for double harvest
        if (random.nextFloat() < doubleChance) {
            dropExtraCrop(world, pos, state, player);
            LOGGER.debug("Player {} got double harvest at {}", player.getName().getString(), pos);
        }

        // Level 50+: 5% chance for extra seeds
        if (level >= SkillConfig.FARMER_NUTRIENT_CYCLE_LEVEL) {
            if (random.nextFloat() < SkillConfig.FARMER_NUTRIENT_SEED_CHANCE) {
                dropExtraSeeds(world, pos, block);
            }
        }

        // Level 75+: 10% chance to harvest adjacent crops
        if (level >= SkillConfig.FARMER_ABUNDANT_HARVEST_LEVEL) {
            if (random.nextFloat() < SkillConfig.FARMER_ABUNDANT_ADJACENT) {
                harvestAdjacentCrops(player, world, pos);
            }
        }

        // Golden crop chance (gives saturation effect when eating)
        float goldenChance = FarmerSkill.getGoldenCropChance(level);
        if (random.nextFloat() < goldenChance) {
            applyGoldenCropBonus(player, block);
        }
    }

    /**
     * Drops extra crop items (for double harvest).
     */
    private static void dropExtraCrop(ServerWorld world, BlockPos pos, BlockState state, ServerPlayerEntity player) {
        java.util.List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player,
                player.getMainHandStack());

        for (ItemStack drop : drops) {
            ItemEntity itemEntity = new ItemEntity(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    drop.copy());
            world.spawnEntity(itemEntity);
        }

        // Play a subtle sound
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5f, 1.2f);
    }

    /**
     * Drops extra seeds for the crop.
     */
    private static void dropExtraSeeds(ServerWorld world, BlockPos pos, Block block) {
        ItemStack seeds = null;

        if (block == Blocks.WHEAT)
            seeds = new ItemStack(Items.WHEAT_SEEDS);
        else if (block == Blocks.BEETROOTS)
            seeds = new ItemStack(Items.BEETROOT_SEEDS);
        else if (block == Blocks.CARROTS)
            seeds = new ItemStack(Items.CARROT);
        else if (block == Blocks.POTATOES)
            seeds = new ItemStack(Items.POTATO);
        else if (block == Blocks.NETHER_WART)
            seeds = new ItemStack(Items.NETHER_WART);

        if (seeds != null) {
            ItemEntity itemEntity = new ItemEntity(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    seeds);
            world.spawnEntity(itemEntity);
        }
    }

    /**
     * Harvests adjacent crops (1 block radius).
     */
    private static void harvestAdjacentCrops(ServerPlayerEntity player, ServerWorld world, BlockPos center) {
        for (BlockPos adjacent : BlockPos.iterate(center.add(-1, 0, -1), center.add(1, 0, 1))) {
            if (adjacent.equals(center))
                continue;

            BlockState adjState = world.getBlockState(adjacent);
            Block adjBlock = adjState.getBlock();

            if (adjBlock instanceof CropBlock cropBlock) {
                if (cropBlock.isMature(adjState)) {
                    // Drop the items
                    java.util.List<ItemStack> drops = Block.getDroppedStacks(adjState, world, adjacent, null, player,
                            player.getMainHandStack());
                    for (ItemStack drop : drops) {
                        ItemEntity itemEntity = new ItemEntity(world,
                                adjacent.getX() + 0.5, adjacent.getY() + 0.5, adjacent.getZ() + 0.5,
                                drop.copy());
                        world.spawnEntity(itemEntity);
                    }

                    // Break the block without drops (we already dropped them)
                    world.breakBlock(adjacent, false, player);

                    LOGGER.debug("Adjacent harvest at {} for player {}", adjacent, player.getName().getString());
                    break; // Only harvest one adjacent crop per trigger
                }
            }
        }
    }

    /**
     * Applies golden crop bonus - grants saturation effect.
     * Uses vanilla saturation status effect for +50% nutrition effect.
     */
    private static void applyGoldenCropBonus(ServerPlayerEntity player, Block block) {
        // Give a brief saturation effect to simulate +50% nutrition
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SATURATION,
                60, // 3 seconds
                0, // Level 1
                false, false, true));

        // Notify player
        player.sendMessage(net.minecraft.text.Text.literal("✨ Colheita Dourada!")
                .formatted(net.minecraft.util.Formatting.GOLD), true);

        LOGGER.debug("Player {} got golden crop from {}", player.getName().getString(), block);
    }
}
