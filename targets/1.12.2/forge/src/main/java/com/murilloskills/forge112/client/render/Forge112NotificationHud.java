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
    private static final int MAX_XP_TOASTS = 5;
    private static final long XP_DURATION_MS = 2500L;
    private static final long CARD_DURATION_MS = 4200L;
    private static final long FADE_MS = 300L;
    private static final Deque<XpToast> XP_TOASTS = new ArrayDeque<XpToast>();
    private static final Deque<CardNotice> CARDS = new ArrayDeque<CardNotice>();
    private static boolean enabled = true;

    private Forge112NotificationHud() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            XP_TOASTS.clear();
        }
    }

    public static boolean toggle() {
        setEnabled(!enabled);
        addLocalCard("Notifications", enabled ? "XP cards enabled" : "XP cards disabled", "", 0xFF5DDC7A);
        return enabled;
    }

    public static void addLocalCard(String title, String subtitle, String body, int accent) {
        while (CARDS.size() >= 4) {
            CARDS.pollFirst();
        }
        CARDS.addLast(new CardNotice(title, subtitle, body, accent));
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
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.fontRenderer == null) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(mc);
        renderXpToasts(mc, resolution);
        renderCards(mc, resolution);
    }

    private static void parsePayload(String payload) {
        String[] parts = payload.split("\\|", -1);
        if (parts.length == 0) {
            return;
        }
        String type = parts[0];
        if ("xp".equals(type) && parts.length >= 4) {
            SkillType skill = parseSkill(parts[1]);
            int amount = parseInt(parts[2], 0);
            addXpToast(skill, amount, parts[3]);
        } else if ("level".equals(type) && parts.length >= 4) {
            SkillType skill = parseSkill(parts[1]);
            int next = parseInt(parts[3], 0);
            addLocalCard("Level up", skillTitle(skill), "Level " + next, skillColor(skill));
        } else if ("toggle".equals(type) && parts.length >= 4) {
            SkillType skill = parseSkill(parts[1]);
            addLocalCard("Toggle", skillTitle(skill), parts[2] + " = " + parts[3], skillColor(skill));
        } else if ("challenge".equals(type) && parts.length >= 6) {
            SkillType skill = parseSkill(parts[1]);
            addLocalCard("Daily challenge", skillTitle(skill),
                    parts[2] + " " + parts[3] + "/" + parts[4] + " +" + parts[5] + " XP",
                    skillColor(skill));
        } else if ("notice".equals(type) && parts.length >= 4) {
            addLocalCard(parts[1], parts[2], parts[3], Palette.ACCENT_GOLD);
        }
    }

    private static void addXpToast(SkillType skill, int amount, String source) {
        if (!enabled || skill == null || amount <= 0) {
            return;
        }
        while (XP_TOASTS.size() >= MAX_XP_TOASTS) {
            XP_TOASTS.pollFirst();
        }
        XP_TOASTS.addLast(new XpToast(skill, amount, source));
    }

    private static void renderXpToasts(Minecraft mc, ScaledResolution resolution) {
        long now = System.currentTimeMillis();
        Iterator<XpToast> iterator = XP_TOASTS.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().createdAt > XP_DURATION_MS) {
                iterator.remove();
            }
        }
        int y = 6;
        for (XpToast toast : XP_TOASTS) {
            float alpha = alpha(now - toast.createdAt, XP_DURATION_MS);
            if (alpha <= 0.01F) {
                continue;
            }
            String text = "+" + toast.amount + " XP " + skillTitle(toast.skill)
                    + (toast.source.length() == 0 ? "" : " (" + toast.source + ")");
            int textWidth = mc.fontRenderer.getStringWidth(text);
            int w = Math.min(resolution.getScaledWidth() - 12, textWidth + 14);
            int x = resolution.getScaledWidth() - w - 6;
            int accent = skillColor(toast.skill);
            drawPanel(x, y, w, 17, accent, alpha);
            mc.fontRenderer.drawStringWithShadow(fit(mc.fontRenderer, text, w - 12), x + 7, y + 5,
                    colorWithAlpha(0xFFFFFFFF, alpha));
            y += 21;
        }
    }

    private static void renderCards(Minecraft mc, ScaledResolution resolution) {
        long now = System.currentTimeMillis();
        Iterator<CardNotice> iterator = CARDS.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().createdAt > CARD_DURATION_MS) {
                iterator.remove();
            }
        }
        int w = Math.min(220, Math.max(150, resolution.getScaledWidth() / 3));
        int x = 8;
        int y = resolution.getScaledHeight() - 8;
        for (CardNotice card : CARDS) {
            float alpha = alpha(now - card.createdAt, CARD_DURATION_MS);
            int h = card.body.length() == 0 ? 34 : 46;
            y -= h;
            drawPanel(x, y, w, h, card.accent, alpha);
            mc.fontRenderer.drawStringWithShadow(fit(mc.fontRenderer, card.title, w - 14), x + 8, y + 7,
                    colorWithAlpha(Palette.TEXT_GOLD, alpha));
            mc.fontRenderer.drawString(fit(mc.fontRenderer, card.subtitle, w - 14), x + 8, y + 19,
                    colorWithAlpha(Palette.TEXT_LIGHT, alpha));
            if (card.body.length() > 0) {
                mc.fontRenderer.drawString(fit(mc.fontRenderer, card.body, w - 14), x + 8, y + 31,
                        colorWithAlpha(Palette.TEXT_MUTED, alpha));
            }
            y -= 6;
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

    private static final class XpToast {
        private final SkillType skill;
        private final int amount;
        private final String source;
        private final long createdAt;

        private XpToast(SkillType skill, int amount, String source) {
            this.skill = skill;
            this.amount = amount;
            this.source = source == null ? "" : source;
            this.createdAt = System.currentTimeMillis();
        }
    }

    private static final class CardNotice {
        private final String title;
        private final String subtitle;
        private final String body;
        private final int accent;
        private final long createdAt;

        private CardNotice(String title, String subtitle, String body, int accent) {
            this.title = title == null ? "" : title;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.body = body == null ? "" : body;
            this.accent = accent;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
