package com.murilloskills.forge112.commands;

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

public final class SkillsCommand extends CommandBase {
    @Override
    public String getName() {
        return "murilloskills";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/murilloskills <stats|select|paragon|ability|toggle|addxp|setlevel|reset|guide|selftest>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            say(sender, getUsage(sender));
            return;
        }
        EntityPlayer player = sender instanceof EntityPlayer ? (EntityPlayer) sender : null;
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("guide".equals(sub)) {
            say(sender, "MurilloSkills: select up to " + CONFIG.getMaxSelectedSkills()
                    + " skills; paragon enables level 100; ability uses active paragon.");
            say(sender, "Skills: miner warrior archer farmer fisher blacksmith builder explorer.");
            return;
        }
        if ("selftest".equals(sub)) {
            if (player == null) {
                throw new WrongUsageException("Selftest precisa de player.");
            }
            runSelfTest(player);
            return;
        }
        if (player == null) {
            throw new WrongUsageException("Comando precisa ser executado por um player.");
        }
        PlayerSkillDataCore data = data(player);
        if ("stats".equals(sub)) {
            say(player, statsText(data));
        } else if ("select".equals(sub)) {
            SkillType skill = parseSkill(args.length > 1 ? args[1] : null);
            if (data.setSelectedSkills(Collections.singletonList(skill), CONFIG)) {
                STORE.save(player.getUniqueID());
                say(player, "MurilloSkills: selecionada " + skillName(skill) + ".");
                Forge112Notifications.selection(player, skill);
                Forge112FirstTimeHints.onSelection(player, skill);
                LOG.info("[MurilloSkills][1.12.2][Selection] {} selected {}", player.getName(), skill);
            } else {
                say(player, "MurilloSkills: limite de " + CONFIG.getMaxSelectedSkills() + " skills ou skill invalida.");
            }
        } else if ("paragon".equals(sub)) {
            SkillType skill = parseSkill(args.length > 1 ? args[1] : null);
            if (data.activateParagonSkill(skill)) {
                data.getSkill(skill).setLevel(Math.max(data.getSkill(skill).getLevel(), 100));
                STORE.save(player.getUniqueID());
                say(player, "MurilloSkills: paragon ativo em " + skillName(skill) + ".");
                Forge112Notifications.paragon(player, skill);
                LOG.info("[MurilloSkills][1.12.2][Paragon] {} activated {}", player.getName(), skill);
            } else {
                say(player, "MurilloSkills: nao foi possivel ativar paragon.");
            }
        } else if ("ability".equals(sub)) {
            SkillType skill = args.length > 1 ? parseSkill(args[1]) : null;
            triggerAbility(player, skill);
        } else if ("toggle".equals(sub)) {
            if (args.length < 3) {
                throw new WrongUsageException("/murilloskills toggle <skill> <name>");
            }
            toggle(player, parseSkill(args[1]), args[2].toLowerCase(Locale.ROOT), false);
        } else if ("clientstate".equals(sub)) {
            if (args.length < 3) {
                throw new WrongUsageException("/murilloskills clientstate <name> <true|false>");
            }
            setClientState(player, args[1].toLowerCase(Locale.ROOT), parseBoolean(args[2]));
        } else if ("addxp".equals(sub) || "setlevel".equals(sub)) {
            if (!sender.canUseCommand(2, getName())) {
                throw new WrongUsageException("Precisa de permissao para " + sub + ".");
            }
            SkillType skill = parseSkill(args.length > 1 ? args[1] : null);
            int value = args.length > 2 ? parseInt(args[2]) : 0;
            if ("addxp".equals(sub)) {
                XpAddResult result = data.addXpToSkill(skill, value, CONFIG);
                say(player, "MurilloSkills: addxp " + skillName(skill) + " result level=" + result.getNewLevel());
                Forge112Notifications.xp(player, skill, value, "command");
                if (result.isLeveledUp()) {
                    Forge112Notifications.levelUp(player, skill, result.getOldLevel(), result.getNewLevel());
                    Forge112AchievementTracker.levelUp(player, skill, result.getOldLevel(), result.getNewLevel());
                }
            } else {
                data.getSkill(skill).setLevel(Math.max(0, Math.min(100, value)));
                say(player, "MurilloSkills: " + skillName(skill) + " level=" + data.getSkill(skill).getLevel());
                Forge112Notifications.notice(player, "Level set", skillName(skill),
                        "Level " + data.getSkill(skill).getLevel());
            }
            STORE.save(player.getUniqueID());
        } else if ("reset".equals(sub)) {
            STORE.cache.put(player.getUniqueID(), new PlayerSkillDataCore());
            STORE.save(player.getUniqueID());
            say(player, "MurilloSkills: resetado.");
            Forge112Notifications.notice(player, "MurilloSkills", "Reset", "Skill data cleared");
        } else {
            say(sender, getUsage(sender));
        }
    }

    private void setClientState(EntityPlayer player, String name, boolean enabled) {
        if ("ultmine_hold".equals(name)) {
            setUltmineHeld(player, enabled);
            return;
        }
        data(player).setToggle(SkillType.MINER, name, enabled);
    }

    private String statsText(PlayerSkillDataCore data) {
        StringBuilder out = new StringBuilder("MurilloSkills selected=").append(data.getSelectedSkills())
                .append(" paragon=").append(data.getActiveParagonSkill());
        for (SkillType skill : SkillType.values()) {
            SkillStatsCore stats = data.getSkill(skill);
            out.append(" | ").append(skillName(skill)).append(" L").append(stats.getLevel()).append(" P").append(stats.getPrestige());
        }
        return out.toString();
    }
}
