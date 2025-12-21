package com.murilloskills.impl;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builder skill implementation with focus on construction and architecture.
 * Features:
 * - Passive: +0.05 block reach per level (max +5 at level 100)
 * - Level 10: Extended Reach (+1 block)
 * - Level 15: Efficient Crafting (20% material economy for decorative blocks)
 * - Level 25: Safe Landing (25% fall damage reduction)
 * - Level 50: Scaffold Master (faster scaffolding + 50% economy structural)
 * - Level 75: Master Reach (+5 blocks cumulative)
 * - Level 100: Creative Brush (active ability - WorldEdit style area fill)
 */
public class BuilderSkill extends AbstractSkill {

    private static final Identifier BUILDER_REACH_ID = Identifier.of("murilloskills", "builder_reach");

    // Map to track players with Creative Brush active (UUID → start timestamp)
    private static final Map<UUID, Long> creativeBrushPlayers = new HashMap<>();

    // Map to track first corner position for area fill
    private static final Map<UUID, BlockPos> firstCornerPos = new HashMap<>();

    // Map to track remaining time when Creative Brush is paused (UUID → remaining
    // ticks)
    private static final Map<UUID, Long> pausedRemainingTime = new HashMap<>();

    // Map to track hollow mode preference (UUID → hollow enabled)
    private static final Map<UUID, Boolean> hollowModeEnabled = new HashMap<>();

    // Map to track fill mode preference (UUID → mode)
    private static final Map<UUID, BuilderFillMode> fillModePreference = new HashMap<>();

    // Map to track cylinder orientation (UUID → is horizontal)
    private static final Map<UUID, Boolean> cylinderHorizontal = new HashMap<>();

    /**
     * Toggle hollow mode for a player
     * 
     * @return true if now hollow, false if now filled
     */
    public static boolean toggleHollowMode(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean currentlyHollow = hollowModeEnabled.getOrDefault(uuid, false);
        hollowModeEnabled.put(uuid, !currentlyHollow);
        return !currentlyHollow;
    }

    /**
     * Check if a player has hollow mode enabled
     */
    public static boolean isHollowModeEnabled(ServerPlayerEntity player) {
        return hollowModeEnabled.getOrDefault(player.getUuid(), false);
    }

    /**
     * Get the current fill mode for a player
     */
    public static BuilderFillMode getFillMode(ServerPlayerEntity player) {
        return fillModePreference.getOrDefault(player.getUuid(), BuilderFillMode.CUBOID);
    }

    /**
     * Set the fill mode for a player
     */
    public static void setFillMode(ServerPlayerEntity player, BuilderFillMode mode) {
        fillModePreference.put(player.getUuid(), mode);
    }

    /**
     * Cycle to the next fill mode
     * 
     * @return the new mode
     */
    public static BuilderFillMode cycleNextFillMode(ServerPlayerEntity player) {
        BuilderFillMode currentMode = getFillMode(player);
        BuilderFillMode newMode = currentMode.next();
        setFillMode(player, newMode);
        return newMode;
    }

    /**
     * Toggle cylinder orientation between vertical and horizontal
     * 
     * @return true if now horizontal, false if now vertical
     */
    public static boolean toggleCylinderOrientation(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean currentlyHorizontal = cylinderHorizontal.getOrDefault(uuid, false);
        cylinderHorizontal.put(uuid, !currentlyHorizontal);
        return !currentlyHorizontal;
    }

    /**
     * Check if cylinder orientation is horizontal
     */
    public static boolean isCylinderHorizontal(ServerPlayerEntity player) {
        return cylinderHorizontal.getOrDefault(player.getUuid(), false);
    }

    @Override
    public MurilloSkillsList getSkillType() {
        return MurilloSkillsList.BUILDER;
    }

