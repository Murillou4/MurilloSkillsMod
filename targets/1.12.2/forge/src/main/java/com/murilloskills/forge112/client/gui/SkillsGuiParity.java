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

public final class SkillsGuiParity extends GuiScreen {
    private static final int GUIDE_BUTTON = 9400;
    private static final int TOAST_BUTTON = 9401;
    private static final int CONFIRM_BUTTON = 9402;
    private static final int SELECT_ALL_BUTTON = 9403;
    private final Map<Integer, SkillButton> buttonActions = new HashMap<Integer, SkillButton>();
    private final List<IconButton> iconButtons = new ArrayList<IconButton>();
    private final Set<SkillType> pendingSelection = new java.util.LinkedHashSet<SkillType>();
    private int cardWidth;
    private int cardHeight;
    private int padding;
    private int columns;
    private int startX;
    private int startY;
    private int headerHeight;
    private boolean compactCards;
    private boolean ultraCompactCards;
    private Boolean lastSelectionMode;

    @Override
    public void initGui() {
        buttonList.clear();
        buttonActions.clear();
        iconButtons.clear();

        EntityPlayer player = mc.player;
        PlayerSkillDataCore data = player == null ? new PlayerSkillDataCore() : data(player);
        boolean selectionMode = isSelectionMode(data);
        lastSelectionMode = Boolean.valueOf(selectionMode);
        if (selectionMode && pendingSelection.isEmpty()) {
            pendingSelection.addAll(data.getSelectedSkills());
        }
        calculateResponsiveLayout(selectionMode);

        SkillType[] skills = SkillType.values();
        if (selectionMode) {
            for (int i = 0; i < skills.length; i++) {
                SkillType skill = skills[i];
                int x = cardX(i);
                int y = cardY(i);
                boolean permanent = data.isSkillSelected(skill);
                boolean selected = permanent || pendingSelection.contains(skill);
                int btnHeight = cardActionButtonHeight();
                int btnX = x + 20;
                int btnY = cardBottomButtonY(y, btnHeight);
                int btnWidth = cardWidth - 40;
                String label = permanent ? "Defined" : selected ? "Selected" : "Select";
                GuiButton select = flatButton(10000 + i, btnX, btnY, btnWidth, btnHeight, label);
                select.enabled = !permanent && (selected || pendingSelection.size() < CONFIG.getMaxSelectedSkills());
                buttonList.add(select);
                buttonActions.put(Integer.valueOf(select.id), new SkillButton(skill, "select_pending"));
                if (permanent) {
                    int size = cardBottomButtonSize();
                    int resetX = x + cardWidth - size - 4;
                    int resetY = cardBottomButtonY(y, size);
                    addActionButton(10100 + i, resetX, resetY, size, size, "R", skill, "reset");
                }
            }
            boolean canSelectAll = CONFIG.getMaxSelectedSkills() >= skills.length;
            int confirmW = Math.min(300, Math.max(180, width / 3));
            int confirmH = 22;
            int rows = (int) Math.ceil(skills.length / (double) columns);
            int gridBottom = startY + rows * cardHeight + Math.max(0, rows - 1) * padding;
            int confirmY = Math.min(height - confirmH - 8, gridBottom + 6);
            int selectAllW = 92;
            int gap = canSelectAll ? 8 : 0;
            int totalW = confirmW + (canSelectAll ? selectAllW + gap : 0);
            int confirmX = (width - totalW) / 2;
            GuiButton confirm = flatButton(CONFIRM_BUTTON, confirmX, confirmY, confirmW, confirmH, confirmText());
            confirm.enabled = !pendingSelection.isEmpty() && pendingSelection.size() <= CONFIG.getMaxSelectedSkills();
            buttonList.add(confirm);
            if (canSelectAll) {
                GuiButton selectAll = flatButton(SELECT_ALL_BUTTON, confirmX + confirmW + gap, confirmY,
                        selectAllW, confirmH, "Select All");
                selectAll.enabled = pendingSelection.size() < skills.length;
                buttonList.add(selectAll);
            }
            return;
        }

        for (int i = 0; i < skills.length; i++) {
            SkillType skill = skills[i];
            SkillStatsCore stats = data.getSkill(skill);
            boolean selected = data.isSkillSelected(skill);
            int x = cardX(i);
            int y = cardY(i);

            if (data.canActivateParagonSkill(skill) && selected && stats.getLevel() >= CONFIG.getMaxLevel() - 1) {
                int rightReserve = bottomControlReserve(skill, selected);
                int btnHeight = cardActionButtonHeight();
                int btnWidth = Math.max(72, cardWidth - 16 - rightReserve);
                addActionButton(10200 + i, x + 8, cardBottomButtonY(y, btnHeight), btnWidth,
                        btnHeight, "MAKE PARAGON", skill, "paragon");
            }

            if (!compactCards && data.isParagonSkill(skill) && stats.getLevel() >= CONFIG.getMaxLevel()
                    && stats.getPrestige() < CONFIG.getMaxPrestigeLevel()) {
                int btnHeight = 15;
                int btnY = cardBottomButtonY(y, btnHeight);
                int rightReserve = bottomControlReserve(skill, selected);
                int btnWidth = Math.max(72, cardWidth - 16 - rightReserve);
                addActionButton(10300 + i, x + 8, btnY, btnWidth, btnHeight,
                        "PRESTIGE P" + (stats.getPrestige() + 1), skill, "prestige");
            }

            if (selected) {
                int size = cardBottomButtonSize();
                int resetX = x + cardWidth - size - 4;
                int resetY = cardBottomButtonY(y, size);
                addActionButton(10400 + i, resetX, resetY, size, size, "R", skill, "reset");
                if (skill == SkillType.MINER) {
                    int gap = 2;
                    int ultmineX = resetX - gap - size;
                    int filterX = ultmineX - gap - size;
                    if (filterX < x + 4) {
                        filterX = x + 4;
                        ultmineX = filterX + size + gap;
                    }
                    addIconButton(10500 + i, filterX, resetY, size, new ItemStack(Blocks.DIAMOND_ORE), skill, "filter");
                    addIconButton(10600 + i, ultmineX, resetY, size, new ItemStack(Items.DIAMOND_PICKAXE), skill, "ultmine");
                }
            }
        }

        int toastW = 130;
        int toastH = 16;
        int toastX = width - toastW - 10;
        buttonList.add(flatButton(TOAST_BUTTON, toastX, 8, toastW, toastH,
                "XP Toasts: " + (Forge112NotificationHud.isEnabled() ? "ON" : "OFF")));
        buttonList.add(flatButton(GUIDE_BUTTON, toastX - 80, 7, 70, 18, "Guide"));
    }

