package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

// Server -> Client: Lista de blocos para destacar
public record MinerScanResultPayload(List<BlockPos> ores) implements CustomPayload {
    public static final CustomPayload.Id<MinerScanResultPayload> ID = new CustomPayload.Id<>(Identifier.of(MurilloSkills.MOD_ID, "miner_scan_result"));

    public static final PacketCodec<RegistryByteBuf, MinerScanResultPayload> CODEC = PacketCodec.tuple(
            PacketCodec.ofStatic(
                    (buf, val) -> {
                        buf.writeInt(val.size());
                        val.forEach(buf::writeBlockPos);
                    },
                    (buf) -> {
                        int size = buf.readInt();
                        List<BlockPos> list = new ArrayList<>();
                        for (int i = 0; i < size; i++) list.add(buf.readBlockPos());
                        return list;
                    }
            ),
            MinerScanResultPayload::ores,
            MinerScanResultPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}