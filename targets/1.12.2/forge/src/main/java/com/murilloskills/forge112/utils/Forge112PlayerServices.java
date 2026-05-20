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

public final class Forge112PlayerServices {
    private Forge112PlayerServices() {
    }

    public static void addXp(EntityPlayer player, SkillType skill, int amount, String reason) {
        if (player == null || player.world.isRemote) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        amount = Forge112SkillSynergyManager.applyXpBonus(data, skill, amount);
        XpAddResult result = data.addXpToSkill(skill, amount, CONFIG);
        if (result != XpAddResult.NO_CHANGE) {
            Forge112Notifications.xp(player, skill, amount, reason);
            if (result.isLeveledUp()) {
                say(player, "MurilloSkills: " + skillName(skill) + " level " + result.getNewLevel() + ".");
                Forge112Notifications.levelUp(player, skill, result.getOldLevel(), result.getNewLevel());
                Forge112AchievementTracker.levelUp(player, skill, result.getOldLevel(), result.getNewLevel());
                LOG.info("[MurilloSkills][1.12.2][XP] {} leveled {} {}->{} via {}",
                        player.getName(), skill, result.getOldLevel(), result.getNewLevel(), reason);
            } else if (player.ticksExisted % 60 == 0) {
                LOG.debug("[MurilloSkills][1.12.2][XP] {} +{} {} via {}", player.getName(), amount, skill, reason);
            }
            STORE.markDirty(player.getUniqueID());
        }
    }

    public static PlayerSkillDataCore data(EntityPlayer player) {
        return STORE.load(player.getUniqueID());
    }

    public static PlayerRuntime runtime(EntityPlayer player) {
        PlayerRuntime runtime = RUNTIME.get(player.getUniqueID());
        if (runtime == null) {
            runtime = new PlayerRuntime(player);
            RUNTIME.put(player.getUniqueID(), runtime);
        }
        return runtime;
    }

    public static boolean isSelected(PlayerSkillDataCore data, SkillType skill) {
        return data.isSkillSelected(skill);
    }

    public static double passiveScale(PlayerSkillDataCore data, SkillType skill, double perLevel, double perPrestige) {
        SkillStatsCore stats = data.getSkill(skill);
        return stats.getLevel() * perLevel + stats.getPrestige() * perPrestige;
    }

    public static EntityPlayer nearestSelectedPlayer(World world, BlockPos pos, SkillType skill, double maxDistance) {
        if (world == null) {
            return null;
        }
        double best = maxDistance * maxDistance;
        EntityPlayer found = null;
        for (EntityPlayer player : world.playerEntities) {
            if (player == null || player.world.isRemote || !isSelected(data(player), skill)) {
                continue;
            }
            double dist = player.getDistanceSq(pos);
            if (dist <= best) {
                best = dist;
                found = player;
            }
        }
        return found;
    }

    public static String blockId(IBlockState state) {
        if (state == null || state.getBlock() == null || state.getBlock().getRegistryName() == null) {
            return "";
        }
        return state.getBlock().getRegistryName().toString().toLowerCase(Locale.ROOT);
    }

    public static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        Item item = stack.getItem();
        ResourceLocation id = item.getRegistryName();
        return id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
    }

    public static SkillType parseSkill(String value) throws CommandException {
        if (value == null) {
            throw new WrongUsageException("Skill obrigatoria.");
        }
        try {
            return SkillType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new WrongUsageException("Skill invalida: " + value);
        }
    }

    public static boolean parseBoolean(String value) throws CommandException {
        if ("true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value) || "1".equals(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value) || "0".equals(value)) {
            return false;
        }
        throw new WrongUsageException("Boolean invalido: " + value);
    }

    public static String skillName(SkillType skill) {
        return skill.name().toLowerCase(Locale.ROOT);
    }

    public static void say(EntityPlayer player, String message) {
        if (!Boolean.getBoolean("murilloskills.chat112") && message != null && message.startsWith("MurilloSkills")) {
            LOG.debug("[MurilloSkills][1.12.2][ChatSuppressed] {}", message);
            return;
        }
        player.sendMessage(new TextComponentString(message));
    }

    public static void say(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(message));
    }

    public static void toggle(EntityPlayer player, SkillType skill, String toggle, boolean defaultValue) {
        PlayerSkillDataCore data = data(player);
        boolean next = !data.getToggle(skill, toggle, defaultValue);
        data.setToggle(skill, toggle, next);
        STORE.save(player.getUniqueID());
        say(player, "MurilloSkills: " + toggle + " = " + next);
        Forge112Notifications.toggle(player, skill, toggle, next);
        LOG.info("[MurilloSkills][1.12.2][Toggle] {} {}.{}={}", player.getName(), skill, toggle, next);
    }
}