    private void addActionButton(int id, int x, int y, int w, int h, String text, SkillType skill, String action) {
        buttonList.add(flatButton(id, x, y, w, h, text));
        buttonActions.put(Integer.valueOf(id), new SkillButton(skill, action));
    }

    private void addIconButton(int id, int x, int y, int size, ItemStack icon, SkillType skill, String action) {
        addActionButton(id, x, y, size, size, "", skill, action);
        iconButtons.add(new IconButton(id, x, y, size, icon));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == GUIDE_BUTTON) {
            mc.displayGuiScreen(new GuideGuiParity(this));
            return;
        }
        if (button.id == TOAST_BUTTON) {
            Forge112NotificationHud.toggle();
            button.displayString = "XP Toasts: " + (Forge112NotificationHud.isEnabled() ? "ON" : "OFF");
            return;
        }
        if (button.id == CONFIRM_BUTTON) {
            if (mc.player != null) {
                for (SkillType skill : pendingSelection) {
                    mc.player.sendChatMessage("/murilloskills select " + skill.name());
                }
            }
            mc.displayGuiScreen(null);
            return;
        }
        if (button.id == SELECT_ALL_BUTTON) {
            pendingSelection.clear();
            Collections.addAll(pendingSelection, SkillType.values());
            initGui();
            return;
        }

