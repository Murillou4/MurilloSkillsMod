package com.murilloskills.forge112.utils;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Map;

import static com.murilloskills.forge112.MurilloSkillsForge112.STORE;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.data;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.skillName;

public final class Forge112AchievementTracker {
    private Forge112AchievementTracker() {
    }

    public static void levelUp(EntityPlayer player, SkillType skill, int oldLevel, int newLevel) {
        if (player == null || skill == null || newLevel <= oldLevel) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        Map<String, Integer> stats = data.getAchievementStats();
        add(stats, "levels.total", newLevel - oldLevel);
        add(stats, "levels." + skill.name(), newLevel - oldLevel);
        for (int milestone : new int[] { 10, 25, 50, 75, 100 }) {
            if (oldLevel < milestone && newLevel >= milestone) {
                Forge112Notifications.notice(player, "Milestone", skillName(skill), "Level " + milestone);
            }
        }
        STORE.save(player.getUniqueID());
    }

    public static void increment(EntityPlayer player, String key, int amount) {
        if (player == null || key == null || amount <= 0) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        add(data.getAchievementStats(), key, amount);
        STORE.save(player.getUniqueID());
    }

    private static void add(Map<String, Integer> stats, String key, int amount) {
        Integer current = stats.get(key);
        stats.put(key, Integer.valueOf((current == null ? 0 : current.intValue()) + amount));
    }
}
