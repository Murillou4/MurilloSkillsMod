package com.murilloskills.forge112.utils;

import com.murilloskills.core.config.SkillType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;

public final class Forge112Notifications {
    public static final String PREFIX = "__MURILLOSKILLS_112__|";

    private Forge112Notifications() {
    }

    public static void xp(EntityPlayer player, SkillType skill, int amount, String source) {
        if (player == null || player.world == null || player.world.isRemote || skill == null || amount <= 0) {
            return;
        }
        send(player, "xp", skill.name(), String.valueOf(amount), clean(source));
    }

    public static void levelUp(EntityPlayer player, SkillType skill, int oldLevel, int newLevel) {
        if (player == null || player.world == null || player.world.isRemote || skill == null) {
            return;
        }
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS, 0.85F, 1.12F);
        send(player, "level", skill.name(), String.valueOf(oldLevel), String.valueOf(newLevel));
    }

    public static void ability(EntityPlayer player, SkillType skill, String message) {
        if (player == null || skill == null) {
            return;
        }
        send(player, "notice", "Ability", Forge112PlayerServices.skillName(skill), clean(message));
    }

    public static void selection(EntityPlayer player, SkillType skill) {
        if (player == null || skill == null) {
            return;
        }
        send(player, "notice", "Skill selected", Forge112PlayerServices.skillName(skill), "Active progression slot");
    }

    public static void paragon(EntityPlayer player, SkillType skill) {
        if (player == null || skill == null) {
            return;
        }
        send(player, "notice", "Paragon", Forge112PlayerServices.skillName(skill), "Level 100 path unlocked");
    }

    public static void toggle(EntityPlayer player, SkillType skill, String toggle, boolean enabled) {
        if (player == null || skill == null) {
            return;
        }
        send(player, "toggle", skill.name(), clean(toggle), enabled ? "on" : "off");
    }

    public static void challenge(EntityPlayer player, SkillType skill, String label, int progress, int target, int reward) {
        if (player == null || skill == null) {
            return;
        }
        send(player, "challenge", skill.name(), clean(label), String.valueOf(progress), String.valueOf(target),
                String.valueOf(reward));
    }

    public static void notice(EntityPlayer player, String title, String subtitle, String body) {
        if (player == null) {
            return;
        }
        send(player, "notice", clean(title), clean(subtitle), clean(body));
    }

    private static void send(EntityPlayer player, String type, String... fields) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        StringBuilder payload = new StringBuilder(PREFIX).append(clean(type));
        if (fields != null) {
            for (String field : fields) {
                payload.append('|').append(clean(field));
            }
        }
        player.sendMessage(new TextComponentString(payload.toString()));
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }
}
