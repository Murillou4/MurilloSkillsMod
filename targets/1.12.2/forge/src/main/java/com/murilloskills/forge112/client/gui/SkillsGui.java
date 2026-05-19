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

public final class SkillsGui extends GuiScreen {
    private static final int GUIDE_BUTTON = 9000;
    private static final int TOAST_BUTTON = 9001;
    private static final int PARAGON_BUTTON = 9002;
    private final Map<Integer, SkillButton> buttonActions = new HashMap<Integer, SkillButton>();
    private int scroll;

    @Override
    public void initGui() {
        buttonList.clear();
        buttonActions.clear();
        Layout layout = layout();
        buttonList.add(new GuiButton(PARAGON_BUTTON, width - 250, 12, 76, 20, "Paragon"));
        buttonList.add(new GuiButton(GUIDE_BUTTON, width - 168, 12, 74, 20, "Guide"));
        buttonList.add(new GuiButton(TOAST_BUTTON, width - 88, 12, 76, 20,
                "XP: " + (Forge112NotificationHud.isEnabled() ? "ON" : "OFF")));

        EntityPlayer player = mc.player;
        PlayerSkillDataCore data = player == null ? new PlayerSkillDataCore() : data(player);
        for (SkillCard card : layout.cards) {
            if (card.y + card.h < 36 || card.y > height - 8) {
                continue;
            }
            SkillStatsCore stats = data.getSkill(card.skill);
            int base = 1000 + card.skill.ordinal() * 10;
            int buttonY = card.y + card.h - 21;
            if (!data.isSkillSelected(card.skill)) {
                addSkillButton(base + 1, card.x + card.w - 82, buttonY, 74, 16, "Select", card.skill, "select");
            } else {
                addSkillButton(base + 3, card.x + card.w - 56, buttonY, 48, 16,
                        stats.getLevel() >= CONFIG.getMaxLevel() ? "Use" : "Use", card.skill, "ability");
                if (!data.isParagonSkill(card.skill) && stats.getLevel() >= CONFIG.getMaxLevel()) {
                    addSkillButton(base + 2, card.x + card.w - 136, buttonY, 74, 16, "Paragon", card.skill, "paragon");
                }
            }
            if (card.skill == SkillType.MINER) {
                addSkillButton(base + 4, card.x + 10, buttonY, 18, 16, "F", card.skill, "filter");
                addSkillButton(base + 5, card.x + 32, buttonY, 18, 16, "U", card.skill, "ultmine");
            }
        }
    }

