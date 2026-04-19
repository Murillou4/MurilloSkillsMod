package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Client -> Server: Player syncs their trash item list.
 */
public record TrashListSyncC2SPayload(List<String> trashItems) implements CustomPayload {
    public static final CustomPayload.Id<TrashListSyncC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "trash_list_sync"));
    public static final PacketCodec<RegistryByteBuf, TrashListSyncC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeVarInt(payload.trashItems.size());
                for (String item : payload.trashItems) {
                    buf.writeString(item);
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<String> items = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    items.add(buf.readString());
                }
                return new TrashListSyncC2SPayload(items);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
