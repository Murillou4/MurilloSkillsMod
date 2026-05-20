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

public class UltmineListPickerGui112 extends GuiScreen {
    private static final int BACK = 30000;
    private static final int OPTION_BASE = 30100;
    private final GuiScreen parent;
    private final boolean classicBlocks;
    private final List<String> allIds = new ArrayList<String>();
    private final List<String> ids = new ArrayList<String>();
    private int scroll;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int rowX;
    private int rowY;
    private int rowW;
    private int rowH;
    private int visibleRows;
    private GuiTextField searchField;
    private String searchText = "";
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;
    private int scrollbarX;
    private int scrollbarY;
    private int scrollbarH;
    private int scrollbarThumbY;
    private int scrollbarThumbH;

    public UltmineListPickerGui112(GuiScreen parent, boolean classicBlocks) {
        this.parent = parent;
        this.classicBlocks = classicBlocks;
    }

    @Override
    public void initGui() {
        ClientUltmineConfig.load();
        if (searchField != null) {
            searchText = searchField.getText();
        }
        buttonList.clear();
        allIds.clear();
        ids.clear();
        if (classicBlocks) {
            for (ResourceLocation id : Block.REGISTRY.getKeys()) {
                if (id != null) {
                    Block block = Block.REGISTRY.getObject(id);
                    if (block != null && block != Blocks.AIR) {
                        allIds.add(id.toString());
                    }
                }
            }
        } else {
            for (ResourceLocation id : Item.REGISTRY.getKeys()) {
                if (id != null) {
                    Item item = Item.REGISTRY.getObject(id);
                    if (item != null && item != Items.AIR) {
                        allIds.add(id.toString());
                    }
                }
            }
        }
        Collections.sort(allIds);
        filterPickerIds();
        panelW = Math.min(620, width - 20);
        panelH = Math.min(390, height - 20);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        rowX = panelX + 14;
        rowY = panelY + 86;
        rowW = panelW - 38;
        rowH = 22;
        visibleRows = Math.max(1, (panelH - 128) / (rowH + 3));
        scroll = clamp(scroll, 0, Math.max(0, ids.size() - visibleRows));
        buttonList.add(flatButton(BACK, panelX + panelW - 82, panelY + panelH - 28, 70, 20, "Back"));
        searchField = new GuiTextField(3, fontRenderer, panelX + 14, panelY + 48, panelW - 28, 18);
        searchField.setMaxStringLength(80);
        searchField.setText(searchText == null ? "" : searchText);
        int last = Math.min(ids.size(), scroll + visibleRows);
        for (int i = scroll; i < last; i++) {
            int local = i - scroll;
            buttonList.add(invisibleButton(OPTION_BASE + local, rowX, rowY + local * (rowH + 3), rowW, rowH));
        }
    }

