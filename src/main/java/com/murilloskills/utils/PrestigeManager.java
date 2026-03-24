package com.murilloskills.utils;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Sistema de Prestige para skills.
 * Quando uma skill atinge nível 100, o jogador pode "prestigiar" para:
 * - Resetar a skill para nível 1
 * - Ganhar um nível de prestígio (máx 10)
 * - Receber bônus permanentes por prestígio
 * 
 * Bônus por nível de prestígio:
 * - +5% XP gain para essa skill
 * - +2% efetividade dos bônus passivos
 * - Cosmético: ícone/cor diferenciado na GUI
 */
public class PrestigeManager {

    // Configurações
    // MAX_PRESTIGE_LEVEL available via SkillConfig.getMaxPrestigeLevel()
    // XP_BONUS_PER_PRESTIGE and PASSIVE_BONUS_PER_PRESTIGE are now in SkillConfig

    /**
     * Verifica se uma skill pode ser prestigiada.
     * Requer nível 100 e prestígio atual < 10.
     */
    public static boolean canPrestige(ServerPlayerEntity player, MurilloSkillsList skill) {
        com.murilloskills.data.PlayerSkillData data = player
                .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        PlayerSkillData.SkillStats stats = data.getSkill(skill);

        return stats.level >= 100 && stats.prestige < SkillConfig.getMaxPrestigeLevel();
    }

    /**
     * Executa o prestígio de uma skill.
     * - Incrementa nível de prestígio
     * - Reseta nível para 1
     * - Reseta XP para 0
     * - Mantém cooldowns
     * 
     * @return true se o prestígio foi executado com sucesso
     */
    public static boolean doPrestige(ServerPlayerEntity player, MurilloSkillsList skill) {
        if (!canPrestige(player, skill)) {
            return false;
        }

        com.murilloskills.data.PlayerSkillData data = player
                .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        PlayerSkillData.SkillStats stats = data.getSkill(skill);

        // Incrementar prestígio
        int newPrestige = stats.prestige + 1;
        stats.prestige = newPrestige;

        // Resetar nível e XP
        stats.level = 1;
        stats.xp = 0;

        SkillsNetworkUtils.syncSkills(player);

        // Notificar jogador
        notifyPrestige(player, skill, newPrestige);

        // Efeitos visuais e sonoros
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // Grant advancement for prestige
        AdvancementGranter.grantPrestigeAdvancement(player, skill, newPrestige);

        return true;
    }

    /**
     * Calcula o multiplicador de XP baseado no nível de prestígio.
     * Usado ao adicionar XP para aplicar bônus de prestígio.
     */
    public static float getXpMultiplier(int prestigeLevel) {
        return 1.0f + (prestigeLevel * SkillConfig.getPrestigeXpBonus());
    }

    /**
     * Calcula o multiplicador de efetividade baseado no nível de prestígio.
     * Usado para melhorar passivas da skill.
     */
    public static float getPassiveMultiplier(int prestigeLevel) {
        return 1.0f + (prestigeLevel * SkillConfig.getPrestigePassiveBonus());
    }

    /**
     * Retorna o símbolo de prestígio para exibição na GUI.
     */
    public static String getPrestigeSymbol(int prestigeLevel) {
        if (prestigeLevel <= 0)
            return "";

        String[] symbols = SkillConfig.getPrestigeSymbols();
        if (prestigeLevel <= symbols.length) {
            return symbols[prestigeLevel - 1]; // 0-indexed
        }

        return "P" + prestigeLevel;
    }

    /**
     * Retorna a cor associada ao nível de prestígio.
     */
    public static int getPrestigeColor(int prestigeLevel) {
        if (prestigeLevel <= 0)
            return 0xFFFFFFFF;

        int[] colors = SkillConfig.getPrestigeColors();
        if (prestigeLevel <= colors.length) {
            return colors[prestigeLevel - 1]; // 0-indexed
        }

        return 0xFFFFFFFF; // Branco
    }

    private static void notifyPrestige(ServerPlayerEntity player, MurilloSkillsList skill, int newPrestige) {
        Text message = Text.empty()
                .append(Text.literal("🌟 ").formatted(Formatting.GOLD))
                .append(Text.translatable("murilloskills.notify.prestige").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
                .append(Text.translatable("murilloskills.skill.name." + skill.name().toLowerCase())
                        .formatted(Formatting.YELLOW))
                .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(getPrestigeSymbol(newPrestige) + " P" + newPrestige)
                        .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));

        player.sendMessage(message, false);

        // Bônus info
        int xpBonus = (int) (newPrestige * SkillConfig.getPrestigeXpBonus() * 100);
        int passiveBonus = (int) (newPrestige * SkillConfig.getPrestigePassiveBonus() * 100);

        Text bonusText = Text.translatable("murilloskills.prestige.bonus", xpBonus, passiveBonus)
                .formatted(Formatting.GRAY);
        player.sendMessage(bonusText, false);
    }
}
