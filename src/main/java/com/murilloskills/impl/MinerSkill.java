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

import java.util.ArrayList;
import java.util.List;

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

            List<MinerScanResultPayload.OreEntry> ores = scanForOres(player);

            if (!ores.isEmpty()) {
                // Envia pacote para o cliente renderizar
                ServerPlayNetworking.send(player, new MinerScanResultPayload(ores));
                player.sendMessage(
                        Text.translatable("murilloskills.miner.instinct_activated").formatted(Formatting.GREEN), true);
                player.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM);

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

            // --- LEVEL 10: NIGHT VISION ---
            if (meetsLevelRequirement(level, SkillConfig.MINER_NIGHT_VISION_LEVEL)) {
                handleNightVision(player);
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
        int radius = SkillConfig.MINER_ABILITY_RADIUS;

        // Limit blocks scanned per invocation to prevent lag (5000 blocks max)
        int maxBlocksToScan = SkillConfig.getMinerScanLimit();
        int blocksScanned = 0;

        // Scan in a spiral pattern from center outward for better UX
        // (finds nearby ores first even if we hit the limit)
        for (int r = 0; r <= radius && blocksScanned < maxBlocksToScan; r++) {
            for (int dx = -r; dx <= r && blocksScanned < maxBlocksToScan; dx++) {
                for (int dy = -r; dy <= r && blocksScanned < maxBlocksToScan; dy++) {
                    for (int dz = -r; dz <= r && blocksScanned < maxBlocksToScan; dz++) {
                        // Only check blocks at the current radius shell
                        if (Math.abs(dx) == r || Math.abs(dy) == r || Math.abs(dz) == r) {
                            BlockPos pos = center.add(dx, dy, dz);
                            blocksScanned++;

                            net.minecraft.block.Block block = player.getEntityWorld().getBlockState(pos).getBlock();
                            var result = MinerXpGetter.isMinerXpBlock(block, false, true);
                            if (result.didGainXp()) {
                                MinerScanResultPayload.OreType oreType = getOreType(block);
                                ores.add(new MinerScanResultPayload.OreEntry(pos.toImmutable(), oreType));
                            }
                        }
                    }
                }
            }
        }

        if (blocksScanned >= maxBlocksToScan) {
            LOGGER.debug("Miner scan hit block limit ({} blocks scanned, {} ores found)",
                    blocksScanned, ores.size());
        }

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
}
