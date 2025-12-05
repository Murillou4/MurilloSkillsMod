package com.murilloskills.render;

import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class OreHighlighter {

    private static List<BlockPos> highlightedOres = null;
    private static long highlightEndTime = 0;

    public static void setHighlights(List<BlockPos> ores) {
        highlightedOres = ores;
        if (MinecraftClient.getInstance().world != null) {
            highlightEndTime = MinecraftClient.getInstance().world.getTime()
                    + SkillConfig.toTicks(SkillConfig.MINER_ABILITY_DURATION_SECONDS);
        }
    }

    public static void render(WorldRenderContext context) {
        if (highlightedOres == null || highlightedOres.isEmpty())
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return;

        if (client.world.getTime() > highlightEndTime) {
            highlightedOres = null;
            return;
        }

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        MatrixStack matrices = context.matrices();

        // 1. Configurações de Raio-X (OpenGL Puro)
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_ALWAYS); // Desenha sobre tudo
        GL11.glLineWidth(3.0f);

        VertexConsumer consumer = client.getBufferBuilders().getEntityVertexConsumers()
                .getBuffer(RenderLayer.getLines());

        // 2. Loop de Desenho
        for (BlockPos pos : highlightedOres) {
            double x = pos.getX() - cameraPos.x;
            double y = pos.getY() - cameraPos.y;
            double z = pos.getZ() - cameraPos.z;

            // Desenha a caixa amarela (R=1, G=0.84, B=0, A=1)
            drawSimpleBox(matrices, consumer, x, y, z, 1.0f, 0.84f, 0.0f, 1.0f);
        }

        // 3. Renderiza e Limpa
        client.getBufferBuilders().getEntityVertexConsumers().draw(RenderLayer.getLines());
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glLineWidth(1.0f);
    }

    // Método auxiliar simples para desenhar as 12 arestas de um cubo
    private static void drawSimpleBox(MatrixStack matrices, VertexConsumer consumer,
            double x, double y, double z,
            float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        float minX = (float) x, minY = (float) y, minZ = (float) z;
        float maxX = (float) (x + 1), maxY = (float) (y + 1), maxZ = (float) (z + 1);

        // Base
        drawLine(entry, consumer, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        drawLine(entry, consumer, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        drawLine(entry, consumer, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        drawLine(entry, consumer, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        // Topo
        drawLine(entry, consumer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawLine(entry, consumer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        drawLine(entry, consumer, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        drawLine(entry, consumer, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        // Verticais
        drawLine(entry, consumer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        drawLine(entry, consumer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawLine(entry, consumer, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        drawLine(entry, consumer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void drawLine(MatrixStack.Entry entry, VertexConsumer consumer,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float r, float g, float b, float a) {
        // Normal fixa (0, 1, 0) pois linhas de debug não precisam de iluminação
        // complexa
        consumer.vertex(entry.getPositionMatrix(), x1, y1, z1).color(r, g, b, a).normal(entry, 0, 1, 0);
        consumer.vertex(entry.getPositionMatrix(), x2, y2, z2).color(r, g, b, a).normal(entry, 0, 1, 0);
    }
}