    @Override
    public void onActiveAbility(ServerPlayerEntity player, SkillGlobalState.SkillStats stats) {
        try {
            // 1. Verifica Nível (permite se level >= 100 OU se já prestigiou)
            boolean hasReachedMaster = stats.level >= SkillConfig.BUILDER_MASTER_LEVEL || stats.prestige > 0;
            if (!hasReachedMaster) {
                player.sendMessage(Text.translatable("murilloskills.error.level_required", 100,
                        Text.translatable("murilloskills.skill.name.builder")).formatted(Formatting.RED), true);
                return;
            }

            UUID uuid = player.getUuid();

            // If active, pause it and save remaining time
            if (isCreativeBrushActive(player)) {
                long startTime = creativeBrushPlayers.get(uuid);
                long currentTime = player.getEntityWorld().getTime();
                long elapsed = currentTime - startTime;
                long durationTicks = SkillConfig.toTicks(SkillConfig.BUILDER_ABILITY_DURATION_SECONDS);
                long remaining = durationTicks - elapsed;

                if (remaining > 0) {
                    pausedRemainingTime.put(uuid, remaining);
                    long remainingSeconds = remaining / 20;
                    player.sendMessage(
                            Text.translatable("murilloskills.builder.creative_brush_paused", remainingSeconds)
                                    .formatted(Formatting.YELLOW),
                            true);
                }

                endCreativeBrush(player);
                return;
            }

            // Check if there's paused time available
            if (pausedRemainingTime.containsKey(uuid) && pausedRemainingTime.get(uuid) > 0) {
                long remaining = pausedRemainingTime.get(uuid);
                resumeCreativeBrush(player, remaining);
                pausedRemainingTime.remove(uuid);
                long remainingSeconds = remaining / 20;
                player.sendMessage(Text.translatable("murilloskills.builder.creative_brush_resumed", remainingSeconds)
                        .formatted(Formatting.GREEN), true);
                return;
            }

            // Normal activation - check cooldown
            long worldTime = player.getEntityWorld().getTime();
            long timeSinceUse = worldTime - stats.lastAbilityUse;

            long cooldownTicks = SkillConfig.toTicksLong(SkillConfig.BUILDER_ABILITY_COOLDOWN_SECONDS);
            if (stats.lastAbilityUse >= 0 && timeSinceUse < cooldownTicks) {
                long minutesLeft = (cooldownTicks - timeSinceUse) / 20 / 60;
                player.sendMessage(Text.translatable("murilloskills.error.cooldown_minutes", minutesLeft)
                        .formatted(Formatting.RED), true);
                return;
            }

            stats.lastAbilityUse = worldTime;
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            state.markDirty();

            // Clear any old paused time
            pausedRemainingTime.remove(uuid);

            startCreativeBrush(player);

            LOGGER.info("Player {} ativou Creative Brush", player.getName().getString());

        } catch (Exception e) {
            LOGGER.error("Error executing Builder active ability for " + player.getName().getString(), e);
            player.sendMessage(Text.translatable("murilloskills.error.ability_error").formatted(Formatting.RED), false);
        }
    }

    @Override
    public void onTick(ServerPlayerEntity player, int level) {
        try {
            if (player.age % 20 != 0)
                return;

            if (creativeBrushPlayers.containsKey(player.getUuid())) {
                long startTime = creativeBrushPlayers.get(player.getUuid());
                long currentTime = player.getEntityWorld().getTime();
                long elapsed = currentTime - startTime;

                if (elapsed >= SkillConfig.toTicks(SkillConfig.BUILDER_ABILITY_DURATION_SECONDS)) {
                    endCreativeBrush(player);
                }
            }

        } catch (Exception e) {
            if (player.age % 200 == 0) {
                LOGGER.error("Erro no tick do Construtor para " + player.getName().getString(), e);
            }
        }
    }

    @Override
    public void updateAttributes(ServerPlayerEntity player, int level) {
        try {
            // Get prestige level for passive multiplier
            var state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            int prestige = state.getPlayerData(player).getSkill(MurilloSkillsList.BUILDER).prestige;

            double reachBonus = getReachBonus(level, prestige);

            LOGGER.info("Applying Builder reach for {} - Level: {}, Prestige: {}, Reach Bonus: {}",
                    player.getName().getString(), level, prestige, reachBonus);

            var reachAttr = player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
            if (reachAttr != null) {
                reachAttr.removeModifier(BUILDER_REACH_ID);
                if (reachBonus > 0) {
                    reachAttr.addTemporaryModifier(new EntityAttributeModifier(
                            BUILDER_REACH_ID, reachBonus,
                            EntityAttributeModifier.Operation.ADD_VALUE));
                    LOGGER.info("Added reach modifier - New range: {}", reachAttr.getValue());
                }
            } else {
                LOGGER.warn("BLOCK_INTERACTION_RANGE attribute is null for {}", player.getName().getString());
            }

        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar atributos do Construtor para " + player.getName().getString(), e);
        }
    }

