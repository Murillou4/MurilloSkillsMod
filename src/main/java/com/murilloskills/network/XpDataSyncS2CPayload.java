package com.murilloskills.network;

import com.murilloskills.data.XpCurveDefinition;
import com.murilloskills.data.XpDataManager;
import com.murilloskills.data.XpValuesDefinition;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record XpDataSyncS2CPayload(String curveJson, String valuesJson) implements CustomPayload {
    public static final CustomPayload.Id<XpDataSyncS2CPayload> ID = new CustomPayload.Id<>(
            Identifier.of("murilloskills", "xp_data_sync"));

    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, XpDataSyncS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, XpDataSyncS2CPayload::curveJson,
            PacketCodecs.STRING, XpDataSyncS2CPayload::valuesJson,
            XpDataSyncS2CPayload::new);

    public static XpDataSyncS2CPayload fromData(XpCurveDefinition curve, XpValuesDefinition values) {
        return new XpDataSyncS2CPayload(XpDataManager.toJson(curve), XpDataManager.toJson(values));
    }
}
