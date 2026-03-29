package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    // Injeta na verificação de nível de encantamento
    @Inject(method = "getEquipmentLevel", at = @At("RETURN"), cancellable = true)
    private static void getEquipmentLevel(RegistryEntry<Enchantment> enchantment, LivingEntity entity,
            CallbackInfoReturnable<Integer> cir) {

        // 1. Verifica se estamos no servidor e se a entidade é um Player
        if (!entity.getEntityWorld().isClient() && entity instanceof ServerPlayerEntity player) {

            if (enchantment.matchesKey(Enchantments.LOOTING)) {
                PlayerSkillData playerData = player
                        .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

                int totalBonus = 0;

                // Warrior looting bonus
                if (playerData.isSkillSelected(MurilloSkillsList.WARRIOR)) {
                    int warriorLevel = playerData.getSkill(MurilloSkillsList.WARRIOR).level;
                    totalBonus += (int) (warriorLevel * SkillConfig.getWarriorLootingPerLevel());
                }

                // Archer looting bonus
                if (playerData.isSkillSelected(MurilloSkillsList.ARCHER)) {
                    int archerLevel = playerData.getSkill(MurilloSkillsList.ARCHER).level;
                    totalBonus += (int) (archerLevel * SkillConfig.getArcherLootingPerLevel());
                }

                if (totalBonus > 0) {
                    cir.setReturnValue(cir.getReturnValue() + totalBonus);
                }
            }
        }
    }
}