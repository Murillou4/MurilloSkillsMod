package com.murilloskills.forge112.impl;

import com.murilloskills.core.compat.CrossModCompatRules;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.api.AbstractSkill;
import com.murilloskills.forge112.data.PlayerRuntime;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;

import static com.murilloskills.forge112.MurilloSkillsForge112.*;
import static com.murilloskills.forge112.skills.Forge112Passives.applyAttribute;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.activateTimed;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.isTimedActive;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.repairEquippedGear;
import static com.murilloskills.forge112.utils.Forge112EnvironmentEffects.accelerateNearbyFurnaces;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.*;
import static com.murilloskills.forge112.utils.Forge112SkillMath.getBlacksmithDamageMultiplier;

public final class BlacksmithSkill extends AbstractSkill {
    @Override
    public SkillType getSkillType() {
        return SkillType.BLACKSMITH;
    }

    @Override
    public void applyPassives(EntityPlayer player, PlayerSkillDataCore data) {
        boolean selected = isSelected(data, SkillType.BLACKSMITH);
        SkillStatsCore stats = data.getSkill(SkillType.BLACKSMITH);
        applyAttribute(player, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, BLACKSMITH_KNOCKBACK,
                "MurilloSkills blacksmith knockback", selected && stats.getLevel() >= BLACKSMITH_THORNS_LEVEL ? 0.50D : 0.0D, 0);
        applyAttribute(player, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, BLACKSMITH_TITANIUM_KNOCKBACK,
                "MurilloSkills titanium knockback", isTimedActive(player, TITANIUM_AURA_UNTIL) ? 1.0D : 0.0D, 0);
        if (selected && stats.getLevel() >= BLACKSMITH_FIRE_MASTERY_LEVEL) {
            player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 80, 0, true, false));
        }
    }

    @Override
    public void onTick(EntityPlayer player, PlayerSkillDataCore data, PlayerRuntime runtime) {
        if (runtime.ticks % 80 == 0 && isSelected(data, SkillType.BLACKSMITH)) {
            accelerateNearbyFurnaces(player, data);
        }
    }

    @Override
    public void onActiveAbility(EntityPlayer player, PlayerSkillDataCore data, SkillStatsCore stats) {
        activateTimed(player, TITANIUM_AURA_UNTIL, BLACKSMITH_ABILITY_DURATION_SECONDS, "Titanium Aura");
        player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, BLACKSMITH_ABILITY_DURATION_SECONDS * 20, 0, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, BLACKSMITH_ABILITY_DURATION_SECONDS * 20, 1, false, true));
        int repaired = repairEquippedGear(player, 80);
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 0.80F, 1.00F);
        say(player, "MurilloSkills Blacksmith: Titanium Aura ativo. Durabilidade reparada: " + repaired + ".");
    }

    @Override
    public void onIncomingDamage(LivingHurtEvent event, EntityPlayer player, PlayerSkillDataCore data, DamageSource source) {
        if (!isSelected(data, SkillType.BLACKSMITH)) {
            return;
        }
        float amount = event.getAmount() * getBlacksmithDamageMultiplier(player, data.getSkill(SkillType.BLACKSMITH), source);
        if (amount != event.getAmount()) {
            event.setAmount(amount);
            LOG.info("[MurilloSkills][1.12.2][Blacksmith] Reduced damage for {} to {}", player.getName(), amount);
        }
    }

    @Override
    public void onCraft(ItemCraftedEvent event, String itemId) {
        if (CrossModCompatRules.builderCategory(itemId) == CrossModCompatRules.BuilderCategory.NONE) {
            addXp(event.player, SkillType.BLACKSMITH, 18, "craft " + itemId);
        }
    }

    @Override
    public void onSmelt(ItemSmeltedEvent event, String itemId) {
        addXp(event.player, SkillType.BLACKSMITH, 24, "smelt " + itemId);
    }

    @Override
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event, EntityPlayer player, PlayerSkillDataCore data, String blockId) {
        if (CrossModCompatRules.isLikelyMachineIdOrClass(blockId)) {
            addXp(player, SkillType.BLACKSMITH, 10, "machine interact " + blockId);
        }
    }
}
