package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Client -> Server: Player syncs their storage routing whitelist (item ids).
 */
public record StorageWhitelistSyncC2SPayload(List<String> items) implements CustomPayload {
    public static final CustomPayload.Id<StorageWhitelistSyncC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "storage_whitelist_sync"));
    public static final PacketCodec<RegistryByteBuf, StorageWhitelistSyncC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeVarInt(payload.items.size());
                for (String item : payload.items) {
                    buf.writeString(item);
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<String> items = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    items.add(buf.readString());
                }
                return new StorageWhitelistSyncC2SPayload(items);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
