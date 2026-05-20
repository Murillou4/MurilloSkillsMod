package com.murilloskills.forge112.api;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.data.PlayerRuntime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;

import static com.murilloskills.forge112.utils.Forge112PlayerServices.say;

public abstract class AbstractSkill {
    public abstract SkillType getSkillType();

    public void onPlayerJoin(EntityPlayer player, PlayerSkillDataCore data) {
        applyPassives(player, data);
    }

    public void applyPassives(EntityPlayer player, PlayerSkillDataCore data) {
    }

    public void onTick(EntityPlayer player, PlayerSkillDataCore data, PlayerRuntime runtime) {
    }

    public void onActiveAbility(EntityPlayer player, PlayerSkillDataCore data, SkillStatsCore stats) {
        say(player, "MurilloSkills: " + getSkillType().name().toLowerCase() + " ainda nao possui habilidade ativa.");
    }

    public void onBreakSpeed(PlayerEvent.BreakSpeed event, EntityPlayer player, PlayerSkillDataCore data) {
    }

    public void onBlockBreak(BlockEvent.BreakEvent event, EntityPlayer player, PlayerSkillDataCore data, String blockId) {
    }

    public void onBlockPlace(BlockEvent.PlaceEvent event, EntityPlayer player, PlayerSkillDataCore data, String blockId) {
    }

    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event, EntityPlayer player, PlayerSkillDataCore data, String blockId) {
    }

    public void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
    }

    public void onIncomingDamage(LivingHurtEvent event, EntityPlayer player, PlayerSkillDataCore data, DamageSource source) {
    }

    public void onOutgoingDamage(LivingHurtEvent event, EntityPlayer attacker, PlayerSkillDataCore data, boolean arrow) {
    }

    public void onLivingDeath(LivingDeathEvent event) {
    }

    public void onAttackEntity(AttackEntityEvent event, EntityPlayer player) {
    }

    public double getFallDistanceReduction(PlayerSkillDataCore data) {
        return 0.0D;
    }

    public void onFall(LivingFallEvent event, EntityPlayer player, PlayerSkillDataCore data) {
    }

    public void onArrowJoin(EntityJoinWorldEvent event, EntityArrow arrow, EntityPlayer shooter, PlayerSkillDataCore data) {
    }

    public void onCraft(ItemCraftedEvent event, String itemId) {
    }

    public void onSmelt(ItemSmeltedEvent event, String itemId) {
    }

    public void onFish(ItemFishedEvent event, EntityPlayer player, PlayerSkillDataCore data) {
    }

    public void onAnvilUpdate(AnvilUpdateEvent event) {
    }

    public void onEnchantmentLevelSet(EnchantmentLevelSetEvent event) {
    }

    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event, EntityPlayer player, PlayerSkillDataCore data, String blockId) {
    }

    public void onDimensionChanged(PlayerChangedDimensionEvent event) {
    }
}
