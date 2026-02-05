package com.murilloskills.render;

import com.murilloskills.client.config.OreFilterConfig;
import com.murilloskills.network.MinerScanResultPayload;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Clean, elegant ore radar renderer using beacon-style markers.
 * 
 * Visual design:
 * - Diamond/losango markers (not ugly boxes)
 * - Vertical beacon line rising from each ore
 * - Distance-based hierarchy (closer = brighter, bigger)
 * - Respects ore filter preferences from OreFilterConfig
 * - Smooth pulse animation
 * - Gentle fade-out at end
 */
public class OreHighlighter {

    private static List<MinerScanResultPayload.OreEntry> highlightedOres = null;
    private static long highlightStartTime = 0;
    private static long highlightEndTime = 0;
    private static final long TOTAL_DURATION_TICKS = SkillConfig.toTicks(SkillConfig.MINER_ABILITY_DURATION_SECONDS);

    // Distance thresholds for visual hierarchy
    private static final double CLOSE_DISTANCE = 8.0; // Full brightness
    private static final double MEDIUM_DISTANCE = 16.0; // 70% brightness
    private static final double FAR_DISTANCE = 30.0; // 40% brightness

    public static void setHighlights(List<MinerScanResultPayload.OreEntry> ores) {
        highlightedOres = ores;
        if (MinecraftClient.getInstance().world != null) {
            highlightStartTime = MinecraftClient.getInstance().world.getTime();
            highlightEndTime = highlightStartTime + TOTAL_DURATION_TICKS;
            spawnPulseParticles();
        }
    }

    public static void clearHighlights() {
        highlightedOres = null;
    }

