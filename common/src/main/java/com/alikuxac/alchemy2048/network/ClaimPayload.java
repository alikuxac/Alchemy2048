package com.alikuxac.alchemy2048.network;

import com.alikuxac.alchemy2048.Alchemy2048Common;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClaimPayload(byte mode) implements CustomPacketPayload {
    public static final byte MODE_HIGHEST = 0;
    public static final byte MODE_ALL = 1;

    public static final Type<ClaimPayload> TYPE = new Type<>(Alchemy2048Common.id("claim"));

    public static final StreamCodec<ByteBuf, ClaimPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE,
            ClaimPayload::mode,
            ClaimPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
