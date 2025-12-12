package com.murilloskills.render;

import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Renders XP gain toast notifications in the top-right corner of the screen.
 * Toasts appear stacked and fade out after a duration.
 */
public class XpToastRenderer {

    private static final int MAX_VISIBLE_TOASTS = 5;
    private static final long TOAST_DURATION_MS = 2500; // 2.5 seconds
    private static final long FADE_DURATION_MS = 300;
    private static final int TOAST_HEIGHT = 16;
    private static final int TOAST_PADDING = 4;
    private static final int TOAST_MARGIN_RIGHT = 5;
    private static final int TOAST_MARGIN_TOP = 5;

    // Toast colors
    private static final int BG_COLOR = 0xCC101018;
    private static final int BORDER_COLOR = 0xFF2A2A3A;

    private static final Deque<XpToast> toasts = new ArrayDeque<>();

    // Toggle for enabling/disabling toasts (persists across sessions via client
    // config)
    private static boolean enabled = true;

    /**
     * Gets whether XP toasts are enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether XP toasts are enabled.
     */
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            toasts.clear(); // Clear any existing toasts when disabled
        }
    }

    /**
     * Toggles XP toasts on/off.
     * 
     * @return The new enabled state
     */
    public static boolean toggle() {
        setEnabled(!enabled);
        return enabled;
    }

    /**
     * Represents a single XP toast notification.
     */
    private static class XpToast {
        final MurilloSkillsList skill;
        final int xpAmount;
        final String source;
        final long createdAt;

        XpToast(MurilloSkillsList skill, int xpAmount, String source) {
            this.skill = skill;
            this.xpAmount = xpAmount;
            this.source = source;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > TOAST_DURATION_MS;
        }

        float getAlpha() {
            long age = System.currentTimeMillis() - createdAt;

            // Fade in
            if (age < FADE_DURATION_MS) {
                return (float) age / FADE_DURATION_MS;
            }

            // Fade out
            long timeUntilExpire = TOAST_DURATION_MS - age;
            if (timeUntilExpire < FADE_DURATION_MS) {
                return (float) timeUntilExpire / FADE_DURATION_MS;
            }

            return 1.0f;
        }
    }

    /**
     * Adds a new XP toast to the queue.
     * 
     * @param skill    The skill that gained XP
     * @param xpAmount Amount of XP gained
     * @param source   Source of XP (e.g., "Diamond", "Zombie")
     */
    public static void addToast(MurilloSkillsList skill, int xpAmount, String source) {
        if (!enabled)
            return;
        if (skill == null || xpAmount <= 0)
            return;

        // Remove oldest if at max
        while (toasts.size() >= MAX_VISIBLE_TOASTS) {
            toasts.pollFirst();
        }

        toasts.addLast(new XpToast(skill, xpAmount, source));
    }

    /**
     * Renders all active toasts. Should be called from the HUD render event.
     */
    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null)
            return;
        if (toasts.isEmpty())
            return;

        // Remove expired toasts
        Iterator<XpToast> iterator = toasts.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isExpired()) {
                iterator.remove();
            }
        }

        int screenWidth = context.getScaledWindowWidth();
        int y = TOAST_MARGIN_TOP;

        for (XpToast toast : toasts) {
            renderToast(context, client, toast, screenWidth, y);
            y += TOAST_HEIGHT + TOAST_PADDING;
        }
    }

    private static void renderToast(DrawContext context, MinecraftClient client,
            XpToast toast, int screenWidth, int y) {
        float alpha = toast.getAlpha();
        if (alpha <= 0)
            return;

        // Build text
        String icon = getSkillIcon(toast.skill);
        Text skillName = Text.translatable("murilloskills.skill.name." + toast.skill.name().toLowerCase());
        String xpText = "+" + toast.xpAmount + " XP";

        Text fullText;
        if (toast.source != null && !toast.source.isEmpty()) {
            fullText = Text.literal(xpText + " " + icon + " ")
                    .formatted(Formatting.GREEN)
                    .append(skillName.copy().formatted(Formatting.YELLOW))
                    .append(Text.literal(" (" + toast.source + ")").formatted(Formatting.GRAY));
        } else {
            fullText = Text.literal(xpText + " " + icon + " ")
                    .formatted(Formatting.GREEN)
                    .append(skillName.copy().formatted(Formatting.YELLOW));
        }

        int textWidth = client.textRenderer.getWidth(fullText);
        int toastWidth = textWidth + 12;
        int x = screenWidth - toastWidth - TOAST_MARGIN_RIGHT;

        // Apply alpha to colors
        int bgAlpha = (int) (0xCC * alpha) << 24;
        int borderAlpha = (int) (0xFF * alpha) << 24;

        // Background
        context.fill(x - 1, y - 1, x + toastWidth + 1, y + TOAST_HEIGHT + 1,
                (borderAlpha & 0xFF000000) | (BORDER_COLOR & 0x00FFFFFF));
        context.fill(x, y, x + toastWidth, y + TOAST_HEIGHT,
                (bgAlpha & 0xFF000000) | (BG_COLOR & 0x00FFFFFF));

        // Accent line on left
        int accentColor = getSkillColor(toast.skill);
        context.fill(x, y, x + 2, y + TOAST_HEIGHT,
                (borderAlpha & 0xFF000000) | (accentColor & 0x00FFFFFF));

        // Text (with alpha via color)
        int textAlpha = (int) (0xFF * alpha);
        context.drawTextWithShadow(client.textRenderer, fullText, x + 6, y + 4,
                (textAlpha << 24) | 0xFFFFFF);
    }

    private static String getSkillIcon(MurilloSkillsList skill) {
        return switch (skill) {
            case MINER -> "⛏";
            case WARRIOR -> "⚔";
            case FARMER -> "🌾";
            case ARCHER -> "🏹";
            case FISHER -> "🎣";
            case BUILDER -> "🧱";
            case BLACKSMITH -> "🔨";
            case EXPLORER -> "🧭";
        };
    }

    private static int getSkillColor(MurilloSkillsList skill) {
        return switch (skill) {
            case MINER -> 0xFF888888; // Gray
            case WARRIOR -> 0xFFFF4444; // Red
            case FARMER -> 0xFF44AA44; // Green
            case ARCHER -> 0xFF44FF44; // Lime
            case FISHER -> 0xFF4488FF; // Blue
            case BUILDER -> 0xFFFFAA00; // Orange
            case BLACKSMITH -> 0xFFAA8866; // Bronze
            case EXPLORER -> 0xFF44DDDD; // Cyan
        };
    }

    /**
     * Clears all active toasts.
     */
    public static void clear() {
        toasts.clear();
    }
}
