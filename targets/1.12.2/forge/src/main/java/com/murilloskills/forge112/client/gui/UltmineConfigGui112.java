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

public final class UltmineConfigGui112 extends GuiScreen {
    private static final int BACK = 28000;
    private static final int SHAPE_BASE = 28100;
    private static final int DROPS = 28200;
    private static final int STORAGE = 28201;
    private static final int XP = 28202;
    private static final int SAME = 28203;
    private static final int MAGNET = 28204;
    private static final int DEPTH_MINUS = 28300;
    private static final int DEPTH_PLUS = 28301;
    private static final int LENGTH_MINUS = 28302;
    private static final int LENGTH_PLUS = 28303;
    private static final int VARIANT = 28304;
    private static final int LEGACY_MINUS = 28305;
    private static final int LEGACY_PLUS = 28306;
    private static final int MAGNET_MINUS = 28307;
    private static final int MAGNET_PLUS = 28308;
    private static final int TRASH_ADD = 28400;
    private static final int TRASH_BROWSE = 28401;
    private static final int TRASH_UP = 28402;
    private static final int TRASH_DOWN = 28403;
    private static final int TRASH_REMOVE_BASE = 28500;
    private static final int CLASSIC_ADD = 28600;
    private static final int CLASSIC_BROWSE = 28601;
    private static final int CLASSIC_UP = 28602;
    private static final int CLASSIC_DOWN = 28603;
    private static final int CLASSIC_REMOVE_BASE = 28700;
    private static final int STORAGE_WHITELIST = 28800;
    private final GuiScreen parent;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int leftX;
    private int leftW;
    private int rightX;
    private int rightW;
    private int trashScroll;
    private int classicScroll;
    private int trashY;
    private int trashH;
    private int classicY;
    private int classicH;
    private boolean draggingTrashScrollbar;
    private boolean draggingClassicScrollbar;
    private int trashScrollbarGrabOffset;
    private int classicScrollbarGrabOffset;
    private GuiTextField trashField;
    private GuiTextField classicField;
    private List<String> trashItems = new ArrayList<String>();
    private List<String> classicBlocks = new ArrayList<String>();

