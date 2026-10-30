package com.alikuxac.alchemy2048.block.entity;

import org.jetbrains.annotations.Nullable;

import java.util.function.IntSupplier;

public class AlchemyEnergyStorage {
    @Nullable
    private final Runnable onChange;
    private final IntSupplier capacity;
    private final IntSupplier maxReceive;
    private int energy;

    public AlchemyEnergyStorage(IntSupplier capacity, IntSupplier maxReceive, @Nullable Runnable onChange) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.onChange = onChange;
    }

    public AlchemyEnergyStorage(int capacity, int maxReceive, @Nullable Runnable onChange) {
        this(() -> capacity, () -> maxReceive, onChange);
    }

    public int receiveEnergy(int maxReceive, boolean simulate) {
        int cap = getMaxEnergyStored();
        if (energy > cap) {
            energy = cap;
        }
        int received = Math.min(cap - energy, Math.min(this.maxReceive.getAsInt(), maxReceive));
        if (received <= 0) {
            return 0;
        }
        if (!simulate) {
            energy += received;
            notifyChanged();
        }
        return received;
    }

    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    public int getEnergyStored() {
        int cap = getMaxEnergyStored();
        return Math.min(energy, cap);
    }

    public int getMaxEnergyStored() {
        return capacity.getAsInt();
    }

    public boolean canReceive() {
        return maxReceive.getAsInt() > 0;
    }

    public boolean canExtract() {
        return false;
    }

    public boolean consume(int amount) {
        if (energy > getMaxEnergyStored()) {
            energy = getMaxEnergyStored();
        }
        if (amount <= 0) {
            return true;
        }
        if (this.energy >= amount) {
            this.energy -= amount;
            return true;
        }
        return false;
    }

    public void setEnergy(int energy) {
        this.energy = Math.clamp(energy, 0, getMaxEnergyStored());
        notifyChanged();
    }

    public int getCapacity() {
        return getMaxEnergyStored();
    }

    private void notifyChanged() {
        if (onChange != null) {
            onChange.run();
        }
    }
}