package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    /**
     * Injeta na lógica de dano do item (Server Side).
     * Alvo: public void damage(int amount, LivingEntity entity, EquipmentSlot slot)
     * Esse método é o ponto de entrada principal para perda de durabilidade (quebrar blocos, atacar).
     */
    @Inject(method = "damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V", at = @At("HEAD"), cancellable = true)
    private void checkMinerDurability(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {

        // Verifica se é no servidor e se é um jogador
        if (entity.getEntityWorld().isClient() || !(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        // Recupera o estado global do servidor
        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        int level = state.getPlayerData(player).getSkill(MurilloSkillsList.MINER).level;

        if (level >= SkillConfig.MINER_DURABILITY_LEVEL) {
            // 15% de chance de cancelar o dano totalmente (ignora Unbreaking, apenas não gasta)
            if (player.getRandom().nextFloat() < SkillConfig.MINER_DURABILITY_CHANCE) {
                ci.cancel();
            }
        }
    }
}