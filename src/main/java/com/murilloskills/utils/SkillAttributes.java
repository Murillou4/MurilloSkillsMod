package com.murilloskills.utils;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SkillAttributes {

    private static final Identifier MINER_SPEED_ID = Identifier.of("murilloskills", "miner_speed_bonus");
    private static final Identifier WARRIOR_DAMAGE_ID = Identifier.of("murilloskills", "warrior_damage");
    private static final Identifier WARRIOR_HEALTH_ID = Identifier.of("murilloskills", "warrior_health");

    public static final float mineSpeedMultiplier = 0.03f;
    public static final float mineFortuneMultiplier = 0.03f;
    public static final float warriorDamageMultiplier = 0.05f;
    public static final float warriorLootingMultiplier = 0.02f;

    public static void updateAllStats(ServerPlayerEntity player) {
        updateMinerStats(player);
        updateWarriorStats(player);
    }


    public static void updateMinerStats(ServerPlayerEntity player) {
        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        int level = state.getPlayerData(player).getSkill(MurilloSkillsList.MINER).level;

        double speedBonus = level * mineSpeedMultiplier;

        var attributeInstance = player.getAttributeInstance(EntityAttributes.BLOCK_BREAK_SPEED);

        if (attributeInstance != null) {
            attributeInstance.removeModifier(MINER_SPEED_ID);

            if (speedBonus > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                        MINER_SPEED_ID,
                        speedBonus,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                attributeInstance.addTemporaryModifier(modifier);
            }
        }
    }

    public static void updateWarriorStats(ServerPlayerEntity player) {
        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        int level = state.getPlayerData(player).getSkill(MurilloSkillsList.WARRIOR).level;

        // --- 1. DANO (0.1 por level) ---
        double damageBonus = level * warriorDamageMultiplier;

        var damageAttr = player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.removeModifier(WARRIOR_DAMAGE_ID);
            if (damageBonus > 0) {
                // ADD_VALUE = Adiciona valor fixo (ex: Espada 7 + 50 = 57)
                damageAttr.addTemporaryModifier(new EntityAttributeModifier(
                        WARRIOR_DAMAGE_ID, damageBonus, EntityAttributeModifier.Operation.ADD_VALUE
                ));
            }
        }

        // --- 2. VIDA EXTRA (Milestones) ---
        double healthBonus = 0;

        // Nível 10: +1 Coração (+2 HP)
        if (level >= 10) healthBonus += 2.0;
        // Nível 50: +1 Coração (Total +4 HP)
        if (level >= 50) healthBonus += 2.0;
        // Nível 100: +3 Corações (Total +10 HP = 5 Corações)
        if (level >= 100) healthBonus += 6.0;

        var healthAttr = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(WARRIOR_HEALTH_ID);
            if (healthBonus > 0) {
                healthAttr.addTemporaryModifier(new EntityAttributeModifier(
                        WARRIOR_HEALTH_ID, healthBonus, EntityAttributeModifier.Operation.ADD_VALUE
                ));

                // Cura o player para preencher a vida nova se ele estiver full
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }
    }
}