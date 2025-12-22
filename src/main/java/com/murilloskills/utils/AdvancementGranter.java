package com.murilloskills.utils;

import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

/**
 * Utilitário para conceder advancements de milestone quando jogadores
 * atingem certos níveis nas skills.
 */
public class AdvancementGranter {

    /**
     * Verifica e concede advancements de milestone baseado no nível alcançado.
     * 
     * @param player   Jogador que atingiu o nível
     * @param skill    Skill que foi levelada
     * @param oldLevel Nível anterior
     * @param newLevel Novo nível
     */
    public static void checkAndGrantAdvancement(ServerPlayerEntity player, MurilloSkillsList skill, int oldLevel,
            int newLevel) {
        // Milestones: 10, 25, 50, 75, 100
        int[] milestones = { 10, 25, 50, 75, 100 };

        for (int milestone : milestones) {
            if (oldLevel < milestone && newLevel >= milestone) {
                grantSkillAdvancement(player, skill, milestone);
            }
        }

        // Special case: Explorer level 35 (Night Vision unlock)
        if (skill == MurilloSkillsList.EXPLORER && oldLevel < 35 && newLevel >= 35) {
            grantSkillAdvancement(player, skill, 35);
        }
    }

    /**
     * Concede um advancement específico de skill/milestone.
     */
    private static void grantSkillAdvancement(ServerPlayerEntity player, MurilloSkillsList skill, int milestone) {
        String skillName = skill.name().toLowerCase();
        String advancementId = String.format("murilloskills:%s/level_%d", skillName, milestone);

        Identifier id = Identifier.tryParse(advancementId);
        if (id == null)
            return;

        MinecraftServer server = (MinecraftServer) player.getEntityWorld().getServer();
        if (server == null)
            return;

        var advancementLoader = server.getAdvancementLoader();
        AdvancementEntry advancementEntry = advancementLoader.get(id);

        if (advancementEntry == null) {
            // Advancement doesn't exist for this skill/milestone, skip silently
            return;
        }

        AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancementEntry);

