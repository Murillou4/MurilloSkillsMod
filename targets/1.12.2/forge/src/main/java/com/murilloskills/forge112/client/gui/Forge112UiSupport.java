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

public final class Forge112UiSupport {
    private Forge112UiSupport() {
    }

    public static int intFrom(JsonObject object, String name, int fallback) {
        try {
            JsonElement element = object.get(name);
            return element == null || element.isJsonNull() ? fallback : element.getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }

    public static String normalizeId(String id) {
        if (id == null) {
            return "";
        }
        String value = id.trim().toLowerCase(Locale.ROOT);
        if (value.length() == 0) {
            return "";
        }
        return value.indexOf(':') >= 0 ? value : "minecraft:" + value;
    }

    public static void drawPanelBorder(int x, int y, int w, int h, int color) {
        GuiScreen.drawRect(x, y, x + w, y + 1, color);
        GuiScreen.drawRect(x, y + h - 1, x + w, y + h, color);
        GuiScreen.drawRect(x, y, x + 1, y + h, color);
        GuiScreen.drawRect(x + w - 1, y, x + w, y + h, color);
    }

    public static void renderCornerAccents(int x, int y, int w, int h, int size, int color) {
        GuiScreen.drawRect(x, y, x + size, y + 1, color);
        GuiScreen.drawRect(x, y, x + 1, y + size, color);
        GuiScreen.drawRect(x + w - size, y, x + w, y + 1, color);
        GuiScreen.drawRect(x + w - 1, y, x + w, y + size, color);
        GuiScreen.drawRect(x, y + h - 1, x + size, y + h, color);
        GuiScreen.drawRect(x, y + h - size, x + 1, y + h, color);
        GuiScreen.drawRect(x + w - size, y + h - 1, x + w, y + h, color);
        GuiScreen.drawRect(x + w - 1, y + h - size, x + w, y + h, color);
    }

    public static int scrollbarThumbHeight(int trackH, int visible, int total) {
        if (trackH <= 0 || total <= visible) {
            return 0;
        }
        return Math.max(18, trackH * Math.max(1, visible) / Math.max(1, total));
    }

    public static int scrollbarThumbY(int trackY, int trackH, int thumbH, int scroll, int maxScroll) {
        if (maxScroll <= 0 || trackH <= thumbH) {
            return trackY;
        }
        return trackY + (trackH - thumbH) * clamp(scroll, 0, maxScroll) / maxScroll;
    }

    public static int scrollbarScrollFromMouse(int mouseY, int trackY, int trackH, int thumbH, int maxScroll,
            int grabOffset) {
        if (maxScroll <= 0 || trackH <= thumbH) {
            return 0;
        }
        int y = mouseY - trackY - grabOffset;
        return clamp(y * maxScroll / Math.max(1, trackH - thumbH), 0, maxScroll);
    }

    public static void renderScrollbar(int trackX, int trackY, int trackH, int thumbY, int thumbH) {
        if (trackH <= 0 || thumbH <= 0) {
            return;
        }
        GuiScreen.drawRect(trackX, trackY, trackX + 5, trackY + trackH, Palette.PROGRESS_BAR_EMPTY);
        GuiScreen.drawRect(trackX + 1, thumbY, trackX + 4, thumbY + thumbH, Palette.ACCENT_GOLD);
        GuiScreen.drawRect(trackX, thumbY, trackX + 5, thumbY + 1, 0x55FFFFFF);
    }

    public static void renderProgressBar(int x, int y, int w, int h, float progress, int empty, int fill, int shine) {
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        GuiScreen.drawRect(x, y, x + w, y + h, empty);
        int fillW = Math.round((w - 2) * progress);
        if (fillW > 0) {
            GuiScreen.drawRect(x + 1, y + 1, x + 1 + fillW, y + h - 1, fill);
            GuiScreen.drawRect(x + 1, y + 1, x + 1 + fillW, y + 2, shine);
        }
        drawPanelBorder(x, y, w, h, Palette.SECTION_BORDER);
    }

    public static void renderMilestoneMarker(int x, int y, int size, boolean unlocked, boolean master) {
        int fill = unlocked ? (master ? Palette.ACCENT_GOLD : Palette.PROGRESS_BAR_FILL) : Palette.TEXT_MUTED;
        int border = master ? Palette.ACCENT_GOLD : Palette.SECTION_BORDER;
        GuiScreen.drawRect(x, y, x + size, y + size, Palette.PROGRESS_BAR_EMPTY);
        GuiScreen.drawRect(x + 1, y + 1, x + size - 1, y + size - 1, fill);
        drawPanelBorder(x, y, size, size, border);
    }

    public static void renderGlowingBorder(int x, int y, int w, int h, int color) {
        GuiScreen.drawRect(x - 2, y - 2, x + w + 2, y + h + 2, color);
        GuiScreen.drawRect(x - 1, y - 1, x + w + 1, y + h + 1, color);
    }

    public static boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int prestigeColor(int prestige) {
        if (prestige >= 10) return 0xFFFF55FF;
        if (prestige >= 7) return 0xFFFFD700;
        if (prestige >= 4) return 0xFF55FFFF;
        return 0xFFDD88FF;
    }

    public static boolean isSynergyActive(PlayerSkillDataCore data, String synergy) {
        SkillType[] pair = synergyPair(synergy);
        return pair[0] != null && pair[1] != null && data.isSkillSelected(pair[0]) && data.isSkillSelected(pair[1]);
    }

    public static SkillType[] synergyPair(String synergy) {
        SkillType first = null;
        SkillType second = null;
        for (SkillType skill : SkillType.values()) {
            if (synergy.toUpperCase(Locale.ROOT).contains(skill.name())) {
                if (first == null) {
                    first = skill;
                } else if (second == null && first != skill) {
                    second = skill;
                    break;
                }
            }
        }
        return new SkillType[] { first, second };
    }

    public static String synergyName(String synergy) {
        int dash = synergy.indexOf(" - ");
        return dash > 0 ? synergy.substring(0, dash) : synergy;
    }

    public static String synergyBonus(String synergy) {
        int colon = synergy.indexOf(": ");
        return colon > 0 ? synergy.substring(colon + 2) : synergy;
    }

    public static String pairName(SkillType skill) {
        return skill == null ? "?" : UiData.title(skill);
    }

    public static String whyChoose(SkillType skill) {
        switch (skill) {
            case MINER: return "Choose Miner for faster block breaking, ore utility, cave visibility and mining-focused quality of life.";
            case WARRIOR: return "Choose Warrior for direct combat strength, more health, survivability and the Berserk burst window.";
            case ARCHER: return "Choose Archer for ranged damage scaling, faster arrows and Master Ranger uptime.";
            case FARMER: return "Choose Farmer for crop automation, stronger harvests and passive recovery around natural blocks.";
            case FISHER: return "Choose Fisher for faster fishing, treasure odds, water bonuses and Rain Dance reward windows.";
            case BLACKSMITH: return "Choose Blacksmith for gear durability, machine speed, repairs, fire mastery and Titanium Aura.";
            case BUILDER: return "Choose Builder for reach, fall safety, placement utility and the Creative Brush active.";
            case EXPLORER: return "Choose Explorer for mobility, survival traversal, treasure reveal and travel quality of life.";
            default: return "";
        }
    }

    public static String[] xpSources(SkillType skill) {
        switch (skill) {
            case MINER: return new String[] { "Mine ores and stone", "Use mining utilities", "Reveal ore clusters" };
            case WARRIOR: return new String[] { "Defeat mobs", "Deal melee damage", "Take combat damage" };
            case ARCHER: return new String[] { "Hit targets with arrows", "Land stronger shots", "Defeat mobs at range" };
            case FARMER: return new String[] { "Harvest mature crops", "Plant and grow crops", "Use farming utilities" };
            case FISHER: return new String[] { "Catch fish", "Find treasure catches", "Fish during ability windows" };
            case BLACKSMITH: return new String[] { "Craft gear", "Smelt items", "Repair, enchant and use machines" };
            case BUILDER: return new String[] { "Place building blocks", "Craft building materials", "Use fill and brush tools" };
            case EXPLORER: return new String[] { "Travel distance", "Discover structures", "Open treasure and survive hazards" };
            default: return new String[0];
        }
    }

    public static GuiButton flatButton(int id, int x, int y, int w, int h, String text) {
        return new FlatButton112(id, x, y, w, h, text);
    }

    public static GuiButton invisibleButton(int id, int x, int y, int w, int h) {
        return new InvisibleButton112(id, x, y, w, h);
    }

    @SideOnly(Side.CLIENT)
    public static void renderItemStack(Minecraft mc, ItemStack stack, int x, int y, float scale) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
        GlStateManager.popMatrix();
        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
