package com.murilloskills.mixin;

import com.murilloskills.skills.MinerFortuneHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.loot.condition.TableBonusLootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TableBonusLootCondition.class)
public class TableBonusLootConditionMixin {
    @Shadow
    @Final
    private RegistryEntry<Enchantment> enchantment;

    @ModifyVariable(method = "test", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private int addSkillFortuneLevel(int originalLevel, LootContext context) {
        if (!this.enchantment.matchesKey(Enchantments.FORTUNE)) {
            return originalLevel;
        }
        return MinerFortuneHandler.addSkillFortuneToVanillaLevel(originalLevel, context);
    }
}