    private static void spawnPulseParticles() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || highlightedOres == null) {
            return;
        }
        int maxParticles = Math.min(highlightedOres.size(), 30);
        Vec3d start = client.player.getPos().add(0, client.player.getStandingEyeHeight(), 0);

        for (int i = 0; i < maxParticles; i++) {
            MinerScanResultPayload.OreEntry entry = highlightedOres.get(i);
            Vec3d target = Vec3d.ofCenter(entry.pos());
            Vec3d delta = target.subtract(start);
            int steps = 6;
            for (int step = 1; step <= steps; step++) {
                Vec3d point = start.add(delta.multiply(step / (double) steps));
                client.world.addParticle(net.minecraft.particle.ParticleTypes.END_ROD,
                        point.x, point.y, point.z, 0, 0.01, 0);
            }
        }
    }

    public static void render(WorldRenderContext context) {
        if (highlightedOres == null || highlightedOres.isEmpty())
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return;

        long now = client.world.getTime();
        if (now > highlightEndTime) {
            highlightedOres = null;
            return;
        }

        // Animation timing
        long elapsed = now - highlightStartTime;
        float baseAlpha = 0.8f + 0.2f * (float) Math.sin(elapsed * 0.15);

        // Smooth fade out in last 25%
        float remaining = (float) (highlightEndTime - now) / TOTAL_DURATION_TICKS;
        if (remaining < 0.25f) {
            baseAlpha *= remaining / 0.25f;
        }

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        MatrixStack matrices = context.matrices();
        BlockPos playerBlockPos = client.player.getBlockPos();

        // Get settings from config
        int maxOres = OreFilterConfig.getMaxOres();
        OreFilterConfig.DisplayMode displayMode = OreFilterConfig.getDisplayMode();

        // Filter ores based on user preferences and sort by distance
        List<MinerScanResultPayload.OreEntry> filtered = highlightedOres.stream()
                .filter(e -> OreFilterConfig.isOreEnabled(e.type()))
                .sorted(Comparator.comparingDouble(e -> e.pos().getSquaredDistance(playerBlockPos)))
                .collect(Collectors.toList());

        // Apply display mode logic
        List<MinerScanResultPayload.OreEntry> toRender;
        switch (displayMode) {
            case NEAREST_ONLY:
                // Only show the nearest ore of each type
                toRender = filterNearestOnly(filtered);
                break;
            case VISIBLE_ONLY:
                // Only show ores that are exposed (have at least one air block adjacent)
                toRender = filterVisibleOnly(filtered, client);
                break;
            case XRAY:
            default:
                // Show all ores (through walls)
                toRender = filtered.stream().limit(maxOres).collect(Collectors.toList());
                break;
        }

        if (toRender.isEmpty())
            return;

        // Configure rendering based on display mode
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        // VISIBLE_ONLY uses depth testing to hide ores behind blocks
        if (displayMode == OreFilterConfig.DisplayMode.VISIBLE_ONLY) {
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        } else {
            // XRAY and NEAREST_ONLY show through walls
            GL11.glDepthFunc(GL11.GL_ALWAYS);
        }
        GL11.glLineWidth(1.5f);

        VertexConsumer consumer = client.getBufferBuilders().getEntityVertexConsumers()
                .getBuffer(RenderLayer.getLines());

        int index = 0;
        for (MinerScanResultPayload.OreEntry entry : toRender) {
            double dx = entry.pos().getX() + 0.5 - cameraPos.x;
            double dy = entry.pos().getY() + 0.5 - cameraPos.y;
            double dz = entry.pos().getZ() + 0.5 - cameraPos.z;

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Visual hierarchy by distance
            float distanceAlpha = calculateDistanceAlpha(distance);
            float diamondSize = calculateDiamondSize(distance);
            float beaconHeight = calculateBeaconHeight(distance, index);

            float finalAlpha = baseAlpha * distanceAlpha;
            float r = entry.type().r;
            float g = entry.type().g;
            float b = entry.type().b;

            // Draw diamond marker at ore position
            drawDiamond(matrices, consumer, dx, dy, dz, diamondSize, r, g, b, finalAlpha);

            // Draw vertical beacon line (only for close ores)
            if (distance < MEDIUM_DISTANCE) {
                drawBeaconLine(matrices, consumer, dx, dy, dz, beaconHeight, r, g, b, finalAlpha * 0.6f);
            }

            // Draw small dot at exact position (subtle)
            drawCenterDot(matrices, consumer, dx, dy, dz, r, g, b, finalAlpha * 0.5f);

            index++;
        }

        // Flush and restore
        client.getBufferBuilders().getEntityVertexConsumers().draw(RenderLayer.getLines());
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glLineWidth(1.0f);
    }

    /**
     * Calculate alpha based on distance (closer = brighter)
     */
    private static float calculateDistanceAlpha(double distance) {
        if (distance < CLOSE_DISTANCE) {
            return 1.0f;
        } else if (distance < MEDIUM_DISTANCE) {
            return 0.7f;
        } else if (distance < FAR_DISTANCE) {
            return 0.4f;
        } else {
            return 0.25f;
        }
    }

    /**
     * Calculate diamond size based on distance (closer = bigger)
     */
    private static float calculateDiamondSize(double distance) {
        if (distance < CLOSE_DISTANCE) {
            return 0.4f; // Big, prominent
        } else if (distance < MEDIUM_DISTANCE) {
            return 0.3f; // Medium
        } else {
            return 0.2f; // Small, subtle
        }
    }

    /**
     * Calculate beacon height based on distance and priority
     */
    private static float calculateBeaconHeight(double distance, int index) {
        // Closest ore gets tallest beacon
        if (index == 0) {
            return 3.0f;
        } else if (distance < CLOSE_DISTANCE) {
            return 2.0f;
        } else {
            return 1.0f;
        }
    }

    /**
     * Draw a diamond/losango shape (4 points)
     * Much cleaner than boxes!
     */
    private static void drawDiamond(MatrixStack matrices, VertexConsumer consumer,
            double x, double y, double z, float size, float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        float cx = (float) x;
        float cy = (float) y;
        float cz = (float) z;

        // Diamond shape: 4 points (top, right, bottom, left)
        float top = cy + size;
        float bottom = cy - size;
        float front = cz + size;
        float back = cz - size;
        float right = cx + size;
        float left = cx - size;

        // Horizontal diamond (X-Z plane)
        line(entry, consumer, cx, cy, front, right, cy, cz, r, g, b, a); // Front to Right
        line(entry, consumer, right, cy, cz, cx, cy, back, r, g, b, a); // Right to Back
        line(entry, consumer, cx, cy, back, left, cy, cz, r, g, b, a); // Back to Left
        line(entry, consumer, left, cy, cz, cx, cy, front, r, g, b, a); // Left to Front

        // Vertical diamond (Y axis) - creates 3D diamond shape
        line(entry, consumer, cx, top, cz, right, cy, cz, r, g, b, a); // Top to Right
        line(entry, consumer, cx, top, cz, left, cy, cz, r, g, b, a); // Top to Left
        line(entry, consumer, cx, top, cz, cx, cy, front, r, g, b, a); // Top to Front
        line(entry, consumer, cx, top, cz, cx, cy, back, r, g, b, a); // Top to Back

        line(entry, consumer, cx, bottom, cz, right, cy, cz, r, g, b, a); // Bottom to Right
        line(entry, consumer, cx, bottom, cz, left, cy, cz, r, g, b, a); // Bottom to Left
        line(entry, consumer, cx, bottom, cz, cx, cy, front, r, g, b, a); // Bottom to Front
        line(entry, consumer, cx, bottom, cz, cx, cy, back, r, g, b, a); // Bottom to Back
    }

    /**
     * Draw a vertical beacon line rising from the ore
     */
    private static void drawBeaconLine(MatrixStack matrices, VertexConsumer consumer,
            double x, double y, double z, float height, float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        float cx = (float) x;
        float cy = (float) y;
        float cz = (float) z;

        // Vertical line going up
        line(entry, consumer, cx, cy, cz, cx, cy + height, cz, r, g, b, a);

        // Small cross at top of beacon
        float crossSize = 0.15f;
        float topY = cy + height;
        line(entry, consumer, cx - crossSize, topY, cz, cx + crossSize, topY, cz, r, g, b, a * 0.7f);
        line(entry, consumer, cx, topY, cz - crossSize, cx, topY, cz + crossSize, r, g, b, a * 0.7f);
    }

    /**
     * Draw a tiny center dot (cross) at exact ore position
     */
    private static void drawCenterDot(MatrixStack matrices, VertexConsumer consumer,
            double x, double y, double z, float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        float cx = (float) x;
        float cy = (float) y;
        float cz = (float) z;
        float size = 0.1f;

        // Tiny cross at center
        line(entry, consumer, cx - size, cy, cz, cx + size, cy, cz, r, g, b, a);
        line(entry, consumer, cx, cy - size, cz, cx, cy + size, cz, r, g, b, a);
        line(entry, consumer, cx, cy, cz - size, cx, cy, cz + size, r, g, b, a);
    }

    private static void line(MatrixStack.Entry entry, VertexConsumer consumer,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float r, float g, float b, float a) {
        consumer.vertex(entry.getPositionMatrix(), x1, y1, z1).color(r, g, b, a).normal(entry, 0, 1, 0);
        consumer.vertex(entry.getPositionMatrix(), x2, y2, z2).color(r, g, b, a).normal(entry, 0, 1, 0);
    }

    /**
     * Filter to keep only the nearest ore of each type.
     * Useful for focused mining - shows one of each ore type.
     */
    private static List<MinerScanResultPayload.OreEntry> filterNearestOnly(
            List<MinerScanResultPayload.OreEntry> sortedOres) {
        java.util.Map<MinerScanResultPayload.OreType, MinerScanResultPayload.OreEntry> nearestByType = new java.util.LinkedHashMap<>();

        for (MinerScanResultPayload.OreEntry entry : sortedOres) {
            // putIfAbsent ensures we keep only the first (nearest, since list is sorted)
            nearestByType.putIfAbsent(entry.type(), entry);
        }

        return new java.util.ArrayList<>(nearestByType.values());
    }

    /**
     * Filter to keep only ores that are "visible" (exposed to air).
     * An ore is considered visible if at least one adjacent block is
     * air/transparent.
     * This creates a more realistic mining experience.
     */
    private static List<MinerScanResultPayload.OreEntry> filterVisibleOnly(
            List<MinerScanResultPayload.OreEntry> sortedOres, MinecraftClient client) {
        if (client.world == null) {
            return sortedOres;
        }

        int maxOres = OreFilterConfig.getMaxOres();
        List<MinerScanResultPayload.OreEntry> visible = new java.util.ArrayList<>();

        for (MinerScanResultPayload.OreEntry entry : sortedOres) {
            if (visible.size() >= maxOres)
                break;

            if (isOreExposed(entry.pos(), client)) {
                visible.add(entry);
            }
        }

        return visible;
    }

    /**
     * Check if an ore block is exposed (has at least one air/transparent neighbor).
     * This means the player could potentially see the ore without X-ray.
     */
    private static boolean isOreExposed(BlockPos pos, MinecraftClient client) {
        if (client.world == null)
            return true;

        // Check all 6 adjacent positions
        BlockPos[] neighbors = {
                pos.up(), pos.down(),
                pos.north(), pos.south(),
                pos.east(), pos.west()
        };

        for (BlockPos neighbor : neighbors) {
            net.minecraft.block.BlockState state = client.world.getBlockState(neighbor);
            // Consider exposed if neighbor is air, transparent, or non-solid
            if (state.isAir() || !state.isOpaque() || !state.isSolidBlock(client.world, neighbor)) {
                return true;
            }
        }

        return false;
    }
}
