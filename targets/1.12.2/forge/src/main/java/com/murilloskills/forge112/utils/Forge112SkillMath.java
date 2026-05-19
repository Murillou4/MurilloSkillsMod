package com.murilloskills.forge112.utils;

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
import static com.murilloskills.forge112.dev.Forge112SelfTest.*;
import static com.murilloskills.forge112.skills.Forge112Abilities.*;
import static com.murilloskills.forge112.skills.Forge112Passives.*;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.*;
import static com.murilloskills.forge112.utils.Forge112EnvironmentEffects.*;
import static com.murilloskills.forge112.utils.Forge112MiningTools.*;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.*;

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

public final class Forge112SkillMath {
    private Forge112SkillMath() {
    }

    public static double prestigePassiveMultiplier(SkillStatsCore stats) {
        return 1.0D + stats.getPrestige() * CONFIG.getPrestigePassiveBonus();
    }

    public static double getMinerSpeedBonus(SkillStatsCore stats) {
        return Math.min(4.00D, stats.getLevel() * 0.03D * prestigePassiveMultiplier(stats));
    }

    public static double getWarriorDamageBonus(SkillStatsCore stats) {
        return stats.getLevel() * 0.20D * prestigePassiveMultiplier(stats);
    }

    public static double getWarriorHealthBonus(SkillStatsCore stats) {
        return Math.floor(stats.getLevel() / 10.0D) * 2.0D * prestigePassiveMultiplier(stats);
    }

    public static double getArcherDamageMultiplier(SkillStatsCore stats) {
        double bonus = stats.getLevel() * 0.03D * prestigePassiveMultiplier(stats);
        if (stats.getLevel() >= ARCHER_BONUS_DAMAGE_LEVEL) {
            bonus += 0.05D;
        }
        if (stats.getLevel() >= ARCHER_PENETRATION_LEVEL) {
            bonus += 0.10D;
        }
        return 1.0D + Math.min(4.0D, bonus);
    }

    public static double getFarmerDoubleHarvestChance(SkillStatsCore stats) {
        double chance = stats.getLevel() * 0.01D * prestigePassiveMultiplier(stats);
        if (stats.getLevel() >= 10) {
            chance += 0.05D;
        }
        if (stats.getLevel() >= 75) {
            chance += 0.15D;
        }
        return Math.min(1.0D, chance);
    }

    public static double getFarmerFertileGrowthChance(SkillStatsCore stats) {
        if (stats.getLevel() < 25) {
            return 0.0D;
        }
        return Math.min(0.55D, 0.08D + stats.getLevel() * 0.003D * prestigePassiveMultiplier(stats));
    }

    public static double getFisherBundleChance(SkillStatsCore stats) {
        double chance = stats.getLevel() * 0.003D * prestigePassiveMultiplier(stats);
        if (stats.getLevel() >= 25) {
            chance += 0.05D;
        }
        if (stats.getLevel() >= FISHER_LUCK_SEA_LEVEL) {
            chance += 0.15D;
        }
        return Math.min(0.95D, chance);
    }

    public static float getBlacksmithDamageMultiplier(EntityPlayer player, SkillStatsCore stats, DamageSource source) {
        float multiplier = 1.0F;
        if (stats.getLevel() >= 50) {
            multiplier *= 0.90F;
        }
        if (stats.getLevel() >= BLACKSMITH_FIRE_MASTERY_LEVEL && (source.isFireDamage() || source.isExplosion())) {
            multiplier *= 0.45F;
        }
        if (isTimedActive(player, TITANIUM_AURA_UNTIL)) {
            multiplier *= 0.70F;
        }
        return multiplier;
    }

    public static double getExplorerSpeedBonus(SkillStatsCore stats, boolean sprinting) {
        double bonus = stats.getLevel() * 0.004D * prestigePassiveMultiplier(stats);
        if (stats.getLevel() >= EXPLORER_PATHFINDER_LEVEL) {
            bonus += sprinting ? 0.04D : 0.02D;
        }
        return Math.min(0.70D, bonus);
    }

    public static double getBuilderReachBonus(SkillStatsCore stats) {
        double bonus = stats.getLevel() * 0.08D * prestigePassiveMultiplier(stats);
        if (stats.getLevel() >= BUILDER_EXTENDED_REACH_LEVEL) {
            bonus += 1.0D;
        }
        if (stats.getLevel() >= BUILDER_MASTER_REACH_LEVEL) {
            bonus += 5.0D;
        }
        return Math.min(16.0D, bonus);
    }

    public static boolean isNaturalGround(IBlockState state) {
}
