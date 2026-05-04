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
 * Server -> Client: Lista de blocos de minério para destacar com cores.
 * Cada entrada contém posição, chave de filtro, nome exibível e cor.
 */
public record MinerScanResultPayload(List<OreEntry> ores, int remainingDurationTicks) implements CustomPayload {
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
        MODDED(1.0f, 0.95f, 0.25f); // Dynamic modded ore, color comes from OreEntry

        public final float r, g, b;
        public final int color;

        OreType(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.color = 0xFF000000
                    | (((int) (r * 255.0f) & 0xFF) << 16)
                    | (((int) (g * 255.0f) & 0xFF) << 8)
                    | ((int) (b * 255.0f) & 0xFF);
        }
    }

    /**
     * Entrada de minério com posição e dados de filtro/render.
     */
    public record OreEntry(BlockPos pos, OreType type, String filterKey, String displayName, int color) {
        public OreEntry(BlockPos pos, OreType type) {
            this(pos, type, type.name(), "", type.color);
        }

        public String filterKey() {
            return filterKey == null || filterKey.isBlank() ? type.name() : filterKey;
        }

        public String displayName() {
            return displayName == null || displayName.isBlank() ? filterKey() : displayName;
        }

        public int color() {
            return (color & 0xFF000000) == 0 ? (0xFF000000 | color) : color;
        }

        public float r() {
            return ((color() >> 16) & 0xFF) / 255.0f;
        }

        public float g() {
            return ((color() >> 8) & 0xFF) / 255.0f;
        }

        public float b() {
            return (color() & 0xFF) / 255.0f;
        }
    }

    public static final PacketCodec<RegistryByteBuf, MinerScanResultPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeInt(payload.ores.size());
                for (OreEntry entry : payload.ores) {
                    buf.writeBlockPos(entry.pos);
                    buf.writeEnumConstant(entry.type);
                    buf.writeString(entry.filterKey());
                    buf.writeString(entry.displayName());
                    buf.writeInt(entry.color());
                }
                buf.writeVarInt(Math.max(0, payload.remainingDurationTicks));
            },
            (buf) -> {
                int size = buf.readInt();
                List<OreEntry> list = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    BlockPos pos = buf.readBlockPos();
                    OreType type = buf.readEnumConstant(OreType.class);
                    String filterKey = buf.readString();
                    String displayName = buf.readString();
                    int color = buf.readInt();
                    list.add(new OreEntry(pos, type, filterKey, displayName, color));
                }
                int remainingDurationTicks = buf.readVarInt();
                return new MinerScanResultPayload(list, remainingDurationTicks);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
