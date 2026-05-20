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

public final class Forge112MiningTools {
    private static final int MAX_NETWORK_CONFIG_CHARS = 32767;
    private static final int MAX_NETWORK_ID_SET_SIZE = 2048;
    private static final int MAX_NETWORK_ID_LENGTH = 128;
    private static final Map<UUID, UltmineSettings> ULTMINE_SETTINGS = new HashMap<UUID, UltmineSettings>();
    private static final Map<UUID, UltmineBreakJob> ULTMINE_JOBS = new HashMap<UUID, UltmineBreakJob>();
    private static final Map<UUID, BlockPos> ULTMINE_LAST_TARGET_POS = new HashMap<UUID, BlockPos>();
    private static final Map<UUID, EnumFacing> ULTMINE_LAST_TARGET_FACE = new HashMap<UUID, EnumFacing>();
    private static final Map<UUID, Long> ULTMINE_LAST_TARGET_TICK = new HashMap<UUID, Long>();
    private static UltmineTuning ultmineTuning;

    private Forge112MiningTools() {
    }

    public static void clearUltmineState(EntityPlayer player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueID();
        ULTMINE_HELD.remove(uuid);
        ULTMINE_RUNNING.remove(uuid);
        ULTMINE_JOBS.remove(uuid);
        ULTMINE_SETTINGS.remove(uuid);
        ULTMINE_LAST_TARGET_POS.remove(uuid);
        ULTMINE_LAST_TARGET_FACE.remove(uuid);
        ULTMINE_LAST_TARGET_TICK.remove(uuid);
    }

    public static void setUltmineSelection(EntityPlayer player, UltmineShape112 shape, int depth, int length,
            int variant, int legacyMaxBlocks) {
        UltmineSettings settings = settings(player);
        settings.selectedShape = shape == null ? UltmineShape112.S_3x3 : shape;
        settings.setDepth(settings.selectedShape, depth);
        settings.setLength(settings.selectedShape, length);
        settings.setVariant(settings.selectedShape, variant);
        settings.legacyMaxBlocks = clamp(legacyMaxBlocks, 1, 4096);
    }

    public static void applyClientUltmineConfig(EntityPlayer player, String json) {
        if (player == null || json == null || json.trim().length() == 0) {
            return;
        }
        if (json.length() > MAX_NETWORK_CONFIG_CHARS) {
            LOG.warn("[MurilloSkills][1.12.2][Ultmine] Ignored oversized config sync from {}: {} chars.",
                    player.getName(), json.length());
            return;
        }
        try {
            JsonElement parsed = new JsonParser().parse(json);
            if (parsed == null || !parsed.isJsonObject()) {
                return;
            }
            JsonObject root = parsed.getAsJsonObject();
            UltmineSettings settings = settings(player);
            settings.dropsToInventory = boolFrom(root, "dropsToInventory", settings.dropsToInventory);
            settings.dropsToStorage = boolFrom(root, "dropsToStorage", settings.dropsToStorage);
            settings.xpDirectToPlayer = boolFrom(root, "xpDirectToPlayer", settings.xpDirectToPlayer);
            settings.sameBlockOnly = boolFrom(root, "sameBlockOnly", settings.sameBlockOnly);
            settings.magnetEnabled = boolFrom(root, "magnetEnabled", settings.magnetEnabled);
            settings.magnetRange = clamp(intFrom(root, "magnetRange", settings.magnetRange), 1, 32);
            settings.legacyMaxBlocks = clamp(intFrom(root, "legacyMaxBlocks", settings.legacyMaxBlocks), 1, 4096);
            if (root.has("selectedShape")) {
                try {
                    settings.selectedShape = UltmineShape112.valueOf(root.get("selectedShape").getAsString());
                } catch (Exception ignored) {
                }
            }
            if (root.has("shapePrefs") && root.get("shapePrefs").isJsonObject()) {
                JsonObject prefs = root.getAsJsonObject("shapePrefs");
                for (UltmineShape112 shape : UltmineShape112.values()) {
                    JsonElement element = prefs.get(shape.name());
                    if (element != null && element.isJsonObject()) {
                        JsonObject obj = element.getAsJsonObject();
                        settings.setDepth(shape, intFrom(obj, "depth", settings.getDepth(shape)));
                        settings.setLength(shape, intFrom(obj, "length", settings.getLength(shape)));
                        settings.setVariant(shape, intFrom(obj, "variant", settings.getVariant(shape)));
                    }
                }
            }
            settings.trashItems = normalizedSet(root, "trashItems");
            settings.legacyBlockedBlocks = normalizedSet(root, "legacyBlockedBlocks");
            settings.storageWhitelist = normalizedSet(root, "storageWhitelist");
            int purged = purgeTrashInventory(player);
            LOG.info("[MurilloSkills][1.12.2][Ultmine] Config sync for {} shape={} inv={} storage={} xp={} same={} magnet={}/{} trash={} classicLock={} storageFilter={}",
                    player.getName(), settings.selectedShape, settings.dropsToInventory, settings.dropsToStorage,
                    settings.xpDirectToPlayer, settings.sameBlockOnly, settings.magnetEnabled, settings.magnetRange,
                    settings.trashItems.size(), settings.legacyBlockedBlocks.size(), settings.storageWhitelist.size());
            if (purged > 0) {
                LOG.info("[MurilloSkills][1.12.2][Ultmine] Auto-trash purged {} items from {} on config sync.",
                        purged, player.getName());
            }
        } catch (Exception error) {
            LOG.warn("[MurilloSkills][1.12.2][Ultmine] Config sync failed for {}: {}",
                    player.getName(), error.toString());
        }
    }

