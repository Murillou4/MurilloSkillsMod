package com.murilloskills.mixin;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.api.SkillRegistry;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.WarriorSkill;
import com.murilloskills.skills.ArcherHitHandler;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // LIFESTEAL & LOG (Combined for performance - one injection instead of two)
    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamageLogic(ServerWorld world, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        // Fast fail: Se não for player atacando, sai logo.
        if (!(source.getAttacker() instanceof ServerPlayerEntity attacker))
            return;

        SkillGlobalState state = SkillGlobalState.getServerState(attacker.getEntityWorld().getServer());
        int level = state.getPlayerData(attacker).getSkill(MurilloSkillsList.WARRIOR).level;

        if (level <= 0)
            return;

        // --- LIFESTEAL ---
        float healAmount = 0f;
        boolean isBerserk = false;

        // Verifica se está em modo Berserk
        try {
            AbstractSkill skill = SkillRegistry.get(MurilloSkillsList.WARRIOR);
            if (skill instanceof WarriorSkill warriorSkill) {
                isBerserk = warriorSkill.isBerserkActive(attacker);
            }
        } catch (Exception ignored) {
        }

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

    // RESISTÊNCIA (Warrior + Blacksmith)
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float applyResistance(float amount, ServerWorld world, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        // Fast fail check
        if (self.getEntityWorld().isClient() || !(self instanceof ServerPlayerEntity player))
            return amount;

        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        var playerData = state.getPlayerData(player);
        float modifiedAmount = amount;

        // --- WARRIOR RESISTANCE ---
        int warriorLevel = playerData.getSkill(MurilloSkillsList.WARRIOR).level;
        if (warriorLevel >= SkillConfig.RESISTANCE_UNLOCK_LEVEL) {
            modifiedAmount *= SkillConfig.RESISTANCE_REDUCTION;
        }

        // --- BLACKSMITH RESISTANCE ---
        if (playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            int blacksmithLevel = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;
            int blacksmithPrestige = playerData.getSkill(MurilloSkillsList.BLACKSMITH).prestige;

            // Check if damage is fire or explosion
            boolean isFireOrExplosion = source.getName().contains("fire")
                    || source.getName().contains("explosion")
                    || source.getName().contains("lava")
                    || source.getName().contains("inFire")
                    || source.getName().contains("onFire");

            // Apply Blacksmith damage multiplier (with prestige bonus)
            float damageMultiplier = com.murilloskills.impl.BlacksmithSkill
                    .calculateDamageMultiplier(player, blacksmithLevel, blacksmithPrestige, isFireOrExplosion);
            modifiedAmount *= damageMultiplier;

            // --- THORNS MASTER (Level 75+) ---
            if (com.murilloskills.impl.BlacksmithSkill.shouldReflectDamage(blacksmithLevel)) {
                if (source.getAttacker() instanceof LivingEntity attacker && attacker != player) {
                    float reflectedDamage = com.murilloskills.impl.BlacksmithSkill.getReflectedDamage(amount);
                    // Deal damage back to attacker
                    attacker.damage(world, player.getDamageSources().thorns(player), reflectedDamage);
                }
            }
        }

        // --- BUILDER FALL DAMAGE REDUCTION ---
        if (playerData.isSkillSelected(MurilloSkillsList.BUILDER)) {
            int builderLevel = playerData.getSkill(MurilloSkillsList.BUILDER).level;

            // Check if damage is from falling
            boolean isFallDamage = source.getName().contains("fall");

            if (isFallDamage && com.murilloskills.impl.BuilderSkill.shouldReduceFallDamage(builderLevel)) {
                float fallMultiplier = com.murilloskills.impl.BuilderSkill.getFallDamageMultiplier(builderLevel);
                modifiedAmount *= fallMultiplier;
            }
        }

        // --- EXPLORER FALL DAMAGE REDUCTION (Feather Feet) ---
        if (playerData.isSkillSelected(MurilloSkillsList.EXPLORER)) {
            int explorerLevel = playerData.getSkill(MurilloSkillsList.EXPLORER).level;

            // Check if damage is from falling
            boolean isFallDamage = source.getName().contains("fall");

            if (isFallDamage && com.murilloskills.impl.ExplorerSkill.hasFeatherFeet(explorerLevel)) {
                float fallMultiplier = com.murilloskills.impl.ExplorerSkill.getFallDamageMultiplier(explorerLevel);
                modifiedAmount *= fallMultiplier;
            }
        }

        return modifiedAmount;
    }

    // XP DO ARCHER - Quando uma entidade morre por flecha
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onEntityDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Fast fail: Só no server
        if (self.getEntityWorld().isClient())
            return;

        // Verifica se a fonte do dano é um projétil (flecha)
        if (!(source.getSource() instanceof PersistentProjectileEntity projectile))
            return;

        // Verifica se o dono do projétil é um jogador
        if (!(projectile.getOwner() instanceof ServerPlayerEntity player))
            return;

        // Calcula a distância do tiro
        double distance = projectile.getEntityPos().distanceTo(player.getEntityPos());

        // Dá XP de kill ao Archer
        ArcherHitHandler.handleArrowKill(player, self, distance);
    }
}