    /**
     * Calculates the reach bonus based on level
     * 
     * @param level    The player's builder level
     * @param prestige The player's prestige level for passive bonus
     */
    public static double getReachBonus(int level, int prestige) {
        // Get prestige passive multiplier (+2% per prestige level)
        float prestigeMultiplier = com.murilloskills.utils.PrestigeManager.getPassiveMultiplier(prestige);

        double bonus = level * SkillConfig.BUILDER_REACH_PER_LEVEL * prestigeMultiplier;

        if (level >= SkillConfig.BUILDER_EXTENDED_REACH_LEVEL) {
            bonus += SkillConfig.BUILDER_LEVEL_10_REACH * prestigeMultiplier;
        }

        if (level >= SkillConfig.BUILDER_MASTER_REACH_LEVEL) {
            bonus += SkillConfig.BUILDER_LEVEL_75_REACH * prestigeMultiplier;
        }

        return bonus;
    }

    public static boolean isCreativeBrushActive(ServerPlayerEntity player) {
        if (!creativeBrushPlayers.containsKey(player.getUuid())) {
            return false;
        }

        long startTime = creativeBrushPlayers.get(player.getUuid());
        long currentTime = player.getEntityWorld().getTime();
        long elapsed = currentTime - startTime;

        return elapsed < SkillConfig.toTicks(SkillConfig.BUILDER_ABILITY_DURATION_SECONDS);
    }

    public static boolean isCreativeBrushActive(UUID playerUuid) {
        return creativeBrushPlayers.containsKey(playerUuid);
    }

    private void startCreativeBrush(ServerPlayerEntity player) {
        creativeBrushPlayers.put(player.getUuid(), player.getEntityWorld().getTime());
        firstCornerPos.remove(player.getUuid());

        player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        player.sendMessage(
                Text.translatable("murilloskills.builder.creative_brush_activated").formatted(Formatting.AQUA,
                        Formatting.BOLD),
                false);
        player.sendMessage(
                Text.translatable("murilloskills.builder.creative_brush_instructions")
                        .formatted(Formatting.DARK_AQUA),
                false);
    }

