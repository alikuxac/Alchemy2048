package com.alikuxac.alchemy2048.neoforge.energy;

import com.alikuxac.alchemy2048.block.entity.AlchemyEnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class NeoForgeEnergyStorage implements IEnergyStorage {
    private final AlchemyEnergyStorage delegate;

    public NeoForgeEnergyStorage(AlchemyEnergyStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return delegate.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return delegate.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return delegate.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return delegate.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return delegate.canReceive();
    }
}