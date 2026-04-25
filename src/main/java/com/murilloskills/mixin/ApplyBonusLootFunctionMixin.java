package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.PrestigeManager;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ApplyBonusLootFunction.class)
public class ApplyBonusLootFunctionMixin {

    @Shadow
    private RegistryEntry<Enchantment> enchantment;

    /**
     * Intercepta a variável 'level' (int) logo após ela ser calculada pelo jogo.
     * O jogo calcula: Nível da Picareta.
     * Nós fazemos: Nível da Picareta + Bônus da Skill.
     */
    @ModifyVariable(method = "process", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private int addSkillFortuneLevel(int originalLevel, ItemStack stack, LootContext context) {

        // 1. Verifica se a função de loot está calculando FORTUNA
        if (!this.enchantment.matchesKey(Enchantments.FORTUNE)) {
            return originalLevel;
        }

        // 2. Tenta pegar o jogador que quebrou o bloco
        Entity entity = context.get(LootContextParameters.THIS_ENTITY);

        if (entity instanceof ServerPlayerEntity player) {

            // 3. Pega os dados da Skill
            PlayerSkillData playerData = player
                    .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
            var minerStats = playerData.getSkill(MurilloSkillsList.MINER);
            int minerLevel = minerStats.level;
            float prestigeMultiplier = PrestigeManager.getPassiveMultiplier(minerStats.prestige);

            // 4. Calcula o bônus (level * taxa * multiplicador) + bônus flat por prestígio
            // Ex: Nível 100, taxa 0.03, prestígio 0 -> +3 Fortuna
            // Ex: Nível 100, taxa 0.03, prestígio 10, fortunePerPrestige 0.5 -> +3 + 5 = +8 Fortuna
            int bonusFortune = (int) (minerLevel * SkillConfig.getMinerFortunePerLevel() * prestigeMultiplier
                    + minerStats.prestige * SkillConfig.getMinerFortunePerPrestige());

            if (bonusFortune > 0) {
                // Retorna o nível original + o nosso bônus
                return originalLevel + bonusFortune;
            }
        }

        return originalLevel;
    }
}
