package com.murilloskills.forge112.dev;

import com.murilloskills.forge112.MurilloSkillsForge112;
import com.murilloskills.forge112.client.*;
import com.murilloskills.forge112.client.config.*;
import com.murilloskills.forge112.client.gui.*;
import com.murilloskills.forge112.client.input.*;
import com.murilloskills.forge112.client.render.*;
import com.murilloskills.forge112.commands.*;
import com.murilloskills.forge112.config.*;
import com.murilloskills.forge112.data.*;
import com.murilloskills.forge112.dev.*;
import com.murilloskills.forge112.events.*;
import com.murilloskills.forge112.skills.*;
import com.murilloskills.forge112.utils.*;
import static com.murilloskills.forge112.MurilloSkillsForge112.*;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.*;
import static com.murilloskills.forge112.skills.Forge112Abilities.*;
import static com.murilloskills.forge112.skills.Forge112Passives.*;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.*;
import static com.murilloskills.forge112.utils.Forge112EnvironmentEffects.*;
import static com.murilloskills.forge112.utils.Forge112MiningTools.*;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.*;
import static com.murilloskills.forge112.utils.Forge112SkillMath.*;

import com.google.gson.JsonElement;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.murilloskills.core.compat.CrossModCompatRules;
import com.murilloskills.core.config.SkillProgressionConfig;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.core.data.XpAddResult;
import com.murilloskills.core.io.PlayerSkillJsonCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class Forge112SelfTest {
    private Forge112SelfTest() {
    }

    public static void runSelfTest(EntityPlayer player) {
        try {
            PlayerSkillDataCore data = data(player);
            data.setSelectedSkillsDirect(new ArrayList<SkillType>(Arrays.asList(SkillType.values())));
            data.clearAllParagonSkills();
            data.setToggle(SkillType.EXPLORER, "speed_boost", true);
            data.setToggle(SkillType.EXPLORER, "step_assist", true);
            data.setToggle(SkillType.EXPLORER, "night_vision", true);
            data.setToggle(SkillType.FARMER, "area_planting", true);
            data.setToggle(SkillType.BUILDER, "hollow_fill", true);
            data.setToggle(SkillType.BUILDER, "ultplace", true);
            data.setToggle(SkillType.BUILDER, "ultplace_config", true);
            data.setToggle(SkillType.BUILDER, "fill_mode", true);
            data.setToggle(SkillType.MINER, "auto_torch", true);
            data.setToggle(SkillType.MINER, "ultmine_hold", true);
            data.setToggle(SkillType.MINER, "ultmine_drops", true);
            data.setToggle(SkillType.MINER, "ultmine_menu", true);
            data.setToggle(SkillType.BLACKSMITH, "melting_touch", true);
            data.setToggle(SkillType.ARCHER, "focus", false);
            for (SkillType skill : SkillType.values()) {
                data.setSkill(skill, 100, 0.0D, -1L, 1);
                if (skill.isMasterClass() && data.getMasterParagonSkill() == null) {
                    data.activateParagonSkill(skill);
                }
                if (!data.isSkillSelected(skill)) {
                    throw new IllegalStateException("selection failed for " + skill);
                }
                if (passiveScale(data, skill, 0.01D, 0.05D) <= 0.0D) {
                    throw new IllegalStateException("passive scale failed for " + skill);
                }
            }
            SelfTestFixture fixture = prepareSelfTestWorld(player);
            validateToggleStates(data);
            validatePassiveFormulaSurfaces(player, data);
            applyAllPassives(player, data, true);
            requireModifier(player, SharedMonsterAttributes.ATTACK_DAMAGE, WARRIOR_DAMAGE, "warrior damage");
            requireModifier(player, SharedMonsterAttributes.MAX_HEALTH, WARRIOR_HEALTH, "warrior health");
            requireModifier(player, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, BLACKSMITH_KNOCKBACK, "blacksmith knockback");
            requireModifier(player, SharedMonsterAttributes.MOVEMENT_SPEED, EXPLORER_SPEED, "explorer speed");
            requireModifier(player, SharedMonsterAttributes.LUCK, EXPLORER_LUCK, "explorer luck");
            IAttribute reach = reachAttribute();
            if (reach == null) {
                throw new IllegalStateException("builder reach attribute missing on Forge 1.12.2");
            }
            requireModifier(player, reach, BUILDER_REACH, "builder reach");
            if (!player.isPotionActive(MobEffects.HASTE)) {
                throw new IllegalStateException("miner haste passive missing");
            }
            if (!player.isPotionActive(MobEffects.FIRE_RESISTANCE)) {
                throw new IllegalStateException("blacksmith fire passive missing");
            }
            if (player.stepHeight < 1.0F) {
                throw new IllegalStateException("explorer step assist missing");
            }
            requirePotion(player, MobEffects.LUCK, "fisher luck passive");
            requirePotion(player, MobEffects.REGENERATION, "farmer/explorer regeneration passive");
            requirePotion(player, MobEffects.RESISTANCE, "warrior resistance passive");
            for (SkillType skill : SkillType.values()) {
                SkillStatsCore stats = data.getSkill(skill);
                stats.setLastAbilityUse(-1L);
                triggerAbility(player, skill);
                if (stats.getLastAbilityUse() <= 0L) {
                    throw new IllegalStateException("ability cooldown not updated for " + skill);
                }
                requireTimedAbility(player, skill);
                validatePracticalAbilityEffect(player, data, skill, fixture);
            }
            validatePracticalCombatAndDamageSurfaces(player, data);
            STORE.save(player.getUniqueID());
            writeSelfTestResult("PASS");
            LOG.info("[MurilloSkills][1.12.2][SelfTest] PASS");
            say(player, "MurilloSkills 1.12.2 selftest PASS");
        } catch (Throwable error) {
            writeSelfTestResult("FAIL: " + error.getMessage());
            LOG.error("[MurilloSkills][1.12.2][SelfTest] FAIL", error);
            say(player, "MurilloSkills 1.12.2 selftest FAIL: " + error.getMessage());
        }
    }

    public static void requireTimedAbility(EntityPlayer player, SkillType skill) {
        Map<UUID, Long> state;
        switch (skill) {
            case MINER:
                state = MINER_VISION_UNTIL;
                break;
            case WARRIOR:
                state = BERSERK_UNTIL;
                break;
            case ARCHER:
                state = MASTER_RANGER_UNTIL;
                break;
            case FARMER:
                state = HARVEST_MOON_UNTIL;
                break;
            case FISHER:
                state = RAIN_DANCE_UNTIL;
                break;
            case BLACKSMITH:
                state = TITANIUM_AURA_UNTIL;
                break;
            case BUILDER:
                state = CREATIVE_BRUSH_UNTIL;
                break;
            case EXPLORER:
                state = TREASURE_HUNTER_UNTIL;
                break;
            default:
                state = null;
        }
        if (state == null || !isTimedActive(player, state)) {
            throw new IllegalStateException("timed ability state missing for " + skill);
        }
    }

    public static SelfTestFixture prepareSelfTestWorld(EntityPlayer player) {
        World world = player.world;
        BlockPos base = player.getPosition().add(5, 0, 5);
        for (BlockPos pos : BlockPos.getAllInBox(base.add(-2, 0, -2), base.add(8, 2, 8))) {
            world.setBlockToAir(pos);
        }
        BlockPos orePos = base;
        BlockPos chestPos = base.add(2, 0, 0);
        BlockPos cropPos = base.add(0, 0, 2);
        BlockPos brushOrigin = base.add(5, 3, 0);
        world.setBlockState(orePos, Blocks.DIAMOND_ORE.getDefaultState(), 3);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 3);
        world.setBlockState(cropPos.down(), Blocks.FARMLAND.getDefaultState(), 3);
        BlockCrops wheat = (BlockCrops) Blocks.WHEAT;
        world.setBlockState(cropPos, wheat.withAge(wheat.getMaxAge()), 3);
        for (BlockPos pos : BlockPos.getAllInBox(brushOrigin.add(-1, 0, -1), brushOrigin.add(1, 0, 1))) {
            world.setBlockToAir(pos);
        }
        ItemStack damagedTool = new ItemStack(Items.IRON_PICKAXE);
        damagedTool.setItemDamage(120);
        player.setHeldItem(EnumHand.MAIN_HAND, damagedTool);
        LOG.info("[MurilloSkills][1.12.2][SelfTest] Practical fixtures ore={} chest={} crop={} brush={} toolDamage={}",
                orePos, chestPos, cropPos, brushOrigin, damagedTool.getItemDamage());
        return new SelfTestFixture(orePos.toImmutable(), chestPos.toImmutable(), cropPos.toImmutable(),
                brushOrigin.toImmutable(), damagedTool, 120);
    }

    public static void validateToggleStates(PlayerSkillDataCore data) {
        requireToggle(data, SkillType.EXPLORER, "speed_boost", true);
        requireToggle(data, SkillType.EXPLORER, "step_assist", true);
        requireToggle(data, SkillType.EXPLORER, "night_vision", true);
        requireToggle(data, SkillType.FARMER, "area_planting", true);
        requireToggle(data, SkillType.BUILDER, "hollow_fill", true);
        requireToggle(data, SkillType.BUILDER, "ultplace", true);
        requireToggle(data, SkillType.BUILDER, "ultplace_config", true);
        requireToggle(data, SkillType.BUILDER, "fill_mode", true);
        requireToggle(data, SkillType.MINER, "auto_torch", true);
        requireToggle(data, SkillType.MINER, "ultmine_hold", true);
        requireToggle(data, SkillType.MINER, "ultmine_drops", true);
        requireToggle(data, SkillType.MINER, "ultmine_menu", true);
        requireToggle(data, SkillType.BLACKSMITH, "melting_touch", true);
        LOG.info("[MurilloSkills][1.12.2][SelfTest] Toggle states PASS");
    }

    public static void validatePassiveFormulaSurfaces(EntityPlayer player, PlayerSkillDataCore data) {
        requirePositive(getMinerSpeedBonus(data.getSkill(SkillType.MINER)), "miner mining speed fallback");
        requirePositive(getWarriorDamageBonus(data.getSkill(SkillType.WARRIOR)), "warrior damage formula");
        requirePositive(getWarriorHealthBonus(data.getSkill(SkillType.WARRIOR)), "warrior health formula");
        if (getArcherDamageMultiplier(data.getSkill(SkillType.ARCHER)) <= 1.0D) {
            throw new IllegalStateException("archer damage multiplier formula failed");
        }
        requirePositive(getFarmerDoubleHarvestChance(data.getSkill(SkillType.FARMER)), "farmer double harvest formula");
        requirePositive(getFarmerFertileGrowthChance(data.getSkill(SkillType.FARMER)), "farmer fertile growth formula");
        requirePositive(getFisherBundleChance(data.getSkill(SkillType.FISHER)), "fisher bundle formula");
        if (getBlacksmithDamageMultiplier(player, data.getSkill(SkillType.BLACKSMITH), DamageSource.IN_FIRE) >= 1.0F) {
            throw new IllegalStateException("blacksmith fire resistance multiplier failed");
        }
        requirePositive(getExplorerSpeedBonus(data.getSkill(SkillType.EXPLORER), false), "explorer speed formula");
        requirePositive(getBuilderReachBonus(data.getSkill(SkillType.BUILDER)), "builder reach formula");
        LOG.info("[MurilloSkills][1.12.2][SelfTest] Passive formulas PASS");
    }

    public static void validatePracticalAbilityEffect(EntityPlayer player, PlayerSkillDataCore data, SkillType skill,
            SelfTestFixture fixture) {
        UUID id = player.getUniqueID();
        switch (skill) {
            case MINER:
                requireContains(MINER_VISIBLE_ORES.get(id), fixture.orePos, "miner ore scan");
                requirePotion(player, MobEffects.NIGHT_VISION, "miner Master Miner night vision");
                break;
            case WARRIOR:
                requirePotion(player, MobEffects.STRENGTH, "warrior Berserk strength");
                requirePotion(player, MobEffects.SPEED, "warrior Berserk speed");
                requirePotion(player, MobEffects.RESISTANCE, "warrior Berserk resistance");
                applyAllPassives(player, data, false);
                requireModifier(player, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, WARRIOR_BERSERK_KNOCKBACK,
                        "warrior berserk knockback");
                break;
            case ARCHER:
                requireToggle(data, SkillType.ARCHER, "focus", true);
                requirePotion(player, MobEffects.SPEED, "archer Master Ranger speed");
                break;
            case FARMER:
                IBlockState cropState = player.world.getBlockState(fixture.cropPos);
                if (!(cropState.getBlock() instanceof BlockCrops) || ((BlockCrops) cropState.getBlock()).isMaxAge(cropState)) {
                    throw new IllegalStateException("farmer Harvest Moon did not reset mature crop");
                }
                break;
            case FISHER:
                PotionEffect luck = player.getActivePotionEffect(MobEffects.LUCK);
                if (luck == null || luck.getAmplifier() < 2) {
                    throw new IllegalStateException("fisher Rain Dance luck amplifier missing");
                }
                break;
            case BLACKSMITH:
                if (fixture.damagedTool.getItemDamage() >= fixture.initialToolDamage) {
                    throw new IllegalStateException("blacksmith Titanium Aura did not repair held gear");
                }
                requirePotion(player, MobEffects.FIRE_RESISTANCE, "blacksmith Titanium Aura fire resistance");
                requirePotion(player, MobEffects.RESISTANCE, "blacksmith Titanium Aura resistance");
                applyAllPassives(player, data, false);
                requireModifier(player, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, BLACKSMITH_TITANIUM_KNOCKBACK,
                        "blacksmith titanium knockback");
                break;
            case BUILDER:
                int placed = creativeBrushFill(player, player.world, fixture.brushOrigin, Blocks.STONE.getDefaultState());
                if (placed < 8) {
                    throw new IllegalStateException("builder Creative Brush placed too few blocks: " + placed);
                }
                requirePotion(player, MobEffects.RESISTANCE, "builder Creative Brush resistance");
                break;
            case EXPLORER:
                requireContains(TREASURE_VISIBLE_TARGETS.get(id), fixture.chestPos, "explorer treasure scan");
                requirePotion(player, MobEffects.NIGHT_VISION, "explorer Treasure Hunter night vision");
                requirePotion(player, MobEffects.SPEED, "explorer Treasure Hunter speed");
                requirePotion(player, MobEffects.WATER_BREATHING, "explorer Treasure Hunter water breathing");
                break;
            default:
                break;
        }
        LOG.info("[MurilloSkills][1.12.2][SelfTest] Practical ability PASS {}", skill);
    }

    public static void validatePracticalCombatAndDamageSurfaces(EntityPlayer player, PlayerSkillDataCore data) {
        Forge112SkillEvents hooks = new Forge112SkillEvents();
        net.minecraft.entity.monster.EntityZombie target = new net.minecraft.entity.monster.EntityZombie(player.world);

        float healthBefore = Math.max(1.0F, Math.min(player.getMaxHealth() - 2.0F, 8.0F));
        player.setHealth(healthBefore);
        LivingHurtEvent melee = new LivingHurtEvent(target, DamageSource.causePlayerDamage(player), 5.0F);
        hooks.onLivingHurt(melee);
        if (melee.getAmount() <= 5.0F) {
            throw new IllegalStateException("warrior melee damage practical event failed");
        }
        if (player.getHealth() <= healthBefore) {
            throw new IllegalStateException("warrior lifesteal practical event failed");
        }

        data.setToggle(SkillType.ARCHER, "focus", true);
        net.minecraft.entity.projectile.EntityTippedArrow arrow =
                new net.minecraft.entity.projectile.EntityTippedArrow(player.world, player);
        arrow.shootingEntity = player;
        LivingHurtEvent arrowDamage = new LivingHurtEvent(target, DamageSource.causeArrowDamage(arrow, player), 4.0F);
        hooks.onLivingHurt(arrowDamage);
        if (arrowDamage.getAmount() <= 4.0F) {
            throw new IllegalStateException("archer arrow damage practical event failed");
        }
        if (data.getToggle(SkillType.ARCHER, "focus", false)) {
            throw new IllegalStateException("archer focus practical event was not consumed");
        }

        net.minecraft.entity.projectile.EntityTippedArrow speedArrow =
                new net.minecraft.entity.projectile.EntityTippedArrow(player.world, player);
        speedArrow.shootingEntity = player;
        speedArrow.motionX = 1.0D;
        speedArrow.motionY = 0.0D;
        speedArrow.motionZ = 0.0D;
        hooks.onArrowJoin(new EntityJoinWorldEvent(speedArrow, player.world));
        if (speedArrow.motionX <= 1.0D || !speedArrow.getIsCritical()) {
            throw new IllegalStateException("archer arrow speed/critical practical event failed");
        }

        LivingHurtEvent defense = new LivingHurtEvent(player, DamageSource.FALL, 20.0F);
        hooks.onLivingHurt(defense);
        if (defense.getAmount() >= 20.0F) {
            throw new IllegalStateException("defensive damage reduction practical event failed");
        }
        LivingFallEvent fall = new LivingFallEvent(player, 20.0F, 1.0F);
        hooks.onFall(fall);
        if (fall.getDistance() >= 20.0F || fall.getDamageMultiplier() >= 1.0F) {
            throw new IllegalStateException("builder/explorer fall reduction practical event failed");
        }
        LOG.info("[MurilloSkills][1.12.2][SelfTest] Practical combat/passive events PASS");
    }

    public static void requireToggle(PlayerSkillDataCore data, SkillType skill, String toggle, boolean expected) {
        boolean value = data.getToggle(skill, toggle, !expected);
        if (value != expected) {
            throw new IllegalStateException("toggle " + skill + "." + toggle + " expected " + expected + " but was " + value);
        }
    }

    public static void requirePositive(double value, String label) {
        if (value <= 0.0D) {
            throw new IllegalStateException(label + " expected positive value, got " + value);
        }
    }

    public static void requirePotion(EntityPlayer player, net.minecraft.potion.Potion potion, String label) {
        if (!player.isPotionActive(potion)) {
            throw new IllegalStateException(label + " potion missing");
        }
    }

    public static void requireContains(List<BlockPos> positions, BlockPos expected, String label) {
        if (positions == null || !positions.contains(expected)) {
            throw new IllegalStateException(label + " missing " + expected + " in " + positions);
        }
    }

    public static void requireModifier(EntityPlayer player, IAttribute attribute, UUID modifierId, String label) {
        IAttributeInstance instance = player.getEntityAttribute(attribute);
        if (instance == null) {
            throw new IllegalStateException(label + " attribute instance missing");
        }
        if (instance.getModifier(modifierId) == null) {
            throw new IllegalStateException(label + " modifier missing");
        }
    }

    public static void writeSelfTestResult(String line) {
        try {
            if (STORE.root != null) {
                Files.createDirectories(STORE.root);
                Files.write(STORE.root.resolve("selftest-result.txt"), Collections.singletonList(line), StandardCharsets.UTF_8);
            }
        } catch (IOException error) {
            LOG.warn("[MurilloSkills][1.12.2][SelfTest] Could not write result file: {}", error.toString());
        }
    }
}