    public static List<BlockPos> getValidatedUltminePreview(EntityPlayer player, BlockPos origin, EnumFacing face) {
        List<BlockPos> out = new ArrayList<BlockPos>();
        if (player == null || origin == null || player.world == null || !isUltmineHeld(player)) {
            return out;
        }
        if (!isLoadedBlock(player.world, origin)) {
            return out;
        }
        IBlockState originState = player.world.getBlockState(origin);
        if (originState == null || originState.getBlock() == Blocks.AIR) {
            return out;
        }
        UltmineSettings settings = settings(player);
        UltmineShape112 shape = settings.selectedShape;
        EnumFacing safeFace = face == null ? faceFromLook(player) : face;
        List<BlockPos> candidates = shape == UltmineShape112.LEGACY
                ? collectLegacyUltmine(player, origin, originState, settings)
                : getUltmineShapeBlocks(origin, shape, settings.getDepth(shape), settings.getLength(shape),
                        settings.getVariant(shape), safeFace, player.getLookVec());
        int max = Math.min(maxBlocksForShape(settings, shape), candidates.size());
        String originId = blockId(originState);
        for (BlockPos pos : candidates) {
            if (out.size() >= max) {
                break;
            }
            if (pos == null) {
                continue;
            }
            if (!isLoadedBlock(player.world, pos)) {
                continue;
            }
            IBlockState state = player.world.getBlockState(pos);
            if (canUltmineBlock(player, pos, state, originId, shape, settings)) {
                out.add(pos.toImmutable());
            }
        }
        return out;
    }

    public static int handleUltmineBreak(EntityPlayer player, BlockPos origin, IBlockState originState) {
        if (player == null || origin == null || originState == null || player.world == null) {
            return 0;
        }
        UUID uuid = player.getUniqueID();
        if (!isUltmineHeld(player) || ULTMINE_RUNNING.contains(uuid) || ULTMINE_JOBS.containsKey(uuid)) {
            return 0;
        }
        return runUltmine(player, origin, originState);
    }

