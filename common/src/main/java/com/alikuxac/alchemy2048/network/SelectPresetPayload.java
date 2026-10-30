package com.alikuxac.alchemy2048.network;

import com.alikuxac.alchemy2048.Alchemy2048Common;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectPresetPayload(ResourceLocation recipeId, int slotIndex) implements CustomPacketPayload {
    public static final Type<SelectPresetPayload> TYPE = new Type<>(Alchemy2048Common.id("select_preset"));

    public static final StreamCodec<ByteBuf, SelectPresetPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            SelectPresetPayload::recipeId,
            ByteBufCodecs.VAR_INT,
            SelectPresetPayload::slotIndex,
            SelectPresetPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
