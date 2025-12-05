package com.murilloskills.api;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all skills in the MurilloSkills system.
 * Each skill should extend this class and implement the required methods.
 * This class provides:
 * - Dedicated logging for each skill
 * - Error handling with try-catch blocks
 * - Standard lifecycle methods that skills can override
 * - Default implementations for common functionality
 */
public abstract class AbstractSkill {

    // Logger dedicado para cada instância de skill
    protected final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-" + getClass().getSimpleName());

    /**
     * Returns the skill type that this implementation handles.
     * This is used by the registry to map skills to their handlers.
     */
    public abstract MurilloSkillsList getSkillType();

    /**
     * Called when a player levels up in this skill.
     * Override this method to implement level-up specific logic.
     * 
     * @param player The player who leveled up
     * @param newLevel The new level achieved
     */
    public void onLevelUp(ServerPlayerEntity player, int newLevel) {
        try {
            // Default implementation: send a level up message
            player.sendMessage(Text.literal("Parabéns! Você alcançou o nível " + newLevel + " em " + getSkillType().name() + "!")
                    .formatted(Formatting.GREEN, Formatting.BOLD), false);
            
            // Update attributes when leveling up
            updateAttributes(player, newLevel);
            
            LOGGER.info("Player {} leveled up to {} in skill {}", 
                    player.getName().getString(), newLevel, getSkillType());
        } catch (Exception e) {
            LOGGER.error("Erro ao processar Level Up para a skill " + getSkillType() + " do jogador " + player.getName().getString(), e);
        }
    }

    /**
     * Called when a player uses their active ability (Paragon skill).
     * Override this method to implement the skill's active ability.
     * 
     * @param player The player using the ability
     * @param stats The player's skill stats
     */
    public void onActiveAbility(ServerPlayerEntity player, SkillGlobalState.SkillStats stats) {
        try {
            // Default implementation: inform that the skill has no active ability yet
            player.sendMessage(Text.literal("Esta skill não possui habilidade ativa ainda.")
                    .formatted(Formatting.RED), true);
            LOGGER.debug("Player {} tried to use active ability for skill {} which has no implementation", 
                    player.getName().getString(), getSkillType());
        } catch (Exception e) {
            LOGGER.error("Erro ao executar habilidade ativa da skill " + getSkillType() + " para " + player.getName().getString(), e);
            player.sendMessage(Text.literal("Erro ao ativar habilidade. Contate o admin.")
                    .formatted(Formatting.RED), false);
        }
    }
    
    /**
     * Called to apply or update passive attributes for this skill.
     * This is typically called when a player joins the server or levels up.
     * 
     * @param player The player to update attributes for
     * @param level The current level of the skill
     */
    public void updateAttributes(ServerPlayerEntity player, int level) {
        try {
            // Default implementation: no attributes to update
            // Skills should override this to implement their passive bonuses
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar atributos da skill " + getSkillType() + " para " + player.getName().getString(), e);
        }
    }

    /**
     * Called every server tick for players who have this skill.
     * Override this method to implement tick-based functionality like passive effects.
     * 
     * Note: This is called every tick, so be mindful of performance.
     * Consider using player.age % X to limit execution frequency.
     * 
     * @param player The player to tick
     * @param level The current level of the skill
     */
    public void onTick(ServerPlayerEntity player, int level) {
        try {
            // Default implementation: no tick behavior
            // Skills should override this to implement their tick-based effects
        } catch (Exception e) {
            // Log with limited frequency to avoid spamming console on tick errors
            if (player.age % 200 == 0) {
                LOGGER.error("Erro no tick da skill " + getSkillType() + " para " + player.getName().getString(), e);
            }
        }
    }

    /**
     * Called when a player joins the server or changes worlds.
     * Override this method to implement join-specific logic.
     * 
     * @param player The player who joined
     * @param level The current level of the skill
     */
    public void onPlayerJoin(ServerPlayerEntity player, int level) {
        try {
            // Default implementation: update attributes
            updateAttributes(player, level);
        } catch (Exception e) {
            LOGGER.error("Erro ao processar entrada do jogador para skill " + getSkillType(), e);
        }
    }

    /**
     * Utility method to check if a player meets the level requirement for a feature.
     * 
     * @param level Current skill level
     * @param requiredLevel Required level
     * @return true if the level requirement is met
     */
    protected boolean meetsLevelRequirement(int level, int requiredLevel) {
        return level >= requiredLevel;
    }

    /**
     * Utility method to send a formatted message to a player.
     * 
     * @param player The player to send the message to
     * @param message The message text
     * @param formatting The formatting to apply
     * @param actionBar Whether to send as action bar message
     */
    protected void sendMessage(ServerPlayerEntity player, String message, Formatting formatting, boolean actionBar) {
        player.sendMessage(Text.literal(message).formatted(formatting), actionBar);
    }
}
