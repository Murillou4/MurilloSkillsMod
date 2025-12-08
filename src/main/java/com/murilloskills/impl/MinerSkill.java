package com.murilloskills.impl;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.data.SkillGlobalState;
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
    public void onActiveAbility(ServerPlayerEntity player, SkillGlobalState.SkillStats stats) {
        try {
            // 1. Verifica Nível
            if (stats.level < SkillConfig.MINER_MASTER_LEVEL) {
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
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            state.markDirty();

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
            double speedBonus = level * SkillConfig.MINER_SPEED_PER_LEVEL;
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

            LOGGER.debug("Updated miner attributes for {} - Speed bonus: {}",
                    player.getName().getString(), speedBonus);
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
     * color-coded display
     */
    private List<MinerScanResultPayload.OreEntry> scanForOres(ServerPlayerEntity player) {
        List<MinerScanResultPayload.OreEntry> ores = new ArrayList<>();
        BlockPos center = player.getBlockPos();
        int radius = SkillConfig.MINER_ABILITY_RADIUS;

        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius),
                center.add(radius, radius, radius))) {
            net.minecraft.block.Block block = player.getEntityWorld().getBlockState(pos).getBlock();
            var result = MinerXpGetter.isMinerXpBlock(block, false, true);
            if (result.didGainXp()) {
                MinerScanResultPayload.OreType oreType = getOreType(block);
                ores.add(new MinerScanResultPayload.OreEntry(pos.toImmutable(), oreType));
            }
        }

        return ores;
    }

    /**
     * Determines the ore type from a block for color-coded highlighting
     */
    private MinerScanResultPayload.OreType getOreType(net.minecraft.block.Block block) {
        if (block == net.minecraft.block.Blocks.COAL_ORE || block == net.minecraft.block.Blocks.DEEPSLATE_COAL_ORE) {
            return MinerScanResultPayload.OreType.COAL;
        } else if (block == net.minecraft.block.Blocks.COPPER_ORE
                || block == net.minecraft.block.Blocks.DEEPSLATE_COPPER_ORE) {
            return MinerScanResultPayload.OreType.COPPER;
        } else if (block == net.minecraft.block.Blocks.IRON_ORE
                || block == net.minecraft.block.Blocks.DEEPSLATE_IRON_ORE) {
            return MinerScanResultPayload.OreType.IRON;
        } else if (block == net.minecraft.block.Blocks.GOLD_ORE
                || block == net.minecraft.block.Blocks.DEEPSLATE_GOLD_ORE) {
            return MinerScanResultPayload.OreType.GOLD;
        } else if (block == net.minecraft.block.Blocks.LAPIS_ORE
                || block == net.minecraft.block.Blocks.DEEPSLATE_LAPIS_ORE) {
            return MinerScanResultPayload.OreType.LAPIS;
        } else if (block == net.minecraft.block.Blocks.REDSTONE_ORE
                || block == net.minecraft.block.Blocks.DEEPSLATE_REDSTONE_ORE) {
            return MinerScanResultPayload.OreType.REDSTONE;
        } else if (block == net.minecraft.block.Blocks.DIAMOND_ORE
                || block == net.minecraft.block.Blocks.DEEPSLATE_DIAMOND_ORE) {
            return MinerScanResultPayload.OreType.DIAMOND;
        } else if (block == net.minecraft.block.Blocks.EMERALD_ORE
                || block == net.minecraft.block.Blocks.DEEPSLATE_EMERALD_ORE) {
            return MinerScanResultPayload.OreType.EMERALD;
        } else if (block == net.minecraft.block.Blocks.ANCIENT_DEBRIS) {
            return MinerScanResultPayload.OreType.ANCIENT_DEBRIS;
        } else if (block == net.minecraft.block.Blocks.NETHER_QUARTZ_ORE) {
            return MinerScanResultPayload.OreType.NETHER_QUARTZ;
        } else if (block == net.minecraft.block.Blocks.NETHER_GOLD_ORE) {
            return MinerScanResultPayload.OreType.NETHER_GOLD;
        }
        return MinerScanResultPayload.OreType.OTHER;
    }
}
