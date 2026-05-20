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
import net.minecraft.init.Enchantments;
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
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerRepair;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
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
            validateUltmineWithoutMinerSelection(player, data, fixture);
            validateToggleStates(data);
            validatePassiveFormulaSurfaces(player, data);
            validateMinerFortuneDrops(player, data, fixture);
            validateBlacksmithOverEnchanting(player, data);
            BlockPos selfTestGround = player.getPosition().down();
            IBlockState previousGround = player.world.getBlockState(selfTestGround);
            float previousHealth = player.getHealth();
            player.world.setBlockState(selfTestGround, Blocks.GRASS.getDefaultState(), 3);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.4F));
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
            player.setHealth(Math.min(player.getMaxHealth(), previousHealth));
            player.world.setBlockState(selfTestGround, previousGround, 3);
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
            validateOriginalCommandSyntax(player);
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

    public static void validateOriginalCommandSyntax(EntityPlayer player) throws CommandException {
        MinecraftServer server = player.getServer();
        if (server == null) {
            throw new CommandException("selftest server missing for command validation");
        }

        SkillsCommand command = new SkillsCommand();
        if (!"skill".equals(command.getName())) {
            throw new CommandException("1.12.2 command root must be /skill");
        }
        if (!command.getAliases().contains("murilloskills")) {
            throw new CommandException("1.12.2 command alias /murilloskills missing");
        }

        ICommandSender admin = server;
        String target = player.getName();
        command.execute(server, admin, new String[] { "resetall", target });
        PlayerSkillDataCore data = data(player);
        for (SkillType skill : SkillType.values()) {
            requireSkillState(data, skill, 0, 0, 0.0D, "resetall");
        }
        if (!data.getSelectedSkills().isEmpty() || !data.getParagonSkills().isEmpty()) {
            throw new CommandException("resetall did not clear selected/paragon state");
        }

        command.execute(server, admin, new String[] { "select", target, "miner" });
        require(data.isSkillSelected(SkillType.MINER), "select did not add Miner");

        command.execute(server, admin, new String[] { "setparagon", target, "miner" });
        require(data.isParagonSkill(SkillType.MINER), "setparagon did not mark Miner");

        command.execute(server, admin, new String[] { "clearparagon", target });
        require(data.getParagonSkills().isEmpty(), "clearparagon did not clear Miner");

        command.execute(server, admin, new String[] { "setparagon", target, "miner" });
        command.execute(server, admin, new String[] { "setlevel", target, "miner", "42" });
        requireSkillState(data, SkillType.MINER, 42, 0, 0.0D, "setlevel");

        command.execute(server, admin, new String[] { "setprestige", target, "miner", "7" });
        requireSkillState(data, SkillType.MINER, 42, 7, 0.0D, "setprestige");

        command.execute(server, admin, new String[] { "addxp", target, "miner", "100" });
        SkillStatsCore miner = data.getSkill(SkillType.MINER);
        require(miner.getLevel() > 42 || miner.getXp() > 0.0D, "addxp did not change Miner progress");

        command.execute(server, admin, new String[] { "info", target });
        command.execute(server, admin, new String[] { "info", target, "miner" });

        command.execute(server, admin, new String[] { "deselect", target, "miner" });
        require(!data.isSkillSelected(SkillType.MINER), "deselect did not remove Miner");
        require(!data.isParagonSkill(SkillType.MINER), "deselect did not clear Miner paragon");

        command.execute(server, admin, new String[] { "reset", target, "warrior" });
        requireSkillState(data, SkillType.WARRIOR, 0, 0, 0.0D, "reset");

        command.execute(server, admin, new String[] { "maxall", target });
        for (SkillType skill : SkillType.values()) {
            require(data.getSkill(skill).getLevel() == CONFIG.getMaxLevel(), "maxall failed for " + skill);
        }

        data.setSelectedSkillsDirect(new ArrayList<SkillType>(Arrays.asList(SkillType.values())));
        data.clearAllParagonSkills();
        data.activateParagonSkill(SkillType.MINER);
        for (SkillType skill : SkillType.values()) {
            data.setSkill(skill, CONFIG.getMaxLevel(), 0.0D, -1L, 1);
        }
        applyAllPassives(player, data, true);
        STORE.save(player.getUniqueID());
        LOG.info("[MurilloSkills][1.12.2][SelfTest] Original command syntax PASS");
    }

    private static void requireSkillState(PlayerSkillDataCore data, SkillType skill, int level, int prestige,
            double xp, String step) throws CommandException {
        SkillStatsCore stats = data.getSkill(skill);
        if (stats.getLevel() != level || stats.getPrestige() != prestige || stats.getXp() != xp) {
            throw new CommandException(step + " produced wrong state for " + skill + ": L" + stats.getLevel()
                    + " P" + stats.getPrestige() + " XP" + stats.getXp());
        }
    }

    private static void require(boolean condition, String message) throws CommandException {
        if (!condition) {
            throw new CommandException(message);
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

    public static void validateMinerFortuneDrops(EntityPlayer player, PlayerSkillDataCore data,
            SelfTestFixture fixture) {
        BlockPos fortunePos = fixture.orePos.add(18, 0, 0);
        player.world.setBlockState(fortunePos, Blocks.DIAMOND_ORE.getDefaultState(), 3);
        IBlockState state = player.world.getBlockState(fortunePos);
        List<ItemStack> vanillaDrops = state.getBlock().getDrops(player.world, fortunePos, state, 0);
        int vanillaCount = totalItemCount(vanillaDrops);
        BlockEvent.HarvestDropsEvent event = new BlockEvent.HarvestDropsEvent(player.world, fortunePos, state,
                0, 1.0F, new ArrayList<ItemStack>(vanillaDrops), player, false);
        new com.murilloskills.forge112.impl.MinerSkill().onHarvestDrops(event, player, data, blockId(state));
        int boostedCount = totalItemCount(event.getDrops());
        int bonus = com.murilloskills.forge112.impl.MinerSkill.getSkillFortuneBonus(
                data.getSkill(SkillType.MINER), blockId(state), player.getHeldItemMainhand());
        if (bonus <= 0) {
            throw new IllegalStateException("miner fortune bonus did not calculate for " + blockId(state));
        }
        if (boostedCount <= vanillaCount) {
            throw new IllegalStateException("miner fortune did not add drops: vanilla="
                    + vanillaCount + " boosted=" + boostedCount + " bonus=" + bonus);
        }
        LOG.info("[MurilloSkills][1.12.2][SelfTest] Miner fortune PASS vanilla={} boosted={} bonus={}",
                vanillaCount, boostedCount, bonus);
    }

    public static void validateBlacksmithOverEnchanting(EntityPlayer player, PlayerSkillDataCore data) {
        SkillStatsCore stats = data.getSkill(SkillType.BLACKSMITH);
        int previousPrestige = stats.getPrestige();
        Container previousContainer = player.openContainer;
        try {
            stats.setLevel(CONFIG.getMaxLevel());
            stats.setPrestige(CONFIG.getMaxPrestigeLevel());

            Map<net.minecraft.enchantment.Enchantment, Integer> sharpness = new HashMap<net.minecraft.enchantment.Enchantment, Integer>();
            sharpness.put(Enchantments.SHARPNESS, Integer.valueOf(Enchantments.SHARPNESS.getMaxLevel()));
            ItemStack left = new ItemStack(Items.DIAMOND_SWORD);
            ItemStack right = new ItemStack(Items.DIAMOND_SWORD);
            EnchantmentHelper.setEnchantments(sharpness, left);
            EnchantmentHelper.setEnchantments(sharpness, right);
            com.murilloskills.forge112.utils.Forge112BlacksmithEnchanting.OverEnchantResult anvil =
                    com.murilloskills.forge112.utils.Forge112BlacksmithEnchanting.tryApplyAnvil(left, right, 10, stats);
            if (anvil == null) {
                throw new IllegalStateException("blacksmith over-enchant anvil produced no result");
            }
            int sharpnessLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, anvil.stack);
            if (sharpnessLevel != Enchantments.SHARPNESS.getMaxLevel() + 1 || anvil.cost <= 0 || anvil.cost > 25) {
                throw new IllegalStateException("blacksmith anvil over-enchant wrong result: sharpness="
                        + sharpnessLevel + " cost=" + anvil.cost);
            }

            ContainerRepair repair = new ContainerRepair(player.inventory, player.world, player.getPosition(), player);
            player.openContainer = repair;
            ItemStack eventLeft = left.copy();
            ItemStack eventRight = right.copy();
            repair.getSlot(1).putStack(eventLeft);
            repair.getSlot(2).putStack(eventRight);
            AnvilUpdateEvent event = new AnvilUpdateEvent(eventLeft, eventRight, null, 10);
            new com.murilloskills.forge112.impl.BlacksmithSkill().onAnvilUpdate(event);
            int eventSharpness = EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, event.getOutput());
            if (eventSharpness != Enchantments.SHARPNESS.getMaxLevel() + 1) {
                throw new IllegalStateException("blacksmith anvil Forge event path failed: sharpness="
                        + eventSharpness);
            }

            Map<net.minecraft.enchantment.Enchantment, Integer> efficiency = new HashMap<net.minecraft.enchantment.Enchantment, Integer>();
            efficiency.put(Enchantments.EFFICIENCY, Integer.valueOf(Enchantments.EFFICIENCY.getMaxLevel()));
            ItemStack table = new ItemStack(Items.DIAMOND_PICKAXE);
            EnchantmentHelper.setEnchantments(efficiency, table);
            if (!com.murilloskills.forge112.utils.Forge112BlacksmithEnchanting.tryApplyTableBonus(
                    table, stats, new Random(1234L))) {
                throw new IllegalStateException("blacksmith table over-enchant did not apply");
            }
            int efficiencyLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, table);
            if (efficiencyLevel != com.murilloskills.forge112.utils.Forge112BlacksmithEnchanting.OVER_ENCHANT_MAX_LEVEL) {
                throw new IllegalStateException("blacksmith table over-enchant wrong result: efficiency="
                        + efficiencyLevel);
            }
            LOG.info("[MurilloSkills][1.12.2][SelfTest] Blacksmith over-enchant PASS sharpness={} cost={} efficiency={}",
                    sharpnessLevel, anvil.cost, efficiencyLevel);
        } finally {
            player.openContainer = previousContainer;
            stats.setPrestige(previousPrestige);
        }
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

    public static void validateUltmineWithoutMinerSelection(EntityPlayer player, PlayerSkillDataCore data,
            SelfTestFixture fixture) {
        List<SkillType> originalSelection = new ArrayList<SkillType>(data.getSelectedSkills());
        try {
            data.setSelectedSkillsDirect(new ArrayList<SkillType>(Arrays.asList(
                    SkillType.WARRIOR, SkillType.BUILDER, SkillType.EXPLORER)));
            if (data.isSkillSelected(SkillType.MINER)) {
                throw new IllegalStateException("selftest could not deselect Miner for Ultmine validation");
            }
            BlockPos origin = fixture.orePos.add(2, 0, 2);
            player.world.setBlockState(origin, Blocks.STONE.getDefaultState(), 3);
            player.world.setBlockState(origin.east(), Blocks.STONE.getDefaultState(), 3);
            player.world.setBlockState(origin.south(), Blocks.STONE.getDefaultState(), 3);
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            applyClientUltmineConfig(player, "{\"dropsToInventory\":true,\"sameBlockOnly\":false,\"selectedShape\":\"S_3x3\"}");
            setUltmineSelection(player, UltmineShape112.S_3x3, 1, 3, 0, 20);
            setUltmineHeld(player, true);
            recordUltmineTargetFace(player, origin, EnumFacing.UP);
            int preview = getValidatedUltminePreview(player, origin, EnumFacing.UP).size();
            int mined = handleUltmineBreak(player, origin, player.world.getBlockState(origin));
            if (preview < 2 || mined <= 0) {
                throw new IllegalStateException("Ultmine without Miner selected failed: preview="
                        + preview + " mined=" + mined);
            }
            applyClientUltmineConfig(player, "{\"sameBlockOnly\":true,\"selectedShape\":\"S_3x3\"}");
            setUltmineSelection(player, UltmineShape112.S_3x3, 1, 3, 0, 20);
            BlockPos mixed = fixture.orePos.add(6, 0, 6);
            player.world.setBlockState(mixed, Blocks.STONE.getDefaultState(), 3);
            player.world.setBlockState(mixed.east(), Blocks.STONE.getDefaultState(), 3);
            player.world.setBlockState(mixed.south(), Blocks.COBBLESTONE.getDefaultState(), 3);
            recordUltmineTargetFace(player, mixed, EnumFacing.UP);
            int mixedPreview = getValidatedUltminePreview(player, mixed, EnumFacing.UP).size();
            int mixedMined = handleUltmineBreak(player, mixed, player.world.getBlockState(mixed));
            if (mixedPreview != 2 || mixedMined != 1 || player.world.isAirBlock(mixed.south())) {
                throw new IllegalStateException("Ultmine sameBlockOnly config failed: preview="
                        + mixedPreview + " mined=" + mixedMined);
            }
            applyClientUltmineConfig(player, "{\"dropsToInventory\":true,\"sameBlockOnly\":true,\"trashItems\":[\"minecraft:dirt\"],\"selectedShape\":\"S_3x3\"}");
            putSelfTestStack(player, new ItemStack(Blocks.DIRT, 5));
            if (purgeTrashInventory(player) < 5) {
                throw new IllegalStateException("Ultmine auto-trash inventory purge failed");
            }
            BlockPos trashOrigin = fixture.orePos.add(9, 0, 9);
            player.world.setBlockState(trashOrigin, Blocks.DIRT.getDefaultState(), 3);
            player.world.setBlockState(trashOrigin.east(), Blocks.DIRT.getDefaultState(), 3);
            recordUltmineTargetFace(player, trashOrigin, EnumFacing.UP);
            int dirtBefore = countInventoryItem(player, "minecraft:dirt");
            int trashMined = handleUltmineBreak(player, trashOrigin, player.world.getBlockState(trashOrigin));
            int dirtAfter = countInventoryItem(player, "minecraft:dirt");
            if (trashMined <= 0 || dirtAfter != dirtBefore) {
                throw new IllegalStateException("Ultmine auto-trash drop filter failed: mined="
                        + trashMined + " before=" + dirtBefore + " after=" + dirtAfter);
            }
            applyClientUltmineConfig(player, "{\"magnetEnabled\":true,\"magnetRange\":8,\"selectedShape\":\"S_3x3\"}");
            EntityItem item = new EntityItem(player.world, player.posX + 5.0D, player.posY, player.posZ,
                    new ItemStack(Items.APPLE));
            player.world.spawnEntity(item);
            tickUltmineConfigEffects(player);
            if (Math.abs(item.motionX) + Math.abs(item.motionY) + Math.abs(item.motionZ) <= 0.001D) {
                throw new IllegalStateException("Ultmine magnet config failed to pull item");
            }
            BlockPos batchOrigin = fixture.orePos.add(12, 0, 12);
            int placed = 0;
            for (int dx = 0; dx < 20; dx++) {
                for (int dz = 0; dz < 10; dz++) {
                    player.world.setBlockState(batchOrigin.add(dx, 0, dz), Blocks.STONE.getDefaultState(), 3);
                    placed++;
                }
            }
            applyClientUltmineConfig(player, "{\"dropsToInventory\":true,\"selectedShape\":\"LEGACY\",\"legacyMaxBlocks\":220}");
            setUltmineSelection(player, UltmineShape112.LEGACY, 1, 1, 0, 220);
            recordUltmineTargetFace(player, batchOrigin, EnumFacing.UP);
            int batchPreview = getValidatedUltminePreview(player, batchOrigin, EnumFacing.UP).size();
            int batchImmediate = handleUltmineBreak(player, batchOrigin, player.world.getBlockState(batchOrigin));
            if (batchImmediate != 0 || !isUltmineJobRunning(player)) {
                throw new IllegalStateException("Ultmine batch queue failed: preview=" + batchPreview
                        + " immediate=" + batchImmediate + " placed=" + placed);
            }
            for (int tick = 0; tick < 20 && isUltmineJobRunning(player); tick++) {
                tickUltmineJob(player);
            }
            if (isUltmineJobRunning(player)) {
                throw new IllegalStateException("Ultmine batch job did not finish within selftest budget");
            }
            int removed = 0;
            for (int dx = 0; dx < 20; dx++) {
                for (int dz = 0; dz < 10; dz++) {
                    if (player.world.isAirBlock(batchOrigin.add(dx, 0, dz))) {
                        removed++;
                    }
                }
            }
            if (removed < 160) {
                throw new IllegalStateException("Ultmine batch job removed too few blocks: " + removed);
            }
            LOG.info("[MurilloSkills][1.12.2][SelfTest] Ultmine without Miner selected PASS preview={} mined={}",
                    preview, mined);
        } finally {
            setUltmineHeld(player, false);
            player.setHeldItem(EnumHand.MAIN_HAND, fixture.damagedTool);
            data.setSelectedSkillsDirect(originalSelection);
        }
    }

    private static void putSelfTestStack(EntityPlayer player, ItemStack stack) {
        int heldSlot = player.inventory.currentItem;
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            if (i == heldSlot) {
                continue;
            }
            ItemStack existing = player.inventory.mainInventory.get(i);
            if (existing == null || existing.isEmpty()) {
                player.inventory.mainInventory.set(i, stack);
                player.inventory.markDirty();
                return;
            }
        }
        int fallbackSlot = heldSlot == 0 ? 1 : 0;
        if (fallbackSlot >= player.inventory.mainInventory.size()) {
            throw new IllegalStateException("selftest inventory had no slot for " + itemId(stack));
        }
        player.inventory.mainInventory.set(fallbackSlot, stack);
        player.inventory.markDirty();
    }

    public static void requireToggle(PlayerSkillDataCore data, SkillType skill, String toggle, boolean expected) {
        boolean value = data.getToggle(skill, toggle, !expected);
        if (value != expected) {
            throw new IllegalStateException("toggle " + skill + "." + toggle + " expected " + expected + " but was " + value);
        }
    }

    private static int countInventoryItem(EntityPlayer player, String id) {
        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && !stack.isEmpty() && id.equals(itemId(stack))) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            if (stack != null && !stack.isEmpty() && id.equals(itemId(stack))) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int totalItemCount(List<ItemStack> stacks) {
        int count = 0;
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                count += stack.getCount();
            }
        }
        return count;
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
