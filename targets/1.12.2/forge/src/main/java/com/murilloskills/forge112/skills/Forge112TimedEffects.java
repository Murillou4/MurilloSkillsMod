package com.murilloskills.forge112.skills;

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

public final class Forge112TimedEffects {
    private Forge112TimedEffects() {
    }

    public static boolean isTimedActive(EntityPlayer player, Map<UUID, Long> state) {
        if (player == null || player.world == null) {
            return false;
        }
        Long until = state.get(player.getUniqueID());
        return until != null && until.longValue() > player.world.getTotalWorldTime();
    }

    public static void activateTimed(EntityPlayer player, Map<UUID, Long> state, int seconds, String label) {
        long until = player.world.getTotalWorldTime() + seconds * 20L;
        state.put(player.getUniqueID(), Long.valueOf(until));
        LOG.info("[MurilloSkills][1.12.2][Ability] {} active for {} until tick {}", label, player.getName(), until);
    }

    public static boolean clearExpired(EntityPlayer player, Map<UUID, Long> state, String label) {
        Long until = state.get(player.getUniqueID());
        if (until != null && until.longValue() <= player.world.getTotalWorldTime()) {
            state.remove(player.getUniqueID());
            LOG.info("[MurilloSkills][1.12.2][Ability] {} expired for {}", label, player.getName());
            return true;
        }
        return false;

    public static void tickTimedAbilities(EntityPlayer player, PlayerSkillDataCore data, PlayerRuntime runtime) {
        UUID id = player.getUniqueID();
        boolean minerExpired = clearExpired(player, MINER_VISION_UNTIL, "Master Miner");
        boolean treasureExpired = clearExpired(player, TREASURE_HUNTER_UNTIL, "Treasure Hunter");
        clearExpired(player, BERSERK_UNTIL, "Berserk");
        clearExpired(player, MASTER_RANGER_UNTIL, "Master Ranger");
        clearExpired(player, HARVEST_MOON_UNTIL, "Harvest Moon");
        clearExpired(player, RAIN_DANCE_UNTIL, "Rain Dance");
        clearExpired(player, TITANIUM_AURA_UNTIL, "Titanium Aura");
        clearExpired(player, CREATIVE_BRUSH_UNTIL, "Creative Brush");
        if (minerExpired) {
            MINER_VISIBLE_ORES.remove(id);
        }
        if (treasureExpired) {
            TREASURE_VISIBLE_TARGETS.remove(id);
        }
        if (isTimedActive(player, MINER_VISION_UNTIL)) {
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 220, 0, true, false));
        }
        if (isTimedActive(player, BERSERK_UNTIL)) {
            player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 45, 3, true, true));
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 45, 1, true, true));
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 45, 1, true, true));
        }
        if (isTimedActive(player, MASTER_RANGER_UNTIL)) {
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 45, 1, true, true));
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 220, 0, true, false));
        }
        if (isTimedActive(player, HARVEST_MOON_UNTIL) && runtime.ticks % 20 == 0) {
            int harvested = performHarvestMoon(player);
            if (harvested > 0) {
                addXp(player, SkillType.FARMER, harvested * 10, "harvest moon");
                LOG.info("[MurilloSkills][1.12.2][Farmer] Harvest Moon processed {} crops for {}", harvested, player.getName());
            }
        }
        if (isTimedActive(player, RAIN_DANCE_UNTIL)) {
            player.addPotionEffect(new PotionEffect(MobEffects.LUCK, 80, 2, true, true));
            player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 80, 0, true, false));
        }
        if (isTimedActive(player, TITANIUM_AURA_UNTIL)) {
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 80, 1, true, true));
            player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 80, 0, true, true));
            player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 60, 0, true, true));
            if (runtime.ticks % 60 == 0) {
                int repaired = repairEquippedGear(player, 3);
                if (repaired > 0) {
                    LOG.info("[MurilloSkills][1.12.2][Blacksmith] Titanium Aura repaired {} durability for {}", repaired, player.getName());
                }
            }
        }
        if (isTimedActive(player, TREASURE_HUNTER_UNTIL)) {
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 220, 0, true, false));
        }
    }

    public static int performHarvestMoon(EntityPlayer player) {
        int harvested = 0;
        for (BlockPos pos : BlockPos.getAllInBox(player.getPosition().add(-FARMER_ABILITY_RADIUS, -1, -FARMER_ABILITY_RADIUS),
                player.getPosition().add(FARMER_ABILITY_RADIUS, 1, FARMER_ABILITY_RADIUS))) {
            IBlockState state = player.world.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof BlockCrops) {
                BlockCrops crop = (BlockCrops) block;
                if (!crop.isMaxAge(state)) {
                    continue;
                }
                List<ItemStack> drops = crop.getDrops(player.world, pos, state, 0);
                for (ItemStack drop : drops) {
                    if (!drop.isEmpty()) {
                        ItemStack copy = drop.copy();
                        copy.setCount(Math.max(1, copy.getCount() * 3));
                        player.world.spawnEntity(new EntityItem(player.world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, copy));
                    }
                }
                player.world.setBlockState(pos, crop.withAge(0), 3);
                harvested++;
            } else if (block instanceof IGrowable && RANDOM.nextInt(4) == 0) {
                try {
                    ((IGrowable) block).grow(player.world, RANDOM, pos, state);
                    harvested++;
                } catch (Throwable error) {
                    LOG.debug("[MurilloSkills][1.12.2][Farmer] Harvest Moon skipped {}: {}", pos, error.toString());
                }
            }
        }
        return harvested;
    }

    public static int repairEquippedGear(EntityPlayer player, int amountPerItem) {
        int repaired = 0;
        repaired += repairStack(player.getHeldItemMainhand(), amountPerItem);
        repaired += repairStack(player.getHeldItemOffhand(), amountPerItem);
        for (ItemStack stack : player.getArmorInventoryList()) {
            repaired += repairStack(stack, amountPerItem);
        }
        return repaired;
    }

    public static int repairStack(ItemStack stack, int amount) {
        if (stack == null || stack.isEmpty() || !stack.isItemDamaged()) {
            return 0;
        }
        int before = stack.getItemDamage();
        stack.setItemDamage(Math.max(0, before - amount));
        return before - stack.getItemDamage();
    }

    public static int creativeBrushFill(EntityPlayer player, World world, BlockPos origin, IBlockState state) {
        if (world == null || state == null || state.getBlock() == Blocks.AIR) {
            return 0;
        }
        PlayerSkillDataCore data = data(player);
        boolean hollow = data.getToggle(SkillType.BUILDER, "hollow_fill", false);
        int placed = 0;
        int radius = 1;
        for (BlockPos pos : BlockPos.getAllInBox(origin.add(-radius, 0, -radius), origin.add(radius, 0, radius))) {
            if (pos.equals(origin)) {
                continue;
            }
            boolean edge = Math.abs(pos.getX() - origin.getX()) == radius || Math.abs(pos.getZ() - origin.getZ()) == radius;
            if (hollow && !edge) {
                continue;
            }
            IBlockState current = world.getBlockState(pos);
            if (current.getBlock().isReplaceable(world, pos)) {
                world.setBlockState(pos, state, 3);
                placed++;
            }
        }
        return placed;
    }
}
