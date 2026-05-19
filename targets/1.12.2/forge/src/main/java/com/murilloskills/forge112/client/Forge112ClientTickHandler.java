package com.murilloskills.forge112.client;

import com.murilloskills.forge112.MurilloSkillsForge112;
import com.murilloskills.forge112.client.*;
import com.murilloskills.forge112.client.config.*;
import com.murilloskills.forge112.client.data.UltmineClientState112;
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
public final class Forge112ClientTickHandler {
    private static boolean autoWorldStarted;
    private static boolean autoUiOpened;
    private static boolean ultmineKeyHeld;
    private static boolean ultmineMenuHeld;
    private static int uiSelfTestStep;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (Boolean.getBoolean("murilloskills.autoworld") && !autoWorldStarted
                && mc.world == null && mc.currentScreen instanceof GuiMainMenu) {
            autoWorldStarted = true;
            WorldSettings settings = new WorldSettings(1122L, GameType.CREATIVE, true, false, WorldType.FLAT);
            settings.enableCommands();
            LOG.info("[MurilloSkills][1.12.2][ClientSmoke] Launching clean disposable world CodexSmoke112.");
            mc.launchIntegratedServer("CodexSmoke112", "CodexSmoke112", settings);
        }
        if (Boolean.getBoolean("murilloskills.openui") && !autoUiOpened
                && mc.player != null && mc.world != null && mc.currentScreen == null && mc.player.ticksExisted > 80) {
            autoUiOpened = true;
            mc.displayGuiScreen(new SkillsGuiParity());
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] PASS opened SkillsGui.");
        }
        if (Boolean.getBoolean("murilloskills.clientUiSelfTest112")
                && mc.player != null && mc.world != null && mc.player.ticksExisted > 80) {
            runClientUiSelfTest(mc);
        }
        if (mc.player == null || mc.world == null) {
            return;
        }
        boolean held = Forge112ClientHooks.ULTMINE.isKeyDown();
        UltmineClientState112.setHeld(held);
        if (held != ultmineKeyHeld) {
            ultmineKeyHeld = held;
            mc.player.sendChatMessage("/murilloskills clientstate ultmine_hold " + held);
        }
        boolean menuHeld = Forge112ClientHooks.MENU.isKeyDown();
        if (menuHeld && !ultmineMenuHeld && mc.currentScreen == null) {
            ultmineMenuHeld = true;
            mc.displayGuiScreen(new UltmineRadialGui112());
        } else if (!menuHeld) {
            ultmineMenuHeld = false;
        }
    }

    private static void runClientUiSelfTest(Minecraft mc) {
        int tick = mc.player.ticksExisted;
        if (uiSelfTestStep == 0) {
            uiSelfTestStep = 1;
            mc.displayGuiScreen(new SkillsGuiParity());
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened SkillsGuiParity");
        } else if (uiSelfTestStep == 1 && tick > 110) {
            uiSelfTestStep = 2;
            mc.displayGuiScreen(new OreFilterGui112(new SkillsGuiParity()));
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened OreFilterGui112");
        } else if (uiSelfTestStep == 2 && tick > 140) {
            uiSelfTestStep = 3;
            mc.displayGuiScreen(new UltmineConfigGui112(new SkillsGuiParity()));
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened UltmineConfigGui112");
        } else if (uiSelfTestStep == 3 && tick > 170) {
            uiSelfTestStep = 4;
            mc.displayGuiScreen(new UltmineListPickerGui112(new UltmineConfigGui112(new SkillsGuiParity()), false));
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened TrashItemPicker112");
        } else if (uiSelfTestStep == 4 && tick > 200) {
            uiSelfTestStep = 5;
            mc.displayGuiScreen(new UltmineListPickerGui112(new UltmineConfigGui112(new SkillsGuiParity()), true));
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened ClassicBlockPicker112");
        } else if (uiSelfTestStep == 5 && tick > 230) {
            uiSelfTestStep = 6;
            mc.displayGuiScreen(new UltmineRadialGui112());
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened UltmineRadialGui112");
        } else if (uiSelfTestStep == 6 && tick > 260) {
            uiSelfTestStep = 7;
            mc.displayGuiScreen(null);
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] PASS");
            if (Boolean.getBoolean("murilloskills.clientUiSelfTestExit112")) {
                mc.shutdown();
            }
        }
    }
}
