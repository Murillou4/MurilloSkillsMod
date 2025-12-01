package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
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
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            int minerLevel = state.getPlayerData(player).getSkill(MurilloSkillsList.MINER).level;

            // 4. Calcula o bônus (0.1 por nível)
            // Ex: Nível 30 * 0.1 = 3.0 -> +3 Fortuna
            int bonusFortune = (int) (minerLevel * 0.03);

            if (bonusFortune > 0) {
                // Retorna o nível original + o nosso bônus
                return originalLevel + bonusFortune;
            }
        }

        return originalLevel;
    }
}