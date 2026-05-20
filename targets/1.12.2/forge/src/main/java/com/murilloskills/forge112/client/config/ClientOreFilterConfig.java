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

public final class ClientOreFilterConfig {
    private static final String FILE = "murilloskills_ore_filter.json";
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

    private static Path clientConfigPath(String file) {
        Path base = CONFIG_DIR == null ? new java.io.File("config").toPath() : CONFIG_DIR;
        return base.resolve(file);
    }

    private static void normalize() {
        if (!root.has("oreSettings") || !root.get("oreSettings").isJsonObject()) {
            root.add("oreSettings", new JsonObject());
        }
        if (!root.has("oreDisplayNames") || !root.get("oreDisplayNames").isJsonObject()) {
            root.add("oreDisplayNames", new JsonObject());
        }
        if (!root.has("maxOres")) {
            root.addProperty("maxOres", 500);
        }
        if (!root.has("prioritizeRare")) {
            root.addProperty("prioritizeRare", true);
        }
        addDefault("COAL", "Coal", 0x333333);
        addDefault("COPPER", "Copper", 0xE87B35);
        addDefault("IRON", "Iron", 0xD8AF93);
        addDefault("GOLD", "Gold", 0xFFD700);
        addDefault("LAPIS", "Lapis Lazuli", 0x2626CC);
        addDefault("REDSTONE", "Redstone", 0xFF0000);
        addDefault("DIAMOND", "Diamond", 0x4AEDD9);
        addDefault("EMERALD", "Emerald", 0x00FF00);
        addDefault("ANCIENT_DEBRIS", "Netherite", 0x7B4F3A);
        addDefault("NETHER_QUARTZ", "Quartz", 0xE8E4D8);
        addDefault("NETHER_GOLD", "Nether Gold", 0xFFD700);
        discoverModdedOres();
    }

    private static void addDefault(String key, String name, int color) {
        JsonObject settings = root.getAsJsonObject("oreSettings");
        if (!settings.has(key) || !settings.get(key).isJsonObject()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("enabled", true);
            obj.addProperty("customColor", color);
            settings.add(key, obj);
        }
        root.getAsJsonObject("oreDisplayNames").addProperty(key, name);
    }

    private static void discoverModdedOres() {
        JsonObject settings = root.getAsJsonObject("oreSettings");
        JsonObject names = root.getAsJsonObject("oreDisplayNames");
        for (ResourceLocation id : Block.REGISTRY.getKeys()) {
            if (id == null) {
                continue;
            }
            String blockId = id.toString().toLowerCase(Locale.ROOT);
            String key = oreKeyForBlockId(blockId);
            if (isVanillaKey(key) || key.length() == 0 || !CrossModCompatRules.isOreResourceId(blockId)) {
                continue;
            }
            if (!settings.has(key)) {
                JsonObject obj = new JsonObject();
                obj.addProperty("enabled", true);
                obj.addProperty("customColor", moddedColor(key));
                settings.add(key, obj);
            }
            if (!names.has(key)) {
                names.addProperty(key, humanName(key));
            }
        }
    }

    public static List<OreOption> getOptions() {
        load();
        List<OreOption> options = new ArrayList<OreOption>();
        String[] vanilla = new String[] { "COAL", "COPPER", "IRON", "GOLD", "LAPIS", "REDSTONE", "DIAMOND",
                "EMERALD", "ANCIENT_DEBRIS", "NETHER_QUARTZ", "NETHER_GOLD" };
        for (String key : vanilla) {
            options.add(new OreOption(key, oreName(key), color(key)));
        }
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("oreSettings").entrySet()) {
            String key = entry.getKey();
            if (!isVanillaKey(key) && !"OTHER".equals(key)) {
                options.add(new OreOption(key, oreName(key), color(key)));
            }
        }
        return options;
    }

    public static boolean isOreEnabled(String key) {
        load();
        JsonObject obj = setting(key);
        return obj == null || !obj.has("enabled") || obj.get("enabled").getAsBoolean();
    }

    public static boolean isOreEnabledForBlockId(String blockId) {
        String key = oreKeyForBlockId(blockId);
        return key.length() == 0 || isOreEnabled(key);
    }

    public static void toggle(String key) {
        setEnabled(key, !isOreEnabled(key));
    }

    public static void setAll(boolean enabled) {
        load();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("oreSettings").entrySet()) {
            if (entry.getValue() != null && entry.getValue().isJsonObject()) {
                entry.getValue().getAsJsonObject().addProperty("enabled", enabled);
            }
        }
    }

    public static int getMaxOres() {
        load();
        return clamp(intFrom(root, "maxOres", 500), 1, 500);
    }

    public static void setMaxOres(int value) {
        load();
        root.addProperty("maxOres", clamp(value, 1, 500));
    }

    public static void resetDefaults() {
        root = new JsonObject();
        normalize();
        save();
    }

    private static void setEnabled(String key, boolean enabled) {
        JsonObject obj = setting(key);
        if (obj != null) {
            obj.addProperty("enabled", enabled);
        }
    }

    private static JsonObject setting(String key) {
        load();
        JsonElement element = root.getAsJsonObject("oreSettings").get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static int color(String key) {
        JsonObject obj = setting(key);
        return obj != null ? intFrom(obj, "customColor", 0xFFFFFF) : 0xFFFFFF;
    }

    private static String oreName(String key) {
        JsonElement element = root.getAsJsonObject("oreDisplayNames").get(key);
        return element == null || element.isJsonNull() ? humanName(key) : element.getAsString();
    }

    private static boolean isVanillaKey(String key) {
        return "COAL".equals(key) || "COPPER".equals(key) || "IRON".equals(key) || "GOLD".equals(key)
                || "LAPIS".equals(key) || "REDSTONE".equals(key) || "DIAMOND".equals(key)
                || "EMERALD".equals(key) || "ANCIENT_DEBRIS".equals(key) || "NETHER_QUARTZ".equals(key)
                || "NETHER_GOLD".equals(key);
    }

    private static String oreKeyForBlockId(String blockId) {
        String id = normalizeId(blockId);
        if (id.length() == 0) return "";
        if (id.contains("coal_ore")) return "COAL";
        if (id.contains("copper_ore")) return "COPPER";
        if (id.contains("iron_ore")) return "IRON";
        if (id.contains("gold_ore") && id.contains("nether")) return "NETHER_GOLD";
        if (id.contains("gold_ore")) return "GOLD";
        if (id.contains("lapis_ore")) return "LAPIS";
        if (id.contains("redstone_ore")) return "REDSTONE";
        if (id.contains("diamond_ore")) return "DIAMOND";
        if (id.contains("emerald_ore")) return "EMERALD";
        if (id.contains("ancient_debris")) return "ANCIENT_DEBRIS";
        if (id.contains("quartz_ore")) return "NETHER_QUARTZ";
        return id.endsWith("_ore") || id.contains(":ore") ? id : "";
    }

    private static int moddedColor(String key) {
        int[] colors = new int[] { 0x00D1FF, 0xFF6B6B, 0xFFD166, 0x8BFF7A, 0xB088FF, 0xFF8AD8, 0x78E08F };
        return colors[Math.abs(key.hashCode()) % colors.length];
    }

    private static String humanName(String key) {
        String value = key == null ? "" : key;
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(colon + 1);
        }
        value = value.replace('_', ' ');
        return value.length() == 0 ? "Ore" : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public static final class OreOption {
        public final String key;
        public final String name;
        public final int color;

        public OreOption(String key, String name, int color) {
            this.key = key;
            this.name = name;
            this.color = color;
        }
    }
}
