package com.murilloskills.network.handlers;

import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.network.BuilderUndoC2SPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class BuilderUndoNetworkHandler {
    private BuilderUndoNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<BuilderUndoC2SPayload> create() {
        return (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                var data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
                var stats = data.getSkill(MurilloSkillsList.BUILDER);
                boolean hasReachedMaster = stats.level >= SkillConfig.BUILDER_MASTER_LEVEL || stats.prestige > 0;
                if (!hasReachedMaster) {
                    player.sendMessage(Text.translatable("murilloskills.error.level_required", 100,
                            Text.translatable("murilloskills.skill.name.builder")).formatted(Formatting.RED), true);
                    return;
                }

                BuilderSkill.undoLastBrush(player);
            });
        };
    }
}
