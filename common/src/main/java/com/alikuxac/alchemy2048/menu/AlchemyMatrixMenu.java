package com.alikuxac.alchemy2048.menu;

import com.alikuxac.alchemy2048.block.entity.AlchemyMatrixBlockEntity;
import com.alikuxac.alchemy2048.core.Grid2048Logic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class AlchemyMatrixMenu extends AbstractContainerMenu {
    public static Supplier<MenuType<AlchemyMatrixMenu>> TYPE;

    private final ContainerData data;
    private final Container starterContainer;
    @Nullable
    private final AlchemyMatrixBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public AlchemyMatrixMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainerData(23));
    }

    public AlchemyMatrixMenu(int containerId, Inventory playerInventory, @Nullable AlchemyMatrixBlockEntity blockEntity, ContainerData data) {
        super(TYPE.get(), containerId);
        checkContainerDataCount(data, 23);
        this.data = data;
        this.blockEntity = blockEntity;
        this.access = blockEntity != null && blockEntity.getLevel() != null
                ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                : ContainerLevelAccess.NULL;

        this.starterContainer = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                AlchemyMatrixBlockEntity be = AlchemyMatrixMenu.this.blockEntity;
                if (be != null && !ItemStack.matches(getItem(0), be.getStarterStack())) {
                    be.setStarterStack(getItem(0));
                }
            }
        };

        if (blockEntity != null) {
            starterContainer.setItem(0, blockEntity.getStarterStack());
        }

        this.addSlot(new Slot(starterContainer, 0, -999, -999) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        int playerInvY = 118;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }

        int hotbarY = 176;
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }

        this.addDataSlots(data);
    }

    @Nullable
    public AlchemyMatrixBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public byte getGridCell(int index) {
        if (index >= 0 && index < Grid2048Logic.CELL_COUNT) {
            return (byte) data.get(index);
        }
        return 0;
    }

    public boolean isGridEmpty() {
        if (blockEntity != null) {
            return blockEntity.isGridEmpty();
        }
        for (int i = 0; i < Grid2048Logic.CELL_COUNT; i++) {
            if (getGridCell(i) != 0) return false;
        }
        return true;
    }

    public int getScore() {
        return data.get(16);
    }

    public boolean isGameOver() {
        return data.get(17) != 0;
    }

    public int getEnergy() {
        return (data.get(18) & 0xFFFF) | ((data.get(19) & 0xFFFF) << 16);
    }

    public int getMaxEnergy() {
        return (data.get(20) & 0xFFFF) | ((data.get(21) & 0xFFFF) << 16);
    }

    public int getNextMoveCost() {
        return data.get(22);
    }

    public boolean hasEnoughEnergy() {
        return getEnergy() >= getNextMoveCost();
    }

    public ItemStack getStarterStack() {
        if (blockEntity != null) {
            return blockEntity.getStarterStack();
        }
        return starterContainer.getItem(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index == 0) {
                return ItemStack.EMPTY;
            } else if (index >= 1 && index <= 27) {
                if (!this.moveItemStackTo(stackInSlot, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 28 && index <= 36) {
                if (!this.moveItemStackTo(stackInSlot, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemstack;
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null && !ItemStack.matches(starterContainer.getItem(0), blockEntity.getStarterStack())) {
            starterContainer.setItem(0, blockEntity.getStarterStack());
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity != null) {
            BlockPos pos = blockEntity.getBlockPos();
            boolean inRange = player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
            return inRange && blockEntity.canPlayerAccess(player);
        }
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (blockEntity != null) {
            blockEntity.onPlayerClosedMenu(player);
        }
    }
}
