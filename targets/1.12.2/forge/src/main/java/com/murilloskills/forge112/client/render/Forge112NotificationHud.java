package com.murilloskills.forge112.client.render;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.forge112.client.gui.Palette;
import com.murilloskills.forge112.client.gui.UiData;
import com.murilloskills.forge112.utils.Forge112Notifications;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

@SideOnly(Side.CLIENT)
public final class Forge112NotificationHud {
    private static final int MAX_TOASTS = 5;
    private static final long TOAST_DURATION_MS = 2500L;
    private static final long FADE_MS = 300L;
    private static final long DEDUPE_WINDOW_MS = 350L;
    private static final Deque<ToastNotice> TOASTS = new ArrayDeque<ToastNotice>();
    private static boolean enabled = true;

    private Forge112NotificationHud() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            TOASTS.clear();
        }
    }

    public static boolean toggle() {
        setEnabled(!enabled);
        if (enabled) {
            addLocalCard("Notifications", "Enabled", "", 0xFF5DDC7A);
        }
        return enabled;
    }

    public static void addLocalCard(String title, String subtitle, String body, int accent) {
        if (!enabled) {
            return;
        }
        String text = joinMini(clean(title), clean(subtitle), clean(body));
        addToast(text, accent);
    }

    public static void acceptNetwork(String type, String[] fields) {
        if (!enabled) {
            return;
        }
        String safeType = clean(type);
        String[] safeFields = fields == null ? new String[0] : fields;
        if ("xp".equals(safeType) && safeFields.length >= 3) {
            SkillType skill = parseSkill(safeFields[0]);
            int amount = parseInt(safeFields[1], 0);
            addXpToast(skill, amount, safeFields[2]);
        } else if ("level".equals(safeType) && safeFields.length >= 3) {
            return;
        } else if ("toggle".equals(safeType) && safeFields.length >= 3) {
            SkillType skill = parseSkill(safeFields[0]);
            addToast(skillTitle(skill) + " " + clean(safeFields[1]) + ": " + clean(safeFields[2]), skillColor(skill));
        } else if ("challenge".equals(safeType) && safeFields.length >= 5) {
            SkillType skill = parseSkill(safeFields[0]);
            addToast(skillTitle(skill) + " " + clean(safeFields[1]) + " " + clean(safeFields[2]) + "/"
                    + clean(safeFields[3]) + " +" + clean(safeFields[4]) + " XP", skillColor(skill));
        } else if ("notice".equals(safeType) && safeFields.length >= 3) {
            addLocalCard(safeFields[0], safeFields[1], safeFields[2], Palette.ACCENT_GOLD);
        }
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent event) {
        String text = event.getMessage() == null ? "" : event.getMessage().getUnformattedText();
        if (!text.startsWith(Forge112Notifications.PREFIX)) {
            return;
        }
        event.setCanceled(true);
        parsePayload(text.substring(Forge112Notifications.PREFIX.length()));
    }

    @SubscribeEvent
    public static void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }
        if (!enabled) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.fontRenderer == null) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(mc);
        renderToasts(mc, resolution);
    }

    private static void parsePayload(String payload) {
        String[] parts = payload.split("\\|", -1);
        if (parts.length == 0) {
            return;
        }
        String[] fields = new String[Math.max(0, parts.length - 1)];
        for (int i = 1; i < parts.length; i++) {
            fields[i - 1] = parts[i];
        }
        acceptNetwork(parts[0], fields);
    }

    private static void addXpToast(SkillType skill, int amount, String source) {
        if (!enabled || skill == null || amount <= 0) {
            return;
        }
        String text = "+" + amount + " XP " + skillTitle(skill)
                + (clean(source).length() == 0 ? "" : " (" + clean(source) + ")");
        addToast(text, skillColor(skill));
    }

    private static void addToast(String text, int accent) {
        if (!enabled) {
            return;
        }
        String cleanText = clean(text);
        if (cleanText.length() == 0) {
            return;
        }
        ToastNotice last = TOASTS.peekLast();
        long now = System.currentTimeMillis();
        if (last != null && now - last.createdAt < DEDUPE_WINDOW_MS && last.text.equals(cleanText)) {
            return;
        }
        while (TOASTS.size() >= MAX_TOASTS) {
            TOASTS.pollFirst();
        }
        TOASTS.addLast(new ToastNotice(cleanText, accent));
    }

    private static void renderToasts(Minecraft mc, ScaledResolution resolution) {
        long now = System.currentTimeMillis();
        Iterator<ToastNotice> iterator = TOASTS.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().createdAt > TOAST_DURATION_MS) {
                iterator.remove();
            }
        }
        int y = 6;
        for (ToastNotice toast : TOASTS) {
            float alpha = alpha(now - toast.createdAt, TOAST_DURATION_MS);
            if (alpha <= 0.01F) {
                continue;
            }
            int textWidth = mc.fontRenderer.getStringWidth(toast.text);
            int w = Math.min(resolution.getScaledWidth() - 12, textWidth + 14);
            int x = resolution.getScaledWidth() - w - 6;
            drawPanel(x, y, w, 17, toast.accent, alpha);
            mc.fontRenderer.drawStringWithShadow(fit(mc.fontRenderer, toast.text, w - 12), x + 7, y + 5,
                    colorWithAlpha(0xFFFFFFFF, alpha));
            y += 21;
        }
    }

    private static void drawPanel(int x, int y, int w, int h, int accent, float alpha) {
        int bg = colorWithAlpha(0xEA101018, alpha);
        int border = colorWithAlpha(0xFF303448, alpha);
        int accentColor = colorWithAlpha(accent, alpha);
        GuiScreen.drawRect(x, y, x + w, y + h, bg);
        GuiScreen.drawRect(x, y, x + 2, y + h, accentColor);
        GuiScreen.drawRect(x, y, x + w, y + 1, border);
        GuiScreen.drawRect(x, y + h - 1, x + w, y + h, border);
        GuiScreen.drawRect(x, y, x + 1, y + h, border);
        GuiScreen.drawRect(x + w - 1, y, x + w, y + h, border);
    }

    private static float alpha(long age, long duration) {
        if (age < 0L || age > duration) {
            return 0.0F;
        }
        if (age < FADE_MS) {
            return age / (float) FADE_MS;
        }
        long left = duration - age;
        if (left < FADE_MS) {
            return Math.max(0.0F, left / (float) FADE_MS);
        }
        return 1.0F;
    }

    private static int colorWithAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 255) * alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static String fit(FontRenderer font, String text, int maxWidth) {
        String out = text == null ? "" : text;
        while (out.length() > 3 && font.getStringWidth(out) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        if (font.getStringWidth(out) > maxWidth && out.length() > 0) {
            return "";
        }
        return out;
    }

    private static SkillType parseSkill(String value) {
        try {
            return value == null || value.length() == 0 ? null : SkillType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String skillTitle(SkillType skill) {
        return skill == null ? "Skill" : UiData.title(skill);
    }

    private static int skillColor(SkillType skill) {
        return skill == null ? Palette.ACCENT_GOLD : UiData.skillColor(skill);
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String joinMini(String title, String subtitle, String body) {
        String text = title;
        if (subtitle.length() > 0) {
            text += (text.length() == 0 ? "" : " ") + subtitle;
        }
        if (body.length() > 0) {
            text += (text.length() == 0 ? "" : " ") + body;
        }
        return text;
    }

    private static final class ToastNotice {
        private final String text;
        private final int accent;
        private final long createdAt;

        private ToastNotice(String text, int accent) {
            this.text = text == null ? "" : text;
            this.accent = accent;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
