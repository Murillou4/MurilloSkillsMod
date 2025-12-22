package com.murilloskills.network;

import com.murilloskills.impl.BuilderFillMode;
import com.murilloskills.impl.BuilderSkill;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Client-to-server packet for cycling fill mode on Builder's Creative Brush
 * ability.
 * Cycles between: CUBOID → SPHERE → CYLINDER → PYRAMID → WALL → CUBOID...
 */
public record FillModeCycleC2SPayload() implements CustomPayload {

    public static final CustomPayload.Id<FillModeCycleC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of("murilloskills", "fill_mode_cycle"));

    public static final PacketCodec<RegistryByteBuf, FillModeCycleC2SPayload> CODEC = PacketCodec
            .unit(new FillModeCycleC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /**
     * Handles the cycling logic on the server
     */
    public static void handle(FillModeCycleC2SPayload payload, ServerPlayerEntity player) {
        BuilderFillMode newMode = BuilderSkill.cycleNextFillMode(player);

        // Get the translated mode name
        String translationKey = newMode.getTranslationKey();

        // For cylinder, include orientation
        Text modeText;
        if (newMode == BuilderFillMode.CYLINDER) {
            boolean isHorizontal = BuilderSkill.isCylinderHorizontal(player);
            String orientationKey = isHorizontal ? "murilloskills.builder.cylinder_horizontal"
                    : "murilloskills.builder.cylinder_vertical";
            modeText = Text.translatable(translationKey, Text.translatable(orientationKey));
        } else {
            modeText = Text.translatable(translationKey);
        }

        // Send feedback message
        player.sendMessage(Text.literal("").append(modeText).styled(style -> style.withColor(Formatting.AQUA)), true);
    }
}
