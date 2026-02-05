package com.murilloskills.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Admin commands for managing player skills.
 * Commands:
 * - /skill setlevel <player|uuid> <skill> <level>
 * - /skill setprestige <player|uuid> <skill> <prestige>
 * - /skill addxp <player|uuid> <skill> <amount>
 */
public class SkillAdminCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-Admin");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(CommandManager.literal("skill")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2

                // /skill setlevel <player> <skill> <level>
                .then(CommandManager.literal("setlevel")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .then(CommandManager.argument("skill", StringArgumentType.word())
                                        .then(CommandManager.argument("level", IntegerArgumentType.integer(0, 100))
                                                .executes(context -> {
                                                    String target = StringArgumentType.getString(context, "target");
                                                    String skillName = StringArgumentType.getString(context, "skill");
                                                    int level = IntegerArgumentType.getInteger(context, "level");
                                                    return executeSetLevel(context.getSource(), target, skillName,
                                                            level);
                                                })))))

                // /skill setprestige <player> <skill> <prestige>
                .then(CommandManager.literal("setprestige")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .then(CommandManager.argument("skill", StringArgumentType.word())
                                        .then(CommandManager.argument("prestige", IntegerArgumentType.integer(0, 100))
                                                .executes(context -> {
                                                    String target = StringArgumentType.getString(context, "target");
                                                    String skillName = StringArgumentType.getString(context, "skill");
                                                    int prestige = IntegerArgumentType.getInteger(context, "prestige");
                                                    return executeSetPrestige(context.getSource(), target, skillName,
                                                            prestige);
                                                })))))

                // /skill addxp <player> <skill> <amount>
                .then(CommandManager.literal("addxp")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .then(CommandManager.argument("skill", StringArgumentType.word())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 1000000))
                                                .executes(context -> {
                                                    String target = StringArgumentType.getString(context, "target");
                                                    String skillName = StringArgumentType.getString(context, "skill");
                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                    return executeAddXp(context.getSource(), target, skillName, amount);
                                                })))))

                // /skill info <player> [skill]
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .executes(context -> {
                                    String target = StringArgumentType.getString(context, "target");
                                    return executeInfo(context.getSource(), target, null);
                                })
                                .then(CommandManager.argument("skill", StringArgumentType.word())
                                        .executes(context -> {
                                            String target = StringArgumentType.getString(context, "target");
                                            String skillName = StringArgumentType.getString(context, "skill");
                                            return executeInfo(context.getSource(), target, skillName);
                                        }))))

                // /skill reset <player> <skill> - Reset a specific skill to level 0
                .then(CommandManager.literal("reset")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .then(CommandManager.argument("skill", StringArgumentType.word())
                                        .executes(context -> {
                                            String target = StringArgumentType.getString(context, "target");
                                            String skillName = StringArgumentType.getString(context, "skill");
                                            return executeReset(context.getSource(), target, skillName);
                                        }))))

                // /skill resetall <player> - Reset ALL skills for a player
                .then(CommandManager.literal("resetall")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .executes(context -> {
                                    String target = StringArgumentType.getString(context, "target");
                                    return executeResetAll(context.getSource(), target);
                                })))

                // /skill select <player> <skill> - Add a skill to player's selected skills
                .then(CommandManager.literal("select")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .then(CommandManager.argument("skill", StringArgumentType.word())
                                        .executes(context -> {
                                            String target = StringArgumentType.getString(context, "target");
                                            String skillName = StringArgumentType.getString(context, "skill");
                                            return executeSelect(context.getSource(), target, skillName);
                                        }))))

                // /skill deselect <player> <skill> - Remove a skill from player's selected
                // skills
                .then(CommandManager.literal("deselect")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .then(CommandManager.argument("skill", StringArgumentType.word())
                                        .executes(context -> {
                                            String target = StringArgumentType.getString(context, "target");
                                            String skillName = StringArgumentType.getString(context, "skill");
                                            return executeDeselect(context.getSource(), target, skillName);
                                        }))))

                // /skill setparagon <player> <skill> - Set the player's paragon skill
                .then(CommandManager.literal("setparagon")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .then(CommandManager.argument("skill", StringArgumentType.word())
                                        .executes(context -> {
                                            String target = StringArgumentType.getString(context, "target");
                                            String skillName = StringArgumentType.getString(context, "skill");
                                            return executeSetParagon(context.getSource(), target, skillName);
                                        }))))

                // /skill clearparagon <player> - Clear the player's paragon skill
                .then(CommandManager.literal("clearparagon")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .executes(context -> {
                                    String target = StringArgumentType.getString(context, "target");
                                    return executeClearParagon(context.getSource(), target);
                                })))

                // /skill maxall <player> - Max out all skills to level 100
                .then(CommandManager.literal("maxall")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .executes(context -> {
                                    String target = StringArgumentType.getString(context, "target");
                                    return executeMaxAll(context.getSource(), target);
                                }))));

        LOGGER.info("Skill admin commands registered");
    }

    private static int executeSetLevel(ServerCommandSource source, String target, String skillName, int level) {
        try {
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(target);
            if (player == null) {
                source.sendError(Text.literal("Player not found or offline: " + target));
                return 0;
            }

            MurilloSkillsList skill = parseSkill(skillName);
            if (skill == null) {
                source.sendError(Text.literal("Unknown skill: " + skillName + ". Valid skills: " + getSkillNames()));
                return 0;
            }

            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
            var stats = playerData.getSkill(skill);

            stats.level = level;
            stats.xp = 0; // Reset XP when setting level

            // Sync
            com.murilloskills.utils.SkillAttributes.updateAllStats(player, playerData);
            SkillsNetworkUtils.syncSkills(player);

            source.sendFeedback(() -> Text.literal("Set " + skill.name() + " level to " + level + " for " + target)
                    .formatted(Formatting.GREEN), true);
            LOGGER.info("Admin {} set {} level to {} for {}",
                    source.getName(), skill.name(), level, player.getUuid());
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing setlevel command", e);
            source.sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeSetPrestige(ServerCommandSource source, String target, String skillName, int prestige) {
        try {
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(target);
            if (player == null) {
                source.sendError(Text.literal("Player not found or offline: " + target));
                return 0;
            }

            MurilloSkillsList skill = parseSkill(skillName);
            if (skill == null) {
                source.sendError(Text.literal("Unknown skill: " + skillName + ". Valid skills: " + getSkillNames()));
                return 0;
            }

            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
            var stats = playerData.getSkill(skill);

            stats.prestige = prestige;

            // Sync
            com.murilloskills.utils.SkillAttributes.updateAllStats(player, playerData);
            SkillsNetworkUtils.syncSkills(player);

            source.sendFeedback(
                    () -> Text.literal("Set " + skill.name() + " prestige to " + prestige + " for " + target)
                            .formatted(Formatting.LIGHT_PURPLE),
                    true);
            LOGGER.info("Admin {} set {} prestige to {} for {}",
                    source.getName(), skill.name(), prestige, player.getUuid());
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing setprestige command", e);
            source.sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeAddXp(ServerCommandSource source, String target, String skillName, int amount) {
        try {
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(target);
            if (player == null) {
                source.sendError(Text.literal("Player not found or offline: " + target));
                return 0;
            }

            MurilloSkillsList skill = parseSkill(skillName);
            if (skill == null) {
                source.sendError(Text.literal("Unknown skill: " + skillName + ". Valid skills: " + getSkillNames()));
                return 0;
            }

            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            // Force add XP even if skill is not selected
            var stats = playerData.getSkill(skill);
            stats.xp += amount;

            // Level up if needed - using formula: 60 + (level * 15) + (2 * level²)
            while (stats.level < 100) {
                int xpNeeded = 60 + (stats.level * 15) + (2 * stats.level * stats.level);
                if (stats.xp >= xpNeeded) {
                    stats.xp -= xpNeeded;
                    stats.level++;
                } else {
                    break;
                }
            }

            // Sync
            com.murilloskills.utils.SkillAttributes.updateAllStats(player, playerData);
            SkillsNetworkUtils.syncSkills(player);

            source.sendFeedback(() -> Text.literal("Added " + amount + " XP to " + skill.name() + " for " + target +
                    " (now level " + stats.level + ")")
                    .formatted(Formatting.GOLD), true);
            LOGGER.info("Admin {} added {} XP to {} for {} (now level {})",
                    source.getName(), amount, skill.name(), player.getUuid(), stats.level);
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing addxp command", e);
            source.sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeInfo(ServerCommandSource source, String target, String skillName) {
        try {
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(target);
            if (player == null) {
                source.sendError(Text.literal("Player not found or offline: " + target));
                return 0;
            }

            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            if (skillName != null) {
                // Show specific skill info
                MurilloSkillsList skill = parseSkill(skillName);
                if (skill == null) {
                    source.sendError(Text.literal("Unknown skill: " + skillName));
                    return 0;
                }

                var stats = playerData.getSkill(skill);
                source.sendFeedback(() -> Text.literal("=== " + skill.name() + " for " + target + " ===")
                        .formatted(Formatting.GOLD), false);
                source.sendFeedback(() -> Text.literal("Level: " + stats.level + " | XP: " + (int) stats.xp)
                        .formatted(Formatting.YELLOW), false);
                source.sendFeedback(() -> Text.literal("Prestige: " + stats.prestige)
                        .formatted(Formatting.LIGHT_PURPLE), false);
                source.sendFeedback(() -> Text.literal("Selected: " + playerData.isSkillSelected(skill))
                        .formatted(Formatting.AQUA), false);
            } else {
                // Show all skills
                source.sendFeedback(() -> Text.literal("=== Skills for " + target + " ===")
                        .formatted(Formatting.GOLD), false);
                source.sendFeedback(() -> Text.literal("Paragon: " +
                        (playerData.paragonSkill != null ? playerData.paragonSkill.name() : "None"))
                        .formatted(Formatting.LIGHT_PURPLE), false);
                source.sendFeedback(() -> Text.literal("Selected: " + playerData.getSelectedSkills())
                        .formatted(Formatting.AQUA), false);

                for (MurilloSkillsList skill : MurilloSkillsList.values()) {
                    var stats = playerData.getSkill(skill);
                    String prefix = playerData.isSkillSelected(skill) ? "✓ " : "  ";
                    source.sendFeedback(() -> Text.literal(prefix + skill.name() +
                            ": Lv" + stats.level + " P" + stats.prestige)
                            .formatted(playerData.isSkillSelected(skill) ? Formatting.GREEN : Formatting.GRAY), false);
                }
            }
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing info command", e);
            source.sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeReset(ServerCommandSource source, String target, String skillName) {
        try {
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(target);
            if (player == null) {
                source.sendError(Text.literal("Player not found or offline: " + target));
                return 0;
            }

            MurilloSkillsList skill = parseSkill(skillName);
            if (skill == null) {
                source.sendError(Text.literal("Unknown skill: " + skillName + ". Valid skills: " + getSkillNames()));
                return 0;
            }

            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
            var stats = playerData.getSkill(skill);

            stats.level = 0;
            stats.xp = 0;
            stats.prestige = 0;
            stats.lastAbilityUse = -1;

            // Sync
            com.murilloskills.utils.SkillAttributes.updateAllStats(player, playerData);
            SkillsNetworkUtils.syncSkills(player);

            source.sendFeedback(() -> Text.literal("Reset " + skill.name() + " for " + target)
                    .formatted(Formatting.YELLOW), true);
            LOGGER.info("Admin {} reset {} for {}", source.getName(), skill.name(), player.getUuid());
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing reset command", e);
            source.sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeResetAll(ServerCommandSource source, String target) {
        try {
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(target);
            if (player == null) {
                source.sendError(Text.literal("Player not found or offline: " + target));
                return 0;
            }

            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            // Reset all skills
            for (MurilloSkillsList skill : MurilloSkillsList.values()) {
                var stats = playerData.getSkill(skill);
                stats.level = 0;
                stats.xp = 0;
                stats.prestige = 0;
                stats.lastAbilityUse = -1;
            }

            // Clear paragon and selected skills
            playerData.paragonSkill = null;
            playerData.selectedSkills.clear();

            // Sync
            com.murilloskills.utils.SkillAttributes.updateAllStats(player, playerData);
            SkillsNetworkUtils.syncSkills(player);

            source.sendFeedback(() -> Text.literal("Reset ALL skills for " + target)
                    .formatted(Formatting.RED, Formatting.BOLD), true);
            LOGGER.info("Admin {} reset ALL skills for {}", source.getName(), player.getUuid());
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing resetall command", e);
            source.sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeSelect(ServerCommandSource source, String target, String skillName) {
        try {
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(target);
            if (player == null) {
                source.sendError(Text.literal("Player not found or offline: " + target));
                return 0;
            }

            MurilloSkillsList skill = parseSkill(skillName);
            if (skill == null) {
                source.sendError(Text.literal("Unknown skill: " + skillName + ". Valid skills: " + getSkillNames()));
                return 0;
            }

            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            if (playerData.isSkillSelected(skill)) {
                source.sendError(Text.literal("Skill " + skill.name() + " is already selected for " + target));
                return 0;
            }

            if (playerData.selectedSkills.size() >= 3) {
                source.sendError(Text
                        .literal("Player " + target + " already has 3 skills selected. Use /skill deselect first."));
                return 0;
            }

            playerData.selectedSkills.add(skill);

            // Sync
            com.murilloskills.utils.SkillAttributes.updateAllStats(player, playerData);
            SkillsNetworkUtils.syncSkills(player);

            source.sendFeedback(() -> Text.literal("Added " + skill.name() + " to selected skills for " + target)
                    .formatted(Formatting.GREEN), true);
            LOGGER.info("Admin {} added {} to selected skills for {}", source.getName(), skill.name(),
                    player.getUuid());
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing select command", e);
            source.sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeDeselect(ServerCommandSource source, String target, String skillName) {
        try {
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(target);
            if (player == null) {
                source.sendError(Text.literal("Player not found or offline: " + target));
                return 0;
            }

            MurilloSkillsList skill = parseSkill(skillName);
            if (skill == null) {
                source.sendError(Text.literal("Unknown skill: " + skillName + ". Valid skills: " + getSkillNames()));
                return 0;
            }

            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            if (!playerData.isSkillSelected(skill)) {
                source.sendError(Text.literal("Skill " + skill.name() + " is not selected for " + target));
                return 0;
            }

            playerData.selectedSkills.remove(skill);

            // If this was paragon skill, clear it
            if (playerData.paragonSkill == skill) {
                playerData.paragonSkill = null;
            }

            // Sync
            com.murilloskills.utils.SkillAttributes.updateAllStats(player, playerData);
            SkillsNetworkUtils.syncSkills(player);

            source.sendFeedback(() -> Text.literal("Removed " + skill.name() + " from selected skills for " + target)
                    .formatted(Formatting.YELLOW), true);
            LOGGER.info("Admin {} removed {} from selected skills for {}", source.getName(), skill.name(),
                    player.getUuid());
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing deselect command", e);
            source.sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeSetParagon(ServerCommandSource source, String target, String skillName) {
        try {
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(target);
            if (player == null) {
                source.sendError(Text.literal("Player not found or offline: " + target));
                return 0;
            }

            MurilloSkillsList skill = parseSkill(skillName);
            if (skill == null) {
                source.sendError(Text.literal("Unknown skill: " + skillName + ". Valid skills: " + getSkillNames()));
                return 0;
            }

            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            // Auto-select skill if not selected
            if (!playerData.isSkillSelected(skill)) {
                if (playerData.selectedSkills.size() >= 3) {
                    source.sendError(
                            Text.literal("Player has 3 skills selected and " + skill.name() + " is not one of them."));
                    return 0;
                }
                playerData.selectedSkills.add(skill);
            }

            playerData.paragonSkill = skill;

            // Sync
            com.murilloskills.utils.SkillAttributes.updateAllStats(player, playerData);
            SkillsNetworkUtils.syncSkills(player);

            source.sendFeedback(() -> Text.literal("Set paragon skill to " + skill.name() + " for " + target)
                    .formatted(Formatting.GOLD, Formatting.BOLD), true);
            LOGGER.info("Admin {} set paragon to {} for {}", source.getName(), skill.name(), player.getUuid());
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing setparagon command", e);
            source.sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeClearParagon(ServerCommandSource source, String target) {
        try {
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(target);
            if (player == null) {
                source.sendError(Text.literal("Player not found or offline: " + target));
                return 0;
            }

            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            if (playerData.paragonSkill == null) {
                source.sendError(Text.literal("Player " + target + " does not have a paragon skill set"));
                return 0;
            }

            MurilloSkillsList oldParagon = playerData.paragonSkill;
            playerData.paragonSkill = null;

            // Sync
            com.murilloskills.utils.SkillAttributes.updateAllStats(player, playerData);
            SkillsNetworkUtils.syncSkills(player);

            source.sendFeedback(() -> Text.literal("Cleared paragon skill (" + oldParagon.name() + ") for " + target)
                    .formatted(Formatting.YELLOW), true);
            LOGGER.info("Admin {} cleared paragon for {}", source.getName(), player.getUuid());
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing clearparagon command", e);
            source.sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeMaxAll(ServerCommandSource source, String target) {
        try {
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(target);
            if (player == null) {
                source.sendError(Text.literal("Player not found or offline: " + target));
                return 0;
            }

            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            // Max all skills
            for (MurilloSkillsList skill : MurilloSkillsList.values()) {
                var stats = playerData.getSkill(skill);
                stats.level = 100;
                stats.xp = 0;
            }

            // Persistence handled automatically by attachments

            // Sync if player is online
            // In this command context, 'player' variable IS the target player (online or
            // offline loaded).
            // But since getPlayerManager().getPlayer(target) returns NULL if offline,
            // 'player' here IS online.
            com.murilloskills.utils.SkillAttributes.updateAllStats(player, playerData);
            SkillsNetworkUtils.syncSkills(player);

            source.sendFeedback(() -> Text.literal("Maxed ALL skills (level 100) for " + target)
                    .formatted(Formatting.GOLD, Formatting.BOLD), true);
            LOGGER.info("Admin {} maxed ALL skills for {}", source.getName(), target);
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing maxall command", e);
            source.sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Resolves a player name or UUID string to a UUID.
     */

    /**
     * Parses a skill name (case-insensitive) to MurilloSkillsList enum.
     */
    private static MurilloSkillsList parseSkill(String name) {
        for (MurilloSkillsList skill : MurilloSkillsList.values()) {
            if (skill.name().equalsIgnoreCase(name)) {
                return skill;
            }
        }
        return null;
    }

    /**
     * Gets a comma-separated list of valid skill names.
     */
    private static String getSkillNames() {
        StringBuilder sb = new StringBuilder();
        for (MurilloSkillsList skill : MurilloSkillsList.values()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(skill.name().toLowerCase());
        }
        return sb.toString();
    }
}
