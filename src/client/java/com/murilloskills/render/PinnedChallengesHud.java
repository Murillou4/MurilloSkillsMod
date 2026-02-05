package com.murilloskills.render;

import com.murilloskills.data.ClientSkillData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

public class PinnedChallengesHud {
    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        List<ClientSkillData.ChallengeInfo> pinned = ClientSkillData.getPinnedChallenges();
        if (pinned.isEmpty()) {
            return;
        }

        int x = 10;
        int y = 10;
        int padding = 4;
        int lineHeight = 12;

        int width = 180;
        int height = padding * 2 + (pinned.size() * lineHeight);
        context.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x88000000);

        int textY = y + padding;
        for (ClientSkillData.ChallengeInfo challenge : pinned) {
            String progress = challenge.progress() + "/" + challenge.target();
            Text label = Text.translatable("murilloskills.challenge." + challenge.type().toLowerCase());
            context.drawTextWithShadow(client.textRenderer, label, x + padding, textY, 0xFFFFFFFF);
            int progressWidth = client.textRenderer.getWidth(progress);
            context.drawTextWithShadow(client.textRenderer, Text.literal(progress),
                    x + width - progressWidth - padding, textY, 0xFFB0B0B0);
            textY += lineHeight;
        }
    }
}
