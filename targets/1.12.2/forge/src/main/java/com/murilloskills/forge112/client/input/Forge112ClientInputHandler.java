package com.murilloskills.forge112.client.input;

import com.murilloskills.forge112.MurilloSkillsForge112;
import com.murilloskills.forge112.client.*;
import com.murilloskills.forge112.client.config.*;
import com.murilloskills.forge112.client.data.UltPlaceClientState112;
import com.murilloskills.forge112.client.gui.*;
import com.murilloskills.forge112.client.input.*;
import com.murilloskills.forge112.client.render.*;
import com.murilloskills.forge112.commands.*;
import com.murilloskills.forge112.config.*;
import com.murilloskills.forge112.data.*;
import com.murilloskills.forge112.dev.*;
import com.murilloskills.forge112.events.*;
import com.murilloskills.forge112.network.ModNetwork112;
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
public final class Forge112ClientInputHandler {
    private static final Map<KeyBinding, Boolean> HELD_KEYS = new HashMap<KeyBinding, Boolean>();
    private static final KeyBinding[] DIAGNOSTIC_KEYS = new KeyBinding[] {
            Forge112ClientHooks.OPEN, Forge112ClientHooks.ABILITY, Forge112ClientHooks.AREA_PLANTING,
            Forge112ClientHooks.HOLLOW_FILL, Forge112ClientHooks.NIGHT_VISION, Forge112ClientHooks.STEP_ASSIST,
            Forge112ClientHooks.ULTPLACE, Forge112ClientHooks.SPEED, Forge112ClientHooks.CONFIG,
            Forge112ClientHooks.FILL, Forge112ClientHooks.ULTMINE, Forge112ClientHooks.DROPS,
            Forge112ClientHooks.TORCH, Forge112ClientHooks.MELT, Forge112ClientHooks.MENU
    };

    @SubscribeEvent
    public static void onKey(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        logMurilloKeyEvent();
        if (consumePress(Forge112ClientHooks.OPEN)) {
            mc.displayGuiScreen(new SkillsGuiParity());
        }
        if (consumePress(Forge112ClientHooks.ABILITY)) {
            if (data(mc.player).getParagonSkills().size() > 1) {
                mc.displayGuiScreen(new ParagonAbilityGui112());
            } else {
                mc.player.sendChatMessage("/murilloskills ability");
            }
        }
        keyToggle(Forge112ClientHooks.AREA_PLANTING, "FARMER", "area_planting");
        keyToggle(Forge112ClientHooks.HOLLOW_FILL, "BUILDER", "hollow_fill");
        keyToggle(Forge112ClientHooks.NIGHT_VISION, "EXPLORER", "night_vision");
        keyToggle(Forge112ClientHooks.STEP_ASSIST, "EXPLORER", "step_assist");
        if (consumePress(Forge112ClientHooks.ULTPLACE)) {
            boolean enabled = UltPlaceClientState112.toggleEnabled();
            LOG.info("[MurilloSkills][1.12.2][Client] One-shot BUILDER.ultplace from key {} ({})",
                    Forge112ClientHooks.getKeyName(Forge112ClientHooks.ULTPLACE),
                    Forge112ClientHooks.getKeyCode(Forge112ClientHooks.ULTPLACE));
            mc.player.sendChatMessage("/murilloskills toggle BUILDER ultplace");
            Forge112NotificationHud.addLocalCard("UltPlace", enabled ? "Enabled" : "Disabled",
                    UltPlaceClientState112.summary(), Palette.ACCENT_BLUE);
        }
        keyToggle(Forge112ClientHooks.SPEED, "EXPLORER", "speed_boost");
        if (consumePress(Forge112ClientHooks.CONFIG)) {
            mc.displayGuiScreen(new UltPlaceConfigGui112(null));
        }
        keyToggle(Forge112ClientHooks.FILL, "BUILDER", "fill_mode");
        if (consumePress(Forge112ClientHooks.DROPS)) {
            ClientUltmineConfig.toggleDropsToInventory();
            ClientUltmineConfig.save();
            ModNetwork112.sendUltmineConfigToServer();
            Forge112NotificationHud.addLocalCard("Ultmine",
                    ClientUltmineConfig.isDropsToInventory() ? "Inventory drops" : "World drops",
                    "Drops to inventory = " + ClientUltmineConfig.isDropsToInventory(), Palette.ACCENT_GOLD);
            if (Boolean.getBoolean("murilloskills.chat112")) {
                mc.player.sendMessage(new TextComponentString("MurilloSkills Ultmine dropsToInventory = "
                        + ClientUltmineConfig.isDropsToInventory()));
            }
        }
        keyToggle(Forge112ClientHooks.TORCH, "MINER", "auto_torch");
        keyToggle(Forge112ClientHooks.MELT, "BLACKSMITH", "melting_touch");
    }

    private static void keyToggle(KeyBinding key, String skill, String name) {
        if (consumePress(key)) {
            LOG.info("[MurilloSkills][1.12.2][Client] One-shot {}.{} from key {} ({})",
                    skill, name, Forge112ClientHooks.getKeyName(key), Forge112ClientHooks.getKeyCode(key));
            Minecraft.getMinecraft().player.sendChatMessage("/murilloskills toggle " + skill + " " + name);
        }
    }

    private static boolean consumePress(KeyBinding key) {
        if (isReservedHoldConflict(key)) {
            return false;
        }
        boolean down = isBindingDown(key);
        boolean wasDown = Boolean.TRUE.equals(HELD_KEYS.get(key));
        HELD_KEYS.put(key, down);
        return down && !wasDown;
    }

    private static boolean isReservedHoldConflict(KeyBinding key) {
        return sameBinding(key, Forge112ClientHooks.ULTMINE) || sameBinding(key, Forge112ClientHooks.MENU);
    }

    private static boolean sameBinding(KeyBinding a, KeyBinding b) {
        return a != null && b != null && a != b && a.getKeyCode() == b.getKeyCode() && a.getKeyCode() != 0;
    }

    private static boolean isBindingDown(KeyBinding key) {
        if (key == null) {
            return false;
        }
        int code = key.getKeyCode();
        if (code < 0) {
            return Mouse.isButtonDown(code + 100);
        }
        return code > 0 && Keyboard.isKeyDown(code);
    }

    private static void logMurilloKeyEvent() {
        if (!Keyboard.getEventKeyState()) {
            return;
        }
        int eventKey = Keyboard.getEventKey();
        if (eventKey <= 0) {
            return;
        }
        KeyBinding matched = findMurilloKey(eventKey);
        if (matched == null) {
            return;
        }
        LOG.info("[MurilloSkills][1.12.2][Client] KeyInput {} ({}) matched {} | ultmine={} ({}) radial={} ({}) ultplace={} ({})",
                Keyboard.getKeyName(eventKey), eventKey, matched.getKeyDescription(),
                Forge112ClientHooks.getUltmineKeyName(), Forge112ClientHooks.getUltmineKeyCode(),
                Forge112ClientHooks.getKeyName(Forge112ClientHooks.MENU), Forge112ClientHooks.getKeyCode(Forge112ClientHooks.MENU),
                Forge112ClientHooks.getKeyName(Forge112ClientHooks.ULTPLACE), Forge112ClientHooks.getKeyCode(Forge112ClientHooks.ULTPLACE));
    }

    private static KeyBinding findMurilloKey(int eventKey) {
        for (KeyBinding key : DIAGNOSTIC_KEYS) {
            if (key != null && key.getKeyCode() == eventKey) {
                return key;
            }
        }
        return null;
    }
}
