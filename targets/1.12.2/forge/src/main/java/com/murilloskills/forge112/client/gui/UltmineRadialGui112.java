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

public final class UltmineRadialGui112 extends GuiScreen {
    private static final UltmineShape112[] SHAPE_ORDER = new UltmineShape112[] {
            UltmineShape112.S_3x3,
            UltmineShape112.R_2x1,
            UltmineShape112.LEGACY,
            UltmineShape112.LINE,
            UltmineShape112.STAIRS,
            UltmineShape112.SQUARE_20x20_D1
    };
    private static final int[] SHAPE_COLORS = new int[] {
            0xFF5599FF,
            0xFF44DDDD,
            0xFFFF9944,
            0xFF55DD66,
            0xFFDDAA33,
            0xFFBB66FF
    };
    private int hoveredIndex = -1;
    private int selectedIndex = 0;
    private boolean selectionChanged;

    @Override
    public void initGui() {
        buttonList.clear();
        ClientUltmineConfig.load();
        UltmineShape112 selected = ClientUltmineConfig.getSelectedShape();
        for (int i = 0; i < SHAPE_ORDER.length; i++) {
            if (SHAPE_ORDER[i] == selected) {
                selectedIndex = i;
                break;
            }
        }
        selectionChanged = false;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1) {
            mc.displayGuiScreen(null);
            return;
        }
        if (mouseButton == 0) {
            if (hoveredIndex >= 0) {
                selectShape(hoveredIndex, false);
            } else {
                mc.displayGuiScreen(null);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_SPACE) {
            selectShape(hoveredIndex >= 0 ? hoveredIndex : selectedIndex, false);
            return;
        }
        if (keyCode == Keyboard.KEY_A || keyCode == Keyboard.KEY_LEFT) {
            cycleVariant(-1);
            return;
        }
        if (keyCode == Keyboard.KEY_D || keyCode == Keyboard.KEY_RIGHT) {
            cycleVariant(1);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0) {
            cycleVariant(delta > 0 ? 1 : -1);
        }
    }

    public void releaseAndClose() {
        int index = hoveredIndex >= 0 ? hoveredIndex : selectedIndex;
        if (index >= 0) {
            selectShape(index, false);
        } else if (selectionChanged && selectedIndex >= 0) {
            selectShape(selectedIndex, false);
        }
        mc.displayGuiScreen(null);
    }

    private void selectShape(int index, boolean close) {
        if (index >= 0 && index < SHAPE_ORDER.length) {
            selectionChanged = selectionChanged || selectedIndex != index;
            selectedIndex = index;
            ClientUltmineConfig.setSelectedShape(SHAPE_ORDER[index]);
            ClientUltmineConfig.save();
            syncSelection(SHAPE_ORDER[index]);
            if (close) {
                mc.displayGuiScreen(null);
            }
        }
    }

    private void cycleVariant(int direction) {
        int index = hoveredIndex >= 0 ? hoveredIndex : selectedIndex;
        if (index < 0 || index >= SHAPE_ORDER.length) {
            return;
        }
        UltmineShape112 shape = SHAPE_ORDER[index];
        int count = variantCount(shape);
        if (count <= 1) {
            return;
        }
        int current = ClientUltmineConfig.getVariant(shape);
        int next = (current + direction + count) % count;
        ClientUltmineConfig.setVariant(shape, next);
        ClientUltmineConfig.save();
        syncSelection(shape);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiScreen.drawRect(0, 0, width, height, 0xC8000000);
        int cx = width / 2;
        int cy = height / 2;
        hoveredIndex = hoveredIndex(mouseX, mouseY, cx, cy);
        if (hoveredIndex >= 0 && hoveredIndex != selectedIndex) {
            selectShape(hoveredIndex, false);
        }
        float slice = (float) (Math.PI * 2.0D / SHAPE_ORDER.length);
        float startBase = (float) (-Math.PI / 2.0D);
        for (int i = 0; i < SHAPE_ORDER.length; i++) {
            float start = startBase + i * slice + 0.045F;
            float end = startBase + (i + 1) * slice - 0.045F;
            boolean active = i == hoveredIndex || i == selectedIndex;
            int color = active ? withAlpha(SHAPE_COLORS[i], i == selectedIndex ? 0x92 : 0x74) : 0x48181828;
            drawSector(cx, cy, 54.0F, active ? 146.0F : 138.0F, start, end, color);
            drawSector(cx, cy, 52.0F, 54.0F, start, end, Palette.SECTION_BORDER);
            drawSector(cx, cy, active ? 146.0F : 138.0F, active ? 149.0F : 140.0F, start, end,
                    active ? SHAPE_COLORS[i] : Palette.SECTION_BORDER);
        }

        for (int i = 0; i < SHAPE_ORDER.length; i++) {
            float angle = startBase + i * slice + slice / 2.0F;
            int lx = cx + Math.round((float) Math.cos(angle) * 102.0F);
            int ly = cy + Math.round((float) Math.sin(angle) * 102.0F);
            boolean active = i == hoveredIndex || i == selectedIndex;
            String label = shapeLabel(SHAPE_ORDER[i]);
            drawCenteredString(fontRenderer, label, lx, ly - 5, active ? Palette.TEXT_GOLD : Palette.TEXT_LIGHT);
            String variant = variantLabel(SHAPE_ORDER[i]);
            if (variantCount(SHAPE_ORDER[i]) > 1) {
                drawCenteredString(fontRenderer, variant, lx, ly + 7, active ? Palette.TEXT_LIGHT : Palette.TEXT_MUTED);
            }
        }

        GuiScreen.drawRect(cx - 54, cy - 34, cx + 54, cy + 34, Palette.PANEL_BG);
        drawPanelBorder(cx - 54, cy - 34, 108, 68, Palette.ACCENT_GOLD);
        drawCenteredString(fontRenderer, "Ultmine Shape", cx, cy - 22, Palette.TEXT_GOLD);
        UltmineShape112 activeShape = SHAPE_ORDER[hoveredIndex >= 0 ? hoveredIndex : selectedIndex];
        drawCenteredString(fontRenderer, shapeLabel(activeShape), cx, cy - 4, Palette.TEXT_LIGHT);
        drawCenteredString(fontRenderer, "Release key: select", cx, cy + 13, Palette.TEXT_MUTED);
    }

