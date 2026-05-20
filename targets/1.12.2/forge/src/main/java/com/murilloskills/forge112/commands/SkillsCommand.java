package com.murilloskills.forge112.commands;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.core.data.XpAddResult;
import com.murilloskills.forge112.api.SkillRegistry;
import com.murilloskills.forge112.utils.Forge112AchievementTracker;
import com.murilloskills.forge112.utils.Forge112FirstTimeHints;
import com.murilloskills.forge112.utils.Forge112Notifications;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static com.murilloskills.forge112.MurilloSkillsForge112.CONFIG;
import static com.murilloskills.forge112.MurilloSkillsForge112.LOG;
import static com.murilloskills.forge112.MurilloSkillsForge112.STORE;
import static com.murilloskills.forge112.dev.Forge112SelfTest.runSelfTest;
import static com.murilloskills.forge112.skills.Forge112Abilities.triggerAbility;
import static com.murilloskills.forge112.utils.Forge112MiningTools.setUltmineHeld;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.data;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.parseBoolean;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.say;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.skillName;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.toggle;

public final class SkillsCommand extends CommandBase {
    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList(
            "setlevel", "setprestige", "addxp", "info", "reset", "resetall",
            "select", "deselect", "setparagon", "clearparagon", "maxall");
    private static final List<String> LEGACY_SUBCOMMANDS = Arrays.asList(
            "stats", "paragon", "prestige", "ability", "toggle", "clientstate", "guide", "selftest");