    public UltmineConfigGui112(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        ClientUltmineConfig.load();
        trashItems = ClientUltmineConfig.getTrashItems();
        classicBlocks = ClientUltmineConfig.getLegacyBlockedBlocks();
        buttonList.clear();
        panelW = Math.min(720, width - 20);
        panelH = Math.min(430, height - 20);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        leftX = panelX + 14;
        leftW = (panelW - 42) / 2;
        rightX = leftX + leftW + 14;
        rightW = panelW - 28 - leftW - 14;

        buttonList.add(flatButton(BACK, panelX + panelW - 82, panelY + panelH - 28, 70, 20, "Back"));
        int shapeW = Math.max(58, (panelW - 28 - (UltmineShape112.values().length - 1) * 4) / UltmineShape112.values().length);
        int startX = panelX + 14;
        int shapeY = panelY + 42;
        UltmineShape112[] shapes = UltmineShape112.values();
        for (int i = 0; i < shapes.length; i++) {
            UltmineShape112 shape = shapes[i];
            GuiButton b = flatButton(SHAPE_BASE + i, startX + i * (shapeW + 4), shapeY, shapeW, 20, shapeLabel(shape));
            b.enabled = shape != ClientUltmineConfig.getSelectedShape();
            buttonList.add(b);
        }
        int toggleY = panelY + 72;
        int toggleW = (panelW - 36) / 4;
        addToggle(DROPS, panelX + 12, toggleY, toggleW, "Inventory", ClientUltmineConfig.isDropsToInventory());
        addToggle(STORAGE, panelX + 18 + toggleW, toggleY, toggleW, "Storage", ClientUltmineConfig.isDropsToStorage());
        addToggle(XP, panelX + 24 + toggleW * 2, toggleY, toggleW, "XP Direct", ClientUltmineConfig.isXpDirectToPlayer());
        addToggle(SAME, panelX + 30 + toggleW * 3, toggleY, toggleW, "Same Block", ClientUltmineConfig.isSameBlockOnly());
        buttonList.add(flatButton(STORAGE_WHITELIST, panelX + 18 + toggleW, toggleY + 24, toggleW, 18, "Whitelist"));

        int sectionTop = panelY + 106;
        int bottom = panelY + panelH - 40;
        int shapeCardY = sectionTop;
        int shapeCardH = 120;
        int classicCardY = shapeCardY + shapeCardH + 10;
        int classicCardH = 64;
        int magnetCardY = classicCardY + classicCardH + 10;
        int rowX = leftX + 12;
        int shapeRowY = shapeCardY + 42;
        int classicRowY = classicCardY + 28;
        int magnetToggleY = magnetCardY + 26;
        int magnetRangeY = magnetCardY + 58;
        buttonList.add(flatButton(DEPTH_MINUS, rowX + 94, shapeRowY, 24, 18, "-"));
        buttonList.add(flatButton(DEPTH_PLUS, rowX + 172, shapeRowY, 24, 18, "+"));
        buttonList.add(flatButton(LENGTH_MINUS, rowX + 94, shapeRowY + 28, 24, 18, "-"));
        buttonList.add(flatButton(LENGTH_PLUS, rowX + 172, shapeRowY + 28, 24, 18, "+"));
        buttonList.add(flatButton(VARIANT, rowX + 94, shapeRowY + 56, 102, 18, variantLabel()));
        buttonList.add(flatButton(LEGACY_MINUS, rowX + 94, classicRowY, 24, 18, "-"));
        buttonList.add(flatButton(LEGACY_PLUS, rowX + 172, classicRowY, 24, 18, "+"));
        addToggle(MAGNET, rowX + 64, magnetToggleY, 132, "Magnet", ClientUltmineConfig.isMagnetEnabled());
        buttonList.add(flatButton(MAGNET_MINUS, rowX + 94, magnetRangeY, 24, 18, "-"));
        buttonList.add(flatButton(MAGNET_PLUS, rowX + 172, magnetRangeY, 24, 18, "+"));

        int listH = Math.max(88, (bottom - sectionTop - 12) / 2);
        trashY = sectionTop;
        trashH = listH;
        classicY = trashY + listH + 12;
        classicH = Math.max(88, bottom - classicY);
        int fieldW = Math.max(80, rightW - 90);
        trashField = new GuiTextField(1, fontRenderer, rightX + 10, trashY + 26, fieldW, 18);
        trashField.setMaxStringLength(100);
        classicField = new GuiTextField(2, fontRenderer, rightX + 10, classicY + 26, fieldW, 18);
        classicField.setMaxStringLength(120);
        buttonList.add(flatButton(TRASH_ADD, rightX + 14 + fieldW, trashY + 26, 22, 18, "+"));
        buttonList.add(flatButton(TRASH_BROWSE, rightX + 40 + fieldW, trashY + 26, 48, 18, "Browse"));
        buttonList.add(flatButton(CLASSIC_ADD, rightX + 14 + fieldW, classicY + 26, 22, 18, "+"));
        buttonList.add(flatButton(CLASSIC_BROWSE, rightX + 40 + fieldW, classicY + 26, 48, 18, "Browse"));

        addListControls(false, trashItems, trashY, trashH, trashScroll, TRASH_REMOVE_BASE);
        addListControls(true, classicBlocks, classicY, classicH, classicScroll, CLASSIC_REMOVE_BASE);
    }

    private void addToggle(int id, int x, int y, int w, String label, boolean enabled) {
        buttonList.add(flatButton(id, x, y, w, 20, label + ": " + (enabled ? "ON" : "OFF")));
    }

    private void addListControls(boolean classic, List<String> values, int sectionY, int sectionH, int offset,
            int removeBase) {
        int visible = visibleListRows(sectionH);
        int maxScroll = Math.max(0, values.size() - visible);
        if (classic) {
            classicScroll = clamp(classicScroll, 0, maxScroll);
            offset = classicScroll;
        } else {
            trashScroll = clamp(trashScroll, 0, maxScroll);
            offset = trashScroll;
        }
        int count = Math.min(visible, Math.max(0, values.size() - offset));
        for (int i = 0; i < count; i++) {
            int y = sectionY + 50 + i * 16;
            buttonList.add(flatButton(removeBase + i, rightX + rightW - 29, y - 2, 18, 14, "x"));
        }
    }

