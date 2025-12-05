package com.murilloskills.mixin;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.api.SkillRegistry;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.WarriorSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // LIFESTEAL & LOG (Combined for performance - one injection instead of two)
    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamageLogic(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // Fast fail: Se não for player atacando, sai logo.
        if (!(source.getAttacker() instanceof ServerPlayerEntity attacker)) return;

        SkillGlobalState state = SkillGlobalState.getServerState(attacker.getEntityWorld().getServer());
        int level = state.getPlayerData(attacker).getSkill(MurilloSkillsList.WARRIOR).level;

        if (level <= 0) return;

        // --- LIFESTEAL ---
        float healAmount = 0f;
        boolean isBerserk = false;

        // Verifica se está em modo Berserk
        try {
            AbstractSkill skill = SkillRegistry.get(MurilloSkillsList.WARRIOR);
            if (skill instanceof WarriorSkill warriorSkill) {
                isBerserk = warriorSkill.isBerserkActive(attacker);
            }
        } catch (Exception ignored) {}

        if (isBerserk) {
            // Lifesteal massivo durante Berserk (ignora requisito de nível 75)
            healAmount = amount * SkillConfig.WARRIOR_BERSERK_LIFESTEAL;
        } else if (level >= SkillConfig.LIFESTEAL_UNLOCK_LEVEL) {
            // Lifesteal normal (nível 75+)
            healAmount = amount * SkillConfig.LIFESTEAL_PERCENTAGE;
        }

        if (healAmount > 0) {
            attacker.heal(healAmount);
        }

    }

    // RESISTÊNCIA
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float applyResistance(float amount, ServerWorld world, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        // Fast fail check
        if (self.getEntityWorld().isClient() || !(self instanceof ServerPlayerEntity player)) return amount;

        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        int level = state.getPlayerData(player).getSkill(MurilloSkillsList.WARRIOR).level;

        if (level >= SkillConfig.RESISTANCE_UNLOCK_LEVEL) {
            return amount * SkillConfig.RESISTANCE_REDUCTION;
        }
        return amount;
    }
}