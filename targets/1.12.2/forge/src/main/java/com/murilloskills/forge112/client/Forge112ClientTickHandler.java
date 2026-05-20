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
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
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
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
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
    private static boolean ultmineConfigSynced;
    private static long lastPreviewRequestTick = -1L;
    private static int lastPreviewLogCount = -1;
    private static int uiSelfTestStep;
    private static int uiSelfTestTicks;
    private static boolean tooltipCoverageChecked;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Forge112ClientHooks.ensurePostOptionsKeyAliases();
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
            ultmineConfigSynced = false;
            UltmineClientState112.clearPreview();
            return;
        }
        if (!ultmineConfigSynced) {
            ultmineConfigSynced = true;
            ModNetwork112.sendUltmineConfigToServer();
            UltmineShape112 shape = ClientUltmineConfig.getSelectedShape();
            ModNetwork112.sendUltmineSelection(shape, ClientUltmineConfig.getDepth(shape),
                    ClientUltmineConfig.getLength(shape), ClientUltmineConfig.getVariant(shape),
                    ClientUltmineConfig.getLegacyMaxBlocks());
            applyClientUltmineConfig(mc.player, ClientUltmineConfig.toNetworkJson());
            setUltmineSelection(mc.player, shape, ClientUltmineConfig.getDepth(shape),
                    ClientUltmineConfig.getLength(shape), ClientUltmineConfig.getVariant(shape),
                    ClientUltmineConfig.getLegacyMaxBlocks());
        }
        boolean held = isUltmineHoldDown();
        UltmineClientState112.setHeld(held);
        setUltmineHeld(mc.player, held);
        if (held != ultmineKeyHeld) {
            ultmineKeyHeld = held;
            ModNetwork112.sendUltmineHeld(held);
            LOG.info("[MurilloSkills][1.12.2][Client] Ultmine hold {} via key {} ({})",
                    held ? "pressed" : "released", Forge112ClientHooks.getUltmineKeyName(),
                    Forge112ClientHooks.getUltmineKeyCode());
            if (!held) {
                UltmineClientState112.clearPreview();
                lastPreviewLogCount = -1;
            }
        }
        if (held) {
            RayTraceResult hit = mc.objectMouseOver;
            if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK && hit.getBlockPos() != null) {
                UltmineClientState112.setPreview(getValidatedUltminePreview(mc.player,
                        hit.getBlockPos(), hit.sideHit));
            } else {
                UltmineClientState112.clearPreview();
            }
            long time = mc.world.getTotalWorldTime();
            if (time != lastPreviewRequestTick && time % 4L == 0L) {
                lastPreviewRequestTick = time;
                if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK && hit.getBlockPos() != null) {
                    ModNetwork112.requestUltminePreview(hit.getBlockPos(), hit.sideHit);
                }
            }
            int previewCount = UltmineClientState112.getPreviewBlocks();
            if (previewCount != lastPreviewLogCount) {
                lastPreviewLogCount = previewCount;
                LOG.info("[MurilloSkills][1.12.2][Client] Ultmine preview {} blocks for shape {}",
                        previewCount, ClientUltmineConfig.getSelectedShape());
            }
        }
        boolean menuHeld = isBindingDown(Forge112ClientHooks.MENU);
        if (menuHeld && !ultmineMenuHeld && mc.currentScreen == null) {
            ultmineMenuHeld = true;
            LOG.info("[MurilloSkills][1.12.2][Client] Ultmine radial pressed via key {} ({})",
                    Forge112ClientHooks.getKeyName(Forge112ClientHooks.MENU),
                    Forge112ClientHooks.getKeyCode(Forge112ClientHooks.MENU));
            mc.displayGuiScreen(new UltmineRadialGui112());
        } else if (!menuHeld && ultmineMenuHeld) {
            ultmineMenuHeld = false;
            LOG.info("[MurilloSkills][1.12.2][Client] Ultmine radial released via key {} ({})",
                    Forge112ClientHooks.getKeyName(Forge112ClientHooks.MENU),
                    Forge112ClientHooks.getKeyCode(Forge112ClientHooks.MENU));
            if (mc.currentScreen instanceof UltmineRadialGui112) {
                ((UltmineRadialGui112) mc.currentScreen).releaseAndClose();
            }
        }
    }

    private static boolean isUltmineHoldDown() {
        return isBindingDown(Forge112ClientHooks.ULTMINE);
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

    private static void runClientUiSelfTest(Minecraft mc) {
        int tick = ++uiSelfTestTicks;
        if (uiSelfTestStep == 0) {
            if (!tooltipCoverageChecked) {
                tooltipCoverageChecked = true;
                UiData.validateTooltipCoverage();
                LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] tooltip coverage PASS");
            }
            uiSelfTestStep = 1;
            mc.displayGuiScreen(new SkillsGuiParity());
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened SkillsGuiParity");
        } else if (uiSelfTestStep == 1 && tick > 110) {
            saveSmokeScreenshot(mc, "skills");
            uiSelfTestStep = 2;
            mc.displayGuiScreen(new OreFilterGui112(new SkillsGuiParity()));
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened OreFilterGui112");
        } else if (uiSelfTestStep == 2 && tick > 140) {
            saveSmokeScreenshot(mc, "ore_filter");
            uiSelfTestStep = 3;
            mc.displayGuiScreen(new UltmineConfigGui112(new SkillsGuiParity()));
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened UltmineConfigGui112");
        } else if (uiSelfTestStep == 3 && tick > 170) {
            saveSmokeScreenshot(mc, "ultmine_config");
            uiSelfTestStep = 4;
            mc.displayGuiScreen(new UltmineListPickerGui112(new UltmineConfigGui112(new SkillsGuiParity()), false));
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened TrashItemPicker112");
        } else if (uiSelfTestStep == 4 && tick > 200) {
            saveSmokeScreenshot(mc, "trash_picker");
            uiSelfTestStep = 5;
            mc.displayGuiScreen(new UltmineListPickerGui112(new UltmineConfigGui112(new SkillsGuiParity()), true));
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened ClassicBlockPicker112");
        } else if (uiSelfTestStep == 5 && tick > 230) {
            saveSmokeScreenshot(mc, "classic_picker");
            uiSelfTestStep = 6;
            mc.displayGuiScreen(new UltmineRadialGui112());
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened UltmineRadialGui112");
        } else if (uiSelfTestStep == 6 && tick > 260) {
            saveSmokeScreenshot(mc, "ultmine_radial");
            uiSelfTestStep = 7;
            mc.displayGuiScreen(new GuiControls(new GuiOptions(new SkillsGuiParity(), mc.gameSettings), mc.gameSettings));
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened Controls");
        } else if (uiSelfTestStep == 7 && tick > 290) {
            saveSmokeScreenshot(mc, "controls");
            uiSelfTestStep = 8;
            mc.displayGuiScreen(null);
            seedUltminePreviewForScreenshot(mc);
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] opened world preview");
        } else if (uiSelfTestStep == 8 && tick > 320) {
            saveSmokeScreenshot(mc, "ultmine_preview_world");
            UltmineClientState112.clearPreview();
            UltmineClientState112.setHeld(false);
            uiSelfTestStep = 9;
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] PASS");
            if (Boolean.getBoolean("murilloskills.clientUiSelfTestExit112")) {
                mc.shutdown();
            }
        }
    }

    private static void saveSmokeScreenshot(Minecraft mc, String label) {
        try {
            ITextComponent result = ScreenShotHelper.saveScreenshot(mc.mcDataDir,
                    "murilloskills-112-" + label + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            LOG.info("[MurilloSkills][1.12.2][ClientUiSelfTest] SCREENSHOT {} {}",
                    label, result == null ? "" : result.getUnformattedText());
        } catch (Throwable error) {
            LOG.warn("[MurilloSkills][1.12.2][ClientUiSelfTest] screenshot {} failed: {}",
                    label, error.toString());
        }
    }

    private static void seedUltminePreviewForScreenshot(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        List<BlockPos> preview = new ArrayList<BlockPos>();
        BlockPos base = mc.player.getPosition().add(2, 0, 2);
        preview.add(base);
        preview.add(base.east());
        preview.add(base.south());
        preview.add(base.east().south());
        UltmineClientState112.setHeld(true);
        UltmineClientState112.setPreview(preview);
    }
}