    private int visibleListRows(int sectionH) {
        return Math.max(1, (sectionH - 58) / 16);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        UltmineShape112 shape = ClientUltmineConfig.getSelectedShape();
        if (button.id == BACK) {
            ClientUltmineConfig.save();
            mc.displayGuiScreen(parent);
            return;
        }
        int shapeIndex = button.id - SHAPE_BASE;
        if (shapeIndex >= 0 && shapeIndex < UltmineShape112.values().length) {
            ClientUltmineConfig.setSelectedShape(UltmineShape112.values()[shapeIndex]);
        } else if (button.id == DROPS) {
            ClientUltmineConfig.toggleDropsToInventory();
        } else if (button.id == STORAGE) {
            ClientUltmineConfig.toggleDropsToStorage();
        } else if (button.id == XP) {
            ClientUltmineConfig.toggleXpDirectToPlayer();
        } else if (button.id == SAME) {
            ClientUltmineConfig.toggleSameBlockOnly();
        } else if (button.id == MAGNET) {
            ClientUltmineConfig.toggleMagnet();
        } else if (button.id == DEPTH_MINUS || button.id == DEPTH_PLUS) {
            ClientUltmineConfig.setDepth(shape, ClientUltmineConfig.getDepth(shape) + (button.id == DEPTH_PLUS ? 1 : -1));
        } else if (button.id == LENGTH_MINUS || button.id == LENGTH_PLUS) {
            ClientUltmineConfig.setLength(shape, ClientUltmineConfig.getLength(shape) + (button.id == LENGTH_PLUS ? 1 : -1));
        } else if (button.id == VARIANT) {
            ClientUltmineConfig.setVariant(shape, ClientUltmineConfig.getVariant(shape) + 1);
        } else if (button.id == LEGACY_MINUS || button.id == LEGACY_PLUS) {
            ClientUltmineConfig.setLegacyMaxBlocks(ClientUltmineConfig.getLegacyMaxBlocks() + (button.id == LEGACY_PLUS ? 25 : -25));
        } else if (button.id == MAGNET_MINUS || button.id == MAGNET_PLUS) {
            ClientUltmineConfig.setMagnetRange(ClientUltmineConfig.getMagnetRange() + (button.id == MAGNET_PLUS ? 1 : -1));
        } else if (button.id == TRASH_ADD) {
            addFromField(trashField, false);
            return;
        } else if (button.id == CLASSIC_ADD) {
            addFromField(classicField, true);
            return;
        } else if (button.id == TRASH_BROWSE) {
            mc.displayGuiScreen(new TrashItemPickerGui112(this));
            return;
        } else if (button.id == CLASSIC_BROWSE) {
            mc.displayGuiScreen(new UltmineClassicBlockPickerGui112(this));
            return;
        } else if (button.id == STORAGE_WHITELIST) {
            mc.displayGuiScreen(new StorageWhitelistPickerGui112(this));
            return;
        } else if (button.id >= TRASH_REMOVE_BASE && button.id < TRASH_REMOVE_BASE + 100) {
            int index = trashScroll + button.id - TRASH_REMOVE_BASE;
            if (index >= 0 && index < trashItems.size()) {
                ClientUltmineConfig.removeTrashItem(trashItems.get(index));
            }
        } else if (button.id >= CLASSIC_REMOVE_BASE && button.id < CLASSIC_REMOVE_BASE + 100) {
            int index = classicScroll + button.id - CLASSIC_REMOVE_BASE;
            if (index >= 0 && index < classicBlocks.size()) {
                ClientUltmineConfig.removeLegacyBlockedBlock(classicBlocks.get(index));
            }
        }
        ClientUltmineConfig.save();
        initGui();
    }

