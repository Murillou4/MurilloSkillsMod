package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Client -> Server: Player syncs their classic-mode blocked block list.
 */
public record UltmineClassicBlockListSyncC2SPayload(List<String> blockIds) implements CustomPayload {
    public static final CustomPayload.Id<UltmineClassicBlockListSyncC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "ultmine_classic_block_list_sync"));
    public static final PacketCodec<RegistryByteBuf, UltmineClassicBlockListSyncC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeVarInt(payload.blockIds.size());
                for (String blockId : payload.blockIds) {
                    buf.writeString(blockId);
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<String> blockIds = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    blockIds.add(buf.readString());
                }
                return new UltmineClassicBlockListSyncC2SPayload(blockIds);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
