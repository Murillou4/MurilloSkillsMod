package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Client -> Server: Jogador clicou no botão para tornar essa skill sua Paragon
public record ParagonActivationC2SPayload(MurilloSkillsList skill) implements CustomPayload {
    public static final CustomPayload.Id<ParagonActivationC2SPayload> ID = new CustomPayload.Id<>(Identifier.of(MurilloSkills.MOD_ID, "paragon_activation"));

    public static final PacketCodec<RegistryByteBuf, ParagonActivationC2SPayload> CODEC = PacketCodec.tuple(
            PacketCodec.ofStatic(
                    PacketByteBuf::writeEnumConstant, // CORRIGIDO: 'val' já é o Enum
                    (buf) -> buf.readEnumConstant(MurilloSkillsList.class) // CORRIGIDO: Retorna apenas o Enum
            ),
            ParagonActivationC2SPayload::skill,
            ParagonActivationC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}