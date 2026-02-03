package com.murilloskills.render;

import com.murilloskills.MurilloSkillsClient;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Renders a preview outline of blocks that will be mined when using vein miner.
 * Shows connected blocks of the same type when the player is holding the vein miner key.
 */
public class VeinMinerPreview {

    // Outline color (white)
    private static final float R = 1.0f;
    private static final float G = 1.0f;
    private static final float B = 1.0f;
    private static final float ALPHA = 0.8f;

    public static void render(WorldRenderContext context) {
        // Only render if vein miner key is held
        if (!MurilloSkillsClient.isVeinMinerKeyHeld()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        // Get the block the player is looking at
        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos targetPos = blockHit.getBlockPos();
        BlockState targetState = client.world.getBlockState(targetPos);

        // Skip air blocks
        if (targetState.isAir()) {
            return;
        }

        // Skip unbreakable blocks (bedrock, etc.)
        if (targetState.getHardness(client.world, targetPos) < 0.0f) {
            return;
        }

        // Collect connected blocks
        int maxBlocks = Math.max(1, SkillConfig.getVeinMinerMaxBlocks());
        Set<BlockPos> connectedBlocks = collectConnectedBlocks(client.world, targetPos, targetState, maxBlocks + 1);

        if (connectedBlocks.isEmpty()) {
            return;
        }

        // Setup rendering
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        MatrixStack matrices = context.matrices();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glLineWidth(2.0f);

        VertexConsumer consumer = client.getBufferBuilders().getEntityVertexConsumers()
                .getBuffer(RenderLayer.getLines());

        // Pulse animation
        long time = client.world.getTime();
        float pulse = 0.7f + 0.3f * (float) Math.sin(time * 0.2);
        float alpha = ALPHA * pulse;

        // Draw outline for the target block (brighter)
        drawBlockOutline(matrices, consumer, targetPos, cameraPos, R, G, B, alpha);

        // Draw outline for connected blocks
        for (BlockPos pos : connectedBlocks) {
            drawBlockOutline(matrices, consumer, pos, cameraPos, R, G, B, alpha * 0.7f);
        }

        // Flush rendering
        client.getBufferBuilders().getEntityVertexConsumers().draw(RenderLayer.getLines());
        GL11.glLineWidth(1.0f);
    }

    /**
     * Collect connected blocks of the same type using BFS (same algorithm as server).
     */
    private static Set<BlockPos> collectConnectedBlocks(World world, BlockPos origin, BlockState originState,
            int maxBlocks) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        queue.add(origin);

        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            BlockPos current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }

            // Check all 26 neighbors (including diagonals)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (visited.size() >= maxBlocks) break;

                        BlockPos neighbor = current.add(dx, dy, dz);
                        if (visited.contains(neighbor)) continue;

                        BlockState neighborState = world.getBlockState(neighbor);
                        if (neighborState.equals(originState)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        visited.remove(origin);
        return visited;
    }

    /**
     * Draw a box outline around a block.
     */
    private static void drawBlockOutline(MatrixStack matrices, VertexConsumer consumer,
            BlockPos pos, Vec3d cameraPos, float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();

        double x = pos.getX() - cameraPos.x;
        double y = pos.getY() - cameraPos.y;
        double z = pos.getZ() - cameraPos.z;

        float x0 = (float) x;
        float y0 = (float) y;
        float z0 = (float) z;
        float x1 = (float) (x + 1);
        float y1 = (float) (y + 1);
        float z1 = (float) (z + 1);

        // Bottom face edges
        line(entry, consumer, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(entry, consumer, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(entry, consumer, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(entry, consumer, x0, y0, z1, x0, y0, z0, r, g, b, a);

        // Top face edges
        line(entry, consumer, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(entry, consumer, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(entry, consumer, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(entry, consumer, x0, y1, z1, x0, y1, z0, r, g, b, a);

        // Vertical edges
        line(entry, consumer, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(entry, consumer, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(entry, consumer, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(entry, consumer, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void line(MatrixStack.Entry entry, VertexConsumer consumer,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float r, float g, float b, float a) {
        consumer.vertex(entry.getPositionMatrix(), x1, y1, z1).color(r, g, b, a).normal(entry, 0, 1, 0);
        consumer.vertex(entry.getPositionMatrix(), x2, y2, z2).color(r, g, b, a).normal(entry, 0, 1, 0);
    }
}
