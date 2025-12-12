package com.murilloskills.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-to-Client payload to sync Daily Challenges data.
 * Sent on login and when challenges are updated.
 */
public record DailyChallengesSyncS2CPayload(
        List<ChallengeData> challenges,
        String dateKey,
        boolean allComplete) implements CustomPayload {

    public static final CustomPayload.Id<DailyChallengesSyncS2CPayload> ID = new CustomPayload.Id<>(
            Identifier.of("murilloskills", "daily_challenges_sync"));

    public static final PacketCodec<RegistryByteBuf, DailyChallengesSyncS2CPayload> CODEC = new PacketCodec<>() {
        @Override
        public DailyChallengesSyncS2CPayload decode(RegistryByteBuf buf) {
            int count = buf.readVarInt();
            List<ChallengeData> challenges = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String type = buf.readString();
                String skillName = buf.readString();
                int target = buf.readVarInt();
                int progress = buf.readVarInt();
                boolean completed = buf.readBoolean();
                int xpReward = buf.readVarInt();
                challenges.add(new ChallengeData(type, skillName, target, progress, completed, xpReward));
            }
            String dateKey = buf.readString();
            boolean allComplete = buf.readBoolean();
            return new DailyChallengesSyncS2CPayload(challenges, dateKey, allComplete);
        }

        @Override
        public void encode(RegistryByteBuf buf, DailyChallengesSyncS2CPayload payload) {
            buf.writeVarInt(payload.challenges.size());
            for (ChallengeData challenge : payload.challenges) {
                buf.writeString(challenge.type);
                buf.writeString(challenge.skillName);
                buf.writeVarInt(challenge.target);
                buf.writeVarInt(challenge.progress);
                buf.writeBoolean(challenge.completed);
                buf.writeVarInt(challenge.xpReward);
            }
            buf.writeString(payload.dateKey);
            buf.writeBoolean(payload.allComplete);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /**
     * Individual challenge data for client display.
     */
    public record ChallengeData(
            String type, // e.g., "MINE_BLOCKS"
            String skillName, // e.g., "MINER" or empty
            int target, // Total needed
            int progress, // Current progress
            boolean completed, // Is done?
            int xpReward // XP reward on completion
    ) {
    }
}