        if (!progress.isDone()) {
            // Grant all criteria for this advancement
            for (String criterion : progress.getUnobtainedCriteria()) {
                player.getAdvancementTracker().grantCriterion(advancementEntry, criterion);
            }
        }
    }

    /**
     * Verifica se um jogador tem um advancement específico.
     */
    public static boolean hasAdvancement(ServerPlayerEntity player, String advancementId) {
        Identifier id = Identifier.tryParse(advancementId);
        if (id == null)
            return false;

        MinecraftServer server = (MinecraftServer) player.getEntityWorld().getServer();
        if (server == null)
            return false;

        var advancementLoader = server.getAdvancementLoader();
        AdvancementEntry advancementEntry = advancementLoader.get(id);

        if (advancementEntry == null)
            return false;

        return player.getAdvancementTracker().getProgress(advancementEntry).isDone();
    }

    /**
     * Concede um advancement de prestige para uma skill.
     */
    public static void grantPrestigeAdvancement(ServerPlayerEntity player, MurilloSkillsList skill, int prestigeLevel) {
        // Prestige advancements: murilloskills:prestige/{skill}/p{level}
        String advancementId = String.format("murilloskills:prestige/%s/p%d", skill.name().toLowerCase(),
                prestigeLevel);

        Identifier id = Identifier.tryParse(advancementId);
        if (id == null)
            return;

        MinecraftServer server = (MinecraftServer) player.getEntityWorld().getServer();
        if (server == null)
            return;

        var advancementLoader = server.getAdvancementLoader();
        AdvancementEntry advancementEntry = advancementLoader.get(id);

        if (advancementEntry == null) {
            // Advancement doesn't exist, skip silently
            return;
        }

        AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancementEntry);

        if (!progress.isDone()) {
            for (String criterion : progress.getUnobtainedCriteria()) {
                player.getAdvancementTracker().grantCriterion(advancementEntry, criterion);
            }
        }
    }

    // ==================== CONQUISTAS ESPECIAIS ====================

    /**
     * Concede a conquista "Trio Escolhido" quando o jogador seleciona 3 skills.
     */
    public static void grantTrioChosen(ServerPlayerEntity player) {
        grantAdvancement(player, "murilloskills:special/trio_chosen");
    }

    /**
     * Concede a conquista "Primeiro Paragon" quando o jogador confirma seu primeiro
     * Paragon.
     */
    public static void grantFirstParagon(ServerPlayerEntity player) {
        grantAdvancement(player, "murilloskills:special/first_paragon");
    }

    /**
     * Concede a conquista "Primeiro Prestige" quando o jogador faz seu primeiro
     * Prestige.
     */
    public static void grantFirstPrestige(ServerPlayerEntity player) {
        grantAdvancement(player, "murilloskills:special/first_prestige");
    }

    /**
     * Concede a conquista "Sinergista" quando o jogador ativa sua primeira
     * sinergia.
     */
    public static void grantFirstSynergy(ServerPlayerEntity player) {
        grantAdvancement(player, "murilloskills:special/first_synergy");
    }

    /**
     * Concede a conquista "Mestre Supremo" quando o jogador atinge level 100 em
     * todas as skills.
     */
    public static void grantMasterSupreme(ServerPlayerEntity player) {
        grantAdvancement(player, "murilloskills:special/master_supreme");
    }

    // ==================== CONQUISTAS DE AÇÕES ====================

    /**
     * Concede uma conquista de ação para uma skill específica.
     * Só concede se a skill estiver selecionada.
     */
    public static void grantActionAdvancement(ServerPlayerEntity player, MurilloSkillsList skill, String actionName) {
        // Verificar se a skill está selecionada
        if (!isSkillSelected(player, skill)) {
            return;
        }

        String advancementId = String.format("murilloskills:%s/%s", skill.name().toLowerCase(), actionName);
        grantAdvancement(player, advancementId);
    }

    /**
     * Verifica se uma skill está selecionada para o jogador.
     */
    private static boolean isSkillSelected(ServerPlayerEntity player, MurilloSkillsList skill) {
        MinecraftServer server = (MinecraftServer) player.getEntityWorld().getServer();
        if (server == null)
            return false;

        var data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        if (data == null)
            return false;

        // Se for Paragon, todas as skills estão "ativas"
        if (data.paragonSkill != null) {
            return true;
        }

        // Verificar se skill está nas selecionadas
        return data.selectedSkills.contains(skill);
    }

    /**
     * Concede um advancement genérico pelo ID.
     */
    public static void grantAdvancement(ServerPlayerEntity player, String advancementId) {
        Identifier id = Identifier.tryParse(advancementId);
        if (id == null)
            return;

        MinecraftServer server = (MinecraftServer) player.getEntityWorld().getServer();
        if (server == null)
            return;

        var advancementLoader = server.getAdvancementLoader();
        AdvancementEntry advancementEntry = advancementLoader.get(id);

        if (advancementEntry == null)
            return;

        AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancementEntry);

        if (!progress.isDone()) {
            for (String criterion : progress.getUnobtainedCriteria()) {
                player.getAdvancementTracker().grantCriterion(advancementEntry, criterion);
            }
        }
    }

    // ==================== MÉTODOS CONVENIENCE PARA CADA SKILL ====================

    // MINER
    public static void grantFirstDiamond(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.MINER, "first_diamond");
    }

    public static void grantEmeraldHunter(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.MINER, "emerald_hunter");
    }

    // WARRIOR
    public static void grantFirstBlood(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.WARRIOR, "first_blood");
    }

    public static void grantDragonSlayer(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.WARRIOR, "dragon_slayer");
    }

    // FARMER
    public static void grantFirstHarvest(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.FARMER, "first_harvest");
    }

    public static void grantMegaFarmer(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.FARMER, "mega_farmer");
    }

    // ARCHER
    public static void grantSharpshooter(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.ARCHER, "sharpshooter");
    }

    public static void grantSniper(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.ARCHER, "sniper");
    }

    // FISHER
    public static void grantTreasureCatch(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.FISHER, "treasure_catch");
    }

    public static void grantSeaKing(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.FISHER, "sea_king");
    }

    // BUILDER
    public static void grantFirstBuild(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.BUILDER, "first_build");
    }

    public static void grantDivineArchitect(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.BUILDER, "divine_architect");
    }

    // EXPLORER
    public static void grantTraveler(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.EXPLORER, "traveler");
    }

    public static void grantBiomeDiscoverer(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.EXPLORER, "biome_discoverer");
    }

    // ==================== NEW ACHIEVEMENTS ====================

    // BLACKSMITH (new)
    public static void grantFirstForge(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.BLACKSMITH, "first_forge");
    }

    public static void grantMasterEnchanter(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.BLACKSMITH, "master_enchanter");
    }

    // WARRIOR (new)
    public static void grantWitherSlayer(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.WARRIOR, "wither_slayer");
    }

    public static void grantEliteHunter(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.WARRIOR, "elite_hunter");
    }

    // FARMER (new)
    public static void grantAnimalBreeder(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.FARMER, "animal_breeder");
    }

    // ARCHER (new)
    public static void grantPrecisionMaster(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.ARCHER, "precision_master");
    }

    // FISHER (new)
    public static void grantLuckyFisher(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.FISHER, "lucky_fisher");
    }

    // BUILDER (new)
    public static void grantHeightMaster(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.BUILDER, "height_master");
    }

    // EXPLORER (new)
    public static void grantDimensionWalker(ServerPlayerEntity player) {
        grantActionAdvancement(player, MurilloSkillsList.EXPLORER, "dimension_walker");
    }

    // SPECIAL (new)
    public static void grantSpeedLeveler(ServerPlayerEntity player) {
        grantAdvancement(player, "murilloskills:special/speed_leveler");
    }

    public static void grantXpStreak(ServerPlayerEntity player) {
        grantAdvancement(player, "murilloskills:special/xp_streak");
    }

    public static void grantSynergyMaster(ServerPlayerEntity player) {
        grantAdvancement(player, "murilloskills:special/synergy_master");
    }
}
