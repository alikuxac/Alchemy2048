package com.alikuxac.alchemy2048.forge.block.entity;

import com.alikuxac.alchemy2048.block.entity.AlchemyEnergyStorage;
import com.alikuxac.alchemy2048.block.entity.AlchemyMatrixBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ForgeAlchemyMatrixBlockEntity extends AlchemyMatrixBlockEntity implements ICapabilityProvider {
    private final ForgeEnergyStorage forgeEnergy;
    private final LazyOptional<ForgeEnergyStorage> energyOptional;

    public ForgeAlchemyMatrixBlockEntity(BlockPos pos, BlockState blockState) {
        super(pos, blockState);
        this.forgeEnergy = new ForgeEnergyStorage(getEnergyStorage());
        this.energyOptional = LazyOptional.of(() -> forgeEnergy);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyOptional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOptional.invalidate();
    }

    private static final class ForgeEnergyStorage implements IEnergyStorage {
        private final AlchemyEnergyStorage delegate;

        ForgeEnergyStorage(AlchemyEnergyStorage delegate) {
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
}