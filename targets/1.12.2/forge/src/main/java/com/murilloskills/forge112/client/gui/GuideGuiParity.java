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

public final class GuideGuiParity extends GuiScreen {
    private static final int BACK = 9500;
    private static final int TAB_BASE = 9510;
    private static final int HEADER_HEIGHT = 48;
    private static final int SECTION_PADDING = 16;
    private static final int SKILL_CARD_GAP = 4;
    private static final int SKILL_CARD_HEIGHT = 24;
    private final GuiScreen parent;
    private int currentTab;
    private int scroll;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private int textMaxWidth;
    private int lastMouseX;
    private int lastMouseY;
    private SkillType selectedGuideSkill = SkillType.MINER;

    public GuideGuiParity(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int margin = 15;
        contentX = margin;
        contentY = HEADER_HEIGHT + 8;
        contentWidth = width - margin * 2;
        contentHeight = height - contentY - 35;
        textMaxWidth = contentWidth - 40;

        int tabW = 75;
        int tabH = 22;
        int tabY = HEADER_HEIGHT - 26;
        int totalTabW = tabW * 4 + 9;
        int tabStartX = (width - totalTabW) / 2;
        String[] tabs = new String[] { "Status", "Synergy", "Prestige", "Perks" };
        for (int i = 0; i < tabs.length; i++) {
            GuiButton tab = new GuiButton(TAB_BASE + i, tabStartX + i * (tabW + 3), tabY, tabW, tabH, tabs[i]);
            tab.enabled = currentTab != i;
            buttonList.add(tab);
        }
        buttonList.add(new GuiButton(BACK, 10, height - 28, 80, 20, "Back"));
        clampScroll();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BACK) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id >= TAB_BASE && button.id < TAB_BASE + 4) {
            currentTab = button.id - TAB_BASE;
            scroll = 0;
            initGui();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && currentTab == 3) {
            SkillType clicked = guideSelectorHit(mouseX, mouseY);
            if (clicked != null) {
                selectedGuideSkill = clicked;
                scroll = 0;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0 && inside(lastMouseX, lastMouseY, contentX, contentY, contentWidth, contentHeight)) {
            scroll = clamp(scroll + (delta > 0 ? -24 : 24), 0, Math.max(0, contentHeightForTab() - contentHeight));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        renderGradientBackground();
        renderVignette();
        renderHeader();
        renderContentPanel();

        enableScissor(contentX + 2, contentY + 2, contentWidth - 4, contentHeight - 4);
        if (currentTab == 0) {
            renderStatusTab();
        } else if (currentTab == 1) {
            renderSynergiesTab();
        } else if (currentTab == 2) {
            renderPrestigeTab();
        } else {
            renderPerksTab();
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        renderScrollbar();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderGradientBackground() {
        for (int y = 0; y < height; y++) {
            float ratio = (float) y / Math.max(1, height);
            int r = (int) (8 + ratio * 4);
            int g = (int) (8 + ratio * 4);
            int b = (int) (16 + ratio * 8);
            drawRect(0, y, width, y + 1, Palette.BG_OVERLAY | (r << 16) | (g << 8) | b);
        }
    }

    private void renderVignette() {
        int size = Math.min(width, height) / 3;
        for (int i = 0; i < 6; i++) {
            int alpha = (int) (0x12 * (1 - i / 6.0F));
            if (alpha <= 0) {
                continue;
            }
            int color = alpha << 24;
            int offset = size * i / 6;
            drawRect(0, 0, size - offset, size - offset, color);
            drawRect(width - size + offset, 0, width, size - offset, color);
            drawRect(0, height - size + offset, size - offset, height, color);
            drawRect(width - size + offset, height - size + offset, width, height, color);
        }
    }

    private void renderHeader() {
        for (int y = 0; y < HEADER_HEIGHT; y++) {
            float ratio = (float) y / HEADER_HEIGHT;
            int alpha = (int) (0xF0 * (1 - ratio * 0.3F));
            drawRect(0, y, width, y + 1, (alpha << 24) | Palette.HEADER_GRADIENT_BASE);
        }
        drawRect(0, HEADER_HEIGHT - 2, width, HEADER_HEIGHT - 1, Palette.HEADER_ACCENT_LINE);
        int accentW = width / 2;
        int accentX = (width - accentW) / 2;
        drawRect(accentX, HEADER_HEIGHT - 1, accentX + accentW, HEADER_HEIGHT, Palette.ACCENT_GOLD);
        drawCenteredString(fontRenderer, "MurilloSkills Guide", width / 2, 8, Palette.TEXT_GOLD);
    }

    private void renderContentPanel() {
        drawRect(contentX - 1, contentY - 1, contentX + contentWidth + 1, contentY + contentHeight + 1, Palette.PANEL_SHADOW);
        drawRect(contentX, contentY, contentX + contentWidth, contentY + contentHeight, Palette.PANEL_BG);
        drawRect(contentX + 1, contentY + 1, contentX + contentWidth - 1, contentY + 2, Palette.PANEL_HIGHLIGHT);
        drawPanelBorder(contentX, contentY, contentWidth, contentHeight, Palette.SECTION_BORDER);
        renderCornerAccents(contentX, contentY, contentWidth, contentHeight, 6, Palette.ACCENT_GOLD);
    }

    private void renderStatusTab() {
        int x = contentX + SECTION_PADDING;
        int y = contentY + SECTION_PADDING - scroll;
        y = sectionTitle(x, y, "Status");
        y += 10;
        y = subsection(x, y, "Selected Skills");
        EntityPlayer player = mc.player;
        PlayerSkillDataCore data = player == null ? new PlayerSkillDataCore() : data(player);
        if (data.getSelectedSkills().isEmpty()) {
            y = wrapped(x + 12, y, textMaxWidth, "No skills selected yet.", Palette.TEXT_MUTED) + 10;
        } else {
            for (SkillType skill : data.getSelectedSkills()) {
                SkillStatsCore stats = data.getSkill(skill);
                renderSkillMiniCard(x + 8, y, skill, stats.getLevel(), stats.getPrestige(), data.isParagonSkill(skill));
                y += 46;
            }
        }
        y += 12;
        y = subsection(x, y, "Active Synergies");
        boolean any = false;
        for (String synergy : UiData.allSynergies()) {
            if (isSynergyActive(data, synergy)) {
                renderActiveSynergyBadge(x + 8, y, synergy);
                y += 22;
                any = true;
            }
        }
        if (!any) {
            wrapped(x + 12, y, textMaxWidth, "No active synergy.", Palette.TEXT_MUTED);
        }
    }

    private void renderSynergiesTab() {
        int x = contentX + SECTION_PADDING;
        int y = contentY + SECTION_PADDING - scroll;
        y = sectionTitle(x, y, "Synergies");
        y += 10;
        renderInfoBox(x, y, contentWidth - SECTION_PADDING * 2 - 16, 24, Palette.ACCENT_BLUE);
        drawString(fontRenderer, "Choose matching skills to unlock the paired bonus.", x + 8, y + 7, Palette.TEXT_LIGHT);
        y += 36;
        EntityPlayer player = mc.player;
        PlayerSkillDataCore data = player == null ? new PlayerSkillDataCore() : data(player);
        for (String synergy : UiData.allSynergies()) {
            renderSynergyCard(x, y, synergy, isSynergyActive(data, synergy));
            y += 78;
        }
    }

    private void renderPrestigeTab() {
        int x = contentX + SECTION_PADDING;
        int y = contentY + SECTION_PADDING - scroll;
        y = sectionTitle(x, y, "Prestige");
        y += 10;
        int boxW = contentWidth - SECTION_PADDING * 2 - 16;
        renderInfoBox(x, y, boxW, 58, Palette.TEXT_PURPLE);
        int ty = y + 6;
        ty = wrapped(x + 8, ty, boxW - 16, "Prestige resets a level 100 Paragon skill and improves scaling.", Palette.TEXT_LIGHT);
        ty = wrapped(x + 8, ty, boxW - 16, "Each prestige increases XP, passive strength and cooldown recovery.", Palette.TEXT_LIGHT);
        wrapped(x + 8, ty, boxW - 16, "Keep one Master Paragon and unlock subclass paragons separately.", Palette.TEXT_LIGHT);
        y += 72;
        y = subsection(x, y, "Bonus Table");
        int col1 = x + 10;
        int col2 = x + 80;
        int col3 = x + 170;
        drawString(fontRenderer, "Level", col1, y, Palette.TEXT_AQUA);
        drawString(fontRenderer, "XP Bonus", col2, y, Palette.TEXT_AQUA);
        drawString(fontRenderer, "Passive", col3, y, Palette.TEXT_AQUA);
        y += 14;
        renderDivider(x + 5, y, contentWidth - SECTION_PADDING * 2 - 20);
        y += 8;
        int[] levels = new int[] { 1, 2, 3, 5, CONFIG.getMaxPrestigeLevel() };
        for (int level : levels) {
            drawString(fontRenderer, "P" + level, col1, y, level == CONFIG.getMaxPrestigeLevel() ? Palette.TEXT_GOLD : Palette.TEXT_LIGHT);
            drawString(fontRenderer, "+" + (int) (level * CONFIG.getPrestigeXpBonus() * 100.0F) + "%", col2, y, Palette.TEXT_GREEN);
            drawString(fontRenderer, "+" + (int) (level * CONFIG.getPrestigePassiveBonus() * 100.0F) + "%", col3, y, Palette.TEXT_AQUA);
            y += 14;
        }
        y += 12;
        y = subsection(x, y, "Requirements");
        y = wrapped(x + 8, y, textMaxWidth, "> Level 100", Palette.TEXT_MUTED);
        y = wrapped(x + 8, y, textMaxWidth, "> Paragon skill", Palette.TEXT_MUTED);
        wrapped(x + 8, y, textMaxWidth, "> Prestige below " + CONFIG.getMaxPrestigeLevel(), Palette.TEXT_MUTED);
    }

    private void renderPerksTab() {
        int x = contentX + SECTION_PADDING;
        int y = contentY + SECTION_PADDING - scroll;
        y = sectionTitle(x, y, "Perks");
        y += 10;
        y = renderGuideSkillSelector(x, y) + 12;
        if (selectedGuideSkill != null) {
            renderGuideSkillSection(x, y, selectedGuideSkill);
        }
    }

    private int renderGuideSkillSelector(int x, int y) {
        SkillType[] skills = SkillType.values();
        int totalW = contentWidth - SECTION_PADDING * 2 - 16;
        int cardW = Math.max(34, (totalW - SKILL_CARD_GAP * (skills.length - 1)) / skills.length);
        for (int i = 0; i < skills.length; i++) {
            SkillType skill = skills[i];
            int cardX = x + i * (cardW + SKILL_CARD_GAP);
            boolean selected = skill == selectedGuideSkill;
            boolean hovered = !selected && inside(lastMouseX, lastMouseY, cardX, y, cardW, SKILL_CARD_HEIGHT);
            int color = UiData.skillColor(skill);
            drawRect(cardX, y, cardX + cardW, y + SKILL_CARD_HEIGHT,
                    selected ? Palette.SECTION_BG_ACTIVE : hovered ? Palette.SECTION_BG : Palette.CARD_BG_SUBTLE);
            drawPanelBorder(cardX, y, cardW, SKILL_CARD_HEIGHT,
                    selected ? color : hovered ? withAlpha(color, 0x80) : Palette.SECTION_BORDER);
            if (selected) {
                drawRect(cardX + 1, y + 1, cardX + cardW - 1, y + 2, color);
                drawRect(cardX + 1, y + SKILL_CARD_HEIGHT - 2, cardX + cardW - 1, y + SKILL_CARD_HEIGHT - 1,
                        withAlpha(color, 0x60));
            }
            drawItem(UiData.itemForSkill(skill), cardX + 2, y + 4, 0.78F);
            String name = UiData.title(skill);
            int maxW = cardW - 24;
            while (fontRenderer.getStringWidth(name) > maxW && name.length() > 2) {
                name = name.substring(0, name.length() - 1);
            }
            if (!name.equals(UiData.title(skill))) {
                name = name + ".";
            }
            drawString(fontRenderer, name, cardX + 20, y + 8, selected ? color : hovered ? Palette.TEXT_LIGHT : Palette.TEXT_GRAY);
        }
        return y + SKILL_CARD_HEIGHT;
    }

    private SkillType guideSelectorHit(int mouseX, int mouseY) {
        int x = contentX + SECTION_PADDING;
        int y = contentY + SECTION_PADDING - scroll + 38;
        SkillType[] skills = SkillType.values();
        int totalW = contentWidth - SECTION_PADDING * 2 - 16;
        int cardW = Math.max(34, (totalW - SKILL_CARD_GAP * (skills.length - 1)) / skills.length);
        for (int i = 0; i < skills.length; i++) {
            int cardX = x + i * (cardW + SKILL_CARD_GAP);
            if (inside(mouseX, mouseY, cardX, y, cardW, SKILL_CARD_HEIGHT)) {
                return skills[i];
            }
        }
        return null;
    }

    private int renderGuideSkillSection(int x, int y, SkillType skill) {
        int boxW = contentWidth - SECTION_PADDING * 2 - 16;
        int color = UiData.skillColor(skill);
        SkillStatsCore stats = mc.player == null ? new SkillStatsCore(0, 0.0D, -1L, 0) : data(mc.player).getSkill(skill);
        drawRect(x, y, x + boxW, y + 30, Palette.PANEL_BG_HEADER);
        drawPanelBorder(x, y, boxW, 30, color);
        drawRect(x, y, x + 3, y + 30, color);
        drawItem(UiData.itemForSkill(skill), x + 8, y + 7, 1.0F);
        drawString(fontRenderer, UiData.title(skill), x + 28, y + 6, color);
        drawString(fontRenderer, "Lv " + stats.getLevel(), x + 28, y + 18,
                stats.getLevel() >= CONFIG.getMaxLevel() ? Palette.TEXT_GOLD : Palette.TEXT_LIGHT);
        y += 38;
        y = guideCard(x, y, boxW, "Overview", UiData.description(skill), Palette.TEXT_LIGHT, Palette.ACCENT_BLUE);
        y = guideCard(x, y, boxW, "Why Choose", whyChoose(skill), Palette.TEXT_AQUA, Palette.TEXT_AQUA);
        y = xpSourcesCard(x, y, boxW, skill);
        y = guideCard(x, y, boxW, "Master Ability", UiData.ability(skill), Palette.TEXT_GOLD, Palette.ACCENT_GOLD);

        int passiveH = 22 + UiData.passives(skill).length * 12;
        drawRect(x, y, x + boxW, y + passiveH, Palette.CARD_BG_SUBTLE);
        drawPanelBorder(x, y, boxW, passiveH, Palette.SECTION_BORDER);
        drawRect(x, y, x + 3, y + passiveH, Palette.TEXT_PURPLE);
        drawString(fontRenderer, "Level 100 Passives", x + 8, y + 5, Palette.TEXT_PURPLE);
        int py = y + 20;
        for (String passive : UiData.passives(skill)) {
            py = wrapped(x + 12, py, boxW - 24, passive, Palette.TEXT_LIGHT);
        }
        y += passiveH + 6;
        drawRect(x, y, x + boxW, y + 18, Palette.CARD_BG_SUBTLE);
        drawPanelBorder(x, y, boxW, 18, Palette.SECTION_BORDER);
        drawRect(x, y, x + 3, y + 18, Palette.TEXT_YELLOW);
        drawString(fontRenderer, "Timeline", x + 8, y + 5, Palette.TEXT_YELLOW);
        y += 22;
        int lineX = x + 6;
        for (UiData.Perk perk : UiData.perks(skill)) {
            boolean reached = perk.level <= stats.getLevel();
            int textColor = reached ? Palette.TEXT_LIGHT : Palette.TEXT_MUTED;
            drawRect(lineX, y, lineX + 2, y + 20, reached ? withAlpha(color, 0x60) : Palette.HEADER_ACCENT_LINE);
            drawRect(lineX - 2, y + 2, lineX + 4, y + 8, reached ? color : Palette.TEXT_MUTED);
            y = wrapped(lineX + 10, y, boxW - 24, "Lv" + perk.level + " - " + perk.name + ": " + perk.detail, textColor);
        }
        return y;
    }

    private int sectionTitle(int x, int y, String title) {
        renderDivider(x, y + 5, 20);
        drawString(fontRenderer, title, x + 25, y, Palette.TEXT_GOLD);
        int titleW = fontRenderer.getStringWidth(title);
        renderDivider(x + 30 + titleW, y + 5, contentWidth - SECTION_PADDING * 2 - titleW - 50);
        return y + 18;
    }

    private int subsection(int x, int y, String title) {
        drawString(fontRenderer, "> " + title, x, y, Palette.TEXT_YELLOW);
        return y + 18;
    }

    private void renderInfoBox(int x, int y, int w, int h, int accent) {
        drawRect(x, y, x + w, y + h, Palette.HUD_INDICATOR_BG);
        drawPanelBorder(x, y, w, h, Palette.INFO_BOX_BORDER);
        drawRect(x, y + 2, x + 3, y + h - 2, accent);
    }

    private void renderSkillMiniCard(int x, int y, SkillType skill, int level, int prestige, boolean paragon) {
        int w = contentWidth - SECTION_PADDING * 2 - 24;
        int h = 50;
        if (paragon) {
            drawRect(x - 1, y - 1, x + w + 1, y + h + 1, Palette.CARD_GLOW_PARAGON);
        }
        drawRect(x, y, x + w, y + h, paragon ? Palette.CARD_BG_PARAGON : Palette.SECTION_BG);
        drawPanelBorder(x, y, w, h, paragon ? Palette.ACCENT_GOLD : Palette.SECTION_BORDER);
        if (paragon) {
            renderCornerAccents(x, y, w, h, 4, Palette.ACCENT_GOLD);
        }
        drawItem(UiData.itemForSkill(skill), x + 4, y + 8, 1.0F);
        drawString(fontRenderer, UiData.title(skill), x + 26, y + 5, paragon ? Palette.TEXT_GOLD : UiData.skillColor(skill));
        drawString(fontRenderer, "Lv " + level + (prestige > 0 ? " P" + prestige : ""), x + 26, y + 17,
                level >= CONFIG.getMaxLevel() ? Palette.TEXT_GOLD : Palette.TEXT_LIGHT);
        float progress = level >= CONFIG.getMaxLevel() ? 1.0F : 0.0F;
        renderProgressBar(x + 26, y + 30, w - 75, 6, progress, Palette.PROGRESS_BAR_EMPTY,
                level >= CONFIG.getMaxLevel() ? Palette.ACCENT_GOLD : Palette.PROGRESS_BAR_FILL, Palette.PROGRESS_BAR_SHINE);
        drawString(fontRenderer, level >= CONFIG.getMaxLevel() ? "MAX" : "0%", x + w - 34, y + 30,
                level >= CONFIG.getMaxLevel() ? Palette.TEXT_GOLD : Palette.TEXT_MUTED);
        if (paragon) {
            drawString(fontRenderer, "P", x + w - 18, y + 5, Palette.ACCENT_GOLD);
        }
    }

    private void renderActiveSynergyBadge(int x, int y, String synergy) {
        String name = synergyName(synergy);
        String bonus = synergyBonus(synergy);
        int w = Math.max(fontRenderer.getStringWidth(name), fontRenderer.getStringWidth(bonus)) + 20;
        drawRect(x, y, x + w, y + 18, Palette.SYNERGY_BADGE_BG);
        drawPanelBorder(x, y, w, 18, Palette.ACCENT_GREEN);
        drawString(fontRenderer, "[+] " + name, x + 4, y + 2, Palette.TEXT_GREEN);
        drawString(fontRenderer, "    " + bonus, x + 4, y + 10, Palette.TEXT_AQUA);
    }

    private void renderSynergyCard(int x, int y, String synergy, boolean active) {
        int w = contentWidth - SECTION_PADDING * 2 - 16;
        int h = 68;
        drawRect(x, y, x + w, y + h, active ? Palette.SYNERGY_ACTIVE_BG : Palette.SECTION_BG);
        if (active) {
            drawRect(x + 1, y + 1, x + w - 1, y + 3, Palette.SYNERGY_ACTIVE_GLOW);
        }
        drawPanelBorder(x, y, w, h, active ? Palette.ACCENT_GREEN : Palette.SECTION_BORDER);
        drawRect(x, y + 2, x + (active ? 4 : 3), y + h - 2, active ? Palette.ACCENT_GREEN : Palette.TEXT_MUTED);
        SkillType[] pair = synergyPair(synergy);
        if (pair[0] != null) {
            drawItem(UiData.itemForSkill(pair[0]), x + 10, y + 8, 1.0F);
        }
        if (pair[1] != null) {
            drawItem(UiData.itemForSkill(pair[1]), x + 38, y + 8, 1.0F);
        }
        drawString(fontRenderer, "+", x + 29, y + 13, Palette.TEXT_WHITE);
        drawString(fontRenderer, (active ? "[+] " : "[ ] ") + synergyName(synergy), x + 58, y + 8,
                active ? Palette.TEXT_GREEN : Palette.TEXT_GRAY);
        drawString(fontRenderer, pairName(pair[0]) + " + " + pairName(pair[1]), x + 58, y + 22, Palette.TEXT_MUTED);
        drawRect(x + 12, y + 40, x + w - 12, y + 58, Palette.BONUS_BAR_BG);
        drawPanelBorder(x + 12, y + 40, w - 24, 18, active ? Palette.BONUS_BAR_BORDER_ACTIVE : Palette.BONUS_BAR_BORDER_INACTIVE);
        String bonus = synergyBonus(synergy);
        drawString(fontRenderer, bonus, x + 12 + (w - 24 - fontRenderer.getStringWidth(bonus)) / 2, y + 45,
                active ? Palette.TEXT_AQUA : Palette.TEXT_MUTED);
        if (active) {
            String activeText = "ACTIVE";
            drawString(fontRenderer, activeText, x + w - fontRenderer.getStringWidth(activeText) - 12, y + 8,
                    Palette.TEXT_GREEN);
        }
    }

    private int guideCard(int x, int y, int w, String label, String value, int textColor, int accent) {
        int textH = Math.max(1, fontRenderer.listFormattedStringToWidth(value, w - 32).size()) * 12;
        int h = 20 + textH + 6;
        drawRect(x, y, x + w, y + h, Palette.CARD_BG_SUBTLE);
        drawPanelBorder(x, y, w, h, Palette.SECTION_BORDER);
        drawRect(x, y, x + 3, y + h, accent);
        drawString(fontRenderer, label, x + 8, y + 5, accent);
        wrapped(x + 12, y + 18, w - 32, value, textColor);
        return y + h + 6;
    }

    private int xpSourcesCard(int x, int y, int w, SkillType skill) {
        String[] sources = xpSources(skill);
        int rowH = 12;
        int h = 20 + sources.length * rowH + 8;
        drawRect(x, y, x + w, y + h, Palette.CARD_BG_SUBTLE);
        drawPanelBorder(x, y, w, h, Palette.SECTION_BORDER);
        drawRect(x, y, x + 3, y + h, Palette.ACCENT_GREEN);
        drawString(fontRenderer, "XP Sources", x + 8, y + 5, Palette.ACCENT_GREEN);
        int rowY = y + 20;
        for (int i = 0; i < sources.length; i++) {
            if (i % 2 == 0) {
                drawRect(x + 4, rowY, x + w - 4, rowY + rowH, Palette.ALTERNATING_ROW_BG);
            }
            drawString(fontRenderer, sources[i], x + 8, rowY + 2, Palette.TEXT_LIGHT);
            rowY += rowH;
        }
        return y + h + 6;
    }

    private int wrapped(int x, int y, int w, String text, int color) {
        for (String line : fontRenderer.listFormattedStringToWidth(text, Math.max(20, w))) {
            drawString(fontRenderer, line, x, y, color);
            y += 12;
        }
        return y;
    }

    private void renderDivider(int x, int y, int w) {
        if (w > 0) {
            drawRect(x, y, x + w, y + 1, Palette.DIVIDER_COLOR);
        }
    }

    private void renderScrollbar() {
        int max = Math.max(0, contentHeightForTab() - contentHeight);
        if (max <= 0) {
            return;
        }
        int top = contentY + 4;
        int bottom = contentY + contentHeight - 4;
        int trackH = bottom - top;
        int knobH = Math.max(18, trackH * trackH / Math.max(trackH, contentHeightForTab()));
        int knobY = top + (trackH - knobH) * scroll / Math.max(1, max);
        drawRect(contentX + contentWidth - 6, top, contentX + contentWidth - 4, bottom, 0x6636384A);
        drawRect(contentX + contentWidth - 7, knobY, contentX + contentWidth - 3, knobY + knobH, Palette.ACCENT_GOLD);
    }

    private int contentHeightForTab() {
        if (currentTab == 0) {
            EntityPlayer player = mc.player;
            PlayerSkillDataCore data = player == null ? new PlayerSkillDataCore() : data(player);
            return 120 + Math.max(1, data.getSelectedSkills().size()) * 50 + UiData.allSynergies().length * 4;
        }
        if (currentTab == 1) {
            return 80 + UiData.allSynergies().length * 78;
        }
        if (currentTab == 2) {
            return 350;
        }
        return 560;
    }

    private void clampScroll() {
        scroll = clamp(scroll, 0, Math.max(0, contentHeightForTab() - contentHeight));
    }

    private void drawItem(ItemStack stack, int x, int y, float scale) {
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

    private void enableScissor(int x, int y, int w, int h) {
        ScaledResolution scaled = new ScaledResolution(mc);
        int factor = scaled.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * factor, mc.displayHeight - (y + h) * factor, w * factor, h * factor);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
