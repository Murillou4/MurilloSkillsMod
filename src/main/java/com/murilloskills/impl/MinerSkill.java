package com.murilloskills.impl;

import com.murilloskills.api.AbstractSkill;

import com.murilloskills.network.MinerScanResultPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.MinerXpGetter;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Miner skill implementation with all miner-specific logic.
 * Features:
 * - Level 10: Night Vision in underground areas
 * - Level 60: Ore Radar (sound alerts for nearby valuable ores)
 * - Level 100: Master Miner ability (reveals all ores in large radius)
 * - Passive: Mining speed bonus per level
 */
public class MinerSkill extends AbstractSkill {

    private static final Identifier MINER_SPEED_ID = Identifier.of("murilloskills", "miner_speed_bonus");
    private static final Map<UUID, Long> masterVisionActiveUntil = new HashMap<>();

    // Toggle key name for persistent storage
    private static final String TOGGLE_AUTO_TORCH = "autoTorch";

    @Override
    public MurilloSkillsList getSkillType() {
        return MurilloSkillsList.MINER;
    }

    @Override
    public void onActiveAbility(ServerPlayerEntity player, com.murilloskills.data.PlayerSkillData.SkillStats stats) {
        try {
            // 1. Verifica Nível
            // 1. Verifica Nível (permite se level >= 100 OU se já prestigiou)
            boolean hasReachedMaster = stats.level >= SkillConfig.MINER_MASTER_LEVEL || stats.prestige > 0;
            if (!hasReachedMaster) {
                player.sendMessage(Text.translatable("murilloskills.error.level_required", 100,
                        Text.translatable("murilloskills.skill.name.miner")).formatted(Formatting.RED), true);
                return;
            }

            // 2. Verifica Cooldown (pula se nunca usou: lastAbilityUse == -1)
            long worldTime = player.getEntityWorld().getTime();
            long timeSinceUse = worldTime - stats.lastAbilityUse;

            long cooldownTicks = SkillConfig.toTicksLong(SkillConfig.MINER_ABILITY_COOLDOWN_SECONDS);
            if (stats.lastAbilityUse >= 0 && timeSinceUse < cooldownTicks) {
                long minutesLeft = (cooldownTicks - timeSinceUse) / 20 / 60;
                player.sendMessage(Text.translatable("murilloskills.error.cooldown_minutes", minutesLeft)
                        .formatted(Formatting.RED), true);
                return;
            }

            // 3. Executa Habilidade
            stats.lastAbilityUse = worldTime;
            // Stats are automatically synced via Data Attachments API
            // No need to force immediate save here

            int durationTicks = SkillConfig.toTicks(SkillConfig.getMinerAbilityDurationSeconds());
            masterVisionActiveUntil.put(player.getUuid(), worldTime + durationTicks);

            List<MinerScanResultPayload.OreEntry> ores = scanForOres(player);

            // Envia pacote para o cliente renderizar, mesmo se a lista vier vazia.
            ServerPlayNetworking.send(player, new MinerScanResultPayload(ores, durationTicks));

            player.sendMessage(
                    Text.translatable("murilloskills.miner.instinct_activated").formatted(Formatting.GREEN), true);
            player.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM);

            if (!ores.isEmpty()) {
                LOGGER.info("Player {} activated Miner Master ability at position {} - {} ores found",
                        player.getName().getString(), player.getBlockPos(), ores.size());
            } else {
                player.sendMessage(Text.translatable("murilloskills.miner.no_ores_found").formatted(Formatting.YELLOW),
                        true);
            }

        } catch (Exception e) {
            LOGGER.error("Error executing Miner active ability for " + player.getName().getString(), e);
            player.sendMessage(Text.translatable("murilloskills.error.ability_error").formatted(Formatting.RED), false);
        }
    }

    @Override
    public void onTick(ServerPlayerEntity player, int level) {
        try {
            if (player.age % 20 != 0)
                return; // Executa apenas 1 vez por segundo para otimização

            syncActiveMasterVision(player);

            // --- LEVEL 10: NIGHT VISION ---
            if (meetsLevelRequirement(level, SkillConfig.MINER_NIGHT_VISION_LEVEL)) {
                handleNightVision(player);
            }

            // --- LEVEL 25: AUTO-TORCH ---
            if (meetsLevelRequirement(level, SkillConfig.MINER_AUTO_TORCH_LEVEL)) {
                if (player.age % SkillConfig.MINER_AUTO_TORCH_INTERVAL_TICKS == 0 && isAutoTorchEnabled(player)) {
                    handleAutoTorch(player);
                }
            }

            // --- LEVEL 60: RADAR ---
            if (meetsLevelRequirement(level, SkillConfig.MINER_RADAR_LEVEL)) {
                handleRadar(player);
            }

        } catch (Exception e) {
            // Log with limited frequency to avoid spamming console on tick errors
            if (player.age % 200 == 0) {
                LOGGER.error("Erro no tick do Minerador para " + player.getName().getString(), e);
            }
        }
    }

    private void syncActiveMasterVision(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        Long endTime = masterVisionActiveUntil.get(playerUuid);
        if (endTime == null) {
            return;
        }

        long worldTime = player.getEntityWorld().getTime();
        int remainingTicks = (int) Math.max(0L, endTime - worldTime);
        if (remainingTicks <= 0) {
            masterVisionActiveUntil.remove(playerUuid);
            ServerPlayNetworking.send(player, new MinerScanResultPayload(List.of(), 0));
            return;
        }

        List<MinerScanResultPayload.OreEntry> ores = scanForOres(player);
        ServerPlayNetworking.send(player, new MinerScanResultPayload(ores, remainingTicks));
    }

    @Override
    public void updateAttributes(ServerPlayerEntity player, int level) {
        try {
            // Get prestige level for passive multiplier
            var data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
            int prestige = data.getSkill(MurilloSkillsList.MINER).prestige;
            float prestigeMultiplier = com.murilloskills.utils.PrestigeManager.getPassiveMultiplier(prestige);

            // Apply prestige bonus to mining speed
            double speedBonus = level * SkillConfig.MINER_SPEED_PER_LEVEL * prestigeMultiplier;
            var attributeInstance = player.getAttributeInstance(EntityAttributes.BLOCK_BREAK_SPEED);

            if (attributeInstance != null) {
                attributeInstance.removeModifier(MINER_SPEED_ID);
                if (speedBonus > 0) {
                    attributeInstance.addTemporaryModifier(new EntityAttributeModifier(
                            MINER_SPEED_ID,
                            speedBonus,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }

            LOGGER.debug("Updated miner attributes for {} - Speed bonus: {} (prestige: {})",
                    player.getName().getString(), speedBonus, prestige);
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar atributos do Minerador para " + player.getName().getString(), e);
        }
    }

    /**
     * Handles night vision effect for underground areas
     */
    private void handleNightVision(ServerPlayerEntity player) {
        // Aplica apenas se estiver no subsolo (Y < 55) e em local escuro
        // Não aplica se estiver com o céu visível (ex: noite na superfície) para não
        // atrapalhar
        if (player.getY() < 55 && !player.getEntityWorld().isSkyVisible(player.getBlockPos())) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, false, false, true));
        }
    }

    /**
     * Handles ore radar functionality
     */
    private void handleRadar(ServerPlayerEntity player) {
        // A cada 2 segundos (checagem dentro do tick de 1s, usamos age % 40)
        if (player.age % 40 != 0)
            return;

        BlockPos playerPos = player.getBlockPos();
        int radius = 5;
        boolean foundRareOre = false;

        // Escaneia área pequena
        for (BlockPos pos : BlockPos.iterate(playerPos.add(-radius, -radius, -radius),
                playerPos.add(radius, radius, radius))) {
            var result = MinerXpGetter.isMinerXpBlock(player.getEntityWorld().getBlockState(pos).getBlock(), false,
                    true);

            // Só apita para minérios valiosos (XP > 5)
            if (result.didGainXp() && result.getXpAmount() > 5) {
                foundRareOre = true;
                break;
            }
        }

        if (foundRareOre) {
            // Toca um som sutil "Geiger Counter"
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value());
        }
    }

    /**
     * Scans for ores in a large radius around the player, identifying ore types for
     * color-coded display.
     * Uses optimized scanning with early exit to prevent lag spikes.
     */
    private List<MinerScanResultPayload.OreEntry> scanForOres(ServerPlayerEntity player) {
        List<MinerScanResultPayload.OreEntry> ores = new ArrayList<>();
        BlockPos center = player.getBlockPos();
        int radius = SkillConfig.getMinerAbilityRadius();

        // Full volume scan — iterate every block in the cube
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    net.minecraft.block.Block block = player.getEntityWorld().getBlockState(pos).getBlock();
                    var result = MinerXpGetter.isMinerXpBlock(block, false, true);
                    if (result.didGainXp()) {
                        MinerScanResultPayload.OreType oreType = getOreType(block);
                        ores.add(new MinerScanResultPayload.OreEntry(pos.toImmutable(), oreType));
                    }
                }
            }
        }

        LOGGER.debug("Miner scan completed: {} ores found in radius {}", ores.size(), radius);
        return ores;
    }

    // Map instead of long if-else chain for O(1) lookup and cleaner code
    private static final java.util.Map<net.minecraft.block.Block, MinerScanResultPayload.OreType> ORE_MAP = new java.util.HashMap<>();

    static {
        ORE_MAP.put(net.minecraft.block.Blocks.COAL_ORE, MinerScanResultPayload.OreType.COAL);
        ORE_MAP.put(net.minecraft.block.Blocks.DEEPSLATE_COAL_ORE, MinerScanResultPayload.OreType.COAL);
        ORE_MAP.put(net.minecraft.block.Blocks.COPPER_ORE, MinerScanResultPayload.OreType.COPPER);
        ORE_MAP.put(net.minecraft.block.Blocks.DEEPSLATE_COPPER_ORE, MinerScanResultPayload.OreType.COPPER);
        ORE_MAP.put(net.minecraft.block.Blocks.IRON_ORE, MinerScanResultPayload.OreType.IRON);
        ORE_MAP.put(net.minecraft.block.Blocks.DEEPSLATE_IRON_ORE, MinerScanResultPayload.OreType.IRON);
        ORE_MAP.put(net.minecraft.block.Blocks.GOLD_ORE, MinerScanResultPayload.OreType.GOLD);
        ORE_MAP.put(net.minecraft.block.Blocks.DEEPSLATE_GOLD_ORE, MinerScanResultPayload.OreType.GOLD);
        ORE_MAP.put(net.minecraft.block.Blocks.LAPIS_ORE, MinerScanResultPayload.OreType.LAPIS);
        ORE_MAP.put(net.minecraft.block.Blocks.DEEPSLATE_LAPIS_ORE, MinerScanResultPayload.OreType.LAPIS);
        ORE_MAP.put(net.minecraft.block.Blocks.REDSTONE_ORE, MinerScanResultPayload.OreType.REDSTONE);
        ORE_MAP.put(net.minecraft.block.Blocks.DEEPSLATE_REDSTONE_ORE, MinerScanResultPayload.OreType.REDSTONE);
        ORE_MAP.put(net.minecraft.block.Blocks.DIAMOND_ORE, MinerScanResultPayload.OreType.DIAMOND);
        ORE_MAP.put(net.minecraft.block.Blocks.DEEPSLATE_DIAMOND_ORE, MinerScanResultPayload.OreType.DIAMOND);
        ORE_MAP.put(net.minecraft.block.Blocks.EMERALD_ORE, MinerScanResultPayload.OreType.EMERALD);
        ORE_MAP.put(net.minecraft.block.Blocks.DEEPSLATE_EMERALD_ORE, MinerScanResultPayload.OreType.EMERALD);
        ORE_MAP.put(net.minecraft.block.Blocks.ANCIENT_DEBRIS, MinerScanResultPayload.OreType.ANCIENT_DEBRIS);
        ORE_MAP.put(net.minecraft.block.Blocks.NETHER_QUARTZ_ORE, MinerScanResultPayload.OreType.NETHER_QUARTZ);
        ORE_MAP.put(net.minecraft.block.Blocks.NETHER_GOLD_ORE, MinerScanResultPayload.OreType.NETHER_GOLD);
    }

    /**
     * Determines the ore type from a block for color-coded highlighting
     */
    private MinerScanResultPayload.OreType getOreType(net.minecraft.block.Block block) {
        return ORE_MAP.getOrDefault(block, MinerScanResultPayload.OreType.OTHER);
    }

    // =====================================================
    // AUTO-TORCH SYSTEM (Level 25+)
    // =====================================================

    /**
     * Handles auto-torch placement when the player is in a dark area.
     * Places torches optimally to prevent mob spawning.
     */
    private void handleAutoTorch(ServerPlayerEntity player) {
        net.minecraft.server.world.ServerWorld world = player.getEntityWorld();
        BlockPos playerPos = player.getBlockPos();

        // Only in overworld-like dimensions (skip Nether ceiling, etc.)
        // Mobs spawn at block light level 0 since 1.18

        // Check if area around player is dark enough to warrant a torch
        int blockLight = world.getLightLevel(net.minecraft.world.LightType.BLOCK, playerPos);
        if (blockLight > SkillConfig.MINER_AUTO_TORCH_LIGHT_THRESHOLD) {
            return; // Already well-lit
        }

        // Find torch in inventory
        int torchSlot = findTorchSlot(player);
        if (torchSlot == -1) {
            return; // No torches available
        }

        // Find the best position to place a torch
        BlockPos torchPos = findBestTorchPosition(world, playerPos);
        if (torchPos == null) {
            return; // No valid position found
        }

        // Place the torch
        net.minecraft.block.BlockState torchState = net.minecraft.block.Blocks.TORCH.getDefaultState();
        if (world.getBlockState(torchPos).isAir() && torchState.canPlaceAt(world, torchPos)) {
            world.setBlockState(torchPos, torchState, net.minecraft.block.Block.NOTIFY_ALL);

            // Consume torch from inventory
            player.getInventory().getStack(torchSlot).decrement(1);

            // Play placement sound
            world.playSound(null, torchPos,
                    net.minecraft.sound.SoundEvents.BLOCK_WOOD_PLACE,
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.5f, 1.0f);
        }
    }

    /**
     * Finds a torch or soul torch in the player's inventory.
     * Returns the slot index, or -1 if none found.
     */
    private int findTorchSlot(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            net.minecraft.item.Item item = player.getInventory().getStack(i).getItem();
            if (item == net.minecraft.item.Items.TORCH || item == net.minecraft.item.Items.SOUL_TORCH) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the optimal position to place a torch near the player.
     * Prioritizes the darkest nearby position that would have the most impact
     * on preventing mob spawning (light level 0 blocks).
     */
    private BlockPos findBestTorchPosition(net.minecraft.server.world.ServerWorld world, BlockPos center) {
        // Search in a small radius around the player for the best spot
        BlockPos bestPos = null;
        int bestScore = -1;

        // Check positions in a 3-block radius, preferring closer positions
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos candidate = center.add(dx, dy, dz);

                    // Must be air and have a solid block below
                    if (!world.getBlockState(candidate).isAir()) continue;
                    if (!world.getBlockState(candidate.down()).isSolidBlock(world, candidate.down())) continue;

                    // Check if torch can be placed here
                    if (!net.minecraft.block.Blocks.TORCH.getDefaultState().canPlaceAt(world, candidate)) continue;

                    // Score = number of dark blocks (light <= 0) in a 7-block radius that would be lit
                    // Simple heuristic: prefer darker areas closer to the player
                    int currentLight = world.getLightLevel(net.minecraft.world.LightType.BLOCK, candidate);
                    int distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    int score = (14 - currentLight) * 10 - distance; // Darker = better, closer = better

                    if (score > bestScore) {
                        bestScore = score;
                        bestPos = candidate.toImmutable();
                    }
                }
            }
        }

        return bestPos;
    }

    /**
     * Toggles auto-torch for the player (persistent across death/logout).
     * Returns the new state.
     */
    public static boolean toggleAutoTorch(ServerPlayerEntity player) {
        var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        boolean currentlyEnabled = playerData.getToggle(MurilloSkillsList.MINER, TOGGLE_AUTO_TORCH, false);
        boolean newState = !currentlyEnabled;
        playerData.setToggle(MurilloSkillsList.MINER, TOGGLE_AUTO_TORCH, newState);
        return newState;
    }

    /**
     * Checks if auto-torch is enabled for the player (persistent storage).
     */
    public static boolean isAutoTorchEnabled(ServerPlayerEntity player) {
        try {
            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
            return playerData.getToggle(MurilloSkillsList.MINER, TOGGLE_AUTO_TORCH, false); // Default: disabled
        } catch (Exception e) {
            return false;
        }
    }

    public static void cleanupPlayerState(UUID playerUuid) {
        masterVisionActiveUntil.remove(playerUuid);
    }
}
