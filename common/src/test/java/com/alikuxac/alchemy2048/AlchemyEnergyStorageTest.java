package com.alikuxac.alchemy2048;

import com.alikuxac.alchemy2048.block.entity.AlchemyEnergyStorage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AlchemyEnergyStorageTest {

    @Test
    void testCapacityFollowsSupplier() {
        int[] capacity = {1000};
        int[] maxReceive = {100};
        AlchemyEnergyStorage storage = new AlchemyEnergyStorage(() -> capacity[0], () -> maxReceive[0], null);

        assertEquals(1000, storage.getMaxEnergyStored());
        assertEquals(100, storage.receiveEnergy(500, false));

        capacity[0] = 100_000;
        assertEquals(100_000, storage.getMaxEnergyStored(), "Capacity must follow the supplier live");

        maxReceive[0] = 0;
        assertFalse(storage.canReceive(), "Receive rate must follow the supplier live");
    }

    @Test
    void testClampsStoredEnergyWhenCapacityShrinks() {
        int[] capacity = {1000};
        AlchemyEnergyStorage storage = new AlchemyEnergyStorage(() -> capacity[0], () -> 1000, null);
        storage.receiveEnergy(800, false);
        assertEquals(800, storage.getEnergyStored());

        capacity[0] = 500;
        assertEquals(500, storage.getEnergyStored(), "Stored energy must never exceed the new capacity");
        assertEquals(0, storage.receiveEnergy(10, false));
    }

    @Test
    void testFixedConstructorStillWorks() {
        AlchemyEnergyStorage storage = new AlchemyEnergyStorage(200, 50, null);
        assertEquals(200, storage.getMaxEnergyStored());
        assertEquals(50, storage.receiveEnergy(100, false));
        assertTrue(storage.consume(50));
        assertEquals(0, storage.getEnergyStored());
    }
}