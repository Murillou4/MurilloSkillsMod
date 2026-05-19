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

public final class OreFilterGui112 extends GuiScreen {
    private static final int BACK = 27000;
    private static final int ALL = 27001;
    private static final int NONE = 27002;
    private static final int MAX_MINUS = 27003;
    private static final int MAX_PLUS = 27004;
    private static final int OPTION_BASE = 27100;
    private final GuiScreen parent;
    private List<ClientOreFilterConfig.OreOption> options = new ArrayList<ClientOreFilterConfig.OreOption>();
    private int scroll;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int cols;
    private int visibleRows;
    private int gridX;
    private int gridY;
    private int cardW;
    private int cardH;
    private int cardGap;
    private int scrollbarX;
    private int scrollbarY;
    private int scrollbarH;
    private int scrollbarThumbY;
    private int scrollbarThumbH;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    public OreFilterGui112(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        ClientOreFilterConfig.load();
        options = ClientOreFilterConfig.getOptions();
        buttonList.clear();
        panelW = Math.min(620, width - 20);
        panelH = Math.min(360, height - 20);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        cols = panelW >= 520 ? 3 : 2;
        cardH = 30;
        cardGap = 6;
        gridX = panelX + 14;
        gridY = panelY + 88;
        cardW = (panelW - 28 - (cols - 1) * cardGap) / cols;
        visibleRows = Math.max(1, (panelH - 128) / (cardH + cardGap));
        int maxScroll = Math.max(0, (int) Math.ceil(options.size() / (double) cols) - visibleRows);
        scroll = clamp(scroll, 0, maxScroll);
        buttonList.add(flatButton(BACK, panelX + panelW - 82, panelY + panelH - 28, 70, 20, "Back"));
        buttonList.add(flatButton(ALL, panelX + 14, panelY + 48, 64, 18, "All"));
        buttonList.add(flatButton(NONE, panelX + 84, panelY + 48, 64, 18, "None"));
        buttonList.add(flatButton(MAX_MINUS, panelX + panelW - 168, panelY + 48, 24, 18, "-"));
        buttonList.add(flatButton(MAX_PLUS, panelX + panelW - 40, panelY + 48, 24, 18, "+"));
        int first = scroll * cols;
        int last = Math.min(options.size(), first + visibleRows * cols);
        for (int i = first; i < last; i++) {
            int local = i - first;
            int col = local % cols;
            int row = local / cols;
            buttonList.add(invisibleButton(OPTION_BASE + i, gridX + col * (cardW + cardGap),
                    gridY + row * (cardH + cardGap), cardW, cardH));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BACK) {
            ClientOreFilterConfig.save();
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == ALL || button.id == NONE) {
            ClientOreFilterConfig.setAll(button.id == ALL);
            ClientOreFilterConfig.save();
            initGui();
            return;
        }
        if (button.id == MAX_MINUS || button.id == MAX_PLUS) {
            ClientOreFilterConfig.setMaxOres(ClientOreFilterConfig.getMaxOres() + (button.id == MAX_PLUS ? 25 : -25));
            ClientOreFilterConfig.save();
            initGui();
            return;
        }
        int index = button.id - OPTION_BASE;
        if (index >= 0 && index < options.size()) {
            ClientOreFilterConfig.toggle(options.get(index).key);
            ClientOreFilterConfig.save();
            initGui();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0 && Mouse.getEventX() >= 0) {
            setScroll(scroll + (delta > 0 ? -1 : 1));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && maxScroll() > 0 && inside(mouseX, mouseY, scrollbarX - 3, scrollbarY, 11, scrollbarH)) {
            draggingScrollbar = true;
            scrollbarGrabOffset = inside(mouseX, mouseY, scrollbarX - 3, scrollbarThumbY, 11, scrollbarThumbH)
                    ? mouseY - scrollbarThumbY : scrollbarThumbH / 2;
            setScroll(scrollbarScrollFromMouse(mouseY, scrollbarY, scrollbarH, scrollbarThumbH, maxScroll(),
                    scrollbarGrabOffset));
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingScrollbar) {
            setScroll(scrollbarScrollFromMouse(mouseY, scrollbarY, scrollbarH, scrollbarThumbH, maxScroll(),
                    scrollbarGrabOffset));
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingScrollbar = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    private int maxScroll() {
        return Math.max(0, (int) Math.ceil(options.size() / (double) cols) - visibleRows);
    }

    private void setScroll(int value) {
        int next = clamp(value, 0, maxScroll());
        if (next != scroll) {
            scroll = next;
            initGui();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiScreen.drawRect(0, 0, width, height, 0xB8000000);
        GuiScreen.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, Palette.SECTION_BG);
        drawPanelBorder(panelX, panelY, panelW, panelH, Palette.SECTION_BORDER);
        renderCornerAccents(panelX, panelY, panelW, panelH, 9, Palette.ACCENT_GOLD);
        GuiScreen.drawRect(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 34, Palette.PANEL_BG_HEADER);
        GuiScreen.drawRect(panelX + 42, panelY + 33, panelX + panelW - 42, panelY + 34, Palette.ACCENT_GOLD);
        drawCenteredString(fontRenderer, "Ore Filter", width / 2, panelY + 9, Palette.TEXT_GOLD);
        drawCenteredString(fontRenderer, "Choose exactly which ores the miner scan should reveal", width / 2,
                panelY + 22, Palette.TEXT_MUTED);
        drawString(fontRenderer, "Ores", panelX + 14, panelY + 72, Palette.TEXT_GOLD);
        drawString(fontRenderer, enabledCount() + " / " + options.size() + " enabled",
                panelX + 54, panelY + 72, Palette.TEXT_MUTED);
        String maxText = "Max ores: " + ClientOreFilterConfig.getMaxOres();
        drawString(fontRenderer, maxText, panelX + panelW - 138, panelY + 53, Palette.TEXT_LIGHT);
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderOreCards(mouseX, mouseY);
        renderOreScrollbar();
    }

    private void renderOreCards(int mouseX, int mouseY) {
        int first = scroll * cols;
        int last = Math.min(options.size(), first + visibleRows * cols);
        for (int i = first; i < last; i++) {
            ClientOreFilterConfig.OreOption option = options.get(i);
            boolean enabled = ClientOreFilterConfig.isOreEnabled(option.key);
            int local = i - first;
            int col = local % cols;
            int row = local / cols;
            int x = gridX + col * (cardW + cardGap);
            int y = gridY + row * (cardH + cardGap);
            boolean hovered = inside(mouseX, mouseY, x, y, cardW, cardH);
            int oreColor = option.color;
            int bg = enabled ? Palette.SECTION_BG_ACTIVE : Palette.SECTION_BG;
            GuiScreen.drawRect(x + 1, y + 1, x + cardW - 1, y + cardH - 1, bg);
            drawPanelBorder(x, y, cardW, cardH, hovered ? Palette.ACCENT_GOLD : Palette.SECTION_BORDER);
            GuiScreen.drawRect(x + 1, y + 1, x + 4, y + cardH - 1,
                    (enabled ? 0xFF000000 : 0x66000000) | (oreColor & 0x00FFFFFF));
            if (enabled) {
                GuiScreen.drawRect(x + 4, y + 1, x + cardW - 1, y + 2, 0x30FFFFFF);
            }

            renderItemStack(mc, oreIcon(option), x + 8, y + 7, 0.78F);

            String state = enabled ? "ON" : "OFF";
            int stateColor = enabled ? Palette.TEXT_GREEN : Palette.TEXT_MUTED;
            int stateW = fontRenderer.getStringWidth(state);
            int swatchX = x + cardW - stateW - 22;
            int swatchY = y + 11;
            GuiScreen.drawRect(swatchX, swatchY, swatchX + 8, swatchY + 8,
                    (enabled ? 0xFF000000 : 0x66000000) | (oreColor & 0x00FFFFFF));
            drawPanelBorder(swatchX - 1, swatchY - 1, 10, 10, Palette.SECTION_BORDER);
            drawString(fontRenderer, state, x + cardW - stateW - 6, y + 11, stateColor);

            String name = fit(option.name, Math.max(10, swatchX - (x + 30) - 5));
            drawString(fontRenderer, name, x + 30, y + 11, enabled ? Palette.TEXT_LIGHT : Palette.TEXT_MUTED);
        }
    }

    private void renderOreScrollbar() {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }
        scrollbarX = panelX + panelW - 10;
        scrollbarY = gridY;
        scrollbarH = visibleRows * (cardH + cardGap) - cardGap;
        scrollbarThumbH = scrollbarThumbHeight(scrollbarH, visibleRows, visibleRows + maxScroll);
        scrollbarThumbY = scrollbarThumbY(scrollbarY, scrollbarH, scrollbarThumbH, scroll, maxScroll);
        renderScrollbar(scrollbarX, scrollbarY, scrollbarH, scrollbarThumbY, scrollbarThumbH);
    }

    private int enabledCount() {
        int count = 0;
        for (ClientOreFilterConfig.OreOption option : options) {
            if (ClientOreFilterConfig.isOreEnabled(option.key)) {
                count++;
            }
        }
        return count;
    }

    private ItemStack oreIcon(ClientOreFilterConfig.OreOption option) {
        if ("COAL".equals(option.key)) return new ItemStack(Blocks.COAL_ORE);
        if ("COPPER".equals(option.key)) return new ItemStack(Blocks.IRON_ORE);
        if ("IRON".equals(option.key)) return new ItemStack(Blocks.IRON_ORE);
        if ("GOLD".equals(option.key)) return new ItemStack(Blocks.GOLD_ORE);
        if ("LAPIS".equals(option.key)) return new ItemStack(Blocks.LAPIS_ORE);
        if ("REDSTONE".equals(option.key)) return new ItemStack(Blocks.REDSTONE_ORE);
        if ("DIAMOND".equals(option.key)) return new ItemStack(Blocks.DIAMOND_ORE);
        if ("EMERALD".equals(option.key)) return new ItemStack(Blocks.EMERALD_ORE);
        if ("ANCIENT_DEBRIS".equals(option.key)) return new ItemStack(Blocks.OBSIDIAN);
        if ("NETHER_QUARTZ".equals(option.key)) return new ItemStack(Blocks.QUARTZ_ORE);
        if ("NETHER_GOLD".equals(option.key)) return new ItemStack(Blocks.GOLD_ORE);
        String normalized = normalizeId(option.key.toLowerCase(Locale.ROOT));
        Block block = Block.getBlockFromName(normalized);
        if (block != null && block != Blocks.AIR) {
            return new ItemStack(block);
        }
        Item item = Item.getByNameOrId(normalized);
        return item == null ? new ItemStack(Blocks.IRON_ORE) : new ItemStack(item);
    }

    private String fit(String text, int maxWidth) {
        String out = text == null ? "" : text;
        while (out.length() > 3 && fontRenderer.getStringWidth(out) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
