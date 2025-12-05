package com.murilloskills.api;

import com.murilloskills.skills.MurilloSkillsList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Central registry for all skills in the MurilloSkills system.
 * 
 * This registry uses the Strategy pattern to manage skill implementations.
 * Instead of having switch-case statements scattered throughout the code,
 * all skill logic is encapsulated in their respective AbstractSkill implementations
 * and accessed through this registry.
 * 
 * Benefits:
 * - Easy to add new skills without modifying existing code
 * - Centralized skill management
 * - Type-safe skill lookups using EnumMap
 * - Proper error handling and logging
 */
public class SkillRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-Registry");
    
    // Using EnumMap for optimal performance and type safety
    private static final Map<MurilloSkillsList, AbstractSkill> SKILLS = new EnumMap<>(MurilloSkillsList.class);

    /**
     * Registers a skill implementation in the registry.
     * 
     * @param skill The skill implementation to register
     * @throws IllegalArgumentException if skill is null or already registered
     */
    public static void register(AbstractSkill skill) {
        if (skill == null) {
            LOGGER.error("Tentativa de registrar skill nula ignorada");
            return;
        }

        try {
            MurilloSkillsList skillType = skill.getSkillType();
            
            if (skillType == null) {
                LOGGER.error("Skill {} retornou tipo nulo - registro ignorado", skill.getClass().getSimpleName());
                return;
            }

            if (SKILLS.containsKey(skillType)) {
                LOGGER.warn("Skill duplicada detectada e ignorada: {} (classe: {})", 
                        skillType, skill.getClass().getSimpleName());
                return;
            }

            SKILLS.put(skillType, skill);
            LOGGER.info("Skill registrada com sucesso: {} (classe: {})", 
                    skillType, skill.getClass().getSimpleName());
                    
        } catch (Exception e) {
            LOGGER.error("Erro fatal ao registrar skill: " + skill.getClass().getName(), e);
        }
    }

    /**
     * Retrieves a skill implementation from the registry.
     * 
     * @param type The skill type to retrieve
     * @return The skill implementation, or null if not found
     */
    public static AbstractSkill get(MurilloSkillsList type) {
        if (type == null) {
            LOGGER.warn("Tentativa de buscar skill com tipo nulo");
            return null;
        }

        AbstractSkill skill = SKILLS.get(type);
        if (skill == null) {
            LOGGER.warn("Skill não encontrada no registry: {}", type);
        }
        
        return skill;
    }

    /**
     * Checks if a skill is registered.
     * 
     * @param type The skill type to check
     * @return true if the skill is registered, false otherwise
     */
    public static boolean isRegistered(MurilloSkillsList type) {
        return type != null && SKILLS.containsKey(type);
    }

    /**
     * Gets all registered skill types.
     * 
     * @return A set of all registered skill types
     */
    public static Set<MurilloSkillsList> getRegisteredSkills() {
        return SKILLS.keySet();
    }

    /**
     * Gets the number of registered skills.
     * 
     * @return The number of registered skills
     */
    public static int getRegisteredSkillCount() {
        return SKILLS.size();
    }

    /**
     * Clears all registered skills.
     * This should only be used for testing or during shutdown.
     */
    public static void clear() {
        LOGGER.info("Limpando registry de skills - {} skills removidas", SKILLS.size());
        SKILLS.clear();
    }

    /**
     * Logs information about all registered skills.
     * Useful for debugging and startup verification.
     */
    public static void logRegisteredSkills() {
        LOGGER.info("=== SKILLS REGISTRADAS ===");
        if (SKILLS.isEmpty()) {
            LOGGER.info("Nenhuma skill registrada");
        } else {
            SKILLS.forEach((type, skill) -> {
                LOGGER.info("- {}: {}", type, skill.getClass().getSimpleName());
            });
        }
        LOGGER.info("Total: {} skills", SKILLS.size());
        LOGGER.info("========================");
    }

    /**
     * Validates that all expected skills are registered.
     * This can be called during startup to ensure the system is properly configured.
     * 
     * @param expectedSkills The skills that should be registered
     * @return true if all expected skills are registered, false otherwise
     */
    public static boolean validateRegistration(MurilloSkillsList... expectedSkills) {
        boolean allRegistered = true;
        
        for (MurilloSkillsList skill : expectedSkills) {
            if (!isRegistered(skill)) {
                LOGGER.error("Skill esperada não está registrada: {}", skill);
                allRegistered = false;
            }
        }
        
        if (allRegistered) {
            LOGGER.info("Validação do registry concluída com sucesso - todas as skills esperadas estão registradas");
        } else {
            LOGGER.error("Validação do registry falhou - algumas skills esperadas não estão registradas");
        }
        
        return allRegistered;
    }
}
