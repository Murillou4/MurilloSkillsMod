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
}
