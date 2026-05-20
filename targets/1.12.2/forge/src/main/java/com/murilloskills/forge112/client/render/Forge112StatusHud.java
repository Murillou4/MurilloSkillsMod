package com.murilloskills.forge112.client.render;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.forge112.client.Forge112ClientHooks;
import com.murilloskills.forge112.client.config.ClientUltmineConfig;
import com.murilloskills.forge112.client.data.TerminalMachineTargetClientState112;
import com.murilloskills.forge112.client.data.UltPlaceClientState112;
import com.murilloskills.forge112.client.data.UltmineClientState112;
import com.murilloskills.forge112.client.gui.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.murilloskills.forge112.utils.Forge112PlayerServices.data;

@SideOnly(Side.CLIENT)
public final class Forge112StatusHud {
    private Forge112StatusHud() {
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
        PlayerSkillDataCore data = data(mc.player);
        int x = 8;
        int y = resolution.getScaledHeight() - 34;
        if (UltmineClientState112.isHeld() || Forge112ClientHooks.ULTMINE.isKeyDown()) {
            y = drawMode(mc.fontRenderer, x, y, "Ultmine",
                    ClientUltmineConfig.getSelectedShape().name() + " "
                            + UltmineClientState112.getPreviewBlocks() + " blocks "
                            + Forge112ClientHooks.getUltmineKeyName(),
                    Palette.ACCENT_GOLD);
        }
        if (TerminalMachineTargetClientState112.isActive()) {
            y = drawMode(mc.fontRenderer, x, y, "Terminal",
                    TerminalMachineTargetClientState112.getTargetName() + " x" + TerminalMachineTargetClientState112.getAmount(),
                    Palette.ACCENT_BLUE);
        }
        if (UltPlaceClientState112.isEnabled() || data.getToggle(SkillType.BUILDER, "ultplace", false)) {
            y = drawMode(mc.fontRenderer, x, y, "UltPlace", UltPlaceClientState112.summary(), Palette.ACCENT_BLUE);
        }
        y = drawToggle(mc.fontRenderer, data, SkillType.MINER, "auto_torch", "AutoTorch", x, y, Palette.ACCENT_GOLD);
        y = drawToggle(mc.fontRenderer, data, SkillType.BLACKSMITH, "melting_touch", "Melting", x, y, Palette.TEXT_PURPLE);
        y = drawToggle(mc.fontRenderer, data, SkillType.FARMER, "area_planting", "AreaPlant", x, y, Palette.ACCENT_GREEN);
        y = drawToggle(mc.fontRenderer, data, SkillType.BUILDER, "hollow_fill", "HollowFill", x, y, Palette.ACCENT_BLUE);
        y = drawToggle(mc.fontRenderer, data, SkillType.EXPLORER, "night_vision", "NightVision", x, y, Palette.TEXT_AQUA);
        y = drawToggle(mc.fontRenderer, data, SkillType.EXPLORER, "speed_boost", "Speed", x, y, Palette.ACCENT_GREEN);
        y = drawToggle(mc.fontRenderer, data, SkillType.EXPLORER, "step_assist", "StepAssist", x, y, Palette.ACCENT_GOLD);
    }

    private static int drawToggle(FontRenderer font, PlayerSkillDataCore data, SkillType skill, String toggle,
            String label, int x, int y, int accent) {
        if (!data.getToggle(skill, toggle, false)) {
            return y;
        }
        return drawMode(font, x, y, label, "ON", accent);
    }

    private static int drawMode(FontRenderer font, int x, int y, String title, String value, int accent) {
        int w = Math.max(96, Math.min(210, font.getStringWidth(title + " " + value) + 18));
        int h = 22;
        GuiScreen.drawRect(x, y, x + w, y + h, Palette.HUD_INDICATOR_BG);
        GuiScreen.drawRect(x, y, x + 2, y + h, accent);
        GuiScreen.drawRect(x, y, x + w, y + 1, Palette.SECTION_BORDER);
        GuiScreen.drawRect(x, y + h - 1, x + w, y + h, Palette.SECTION_BORDER);
        font.drawStringWithShadow(title, x + 7, y + 4, Palette.TEXT_GOLD);
        font.drawString(fit(font, value, w - font.getStringWidth(title) - 18),
                x + 11 + font.getStringWidth(title), y + 5, Palette.TEXT_LIGHT);
        return y - h - 5;
    }

    private static String fit(FontRenderer font, String text, int maxWidth) {
        String out = text == null ? "" : text;
        while (out.length() > 3 && font.getStringWidth(out) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }
}
