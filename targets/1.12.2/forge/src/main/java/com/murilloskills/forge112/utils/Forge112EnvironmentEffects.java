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

public final class Forge112EnvironmentEffects {
    private Forge112EnvironmentEffects() {
    }

    public static List<BlockPos> scanTreasurePositions(EntityPlayer player, int radius, int limit) {
        List<BlockPos> found = new ArrayList<BlockPos>();
        BlockPos center = player.getPosition();
        for (Object object : player.world.loadedTileEntityList) {
            if (!(object instanceof TileEntity)) {
                continue;
            }
            TileEntity tile = (TileEntity) object;
            BlockPos pos = tile.getPos();
            if (pos == null || pos.distanceSq(center) > radius * radius) {
                continue;
            }
            if (isTreasureTile(tile) && found.size() < limit) {
                found.add(pos.toImmutable());
            }
        }
        if (found.size() < limit) {
            int blockRadius = Math.min(24, radius);
            for (BlockPos pos : BlockPos.getAllInBox(center.add(-blockRadius, -8, -blockRadius), center.add(blockRadius, 8, blockRadius))) {
                if (found.size() >= limit) {
                    break;
                }
                if (CrossModCompatRules.isLootContainerId(blockId(player.world.getBlockState(pos)))) {
                    found.add(pos.toImmutable());
                }
            }
        }
        return found;
    }

    public static boolean isTreasureTile(TileEntity tile) {
        if (tile instanceof TileEntityChest || tile instanceof TileEntityMobSpawner) {
            return true;
        }
        return CrossModCompatRules.isLootContainerId(blockId(tile.getWorld().getBlockState(tile.getPos())));
    }

    public static void awardExplorerMovement(EntityPlayer player, PlayerSkillDataCore data, PlayerRuntime runtime) {
        if (!isSelected(data, SkillType.EXPLORER)) {
            runtime.lastX = player.posX;
            runtime.lastY = player.posY;
            runtime.lastZ = player.posZ;
            return;
        }
        double dx = player.posX - runtime.lastX;
        double dy = player.posY - runtime.lastY;
        double dz = player.posZ - runtime.lastZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist >= 16.0D) {
            addXp(player, SkillType.EXPLORER, Math.min(80, (int) dist), "exploration movement");
            runtime.lastX = player.posX;
            runtime.lastY = player.posY;
            runtime.lastZ = player.posZ;
        }
    }

    public static void accelerateNearbyPlants(EntityPlayer player, PlayerSkillDataCore data) {
        int radius = 2 + Math.min(4, data.getSkill(SkillType.FARMER).getLevel() / 25);
        World world = player.world;
        for (BlockPos pos : BlockPos.getAllInBox(player.getPosition().add(-radius, -1, -radius), player.getPosition().add(radius, 1, radius))) {
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof IGrowable && RANDOM.nextInt(12) == 0) {
                try {
                    ((IGrowable) block).grow(world, RANDOM, pos, state);
                    LOG.debug("[MurilloSkills][1.12.2][Farmer] Accelerated plant at {}", pos);
                } catch (Throwable error) {
                    LOG.debug("[MurilloSkills][1.12.2][Farmer] Plant acceleration skipped at {}: {}", pos, error.toString());
                }
            }
        }
    }

    public static void accelerateNearbyFurnaces(EntityPlayer player, PlayerSkillDataCore data) {
        if (data.getSkill(SkillType.BLACKSMITH).getLevel() < 25) {
            return;
        }
        int radius = 4;
        World world = player.world;
        for (BlockPos pos : BlockPos.getAllInBox(player.getPosition().add(-radius, -2, -radius), player.getPosition().add(radius, 2, radius))) {
            String id = blockId(world.getBlockState(pos));
            if (!CrossModCompatRules.isLikelyMachineIdOrClass(id)) {
                continue;
            }
            Object tile = world.getTileEntity(pos);
            if (tile == null) {
                continue;
            }
            boolean changed = tickMachineReflectively(tile, data.getSkill(SkillType.BLACKSMITH).getLevel() >= 75 ? 2 : 1);
            if (changed && player.ticksExisted % 200 == 0) {
                LOG.info("[MurilloSkills][1.12.2][Blacksmith] Accelerated machine {} near {}", id, player.getName());
            }
        }
    }

    public static boolean tickMachineReflectively(Object tile, int bonusTicks) {
        boolean changed = false;
        for (int i = 0; i < bonusTicks; i++) {
            try {
                tile.getClass().getMethod("update").invoke(tile);
                changed = true;
            } catch (Throwable ignored) {
                return changed;
            }
        }
        return changed;
    }
}
