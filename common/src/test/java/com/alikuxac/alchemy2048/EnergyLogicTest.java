package com.alikuxac.alchemy2048;

import com.alikuxac.alchemy2048.core.EnergyLogic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnergyLogicTest {
    @Test
    void testMoveCostScaling() {
        assertEquals(20, EnergyLogic.calculateMoveCost((byte) 0));
        assertEquals(20, EnergyLogic.calculateMoveCost((byte) 1));
        assertEquals(40, EnergyLogic.calculateMoveCost((byte) 2));
        assertEquals(80, EnergyLogic.calculateMoveCost((byte) 3));
        assertEquals(160, EnergyLogic.calculateMoveCost((byte) 4));
        assertEquals(320, EnergyLogic.calculateMoveCost((byte) 5));
        assertEquals(640, EnergyLogic.calculateMoveCost((byte) 6));
        assertEquals(1280, EnergyLogic.calculateMoveCost((byte) 7));
        assertEquals(2560, EnergyLogic.calculateMoveCost((byte) 8));
        assertEquals(5120, EnergyLogic.calculateMoveCost((byte) 9));
        assertEquals(10240, EnergyLogic.calculateMoveCost((byte) 10));
        assertEquals(20480, EnergyLogic.calculateMoveCost((byte) 11));
    }

    @Test
    void testMoveCostDoesNotExceedCapacity() {
        int costTier20 = EnergyLogic.calculateMoveCost((byte) 20);
        assertTrue(costTier20 <= EnergyLogic.CAPACITY);
        assertEquals(EnergyLogic.CAPACITY, costTier20);

        int costTier100 = EnergyLogic.calculateMoveCost((byte) 100);
        assertEquals(EnergyLogic.CAPACITY, costTier100);
    }
}
