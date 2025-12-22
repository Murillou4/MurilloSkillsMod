package com.murilloskills.utils;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

/**
 * Sistema de Sinergias entre Skills.
 * Quando o jogador seleciona certas combinações de skills, recebe bônus
 * adicionais.
 * 
 * Sinergias disponíveis:
 * - Warrior + Blacksmith = "Iron Will" (+10% damage reduction)
 * - Miner + Blacksmith = "Forge Master" (+15% ore drops)
 * - Archer + Explorer = "Ranger" (+20% speed while aiming)
 * - Farmer + Fisher = "Nature's Bounty" (+10% all drops)
 * - Miner + Explorer = "Treasure Hunter" (+25% rare ore chance)
 * - Warrior + Archer = "Combat Master" (+10% all damage)
 * - Builder + Blacksmith = "Master Crafter" (+20% crafting efficiency)
 */
public class SkillSynergyManager {

    // Definição de sinergias
    // Definição de sinergias (Dynamic to support config reload)
    public static List<SkillSynergy> getAll() {
        return List.of(
                new SkillSynergy("iron_will",
                        Set.of(MurilloSkillsList.WARRIOR, MurilloSkillsList.BLACKSMITH),
                        SkillConfig.getSynergyIronWill(), SynergyType.DAMAGE_REDUCTION),

                new SkillSynergy("forge_master",
                        Set.of(MurilloSkillsList.MINER, MurilloSkillsList.BLACKSMITH),
                        SkillConfig.getSynergyForgeMaster(), SynergyType.ORE_DROPS),

                new SkillSynergy("ranger",
                        Set.of(MurilloSkillsList.ARCHER, MurilloSkillsList.EXPLORER),
                        SkillConfig.getSynergyRanger(), SynergyType.MOVEMENT_SPEED),

                new SkillSynergy("natures_bounty",
                        Set.of(MurilloSkillsList.FARMER, MurilloSkillsList.FISHER),
                        SkillConfig.getSynergyNaturesBounty(), SynergyType.ALL_DROPS),

                new SkillSynergy("treasure_hunter",
                        Set.of(MurilloSkillsList.MINER, MurilloSkillsList.EXPLORER),
                        SkillConfig.getSynergyTreasureHunter(), SynergyType.RARE_FINDS),

                new SkillSynergy("combat_master",
                        Set.of(MurilloSkillsList.WARRIOR, MurilloSkillsList.ARCHER),
                        SkillConfig.getSynergyCombatMaster(), SynergyType.ALL_DAMAGE),

                new SkillSynergy("master_crafter",
                        Set.of(MurilloSkillsList.BUILDER, MurilloSkillsList.BLACKSMITH),
                        SkillConfig.getSynergyMasterCrafter(), SynergyType.CRAFTING_EFFICIENCY),

                // === NEW SYNERGIES ===
                new SkillSynergy("survivor",
                        Set.of(MurilloSkillsList.WARRIOR, MurilloSkillsList.EXPLORER),
                        SkillConfig.getSynergySurvivor(), SynergyType.DAMAGE_REDUCTION),

                new SkillSynergy("industrial",
                        Set.of(MurilloSkillsList.MINER, MurilloSkillsList.BUILDER),
                        SkillConfig.getSynergyIndustrial(), SynergyType.CRAFTING_EFFICIENCY),

                new SkillSynergy("sea_warrior",
                        Set.of(MurilloSkillsList.WARRIOR, MurilloSkillsList.FISHER),
                        SkillConfig.getSynergySeaWarrior(), SynergyType.ALL_DAMAGE),

                new SkillSynergy("green_archer",
                        Set.of(MurilloSkillsList.FARMER, MurilloSkillsList.ARCHER),
                        SkillConfig.getSynergyGreenArcher(), SynergyType.MOVEMENT_SPEED),

                new SkillSynergy("prospector",
                        Set.of(MurilloSkillsList.MINER, MurilloSkillsList.WARRIOR),
                        SkillConfig.getSynergyProspector(), SynergyType.ORE_DROPS),

                new SkillSynergy("adventurer",
                        Set.of(MurilloSkillsList.BUILDER, MurilloSkillsList.EXPLORER),
                        SkillConfig.getSynergyAdventurer(), SynergyType.ALL_DROPS),

                new SkillSynergy("hermit",
                        Set.of(MurilloSkillsList.FARMER, MurilloSkillsList.BUILDER),
                        SkillConfig.getSynergyHermit(), SynergyType.CRAFTING_EFFICIENCY));
    }

    /**
     * Obtém todas as sinergias ativas para um jogador.
     */
    public static List<SkillSynergy> getActiveSynergies(ServerPlayerEntity player) {
        PlayerSkillData data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

        Set<MurilloSkillsList> selectedSkills = new HashSet<>(data.getSelectedSkills());
        List<SkillSynergy> activeSynergies = new ArrayList<>();

        for (SkillSynergy synergy : getAll()) {
            if (selectedSkills.containsAll(synergy.requiredSkills())) {
                activeSynergies.add(synergy);
            }
        }

        return activeSynergies;
    }

    /**
     * Verifica se uma sinergia específica está ativa para o jogador.
     */
    public static boolean hasSynergy(ServerPlayerEntity player, String synergyId) {
        return getActiveSynergies(player).stream()
                .anyMatch(s -> s.id().equals(synergyId));
    }

    /**
     * Obtém o bônus total para um tipo de sinergia.
     * Soma todos os bônus de sinergias ativas desse tipo.
     */
    public static float getTotalBonus(ServerPlayerEntity player, SynergyType type) {
        float totalBonus = 0f;

        for (SkillSynergy synergy : getActiveSynergies(player)) {
            if (synergy.type() == type) {
                totalBonus += synergy.bonusMultiplier();
            }
        }

        return totalBonus;
    }

    /**
     * Aplica o multiplicador de sinergia a um valor base.
     */
    public static float applyBonus(float baseValue, ServerPlayerEntity player, SynergyType type) {
        float bonus = getTotalBonus(player, type);
        return baseValue * (1.0f + bonus);
    }

    /**
     * Gera lista de textos para exibir sinergias ativas na GUI.
     */
    public static List<Text> getSynergyTooltipLines(ServerPlayerEntity player) {
        List<SkillSynergy> activeSynergies = getActiveSynergies(player);
        List<Text> lines = new ArrayList<>();

        if (activeSynergies.isEmpty()) {
            lines.add(Text.translatable("murilloskills.synergy.none").formatted(Formatting.GRAY));
            return lines;
        }

        lines.add(Text.translatable("murilloskills.synergy.active").formatted(Formatting.GOLD, Formatting.BOLD));

        for (SkillSynergy synergy : activeSynergies) {
            int bonusPercent = Math.round(synergy.bonusMultiplier() * 100);
            lines.add(Text.literal("✦ ")
                    .append(Text.translatable("murilloskills.synergy." + synergy.id()))
                    .append(Text.literal(" +" + bonusPercent + "%"))
                    .formatted(Formatting.GREEN));
        }

        return lines;
    }

    // ============ RECORDS E ENUMS ============

    public record SkillSynergy(
            String id,
            Set<MurilloSkillsList> requiredSkills,
            float bonusMultiplier,
            SynergyType type) {
    }

    public enum SynergyType {
        DAMAGE_REDUCTION, // Redução de dano
        ORE_DROPS, // Drops de minério
        MOVEMENT_SPEED, // Velocidade de movimento
        ALL_DROPS, // Todos os drops
        RARE_FINDS, // Achados raros
        ALL_DAMAGE, // Todo dano causado
        CRAFTING_EFFICIENCY // Eficiência de craft
    }
}
