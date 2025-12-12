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

        // Aplica bônus de dano baseado no nível do Archer
        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        int level = state.getPlayerData(player).getSkill(MurilloSkillsList.ARCHER).level;

        // IMPORTANTE: Aplica piercing ANTES de processar o hit para evitar que a flecha
        // volte
        // Isso deve ser feito antes de qualquer processamento de dano
        if (level > 0 && ArcherSkill.hasArrowPenetration(level)) {
            byte currentPierce = arrow.getPierceLevel();
            if (currentPierce < 3) {
                ((PersistentProjectileEntityAccessor) arrow).invokeSetPierceLevel((byte) 3);
            }
        }

        // Calcula a distância do tiro
        double distance = arrow.getEntityPos().distanceTo(player.getEntityPos());

        // Dá XP para o Archer
        ArcherHitHandler.handleArrowHit(player, target, distance);

        // Registra último inimigo para homing (Master Ranger)
        if (target instanceof LivingEntity) {
            ArcherSkill.setLastDamagedEnemy(player, target.getUuid());
        }

        if (level > 0) {
            int prestige = state.getPlayerData(player).getSkill(MurilloSkillsList.ARCHER).prestige;
            double damageMultiplier = ArcherSkill.getRangedDamageMultiplier(level, prestige);

            // Headshot detection: check if arrow hit near the target's eye level
            boolean isHeadshot = false;
            if (target instanceof LivingEntity livingTarget) {
                Vec3d hitPos = entityHitResult.getPos();
                double targetEyeY = livingTarget.getEyePos().y;
                double hitY = hitPos.y;

                // Headshot if arrow hits within 0.3 blocks of eye level or above
                isHeadshot = hitY >= targetEyeY - 0.3;
            }

            // Apply headshot bonus (30% extra damage)
            double headshotMultiplier = isHeadshot
                    ? (1.0 + com.murilloskills.utils.SkillConfig.ARCHER_HEADSHOT_DAMAGE_BONUS)
                    : 1.0;

            double baseDamage = ((PersistentProjectileEntityAccessor) arrow).getDamage();
            arrow.setDamage(baseDamage * damageMultiplier * headshotMultiplier);

            // Notify player of headshot
            if (isHeadshot) {
                player.sendMessage(
                        net.minecraft.text.Text.translatable("murilloskills.notify.headshot")
                                .formatted(net.minecraft.util.Formatting.RED, net.minecraft.util.Formatting.BOLD),
                        true);
            }

            // Track daily challenge progress - Archer challenges (uses distance from line
            // 56)
            com.murilloskills.events.ChallengeEventsHandler.onArrowHit(player, distance, isHeadshot);
        }
    }

    /**
     * Implementa o homing arrow - flecha persegue inimigos quando Master
     * Ranger está ativo. Usa SLERP para curvas suaves e naturais.
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

        // NOTE: Piercing agora é aplicado em onArrowHit() para evitar bug de flecha
        // voltando

        // Verifica se o Master Ranger está ativo
        if (!ArcherSkill.isMasterRangerActive(player)) {
            return;
        }

        // Pega a velocidade atual da flecha
        Vec3d currentVelocity = arrow.getVelocity();
        double speed = currentVelocity.length();

        // Se a flecha está muito lenta, não ajusta (provavelmente parou)
        if (speed < 0.1) {
            return;
        }

        Vec3d arrowPos = arrow.getEntityPos();
        Vec3d currentDirection = currentVelocity.normalize();

        // Busca o alvo - primeiro tenta o último inimigo danificado
        Entity targetEntity = null;
        UUID targetUuid = ArcherSkill.getLastDamagedEnemy(player);

        if (targetUuid != null) {
            ServerWorld world = (ServerWorld) arrow.getEntityWorld();
            targetEntity = world.getEntity(targetUuid);

            // Verifica se o alvo ainda é válido
            if (targetEntity == null || !targetEntity.isAlive()) {
                targetEntity = null;
            }
        }

        // Se não tem alvo prévio, busca o inimigo mais próximo na direção da flecha
        if (targetEntity == null) {
            targetEntity = findBestTarget(arrow, player, arrowPos, currentDirection);
        }

        // Se não encontrou nenhum alvo, não faz nada
        if (targetEntity == null) {
            return;
        }

        // Calcula a direção para o alvo (mira no centro do corpo)
        Vec3d targetPos = targetEntity.getEntityPos().add(0, targetEntity.getHeight() * 0.5, 0);
        Vec3d toTarget = targetPos.subtract(arrowPos);
        double distanceToTarget = toTarget.length();

        // Se está muito perto, não precisa ajustar (vai acertar de qualquer jeito)
        if (distanceToTarget < 0.5) {
            return;
        }

        Vec3d targetDirection = toTarget.normalize();

        // Calcula o ângulo entre a direção atual e a direção do alvo
        double dotProduct = currentDirection.dotProduct(targetDirection);
        dotProduct = Math.max(-1.0, Math.min(1.0, dotProduct)); // Clamp
        double angleBetween = Math.acos(dotProduct);

        // =====================================
        // SISTEMA DE CURVA SUAVE
        // =====================================

        // Ângulo máximo de curva por tick (em radianos)
        // ~3 graus por tick = curva bem suave
        // Ajustado inversamente pela velocidade: quanto mais rápido, mais forte a curva
        // para compensar o menor número de ticks até o alvo
        double baseMaxAngle = Math.toRadians(3.0);

        // Fator de compensação de velocidade
        // Velocidade normal de flecha: ~1.5-2.0 blocos/tick
        // Velocidade com power V + força máxima: ~3.0+ blocos/tick
        double velocityFactor = Math.max(1.0, speed / 1.5);
        double maxAnglePerTick = baseMaxAngle * velocityFactor;

        // Fator de distância: curva mais agressiva quando perto do alvo
        // para garantir que acerte
        double distanceFactor = 1.0;
        if (distanceToTarget < 5.0) {
            // Interpolação suave: quanto mais perto, mais forte (até 2x)
            distanceFactor = 1.0 + (5.0 - distanceToTarget) / 5.0;
        }
        maxAnglePerTick *= distanceFactor;

        // Limita a curva ao máximo permitido
        double actualAngle = Math.min(angleBetween, maxAnglePerTick);

        // Se já está apontando pro alvo (dentro de 1 grau), não ajusta
        if (angleBetween < Math.toRadians(1.0)) {
            return;
        }

        // Calcula a nova direção usando interpolação esférica (SLERP)
        Vec3d newDirection = slerp(currentDirection, targetDirection, actualAngle / angleBetween);

        // Aplica a nova velocidade mantendo a mesma speed
        Vec3d newVelocity = newDirection.multiply(speed);
        arrow.setVelocity(newVelocity.x, newVelocity.y, newVelocity.z);
    }

    /**
     * Busca o melhor alvo na direção da flecha.
     * Considera apenas inimigos num cone de 45° à frente da flecha,
     * priorizando os mais próximos e mais alinhados.
     */
    private Entity findBestTarget(PersistentProjectileEntity arrow, ServerPlayerEntity player,
            Vec3d arrowPos, Vec3d arrowDirection) {
        ServerWorld world = (ServerWorld) arrow.getEntityWorld();

        // Raio máximo de busca
        double maxRange = 32.0;
        // Ângulo máximo do cone (45 graus = cos(45°) ≈ 0.707)
        double minDotProduct = 0.707;

        Entity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        // Busca todas as entidades vivas no raio
        for (LivingEntity entity : world.getEntitiesByClass(
                LivingEntity.class,
                arrow.getBoundingBox().expand(maxRange),
                e -> e != player && e.isAlive() && !e.isSpectator())) {

            // Vetor da flecha para a entidade
            Vec3d toEntity = entity.getEntityPos().add(0, entity.getHeight() * 0.5, 0).subtract(arrowPos);
            double distance = toEntity.length();

            // Ignora entidades muito longe
            if (distance > maxRange || distance < 0.5) {
                continue;
            }

            // Verifica se está no cone de visão da flecha
            Vec3d directionToEntity = toEntity.normalize();
            double dot = arrowDirection.dotProduct(directionToEntity);

            if (dot < minDotProduct) {
                continue; // Fora do cone de 45°
            }

            // Score: menor é melhor
            // Combina distância e alinhamento
            // Entidades mais próximas e mais alinhadas têm score menor
            double alignmentFactor = 1.0 - dot; // 0 = perfeito alinhamento, 0.3 = limite do cone
            double score = distance * (1.0 + alignmentFactor * 2.0);

            if (score < bestScore) {
                bestScore = score;
                bestTarget = entity;
            }
        }

        return bestTarget;
    }

    /**
     * Interpolação esférica (SLERP) entre dois vetores unitários.
     * Produz uma rotação suave entre as direções.
     */
    private Vec3d slerp(Vec3d from, Vec3d to, double t) {
        // Clamp t entre 0 e 1
        t = Math.max(0.0, Math.min(1.0, t));

        double dot = from.dotProduct(to);
        dot = Math.max(-1.0, Math.min(1.0, dot));

        double theta = Math.acos(dot);

        // Se os vetores são quase paralelos, usa interpolação linear
        if (theta < 0.001) {
            return from.multiply(1.0 - t).add(to.multiply(t)).normalize();
        }

        double sinTheta = Math.sin(theta);
        double factorFrom = Math.sin((1.0 - t) * theta) / sinTheta;
        double factorTo = Math.sin(t * theta) / sinTheta;

        return from.multiply(factorFrom).add(to.multiply(factorTo)).normalize();
    }
}
