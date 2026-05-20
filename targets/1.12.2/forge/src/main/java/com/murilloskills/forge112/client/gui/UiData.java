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
            case MINER: return "XP from ores and mining blocks; scales mining speed, skill fortune, Auto Torch, Ore Radar, Ultmine and ore reveal.";
            case WARRIOR: return "XP from melee hits and kills; scales attack damage, max health, damage reduction, resistance and lifesteal.";
            case ARCHER: return "XP from arrow hits and kills; scales arrow damage, arrow speed, focus shots and Master Ranger uptime.";
            case FARMER: return "XP from mature crops and Harvest Moon; scales double harvest, plant growth, regeneration and crop automation.";
            case FISHER: return "XP from catches; scales bonus fish/drop rolls, water utility, Luck effects and Rain Dance rewards.";
            case BLACKSMITH: return "XP from crafting, smelting, machine interaction and gear work; scales defenses, machines, repair and over-enchanting.";
            case BUILDER: return "XP from placing, breaking and crafting building blocks; scales reach, fall safety, high-build protection and Creative Brush.";
            case EXPLORER: return "XP from movement, loot containers and dimensions; scales speed, Luck, toggles, fall safety and treasure reveal.";
            default: return "";
        }
    }

    public static String ability(SkillType skill) {
        switch (skill) {
            case MINER: return "Master Miner: 7 min Night Vision and ore reveal in a 30 block radius.";
            case WARRIOR: return "Berserk: 10s Strength IV, Speed II, Resistance II, knockback immunity and 50% lifesteal.";
            case ARCHER: return "Master Ranger: 30s Speed II, Night Vision, faster critical arrows and a primed 1.5x Focus shot.";
            case FARMER: return "Harvest Moon: 20s crop automation in an 8 block radius; mature crops drop triple and replant.";
            case FISHER: return "Rain Dance: 60s Luck III, Water Breathing, +30% bonus catch chance and doubled bonus fish.";
            case BLACKSMITH: return "Titanium Aura: 25s Resistance II, Fire Resistance, Regeneration, knockback immunity and gear repair.";
            case BUILDER: return "Creative Brush: 120s toggle that expands placed blocks into a 3x3 brush.";
            case EXPLORER: return "Treasure Hunter: 60s Night Vision, Speed II, Water Breathing and chest/spawner reveal in 128 blocks.";
            default: return "";
        }
    }

    public static String[] passives(SkillType skill) {
        switch (skill) {
            case MINER:
                return new String[] {
                        "Mining Speed: +3%/level, prestige scaled.",
                        "Skill Fortune: +0.03/level, +0.5/prestige.",
                        "Fortune works on ores, glowstone and axe leaves.",
                        "L75/prestige: Fortune works on broader resources.",
                        "L10 Night Vision underground.",
                        "L25 Auto Torch toggle places torches in dark caves.",
                        "L50 Haste II instead of Haste I.",
                        "L60 Ore Radar support and active ore reveal rendering.",
                        "Ultmine, ore filter and mining previews stay tied to Miner." };
            case WARRIOR:
                return new String[] {
                        "Attack Damage: +0.20/level, prestige scaled.",
                        "Max Health: +1 heart each 10 levels.",
                        "L25 Iron Skin: 15% incoming damage reduction.",
                        "L75 Resistance I passive uptime.",
                        "L75 Vampirism: 15% melee lifesteal.",
                        "Berserk adds +40% damage reduction and 50% lifesteal.",
                        "Berserk gives full knockback immunity while active." };
            case ARCHER:
                return new String[] {
                        "Arrow Damage: +3%/level, prestige scaled.",
                        "L10 Fast Arrows: arrows fly 25% faster.",
                        "L25 Bonus Damage: +5% ranged damage.",
                        "L50 Penetration: +10% ranged damage in 1.12.2.",
                        "Focus Shot: next focused arrow deals 1.5x damage.",
                        "Master Ranger: critical arrows and 1.75x damage.",
                        "Master Ranger also grants Speed II and Night Vision uptime." };
            case FARMER:
                return new String[] {
                        "Double Harvest: +1%/level, prestige scaled.",
                        "L10 Green Thumb: +5% double harvest chance.",
                        "L25 Fertile Ground: nearby crop growth chance unlocks.",
                        "Fertile Ground ticks nearby plants every 4s.",
                        "L35 Nature's Vitality: Regeneration on natural ground.",
                        "L60 Seed Master: Haste I while farming.",
                        "L75 Abundant Harvest: +15% double harvest chance.",
                        "Harvest Moon repeatedly harvests/replants mature crops." };
            case FISHER:
                return new String[] {
                        "Bonus Catch: level-scaled fish/drop chance, prestige scaled.",
                        "L25 Treasure Hunter: +5% bonus catch chance.",
                        "L35 Ocean Blessing: Night Vision while underwater.",
                        "L50 Dolphin's Grace: Speed I while in water.",
                        "L60 Sea's Fortune: Luck I passive uptime.",
                        "L75 Luck of the Sea: +15% bonus catch chance.",
                        "Rain Dance adds Luck III and +30% bonus chance.",
                        "Rain Dance doubled bonus fish and lightly repairs the rod." };
            case BLACKSMITH:
                return new String[] {
                        "Crafting, smelting and machine interaction XP.",
                        "L25 Efficient Anvil: lower over-enchant costs.",
                        "L25 Machine Boost: nearby supported machines tick faster.",
                        "L35 Fire Mastery: Fire Resistance and defense.",
                        "L50 Forged Resilience: 10% generic damage reduction.",
                        "L60 Repair Aura: Titanium Aura repairs gear.",
                        "L75 Thorns Master: 50% knockback resistance.",
                        "L99 Master Enchanter: over-enchants to level 8.",
                        "Max prestige guarantees table upgrades to the level 8 cap." };
            case BUILDER:
                return new String[] {
                        "Reach: +0.08 block/level, prestige scaled.",
                        "L10 Extended Reach: +1 extra block reach.",
                        "L25 Safe Landing: fall damage cut in half.",
                        "Fall distance reduction also scales with level and prestige.",
                        "L35 Builder's Vigor: Haste I while selected.",
                        "L60 Feather Build: Resistance while sneaking above Y100.",
                        "L75 Master Reach: +5 extra block reach.",
                        "Creative Brush: 3x3 placement and hollow toggle." };
            case EXPLORER:
                return new String[] {
                        "Movement Speed: +0.4%/level, prestige scaled.",
                        "Luck: +1 Luck every 20 levels.",
                        "L10 Step Assist toggle: step up full blocks.",
                        "L20 Aquatic: Water Breathing while in water.",
                        "L35 Night Vision toggle.",
                        "L45 Pathfinder: extra speed, stronger while sprinting.",
                        "L55 Swift Recovery: Regeneration below half health.",
                        "L65 Feather Feet: fall damage reduced by 60%.",
                        "L80 Nether Walker: Fire Resistance in the Nether.",
                        "Treasure Hunter reveals chests and spawners nearby." };
            default:
                return new String[0];
        }
    }

    public static Perk[] perks(SkillType skill) {
        switch (skill) {
            case MINER:
                return new Perk[] {
                        new Perk(10, "Night Vision", "See underground while mining."),
                        new Perk(25, "Auto Torch", "Toggle torch placement in dark caves."),
                        new Perk(50, "Haste II", "Mining passive upgrades from Haste I to Haste II."),
                        new Perk(60, "Ore Radar", "Ore reveal support unlocks."),
                        new Perk(75, "Resource Fortune", "Skill Fortune applies broadly to resource drops."),
                        new Perk(100, "Master Miner", "Reveal ores for 7 minutes in a 30 block radius.") };
            case WARRIOR:
                return new Perk[] {
                        new Perk(10, "Battle Hardened", "+1 heart through max-health scaling."),
                        new Perk(25, "Iron Skin", "15% incoming damage reduction."),
                        new Perk(50, "Veteran Body", "More max health through scaling."),
                        new Perk(75, "Vampirism", "15% melee lifesteal and Resistance I uptime."),
                        new Perk(100, "Berserk", "Strength IV, Speed II, Resistance II and 50% lifesteal.") };
            case FARMER:
                return new Perk[] {
                        new Perk(10, "Green Thumb", "+5% double harvest chance."),
                        new Perk(25, "Fertile Ground", "Nearby growable plants and crop ticks accelerate."),
                        new Perk(35, "Nature's Vitality", "Regeneration on natural ground."),
                        new Perk(50, "Nutrient Cycle", "Crop growth and harvest scaling continue."),
                        new Perk(60, "Seed Master", "Haste I while farming."),
                        new Perk(75, "Abundant Harvest", "+15% double harvest chance."),
                        new Perk(100, "Harvest Moon", "Timed harvest/replant automation with triple crop drops.") };
            case ARCHER:
                return new Perk[] {
                        new Perk(10, "Fast Arrows", "Arrows fly 25% faster."),
                        new Perk(25, "Bonus Damage", "+5% ranged damage."),
                        new Perk(50, "Penetration", "+10% ranged damage in 1.12.2."),
                        new Perk(75, "Stable Shot", "Focus shot support for the next empowered arrow."),
                        new Perk(100, "Master Ranger", "Timed critical arrows, Speed II and 1.75x active damage.") };
            case FISHER:
                return new Perk[] {
                        new Perk(10, "Fast Fishing", "Fishing reward scaling continues."),
                        new Perk(25, "Treasure Hunter", "+5% bonus fish/drop roll chance."),
                        new Perk(35, "Ocean Blessing", "Underwater Night Vision."),
                        new Perk(50, "Dolphin's Grace", "Speed I while in water."),
                        new Perk(60, "Sea's Fortune", "Luck I passive uptime."),
                        new Perk(75, "Luck of the Sea", "+15% bonus catch chance."),
                        new Perk(100, "Rain Dance", "Luck III, Water Breathing and doubled bonus fish.") };
            case BLACKSMITH:
                return new Perk[] {
                        new Perk(10, "Iron Skin", "Blacksmith defense path begins."),
                        new Perk(25, "Efficient Anvil", "Discounts over-enchant costs and speeds nearby machines."),
                        new Perk(35, "Fire Mastery", "Fire Resistance and stronger fire/explosion defense."),
                        new Perk(50, "Forged Resilience", "10% generic damage reduction."),
                        new Perk(60, "Repair Aura", "Repair support through Titanium Aura."),
                        new Perk(75, "Thorns Master", "50% knockback resistance."),
                        new Perk(99, "Master Enchanter", "Table and anvil over-enchants up to level 8."),
                        new Perk(100, "Titanium Aura", "Timed resistance, regeneration, knockback immunity and repair.") };
            case BUILDER:
                return new Perk[] {
                        new Perk(10, "Extended Reach", "+1 extra block reach."),
                        new Perk(15, "Efficient Crafting", "Building craft XP support."),
                        new Perk(25, "Safe Landing", "Fall damage cut in half."),
                        new Perk(35, "Builder's Vigor", "Haste I while selected."),
                        new Perk(50, "Scaffold Master", "Building placement path continues."),
                        new Perk(60, "Feather Build", "Resistance while sneaking above Y100."),
                        new Perk(75, "Master Reach", "+5 extra block reach."),
                        new Perk(100, "Creative Brush", "120s 3x3 placement brush toggle.") };
            case EXPLORER:
                return new Perk[] {
                        new Perk(10, "Step Assist", "Toggle step-up to full blocks."),
                        new Perk(20, "Aquatic", "Water Breathing while in water."),
                        new Perk(35, "Night Vision", "Toggle Night Vision."),
                        new Perk(45, "Pathfinder", "Extra speed, stronger while sprinting."),
                        new Perk(55, "Swift Recovery", "Regeneration below half health."),
                        new Perk(65, "Feather Feet", "60% fall damage reduction."),
                        new Perk(80, "Nether Walker", "Fire Resistance in the Nether."),
                        new Perk(100, "Treasure Hunter", "Reveal nearby chests and spawners.") };
            default:
                return new Perk[0];
        }
    }

    public static void validateTooltipCoverage() {
        for (SkillType skill : SkillType.values()) {
            if (ability(skill).trim().isEmpty()) {
                throw new IllegalStateException("missing ability text for " + skill);
            }
            if (passives(skill).length == 0) {
                throw new IllegalStateException("missing passive text for " + skill);
            }
            if (perks(skill).length == 0) {
                throw new IllegalStateException("missing perk text for " + skill);
            }
            String text = tooltipCoverageText(skill);
            for (String token : coverageTokens(skill)) {
                requireCoverage(skill, text, token);
            }
        }
    }

    private static String tooltipCoverageText(SkillType skill) {
        StringBuilder builder = new StringBuilder();
        append(builder, title(skill));
        append(builder, description(skill));
        append(builder, ability(skill));
        for (String passive : passives(skill)) {
            append(builder, passive);
        }
        for (Perk perk : perks(skill)) {
            append(builder, perk.name);
            append(builder, perk.detail);
        }
        for (String synergy : synergies(skill)) {
            append(builder, synergy);
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private static void append(StringBuilder builder, String value) {
        if (value != null) {
            builder.append(' ').append(value);
        }
    }

    private static void requireCoverage(SkillType skill, String text, String token) {
        if (!text.contains(token.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("missing tooltip coverage for " + skill + ": " + token);
        }
    }

    private static String[] coverageTokens(SkillType skill) {
        switch (skill) {
            case MINER:
                return new String[] { "skill fortune", "auto torch", "ore radar", "ultmine", "master miner" };
            case WARRIOR:
                return new String[] { "attack damage", "max health", "iron skin", "vampirism", "berserk" };
            case ARCHER:
                return new String[] { "arrow damage", "fast arrows", "focus shot", "master ranger", "critical" };
            case FARMER:
                return new String[] { "double harvest", "fertile ground", "nature's vitality", "seed master", "harvest moon" };
            case FISHER:
                return new String[] { "bonus catch", "ocean blessing", "dolphin's grace", "sea's fortune", "rain dance" };
            case BLACKSMITH:
                return new String[] { "efficient anvil", "fire mastery", "forged resilience", "repair aura", "master enchanter", "titanium aura" };
            case BUILDER:
                return new String[] { "reach", "safe landing", "builder's vigor", "feather build", "creative brush" };
            case EXPLORER:
                return new String[] { "movement speed", "step assist", "night vision", "pathfinder", "swift recovery", "treasure hunter" };
            default:
                return new String[0];
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
