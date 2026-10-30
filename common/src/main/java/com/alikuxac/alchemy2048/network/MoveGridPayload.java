package com.alikuxac.alchemy2048.network;

import com.alikuxac.alchemy2048.Alchemy2048Common;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MoveGridPayload(byte direction) implements CustomPacketPayload {
    public static final Type<MoveGridPayload> TYPE = new Type<>(Alchemy2048Common.id("move_grid"));

    public static final StreamCodec<ByteBuf, MoveGridPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE,
            MoveGridPayload::direction,
            MoveGridPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
