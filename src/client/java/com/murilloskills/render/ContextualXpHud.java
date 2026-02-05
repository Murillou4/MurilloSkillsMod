package com.murilloskills.render;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.*;

public class ContextualXpHud {
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 6;

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        ItemStack held = client.player.getMainHandStack();
        MurilloSkillsList skill = getSkillForItem(held.getItem());
        if (skill == null) {
            return;
        }

        var stats = ClientSkillData.get(skill);
        int maxLevel = SkillConfig.getMaxLevel();
        int level = Math.min(stats.level, maxLevel);
        int xpNeeded = level >= maxLevel ? 0 : SkillConfig.getXpForLevel(level);
        double currentXp = stats.xp;
        float progress = xpNeeded > 0 ? (float) Math.min(currentXp / xpNeeded, 1.0) : 1.0f;

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - 52;

        int background = 0x88000000;
        int fillColor = 0xFF4CAF50;
        int border = 0xFF202020;

        context.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, border);
        context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, background);
        context.fill(x, y, x + Math.round(BAR_WIDTH * progress), y + BAR_HEIGHT, fillColor);

        String label = skill.name().substring(0, 1) + skill.name().substring(1).toLowerCase() + " "
                + level + (xpNeeded > 0 ? " (" + (int) currentXp + "/" + xpNeeded + ")" : " (MAX)");
        int labelWidth = client.textRenderer.getWidth(label);
        context.drawTextWithShadow(client.textRenderer, label, (screenWidth - labelWidth) / 2, y - 10, 0xFFFFFFFF);
    }

    private static MurilloSkillsList getSkillForItem(Item item) {
        if (item instanceof PickaxeItem) {
            return MurilloSkillsList.MINER;
        }
        if (item instanceof SwordItem) {
            return MurilloSkillsList.WARRIOR;
        }
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            return MurilloSkillsList.ARCHER;
        }
        if (item instanceof HoeItem) {
            return MurilloSkillsList.FARMER;
        }
        if (item instanceof FishingRodItem) {
            return MurilloSkillsList.FISHER;
        }
        if (item instanceof ShearsItem) {
            return MurilloSkillsList.BUILDER;
        }
        return null;
    }
}