        SkillButton action = buttonActions.get(Integer.valueOf(button.id));
        if (action == null || mc.player == null) {
            return;
        }
        if ("select_pending".equals(action.action)) {
            if (pendingSelection.contains(action.skill)) {
                pendingSelection.remove(action.skill);
            } else if (pendingSelection.size() < CONFIG.getMaxSelectedSkills()) {
                pendingSelection.add(action.skill);
            }
            initGui();
        } else if ("paragon".equals(action.action)) {
            mc.player.sendChatMessage("/murilloskills paragon " + action.skill.name());
            predictParagon(action.skill);
            initGui();
        } else if ("prestige".equals(action.action)) {
            mc.player.sendChatMessage("/murilloskills prestige " + action.skill.name());
            predictPrestige(action.skill);
            initGui();
        } else if ("reset".equals(action.action)) {
            STORE.cache.put(mc.player.getUniqueID(), new PlayerSkillDataCore());
            STORE.save(mc.player.getUniqueID());
            pendingSelection.clear();
            mc.player.sendChatMessage("/murilloskills reset");
            mc.displayGuiScreen(new SkillsGuiParity());
        } else if ("filter".equals(action.action)) {
            mc.displayGuiScreen(new OreFilterGui112(this));
        } else if ("ultmine".equals(action.action)) {
            mc.displayGuiScreen(new UltmineConfigGui112(this));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        EntityPlayer player = mc.player;
        PlayerSkillDataCore data = player == null ? new PlayerSkillDataCore() : data(player);
        boolean selectionMode = isSelectionMode(data);
        if (lastSelectionMode == null || lastSelectionMode.booleanValue() != selectionMode) {
            initGui();
            return;
        }
        calculateResponsiveLayout(selectionMode);

        renderGradientBackground();
        int vignetteSize = Math.min(width, height) / 3;
        drawVignetteCorner(0, 0, vignetteSize, true, true);
        drawVignetteCorner(width - vignetteSize, 0, vignetteSize, false, true);
        drawVignetteCorner(0, height - vignetteSize, vignetteSize, true, false);
        drawVignetteCorner(width - vignetteSize, height - vignetteSize, vignetteSize, false, false);
        renderHeader(selectionMode);

        SkillType hovered = null;
        SkillType[] skills = SkillType.values();
        for (int i = 0; i < skills.length; i++) {
            SkillType skill = skills[i];
            int x = cardX(i);
            int y = cardY(i);
            drawSkillCard(skill, data, x, y, mouseX, mouseY, selectionMode);
            if (inside(mouseX, mouseY, x, y, cardWidth, cardHeight)) {
                hovered = skill;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        renderIconButtons();
        renderDailyChallengesPanel(selectionMode);

        if (hovered != null && !hoveringAnyButton(mouseX, mouseY)) {
            boolean expanded = GuiScreen.isShiftKeyDown();
            drawHoveringText(skillTooltip(hovered, data.getSkill(hovered), data, selectionMode, expanded), mouseX, mouseY);
        }
    }

    private void drawSkillCard(SkillType skill, PlayerSkillDataCore data, int x, int y, int mouseX, int mouseY,
            boolean selectionMode) {
        SkillStatsCore stats = data.getSkill(skill);
        boolean selected = data.isSkillSelected(skill);
        boolean paragon = data.isParagonSkill(skill);
        boolean pending = pendingSelection.contains(skill);
        boolean locked = !selectionMode && !selected && !data.getSelectedSkills().isEmpty();
        boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, cardHeight);

        int cardBg;
        int border;
        if (selectionMode) {
            cardBg = pending ? (hovered ? Palette.SECTION_BG_ACTIVE : Palette.CARD_BG_PENDING)
                    : (hovered ? Palette.SECTION_BG_ACTIVE : Palette.SECTION_BG);
            border = pending ? Palette.ACCENT_GREEN : hovered ? Palette.ACCENT_GOLD : Palette.SECTION_BORDER;
        } else if (paragon) {
            cardBg = Palette.CARD_BG_PARAGON;
            border = hovered ? Palette.ACCENT_GOLD : Palette.TEXT_GOLD;
        } else if (locked) {
            cardBg = Palette.CARD_BG_LOCKED;
            border = Palette.CARD_BORDER_LOCKED;
        } else if (selected) {
            cardBg = hovered ? Palette.SECTION_BG_ACTIVE : Palette.CARD_BG_SELECTED;
            border = hovered ? Palette.ACCENT_GOLD : Palette.ACCENT_GREEN;
        } else {
            cardBg = hovered ? Palette.SECTION_BG_ACTIVE : Palette.SECTION_BG;
            border = hovered ? Palette.ACCENT_GOLD : Palette.SECTION_BORDER;
        }

        renderModernCard(x, y, cardWidth, cardHeight, cardBg, border, hovered, selected && !locked, paragon);

        int iconY = y + (ultraCompactCards ? 10 : 14);
        if (!locked && (selected || paragon) && !ultraCompactCards) {
            drawRect(x + 3, iconY - 2, x + 23, iconY + 18, Palette.PANEL_HIGHLIGHT);
        }
        drawItem(UiData.itemForSkill(skill), x + 5, iconY, 1.0F);

        if (!selectionMode && locked) {
            drawString(fontRenderer, "L", x + cardWidth - 16, y + 6, Palette.TEXT_MUTED);
        }

        String level = "Lv" + stats.getLevel();
        int levelColor = locked ? Palette.TEXT_MUTED
                : stats.getLevel() >= CONFIG.getMaxLevel() ? Palette.TEXT_GOLD : Palette.TEXT_LIGHT;
        int levelX = x + cardWidth - fontRenderer.getStringWidth(level) - 6;
        int titleRight = levelX - 4;
        boolean showInlinePrestige = stats.getPrestige() > 0 && !locked && !compactCards;
        if (showInlinePrestige) {
            titleRight -= fontRenderer.getStringWidth("P" + stats.getPrestige()) + 4;
        }
        int titleColor = locked ? Palette.TEXT_MUTED : Palette.TEXT_GOLD;
        drawString(fontRenderer, fitToWidth(UiData.title(skill), Math.max(24, titleRight - (x + 28))), x + 28, y + 6, titleColor);
        drawString(fontRenderer, level, levelX, y + 6, levelColor);
        if (showInlinePrestige) {
            String prestige = "P" + stats.getPrestige();
            drawString(fontRenderer, prestige, levelX - fontRenderer.getStringWidth(prestige) - 4, y + 6,
                    prestigeColor(stats.getPrestige()));
        }

        if (!selectionMode) {
            renderXpBar(x + 28, y + (ultraCompactCards ? 22 : 25), stats, locked);
        }

        boolean compactActionButton = compactCards && !selectionMode && data.canActivateParagonSkill(skill)
                && selected && stats.getLevel() >= CONFIG.getMaxLevel() - 1;
        if (locked) {
            if (!ultraCompactCards) {
                drawString(fontRenderer, "NOT ACTIVE", x + 28, y + 40, Palette.STATUS_INACTIVE);
            }
        } else if (paragon) {
            if (!ultraCompactCards) {
                drawString(fontRenderer, "READY", x + 28, statusY(y), Palette.STATUS_READY);
            }
            if (!compactCards) {
                drawString(fontRenderer, "P", x + 120, y - 4, Palette.TEXT_YELLOW);
            }
        } else if (selected) {
            if (!ultraCompactCards && !compactActionButton) {
                drawString(fontRenderer, "ACTIVE", x + 28, statusY(y), Palette.STATUS_ACTIVE);
            }
        }

        if (!selectionMode && !ultraCompactCards) {
            int bottomButtonY = cardBottomButtonY(y, cardBottomButtonSize());
            int markerSize = perkMarkerSize();
            int minimumGap = cardHeight <= 86 ? 10 : 13;
            int preferredY = cardHeight <= 86 ? y + 51 : y + 55;
            int roadmapY = Math.min(preferredY, bottomButtonY - markerSize - 2);
            roadmapY = Math.max(statusY(y) + minimumGap, roadmapY);
            if (roadmapY + markerSize <= bottomButtonY - 2) {
                renderPerkRoadmap(x + 28, roadmapY, skill, stats.getLevel(), locked);
            }
        }
    }

    private void calculateResponsiveLayout(boolean selectionMode) {
        int marginX = selectionMode ? 12 : 14;
        headerHeight = selectionMode ? Math.max(24, Math.min(42, height / 7))
                : Math.max(28, Math.min(42, height / 11));
        int topGap = selectionMode ? 5 : 8;
        int bottomReserve = selectionMode ? 30 : 34;
        int availableWidth = Math.max(1, width - marginX * 2);
        int availableHeight = Math.max(1, height - headerHeight - topGap - bottomReserve);
        int skillCount = SkillType.values().length;
        int minCardWidth = selectionMode ? 112 : 104;
        int maxCardWidth = selectionMode ? 190 : 180;
        int minCardHeight = selectionMode ? 38 : 44;
        int maxCardHeight = selectionMode ? 64 : 82;

        int bestColumns = 1;
        int bestHeight = -1;
        int bestWidth = minCardWidth;
        for (int candidate = 1; candidate <= 4; candidate++) {
            if (selectionMode && candidate > 3) {
                continue;
            }
            int candidatePadding = Math.max(4, Math.min(selectionMode ? 8 : 10, availableWidth / 60));
            int candidateWidth = (availableWidth - (candidate - 1) * candidatePadding) / candidate;
            if (candidateWidth < minCardWidth) {
                continue;
            }
            candidateWidth = Math.min(maxCardWidth, candidateWidth);
            int rows = (int) Math.ceil(skillCount / (double) candidate);
            int naturalHeight = Math.min(maxCardHeight, Math.max(minCardHeight, candidateWidth * 48 / 100));
            int fitHeight = (availableHeight - (rows - 1) * candidatePadding) / rows;
            int candidateHeight = Math.min(naturalHeight, fitHeight);
            if (candidateHeight >= minCardHeight
                    && (candidate > bestColumns || candidateHeight > bestHeight + 12)) {
                bestColumns = candidate;
                bestHeight = candidateHeight;
                bestWidth = candidateWidth;
            }
        }

        columns = bestColumns;
        int rows = (int) Math.ceil(skillCount / (double) columns);
        padding = selectionMode ? Math.max(4, Math.min(8, availableWidth / 60))
                : Math.max(4, Math.min(10, availableWidth / 55));
        int totalPaddingX = (columns - 1) * padding;
        int totalPaddingY = (rows - 1) * padding;
        cardWidth = Math.min(maxCardWidth, Math.max(minCardWidth, (availableWidth - totalPaddingX) / columns));
        cardHeight = Math.min(maxCardHeight, Math.max(minCardHeight, cardWidth * 48 / 100));
        int neededHeight = rows * cardHeight + totalPaddingY;
        if (neededHeight > availableHeight) {
            cardHeight = Math.max(minCardHeight, (availableHeight - totalPaddingY) / rows);
        }
        if (bestHeight < 0) {
            cardWidth = bestWidth;
            cardHeight = minCardHeight;
        }
        compactCards = cardHeight < 62;
        ultraCompactCards = cardHeight < 50;
        int totalGridWidth = columns * cardWidth + totalPaddingX;
        startX = (width - totalGridWidth) / 2;
        startY = headerHeight + topGap;
    }

    private int cardX(int index) {
        return startX + (index % columns) * (cardWidth + padding);
    }

    private int cardY(int index) {
        return startY + (index / columns) * (cardHeight + padding);
    }

    private boolean isSelectionMode(PlayerSkillDataCore data) {
        return data.getSelectedSkills().isEmpty();
    }

    private int cardActionButtonHeight() {
        return Math.max(11, Math.min(14, cardHeight / 4));
    }

    private int cardBottomButtonSize() {
        return Math.max(12, Math.min(16, cardHeight / 5));
    }

    private int cardBottomButtonY(int cardY, int buttonHeight) {
        int bottomPadding = ultraCompactCards ? 4 : compactCards ? 5 : 8;
        return cardY + cardHeight - buttonHeight - bottomPadding;
    }

    private int bottomControlReserve(SkillType skill, boolean selected) {
        if (!selected) {
            return 0;
        }
        int size = cardBottomButtonSize();
        int gap = 2;
        int controls = skill == SkillType.MINER ? 3 : 1;
        return controls * size + Math.max(0, controls - 1) * gap + 8;
    }

    private int statusY(int cardY) {
        return cardY + (cardHeight <= 86 ? 38 : 40);
    }

    private int perkMarkerSize() {
        return cardHeight <= 86 ? 5 : 6;
    }

    private void predictParagon(SkillType skill) {
        if (mc.player == null) {
            return;
        }
        PlayerSkillDataCore data = data(mc.player);
        SkillStatsCore stats = data.getSkill(skill);
        if (!data.isSkillSelected(skill)) {
            data.setSelectedSkills(Collections.singletonList(skill), CONFIG);
        }
        stats.setLevel(Math.max(stats.getLevel(), CONFIG.getMaxLevel()));
        data.activateParagonSkill(skill);
        STORE.save(mc.player.getUniqueID());
    }

    private void predictPrestige(SkillType skill) {
        if (mc.player == null) {
            return;
        }
        PlayerSkillDataCore data = data(mc.player);
        SkillStatsCore stats = data.getSkill(skill);
        if (!data.isParagonSkill(skill) || stats.getLevel() < CONFIG.getMaxLevel()
                || stats.getPrestige() >= CONFIG.getMaxPrestigeLevel()) {
            return;
        }
        stats.setPrestige(stats.getPrestige() + 1);
        stats.setLevel(0);
        stats.setXp(0.0D);
        stats.setLastAbilityUse(-1L);
        STORE.save(mc.player.getUniqueID());
    }

    private String confirmText() {
        return pendingSelection.size() >= CONFIG.getMaxSelectedSkills()
                ? "Confirm (" + pendingSelection.size() + "/" + CONFIG.getMaxSelectedSkills() + ")"
                : "Save (" + pendingSelection.size() + "/" + CONFIG.getMaxSelectedSkills() + ")";
    }

    private void renderHeader(boolean selectionMode) {
        for (int i = 0; i < headerHeight; i++) {
            float ratio = (float) i / Math.max(1, headerHeight);
            int alpha = (int) (0xF0 * (1 - ratio * 0.3F));
            drawRect(0, i, width, i + 1, (alpha << 24) | Palette.HEADER_GRADIENT_BASE);
        }
        drawRect(0, headerHeight - 2, width, headerHeight - 1, Palette.HEADER_ACCENT_LINE);
        drawRect(width / 4, headerHeight - 1, width * 3 / 4, headerHeight, Palette.ACCENT_GOLD);

        int titleY = (headerHeight - 20) / 2;
        if (selectionMode) {
            drawCenteredString(fontRenderer, "Choose Skills", width / 2, titleY, Palette.TEXT_GOLD);
            drawCenteredString(fontRenderer, "Selected: " + pendingSelection.size() + "/" + CONFIG.getMaxSelectedSkills(),
                    width / 2, titleY + 12, Palette.TEXT_GRAY);
        } else {
            String title = "Skills";
            int titleW = fontRenderer.getStringWidth(title);
            int headerButtonStartX = width - 130 - 10 - 70 - 10;
            if (width / 2 + titleW / 2 + 8 >= headerButtonStartX) {
                drawString(fontRenderer, title, 10, titleY + 5, Palette.TEXT_GOLD);
            } else {
                drawCenteredString(fontRenderer, title, width / 2, titleY + 5, Palette.TEXT_GOLD);
            }
        }
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

    private void drawVignetteCorner(int x, int y, int size, boolean left, boolean top) {
        for (int i = 0; i < 8; i++) {
            float ratio = (float) i / 8.0F;
            int alpha = (int) (0x15 * (1 - ratio));
            if (alpha <= 0) {
                continue;
            }
            int color = alpha << 24;
            int offset = (int) (size * ratio);
            int x1 = left ? x : x + offset;
            int x2 = left ? x + size - offset : x + size;
            int y1 = top ? y : y + offset;
            int y2 = top ? y + size - offset : y + size;
            if (x1 < x2 && y1 < y2) {
                drawRect(x1, y1, x2, y2, color);
            }
        }
    }

    private void renderModernCard(int x, int y, int w, int h, int bgColor, int borderColor, boolean hovered,
            boolean active, boolean paragon) {
        if (paragon) {
            renderGlowingBorder(x, y, w, h, Palette.CARD_GLOW_PARAGON);
        } else if (active) {
            drawRect(x - 2, y - 2, x + w + 2, y + h + 2, Palette.CARD_GLOW_ACTIVE);
        } else if (hovered) {
            drawRect(x - 1, y - 1, x + w + 1, y + h + 1, Palette.PANEL_SHADOW);
        }
        drawRect(x, y, x + w, y + h, bgColor);
        drawRect(x + 1, y + 1, x + w - 1, y + 2, Palette.PANEL_HIGHLIGHT);
        drawPanelBorder(x, y, w, h, borderColor);
        if (hovered || active || paragon) {
            renderCornerAccents(x, y, w, h, 4, paragon ? Palette.ACCENT_GOLD : active ? Palette.ACCENT_GREEN : borderColor);
        }
    }

    private void renderXpBar(int x, int y, SkillStatsCore stats, boolean locked) {
        double needed = Math.max(1.0D, CONFIG.getXpForLevel(stats.getLevel()));
        float progress = (float) Math.max(0.0D, Math.min(1.0D, stats.getXp() / needed));
        String text = "";
        int textColor = Palette.TEXT_MUTED;
        if (stats.getLevel() >= CONFIG.getMaxLevel() && !locked) {
            progress = 1.0F;
            text = "MAX";
            textColor = Palette.TEXT_GOLD;
        } else if (!locked) {
            text = Integer.toString((int) (progress * 100.0F)) + "%";
        }
        int textWidth = text.isEmpty() ? 0 : fontRenderer.getStringWidth(text);
        int textX = 0;
        int barWidth;
        if (!text.isEmpty()) {
            int cardStartX = x - 28;
            int cardRight = cardStartX + cardWidth;
            textX = cardRight - 6 - textWidth;
            barWidth = (textX - 4) - x;
        } else {
            barWidth = cardWidth - 36;
        }
        int fill = locked ? Palette.PROGRESS_LOCKED_FILL
                : stats.getLevel() >= CONFIG.getMaxLevel() ? Palette.ACCENT_GOLD : Palette.PROGRESS_BAR_FILL;
        int shine = locked ? Palette.PROGRESS_LOCKED_SHINE
                : stats.getLevel() >= CONFIG.getMaxLevel() ? Palette.PROGRESS_MAX_SHINE : Palette.PROGRESS_BAR_SHINE;
        renderProgressBar(x, y, Math.max(8, barWidth), 8, progress, Palette.PROGRESS_BAR_EMPTY, fill, shine);
        if (!text.isEmpty()) {
            drawString(fontRenderer, text, textX, y, textColor);
        }
    }

    private void renderPerkRoadmap(int x, int y, SkillType skill, int currentLevel, boolean locked) {
        if (locked) {
            return;
        }
        UiData.Perk[] perks = UiData.perks(skill);
        if (perks.length == 0) {
            return;
        }
        int markerSize = 6;
        int spacing = 10;
        int shownCount = Math.min(perks.length, 5);
        int lineLength = shownCount * spacing - spacing + markerSize;
        drawRect(x, y + markerSize / 2, x + lineLength, y + markerSize / 2 + 1, Palette.TEXT_MUTED);
        int px = x;
        for (int i = 0; i < shownCount; i++) {
            UiData.Perk perk = perks[i];
            boolean unlocked = currentLevel >= perk.level;
            boolean master = perk.level == CONFIG.getMaxLevel();
            boolean close = !unlocked && currentLevel >= perk.level - 15;
            if (close) {
                drawRect(px, y, px + markerSize, y + markerSize, Palette.TEXT_YELLOW);
                drawPanelBorder(px, y, markerSize, markerSize, Palette.PERK_CLOSE_MARKER);
            } else {
                renderMilestoneMarker(px, y, markerSize, unlocked, master);
            }
            px += spacing;
        }
    }

    private void renderDailyChallengesPanel(boolean selectionMode) {
        if (selectionMode) {
            return;
        }
    }

    private void renderIconButtons() {
        for (IconButton button : iconButtons) {
            float scale = 0.72F;
            int iconSize = Math.round(16 * scale);
            int iconX = button.x + Math.max(0, (button.size - iconSize) / 2);
            int iconY = button.y + Math.max(0, (button.size - iconSize) / 2);
            drawItem(button.icon, iconX, iconY, scale);
        }
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

    private List<String> skillTooltip(SkillType skill, SkillStatsCore stats, PlayerSkillDataCore data,
            boolean selectionMode, boolean expanded) {
        List<String> lines = new ArrayList<String>();
        lines.add(UiData.title(skill));
        double needed = Math.max(1.0D, CONFIG.getXpForLevel(stats.getLevel()));
        int percent = stats.getLevel() >= CONFIG.getMaxLevel() ? 100 : (int) Math.min(100.0D, stats.getXp() * 100.0D / needed);
        lines.add("Progress to Level " + Math.min(CONFIG.getMaxLevel(), stats.getLevel() + 1) + ": "
                + (stats.getLevel() >= CONFIG.getMaxLevel() ? "MAX" : percent + "%"));
        if (selectionMode) {
            lines.add(pendingSelection.contains(skill) ? "Selected for save" : "Click Select to choose this skill");
        } else {
            lines.add(data.isParagonSkill(skill) ? "PARAGON" : data.isSkillSelected(skill) ? "ACTIVE" : "NOT ACTIVE");
        }
        if (!expanded) {
            lines.add("");
            lines.add("Hold Shift for ability, passives, perks and synergies.");
            return lines;
        }
        lines.add("");
        lines.add("Special Ability:");
        lines.add(UiData.ability(skill));
        lines.add("");
        lines.add("XP Sources:");
        lines.add(UiData.description(skill));
        lines.add("");
        lines.add("Passives:");
        for (String passive : UiData.passives(skill)) {
            lines.add(passive.trim());
        }
        lines.add("");
        lines.add("Next Perks:");
        for (UiData.Perk perk : UiData.perks(skill)) {
            lines.add("L" + perk.level + " - " + perk.name + ": " + perk.detail);
        }
        lines.add("");
        lines.add("Synergies:");
        String[] synergies = UiData.synergies(skill);
        if (synergies.length == 0) {
            lines.add("No direct synergy listed.");
        } else {
            for (String synergy : synergies) {
                lines.add(synergy.trim());
            }
        }
        return lines;
    }

    private boolean hoveringAnyButton(int mouseX, int mouseY) {
        for (GuiButton button : buttonList) {
            if (button.visible && inside(mouseX, mouseY, button.x, button.y, button.width, button.height)) {
                return true;
            }
        }
        return false;
    }

    private String fitToWidth(String text, int maxWidth) {
        if (fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String trimmed = text;
        while (trimmed.length() > 2 && fontRenderer.getStringWidth(trimmed + ".") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ".";
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
