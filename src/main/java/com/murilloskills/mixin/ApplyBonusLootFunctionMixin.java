package com.murilloskills.mixin;

import com.murilloskills.skills.MinerFortuneHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.registry.entry.RegistryEntry;
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

        return MinerFortuneHandler.addSkillFortuneToVanillaLevel(originalLevel, context);
    }
}
