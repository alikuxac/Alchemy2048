package com.alikuxac.alchemy2048.forge.network;

import com.alikuxac.alchemy2048.Alchemy2048Common;
import com.alikuxac.alchemy2048.core.Direction2048;
import com.alikuxac.alchemy2048.menu.AlchemyMatrixMenu;
import com.alikuxac.alchemy2048.network.ClaimPayload;
import com.alikuxac.alchemy2048.network.MoveGridPayload;
import com.alikuxac.alchemy2048.network.SelectPresetPayload;
import com.alikuxac.alchemy2048.recipe.EvolutionTreeRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;

public final class ForgeNetwork {
    private ForgeNetwork() {}

    public static void register() {
        ChannelBuilder.named(Alchemy2048Common.id("main"))
                .payloadChannel()
                .play().flow(PacketFlow.SERVERBOUND)
                .addMain(MoveGridPayload.TYPE, codec(MoveGridPayload.STREAM_CODEC), ForgeNetwork::handleMoveGrid)
                .addMain(ClaimPayload.TYPE, codec(ClaimPayload.STREAM_CODEC), ForgeNetwork::handleClaim)
                .addMain(SelectPresetPayload.TYPE, codec(SelectPresetPayload.STREAM_CODEC), ForgeNetwork::handleSelectPreset)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static <B extends FriendlyByteBuf, V> StreamCodec<B, V> codec(StreamCodec<? super B, V> codec) {
        return (StreamCodec<B, V>) codec;
    }

    private static void handleMoveGrid(MoveGridPayload payload, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof AlchemyMatrixMenu menu && menu.getBlockEntity() != null) {
                var direction = Direction2048.fromId(payload.direction());
                if (direction != null) {
                    menu.getBlockEntity().applyMove(direction, player);
                }
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleClaim(ClaimPayload payload, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof AlchemyMatrixMenu menu && menu.getBlockEntity() != null) {
                if (payload.mode() == ClaimPayload.MODE_HIGHEST) {
                    menu.getBlockEntity().claimHighest(player);
                } else if (payload.mode() == ClaimPayload.MODE_ALL) {
                    menu.getBlockEntity().claimAll(player);
                }
                menu.getSlot(0).set(menu.getBlockEntity().getStarterStack());
                menu.broadcastChanges();
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleSelectPreset(SelectPresetPayload payload, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof AlchemyMatrixMenu menu && menu.getBlockEntity() != null) {
                var blockEntity = menu.getBlockEntity();
                if (blockEntity.isGridEmpty() && blockEntity.canPlayerAccess(player)) {
                    if (blockEntity.getEnergyStorage().getEnergyStored() < com.alikuxac.alchemy2048.core.EnergyLogic.getBaseCost()) {
                        player.displayClientMessage(Component.translatable(
                                "message.alchemy2048.not_enough_fe",
                                com.alikuxac.alchemy2048.core.EnergyLogic.getBaseCost()), true);
                        return;
                    }
                    var inv = player.getInventory();
                    int slot = payload.slotIndex();
                    if (slot < 0 || slot >= inv.items.size()) {
                        return;
                    }
                    var stack = inv.getItem(slot);
                    if (stack.isEmpty()) {
                        return;
                    }
                    var selected = player.serverLevel().getRecipeManager().byKey(payload.recipeId()).orElse(null);
                    if (selected == null || !(selected.value() instanceof EvolutionTreeRecipe recipe)
                            || !recipe.isValid() || !recipe.starter().test(stack)) {
                        return;
                    }
                    var oldStarter = blockEntity.getStarterStack().copy();
                    if (!oldStarter.isEmpty()) {
                        if (!player.getInventory().add(oldStarter)) {
                            player.drop(oldStarter, false);
                        }
                    }
                    var taken = stack.split(1);
                    blockEntity.setStarterStack(taken);
                    menu.getSlot(0).set(taken);
                    menu.broadcastChanges();
                }
            }
        });
        context.setPacketHandled(true);
    }
}