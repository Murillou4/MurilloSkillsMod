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

public final class GuideGui extends GuiScreen {
    private static final int BACK = 9100;
    private final GuiScreen parent;
    private int tab;
    private int scroll;

    public GuideGui(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(flatButton(BACK, width - 86, 12, 74, 20, "Back"));
        String[] tabs = new String[] { "Status", "Synergy", "Prestige", "Perks" };
        int tabW = Math.max(64, Math.min(92, (width - 38) / tabs.length));
        for (int i = 0; i < tabs.length; i++) {
            buttonList.add(flatButton(9200 + i, 18 + i * (tabW + 4), 36, tabW, 18, tabs[i]));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BACK) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id >= 9200 && button.id < 9204) {
            tab = button.id - 9200;
            scroll = 0;
            initGui();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0) {
            int content = contentHeight();
            int maxScroll = Math.max(0, content - (height - 72));
            scroll = clamp(scroll + (delta > 0 ? -24 : 24), 0, maxScroll);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGradientRect(0, 0, width, height, 0xF20A0B12, 0xF2101320);
        drawCenteredString(fontRenderer, "Guide", width / 2, 15, 0xFFFFC400);
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawRect(16, 60, width - 16, height - 16, 0xC80E0F17);
        drawRect(16, 60, width - 16, 62, 0xFFE0A817);
        int x = 28;
        int y = 74 - scroll;
        int textW = width - 58;
        if (tab == 0) {
            y = drawStatusTab(x, y, textW);
        } else if (tab == 1) {
            y = drawSynergyTab(x, y, textW);
        } else if (tab == 2) {
            y = drawPrestigeTab(x, y, textW);
        } else {
            y = drawPerksTab(x, y, textW);
        }
        int maxScroll = Math.max(0, contentHeight() - (height - 72));
        if (maxScroll > 0) {
            int top = 68;
            int bottom = height - 22;
            int trackH = bottom - top;
            int knobH = Math.max(18, trackH * trackH / Math.max(trackH, contentHeight()));
            int knobY = top + (trackH - knobH) * scroll / Math.max(1, maxScroll);
            drawRect(width - 10, top, width - 8, bottom, 0x6636384A);
            drawRect(width - 11, knobY, width - 7, knobY + knobH, 0xFFE0A817);
        }
    }

    private int drawStatusTab(int x, int y, int w) {
        EntityPlayer player = mc.player;
        PlayerSkillDataCore data = player == null ? new PlayerSkillDataCore() : data(player);
        for (SkillType skill : SkillType.values()) {
            SkillStatsCore stats = data.getSkill(skill);
            y = drawSectionTitle(x, y, UiData.title(skill) + " - Lv" + stats.getLevel() + " P" + stats.getPrestige(), UiData.skillColor(skill));
            y = drawWrapped(x, y, w, UiData.description(skill), 0xFFD9DCF2);
            y = drawWrapped(x, y + 2, w, "Ability: " + UiData.ability(skill), 0xFF7AA7FF);
            for (String passive : UiData.passives(skill)) {
                y = drawWrapped(x + 8, y, w - 8, passive, 0xFFBFC2D8);
            }
            y += 10;
        }
        return y;
    }

    private int drawSynergyTab(int x, int y, int w) {
        for (String synergy : UiData.allSynergies()) {
            y = drawWrapped(x, y, w, synergy, 0xFFD9DCF2) + 6;
        }
        return y;
    }

    private int drawPrestigeTab(int x, int y, int w) {
        y = drawWrapped(x, y, w, "Prestige resets a level 100 skill to level 0 and adds stronger XP, passive and cooldown scaling.", 0xFFD9DCF2);
        y = drawWrapped(x, y + 4, w, "Current scaling: +" + (int) (CONFIG.getPrestigeXpBonus() * 100) + "% XP, +"
                + (int) (CONFIG.getPrestigePassiveBonus() * 100) + "% passive and -"
                + (int) (CONFIG.getPrestigeCooldownReductionPerLevel() * 100) + "% ability cooldown per prestige.", 0xFFBFC2D8);
        y = drawWrapped(x, y + 4, w, "Max prestige: " + CONFIG.getMaxPrestigeLevel() + ". Max cooldown reduction: "
                + (int) (CONFIG.getMaxPrestigeCooldownReduction() * 100) + "%.", 0xFFBFC2D8);
        return y;
    }

    private int drawPerksTab(int x, int y, int w) {
        for (SkillType skill : SkillType.values()) {
            y = drawSectionTitle(x, y, UiData.title(skill), UiData.skillColor(skill));
            for (UiData.Perk perk : UiData.perks(skill)) {
                y = drawWrapped(x + 8, y, w - 8, "L" + perk.level + " - " + perk.name + ": " + perk.detail, 0xFFBFC2D8);
            }
            y += 10;
        }
        return y;
    }

    private int drawSectionTitle(int x, int y, String title, int color) {
        if (y > 60 && y < height - 18) {
            drawString(fontRenderer, title, x, y, color);
        }
        return y + 14;
    }

    private int drawWrapped(int x, int y, int w, String text, int color) {
        List<String> lines = fontRenderer.listFormattedStringToWidth(text, w);
        for (String line : lines) {
            if (y > 60 && y < height - 18) {
                drawString(fontRenderer, line, x, y, color);
            }
            y += 10;
        }
        return y;
    }

    private int contentHeight() {
        int lines = 0;
        if (tab == 0) {
            lines = 8 * 12;
        } else if (tab == 1) {
            lines = UiData.allSynergies().length * 3;
        } else if (tab == 2) {
            lines = 12;
        } else {
            lines = 8 * 9;
        }
        return 24 + lines * 10;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
