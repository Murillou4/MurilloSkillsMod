package com.murilloskills.forge112.impl;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.api.AbstractSkill;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.player.ItemFishedEvent;

import static com.murilloskills.forge112.MurilloSkillsForge112.*;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.activateTimed;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.isTimedActive;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.*;
import static com.murilloskills.forge112.utils.Forge112SkillMath.getFisherBundleChance;

public final class FisherSkill extends AbstractSkill {
    @Override
    public SkillType getSkillType() {
        return SkillType.FISHER;
    }

    @Override
    public void applyPassives(EntityPlayer player, PlayerSkillDataCore data) {
        if (!isSelected(data, SkillType.FISHER)) {
            return;
        }
        int level = data.getSkill(SkillType.FISHER).getLevel();
        if (level >= FISHER_OCEAN_BLESSING_LEVEL && player.isInWater()) {
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 120, 0, true, false));
        }
        if (level >= FISHER_DOLPHIN_GRACE_LEVEL && player.isInWater()) {
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 60, 0, true, false));
        }
        if (level >= FISHER_SEAS_FORTUNE_LEVEL || isTimedActive(player, RAIN_DANCE_UNTIL)) {
            player.addPotionEffect(new PotionEffect(MobEffects.LUCK, 80, isTimedActive(player, RAIN_DANCE_UNTIL) ? 2 : 0, true, false));
        }
    }

    @Override
    public void onActiveAbility(EntityPlayer player, PlayerSkillDataCore data, SkillStatsCore stats) {
        activateTimed(player, RAIN_DANCE_UNTIL, FISHER_ABILITY_DURATION_SECONDS, "Rain Dance");
        player.addPotionEffect(new PotionEffect(MobEffects.LUCK, FISHER_ABILITY_DURATION_SECONDS * 20, 2, false, true));
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.WEATHER_RAIN, SoundCategory.WEATHER, 0.80F, 1.00F);
        say(player, "MurilloSkills Fisher: Rain Dance ativo. Pesca recebe sorte e drops extras.");
    }

    @Override
    public void onFish(ItemFishedEvent event, EntityPlayer player, PlayerSkillDataCore data) {
        addXp(player, SkillType.FISHER, 45 + event.getDrops().size() * 10, "fishing");
        if (!isSelected(data, SkillType.FISHER)) {
            return;
        }
        double chance = getFisherBundleChance(data.getSkill(SkillType.FISHER));
        if (isTimedActive(player, RAIN_DANCE_UNTIL)) {
            chance += 0.30D;
        }
        if (RANDOM.nextDouble() <= chance) {
            int multiplier = isTimedActive(player, RAIN_DANCE_UNTIL) ? 2 : 1;
            event.getDrops().add(new ItemStack(Items.FISH, multiplier * (1 + RANDOM.nextInt(2))));
            event.damageRodBy(-1);
            LOG.info("[MurilloSkills][1.12.2][Fisher] Bonus fish/drop roll for {}", player.getName());
        }
    }
}
