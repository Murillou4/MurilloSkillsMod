package com.murilloskills.forge112;

import com.murilloskills.forge112.api.SkillRegistry;
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
import com.murilloskills.forge112.impl.*;
import com.murilloskills.forge112.skills.*;
import com.murilloskills.forge112.utils.*;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.*;
import static com.murilloskills.forge112.dev.Forge112SelfTest.*;
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


@Mod(
        modid = MurilloSkillsForge112.MOD_ID,
        name = MurilloSkillsForge112.NAME,
        version = MurilloSkillsForge112.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        acceptableRemoteVersions = "*")
public final class MurilloSkillsForge112 {
    public static final String MOD_ID = "murilloskills";
    public static final String NAME = "Murillo Skills";
    public static final String VERSION = "1.2.75";
    public static final Logger LOG = LogManager.getLogger("MurilloSkills-112");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static SkillProgressionConfig CONFIG = SkillProgressionConfig.DEFAULT;
    public static Path CONFIG_DIR;
    public static final Forge112Store STORE = new Forge112Store();
    public static final Map<UUID, PlayerRuntime> RUNTIME = new HashMap<UUID, PlayerRuntime>();
    public static final Random RANDOM = new Random();

    public static final UUID WARRIOR_DAMAGE = UUID.fromString("3d0f8a7a-1614-4ad9-82ad-09d14d738112");
    public static final UUID WARRIOR_HEALTH = UUID.fromString("9899c597-9d19-4a70-850f-a9ff418e2a60");
    public static final UUID BLACKSMITH_KNOCKBACK = UUID.fromString("cbbbd8b2-2d3e-4e62-a9dc-40ac7e316112");
    public static final UUID WARRIOR_BERSERK_KNOCKBACK = UUID.fromString("06a52793-7f5b-46d5-8c4f-fb341112b112");
    public static final UUID BLACKSMITH_TITANIUM_KNOCKBACK = UUID.fromString("0d0b4f28-a72b-4086-9c15-a1791112bb12");
    public static final UUID EXPLORER_SPEED = UUID.fromString("b10412ba-4088-4f0e-92d3-99b500ab6112");
    public static final UUID EXPLORER_LUCK = UUID.fromString("dc5917d5-2ba2-472d-a3cb-e2e1112b3d11");
    public static final UUID BUILDER_REACH = UUID.fromString("ed9ec3af-8c1d-47a0-a97c-78e7684d6112");

    public static final Map<UUID, Long> MINER_VISION_UNTIL = new HashMap<UUID, Long>();
    public static final Map<UUID, Long> BERSERK_UNTIL = new HashMap<UUID, Long>();
    public static final Map<UUID, Long> MASTER_RANGER_UNTIL = new HashMap<UUID, Long>();
    public static final Map<UUID, Long> HARVEST_MOON_UNTIL = new HashMap<UUID, Long>();
    public static final Map<UUID, Long> RAIN_DANCE_UNTIL = new HashMap<UUID, Long>();
    public static final Map<UUID, Long> TITANIUM_AURA_UNTIL = new HashMap<UUID, Long>();
    public static final Map<UUID, Long> CREATIVE_BRUSH_UNTIL = new HashMap<UUID, Long>();
    public static final Map<UUID, Long> TREASURE_HUNTER_UNTIL = new HashMap<UUID, Long>();
    public static final Map<UUID, List<BlockPos>> MINER_VISIBLE_ORES = new HashMap<UUID, List<BlockPos>>();
    public static final Map<UUID, List<BlockPos>> TREASURE_VISIBLE_TARGETS = new HashMap<UUID, List<BlockPos>>();
    public static final Set<UUID> ULTMINE_HELD = new HashSet<UUID>();
    public static final Set<UUID> ULTMINE_RUNNING = new HashSet<UUID>();

