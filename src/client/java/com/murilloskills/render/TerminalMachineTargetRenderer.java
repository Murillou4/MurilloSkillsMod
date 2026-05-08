package com.murilloskills.render;

import com.murilloskills.data.TerminalMachineTargetClientState;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;

public final class TerminalMachineTargetRenderer {
    private TerminalMachineTargetRenderer() {
    }

    public static void render(WorldRenderContext context) {
        if (!TerminalMachineTargetClientState.hasTarget()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        BlockPos target = TerminalMachineTargetClientState.getTargetPos();
        if (target == null || client.world.getBlockState(target).isAir()) {
            TerminalMachineTargetClientState.clear();
            return;
        }
        VeinMinerPreview.renderOutlines(context, Collections.singleton(target), target, 0.05f, 0.82f, 1.0f, 0.95f);
    }
}