    private void addSkillButton(int id, int x, int y, int w, int h, String text, SkillType skill, String action) {
        buttonList.add(new GuiButton(id, x, y, w, h, text));
        buttonActions.put(Integer.valueOf(id), new SkillButton(skill, action));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == GUIDE_BUTTON) {
            mc.displayGuiScreen(new GuideGui(this));
            return;
        }
        if (button.id == PARAGON_BUTTON) {
            mc.displayGuiScreen(new ParagonAbilityGui112());
            return;
        }
        if (button.id == TOAST_BUTTON) {
            Forge112NotificationHud.toggle();
            initGui();
            return;
        }
        SkillButton action = buttonActions.get(Integer.valueOf(button.id));
        if (action == null || mc.player == null) {
            return;
        }
        if ("select".equals(action.action)) {
            mc.player.sendChatMessage("/murilloskills select " + action.skill.name());
        } else if ("paragon".equals(action.action)) {
            mc.player.sendChatMessage("/murilloskills paragon " + action.skill.name());
        } else if ("ability".equals(action.action)) {
            mc.player.sendChatMessage("/murilloskills ability " + action.skill.name());
        } else if ("filter".equals(action.action)) {
            mc.displayGuiScreen(new OreFilterGui112(this));
        } else if ("ultmine".equals(action.action)) {
            mc.displayGuiScreen(new UltmineConfigGui112(this));
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0) {
            Layout layout = layout();
            int direction = delta > 0 ? -1 : 1;
            scroll = clamp(scroll + direction * 24, 0, layout.maxScroll);
            initGui();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawPremiumBackground();
        drawCenteredString(fontRenderer, "Skills", width / 2, 15, 0xFFFFC400);
        drawRect(width / 2 - 400, 37, width / 2 + 400, 39, 0xAAE0A817);

        EntityPlayer player = mc.player;
        PlayerSkillDataCore data = player == null ? new PlayerSkillDataCore() : data(player);
        drawDailyChallenges(data);
        Layout layout = layout();
        SkillCard hovered = null;
        for (SkillCard card : layout.cards) {
            if (card.y + card.h < 36 || card.y > height - 8) {
                continue;
            }
            drawSkillCard(card, data, mouseX, mouseY);
            if (mouseX >= card.x && mouseX <= card.x + card.w && mouseY >= card.y && mouseY <= card.y + card.h) {
                hovered = card;
            }
        }
        if (layout.maxScroll > 0) {
            drawScrollbar(layout);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (hovered != null && GuiScreen.isShiftKeyDown()) {
            drawHoveringText(skillTooltip(hovered.skill, data.getSkill(hovered.skill)), mouseX, mouseY);
        }
    }

    private void drawPremiumBackground() {
        drawGradientRect(0, 0, width, height, 0xF20A0B12, 0xF2101320);
        drawRect(0, 0, width, 1, 0x66000000);
        drawRect(0, 38, width, 40, 0x661C2030);
        drawRect(0, height - 18, width, height, 0xAA080910);
    }

    private void drawSkillCard(SkillCard card, PlayerSkillDataCore data, int mouseX, int mouseY) {
        SkillStatsCore stats = data.getSkill(card.skill);
        boolean selected = data.isSkillSelected(card.skill);
        boolean paragon = data.isParagonSkill(card.skill);
        boolean hovered = mouseX >= card.x && mouseX <= card.x + card.w && mouseY >= card.y && mouseY <= card.y + card.h;
        int accent = UiData.skillColor(card.skill);
        int border = paragon ? 0xFFFFC400 : selected ? 0xFF23C552 : hovered ? accent : 0xFF262A38;
        int background = selected ? 0xDD0C2818 : 0xD20E0F17;
        if (paragon) {
            background = 0xDD242634;
        }
        drawRect(card.x, card.y, card.x + card.w, card.y + card.h, background);
        drawRect(card.x, card.y, card.x + card.w, card.y + 2, border);
        drawRect(card.x, card.y + card.h - 2, card.x + card.w, card.y + card.h, border);
        drawRect(card.x, card.y, card.x + 2, card.y + card.h, border);
        drawRect(card.x + card.w - 2, card.y, card.x + card.w, card.y + card.h, border);

        drawItem(UiData.itemForSkill(card.skill), card.x + 12, card.y + 20);
        drawString(fontRenderer, UiData.title(card.skill), card.x + 44, card.y + 11, selected ? 0xFFFFD329 : 0xFF9C98B8);
        String level = "Lv" + stats.getLevel() + (stats.getPrestige() > 0 ? " P" + stats.getPrestige() : "");
        drawString(fontRenderer, level, card.x + card.w - fontRenderer.getStringWidth(level) - 12, card.y + 11,
                paragon ? 0xFFFFD329 : 0xFFD9DCF2);

        int barX = card.x + 44;
        int barY = card.y + 34;
        int barW = card.w - 72;
        drawRect(barX, barY, barX + barW, barY + 8, 0xFF2C2C3A);
        double pct = stats.getLevel() >= CONFIG.getMaxLevel() ? 1.0D
                : Math.min(1.0D, stats.getXp() / Math.max(1.0D, CONFIG.getXpForLevel(stats.getLevel())));
        drawRect(barX + 1, barY + 1, barX + 1 + (int) ((barW - 2) * pct), barY + 7, selected ? 0xFFE3A91B : 0xFF6F728A);
        String percent = stats.getLevel() >= CONFIG.getMaxLevel() ? "MAX" : (int) (pct * 100.0D) + "%";
        drawString(fontRenderer, percent, card.x + card.w - fontRenderer.getStringWidth(percent) - 12, barY + 1,
                selected ? 0xFFFFD329 : 0xFF9895B3);

        drawString(fontRenderer, paragon ? "PARAGON" : selected ? "ACTIVE" : "NOT ACTIVE", card.x + 44, card.y + 49,
                paragon ? 0xFFFFD329 : selected ? 0xFF23F05A : 0xFFFF3131);
        drawPerkRoad(card, stats);
    }

    private void drawPerkRoad(SkillCard card, SkillStatsCore stats) {
        UiData.Perk[] perks = UiData.perks(card.skill);
        if (perks.length == 0) {
            return;
        }
        int startX = card.x + 44;
        int y = card.y + card.h - 39;
        int gap = Math.max(9, Math.min(16, (card.w - 94) / Math.max(1, perks.length - 1)));
        for (int i = 0; i < perks.length; i++) {
            int px = startX + i * gap;
            int color = stats.getLevel() >= perks[i].level ? 0xFFFFD329 : 0xFF87879C;
            if (perks[i].level == CONFIG.getMaxLevel()) {
                color = stats.getLevel() >= perks[i].level ? 0xFFFFC400 : 0xFFE0A817;
            }
            drawRect(px, y, px + 7, y + 7, 0xFF1E2230);
            drawRect(px + 1, y + 1, px + 6, y + 6, color);
        }
    }

    private void drawItem(ItemStack stack, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();
    }

    private void drawScrollbar(Layout layout) {
        int x = width - 6;
        int top = 44;
        int bottom = height - 22;
        int trackH = Math.max(1, bottom - top);
        int knobH = Math.max(18, trackH * trackH / Math.max(trackH, layout.contentHeight));
        int knobY = top + (trackH - knobH) * scroll / Math.max(1, layout.maxScroll);
        drawRect(x, top, x + 2, bottom, 0x6636384A);
        drawRect(x - 1, knobY, x + 3, knobY + knobH, 0xFFE0A817);
    }

    private List<String> skillTooltip(SkillType skill, SkillStatsCore stats) {
        List<String> lines = new ArrayList<String>();
        lines.add(UiData.title(skill));
        lines.add("Progress to Level " + Math.min(CONFIG.getMaxLevel(), stats.getLevel() + 1) + ": "
                + (stats.getLevel() >= CONFIG.getMaxLevel() ? "MAX" : (int) stats.getXp() + " / " + CONFIG.getXpForLevel(stats.getLevel()) + " XP"));
        lines.add("");
        lines.add("Special Ability:");
        lines.add(UiData.ability(skill));
        lines.add("");
        lines.add("Passives:");
        for (String line : UiData.passives(skill)) {
            lines.add(line);
        }
        lines.add("");
        lines.add("Perks:");
        for (UiData.Perk perk : UiData.perks(skill)) {
            lines.add("L" + perk.level + " - " + perk.name);
        }
        lines.add("");
        lines.add("Synergies:");
        for (String line : UiData.synergies(skill)) {
            lines.add(line);
        }
        return lines;
    }

    private Layout layout() {
        int gap = 14;
        int availableWidth = Math.max(220, width - 48);
        int columns = width >= 900 ? 4 : width >= 620 ? 3 : width >= 400 ? 2 : 1;
        int cardW = Math.min(360, (availableWidth - gap * (columns - 1)) / columns);
        cardW = Math.max(width < 540 ? 164 : 210, cardW);
        int cardH = width < 540 ? 102 : 96;
        int rows = (SkillType.values().length + columns - 1) / columns;
        int startX = Math.max(12, (width - (columns * cardW + (columns - 1) * gap)) / 2);
        int startY = topOffset() - scroll;
        List<SkillCard> cards = new ArrayList<SkillCard>();
        int i = 0;
        for (SkillType skill : SkillType.values()) {
            int col = i % columns;
            int row = i / columns;
            cards.add(new SkillCard(skill, startX + col * (cardW + gap), startY + row * (cardH + gap), cardW, cardH));
            i++;
        }
        int contentHeight = rows * cardH + Math.max(0, rows - 1) * gap;
        int maxScroll = Math.max(0, topOffset() + contentHeight - (height - 26));
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
        return new Layout(cards, contentHeight, maxScroll);
    }

    private int topOffset() {
        return dailyChallenges().isEmpty() ? 54 : 96;
    }

    private List<Forge112DailyChallengeManager.Challenge> dailyChallenges() {
        if (mc == null || mc.player == null || mc.world == null) {
            return new ArrayList<Forge112DailyChallengeManager.Challenge>();
        }
        List<Forge112DailyChallengeManager.Challenge> challenges =
                Forge112DailyChallengeManager.view(data(mc.player), mc.world.getTotalWorldTime() / 24000L);
        return challenges.isEmpty() ? Forge112DailyChallengeManager.ensure(mc.player) : challenges;
    }

    private void drawDailyChallenges(PlayerSkillDataCore data) {
        List<Forge112DailyChallengeManager.Challenge> challenges = dailyChallenges();
        if (challenges.isEmpty()) {
            return;
        }
        int panelW = Math.min(760, width - 24);
        int x = (width - panelW) / 2;
        int y = 44;
        int cardGap = 8;
        int cardW = (panelW - cardGap * (challenges.size() - 1)) / challenges.size();
        for (int i = 0; i < challenges.size(); i++) {
            Forge112DailyChallengeManager.Challenge challenge = challenges.get(i);
            int cx = x + i * (cardW + cardGap);
            int accent = UiData.skillColor(challenge.skill);
            double progress = Math.min(1.0D, challenge.progress / (double) Math.max(1, challenge.target));
            GuiScreen.drawRect(cx, y, cx + cardW, y + 34, challenge.completed ? 0xD0112418 : Palette.CARD_BG_SUBTLE);
            drawPanelBorder(cx, y, cardW, 34, challenge.completed ? Palette.ACCENT_GREEN : Palette.SECTION_BORDER);
            GuiScreen.drawRect(cx, y, cx + 3, y + 34, accent);
            drawString(fontRenderer, fit(challenge.label, cardW - 66), cx + 8, y + 6,
                    challenge.completed ? Palette.TEXT_GREEN : Palette.TEXT_GOLD);
            String amount = challenge.progress + "/" + challenge.target;
            drawString(fontRenderer, amount, cx + cardW - fontRenderer.getStringWidth(amount) - 8, y + 6,
                    Palette.TEXT_LIGHT);
            GuiScreen.drawRect(cx + 8, y + 22, cx + cardW - 8, y + 27, Palette.PROGRESS_BAR_EMPTY);
            GuiScreen.drawRect(cx + 9, y + 23, cx + 9 + (int) ((cardW - 18) * progress), y + 26,
                    challenge.completed ? Palette.ACCENT_GREEN : Palette.ACCENT_GOLD);
        }
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
