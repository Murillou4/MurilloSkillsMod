package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client: Lista de blocos de minério para destacar com cores
 * Cada entrada contém posição e tipo de minério para cor apropriada
 */
public record MinerScanResultPayload(List<OreEntry> ores) implements CustomPayload {
    public static final CustomPayload.Id<MinerScanResultPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "miner_scan_result"));

    /**
     * Tipos de minério com suas cores RGB
     */
    public enum OreType {
        COAL(0.3f, 0.3f, 0.3f), // Gray
        COPPER(1.0f, 0.6f, 0.3f), // Orange
        IRON(0.8f, 0.8f, 0.8f), // Silver
        GOLD(1.0f, 0.84f, 0.0f), // Yellow/Gold
        LAPIS(0.2f, 0.4f, 1.0f), // Blue
        REDSTONE(1.0f, 0.2f, 0.2f), // Red
        DIAMOND(0.4f, 0.9f, 1.0f), // Light Blue/Cyan
        EMERALD(0.0f, 1.0f, 0.5f), // Green
        NETHER_QUARTZ(0.9f, 0.9f, 0.9f), // White
        NETHER_GOLD(1.0f, 0.7f, 0.0f), // Orange-Gold
        ANCIENT_DEBRIS(0.6f, 0.3f, 0.1f), // Brown
        OTHER(1.0f, 1.0f, 0.0f); // Default Yellow

        public final float r, g, b;

        OreType(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    /**
     * Entrada de minério com posição e tipo
     */
    public record OreEntry(BlockPos pos, OreType type) {
    }

    public static final PacketCodec<RegistryByteBuf, MinerScanResultPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeInt(payload.ores.size());
                for (OreEntry entry : payload.ores) {
                    buf.writeBlockPos(entry.pos);
                    buf.writeEnumConstant(entry.type);
                }
            },
            (buf) -> {
                int size = buf.readInt();
                List<OreEntry> list = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    BlockPos pos = buf.readBlockPos();
                    OreType type = buf.readEnumConstant(OreType.class);
                    list.add(new OreEntry(pos, type));
                }
                return new MinerScanResultPayload(list);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}