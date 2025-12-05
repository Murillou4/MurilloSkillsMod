package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.ArcherSkill;
import com.murilloskills.skills.ArcherHitHandler;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Mixin para interceptar hits de flecha e aplicar modificadores do Archer
 */
@Mixin(PersistentProjectileEntity.class)
public abstract class ArrowEntityMixin {

    /**
     * Intercepta quando a flecha atinge uma entidade para dar XP e aplicar bônus
     */
    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void onArrowHit(EntityHitResult entityHitResult, CallbackInfo ci) {
        PersistentProjectileEntity arrow = (PersistentProjectileEntity) (Object) this;
        Entity owner = arrow.getOwner();
        Entity target = entityHitResult.getEntity();

        // Só processa no server e se o dono for um jogador
        if (arrow.getEntityWorld().isClient() || !(owner instanceof ServerPlayerEntity player)) {
            return;
        }

        // Calcula a distância do tiro
        double distance = arrow.getEntityPos().distanceTo(player.getEntityPos());

        // Dá XP para o Archer
        ArcherHitHandler.handleArrowHit(player, target, distance);

        // Registra último inimigo para homing (Master Ranger)
        if (target instanceof LivingEntity) {
            ArcherSkill.setLastDamagedEnemy(player, target.getUuid());
        }

        // Aplica bônus de dano baseado no nível do Archer
        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        int level = state.getPlayerData(player).getSkill(MurilloSkillsList.ARCHER).level;

        if (level > 0) {
            double damageMultiplier = ArcherSkill.getRangedDamageMultiplier(level);
            double baseDamage = ((PersistentProjectileEntityAccessor) arrow).getDamage();
            arrow.setDamage(baseDamage * damageMultiplier);
        }
    }

    /**
     * Implementa o homing arrow - flecha persegue o último inimigo quando Master
     * Ranger está ativo
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onArrowTick(CallbackInfo ci) {
        PersistentProjectileEntity arrow = (PersistentProjectileEntity) (Object) this;

        // Só processa no server
        if (arrow.getEntityWorld().isClient()) {
            return;
        }

        // Verifica se a flecha ainda está voando (não atingiu nada)
        if (arrow.isOnGround()) {
            return;
        }

        Entity owner = arrow.getOwner();

        // Verifica se o dono é um jogador
        if (!(owner instanceof ServerPlayerEntity player)) {
            return;
        }

        // Verifica se o Master Ranger está ativo
        if (!ArcherSkill.isMasterRangerActive(player)) {
            return;
        }

        // Pega o UUID do último inimigo danificado
        UUID targetUuid = ArcherSkill.getLastDamagedEnemy(player);
        if (targetUuid == null) {
            return;
        }

        // Busca a entidade alvo no mundo
        ServerWorld world = (ServerWorld) arrow.getEntityWorld();
        Entity targetEntity = world.getEntity(targetUuid);

        if (targetEntity == null || !targetEntity.isAlive()) {
            return;
        }

        // Calcula a direção para o alvo
        Vec3d arrowPos = arrow.getEntityPos();
        Vec3d targetPos = targetEntity.getEntityPos().add(0, targetEntity.getHeight() / 2, 0); // Mira no centro do alvo
        Vec3d direction = targetPos.subtract(arrowPos).normalize();

        // Pega a velocidade atual da flecha
        Vec3d currentVelocity = arrow.getVelocity();
        double speed = currentVelocity.length();

        // Se a flecha está muito lenta, não ajusta (provavelmente parou)
        if (speed < 0.1) {
            return;
        }

        // Força de homing - quanto maior, mais forte o ajuste de direção
        double homingStrength = 0.15; // 15% de ajuste por tick

        // Interpola entre a direção atual e a direção do alvo
        Vec3d currentDirection = currentVelocity.normalize();
        Vec3d newDirection = currentDirection.multiply(1 - homingStrength).add(direction.multiply(homingStrength))
                .normalize();

        // Aplica a nova velocidade mantendo a mesma speed
        Vec3d newVelocity = newDirection.multiply(speed);
        arrow.setVelocity(newVelocity.x, newVelocity.y, newVelocity.z);
    }
}
