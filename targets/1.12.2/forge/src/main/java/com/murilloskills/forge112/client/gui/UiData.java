package com.murilloskills.forge112.client.gui;

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

public final class UiData {
    private UiData() {
    }

    public static String title(SkillType skill) {
        String name = skill.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public static int skillColor(SkillType skill) {
        switch (skill) {
            case MINER: return 0xFFE0A817;
            case WARRIOR: return 0xFFFF5656;
            case ARCHER: return 0xFF7BBF4B;
            case FARMER: return 0xFF90D35C;
            case FISHER: return 0xFF5BC0EB;
            case BLACKSMITH: return 0xFFB0A08A;
            case BUILDER: return 0xFFD07A4D;
            case EXPLORER: return 0xFFB7A7FF;
            default: return 0xFFD9DCF2;
        }
    }

    public static ItemStack itemForSkill(SkillType skill) {
        switch (skill) {
            case FARMER: return new ItemStack(Items.WHEAT);
            case FISHER: return new ItemStack(Items.FISHING_ROD);
            case ARCHER: return new ItemStack(Items.BOW);
            case MINER: return new ItemStack(Items.IRON_PICKAXE);
            case BUILDER: return new ItemStack(Blocks.BRICK_BLOCK);
            case BLACKSMITH: return new ItemStack(Blocks.ANVIL);
            case EXPLORER: return new ItemStack(Items.COMPASS);
            case WARRIOR: return new ItemStack(Items.IRON_SWORD);
            default: return ItemStack.EMPTY;
        }
    }

    public static String description(SkillType skill) {
        switch (skill) {
            case MINER: return "Mining progression built around break speed, underground utility and ore reveal.";
            case WARRIOR: return "Close combat progression with damage, health, resistance and lifesteal.";
            case ARCHER: return "Bow progression with arrow speed, damage scaling, focus and ranger uptime.";
            case FARMER: return "Crop progression with double harvest, growth acceleration and area harvest.";
            case FISHER: return "Fishing progression with catch speed, treasure chance, luck and Rain Dance.";
            case BLACKSMITH: return "Gear progression with fire mastery, repair aura, machine acceleration and Titanium Aura.";
            case BUILDER: return "Building progression with reach, safer falls, vigor and Creative Brush placement.";
            case EXPLORER: return "Travel progression with speed, step assist, survival tools and treasure reveal.";
            default: return "";
        }
    }

    public static String ability(SkillType skill) {
        switch (skill) {
            case MINER: return "Master Miner reveals nearby ores for 7 minutes.";
            case WARRIOR: return "Berserk grants Strength IV, Speed II, Resistance II and lifesteal for 10 seconds.";
            case ARCHER: return "Master Ranger empowers arrows and focus shots for 30 seconds.";
            case FARMER: return "Harvest Moon auto-harvests and replants mature crops around you for 20 seconds.";
            case FISHER: return "Rain Dance grants Luck III and boosted fishing rewards for 60 seconds.";
            case BLACKSMITH: return "Titanium Aura grants resistance, knockback immunity and repairs gear for 25 seconds.";
            case BUILDER: return "Creative Brush lets placed blocks expand into a 3x3 brush for 120 seconds.";
            case EXPLORER: return "Treasure Hunter reveals nearby chests and spawners for 60 seconds.";
            default: return "";
        }
    }

    public static String[] passives(SkillType skill) {
        switch (skill) {
            case MINER:
                return new String[] {
                        "+3% mining speed per level.",
                        "L10 Night Vision underground.",
                        "L25 Auto Torch toggle.",
                        "L50 Haste II.",
                        "L60 Ore Radar support.",
                        "L75 stronger resource output." };
            case WARRIOR:
                return new String[] {
                        "+0.20 attack damage per level.",
                        "+1 max health heart every 10 levels.",
                        "L25 Iron Skin damage reduction.",
                        "L75 Vampirism lifesteal.",
                        "Berserk adds knockback immunity." };
            case ARCHER:
                return new String[] {
                        "+3% arrow damage per level.",
                        "L10 faster arrows.",
                        "L25 bonus damage.",
                        "L50 penetration damage.",
                        "L75 stable shot support.",
                        "Master Ranger multiplies arrow power." };
            case FARMER:
                return new String[] {
                        "+1% double harvest chance per level.",
                        "L10 Green Thumb bonus.",
                        "L25 Fertile Ground crop ticks.",
                        "L35 regeneration on natural ground.",
                        "L60 Seed Master Haste.",
                        "L75 Abundant Harvest bonus." };
            case FISHER:
                return new String[] {
                        "Fishing rewards scale with level.",
                        "L10 faster fishing support.",
                        "L25 treasure chance.",
                        "L35 Ocean Blessing underwater night vision.",
                        "L50 water speed.",
                        "L60 Sea's Fortune Luck.",
                        "L75 Luck of the Sea bonus." };
            case BLACKSMITH:
                return new String[] {
                        "Crafting, smelting and machine interaction XP.",
                        "L25 efficient machine ticks.",
                        "L35 Fire Mastery.",
                        "L50 forged resilience.",
                        "L60 Repair Aura.",
                        "L75 knockback resistance.",
                        "L99 master enchanter support." };
            case BUILDER:
                return new String[] {
                        "+0.08 reach per level.",
                        "L10 +1 reach.",
                        "L25 Safe Landing.",
                        "L35 Builder's Vigor Haste.",
                        "L60 Feather Build protection.",
                        "L75 +5 master reach." };
            case EXPLORER:
                return new String[] {
                        "+0.4% movement speed per level.",
                        "L10 Step Assist.",
                        "L20 Aquatic breathing.",
                        "L35 Night Vision toggle.",
                        "L45 Pathfinder speed.",
                        "L55 Swift Recovery.",
                        "L65 Feather Feet.",
                        "L80 Nether Walker." };
            default:
                return new String[0];
        }
    }

    public static Perk[] perks(SkillType skill) {
        switch (skill) {
            case MINER:
                return new Perk[] {
                        new Perk(10, "Night Vision", "See underground while mining."),
                        new Perk(30, "Durability", "Mining tools last longer in supported flows."),
                        new Perk(60, "Ore Radar", "Ore reveal support unlocks."),
                        new Perk(75, "Resource Fortune", "Higher output from mining progression."),
                        new Perk(100, "Master Miner", "Reveal nearby ores.") };
            case WARRIOR:
                return new Perk[] {
                        new Perk(10, "+1 Heart", "Extra max health."),
                        new Perk(25, "Iron Skin", "Incoming damage reduction."),
                        new Perk(50, "+1 Heart", "More max health."),
                        new Perk(75, "Vampirism", "Heal from melee damage."),
                        new Perk(100, "Berserk", "Temporary combat surge.") };
            case FARMER:
                return new Perk[] {
                        new Perk(10, "Green Thumb", "More harvest chance."),
                        new Perk(25, "Fertile Ground", "Nearby crops grow faster."),
                        new Perk(35, "Nature's Vitality", "Regeneration on natural ground."),
                        new Perk(50, "Nutrient Cycle", "Crop progression improves."),
                        new Perk(60, "Seed Master", "Haste while farming."),
                        new Perk(75, "Abundant Harvest", "More double harvest chance."),
                        new Perk(100, "Harvest Moon", "Auto-harvest mature crops.") };
            case ARCHER:
                return new Perk[] {
                        new Perk(10, "Fast Arrows", "Arrows fly faster."),
                        new Perk(25, "Bonus Damage", "Arrow damage bonus."),
                        new Perk(50, "Penetration", "More arrow damage."),
                        new Perk(75, "Stable Shot", "Focus support."),
                        new Perk(100, "Master Ranger", "Temporary ranged power.") };
            case FISHER:
                return new Perk[] {
                        new Perk(10, "Fast Fishing", "Fishing support improves."),
                        new Perk(25, "Treasure Hunter", "Better bonus catch rolls."),
                        new Perk(35, "Ocean Blessing", "Underwater night vision."),
                        new Perk(50, "Dolphin's Grace", "Water speed in 1.12.2."),
                        new Perk(60, "Sea's Fortune", "Luck while fishing."),
                        new Perk(75, "Luck of the Sea", "More bonus catch chance."),
                        new Perk(100, "Rain Dance", "Timed fishing luck.") };
            case BLACKSMITH:
                return new Perk[] {
                        new Perk(10, "Iron Skin", "Early resilience."),
                        new Perk(25, "Efficient Anvil", "Machine and gear flow improves."),
                        new Perk(35, "Fire Mastery", "Fire resistance."),
                        new Perk(50, "Forged Resilience", "Generic damage reduction."),
                        new Perk(60, "Repair Aura", "Repairs equipped gear."),
                        new Perk(75, "Thorns Master", "Knockback resistance."),
                        new Perk(99, "Master Enchanter", "Late blacksmith support."),
                        new Perk(100, "Titanium Aura", "Timed defensive aura.") };
            case BUILDER:
                return new Perk[] {
                        new Perk(10, "Extended Reach", "+1 reach."),
                        new Perk(15, "Efficient Crafting", "Building craft XP support."),
                        new Perk(25, "Safe Landing", "Reduced fall damage."),
                        new Perk(35, "Builder's Vigor", "Haste while building."),
                        new Perk(50, "Scaffold Master", "Placement flow improves."),
                        new Perk(60, "Feather Build", "High build protection."),
                        new Perk(75, "Master Reach", "+5 reach."),
                        new Perk(100, "Creative Brush", "3x3 placement brush.") };
            case EXPLORER:
                return new Perk[] {
                        new Perk(10, "Step Assist", "Step up full blocks."),
                        new Perk(20, "Aquatic", "Water breathing."),
                        new Perk(35, "Night Vision", "Night vision toggle."),
                        new Perk(45, "Pathfinder", "Movement speed improves."),
                        new Perk(55, "Swift Recovery", "Regeneration when hurt."),
                        new Perk(65, "Feather Feet", "Fall damage reduction."),
                        new Perk(80, "Nether Walker", "Fire resistance in Nether."),
                        new Perk(100, "Treasure Hunter", "Reveal nearby treasure.") };
            default:
                return new Perk[0];
        }
    }

    public static String[] synergies(SkillType skill) {
        List<String> out = new ArrayList<String>();
        for (String line : allSynergies()) {
            if (line.toUpperCase(Locale.ROOT).contains(skill.name())) {
                out.add(line);
            }
        }
        return out.toArray(new String[out.size()]);
    }

    public static String[] allSynergies() {
        return new String[] {
                "Iron Will - WARRIOR + BLACKSMITH: extra damage reduction.",
                "Forge Master - MINER + BLACKSMITH: stronger ore and gear flow.",
                "Ranger - ARCHER + EXPLORER: movement and ranged uptime.",
                "Nature's Bounty - FARMER + FISHER: all drops improve.",
                "Treasure Hunter - MINER + EXPLORER: rare find support.",
                "Combat Master - WARRIOR + ARCHER: all damage improves.",
                "Master Crafter - BUILDER + BLACKSMITH: crafting support.",
                "Survivor - WARRIOR + EXPLORER: damage reduction.",
                "Industrial - MINER + BUILDER: building and mining crafting support.",
                "Sea Warrior - WARRIOR + FISHER: combat damage support.",
                "Green Archer - FARMER + ARCHER: movement speed support.",
                "Prospector - MINER + WARRIOR: ore drop support.",
                "Adventurer - BUILDER + EXPLORER: all drops support.",
                "Hermit - FARMER + BUILDER: crafting support." };
    }

        static final class Perk {
        final int level;
        final String name;
        final String detail;

        Perk(int level, String name, String detail) {
            this.level = level;
            this.name = name;
            this.detail = detail;
        }
    }
}