    private void addFromField(GuiTextField field, boolean classic) {
        if (field == null) {
            return;
        }
        String value = normalizeId(field.getText());
        if (value.length() == 0) {
            return;
        }
        if (classic) {
            ClientUltmineConfig.addLegacyBlockedBlock(value);
        } else {
            ClientUltmineConfig.addTrashItem(value);
        }
        ClientUltmineConfig.save();
        field.setText("");
        initGui();
    }

    @Override
    public void updateScreen() {
        if (trashField != null) {
            trashField.updateCursorCounter();
        }
        if (classicField != null) {
            classicField.updateCursorCounter();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if (trashField != null && trashField.isFocused()) {
                addFromField(trashField, false);
                return;
            }
            if (classicField != null && classicField.isFocused()) {
                addFromField(classicField, true);
                return;
            }
        }
        if (trashField != null && trashField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (classicField != null && classicField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && handleListScrollbarClick(mouseX, mouseY)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (trashField != null) {
            trashField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (classicField != null) {
            classicField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingTrashScrollbar) {
            setListScroll(false, scrollbarScrollFromMouse(mouseY, listTrackY(trashY), listTrackH(trashH),
                    listThumbH(false), maxListScroll(false), trashScrollbarGrabOffset));
            return;
        }
        if (draggingClassicScrollbar) {
            setListScroll(true, scrollbarScrollFromMouse(mouseY, listTrackY(classicY), listTrackH(classicH),
                    listThumbH(true), maxListScroll(true), classicScrollbarGrabOffset));
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingTrashScrollbar = false;
        draggingClassicScrollbar = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (inside(mouseX, mouseY, rightX, trashY, rightW, trashH)) {
            setListScroll(false, trashScroll + (delta > 0 ? -1 : 1));
        } else if (inside(mouseX, mouseY, rightX, classicY, rightW, classicH)) {
            setListScroll(true, classicScroll + (delta > 0 ? -1 : 1));
        }
    }

    private boolean handleListScrollbarClick(int mouseX, int mouseY) {
        if (maxListScroll(false) > 0 && inside(mouseX, mouseY, listTrackX(), listTrackY(trashY), 9, listTrackH(trashH))) {
            draggingTrashScrollbar = true;
            int thumbY = listThumbY(false);
            int thumbH = listThumbH(false);
            trashScrollbarGrabOffset = inside(mouseX, mouseY, listTrackX(), thumbY, 9, thumbH)
                    ? mouseY - thumbY : thumbH / 2;
            setListScroll(false, scrollbarScrollFromMouse(mouseY, listTrackY(trashY), listTrackH(trashH), thumbH,
                    maxListScroll(false), trashScrollbarGrabOffset));
            return true;
        }
        if (maxListScroll(true) > 0 && inside(mouseX, mouseY, listTrackX(), listTrackY(classicY), 9, listTrackH(classicH))) {
            draggingClassicScrollbar = true;
            int thumbY = listThumbY(true);
            int thumbH = listThumbH(true);
            classicScrollbarGrabOffset = inside(mouseX, mouseY, listTrackX(), thumbY, 9, thumbH)
                    ? mouseY - thumbY : thumbH / 2;
            setListScroll(true, scrollbarScrollFromMouse(mouseY, listTrackY(classicY), listTrackH(classicH), thumbH,
                    maxListScroll(true), classicScrollbarGrabOffset));
            return true;
        }
        return false;
    }

    private int maxListScroll(boolean classic) {
        List<String> values = classic ? classicBlocks : trashItems;
        int sectionH = classic ? classicH : trashH;
        return Math.max(0, values.size() - visibleListRows(sectionH));
    }

    private int listTrackX() {
        return rightX + rightW - 11;
    }

    private int listTrackY(int sectionY) {
        return sectionY + 50;
    }

    private int listTrackH(int sectionH) {
        return Math.max(0, sectionH - 62);
    }

    private int listThumbH(boolean classic) {
        int sectionH = classic ? classicH : trashH;
        List<String> values = classic ? classicBlocks : trashItems;
        return scrollbarThumbHeight(listTrackH(sectionH), visibleListRows(sectionH), values.size());
    }

    private int listThumbY(boolean classic) {
        int sectionY = classic ? classicY : trashY;
        int sectionH = classic ? classicH : trashH;
        int scrollValue = classic ? classicScroll : trashScroll;
        return scrollbarThumbY(listTrackY(sectionY), listTrackH(sectionH), listThumbH(classic), scrollValue,
                maxListScroll(classic));
    }

    private void setListScroll(boolean classic, int value) {
        int next = clamp(value, 0, maxListScroll(classic));
        if (classic) {
            if (next != classicScroll) {
                classicScroll = next;
                refreshKeepingText();
            }
        } else if (next != trashScroll) {
            trashScroll = next;
            refreshKeepingText();
        }
    }

    private void refreshKeepingText() {
        String trashText = trashField == null ? "" : trashField.getText();
        String classicText = classicField == null ? "" : classicField.getText();
        initGui();
        if (trashField != null) {
            trashField.setText(trashText);
        }
        if (classicField != null) {
            classicField.setText(classicText);
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
        drawCenteredString(fontRenderer, "Ultmine Config", width / 2, panelY + 13, Palette.TEXT_GOLD);
        UltmineShape112 shape = ClientUltmineConfig.getSelectedShape();
        GuiScreen.drawRect(panelX + 12, panelY + 38, panelX + panelW - 12, panelY + 100, 0x68101018);
        drawPanelBorder(panelX + 12, panelY + 38, panelW - 24, 62, Palette.SECTION_BORDER);
        GuiScreen.drawRect(panelX + 13, panelY + 67, panelX + panelW - 13, panelY + 68, 0x30FFFFFF);
        int sectionTop = panelY + 106;
        int bottom = panelY + panelH - 40;
        int shapeCardY = sectionTop;
        int shapeCardH = 120;
        int classicCardY = shapeCardY + shapeCardH + 10;
        int classicCardH = 64;
        int magnetCardY = classicCardY + classicCardH + 10;
        int magnetCardH = Math.max(72, bottom - magnetCardY);

        drawSection(leftX, shapeCardY, leftW, shapeCardH, "Shape size");
        drawSection(leftX, classicCardY, leftW, classicCardH, "Classic limits");
        drawSection(leftX, magnetCardY, leftW, magnetCardH, "Magnet");
        int rowX = leftX + 12;
        int shapeRowY = shapeCardY + 42;
        int classicRowY = classicCardY + 28;
        int magnetRangeY = magnetCardY + 58;
        drawString(fontRenderer, "Current: " + shapeLabel(shape), rowX, shapeCardY + 22, Palette.TEXT_LIGHT);
        drawString(fontRenderer, "Depth", rowX, shapeRowY + 5, Palette.TEXT_MUTED);
        drawCenteredString(fontRenderer, String.valueOf(ClientUltmineConfig.getDepth(shape)), rowX + 146, shapeRowY + 5, Palette.TEXT_LIGHT);
        drawString(fontRenderer, "Length", rowX, shapeRowY + 33, Palette.TEXT_MUTED);
        drawCenteredString(fontRenderer, String.valueOf(ClientUltmineConfig.getLength(shape)), rowX + 146, shapeRowY + 33, Palette.TEXT_LIGHT);
        drawString(fontRenderer, "Variant", rowX, shapeRowY + 61, Palette.TEXT_MUTED);
        drawString(fontRenderer, "Max blocks", rowX, classicRowY + 5, Palette.TEXT_MUTED);
        drawCenteredString(fontRenderer, String.valueOf(ClientUltmineConfig.getLegacyMaxBlocks()), rowX + 146, classicRowY + 5, Palette.TEXT_LIGHT);
        drawString(fontRenderer, "Range", rowX, magnetRangeY + 5, Palette.TEXT_MUTED);
        drawCenteredString(fontRenderer, String.valueOf(ClientUltmineConfig.getMagnetRange()), rowX + 146, magnetRangeY + 5, Palette.TEXT_LIGHT);

        drawSection(rightX, trashY, rightW, trashH, "Trash items");
        drawString(fontRenderer, trashItems.size() + " configured", rightX + rightW - 88, trashY + 4, Palette.TEXT_MUTED);
        drawString(fontRenderer, "Item id", rightX + 10, trashY + 16, Palette.TEXT_MUTED);
        drawList(false, trashItems, trashY, trashH, trashScroll);
        drawSection(rightX, classicY, rightW, classicH, "Classic block lock");
        drawString(fontRenderer, classicBlocks.size() + " configured", rightX + rightW - 88, classicY + 4, Palette.TEXT_MUTED);
        drawString(fontRenderer, "Block id", rightX + 10, classicY + 16, Palette.TEXT_MUTED);
        drawList(true, classicBlocks, classicY, classicH, classicScroll);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (trashField != null) {
            trashField.drawTextBox();
        }
        if (classicField != null) {
            classicField.drawTextBox();
        }
    }

    private void drawSection(int x, int y, int w, int h, String title) {
        GuiScreen.drawRect(x, y, x + w, y + h, 0xC2141420);
        GuiScreen.drawRect(x + 1, y + 15, x + w - 1, y + h - 1, 0x66101018);
        drawPanelBorder(x, y, w, h, Palette.SECTION_BORDER);
        GuiScreen.drawRect(x, y, x + w, y + 16, 0x90181828);
        GuiScreen.drawRect(x + 1, y + 1, x + 4, y + h - 1, Palette.ACCENT_GOLD);
        GuiScreen.drawRect(x + 8, y + 15, x + w - 8, y + 16, 0x30FFFFFF);
        drawString(fontRenderer, title, x + 8, y + 4, Palette.TEXT_GOLD);
    }

    private void drawList(boolean classic, List<String> values, int sectionY, int sectionH, int offset) {
        int visible = visibleListRows(sectionH);
        int count = Math.min(visible, Math.max(0, values.size() - offset));
        if (values.isEmpty()) {
            String empty = classic ? "No classic block lock entries" : "No trash items yet";
            drawString(fontRenderer, empty, rightX + 10, sectionY + 52, Palette.TEXT_MUTED);
            return;
        }
        for (int i = 0; i < count; i++) {
            String value = values.get(offset + i);
            int y = sectionY + 50 + i * 16;
            GuiScreen.drawRect(rightX + 8, y - 3, rightX + rightW - 40, y + 12,
                    (i % 2 == 0) ? Palette.ALTERNATING_ROW_BG : 0x00000000);
            drawString(fontRenderer, fitForWidth(value, rightW - 60), rightX + 12, y, Palette.TEXT_LIGHT);
        }
        int maxScroll = maxListScroll(classic);
        if (maxScroll > 0) {
            int sectionForY = classic ? classicY : trashY;
            int sectionForH = classic ? classicH : trashH;
            renderScrollbar(listTrackX(), listTrackY(sectionForY), listTrackH(sectionForH), listThumbY(classic),
                    listThumbH(classic));
        }
        if (values.size() > visible) {
            String range = (offset + 1) + "-" + Math.min(values.size(), offset + visible) + " / " + values.size();
            drawString(fontRenderer, range, rightX + 10, sectionY + sectionH - 16, Palette.TEXT_MUTED);
        }
    }

    private String fitForWidth(String text, int maxWidth) {
        String out = text == null ? "" : text;
        while (out.length() > 3 && fontRenderer.getStringWidth(out) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return fontRenderer.getStringWidth(out) > maxWidth ? out : out;
    }

    private String shapeLabel(UltmineShape112 shape) {
        switch (shape) {
            case S_3x3: return "3x3";
            case R_2x1: return "2x1";
            case LINE: return "Line";
            case STAIRS: return "Stairs";
            case SQUARE_20x20_D1: return "20x20";
            case LEGACY: return "Classic";
            default: return shape.name();
        }
    }

    private String variantLabel() {
        UltmineShape112 shape = ClientUltmineConfig.getSelectedShape();
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
        return "Default";
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