    /**
     * Resume Creative Brush with remaining time
     */
    private void resumeCreativeBrush(ServerPlayerEntity player, long remainingTicks) {
        // Calculate a fake start time that gives us the remaining duration
        long currentTime = player.getEntityWorld().getTime();
        long durationTicks = SkillConfig.toTicks(SkillConfig.BUILDER_ABILITY_DURATION_SECONDS);
        long fakeStartTime = currentTime - (durationTicks - remainingTicks);

        creativeBrushPlayers.put(player.getUuid(), fakeStartTime);
        // Keep firstCornerPos if it exists

        player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);
    }

    private void endCreativeBrush(ServerPlayerEntity player) {
        creativeBrushPlayers.remove(player.getUuid());
        firstCornerPos.remove(player.getUuid());

        player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.2f);
        player.sendMessage(Text.translatable("murilloskills.builder.creative_brush_ended").formatted(Formatting.GRAY),
                true);

        LOGGER.debug("Player {} saiu do Creative Brush", player.getName().getString());
    }

    /**
     * Handle block placement during Creative Brush - WorldEdit style area fill.
     * First placement marks corner 1, second placement fills the entire area.
     */
    public static int handleCreativeBrushPlacement(ServerPlayerEntity player, ServerWorld world, BlockPos pos,
            Block block) {
        if (!isCreativeBrushActive(player)) {
            return 0;
        }

        UUID uuid = player.getUuid();
        BlockPos firstCorner = firstCornerPos.get(uuid);

        // First placement - mark corner 1
        if (firstCorner == null) {
            firstCornerPos.put(uuid, pos);
            player.sendMessage(
                    Text.translatable("murilloskills.builder.corner_marked", pos.getX(), pos.getY(), pos.getZ())
                            .formatted(Formatting.AQUA),
                    true);
            player.sendMessage(Text.translatable("murilloskills.builder.place_second")
                    .formatted(Formatting.YELLOW), false);
            return 0;
        }

        // Second placement - fill the entire area
        firstCornerPos.remove(uuid);

        int minX = Math.min(firstCorner.getX(), pos.getX());
        int minY = Math.min(firstCorner.getY(), pos.getY());
        int minZ = Math.min(firstCorner.getZ(), pos.getZ());
        int maxX = Math.max(firstCorner.getX(), pos.getX());
        int maxY = Math.max(firstCorner.getY(), pos.getY());
        int maxZ = Math.max(firstCorner.getZ(), pos.getZ());

        int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);

        int maxBlocks = 1000;
        if (totalBlocks > maxBlocks) {
            player.sendMessage(
                    Text.translatable("murilloskills.builder.area_too_large", maxBlocks, totalBlocks)
                            .formatted(Formatting.RED),
                    true);
            return 0;
        }

        int blocksPlaced = 0;
        ItemStack heldStack = player.getMainHandStack();

        if (!(heldStack.getItem() instanceof BlockItem blockItem)) {
            return 0;
        }

        boolean hollow = isHollowModeEnabled(player);
        BuilderFillMode fillMode = getFillMode(player);
        boolean isHorizontalCylinder = isCylinderHorizontal(player);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos fillPos = new BlockPos(x, y, z);

                    if (fillPos.equals(firstCorner)) {
                        continue;
                    }

                    // Check if this position should have a block based on the selected shape
                    if (!shouldPlaceBlock(fillMode, hollow, fillPos, minX, minY, minZ, maxX, maxY, maxZ,
                            isHorizontalCylinder)) {
                        continue; // Skip this position
                    }

                    int slot = findBlockInInventory(player, blockItem.getBlock());
                    if (slot == -1) {
                        player.sendMessage(Text.translatable("murilloskills.builder.not_enough_blocks", blocksPlaced)
                                .formatted(Formatting.YELLOW), true);
                        return blocksPlaced;
                    }

                    BlockState currentState = world.getBlockState(fillPos);
                    if (currentState.isAir() || currentState.isReplaceable()) {
                        world.setBlockState(fillPos, block.getDefaultState());
                        player.getInventory().getStack(slot).decrement(1);
                        blocksPlaced++;
                    }
                }
            }
        }

        if (blocksPlaced > 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.0f, 1.5f);
            player.sendMessage(Text.translatable("murilloskills.builder.area_filled", blocksPlaced)
                    .formatted(Formatting.GREEN, Formatting.BOLD), true);
        }

        return blocksPlaced;
    }

    /**
     * Determines if a block should be placed at the given position based on the
     * fill mode.
     * Uses mathematical formulas to create different geometric shapes.
     *
     * @param mode                 The fill mode (shape type)
     * @param hollow               Whether to only fill the outer shell
     * @param pos                  Current position being tested
     * @param minX,                minY, minZ Minimum coordinates of bounding box
     * @param maxX,                maxY, maxZ Maximum coordinates of bounding box
     * @param isHorizontalCylinder Whether cylinder is horizontal (only used for
     *                             CYLINDER mode)
     * @return true if block should be placed at this position
     */
    private static boolean shouldPlaceBlock(BuilderFillMode mode, boolean hollow,
            BlockPos pos, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            boolean isHorizontalCylinder) {

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;

        switch (mode) {
            case CUBOID -> {
                // Standard rectangular box - current behavior
                if (hollow) {
                    boolean isEdge = (x == minX || x == maxX ||
                            y == minY || y == maxY ||
                            z == minZ || z == maxZ);
                    return isEdge;
                }
                return true; // Fill all blocks in cuboid
            }

            case SPHERE -> {
                // Ellipsoid formula: (x-cx)²/rx² + (y-cy)²/ry² + (z-cz)²/rz² ≤ 1
                double radiusX = (maxX - minX + 1) / 2.0;
                double radiusY = (maxY - minY + 1) / 2.0;
                double radiusZ = (maxZ - minZ + 1) / 2.0;

                double dx = (x - centerX) / radiusX;
                double dy = (y - centerY) / radiusY;
                double dz = (z - centerZ) / radiusZ;

                double distanceSquared = dx * dx + dy * dy + dz * dz;

                if (hollow) {
                    // Shell thickness: between 85% and 100% of radius
                    return distanceSquared <= 1.0 && distanceSquared >= 0.72; // 0.85²
                }
                return distanceSquared <= 1.0;
            }

            case CYLINDER -> {
                if (isHorizontalCylinder) {
                    // Horizontal cylinder (along X axis)
                    // Circle in YZ plane
                    double radiusY = (maxY - minY + 1) / 2.0;
                    double radiusZ = (maxZ - minZ + 1) / 2.0;

                    double dy = (y - centerY) / radiusY;
                    double dz = (z - centerZ) / radiusZ;

                    double distanceSquared = dy * dy + dz * dz;

                    if (hollow) {
                        return distanceSquared <= 1.0 && distanceSquared >= 0.72;
                    }
                    return distanceSquared <= 1.0;
                } else {
                    // Vertical cylinder (along Y axis) - default
                    // Circle in XZ plane
                    double radiusX = (maxX - minX + 1) / 2.0;
                    double radiusZ = (maxZ - minZ + 1) / 2.0;

                    double dx = (x - centerX) / radiusX;
                    double dz = (z - centerZ) / radiusZ;

                    double distanceSquared = dx * dx + dz * dz;

                    if (hollow) {
                        return distanceSquared <= 1.0 && distanceSquared >= 0.72;
                    }
                    return distanceSquared <= 1.0;
                }
            }

            case PYRAMID -> {
                // Pyramid: base at minY, tip at maxY
                // At each height level, reduce the allowed radius
                int height = maxY - minY + 1;
                int currentHeight = y - minY;

                // Calculate how much the pyramid should shrink at this height
                // At base (y=minY): full size, at tip (y=maxY): 1 block
                double ratio = 1.0 - (currentHeight / (double) height);

                double allowedRadiusX = ((maxX - minX + 1) / 2.0) * ratio;
                double allowedRadiusZ = ((maxZ - minZ + 1) / 2.0) * ratio;

                double dx = Math.abs(x - centerX);
                double dz = Math.abs(z - centerZ);

                boolean insidePyramid = dx <= allowedRadiusX && dz <= allowedRadiusZ;

                if (hollow && insidePyramid) {
                    // For hollow pyramids, only place blocks on the outer edge at this level
                    boolean isOuterEdge = dx >= allowedRadiusX - 1 || dz >= allowedRadiusZ - 1;
                    return isOuterEdge || y == maxY; // Always fill the tip
                }

                return insidePyramid;
            }

            case WALL -> {
                // Only vertical walls (XZ planes), no floor or ceiling
                // This is different from hollow cuboid which includes floor/ceiling
                boolean isVerticalEdge = (x == minX || x == maxX || z == minZ || z == maxZ);

                if (hollow) {
                    // Hollow walls: only the outer perimeter (corners)
                    boolean isCorner = (x == minX || x == maxX) && (z == minZ || z == maxZ);
                    return isCorner && isVerticalEdge;
                }

                return isVerticalEdge;
            }

            default -> {
                return false;
            }
        }
    }

    private static int findBlockInInventory(ServerPlayerEntity player, Block block) {

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == block && stack.getCount() > 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static boolean shouldReduceFallDamage(int level) {
        return level >= SkillConfig.BUILDER_SAFE_LANDING_LEVEL;
    }

    public static float getFallDamageMultiplier(int level) {
        if (shouldReduceFallDamage(level)) {
            return 1.0f - SkillConfig.BUILDER_FALL_DAMAGE_REDUCTION;
        }
        return 1.0f;
    }

    public static boolean hasScaffoldSpeedBoost(int level) {
        return level >= SkillConfig.BUILDER_SCAFFOLD_MASTER_LEVEL;
    }

    public static float getScaffoldSpeedMultiplier(int level) {
        if (hasScaffoldSpeedBoost(level)) {
            return SkillConfig.BUILDER_SCAFFOLD_SPEED_MULTIPLIER;
        }
        return 1.0f;
    }

    public static boolean hasDecorativeEconomy(int level) {
        return level >= SkillConfig.BUILDER_EFFICIENT_CRAFTING_LEVEL;
    }

    public static boolean hasStructuralEconomy(int level) {
        return level >= SkillConfig.BUILDER_SCAFFOLD_MASTER_LEVEL;
    }

    public static float getCraftingEconomyChance(int level, boolean isStructural) {
        if (isStructural && hasStructuralEconomy(level)) {
            return SkillConfig.BUILDER_STRUCTURAL_ECONOMY;
        }
        if (!isStructural && hasDecorativeEconomy(level)) {
            return SkillConfig.BUILDER_DECORATIVE_ECONOMY;
        }
        return 0f;
    }
}
