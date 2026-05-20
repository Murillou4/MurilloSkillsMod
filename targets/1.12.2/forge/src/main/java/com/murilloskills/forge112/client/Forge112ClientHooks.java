package com.murilloskills.forge112.client;

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

@SideOnly(Side.CLIENT)
public final class Forge112ClientHooks {
    private static final String CATEGORY = "key.category.murilloskills.keybinds";
    private static boolean postOptionsKeyAliasApplied;
    public static final KeyBinding OPEN = new KeyBinding("key.murilloskills.open_gui", Keyboard.KEY_O, CATEGORY);
    public static final KeyBinding ABILITY = new KeyBinding("key.murilloskills.use_ability", Keyboard.KEY_Z, CATEGORY);
    public static final KeyBinding AREA_PLANTING = new KeyBinding("key.murilloskills.area_planting_toggle", Keyboard.KEY_G, CATEGORY);
    public static final KeyBinding HOLLOW_FILL = new KeyBinding("key.murilloskills.hollow_fill_toggle", Keyboard.KEY_H, CATEGORY);
    public static final KeyBinding NIGHT_VISION = new KeyBinding("key.murilloskills.night_vision_toggle", Keyboard.KEY_N, CATEGORY);
    public static final KeyBinding STEP_ASSIST = new KeyBinding("key.murilloskills.step_assist_toggle", Keyboard.KEY_V, CATEGORY);
    public static final KeyBinding ULTPLACE = new KeyBinding("key.murilloskills.ultplace_toggle", Keyboard.KEY_C, CATEGORY);
    public static final KeyBinding SPEED = new KeyBinding("key.murilloskills.speed_boost_toggle", Keyboard.KEY_B, CATEGORY);
    public static final KeyBinding CONFIG = new KeyBinding("key.murilloskills.ultplace_config", Keyboard.KEY_K, CATEGORY);
    public static final KeyBinding FILL = new KeyBinding("key.murilloskills.fill_mode_cycle", Keyboard.KEY_J, CATEGORY);
    public static final KeyBinding ULTMINE = new KeyBinding("key.murilloskills.vein_miner_toggle", Keyboard.KEY_PERIOD, CATEGORY);
    public static final KeyBinding DROPS = new KeyBinding("key.murilloskills.vein_miner_drops_toggle", Keyboard.KEY_COMMA, CATEGORY);
    public static final KeyBinding TORCH = new KeyBinding("key.murilloskills.auto_torch_toggle", Keyboard.KEY_T, CATEGORY);
    public static final KeyBinding MELT = new KeyBinding("key.murilloskills.melting_touch_toggle", Keyboard.KEY_M, CATEGORY);
    public static final KeyBinding MENU = new KeyBinding("key.murilloskills.ultmine_menu", Keyboard.KEY_APOSTROPHE, CATEGORY);

    public static void register() {
        for (KeyBinding key : new KeyBinding[] { OPEN, ABILITY, AREA_PLANTING, HOLLOW_FILL, NIGHT_VISION, STEP_ASSIST,
                ULTPLACE, SPEED, CONFIG, FILL, ULTMINE, DROPS, TORCH, MELT, MENU }) {
            ClientRegistry.registerKeyBinding(key);
        }
        applyUltmineKeyAlias();
        ClientUltmineConfig.load();
        ClientOreFilterConfig.load();
        MinecraftForge.EVENT_BUS.register(Forge112ClientHudOverlay.class);
        MinecraftForge.EVENT_BUS.register(Forge112NotificationHud.class);
        MinecraftForge.EVENT_BUS.register(Forge112StatusHud.class);
        MinecraftForge.EVENT_BUS.register(Forge112ClientWorldRenderer.class);
        FMLCommonHandler.instance().bus().register(Forge112ClientTickHandler.class);
        FMLCommonHandler.instance().bus().register(Forge112ClientInputHandler.class);
        LOG.info("[MurilloSkills][1.12.2][Client] Keybinds registered without duplicates: O/Z/G/H/N/V/C/B/K/J/./,/T/M/'");
        LOG.info("[MurilloSkills][1.12.2][Client] Ultmine hold key is {} ({})",
                getUltmineKeyName(), getUltmineKeyCode());
    }

    public static void ensurePostOptionsKeyAliases() {
        if (postOptionsKeyAliasApplied) {
            return;
        }
        postOptionsKeyAliasApplied = true;
        applyUltmineKeyAlias();
        LOG.info("[MurilloSkills][1.12.2][Client] Post-options Ultmine hold key is {} ({})",
                getUltmineKeyName(), getUltmineKeyCode());
    }

    private static void applyUltmineKeyAlias() {
        int primary = readOptionKey("key_key.murilloskills.vein_miner_toggle", Integer.MIN_VALUE);
        int alias = readOptionKey("key_key.murilloskills.ultmine_hold", Integer.MIN_VALUE);
        int staleUltPlace = readOptionKey("key_key.murilloskills.ultplace_toggle", Integer.MIN_VALUE);
        if (alias != Integer.MIN_VALUE && alias != 0 && (primary == Integer.MIN_VALUE || primary == Keyboard.KEY_PERIOD)) {
            setUltmineKeyCode(alias);
            LOG.info("[MurilloSkills][1.12.2][Client] Migrated legacy ultmine_hold key alias to {} ({})",
                    getUltmineKeyName(), alias);
        } else if ((primary == Integer.MIN_VALUE || primary == Keyboard.KEY_PERIOD)
                && staleUltPlace != Integer.MIN_VALUE && staleUltPlace != 0 && staleUltPlace != Keyboard.KEY_C) {
            setUltmineKeyCode(staleUltPlace);
            ULTPLACE.setKeyCode(Keyboard.KEY_C);
            KeyBinding.resetKeyBindingArrayAndHash();
            LOG.info("[MurilloSkills][1.12.2][Client] Recovered stale Ultmine hold key from ultplace_toggle: ultmine={} ({}) ultplace=C ({})",
                    getUltmineKeyName(), staleUltPlace, Keyboard.KEY_C);
        }
    }

    private static int readOptionKey(String name, int fallback) {
        try {
            Path path = Minecraft.getMinecraft().mcDataDir.toPath().resolve("options.txt");
            if (!Files.isRegularFile(path)) {
                return fallback;
            }
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line != null && line.startsWith(name + ":")) {
                    return Integer.parseInt(line.substring(name.length() + 1).trim());
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static void setUltmineKeyCode(int keyCode) {
        ULTMINE.setKeyCode(keyCode);
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    public static int getUltmineKeyCode() {
        return ULTMINE.getKeyCode();
    }

    public static String getUltmineKeyName() {
        return getKeyName(ULTMINE);
    }

    public static String getKeyName(KeyBinding key) {
        return key == null ? "NONE" : keyName(key.getKeyCode());
    }

    public static int getKeyCode(KeyBinding key) {
        return key == null ? 0 : key.getKeyCode();
    }

    private static String keyName(int code) {
        if (code < 0) {
            return "MOUSE" + (code + 101);
        }
        String name = code == 0 ? "NONE" : Keyboard.getKeyName(code);
        return name == null ? String.valueOf(code) : name;
    }
}
