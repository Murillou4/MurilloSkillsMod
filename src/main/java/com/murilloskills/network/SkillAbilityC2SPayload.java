package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Client -> Server: Jogador apertou a tecla da habilidade (Z)
// Generic Packet: O server decide qual skill ativar
public record SkillAbilityC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<SkillAbilityC2SPayload> ID = new CustomPayload.Id<>(Identifier.of(MurilloSkills.MOD_ID, "skill_ability_activate"));
    public static final PacketCodec<RegistryByteBuf, SkillAbilityC2SPayload> CODEC = PacketCodec.unit(new SkillAbilityC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}