    private void filterPickerIds() {
        String query = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        ids.clear();
        for (String id : allIds) {
            if (query.length() == 0 || id.toLowerCase(Locale.ROOT).contains(query)) {
                ids.add(id);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BACK) {
            mc.displayGuiScreen(parent);
            return;
        }
        int local = button.id - OPTION_BASE;
        if (local >= 0 && local < visibleRows) {
            int index = scroll + local;
            if (index >= 0 && index < ids.size()) {
                String id = ids.get(index);
                if (classicBlocks) {
                    if (ClientUltmineConfig.isLegacyBlockedBlock(id)) {
                        ClientUltmineConfig.removeLegacyBlockedBlock(id);
                    } else {
                        ClientUltmineConfig.addLegacyBlockedBlock(id);
                    }
                } else {
                    if (ClientUltmineConfig.isTrashItem(id)) {
                        ClientUltmineConfig.removeTrashItem(id);
                    } else {
                        ClientUltmineConfig.addTrashItem(id);
                    }
                }
                ClientUltmineConfig.save();
                ModNetwork112.sendUltmineConfigToServer();
                initGui();
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0) {
            setPickerScroll(scroll + (delta > 0 ? -1 : 1));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField != null && searchField.textboxKeyTyped(typedChar, keyCode)) {
            searchText = searchField.getText();
            scroll = 0;
            initGui();
            if (searchField != null) {
                searchField.setFocused(true);
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && maxPickerScroll() > 0 && inside(mouseX, mouseY, scrollbarX - 3, scrollbarY, 11, scrollbarH)) {
            draggingScrollbar = true;
            scrollbarGrabOffset = inside(mouseX, mouseY, scrollbarX - 3, scrollbarThumbY, 11, scrollbarThumbH)
                    ? mouseY - scrollbarThumbY : scrollbarThumbH / 2;
            setPickerScroll(scrollbarScrollFromMouse(mouseY, scrollbarY, scrollbarH, scrollbarThumbH,
                    maxPickerScroll(), scrollbarGrabOffset));
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (searchField != null) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingScrollbar) {
            setPickerScroll(scrollbarScrollFromMouse(mouseY, scrollbarY, scrollbarH, scrollbarThumbH,
                    maxPickerScroll(), scrollbarGrabOffset));
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingScrollbar = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void updateScreen() {
        if (searchField != null) {
            searchField.updateCursorCounter();
        }
    }

    private int maxPickerScroll() {
        return Math.max(0, ids.size() - visibleRows);
    }

    private void setPickerScroll(int value) {
        int next = clamp(value, 0, maxPickerScroll());
        if (next != scroll) {
            scroll = next;
            initGui();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiScreen.drawRect(0, 0, width, height, 0xC0000000);
        GuiScreen.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, Palette.SECTION_BG);
        drawPanelBorder(panelX, panelY, panelW, panelH, Palette.SECTION_BORDER);
        renderCornerAccents(panelX, panelY, panelW, panelH, 8, Palette.ACCENT_GOLD);
        GuiScreen.drawRect(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 32, Palette.PANEL_BG_HEADER);
        String title = classicBlocks ? "Choose Classic Block" : "Choose Trash Item";
        drawCenteredString(fontRenderer, title, width / 2, panelY + 9, Palette.TEXT_GOLD);
        drawString(fontRenderer, ids.size() + " / " + allIds.size() + " entries", panelX + 14, panelY + 38,
                Palette.TEXT_MUTED);
        if (searchField != null) {
            searchField.drawTextBox();
            if (searchField.getText().length() == 0 && !searchField.isFocused()) {
                drawString(fontRenderer, classicBlocks ? "Search blocks..." : "Search items...",
                        panelX + 18, panelY + 53, Palette.TEXT_MUTED);
            }
        }
        int last = Math.min(ids.size(), scroll + visibleRows);
        for (int i = scroll; i < last; i++) {
            int local = i - scroll;
            String id = ids.get(i);
            int y = rowY + local * (rowH + 3);
            boolean selected = classicBlocks ? ClientUltmineConfig.isLegacyBlockedBlock(id) : ClientUltmineConfig.isTrashItem(id);
            boolean hovered = inside(mouseX, mouseY, rowX, y, rowW, rowH);
            GuiScreen.drawRect(rowX + 1, y + 1, rowX + rowW - 1, y + rowH - 1,
                    selected ? Palette.SECTION_BG_ACTIVE : Palette.CARD_BG_SUBTLE);
            drawPanelBorder(rowX, y, rowW, rowH, selected ? Palette.ACCENT_GREEN : hovered ? Palette.ACCENT_GOLD : Palette.SECTION_BORDER);
            renderItemStack(mc, iconForId(id), rowX + 5, y + 3, 0.86F);
            drawString(fontRenderer, fit(id, rowW - 78), rowX + 26, y + 7, selected ? Palette.TEXT_GREEN : Palette.TEXT_LIGHT);
            String state = selected ? "ON" : "ADD";
            drawString(fontRenderer, state, rowX + rowW - fontRenderer.getStringWidth(state) - 8, y + 7,
                    selected ? Palette.TEXT_GREEN : Palette.TEXT_MUTED);
        }
        renderPickerScrollbar();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderPickerScrollbar() {
        int maxScroll = maxPickerScroll();
        if (maxScroll <= 0) {
            return;
        }
        scrollbarX = panelX + panelW - 14;
        scrollbarY = rowY;
        scrollbarH = visibleRows * (rowH + 3) - 3;
        scrollbarThumbH = scrollbarThumbHeight(scrollbarH, visibleRows, ids.size());
        scrollbarThumbY = scrollbarThumbY(scrollbarY, scrollbarH, scrollbarThumbH, scroll, maxScroll);
        renderScrollbar(scrollbarX, scrollbarY, scrollbarH, scrollbarThumbY, scrollbarThumbH);
    }

    private ItemStack iconForId(String id) {
        if (classicBlocks) {
            Block block = Block.getBlockFromName(id);
            if (block != null && block != Blocks.AIR) {
                return new ItemStack(block);
            }
        }
        Item item = Item.getByNameOrId(id);
        return item == null ? new ItemStack(Blocks.STONE) : new ItemStack(item);
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
