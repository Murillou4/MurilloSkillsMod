package com.murilloskills.forge112.utils;

import com.google.gson.JsonObject;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import net.minecraft.entity.player.EntityPlayer;

import static com.murilloskills.forge112.MurilloSkillsForge112.STORE;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.data;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.skillName;

public final class Forge112FirstTimeHints {
    private static final String EXTENSION = "firstHints112";

    private Forge112FirstTimeHints() {
    }

    public static void onLogin(EntityPlayer player) {
        PlayerSkillDataCore data = data(player);
        JsonObject hints = hints(data);
        if (!hints.has("welcome")) {
            Forge112Notifications.notice(player, "MurilloSkills", "Choose up to 3 skills",
                    "Open the skill cards with O");
            hints.addProperty("welcome", true);
            data.getExtensions().put(EXTENSION, hints);
            STORE.save(player.getUniqueID());
        }
    }

    public static void onSelection(EntityPlayer player, SkillType skill) {
        PlayerSkillDataCore data = data(player);
        JsonObject hints = hints(data);
        if (!hints.has("selection")) {
            Forge112Notifications.notice(player, "Skill selected", skillName(skill),
                    "Only Paragon skills reach level 100");
            hints.addProperty("selection", true);
            data.getExtensions().put(EXTENSION, hints);
            STORE.save(player.getUniqueID());
        }
    }

    private static JsonObject hints(PlayerSkillDataCore data) {
        if (data.getExtensions().containsKey(EXTENSION) && data.getExtensions().get(EXTENSION).isJsonObject()) {
            return data.getExtensions().get(EXTENSION).getAsJsonObject();
        }
        return new JsonObject();
    }
}
