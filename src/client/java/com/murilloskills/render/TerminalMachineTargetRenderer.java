package com.murilloskills.render;

import com.murilloskills.data.TerminalMachineTargetClientState;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashSet;
import java.util.List;

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
        List<TerminalMachineTargetClientState.Target> targets = TerminalMachineTargetClientState.getTargetsSnapshot();
        if (targets.isEmpty()) {
            return;
        }
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
        for (TerminalMachineTargetClientState.Target target : targets) {
            if (!client.world.getBlockState(target.pos()).isAir()) {
                positions.add(target.pos());
            }
        }
        if (positions.isEmpty()) {
            TerminalMachineTargetClientState.clear();
            return;
        }
        VeinMinerPreview.renderOutlines(context, positions, TerminalMachineTargetClientState.getTargetPos(), 0.05f,
                0.82f, 1.0f, 0.95f);
    }
}