    public static final int MINER_NIGHT_VISION_LEVEL = 10;
    public static final int MINER_AUTO_TORCH_LEVEL = 25;
    public static final int MINER_RADAR_LEVEL = 60;
    public static final int MINER_MASTER_LEVEL = 100;
    public static final int MINER_ABILITY_RADIUS = 30;
    public static final int MINER_ABILITY_DURATION_SECONDS = 420;
    public static final int WARRIOR_MASTER_LEVEL = 100;
    public static final int WARRIOR_BERSERK_DURATION_SECONDS = 10;
    public static final int WARRIOR_EXHAUSTION_DURATION_SECONDS = 5;
    public static final int ARCHER_FAST_ARROWS_LEVEL = 10;
    public static final int ARCHER_BONUS_DAMAGE_LEVEL = 25;
    public static final int ARCHER_PENETRATION_LEVEL = 50;
    public static final int ARCHER_STABLE_SHOT_LEVEL = 75;
    public static final int ARCHER_MASTER_LEVEL = 100;
    public static final int ARCHER_MASTER_RANGER_DURATION_SECONDS = 30;
    public static final int FARMER_NATURES_VITALITY_LEVEL = 35;
    public static final int FARMER_SEED_MASTER_LEVEL = 60;
    public static final int FARMER_MASTER_LEVEL = 100;
    public static final int FARMER_ABILITY_RADIUS = 8;
    public static final int FARMER_ABILITY_DURATION_SECONDS = 20;
    public static final int FISHER_OCEAN_BLESSING_LEVEL = 35;
    public static final int FISHER_DOLPHIN_GRACE_LEVEL = 50;
    public static final int FISHER_SEAS_FORTUNE_LEVEL = 60;
    public static final int FISHER_LUCK_SEA_LEVEL = 75;
    public static final int FISHER_MASTER_LEVEL = 100;
    public static final int FISHER_ABILITY_DURATION_SECONDS = 60;
    public static final int BLACKSMITH_FIRE_MASTERY_LEVEL = 35;
    public static final int BLACKSMITH_REPAIR_AURA_LEVEL = 60;
    public static final int BLACKSMITH_THORNS_LEVEL = 75;
    public static final int BLACKSMITH_MASTER_LEVEL = 100;
    public static final int BLACKSMITH_ABILITY_DURATION_SECONDS = 25;
    public static final int BUILDER_EXTENDED_REACH_LEVEL = 10;
    public static final int BUILDER_SAFE_LANDING_LEVEL = 25;
    public static final int BUILDER_BUILDERS_VIGOR_LEVEL = 35;
    public static final int BUILDER_FEATHER_BUILD_LEVEL = 60;
    public static final int BUILDER_MASTER_REACH_LEVEL = 75;
    public static final int BUILDER_MASTER_LEVEL = 100;
    public static final int BUILDER_ABILITY_DURATION_SECONDS = 120;
    public static final int EXPLORER_STEP_ASSIST_LEVEL = 10;
    public static final int EXPLORER_AQUATIC_LEVEL = 20;
    public static final int EXPLORER_NIGHT_VISION_LEVEL = 35;
    public static final int EXPLORER_PATHFINDER_LEVEL = 45;
    public static final int EXPLORER_SWIFT_RECOVERY_LEVEL = 55;
    public static final int EXPLORER_FEATHER_FEET_LEVEL = 65;
    public static final int EXPLORER_NETHER_WALKER_LEVEL = 80;
    public static final int EXPLORER_MASTER_LEVEL = 100;
    public static final int EXPLORER_TREASURE_RADIUS = 128;
    public static final int EXPLORER_TREASURE_DURATION_SECONDS = 60;

    private static void registerSkills() {
        SkillRegistry.clear();
        SkillRegistry.register(new FarmerSkill());
        SkillRegistry.register(new FisherSkill());
        SkillRegistry.register(new ArcherSkill());
        SkillRegistry.register(new MinerSkill());
        SkillRegistry.register(new BuilderSkill());
        SkillRegistry.register(new BlacksmithSkill());
        SkillRegistry.register(new ExplorerSkill());
        SkillRegistry.register(new WarriorSkill());
        SkillRegistry.logRegisteredSkills();
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Path configDir = event.getModConfigurationDirectory().toPath();
        CONFIG_DIR = configDir;
        CONFIG = Forge112ConfigLoader.load(configDir.resolve("murilloskills.json"));
        registerSkills();
        STORE.setRoot(configDir.resolve("murilloskills-1.12.2"));
        LOG.info("[MurilloSkills][1.12.2] PreInit complete. dataRoot={} maxSelectedSkills={} xpBase={} xpMultiplier={} xpExponent={}",
                STORE.root, CONFIG.getMaxSelectedSkills(), CONFIG.getXpBase(), CONFIG.getXpMultiplier(), CONFIG.getXpExponent());
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        Forge112SkillEvents events = new Forge112SkillEvents();
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance().bus().register(events);
        if (event.getSide().isClient()) {
            Forge112ClientHooks.register();
        }
        LOG.info("[MurilloSkills][1.12.2] Native Forge handlers registered for SevTech-style packs.");
    }
    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        Path worldDir = event.getServer().getEntityWorld().getSaveHandler().getWorldDirectory().toPath();
        STORE.setRoot(worldDir.resolve("murilloskills"));
        event.registerServerCommand(new SkillsCommand());
        LOG.info("[MurilloSkills][1.12.2] Server starting. worldData={}", STORE.root);
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        STORE.saveAll();
        LOG.info("[MurilloSkills][1.12.2] Server stopping. Saved {} players.", STORE.size());
    }
}
