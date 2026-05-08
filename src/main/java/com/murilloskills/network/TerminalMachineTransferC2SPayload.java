package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Client -> Server: moves an amount of an item from the player's bound Tom's
 * Storage wireless terminal into the machine they targeted.
 */
public record TerminalMachineTransferC2SPayload(ItemStack itemKey, int amount, BlockPos targetPos, Direction face)
        implements CustomPayload {
    public static final Id<TerminalMachineTransferC2SPayload> ID = new Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "terminal_machine_transfer"));

    public static final PacketCodec<RegistryByteBuf, TerminalMachineTransferC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                ItemStack.PACKET_CODEC.encode(buf, payload.itemKey);
                buf.writeVarInt(payload.amount);
                buf.writeBlockPos(payload.targetPos);
                buf.writeEnumConstant(payload.face);
            },
            buf -> new TerminalMachineTransferC2SPayload(
                    ItemStack.PACKET_CODEC.decode(buf),
                    buf.readVarInt(),
                    buf.readBlockPos(),
                    buf.readEnumConstant(Direction.class)));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