    public static void placeAutoTorch(EntityPlayer player) {
        BlockPos pos = player.getPosition();
        if (!isLoadedBlock(player.world, pos) || !isLoadedBlock(player.world, pos.down())) {
            return;
        }
        if (!player.world.isAirBlock(pos) || player.world.getLight(pos) > 7
                || !player.world.getBlockState(pos.down()).isSideSolid(player.world, pos.down(), EnumFacing.UP)) {
            return;
        }
        int slot = -1;
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (!stack.isEmpty() && stack.getItem() == Item.getItemFromBlock(Blocks.TORCH)) {
                slot = i;
                break;
            }
        }
        if (!player.capabilities.isCreativeMode && slot < 0) {
            if (player.ticksExisted % 200 == 0) {
                LOG.info("[MurilloSkills][1.12.2][Miner] Auto torch skipped for {}: no torches.", player.getName());
            }
            return;
        }
        player.world.setBlockState(pos, Blocks.TORCH.getDefaultState(), 3);
        if (!player.capabilities.isCreativeMode) {
            player.inventory.mainInventory.get(slot).shrink(1);
        }
        LOG.info("[MurilloSkills][1.12.2][Miner] Auto torch placed for {} at {}", player.getName(), pos);
    }

    public static List<BlockPos> scanOrePositions(EntityPlayer player, int radius, int limit) {
        List<BlockPos> found = new ArrayList<BlockPos>();
        if (player == null || player.world == null || radius <= 0 || limit <= 0) {
            return found;
        }
        BlockPos center = player.getPosition();
        for (BlockPos pos : BlockPos.getAllInBox(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            if (found.size() >= limit) {
                break;
            }
            if (!isLoadedBlock(player.world, pos)) {
                continue;
            }
            IBlockState state = player.world.getBlockState(pos);
            String id = blockId(state);
            if (CrossModCompatRules.isOreResourceId(id) && ClientOreFilterConfig.isOreEnabledForBlockId(id)) {
                found.add(pos.toImmutable());
            }
        }
        return found;
    }

    public static boolean isUltmineHeld(EntityPlayer player) {
        return player != null && ULTMINE_HELD.contains(player.getUniqueID());
    }

    public static boolean isLoadedBlock(World world, BlockPos pos) {
        return world != null && pos != null && pos.getY() >= 0 && pos.getY() < 256 && world.isBlockLoaded(pos);
    }

    public static void setUltmineHeld(EntityPlayer player, boolean held) {
        if (player == null) {
            return;
        }
        if (held) {
            ULTMINE_HELD.add(player.getUniqueID());
        } else {
            ULTMINE_HELD.remove(player.getUniqueID());
        }
    }

    public static int runUltmine(EntityPlayer player, BlockPos origin, IBlockState originState) {
        if (player == null || origin == null || originState == null || player.world == null) {
            return 0;
        }
        if (!isLoadedBlock(player.world, origin)) {
            return 0;
        }
        UUID uuid = player.getUniqueID();
        ULTMINE_RUNNING.add(uuid);
        try {
            UltmineSettings settings = settings(player).copy();
            UltmineShape112 shape = settings.selectedShape;
            List<BlockPos> candidates = shape == UltmineShape112.LEGACY
                    ? collectLegacyUltmine(player, origin, originState, settings)
                    : getUltmineShapeBlocks(origin, shape, settings.getDepth(shape),
                            settings.getLength(shape), settings.getVariant(shape),
                            resolveUltmineFace(player, origin), player.getLookVec());
            int max = Math.max(1, Math.min(maxBlocksForShape(settings, shape), candidates.size()));
            int extraLimit = Math.max(0, max - 1);
            String originId = blockId(originState);
            List<BlockPos> targets = new ArrayList<BlockPos>(Math.min(extraLimit, candidates.size()));
            for (BlockPos pos : candidates) {
                if (targets.size() >= extraLimit) {
                    break;
                }
                if (pos == null || pos.equals(origin)) {
                    continue;
                }
                if (!isLoadedBlock(player.world, pos)) {
                    continue;
                }
                IBlockState state = player.world.getBlockState(pos);
                if (!canUltmineBlock(player, pos, state, originId, shape, settings)) {
                    continue;
                }
                targets.add(pos.toImmutable());
            }
            if (targets.isEmpty()) {
                return 0;
            }
            if (targets.size() > ultmineInstantBreakThreshold()) {
                ULTMINE_JOBS.put(uuid, new UltmineBreakJob(player.world, origin.toImmutable(), targets, settings));
                LOG.info("[MurilloSkills][1.12.2][Ultmine] {} queued {} blocks at {} ({} per tick).",
                        player.getName(), targets.size(), origin, ultmineBlocksPerTick());
                return 0;
            }
            int mined = executeUltmineTargets(player, origin, targets, settings, false);
            return mined;
        } finally {
            ULTMINE_RUNNING.remove(uuid);
        }
    }

    public static void tickUltmineJob(EntityPlayer player) {
        if (player == null || player.world == null) {
            return;
        }
        UUID uuid = player.getUniqueID();
        UltmineBreakJob job = ULTMINE_JOBS.get(uuid);
        if (job == null) {
            return;
        }
        if (player.isDead || player.world != job.world) {
            ULTMINE_JOBS.remove(uuid);
            return;
        }
        ULTMINE_RUNNING.add(uuid);
        int minedThisTick = 0;
        try {
            BulkDropBuffer dropBuffer = new BulkDropBuffer(job.settings, job.origin);
            int budget = ultmineBlocksPerTick();
            int processed = 0;
            while (processed < budget && job.hasNext()) {
                BlockPos pos = job.next();
                if (!isLoadedBlock(player.world, pos)) {
                    processed++;
                    continue;
                }
                IBlockState state = player.world.getBlockState(pos);
                if (mineUltmineBlock(player, pos, state, job.settings, dropBuffer,
                        shouldSuppressBulkBreakParticles())) {
                    minedThisTick++;
                    job.mined++;
                }
                processed++;
            }
            dropBuffer.flush(player, player.world);
        } finally {
            ULTMINE_RUNNING.remove(uuid);
        }
        if (!job.hasNext()) {
            ULTMINE_JOBS.remove(uuid);
            if (job.mined > 0) {
                addXp(player, SkillType.MINER, job.mined * 6, "ultmine batch");
                LOG.info("[MurilloSkills][1.12.2][Ultmine] {} finished queued job: {} extra blocks.",
                        player.getName(), job.mined);
            }
        } else if (minedThisTick > 0 && player.ticksExisted % 40 == 0) {
            LOG.debug("[MurilloSkills][1.12.2][Ultmine] {} queued progress {}/{}.",
                    player.getName(), job.mined, job.targets.size());
        }
    }

    public static boolean isUltmineJobRunning(EntityPlayer player) {
        return player != null && ULTMINE_JOBS.containsKey(player.getUniqueID());
    }

    private static int executeUltmineTargets(EntityPlayer player, BlockPos origin, List<BlockPos> targets,
            UltmineSettings settings, boolean suppressBreakParticles) {
        BulkDropBuffer dropBuffer = new BulkDropBuffer(settings, origin);
        int mined = 0;
        for (BlockPos pos : targets) {
            if (!isLoadedBlock(player.world, pos)) {
                continue;
            }
            IBlockState state = player.world.getBlockState(pos);
            if (mineUltmineBlock(player, pos, state, settings, dropBuffer, suppressBreakParticles)) {
                mined++;
            }
        }
        dropBuffer.flush(player, player.world);
        return mined;
    }

    public static boolean canUltmineBlock(EntityPlayer player, BlockPos pos, IBlockState state, String originId,
            UltmineShape112 shape) {
        return canUltmineBlock(player, pos, state, originId, shape, settings(player));
    }

    private static boolean canUltmineBlock(EntityPlayer player, BlockPos pos, IBlockState state, String originId,
            UltmineShape112 shape, UltmineSettings settings) {
        if (player == null || player.world == null || pos == null || state == null || state.getBlock() == Blocks.AIR) {
            return false;
        }
        if (!isLoadedBlock(player.world, pos)) {
            return false;
        }
        Block block = state.getBlock();
        if (block == Blocks.BEDROCK || block == Blocks.BARRIER || block == Blocks.COMMAND_BLOCK) {
            return false;
        }
        if (block.getBlockHardness(state, player.world, pos) < 0.0F) {
            return false;
        }
        String id = blockId(state);
        if (settings.legacyBlockedBlocks.contains(id)) {
            return false;
        }
        if (settings.sameBlockOnly && !id.equals(originId)) {
            return false;
        }
        if (shape == UltmineShape112.LEGACY && settings.getVariant(shape) == 1) {
            if (CrossModCompatRules.isOreResourceId(originId)) {
                return CrossModCompatRules.isOreResourceId(id);
            }
            return id.equals(originId);
        }
        return block.getPlayerRelativeBlockHardness(state, player, player.world, pos) > 0.0F
                || player.capabilities.isCreativeMode;
    }

    public static boolean mineUltmineBlock(EntityPlayer player, BlockPos pos, IBlockState state) {
        return mineUltmineBlock(player, pos, state, settings(player));
    }

    private static boolean mineUltmineBlock(EntityPlayer player, BlockPos pos, IBlockState state,
            UltmineSettings settings) {
        BulkDropBuffer dropBuffer = new BulkDropBuffer(settings, pos);
        boolean mined = mineUltmineBlock(player, pos, state, settings, dropBuffer, false);
        dropBuffer.flush(player, player.world);
        return mined;
    }

    private static boolean mineUltmineBlock(EntityPlayer player, BlockPos pos, IBlockState state,
            UltmineSettings settings, BulkDropBuffer dropBuffer, boolean suppressBreakParticles) {
        if (player == null || pos == null || state == null || state.getBlock() == Blocks.AIR || player.world == null) {
            return false;
        }
        World world = player.world;
        if (!isLoadedBlock(world, pos)) {
            return false;
        }
        if (!player.capabilities.isCreativeMode) {
            int fortune = EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.FORTUNE,
                    player.getHeldItemMainhand());
            List<ItemStack> drops = state.getBlock().getDrops(world, pos, state, fortune);
            dropBuffer.addDrops(drops);
            int xp = getVanillaBlockXpDrop(world, pos, state, fortune);
            if (xp > 0) {
                dropBuffer.addXp(xp);
            }
            if (!suppressBreakParticles) {
                world.playEvent(2001, pos, Block.getStateId(state));
            }
            if (!world.setBlockToAir(pos)) {
                return false;
            }
        } else {
            if (!suppressBreakParticles) {
                world.playEvent(2001, pos, Block.getStateId(state));
            }
            if (!world.setBlockToAir(pos)) {
                return false;
            }
        }
        ItemStack held = player.getHeldItemMainhand();
        if (held != null && !held.isEmpty() && !player.capabilities.isCreativeMode) {
            held.damageItem(1, player);
        }
        return true;
    }

    public static void tickUltmineConfigEffects(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        tickMagnet(player);
        if (player.ticksExisted % 10 == 0) {
            purgeTrashInventory(player);
        }
    }

    public static int purgeTrashInventory(EntityPlayer player) {
        if (player == null) {
            return 0;
        }
        UltmineSettings settings = settings(player);
        if (settings.trashItems.isEmpty()) {
            return 0;
        }
        int removed = 0;
        removed += purgeTrashList(player.inventory.mainInventory, settings.trashItems);
        removed += purgeTrashList(player.inventory.armorInventory, settings.trashItems);
        removed += purgeTrashList(player.inventory.offHandInventory, settings.trashItems);
        if (removed > 0) {
            player.inventory.markDirty();
        }
        return removed;
    }

    private static int purgeTrashList(List<ItemStack> stacks, Set<String> trashItems) {
        int removed = 0;
        if (stacks == null) {
            return 0;
        }
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack == null || stack.isEmpty() || !trashItems.contains(itemId(stack))) {
                continue;
            }
            removed += stack.getCount();
            stacks.set(i, ItemStack.EMPTY);
        }
        return removed;
    }

    private static void tickMagnet(EntityPlayer player) {
        UltmineSettings settings = settings(player);
        if (!settings.magnetEnabled || player.isDead) {
            return;
        }
        double range = Math.max(1, Math.min(32, settings.magnetRange));
        AxisAlignedBB box = new AxisAlignedBB(
                player.posX - range, player.posY - range, player.posZ - range,
                player.posX + range, player.posY + range, player.posZ + range);
        List<EntityItem> items = player.world.getEntitiesWithinAABB(EntityItem.class, box);
        for (EntityItem item : items) {
            if (item == null || item.isDead) {
                continue;
            }
            pullEntityTowardPlayer(item, player);
        }
        List<net.minecraft.entity.item.EntityXPOrb> orbs = player.world.getEntitiesWithinAABB(
                net.minecraft.entity.item.EntityXPOrb.class, box);
        for (net.minecraft.entity.item.EntityXPOrb orb : orbs) {
            if (orb == null || orb.isDead) {
                continue;
            }
            pullEntityTowardPlayer(orb, player);
        }
    }

    private static void pullEntityTowardPlayer(Entity entity, EntityPlayer player) {
        double dx = player.posX - entity.posX;
        double dy = player.posY + 0.6D - entity.posY;
        double dz = player.posZ - entity.posZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < 2.25D) {
            entity.setPosition(player.posX, player.posY + 0.2D, player.posZ);
            return;
        }
        double dist = Math.sqrt(Math.max(0.0001D, distSq));
        double speed = Math.min(0.60D, 2.0D / dist);
        entity.motionX += dx / dist * speed;
        entity.motionY += dy / dist * speed + 0.04D;
        entity.motionZ += dz / dist * speed;
        entity.velocityChanged = true;
    }

    public static List<BlockPos> collectLegacyUltmine(EntityPlayer player, BlockPos origin, IBlockState originState) {
        return collectLegacyUltmine(player, origin, originState, settings(player));
    }

    private static List<BlockPos> collectLegacyUltmine(EntityPlayer player, BlockPos origin, IBlockState originState,
            UltmineSettings settings) {
        int max = settings.legacyMaxBlocks;
        List<BlockPos> out = new ArrayList<BlockPos>();
        Queue<BlockPos> queue = new ArrayDeque<BlockPos>();
        Set<BlockPos> visited = new HashSet<BlockPos>();
        queue.add(origin.toImmutable());
        visited.add(origin.toImmutable());
        String originId = blockId(originState);
        boolean connectedOres = settings.getVariant(UltmineShape112.LEGACY) == 1
                && CrossModCompatRules.isOreResourceId(originId);
        while (!queue.isEmpty() && out.size() < max) {
            BlockPos current = queue.remove();
            if (!isLoadedBlock(player.world, current)) {
                continue;
            }
            IBlockState state = player.world.getBlockState(current);
            String id = blockId(state);
            boolean match = connectedOres ? CrossModCompatRules.isOreResourceId(id) : id.equals(originId);
            if (!match || settings.legacyBlockedBlocks.contains(id)) {
                continue;
            }
            out.add(current.toImmutable());
            for (EnumFacing facing : EnumFacing.values()) {
                BlockPos next = current.offset(facing);
                if (isLoadedBlock(player.world, next) && visited.add(next.toImmutable())) {
                    queue.add(next.toImmutable());
                }
            }
        }
        return out;
    }

    public static EnumFacing faceFromLook(EntityPlayer player) {
        Vec3d look = player.getLookVec();
        double ax = Math.abs(look.x);
        double ay = Math.abs(look.y);
        double az = Math.abs(look.z);
        if (ay >= ax && ay >= az) {
            return look.y > 0.0D ? EnumFacing.UP : EnumFacing.DOWN;
        }
        if (ax >= az) {
            return look.x > 0.0D ? EnumFacing.EAST : EnumFacing.WEST;
        }
        return look.z > 0.0D ? EnumFacing.SOUTH : EnumFacing.NORTH;
    }

    public static void recordUltmineTargetFace(EntityPlayer player, BlockPos pos, EnumFacing face) {
        if (player == null || player.world == null || pos == null || face == null) {
            return;
        }
        UUID uuid = player.getUniqueID();
        ULTMINE_LAST_TARGET_POS.put(uuid, pos.toImmutable());
        ULTMINE_LAST_TARGET_FACE.put(uuid, face);
        ULTMINE_LAST_TARGET_TICK.put(uuid, player.world.getTotalWorldTime());
    }

    public static EnumFacing resolveUltmineFace(EntityPlayer player, BlockPos origin) {
        if (player == null || player.world == null || origin == null) {
            return EnumFacing.UP;
        }
        UUID uuid = player.getUniqueID();
        BlockPos lastPos = ULTMINE_LAST_TARGET_POS.get(uuid);
        EnumFacing lastFace = ULTMINE_LAST_TARGET_FACE.get(uuid);
        Long lastTick = ULTMINE_LAST_TARGET_TICK.get(uuid);
        long now = player.world.getTotalWorldTime();
        if (lastPos != null && lastFace != null && lastTick != null
                && lastPos.equals(origin) && now - lastTick.longValue() <= 10L) {
            return lastFace;
        }
        return faceFromLook(player);
    }

    public static List<BlockPos> getUltmineShapeBlocks(BlockPos origin, UltmineShape112 shape, int depth, int length,
            int variant, EnumFacing face, Vec3d lookVector) {
        Set<BlockPos> positions = new LinkedHashSet<BlockPos>();
        int safeDepth = Math.max(1, depth);
        int safeLength = Math.max(1, length);
        switch (shape) {
            case S_3x3:
                addPlanarUltmine(origin, face, 3, 3, safeDepth, positions);
                break;
            case R_2x1:
                addPlanarUltmine(origin, face, variant == 1 ? 1 : 2, variant == 1 ? 2 : 1, safeDepth, positions);
                break;
            case LINE:
                addLineUltmine(origin, safeLength, face, lookVector, positions);
                break;
            case STAIRS:
                addStairsUltmine(origin, safeDepth, face, lookVector, variant == 1, positions);
                break;
            case SQUARE_20x20_D1:
                addSquareUltmine(origin, 20, safeDepth, variant, face, positions);
                break;
            case LEGACY:
            default:
                positions.add(origin);
                break;
        }
        return new ArrayList<BlockPos>(positions);
    }

    public static void addPlanarUltmine(BlockPos origin, EnumFacing face, int width, int height, int depth,
            Set<BlockPos> out) {
        int[] axisA;
        int[] axisB;
        if (face.getAxis() == EnumFacing.Axis.Y) {
            axisA = new int[] { 1, 0, 0 };
            axisB = new int[] { 0, 0, 1 };
        } else if (face.getAxis() == EnumFacing.Axis.X) {
            axisA = new int[] { 0, 0, 1 };
            axisB = new int[] { 0, 1, 0 };
        } else {
            axisA = new int[] { 1, 0, 0 };
            axisB = new int[] { 0, 1, 0 };
        }
        int[] rangeA = centeredRange(width);
        int[] rangeB = centeredRange(height);
        int nx = -face.getFrontOffsetX();
        int ny = -face.getFrontOffsetY();
        int nz = -face.getFrontOffsetZ();
        for (int layer = 0; layer < depth; layer++) {
            for (int a = rangeA[0]; a <= rangeA[1]; a++) {
                for (int b = rangeB[0]; b <= rangeB[1]; b++) {
                    out.add(origin.add(nx * layer + axisA[0] * a + axisB[0] * b,
                            ny * layer + axisA[1] * a + axisB[1] * b,
                            nz * layer + axisA[2] * a + axisB[2] * b));
                }
            }
        }
    }

    public static void addLineUltmine(BlockPos origin, int length, EnumFacing face, Vec3d lookVector,
            Set<BlockPos> out) {
        Vec3d direction = face.getAxis().isHorizontal()
                ? safeDirection(new Vec3d(lookVector.x, 0.0D, lookVector.z), new Vec3d(face.getFrontOffsetX(), face.getFrontOffsetY(), face.getFrontOffsetZ()))
                : new Vec3d(face.getFrontOffsetX(), face.getFrontOffsetY(), face.getFrontOffsetZ());
        out.addAll(traceRayBlocks112(origin, direction, length));
    }

    public static void addStairsUltmine(BlockPos origin, int depth, EnumFacing face, Vec3d lookVector, boolean down,
            Set<BlockPos> out) {
        EnumFacing forward = snapHorizontalFacing(lookVector, face);
        int yStep = down ? -1 : 1;
        for (int i = 0; i < depth; i++) {
            BlockPos step = origin.offset(forward, i);
            out.add(new BlockPos(step.getX(), origin.getY() + i * yStep, step.getZ()));
        }
    }

    public static void addSquareUltmine(BlockPos origin, int size, int depth, int variant, EnumFacing face,
            Set<BlockPos> out) {
        int[] range = centeredRange(size);
        if (variant == 1) {
            int zDir = face == EnumFacing.SOUTH ? 1 : -1;
            for (int layer = 0; layer < depth; layer++) {
                int z = origin.getZ() + layer * zDir;
                for (int dx = range[0]; dx <= range[1]; dx++) {
                    for (int dy = range[0]; dy <= range[1]; dy++) {
                        out.add(new BlockPos(origin.getX() + dx, origin.getY() + dy, z));
                    }
                }
            }
        } else if (variant == 2) {
            int xDir = face == EnumFacing.EAST ? 1 : -1;
            for (int layer = 0; layer < depth; layer++) {
                int x = origin.getX() + layer * xDir;
                for (int dz = range[0]; dz <= range[1]; dz++) {
                    for (int dy = range[0]; dy <= range[1]; dy++) {
                        out.add(new BlockPos(x, origin.getY() + dy, origin.getZ() + dz));
                    }
                }
            }
        } else {
            for (int layer = 0; layer < depth; layer++) {
                int y = origin.getY() - layer;
                for (int dx = range[0]; dx <= range[1]; dx++) {
                    for (int dz = range[0]; dz <= range[1]; dz++) {
                        out.add(new BlockPos(origin.getX() + dx, y, origin.getZ() + dz));
                    }
                }
            }
        }
    }

    public static int[] centeredRange(int size) {
        int min = -(size / 2);
        return new int[] { min, min + size - 1 };
    }

    public static EnumFacing snapHorizontalFacing(Vec3d lookVector, EnumFacing fallback) {
        double absX = Math.abs(lookVector.x);
        double absZ = Math.abs(lookVector.z);
        if (absX < 1.0E-6D && absZ < 1.0E-6D) {
            return fallback.getAxis().isHorizontal() ? fallback : EnumFacing.NORTH;
        }
        return absX >= absZ ? (lookVector.x > 0.0D ? EnumFacing.EAST : EnumFacing.WEST)
                : (lookVector.z > 0.0D ? EnumFacing.SOUTH : EnumFacing.NORTH);
    }

    public static Vec3d safeDirection(Vec3d vector, Vec3d fallback) {
        if (vector.lengthSquared() > 1.0E-6D) {
            return vector.normalize();
        }
        return fallback.lengthSquared() > 1.0E-6D ? fallback.normalize() : new Vec3d(1.0D, 0.0D, 0.0D);
    }

    public static List<BlockPos> traceRayBlocks112(BlockPos origin, Vec3d direction, int length) {
        List<BlockPos> result = new ArrayList<BlockPos>();
        Vec3d dir = safeDirection(direction, new Vec3d(1.0D, 0.0D, 0.0D));
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();
        result.add(origin);
        int stepX = (int) Math.signum(dir.x);
        int stepY = (int) Math.signum(dir.y);
        int stepZ = (int) Math.signum(dir.z);
        double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / dir.x);
        double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / dir.y);
        double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / dir.z);
        double centerX = x + 0.5D;
        double centerY = y + 0.5D;
        double centerZ = z + 0.5D;
        double tMaxX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(((stepX > 0 ? x + 1.0D : x) - centerX) / dir.x);
        double tMaxY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(((stepY > 0 ? y + 1.0D : y) - centerY) / dir.y);
        double tMaxZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(((stepZ > 0 ? z + 1.0D : z) - centerZ) / dir.z);
        while (result.size() < length) {
            double min = Math.min(tMaxX, Math.min(tMaxY, tMaxZ));
            if (Double.compare(tMaxX, min) == 0) {
                x += stepX;
                tMaxX += tDeltaX;
            }
            if (Double.compare(tMaxY, min) == 0) {
                y += stepY;
                tMaxY += tDeltaY;
            }
            if (Double.compare(tMaxZ, min) == 0) {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
            result.add(new BlockPos(x, y, z));
        }
        return result;
    }

    private static UltmineSettings settings(EntityPlayer player) {
        if (player == null) {
            return new UltmineSettings();
        }
        UUID uuid = player.getUniqueID();
        UltmineSettings settings = ULTMINE_SETTINGS.get(uuid);
        if (settings == null) {
            settings = new UltmineSettings();
            ULTMINE_SETTINGS.put(uuid, settings);
        }
        return settings;
    }

    private static boolean boolFrom(JsonObject object, String name, boolean fallback) {
        try {
            return object.has(name) ? object.get(name).getAsBoolean() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int intFrom(JsonObject object, String name, int fallback) {
        try {
            return object.has(name) ? object.get(name).getAsInt() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Set<String> normalizedSet(JsonObject object, String name) {
        Set<String> values = new HashSet<String>();
        if (object == null || !object.has(name) || !object.get(name).isJsonArray()) {
            return values;
        }
        for (JsonElement element : object.getAsJsonArray(name)) {
            if (values.size() >= MAX_NETWORK_ID_SET_SIZE) {
                break;
            }
            if (element == null || element.isJsonNull()) {
                continue;
            }
            String value = normalizeNetworkId(element.getAsString());
            if (value.length() > 0 && value.length() <= MAX_NETWORK_ID_LENGTH) {
                values.add(value);
            }
        }
        return values;
    }

    private static String normalizeNetworkId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static int defaultDepth(UltmineShape112 shape) {
        if (shape == UltmineShape112.STAIRS) return 16;
        return 1;
    }

    private static int maxDepth(UltmineShape112 shape) {
        if (shape == UltmineShape112.LINE || shape == UltmineShape112.LEGACY) return 1;
        if (shape == UltmineShape112.STAIRS) return 64;
        if (shape == UltmineShape112.SQUARE_20x20_D1) return 4;
        return 16;
    }

    private static int defaultLength(UltmineShape112 shape) {
        if (shape == UltmineShape112.LINE) return 12;
        if (shape == UltmineShape112.SQUARE_20x20_D1) return 20;
        if (shape == UltmineShape112.R_2x1) return 2;
        if (shape == UltmineShape112.S_3x3) return 3;
        return 1;
    }

    private static int maxLength(UltmineShape112 shape) {
        if (shape == UltmineShape112.LINE) return 128;
        if (shape == UltmineShape112.SQUARE_20x20_D1) return 20;
        if (shape == UltmineShape112.R_2x1) return 2;
        if (shape == UltmineShape112.S_3x3) return 3;
        return 1;
    }

    private static int variantCount(UltmineShape112 shape) {
        if (shape == UltmineShape112.STAIRS || shape == UltmineShape112.R_2x1 || shape == UltmineShape112.LEGACY) return 2;
        if (shape == UltmineShape112.SQUARE_20x20_D1) return 3;
        return 1;
    }

    private static int maxBlocksForShape(UltmineSettings settings, UltmineShape112 shape) {
        return shape == UltmineShape112.LEGACY ? settings.legacyMaxBlocks : ultmineTuning().maxBlocksPerUse;
    }

    private static int ultmineInstantBreakThreshold() {
        return ultmineTuning().instantBreakThreshold;
    }

    private static int ultmineBlocksPerTick() {
        return ultmineTuning().blocksPerTick;
    }

    private static boolean shouldSuppressBulkBreakParticles() {
        return ultmineTuning().suppressBulkBreakParticles;
    }

    private static UltmineTuning ultmineTuning() {
        if (ultmineTuning != null) {
            return ultmineTuning;
        }
        UltmineTuning defaults = UltmineTuning.defaults();
        if (CONFIG_DIR == null) {
            ultmineTuning = defaults;
            return ultmineTuning;
        }
        Path configPath = CONFIG_DIR.resolve("murilloskills.json");
        if (!Files.isRegularFile(configPath)) {
            ultmineTuning = defaults;
            return ultmineTuning;
        }
        try {
            String json = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
            JsonElement parsed = new JsonParser().parse(json);
            JsonObject root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            JsonObject ultmine = Forge112ConfigLoader.object(root, "ultmine");
            ultmineTuning = new UltmineTuning(
                    Math.max(1, Forge112ConfigLoader.intValue(ultmine, "maxBlocksPerUse", defaults.maxBlocksPerUse)),
                    Math.max(1, Forge112ConfigLoader.intValue(ultmine, "instantBreakThreshold", defaults.instantBreakThreshold)),
                    Math.max(1, Forge112ConfigLoader.intValue(ultmine, "blocksPerTick", defaults.blocksPerTick)),
                    boolFrom(ultmine, "suppressBulkBreakParticles", defaults.suppressBulkBreakParticles));
        } catch (Exception error) {
            LOG.warn("[MurilloSkills][1.12.2][Ultmine] Failed to load tuning from {}; using defaults.",
                    configPath, error);
            ultmineTuning = defaults;
        }
        return ultmineTuning;
    }

    private static int getVanillaBlockXpDrop(World world, BlockPos pos, IBlockState state, int fortune) {
        try {
            return Math.max(0, state.getBlock().getExpDrop(state, world, pos, fortune));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static final class UltmineTuning {
        private final int maxBlocksPerUse;
        private final int instantBreakThreshold;
        private final int blocksPerTick;
        private final boolean suppressBulkBreakParticles;

        private UltmineTuning(int maxBlocksPerUse, int instantBreakThreshold, int blocksPerTick,
                boolean suppressBulkBreakParticles) {
            this.maxBlocksPerUse = maxBlocksPerUse;
            this.instantBreakThreshold = instantBreakThreshold;
            this.blocksPerTick = blocksPerTick;
            this.suppressBulkBreakParticles = suppressBulkBreakParticles;
        }

        private static UltmineTuning defaults() {
            return new UltmineTuning(4096, 160, 192, true);
        }
    }

    private static final class UltmineBreakJob {
        private final World world;
        private final BlockPos origin;
        private final List<BlockPos> targets;
        private final UltmineSettings settings;
        private int nextIndex;
        private int mined;

        private UltmineBreakJob(World world, BlockPos origin, List<BlockPos> targets, UltmineSettings settings) {
            this.world = world;
            this.origin = origin;
            this.targets = new ArrayList<BlockPos>(targets);
            this.settings = settings;
        }

        private boolean hasNext() {
            return nextIndex < targets.size();
        }

        private BlockPos next() {
            return targets.get(nextIndex++);
        }
    }

    private static final class BulkDropBuffer {
        private final boolean inventoryDrops;
        private final boolean storageDrops;
        private final boolean xpDirect;
        private final Set<String> trashItems;
        private final Set<String> storageWhitelist;
        private final BlockPos dropPos;
        private final List<ItemStack> drops = new ArrayList<ItemStack>();
        private int xp;

        private BulkDropBuffer(UltmineSettings settings, BlockPos dropPos) {
            this.inventoryDrops = settings.dropsToInventory;
            this.storageDrops = settings.dropsToStorage;
            this.xpDirect = settings.xpDirectToPlayer;
            this.trashItems = settings.trashItems == null ? Collections.<String>emptySet() : settings.trashItems;
            this.storageWhitelist = settings.storageWhitelist == null
                    ? Collections.<String>emptySet()
                    : settings.storageWhitelist;
            this.dropPos = dropPos == null ? BlockPos.ORIGIN : dropPos.toImmutable();
        }

        private void addDrops(List<ItemStack> newDrops) {
            if (newDrops == null) {
                return;
            }
            for (ItemStack stack : newDrops) {
                addDrop(stack);
            }
        }

        private void addDrop(ItemStack stack) {
            if (stack == null || stack.isEmpty() || trashItems.contains(itemId(stack))) {
                return;
            }
            ItemStack remaining = stack.copy();
            for (ItemStack existing : drops) {
                if (existing == null || existing.isEmpty() || !canMerge(existing, remaining)) {
                    continue;
                }
                int space = Math.min(existing.getMaxStackSize(), remaining.getMaxStackSize()) - existing.getCount();
                if (space <= 0) {
                    continue;
                }
                int moved = Math.min(space, remaining.getCount());
                existing.grow(moved);
                remaining.shrink(moved);
                if (remaining.isEmpty()) {
                    return;
                }
            }
            if (!remaining.isEmpty()) {
                drops.add(remaining);
            }
        }

        private boolean canMerge(ItemStack a, ItemStack b) {
            return ItemStack.areItemsEqual(a, b) && ItemStack.areItemStackTagsEqual(a, b);
        }

        private void addXp(int amount) {
            xp += Math.max(0, amount);
        }

        private void flush(EntityPlayer player, World world) {
            if (player == null || world == null) {
                return;
            }
            for (ItemStack stack : drops) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                ItemStack copy = stack.copy();
                if (routesToInventory(copy) && !player.capabilities.isCreativeMode) {
                    if (!player.inventory.addItemStackToInventory(copy) && !copy.isEmpty()) {
                        spawnDrop(world, copy);
                    }
                } else if (!player.capabilities.isCreativeMode) {
                    spawnDrop(world, copy);
                }
            }
            drops.clear();
            if (xp > 0) {
                if (xpDirect) {
                    player.addExperience(xp);
                } else {
                    world.spawnEntity(new net.minecraft.entity.item.EntityXPOrb(
                            world, dropPos.getX() + 0.5D, dropPos.getY() + 0.5D, dropPos.getZ() + 0.5D, xp));
                }
                xp = 0;
            }
        }

        private boolean routesToInventory(ItemStack stack) {
            if (inventoryDrops) {
                return true;
            }
            if (!storageDrops) {
                return false;
            }
            return storageWhitelist.isEmpty() || storageWhitelist.contains(itemId(stack));
        }

        private void spawnDrop(World world, ItemStack stack) {
            EntityItem item = new EntityItem(world,
                    dropPos.getX() + 0.5D, dropPos.getY() + 0.5D, dropPos.getZ() + 0.5D, stack);
            item.setDefaultPickupDelay();
            world.spawnEntity(item);
        }
    }

    private static final class UltmineSettings {
        private UltmineShape112 selectedShape = UltmineShape112.S_3x3;
        private final Map<UltmineShape112, ShapePrefs> shapePrefs = new EnumMap<UltmineShape112, ShapePrefs>(
                UltmineShape112.class);
        private boolean dropsToInventory = true;
        private boolean dropsToStorage;
        private boolean xpDirectToPlayer;
        private boolean sameBlockOnly;
        private boolean magnetEnabled;
        private int magnetRange = 8;
        private int legacyMaxBlocks = 1500;
        private Set<String> trashItems = new HashSet<String>();
        private Set<String> legacyBlockedBlocks = new HashSet<String>();
        private Set<String> storageWhitelist = new HashSet<String>();

        private UltmineSettings() {
            for (UltmineShape112 shape : UltmineShape112.values()) {
                shapePrefs.put(shape, new ShapePrefs(shape));
            }
        }

        private int getDepth(UltmineShape112 shape) {
            return prefs(shape).depth;
        }

        private void setDepth(UltmineShape112 shape, int value) {
            ShapePrefs prefs = prefs(shape);
            prefs.depth = clamp(value <= 0 ? defaultDepth(shape) : value, 1, maxDepth(shape));
        }

        private int getLength(UltmineShape112 shape) {
            return prefs(shape).length;
        }

        private void setLength(UltmineShape112 shape, int value) {
            ShapePrefs prefs = prefs(shape);
            prefs.length = clamp(value <= 0 ? defaultLength(shape) : value, 1, maxLength(shape));
        }

        private int getVariant(UltmineShape112 shape) {
            return prefs(shape).variant;
        }

        private void setVariant(UltmineShape112 shape, int value) {
            ShapePrefs prefs = prefs(shape);
            int count = Math.max(1, variantCount(shape));
            prefs.variant = value >= count ? 0 : Math.max(0, value);
        }

        private ShapePrefs prefs(UltmineShape112 shape) {
            UltmineShape112 safeShape = shape == null ? UltmineShape112.S_3x3 : shape;
            ShapePrefs prefs = shapePrefs.get(safeShape);
            if (prefs == null) {
                prefs = new ShapePrefs(safeShape);
                shapePrefs.put(safeShape, prefs);
            }
            return prefs;
        }

        private UltmineSettings copy() {
            UltmineSettings copy = new UltmineSettings();
            copy.selectedShape = selectedShape;
            copy.dropsToInventory = dropsToInventory;
            copy.dropsToStorage = dropsToStorage;
            copy.xpDirectToPlayer = xpDirectToPlayer;
            copy.sameBlockOnly = sameBlockOnly;
            copy.magnetEnabled = magnetEnabled;
            copy.magnetRange = magnetRange;
            copy.legacyMaxBlocks = legacyMaxBlocks;
            copy.trashItems = new HashSet<String>(trashItems);
            copy.legacyBlockedBlocks = new HashSet<String>(legacyBlockedBlocks);
            copy.storageWhitelist = new HashSet<String>(storageWhitelist);
            copy.shapePrefs.clear();
            for (Map.Entry<UltmineShape112, ShapePrefs> entry : shapePrefs.entrySet()) {
                copy.shapePrefs.put(entry.getKey(), entry.getValue().copy());
            }
            return copy;
        }
    }

    private static final class ShapePrefs {
        private int depth;
        private int length;
        private int variant;

        private ShapePrefs(UltmineShape112 shape) {
            this.depth = defaultDepth(shape);
            this.length = defaultLength(shape);
            this.variant = 0;
        }

        private ShapePrefs copy() {
            ShapePrefs copy = new ShapePrefs(UltmineShape112.S_3x3);
            copy.depth = depth;
            copy.length = length;
            copy.variant = variant;
            return copy;
        }
    }
}
