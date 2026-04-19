package com.murilloskills.render;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.UltPlaceClientState;
import com.murilloskills.skills.UltPlacePlanner;
import com.murilloskills.skills.MurilloSkillsList;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Renders the current Builder UltPlace preview while the toggle is active.
 */
public final class UltPlacePreview {
    private static final int FULL_BRIGHTNESS = 0x00F000F0;

    private UltPlacePreview() {
    }

    public static void render(WorldRenderContext context) {
        if (!UltPlaceClientState.isEnabled() || !ClientSkillData.isSkillSelected(MurilloSkillsList.BUILDER)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        List<UltPlacePlanner.PreviewBlock> previewBlocks = UltPlaceClientState.getPreview();
        if (previewBlocks.isEmpty()) {
            return;
        }

        renderGhostBlocks(context, client, previewBlocks);

        LinkedHashSet<BlockPos> outlineBlocks = new LinkedHashSet<>();
        for (UltPlacePlanner.PreviewBlock previewBlock : previewBlocks) {
            outlineBlocks.add(previewBlock.pos());
        }
        BlockPos primary = UltPlaceClientState.getPrimaryPreviewPos();
        VeinMinerPreview.renderOutlines(context, outlineBlocks, primary, 0.38f, 0.95f, 0.78f, 0.88f);
    }

    private static void renderGhostBlocks(WorldRenderContext context, MinecraftClient client,
            List<UltPlacePlanner.PreviewBlock> previewBlocks) {
        Vec3d cam = client.gameRenderer.getCamera().getPos();
        MatrixStack matrices = context.matrices();
        BlockRenderManager brm = client.getBlockRenderManager();
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();

        for (UltPlacePlanner.PreviewBlock previewBlock : previewBlocks) {
            BlockPos pos = previewBlock.pos();
            matrices.push();
            matrices.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
            brm.renderBlockAsEntity(previewBlock.state(), matrices, consumers, FULL_BRIGHTNESS,
                    OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }

        consumers.draw();
    }
}
