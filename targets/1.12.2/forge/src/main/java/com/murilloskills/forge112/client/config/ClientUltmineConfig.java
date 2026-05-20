package com.murilloskills.forge112.client.config;

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

public final class ClientUltmineConfig {
    private static final String FILE = "murilloskills_ultmine.json";
    private static JsonObject root;

    public static void load() {
        if (root != null) {
            return;
        }
        Path path = clientConfigPath(FILE);
        try {
            if (Files.isRegularFile(path)) {
                JsonElement parsed = new JsonParser().parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
                root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            } else {
                root = new JsonObject();
            }
        } catch (Exception e) {
            root = new JsonObject();
        }
        normalize();
        save();
    }

    public static void save() {
        load();
        try {
            Path path = clientConfigPath(FILE);
            Files.createDirectories(path.getParent());
            Files.write(path, GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }

    private static void normalize() {
        defaultBool("dropsToInventory", true);
        defaultBool("dropsToStorage", false);
        defaultBool("sameBlockOnly", false);
        defaultBool("xpDirectToPlayer", false);
        defaultBool("magnetEnabled", false);
        defaultInt("magnetRange", 8);
        defaultInt("legacyMaxBlocks", 1500);
        if (!root.has("selectedShape")) {
            root.addProperty("selectedShape", UltmineShape112.S_3x3.name());
        }
        if (!root.has("shapePrefs") || !root.get("shapePrefs").isJsonObject()) {
            root.add("shapePrefs", new JsonObject());
        }
        for (UltmineShape112 shape : UltmineShape112.values()) {
            shapePrefs(shape);
        }
        if (!root.has("trashItems") || !root.get("trashItems").isJsonArray()) {
            root.add("trashItems", new com.google.gson.JsonArray());
        }
        if (!root.has("legacyBlockedBlocks") || !root.get("legacyBlockedBlocks").isJsonArray()) {
            root.add("legacyBlockedBlocks", new com.google.gson.JsonArray());
        }
        if (!root.has("storageWhitelist") || !root.get("storageWhitelist").isJsonArray()) {
            root.add("storageWhitelist", new com.google.gson.JsonArray());
        }
    }

    private static Path clientConfigPath(String file) {
        Path base = CONFIG_DIR == null ? new java.io.File("config").toPath() : CONFIG_DIR;
        return base.resolve(file);
    }

    private static JsonObject shapePrefs(UltmineShape112 shape) {
        JsonObject prefs = root.getAsJsonObject("shapePrefs");
        JsonElement existing = prefs.get(shape.name());
        JsonObject obj = existing != null && existing.isJsonObject() ? existing.getAsJsonObject() : new JsonObject();
        if (!obj.has("depth")) obj.addProperty("depth", -1);
        if (!obj.has("length")) obj.addProperty("length", -1);
        if (!obj.has("variant")) obj.addProperty("variant", 0);
        prefs.add(shape.name(), obj);
        return obj;
    }

    public static UltmineShape112 getSelectedShape() {
        load();
        try {
            return UltmineShape112.valueOf(root.get("selectedShape").getAsString());
        } catch (Exception e) {
            return UltmineShape112.S_3x3;
        }
    }

    public static void setSelectedShape(UltmineShape112 shape) {
        load();
        root.addProperty("selectedShape", (shape == null ? UltmineShape112.S_3x3 : shape).name());
    }

    public static int getDepth(UltmineShape112 shape) {
        int configured = intFrom(shapePrefs(shape), "depth", -1);
        return clamp(configured <= 0 ? defaultDepth(shape) : configured, 1, maxDepth(shape));
    }

    public static void setDepth(UltmineShape112 shape, int value) {
        shapePrefs(shape).addProperty("depth", clamp(value, 1, maxDepth(shape)));
    }

    public static int getLength(UltmineShape112 shape) {
        int configured = intFrom(shapePrefs(shape), "length", -1);
        return clamp(configured <= 0 ? defaultLength(shape) : configured, 1, maxLength(shape));
    }

    public static void setLength(UltmineShape112 shape, int value) {
        shapePrefs(shape).addProperty("length", clamp(value, 1, maxLength(shape)));
    }

    public static int getVariant(UltmineShape112 shape) {
        return clamp(intFrom(shapePrefs(shape), "variant", 0), 0, Math.max(0, variantCount(shape) - 1));
    }

    public static void setVariant(UltmineShape112 shape, int value) {
        shapePrefs(shape).addProperty("variant", value >= variantCount(shape) ? 0 : Math.max(0, value));
    }

    public static boolean isDropsToInventory() { load(); return bool("dropsToInventory", true); }
    public static boolean isDropsToStorage() { load(); return bool("dropsToStorage", false); }
    public static boolean isXpDirectToPlayer() { load(); return bool("xpDirectToPlayer", false); }
    public static boolean isSameBlockOnly() { load(); return bool("sameBlockOnly", false); }
    public static boolean isMagnetEnabled() { load(); return bool("magnetEnabled", false); }
    public static void toggleDropsToInventory() { toggle("dropsToInventory", true); }
    public static void toggleDropsToStorage() { toggle("dropsToStorage", false); }
    public static void toggleXpDirectToPlayer() { toggle("xpDirectToPlayer", false); }
    public static void toggleSameBlockOnly() { toggle("sameBlockOnly", false); }
    public static void toggleMagnet() { toggle("magnetEnabled", false); }
    public static void setDropsToInventory(boolean value) { setBool("dropsToInventory", value); }
    public static void setDropsToStorage(boolean value) { setBool("dropsToStorage", value); }
    public static void setXpDirectToPlayer(boolean value) { setBool("xpDirectToPlayer", value); }
    public static void setSameBlockOnly(boolean value) { setBool("sameBlockOnly", value); }
    public static void setMagnetEnabled(boolean value) { setBool("magnetEnabled", value); }

    public static String toNetworkJson() {
        load();
        return GSON.toJson(root);
    }

    public static void resetDefaults() {
        root = new JsonObject();
        normalize();
        save();
    }

    public static int getLegacyMaxBlocks() {
        load();
        return clamp(intFrom(root, "legacyMaxBlocks", 1500), 1, 4096);
    }

    public static void setLegacyMaxBlocks(int value) {
        load();
        root.addProperty("legacyMaxBlocks", clamp(value, 1, 4096));
    }

    public static int getMagnetRange() {
        load();
        return clamp(intFrom(root, "magnetRange", 8), 1, 32);
    }

    public static void setMagnetRange(int value) {
        load();
        root.addProperty("magnetRange", clamp(value, 1, 32));
    }

    public static boolean isTrashItem(String itemId) {
        load();
        return arrayContains("trashItems", normalizeId(itemId));
    }

    public static boolean isLegacyBlockedBlock(String blockId) {
        load();
        return arrayContains("legacyBlockedBlocks", normalizeId(blockId));
    }

    public static int trashCount() {
        load();
        return root.getAsJsonArray("trashItems").size();
    }

    public static int legacyBlockCount() {
        load();
        return root.getAsJsonArray("legacyBlockedBlocks").size();
    }

    public static boolean isStorageWhitelisted(String itemId) {
        load();
        return arrayContains("storageWhitelist", normalizeId(itemId));
    }

    public static List<String> getTrashItems() {
        return stringList("trashItems");
    }

    public static List<String> getLegacyBlockedBlocks() {
        return stringList("legacyBlockedBlocks");
    }

    public static List<String> getStorageWhitelist() {
        return stringList("storageWhitelist");
    }

    public static boolean addTrashItem(String itemId) {
        return addToArray("trashItems", itemId);
    }

    public static boolean removeTrashItem(String itemId) {
        return removeFromArray("trashItems", itemId);
    }

    public static boolean addLegacyBlockedBlock(String blockId) {
        return addToArray("legacyBlockedBlocks", blockId);
    }

    public static boolean removeLegacyBlockedBlock(String blockId) {
        return removeFromArray("legacyBlockedBlocks", blockId);
    }

    public static boolean addStorageWhitelistItem(String itemId) {
        return addToArray("storageWhitelist", itemId);
    }

    public static boolean removeStorageWhitelistItem(String itemId) {
        return removeFromArray("storageWhitelist", itemId);
    }

    private static List<String> stringList(String name) {
        load();
        List<String> values = new ArrayList<String>();
        if (!root.has(name) || !root.get(name).isJsonArray()) {
            return values;
        }
        for (JsonElement element : root.getAsJsonArray(name)) {
            if (element != null && !element.isJsonNull()) {
                String value = normalizeId(element.getAsString());
                if (value.length() > 0 && !values.contains(value)) {
                    values.add(value);
                }
            }
        }
        Collections.sort(values);
        return values;
    }

    private static boolean addToArray(String name, String rawValue) {
        load();
        String value = normalizeId(rawValue);
        if (value.length() == 0 || arrayContains(name, value)) {
            return false;
        }
        root.getAsJsonArray(name).add(value);
        return true;
    }

    private static boolean removeFromArray(String name, String rawValue) {
        load();
        String value = normalizeId(rawValue);
        if (value.length() == 0 || !root.has(name) || !root.get(name).isJsonArray()) {
            return false;
        }
        com.google.gson.JsonArray next = new com.google.gson.JsonArray();
        boolean removed = false;
        for (JsonElement element : root.getAsJsonArray(name)) {
            if (element == null || element.isJsonNull()) {
                continue;
            }
            String current = normalizeId(element.getAsString());
            if (value.equals(current)) {
                removed = true;
                continue;
            }
            if (current.length() > 0 && !arrayContainsIn(next, current)) {
                next.add(current);
            }
        }
        root.add(name, next);
        return removed;
    }

    private static boolean arrayContainsIn(com.google.gson.JsonArray array, String value) {
        for (JsonElement element : array) {
            if (element != null && !element.isJsonNull() && value.equals(normalizeId(element.getAsString()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean arrayContains(String name, String value) {
        if (value.length() == 0 || !root.has(name) || !root.get(name).isJsonArray()) {
            return false;
        }
        for (JsonElement element : root.getAsJsonArray(name)) {
            if (element != null && !element.isJsonNull() && value.equals(normalizeId(element.getAsString()))) {
                return true;
            }
        }
        return false;
    }

    private static int defaultDepth(UltmineShape112 shape) {
        if (shape == UltmineShape112.STAIRS) return 16;
        return 1;
    }

    private static int maxDepth(UltmineShape112 shape) {
        if (shape == UltmineShape112.LINE || shape == UltmineShape112.LEGACY) return 1;
        if (shape == UltmineShape112.STAIRS) return 64;
        if (shape == UltmineShape112.SQUARE_20x20_D1) return 4;
        return 16;
    }

    private static int defaultLength(UltmineShape112 shape) {
        if (shape == UltmineShape112.LINE) return 12;
        if (shape == UltmineShape112.SQUARE_20x20_D1) return 20;
        if (shape == UltmineShape112.R_2x1) return 2;
        if (shape == UltmineShape112.S_3x3) return 3;
        return 1;
    }

    private static int maxLength(UltmineShape112 shape) {
        if (shape == UltmineShape112.LINE) return 128;
        if (shape == UltmineShape112.SQUARE_20x20_D1) return 20;
        if (shape == UltmineShape112.R_2x1) return 2;
        if (shape == UltmineShape112.S_3x3) return 3;
        return 1;
    }

    private static int variantCount(UltmineShape112 shape) {
        if (shape == UltmineShape112.STAIRS || shape == UltmineShape112.R_2x1 || shape == UltmineShape112.LEGACY) return 2;
        if (shape == UltmineShape112.SQUARE_20x20_D1) return 3;
        return 1;
    }

    private static void defaultBool(String name, boolean value) {
        if (!root.has(name)) root.addProperty(name, value);
    }

    private static void defaultInt(String name, int value) {
        if (!root.has(name)) root.addProperty(name, value);
    }

    private static boolean bool(String name, boolean fallback) {
        try {
            return root.get(name).getAsBoolean();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void toggle(String name, boolean fallback) {
        load();
        root.addProperty(name, !bool(name, fallback));
    }

    private static void setBool(String name, boolean value) {
        load();
        root.addProperty(name, value);
    }
}
