package com.murilloskills.render;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.UltPlaceClientState;
import com.murilloskills.skills.MurilloSkillsList;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.LinkedHashSet;

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

        ItemStack handStack = client.player.getMainHandStack();
        if (!(handStack.getItem() instanceof BlockItem)) {
            handStack = client.player.getOffHandStack();
        }
        if (!(handStack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        BlockPos primary = null;
        HitResult hitResult = client.crosshairTarget;
        if (hitResult instanceof BlockHitResult blockHit) {
            BlockPos candidate = resolvePreviewOrigin(client.world, blockHit.getBlockPos(), blockHit.getSide());
            if (canPreviewAt(client.world, candidate)) {
                primary = candidate;
            }
        }

        LinkedHashSet<BlockPos> blocks = new LinkedHashSet<>(UltPlaceClientState.getPreview());
        if (primary != null && !blocks.isEmpty() && !blocks.contains(primary)) {
            blocks.clear();
        }
        if (blocks.isEmpty() && primary != null) {
            blocks.add(primary.toImmutable());
        } else if (primary == null && !blocks.isEmpty()) {
            primary = blocks.getFirst();
        }

        if (blocks.isEmpty()) {
            return;
        }

        BlockState ghostState = blockItem.getBlock().getDefaultState();
        renderGhostBlocks(context, client, blocks, ghostState);

        VeinMinerPreview.renderOutlines(context, blocks, primary, 0.38f, 0.95f, 0.78f, 0.88f);
    }

    private static void renderGhostBlocks(WorldRenderContext context, MinecraftClient client,
            LinkedHashSet<BlockPos> blocks, BlockState state) {
        Vec3d cam = client.gameRenderer.getCamera().getPos();
        MatrixStack matrices = context.matrices();
        BlockRenderManager brm = client.getBlockRenderManager();
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();

        for (BlockPos pos : blocks) {
            matrices.push();
            matrices.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
            brm.renderBlockAsEntity(state, matrices, consumers, FULL_BRIGHTNESS, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }

        consumers.draw(RenderLayers.getMovingBlockLayer(state));
    }

    private static BlockPos resolvePreviewOrigin(World world, BlockPos targetPos, net.minecraft.util.math.Direction face) {
        if (world == null || targetPos == null || face == null) {
            return targetPos;
        }
        if (canPreviewAt(world, targetPos)) {
            return targetPos.toImmutable();
        }
        return targetPos.offset(face).toImmutable();
    }

    private static boolean canPreviewAt(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }
        var state = world.getBlockState(pos);
        return state.isAir() || state.isReplaceable() || !state.getFluidState().isEmpty();
    }
}