    private int hoveredIndex(int mouseX, int mouseY, int cx, int cy) {
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 50.0D || distance > 154.0D) {
            return -1;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0D;
        while (angle < 0.0D) {
            angle += Math.PI * 2.0D;
        }
        while (angle >= Math.PI * 2.0D) {
            angle -= Math.PI * 2.0D;
        }
        int index = (int) Math.floor(angle / (Math.PI * 2.0D / SHAPE_ORDER.length));
        return clamp(index, 0, SHAPE_ORDER.length - 1);
    }

    private void drawSector(int cx, int cy, float inner, float outer, float start, float end, int color) {
        float a = ((color >> 24) & 255) / 255.0F;
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        int steps = 18;
        for (int i = 0; i <= steps; i++) {
            float t = start + (end - start) * i / (float) steps;
            float cos = (float) Math.cos(t);
            float sin = (float) Math.sin(t);
            buffer.pos(cx + cos * outer, cy + sin * outer, 0.0D).color(r, g, b, a).endVertex();
            buffer.pos(cx + cos * inner, cy + sin * inner, 0.0D).color(r, g, b, a).endVertex();
        }
        tessellator.draw();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private String shapeLabel(UltmineShape112 shape) {
        switch (shape) {
            case S_3x3: return "3x3";
            case R_2x1: return "2x1";
            case LEGACY: return "Classic";
            case LINE: return "Line";
            case STAIRS: return "Stairs";
            case SQUARE_20x20_D1: return "20x20";
            default: return shape.name();
        }
    }

    private int variantCount(UltmineShape112 shape) {
        if (shape == UltmineShape112.STAIRS || shape == UltmineShape112.R_2x1 || shape == UltmineShape112.LEGACY) return 2;
        if (shape == UltmineShape112.SQUARE_20x20_D1) return 3;
        return 1;
    }

    private String variantLabel(UltmineShape112 shape) {
        int variant = ClientUltmineConfig.getVariant(shape);
        if (shape == UltmineShape112.STAIRS) {
            return variant == 1 ? "Down" : "Up";
        }
        if (shape == UltmineShape112.SQUARE_20x20_D1) {
            return variant == 1 ? "Vertical NS" : variant == 2 ? "Vertical EW" : "Horizontal";
        }
        if (shape == UltmineShape112.R_2x1) {
            return variant == 1 ? "Tall" : "Wide";
        }
        if (shape == UltmineShape112.LEGACY) {
            return variant == 1 ? "Ores" : "Same Block";
        }
        return "";
    }

    private void syncSelection(UltmineShape112 shape) {
        ModNetwork112.sendUltmineSelection(shape, ClientUltmineConfig.getDepth(shape),
                ClientUltmineConfig.getLength(shape), ClientUltmineConfig.getVariant(shape),
                ClientUltmineConfig.getLegacyMaxBlocks());
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