    @Override
    public String getName() {
        return "skill";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("murilloskills", "skills", "mskills", "ms");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/skill <setlevel|setprestige|addxp|info|reset|resetall|select|deselect|setparagon|clearparagon|maxall>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            showUsage(sender);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (isOriginalSyntax(sub, args)) {
            requireAdmin(sender);
            executeOriginalCommand(server, sender, sub, args);
            return;
        }

        executeLegacyLocalCommand(server, sender, sub, args);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            BlockPos targetPos) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<String>(ADMIN_SUBCOMMANDS);
            subcommands.addAll(LEGACY_SUBCOMMANDS);
            return getListOfStringsMatchingLastWord(args, subcommands);
        }
        if (args.length == 2 && ADMIN_SUBCOMMANDS.contains(args[0].toLowerCase(Locale.ROOT))) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 3 && expectsSkillAt(args[0].toLowerCase(Locale.ROOT), 2)) {
            return getListOfStringsMatchingLastWord(args, skillNamesArray());
        }
        if (args.length == 2 && expectsLegacySkill(args[0].toLowerCase(Locale.ROOT))) {
            return getListOfStringsMatchingLastWord(args, skillNamesArray());
        }
        if (args.length == 3 && "toggle".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, commonToggles(args[1]));
        }
        return Collections.emptyList();
    }

    private boolean isOriginalSyntax(String sub, String[] args) {
        if ("setlevel".equals(sub) || "setprestige".equals(sub) || "addxp".equals(sub)) {
            return args.length >= 4;
        }
        if ("select".equals(sub) || "deselect".equals(sub) || "setparagon".equals(sub) || "reset".equals(sub)) {
            return args.length >= 3;
        }
        return "info".equals(sub) || "resetall".equals(sub) || "clearparagon".equals(sub) || "maxall".equals(sub);
    }

    private void executeOriginalCommand(MinecraftServer server, ICommandSender sender, String sub, String[] args)
            throws CommandException {
        if ("setlevel".equals(sub)) {
            requireArgs(args, 4, "/skill setlevel <player> <skill> <level>");
            executeSetLevel(server, sender, args[1], args[2], parseInt(args[3], 0, CONFIG.getMaxLevel()));
        } else if ("setprestige".equals(sub)) {
            requireArgs(args, 4, "/skill setprestige <player> <skill> <prestige>");
            executeSetPrestige(server, sender, args[1], args[2], parseInt(args[3], 0, 100));
        } else if ("addxp".equals(sub)) {
            requireArgs(args, 4, "/skill addxp <player> <skill> <amount>");
            executeAddXp(server, sender, args[1], args[2], parseInt(args[3], 1, 1000000));
        } else if ("info".equals(sub)) {
            requireArgs(args, 2, "/skill info <player> [skill]");
            executeInfo(server, sender, args[1], args.length >= 3 ? args[2] : null);
        } else if ("reset".equals(sub)) {
            requireArgs(args, 3, "/skill reset <player> <skill>");
            executeReset(server, sender, args[1], args[2]);
        } else if ("resetall".equals(sub)) {
            requireArgs(args, 2, "/skill resetall <player>");
            executeResetAll(server, sender, args[1]);
        } else if ("select".equals(sub)) {
            requireArgs(args, 3, "/skill select <player> <skill>");
            executeSelect(server, sender, args[1], args[2]);
        } else if ("deselect".equals(sub)) {
            requireArgs(args, 3, "/skill deselect <player> <skill>");
            executeDeselect(server, sender, args[1], args[2]);
        } else if ("setparagon".equals(sub)) {
            requireArgs(args, 3, "/skill setparagon <player> <skill>");
            executeSetParagon(server, sender, args[1], args[2]);
        } else if ("clearparagon".equals(sub)) {
            requireArgs(args, 2, "/skill clearparagon <player>");
            executeClearParagon(server, sender, args[1]);
        } else if ("maxall".equals(sub)) {
            requireArgs(args, 2, "/skill maxall <player>");
            executeMaxAll(server, sender, args[1]);
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    private void executeSetLevel(MinecraftServer server, ICommandSender sender, String target, String skillName,
            int level) throws CommandException {
        EntityPlayerMP player = target(server, sender, target);
        SkillType skill = parseSkillName(skillName);
        PlayerSkillDataCore data = data(player);
        SkillStatsCore stats = data.getSkill(skill);
        stats.setLevel(level);
        stats.setXp(0.0D);
        persistAndRefresh(player, data);
        reply(sender, "Set " + skill.name() + " level to " + level + " for " + player.getName());
        Forge112Notifications.notice(player, "Level set", skillName(skill), "Level " + level);
        LOG.info("[MurilloSkills][1.12.2][Command] {} set {} level={} for {}",
                sender.getName(), skill, level, player.getUniqueID());
    }

    private void executeSetPrestige(MinecraftServer server, ICommandSender sender, String target, String skillName,
            int prestige) throws CommandException {
        EntityPlayerMP player = target(server, sender, target);
        SkillType skill = parseSkillName(skillName);
        PlayerSkillDataCore data = data(player);
        data.getSkill(skill).setPrestige(prestige);
        persistAndRefresh(player, data);
        reply(sender, "Set " + skill.name() + " prestige to " + prestige + " for " + player.getName());
        Forge112Notifications.notice(player, "Prestige set", skillName(skill), "Prestige " + prestige);
        LOG.info("[MurilloSkills][1.12.2][Command] {} set {} prestige={} for {}",
                sender.getName(), skill, prestige, player.getUniqueID());
    }

    private void executeAddXp(MinecraftServer server, ICommandSender sender, String target, String skillName,
            int amount) throws CommandException {
        EntityPlayerMP player = target(server, sender, target);
        SkillType skill = parseSkillName(skillName);
        PlayerSkillDataCore data = data(player);
        SkillStatsCore stats = data.getSkill(skill);
        XpAddResult result = stats.addXp(amount, CONFIG.getMaxLevel(), CONFIG);
        persistAndRefresh(player, data);
        reply(sender, "Added " + amount + " XP to " + skill.name() + " for " + player.getName()
                + " (now level " + stats.getLevel() + ")");
        Forge112Notifications.xp(player, skill, amount, "command");
        if (result.isLeveledUp()) {
            Forge112Notifications.levelUp(player, skill, result.getOldLevel(), result.getNewLevel());
            Forge112AchievementTracker.levelUp(player, skill, result.getOldLevel(), result.getNewLevel());
        }
        LOG.info("[MurilloSkills][1.12.2][Command] {} added {} XP to {} for {}",
                sender.getName(), amount, skill, player.getUniqueID());
    }

    private void executeInfo(MinecraftServer server, ICommandSender sender, String target, String skillName)
            throws CommandException {
        EntityPlayerMP player = target(server, sender, target);
        PlayerSkillDataCore data = data(player);
        data.normalizeParagonState();
        if (skillName != null) {
            SkillType skill = parseSkillName(skillName);
            SkillStatsCore stats = data.getSkill(skill);
            reply(sender, "=== " + skill.name() + " for " + player.getName() + " ===");
            reply(sender, "Level: " + stats.getLevel() + " | XP: " + (int) stats.getXp());
            reply(sender, "Prestige: " + stats.getPrestige());
            reply(sender, "Class: " + skill.getSkillClass().name());
            reply(sender, "Selected: " + data.isSkillSelected(skill));
            reply(sender, "Paragon: " + data.isParagonSkill(skill));
            return;
        }
        reply(sender, "=== Skills for " + player.getName() + " ===");
        reply(sender, "Active Paragon: " + (data.getActiveParagonSkill() == null
                ? "None" : data.getActiveParagonSkill().name()));
        Set<SkillType> paragons = data.getParagonSkills();
        reply(sender, "Paragons: " + (paragons.isEmpty() ? "None" : paragons.toString()));
        reply(sender, "Selected: " + data.getSelectedSkills());
        for (SkillType skill : SkillType.values()) {
            SkillStatsCore stats = data.getSkill(skill);
            String marker = data.isParagonSkill(skill) ? "[P] " : (data.isSkillSelected(skill) ? "[S] " : "    ");
            reply(sender, marker + skill.name() + " [" + skill.getSkillClass().name() + "]: Lv"
                    + stats.getLevel() + " P" + stats.getPrestige());
        }
    }

    private void executeReset(MinecraftServer server, ICommandSender sender, String target, String skillName)
            throws CommandException {
        EntityPlayerMP player = target(server, sender, target);
        SkillType skill = parseSkillName(skillName);
        PlayerSkillDataCore data = data(player);
        data.setSkill(skill, 0, 0.0D, -1L, 0);
        data.clearParagonSkill(skill);
        persistAndRefresh(player, data);
        reply(sender, "Reset " + skill.name() + " for " + player.getName());
        Forge112Notifications.notice(player, "Skill reset", skillName(skill), "Level 0");
        LOG.info("[MurilloSkills][1.12.2][Command] {} reset {} for {}",
                sender.getName(), skill, player.getUniqueID());
    }

    private void executeResetAll(MinecraftServer server, ICommandSender sender, String target)
            throws CommandException {
        EntityPlayerMP player = target(server, sender, target);
        PlayerSkillDataCore data = data(player);
        for (SkillType skill : SkillType.values()) {
            data.setSkill(skill, 0, 0.0D, -1L, 0);
        }
        data.clearAllParagonSkills();
        data.setSelectedSkillsDirect(Collections.<SkillType>emptyList());
        persistAndRefresh(player, data);
        reply(sender, "Reset ALL skills for " + player.getName());
        Forge112Notifications.notice(player, "MurilloSkills", "Reset all", "Skill data cleared");
        LOG.info("[MurilloSkills][1.12.2][Command] {} reset all skills for {}",
                sender.getName(), player.getUniqueID());
    }

    private void executeSelect(MinecraftServer server, ICommandSender sender, String target, String skillName)
            throws CommandException {
        EntityPlayerMP player = target(server, sender, target);
        SkillType skill = parseSkillName(skillName);
        PlayerSkillDataCore data = data(player);
        if (data.isSkillSelected(skill)) {
            throw new CommandException("Skill %s is already selected for %s", skill.name(), player.getName());
        }
        if (!data.setSelectedSkills(Collections.singletonList(skill), CONFIG)) {
            throw new CommandException("Player %s already has %s skills selected. Use /skill deselect first.",
                    player.getName(), CONFIG.getMaxSelectedSkills());
        }
        persistAndRefresh(player, data);
        reply(sender, "Added " + skill.name() + " to selected skills for " + player.getName());
        Forge112Notifications.selection(player, skill);
        Forge112FirstTimeHints.onSelection(player, skill);
        LOG.info("[MurilloSkills][1.12.2][Command] {} selected {} for {}",
                sender.getName(), skill, player.getUniqueID());
    }

    private void executeDeselect(MinecraftServer server, ICommandSender sender, String target, String skillName)
            throws CommandException {
        EntityPlayerMP player = target(server, sender, target);
        SkillType skill = parseSkillName(skillName);
        PlayerSkillDataCore data = data(player);
        if (!data.isSkillSelected(skill)) {
            throw new CommandException("Skill %s is not selected for %s", skill.name(), player.getName());
        }
        List<SkillType> selected = new ArrayList<SkillType>(data.getSelectedSkills());
        selected.remove(skill);
        data.setSelectedSkillsDirect(selected);
        data.clearParagonSkill(skill);
        persistAndRefresh(player, data);
        reply(sender, "Removed " + skill.name() + " from selected skills for " + player.getName());
        LOG.info("[MurilloSkills][1.12.2][Command] {} deselected {} for {}",
                sender.getName(), skill, player.getUniqueID());
    }

    private void executeSetParagon(MinecraftServer server, ICommandSender sender, String target, String skillName)
            throws CommandException {
        EntityPlayerMP player = target(server, sender, target);
        SkillType skill = parseSkillName(skillName);
        PlayerSkillDataCore data = data(player);
        data.normalizeParagonState();
        if (data.isParagonSkill(skill)) {
            reply(sender, skill.name() + " is already Paragon for " + player.getName());
            return;
        }
        if (!data.canActivateParagonSkill(skill)) {
            throw new CommandException(skill.isMasterClass()
                    ? "Player already has a Master Paragon. Clear paragon first to choose a different Master."
                    : "Cannot set %s as Paragon.", skill.name());
        }
        if (!data.isSkillSelected(skill) && !data.setSelectedSkills(Collections.singletonList(skill), CONFIG)) {
            throw new CommandException("Player has %s skills selected and %s is not one of them.",
                    CONFIG.getMaxSelectedSkills(), skill.name());
        }
        if (!data.activateParagonSkill(skill)) {
            throw new CommandException("Cannot set %s as Paragon.", skill.name());
        }
        persistAndRefresh(player, data);
        reply(sender, "Added paragon skill " + skill.name() + " for " + player.getName());
        Forge112Notifications.paragon(player, skill);
        LOG.info("[MurilloSkills][1.12.2][Command] {} set paragon {} for {}",
                sender.getName(), skill, player.getUniqueID());
    }

    private void executeClearParagon(MinecraftServer server, ICommandSender sender, String target)
            throws CommandException {
        EntityPlayerMP player = target(server, sender, target);
        PlayerSkillDataCore data = data(player);
        Set<SkillType> oldParagons = data.getParagonSkills();
        if (oldParagons.isEmpty()) {
            throw new CommandException("Player %s does not have any paragon skill set", player.getName());
        }
        data.clearAllParagonSkills();
        persistAndRefresh(player, data);
        reply(sender, "Cleared paragon skills (" + oldParagons + ") for " + player.getName());
        Forge112Notifications.notice(player, "Paragon", "Cleared", oldParagons.toString());
        LOG.info("[MurilloSkills][1.12.2][Command] {} cleared paragon for {}",
                sender.getName(), player.getUniqueID());
    }

    private void executeMaxAll(MinecraftServer server, ICommandSender sender, String target) throws CommandException {
        EntityPlayerMP player = target(server, sender, target);
        PlayerSkillDataCore data = data(player);
        for (SkillType skill : SkillType.values()) {
            data.setSkill(skill, CONFIG.getMaxLevel(), 0.0D, data.getSkill(skill).getLastAbilityUse(),
                    data.getSkill(skill).getPrestige());
        }
        persistAndRefresh(player, data);
        reply(sender, "Maxed ALL skills (level " + CONFIG.getMaxLevel() + ") for " + player.getName());
        Forge112Notifications.notice(player, "Max all", player.getName(), "All skills level " + CONFIG.getMaxLevel());
        LOG.info("[MurilloSkills][1.12.2][Command] {} maxed all skills for {}",
                sender.getName(), player.getUniqueID());
    }

    private void executeLegacyLocalCommand(MinecraftServer server, ICommandSender sender, String sub, String[] args)
            throws CommandException {
        if ("guide".equals(sub)) {
            showGuide(sender);
            return;
        }
        EntityPlayer player = sender instanceof EntityPlayer ? (EntityPlayer) sender : null;
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
            requireArgs(args, 2, "/murilloskills select <skill>");
            SkillType skill = parseSkillName(args[1]);
            if (data.setSelectedSkills(Collections.singletonList(skill), CONFIG)) {
                STORE.save(player.getUniqueID());
                say(player, "MurilloSkills: selecionada " + skillName(skill) + ".");
                Forge112Notifications.selection(player, skill);
                Forge112FirstTimeHints.onSelection(player, skill);
            } else {
                say(player, "MurilloSkills: limite de " + CONFIG.getMaxSelectedSkills() + " skills ou skill invalida.");
            }
        } else if ("paragon".equals(sub)) {
            requireArgs(args, 2, "/murilloskills paragon <skill>");
            SkillType skill = parseSkillName(args[1]);
            if (data.activateParagonSkill(skill)) {
                data.getSkill(skill).setLevel(Math.max(data.getSkill(skill).getLevel(), CONFIG.getMaxLevel()));
                persistAndRefresh(player, data);
                say(player, "MurilloSkills: paragon ativo em " + skillName(skill) + ".");
                Forge112Notifications.paragon(player, skill);
            } else {
                say(player, "MurilloSkills: nao foi possivel ativar paragon.");
            }
        } else if ("prestige".equals(sub)) {
            requireArgs(args, 2, "/murilloskills prestige <skill>");
            executePrestigeSelf((EntityPlayerMP) player, parseSkillName(args[1]));
        } else if ("ability".equals(sub)) {
            SkillType skill = args.length > 1 ? parseSkillName(args[1]) : null;
            triggerAbility(player, skill);
        } else if ("toggle".equals(sub)) {
            requireArgs(args, 3, "/murilloskills toggle <skill> <name>");
            toggle(player, parseSkillName(args[1]), args[2].toLowerCase(Locale.ROOT), false);
        } else if ("clientstate".equals(sub)) {
            requireArgs(args, 3, "/murilloskills clientstate <name> <true|false>");
            setClientState(player, args[1].toLowerCase(Locale.ROOT), parseBoolean(args[2]));
        } else if ("setlevel".equals(sub)) {
            requireAdmin(sender);
            requireArgs(args, 3, "/murilloskills setlevel <skill> <level>");
            executeSetLevel(server, sender, player.getName(), args[1], parseInt(args[2], 0, CONFIG.getMaxLevel()));
        } else if ("addxp".equals(sub)) {
            requireAdmin(sender);
            requireArgs(args, 3, "/murilloskills addxp <skill> <amount>");
            executeAddXp(server, sender, player.getName(), args[1], parseInt(args[2], 1, 1000000));
        } else if ("reset".equals(sub) && args.length == 1) {
            STORE.cache.put(player.getUniqueID(), new PlayerSkillDataCore());
            STORE.save(player.getUniqueID());
            say(player, "MurilloSkills: resetado.");
            Forge112Notifications.notice(player, "MurilloSkills", "Reset", "Skill data cleared");
        } else {
            showUsage(sender);
        }
    }

    private void executePrestigeSelf(EntityPlayerMP player, SkillType skill) {
        PlayerSkillDataCore data = data(player);
        SkillStatsCore stats = data.getSkill(skill);
        if (!data.isParagonSkill(skill)) {
            say(player, "MurilloSkills: apenas a skill Paragon pode prestigiar.");
            return;
        }
        if (stats.getLevel() < CONFIG.getMaxLevel()) {
            say(player, "MurilloSkills: precisa estar no nivel " + CONFIG.getMaxLevel() + " para prestigiar.");
            return;
        }
        if (stats.getPrestige() >= CONFIG.getMaxPrestigeLevel()) {
            say(player, "MurilloSkills: prestigio maximo ja alcancado.");
            return;
        }
        int nextPrestige = stats.getPrestige() + 1;
        stats.setPrestige(nextPrestige);
        stats.setLevel(0);
        stats.setXp(0.0D);
        stats.setLastAbilityUse(-1L);
        persistAndRefresh(player, data);
        say(player, "MurilloSkills: " + skillName(skill) + " agora esta no Prestige " + nextPrestige + ".");
        Forge112Notifications.notice(player, "Prestige", skillName(skill), "Prestige " + nextPrestige);
    }

    private void setClientState(EntityPlayer player, String name, boolean enabled) {
        if ("ultmine_hold".equals(name)) {
            setUltmineHeld(player, enabled);
            return;
        }
        data(player).setToggle(SkillType.MINER, name, enabled);
        STORE.save(player.getUniqueID());
    }

    private void persistAndRefresh(EntityPlayer player, PlayerSkillDataCore data) {
        data.normalizeParagonState();
        SkillRegistry.applyPassives(player, data);
        STORE.save(player.getUniqueID());
    }

    private EntityPlayerMP target(MinecraftServer server, ICommandSender sender, String target)
            throws CommandException {
        try {
            return getPlayer(server, sender, target);
        } catch (CommandException missingByName) {
            try {
                EntityPlayerMP byUuid = server.getPlayerList().getPlayerByUUID(UUID.fromString(target));
                if (byUuid != null) {
                    return byUuid;
                }
            } catch (IllegalArgumentException ignored) {
            }
            throw new CommandException("Player not found or offline: %s", target);
        }
    }

    private void requireAdmin(ICommandSender sender) throws CommandException {
        if (!sender.canUseCommand(2, getName())) {
            throw new CommandException("You do not have permission to use /skill admin commands.");
        }
    }

    private void requireArgs(String[] args, int min, String usage) throws WrongUsageException {
        if (args.length < min) {
            throw new WrongUsageException(usage);
        }
    }

    private SkillType parseSkillName(String name) throws CommandException {
        if (name == null) {
            throw new WrongUsageException("Skill required. Valid skills: " + skillNames());
        }
        for (SkillType skill : SkillType.values()) {
            if (skill.name().equalsIgnoreCase(name)) {
                return skill;
            }
        }
        throw new CommandException("Unknown skill: %s. Valid skills: %s", name, skillNames());
    }

    private void showUsage(ICommandSender sender) {
        reply(sender, "Original syntax:");
        reply(sender, "/skill setlevel <player> <skill> <level>");
        reply(sender, "/skill setprestige <player> <skill> <prestige>");
        reply(sender, "/skill addxp <player> <skill> <amount>");
        reply(sender, "/skill info <player> [skill]");
        reply(sender, "/skill reset <player> <skill>");
        reply(sender, "/skill resetall <player>");
        reply(sender, "/skill select <player> <skill>");
        reply(sender, "/skill deselect <player> <skill>");
        reply(sender, "/skill setparagon <player> <skill>");
        reply(sender, "/skill clearparagon <player>");
        reply(sender, "/skill maxall <player>");
    }

    private void showGuide(ICommandSender sender) {
        reply(sender, "MurilloSkills: select up to " + CONFIG.getMaxSelectedSkills()
                + " skills; paragon enables level 100; ability uses active paragon.");
        reply(sender, "Skills: " + skillNames() + ".");
    }

    private void reply(ICommandSender sender, String message) {
        say(sender, message);
    }

    private String statsText(PlayerSkillDataCore data) {
        StringBuilder out = new StringBuilder("MurilloSkills selected=").append(data.getSelectedSkills())
                .append(" paragon=").append(data.getActiveParagonSkill());
        for (SkillType skill : SkillType.values()) {
            SkillStatsCore stats = data.getSkill(skill);
            out.append(" | ").append(skillName(skill)).append(" L").append(stats.getLevel())
                    .append(" P").append(stats.getPrestige());
        }
        return out.toString();
    }

    private boolean expectsSkillAt(String sub, int index) {
        return index == 2 && ("setlevel".equals(sub) || "setprestige".equals(sub) || "addxp".equals(sub)
                || "reset".equals(sub) || "select".equals(sub) || "deselect".equals(sub)
                || "setparagon".equals(sub) || "info".equals(sub));
    }

    private boolean expectsLegacySkill(String sub) {
        return "select".equals(sub) || "paragon".equals(sub) || "prestige".equals(sub)
                || "ability".equals(sub) || "toggle".equals(sub) || "setlevel".equals(sub)
                || "addxp".equals(sub);
    }

    private String[] skillNamesArray() {
        String[] names = new String[SkillType.values().length];
        for (int i = 0; i < SkillType.values().length; i++) {
            names[i] = SkillType.values()[i].name().toLowerCase(Locale.ROOT);
        }
        return names;
    }

    private String skillNames() {
        StringBuilder out = new StringBuilder();
        for (String name : skillNamesArray()) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(name);
        }
        return out.toString();
    }

    private String[] commonToggles(String skill) {
        String normalized = skill == null ? "" : skill.toLowerCase(Locale.ROOT);
        if ("miner".equals(normalized)) {
            return new String[] { "auto_torch", "ultmine_hold", "ultmine_drops", "ultmine_menu" };
        }
        if ("builder".equals(normalized)) {
            return new String[] { "hollow_fill", "ultplace", "ultplace_config", "fill_mode" };
        }
        if ("explorer".equals(normalized)) {
            return new String[] { "speed_boost", "step_assist", "night_vision" };
        }
        if ("farmer".equals(normalized)) {
            return new String[] { "area_planting" };
        }
        if ("blacksmith".equals(normalized)) {
            return new String[] { "melting_touch" };
        }
        return new String[0];
    }
}
