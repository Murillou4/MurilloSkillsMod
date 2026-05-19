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
    private Forge112MiningTools() {
    }

    public static void placeAutoTorch(EntityPlayer player) {
        BlockPos pos = player.getPosition();
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
        BlockPos center = player.getPosition();
        for (BlockPos pos : BlockPos.getAllInBox(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            if (found.size() >= limit) {
                break;
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
        UUID uuid = player.getUniqueID();
        ULTMINE_RUNNING.add(uuid);
        try {
            ClientUltmineConfig.load();
            UltmineShape112 shape = ClientUltmineConfig.getSelectedShape();
            List<BlockPos> candidates = shape == UltmineShape112.LEGACY
                    ? collectLegacyUltmine(player, origin, originState)
                    : getUltmineShapeBlocks(origin, shape, ClientUltmineConfig.getDepth(shape),
                            ClientUltmineConfig.getLength(shape), ClientUltmineConfig.getVariant(shape),
                            faceFromLook(player), player.getLookVec());
            int max = Math.max(1, Math.min(ClientUltmineConfig.getLegacyMaxBlocks(), candidates.size()));
            String originId = blockId(originState);
            int mined = 0;
            for (BlockPos pos : candidates) {
                if (mined >= max - 1) {
                    break;
                }
                if (pos == null || pos.equals(origin)) {
                    continue;
                }
                IBlockState state = player.world.getBlockState(pos);
                if (!canUltmineBlock(player, pos, state, originId, shape)) {
                    continue;
                }
                if (mineUltmineBlock(player, pos, state)) {
                    mined++;
                }
            }
            return mined;
        } finally {
            ULTMINE_RUNNING.remove(uuid);
        }
    }

    public static boolean canUltmineBlock(EntityPlayer player, BlockPos pos, IBlockState state, String originId,
            UltmineShape112 shape) {
        if (player == null || pos == null || state == null || state.getBlock() == Blocks.AIR) {
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
        if (ClientUltmineConfig.isLegacyBlockedBlock(id)) {
            return false;
        }
        if (shape == UltmineShape112.LEGACY && ClientUltmineConfig.getVariant(shape) == 1) {
            return CrossModCompatRules.isOreResourceId(id);
        }
        if (ClientUltmineConfig.isSameBlockOnly()) {
            return id.equals(originId);
        }
        return block.getPlayerRelativeBlockHardness(state, player, player.world, pos) > 0.0F
                || player.capabilities.isCreativeMode;
    }

    public static boolean mineUltmineBlock(EntityPlayer player, BlockPos pos, IBlockState state) {
        World world = player.world;
        if (ClientUltmineConfig.isDropsToInventory() && !player.capabilities.isCreativeMode) {
            int fortune = EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.FORTUNE,
                    player.getHeldItemMainhand());
            List<ItemStack> drops = state.getBlock().getDrops(world, pos, state, fortune);
            world.playEvent(2001, pos, Block.getStateId(state));
            world.setBlockToAir(pos);
            for (ItemStack drop : drops) {
                if (drop == null || drop.isEmpty() || ClientUltmineConfig.isTrashItem(itemId(drop))) {
                    continue;
                }
                ItemStack copy = drop.copy();
                if (!player.inventory.addItemStackToInventory(copy)) {
                    player.dropItem(copy, false);
                }
            }
        } else {
            if (!world.destroyBlock(pos, !player.capabilities.isCreativeMode)) {
                return false;
            }
        }
        ItemStack held = player.getHeldItemMainhand();
        if (held != null && !held.isEmpty() && !player.capabilities.isCreativeMode) {
            held.damageItem(1, player);
        }
        return true;
    }

    public static List<BlockPos> collectLegacyUltmine(EntityPlayer player, BlockPos origin, IBlockState originState) {
        int max = ClientUltmineConfig.getLegacyMaxBlocks();
        List<BlockPos> out = new ArrayList<BlockPos>();
        Queue<BlockPos> queue = new ArrayDeque<BlockPos>();
        Set<BlockPos> visited = new HashSet<BlockPos>();
        queue.add(origin.toImmutable());
        visited.add(origin.toImmutable());
        String originId = blockId(originState);
        boolean connectedOres = ClientUltmineConfig.getVariant(UltmineShape112.LEGACY) == 1;
        while (!queue.isEmpty() && out.size() < max) {
            BlockPos current = queue.remove();
            IBlockState state = player.world.getBlockState(current);
            String id = blockId(state);
            boolean match = connectedOres ? CrossModCompatRules.isOreResourceId(id) : id.equals(originId);
            if (!match || ClientUltmineConfig.isLegacyBlockedBlock(id)) {
                continue;
            }
            out.add(current.toImmutable());
            for (EnumFacing facing : EnumFacing.values()) {
                BlockPos next = current.offset(facing);
                if (visited.add(next.toImmutable())) {
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
