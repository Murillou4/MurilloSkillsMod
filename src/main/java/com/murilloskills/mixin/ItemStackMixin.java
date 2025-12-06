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
     * Esse método é o ponto de entrada principal para perda de durabilidade
     * (quebrar blocos, atacar).
     */
    @Inject(method = "damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V", at = @At("HEAD"), cancellable = true)
    private void checkDurabilityProtection(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {

        // Verifica se é no servidor e se é um jogador
        if (entity.getEntityWorld().isClient() || !(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        // Recupera o estado global do servidor
        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        var playerData = state.getPlayerData(player);

        // --- MINER: 15% chance to ignore tool durability loss (level 30+) ---
        int minerLevel = playerData.getSkill(MurilloSkillsList.MINER).level;
        if (minerLevel >= SkillConfig.MINER_DURABILITY_LEVEL) {
            if (player.getRandom().nextFloat() < SkillConfig.MINER_DURABILITY_CHANCE) {
                ci.cancel();
                return;
            }
        }

        // --- BLACKSMITH: Armor durability protection during Titanium Aura ---
        if (playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            // Check if this is armor (helmet, chestplate, leggings, boots)
            boolean isArmor = slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                    || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;

            if (isArmor && com.murilloskills.impl.BlacksmithSkill.isTitaniumAuraActive(player)) {
                // During Titanium Aura, armor doesn't lose durability
                ci.cancel();
                return;
            }
        }
    }
}