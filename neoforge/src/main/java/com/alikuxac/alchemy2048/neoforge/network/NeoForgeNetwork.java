package com.alikuxac.alchemy2048.neoforge.network;

import com.alikuxac.alchemy2048.Alchemy2048Common;
import com.alikuxac.alchemy2048.core.Direction2048;
import com.alikuxac.alchemy2048.menu.AlchemyMatrixMenu;
import com.alikuxac.alchemy2048.network.ClaimPayload;
import com.alikuxac.alchemy2048.network.MoveGridPayload;
import com.alikuxac.alchemy2048.network.SelectPresetPayload;
import com.alikuxac.alchemy2048.recipe.EvolutionTreeRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForgeNetwork {
    private NeoForgeNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(Alchemy2048Common.MOD_ID).versioned("1.0.0");

        registrar.playToServer(
                MoveGridPayload.TYPE,
                MoveGridPayload.STREAM_CODEC,
                NeoForgeNetwork::handleMoveGrid
        );

        registrar.playToServer(
                ClaimPayload.TYPE,
                ClaimPayload.STREAM_CODEC,
                NeoForgeNetwork::handleClaim
        );

        registrar.playToServer(
                SelectPresetPayload.TYPE,
                SelectPresetPayload.STREAM_CODEC,
                NeoForgeNetwork::handleSelectPreset
        );
    }

    private static void handleMoveGrid(MoveGridPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.containerMenu instanceof AlchemyMatrixMenu menu && menu.getBlockEntity() != null) {
                    var direction = Direction2048.fromId(payload.direction());
                    if (direction != null) {
                        menu.getBlockEntity().applyMove(direction, player);
                    }
                }
            }
        });
    }

    private static void handleClaim(ClaimPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.containerMenu instanceof AlchemyMatrixMenu menu && menu.getBlockEntity() != null) {
                    var blockEntity = menu.getBlockEntity();
                    if (payload.mode() == ClaimPayload.MODE_HIGHEST) {
                        blockEntity.claimHighest(player);
                    } else if (payload.mode() == ClaimPayload.MODE_ALL) {
                        if (!blockEntity.isGridEmpty()) {
                            blockEntity.claimAll(player);
                        }
                    }
                    menu.getSlot(0).set(blockEntity.getStarterStack());
                    menu.broadcastChanges();
                }
            }
        });
    }

    private static void handleSelectPreset(SelectPresetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.containerMenu instanceof AlchemyMatrixMenu menu && menu.getBlockEntity() != null) {
                    var blockEntity = menu.getBlockEntity();
                    if (blockEntity.isGridEmpty() && blockEntity.canPlayerAccess(player)) {
                        if (blockEntity.getEnergyStorage().getEnergyStored() < com.alikuxac.alchemy2048.core.EnergyLogic.getBaseCost()) {
                            player.displayClientMessage(Component.translatable(
                                    "message.alchemy2048.not_enough_fe",
                                    com.alikuxac.alchemy2048.core.EnergyLogic.getBaseCost()), true);
                            return;
                        }
                        var inv = player.getInventory();
                        var manager = player.serverLevel().getRecipeManager();
                        var selected = manager.byKey(payload.recipeId()).orElse(null);
                        if (selected == null || !(selected.value() instanceof EvolutionTreeRecipe recipe)
                                || !recipe.isValid()) {
                            return;
                        }

                        int slot = payload.slotIndex();
                        ItemStack stack = (slot >= 0 && slot < inv.items.size()) ? inv.getItem(slot) : ItemStack.EMPTY;
                        if (stack.isEmpty() || !recipe.starter().test(stack)) {
                            slot = -1;
                            for (int i = 0; i < 36; i++) {
                                ItemStack s = inv.getItem(i);
                                if (!s.isEmpty() && recipe.starter().test(s)) {
                                    slot = i;
                                    stack = s;
                                    break;
                                }
                            }
                        }

                        if (slot < 0 || stack.isEmpty() || !recipe.starter().test(stack)) {
                            return;
                        }

                        var taken = inv.removeItem(slot, 1);
                        if (taken.isEmpty()) {
                            return;
                        }

                        int menuSlotIndex = (slot < 9) ? (28 + slot) : (slot - 8);
                        if (menuSlotIndex >= 0 && menuSlotIndex < menu.slots.size()) {
                            menu.getSlot(menuSlotIndex).setChanged();
                        }

                        blockEntity.setStarterStack(taken);
                        menu.getSlot(0).set(taken);
                        menu.broadcastChanges();
                        player.inventoryMenu.broadcastChanges();
                    }
                }
            }
        });
    }
}
