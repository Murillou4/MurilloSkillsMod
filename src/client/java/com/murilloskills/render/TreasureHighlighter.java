package com.murilloskills.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Client-side renderer for Explorer's Treasure Hunter passive.
 * Highlights chests and spawners with a glowing purple/teal outline through
 * walls.
 */
public class TreasureHighlighter {

    private static List<BlockPos> treasurePositions = null;
    private static long lastUpdateTime = 0;
    private static final long TIMEOUT_TICKS = 60; // 3 seconds (updates every 2 seconds from server)

    /**
     * Called when server sends new treasure positions
     */
    public static void setTreasures(List<BlockPos> positions) {
        treasurePositions = positions;
        if (MinecraftClient.getInstance().world != null) {
            lastUpdateTime = MinecraftClient.getInstance().world.getTime();
        }
    }

    /**
     * Render the treasure highlights (called from WorldRenderEvents.END_MAIN)
     */
    public static void render(WorldRenderContext context) {
        if (treasurePositions == null || treasurePositions.isEmpty())
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return;

        // Timeout after 3 seconds if no new data received
        if (client.world.getTime() - lastUpdateTime > TIMEOUT_TICKS) {
            treasurePositions = null;
            return;
        }

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        MatrixStack matrices = context.matrices();

        // Enable X-Ray style rendering (draw through blocks)
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_ALWAYS); // Always draw, ignoring depth
        GL11.glLineWidth(2.5f);

        VertexConsumer consumer = client.getBufferBuilders().getEntityVertexConsumers()
                .getBuffer(RenderLayer.getLines());

        for (BlockPos pos : treasurePositions) {
            double x = pos.getX() - cameraPos.x;
            double y = pos.getY() - cameraPos.y;
            double z = pos.getZ() - cameraPos.z;

            // Teal/Cyan color for treasures (R=0.0, G=0.9, B=0.9, A=1.0)
            drawBox(matrices, consumer, x, y, z, 0.0f, 0.9f, 0.9f, 1.0f);
        }

        // Flush the render buffer and restore OpenGL state
        client.getBufferBuilders().getEntityVertexConsumers().draw(RenderLayer.getLines());
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glLineWidth(1.0f);
    }

    /**
     * Draw a wireframe box at the given position
     */
    private static void drawBox(MatrixStack matrices, VertexConsumer consumer,
            double x, double y, double z,
            float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        float minX = (float) x, minY = (float) y, minZ = (float) z;
        float maxX = (float) (x + 1), maxY = (float) (y + 1), maxZ = (float) (z + 1);

        // Bottom face edges
        drawLine(entry, consumer, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        drawLine(entry, consumer, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        drawLine(entry, consumer, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        drawLine(entry, consumer, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        // Top face edges
        drawLine(entry, consumer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawLine(entry, consumer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        drawLine(entry, consumer, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        drawLine(entry, consumer, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        // Vertical edges
        drawLine(entry, consumer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        drawLine(entry, consumer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawLine(entry, consumer, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        drawLine(entry, consumer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void drawLine(MatrixStack.Entry entry, VertexConsumer consumer,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float r, float g, float b, float a) {
        consumer.vertex(entry.getPositionMatrix(), x1, y1, z1).color(r, g, b, a).normal(entry, 0, 1, 0);
        consumer.vertex(entry.getPositionMatrix(), x2, y2, z2).color(r, g, b, a).normal(entry, 0, 1, 0);
    }
}
