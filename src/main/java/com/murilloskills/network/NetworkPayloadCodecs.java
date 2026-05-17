package com.murilloskills.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

final class NetworkPayloadCodecs {
    static final PacketCodec<RegistryByteBuf, Boolean> BOOLEAN = PacketCodec.ofStatic(
            (buf, value) -> buf.writeBoolean(value.booleanValue()),
            RegistryByteBuf::readBoolean);

    private NetworkPayloadCodecs() {
    }
}
