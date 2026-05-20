package com.murilloskills.forge112.events;

import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.forge112.api.AbstractSkill;
import com.murilloskills.forge112.api.SkillRegistry;
import com.murilloskills.forge112.data.PlayerRuntime;
import com.murilloskills.forge112.utils.Forge112DailyChallengeManager;
import com.murilloskills.forge112.utils.Forge112FirstTimeHints;
import com.murilloskills.forge112.utils.Forge112MiningTools;
import com.murilloskills.forge112.utils.Forge112SkillSynergyManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
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
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import static com.murilloskills.forge112.MurilloSkillsForge112.*;
import static com.murilloskills.forge112.dev.Forge112SelfTest.runSelfTest;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.tickTimedAbilities;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.*;

public final class Forge112SkillEvents {
    @SubscribeEvent
    public void onLogin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        PlayerSkillDataCore data = data(event.player);
        SkillRegistry.onPlayerJoin(event.player, data);
        LOG.info("[MurilloSkills][1.12.2] Player login: {} selected={} paragon={}",
                event.player.getName(), data.getSelectedSkills(), data.getActiveParagonSkill());
        Forge112FirstTimeHints.onLogin(event.player);
        Forge112DailyChallengeManager.sync(event.player);
        if (Boolean.getBoolean("murilloskills.selftest")) {
            runSelfTest(event.player);
        }
    }

    @SubscribeEvent
    public void onLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        STORE.unload(event.player.getUniqueID());
        RUNTIME.remove(event.player.getUniqueID());
        Forge112MiningTools.clearUltmineState(event.player);
        LOG.info("[MurilloSkills][1.12.2] Player logout: {}", event.player.getName());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player == null || event.player.world.isRemote) {
            return;
        }
        PlayerRuntime runtime = runtime(event.player);
        runtime.ticks++;
        PlayerSkillDataCore data = data(event.player);
        Forge112MiningTools.tickUltmineJob(event.player);
        Forge112MiningTools.tickUltmineConfigEffects(event.player);
        tickTimedAbilities(event.player, data, runtime);
        SkillRegistry.tick(event.player, data, runtime);
        if (runtime.ticks % 100 == 0) {
            double dx = event.player.posX - runtime.lastX;
            double dy = event.player.posY - runtime.lastY;
            double dz = event.player.posZ - runtime.lastZ;
            if (dx * dx + dy * dy + dz * dz >= 64.0D) {
                Forge112DailyChallengeManager.record(event.player, com.murilloskills.core.config.SkillType.EXPLORER,
                        "travel", 1);
            }
            runtime.lastX = event.player.posX;
            runtime.lastY = event.player.posY;
            runtime.lastZ = event.player.posZ;
        }
        if (runtime.ticks % 20 == 0) {
            SkillRegistry.applyPassives(event.player, data);
        }
        if (runtime.ticks % 80 == 0) {
            STORE.saveIfDirty(event.player.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onBreakSpeed(event, player, data);
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        Forge112MiningTools.recordUltmineTargetFace(player, event.getPos(), event.getFace());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        String blockId = blockId(event.getState());
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onBlockBreak(event, player, data, blockId);
        }
        if (isCropLike(blockId)) {
            Forge112DailyChallengeManager.record(player, com.murilloskills.core.config.SkillType.FARMER, "harvest", 1);
        } else {
            Forge112DailyChallengeManager.record(player, com.murilloskills.core.config.SkillType.MINER, "break", 1);
        }
        int mined = Forge112MiningTools.handleUltmineBreak(player, event.getPos(), event.getState());
        if (mined > 0) {
            addXp(player, com.murilloskills.core.config.SkillType.MINER, mined * 6, "ultmine " + blockId);
            LOG.info("[MurilloSkills][1.12.2][Ultmine] {} mined {} extra blocks.", player.getName(), mined);
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        String blockId = blockId(event.getPlacedBlock());
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onBlockPlace(event, player, data, blockId);
        }
        Forge112DailyChallengeManager.record(player, com.murilloskills.core.config.SkillType.BUILDER, "place", 1);
    }

    @SubscribeEvent
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        EntityPlayer player = event.getHarvester();
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        String blockId = blockId(event.getState());
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onHarvestDrops(event, player, data, blockId);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onCropGrow(event);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase target = event.getEntityLiving();
        DamageSource source = event.getSource();

        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;
            PlayerSkillDataCore data = data(player);
            for (AbstractSkill skill : SkillRegistry.values()) {
                skill.onIncomingDamage(event, player, data, source);
            }
        }

        EntityPlayer attacker = null;
        boolean arrow = false;
        if (source.getImmediateSource() instanceof EntityArrow) {
            EntityArrow arrowEntity = (EntityArrow) source.getImmediateSource();
            if (arrowEntity.shootingEntity instanceof EntityPlayer) {
                attacker = (EntityPlayer) arrowEntity.shootingEntity;
                arrow = true;
            }
        } else {
            Entity trueSource = source.getTrueSource();
            if (trueSource instanceof EntityPlayer) {
                attacker = (EntityPlayer) trueSource;
            }
        }
        if (attacker == null || attacker.world.isRemote) {
            return;
        }
        PlayerSkillDataCore attackerData = data(attacker);
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onOutgoingDamage(event, attacker, attackerData, arrow);
        }
        event.setAmount(event.getAmount() * Forge112SkillSynergyManager.outgoingDamageMultiplier(attackerData, arrow));
        if (arrow) {
            Forge112DailyChallengeManager.record(attacker, com.murilloskills.core.config.SkillType.ARCHER, "shot", 1);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onLivingDeath(event);
        }
        Entity trueSource = event.getSource().getTrueSource();
        if (trueSource instanceof EntityPlayer && !trueSource.world.isRemote) {
            Forge112DailyChallengeManager.record((EntityPlayer) trueSource,
                    com.murilloskills.core.config.SkillType.WARRIOR, "kill", 1);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onAttackEntity(event, event.getEntityPlayer());
        }
    }

    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        PlayerSkillDataCore data = data(player);
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onFall(event, player, data);
        }
        double reduction = Math.min(0.90D, SkillRegistry.getFallDistanceReduction(data));
        if (reduction <= 0.0D) {
            return;
        }
        event.setDistance((float) (event.getDistance() * (1.0D - reduction)));
        event.setDamageMultiplier((float) (event.getDamageMultiplier() * (1.0D - reduction)));
    }

    @SubscribeEvent
    public void onArrowJoin(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof EntityArrow) || event.getWorld().isRemote) {
            return;
        }
        EntityArrow arrow = (EntityArrow) event.getEntity();
        if (!(arrow.shootingEntity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) arrow.shootingEntity;
        PlayerSkillDataCore data = data(player);
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onArrowJoin(event, arrow, player, data);
        }
    }

    @SubscribeEvent
    public void onCraft(ItemCraftedEvent event) {
        if (event.player == null || event.player.world.isRemote) {
            return;
        }
        String id = itemId(event.crafting);
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onCraft(event, id);
        }
        Forge112DailyChallengeManager.record(event.player, com.murilloskills.core.config.SkillType.BLACKSMITH, "craft", 1);
    }

    @SubscribeEvent
    public void onSmelt(ItemSmeltedEvent event) {
        if (event.player == null || event.player.world.isRemote) {
            return;
        }
        String id = itemId(event.smelting);
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onSmelt(event, id);
        }
        Forge112DailyChallengeManager.record(event.player, com.murilloskills.core.config.SkillType.BLACKSMITH, "smelt", 1);
    }

    @SubscribeEvent
    public void onFish(ItemFishedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world.isRemote) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onFish(event, player, data);
        }
        Forge112DailyChallengeManager.record(player, com.murilloskills.core.config.SkillType.FISHER, "fish", 1);
    }

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onAnvilUpdate(event);
        }
    }

    @SubscribeEvent
    public void onEnchantmentLevelSet(EnchantmentLevelSetEvent event) {
        if (event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onEnchantmentLevelSet(event);
        }
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world.isRemote || !Forge112MiningTools.isLoadedBlock(player.world, event.getPos())) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        String blockId = blockId(player.world.getBlockState(event.getPos()));
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onRightClickBlock(event, player, data, blockId);
        }
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerChangedDimensionEvent event) {
        for (AbstractSkill skill : SkillRegistry.values()) {
            skill.onDimensionChanged(event);
        }
        Forge112DailyChallengeManager.record(event.player, com.murilloskills.core.config.SkillType.EXPLORER, "dimension", 1);
    }

    private boolean isCropLike(String blockId) {
        if (blockId == null) {
            return false;
        }
        return blockId.contains("crop") || blockId.contains("wheat") || blockId.contains("carrot")
                || blockId.contains("potato") || blockId.contains("beetroot") || blockId.contains("cactus")
                || blockId.contains("reeds") || blockId.contains("pumpkin") || blockId.contains("melon");
    }
}
