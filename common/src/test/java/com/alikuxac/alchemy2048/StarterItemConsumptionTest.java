package com.alikuxac.alchemy2048;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StarterItemConsumptionTest {

    record MockSlotItem(String id, int count) {}

    private static int toMenuSlotIndex(int inventorySlot) {
        return (inventorySlot < 9) ? (28 + inventorySlot) : (inventorySlot - 8);
    }

    private static int findStarterSlot(List<MockSlotItem> inventory, int requestedSlot, String starterId) {
        if (requestedSlot >= 0 && requestedSlot < inventory.size()) {
            MockSlotItem item = inventory.get(requestedSlot);
            if (item != null && item.count() > 0 && starterId.equals(item.id())) {
                return requestedSlot;
            }
        }
        for (int i = 0; i < inventory.size(); i++) {
            MockSlotItem item = inventory.get(i);
            if (item != null && item.count() > 0 && starterId.equals(item.id())) {
                return i;
            }
        }
        return -1;
    }

    private static MockSlotItem removeItem(List<MockSlotItem> inventory, int slot, int amount) {
        if (slot < 0 || slot >= inventory.size()) return null;
        MockSlotItem current = inventory.get(slot);
        if (current == null || current.count() <= 0) return null;

        int toTake = Math.min(amount, current.count());
        int remaining = current.count() - toTake;
        if (remaining <= 0) {
            inventory.set(slot, null);
        } else {
            inventory.set(slot, new MockSlotItem(current.id(), remaining));
        }
        return new MockSlotItem(current.id(), toTake);
    }

    @Test
    void testMenuSlotMapping() {
        // Hotbar slots (0..8) map to menu slots 28..36
        assertEquals(28, toMenuSlotIndex(0));
        assertEquals(36, toMenuSlotIndex(8));

        // Main inventory slots (9..35) map to menu slots 1..27
        assertEquals(1, toMenuSlotIndex(9));
        assertEquals(27, toMenuSlotIndex(35));
    }

    @Test
    void testSingleItemDeductionLeavesSlotClean() {
        List<MockSlotItem> inv = new ArrayList<>();
        for (int i = 0; i < 36; i++) inv.add(null);
        inv.set(5, new MockSlotItem("minecraft:cobblestone", 1));

        int targetSlot = findStarterSlot(inv, 5, "minecraft:cobblestone");
        assertEquals(5, targetSlot);

        MockSlotItem taken = removeItem(inv, targetSlot, 1);
        assertNotNull(taken);
        assertEquals(1, taken.count());
        assertEquals("minecraft:cobblestone", taken.id());

        // Slot should be cleanly empty now
        assertNull(inv.get(5), "Slot must be cleanly set to empty when last item is consumed");
    }

    @Test
    void testStackDeductionDecrementsProperly() {
        List<MockSlotItem> inv = new ArrayList<>();
        for (int i = 0; i < 36; i++) inv.add(null);
        inv.set(12, new MockSlotItem("minecraft:oak_log", 64));

        int targetSlot = findStarterSlot(inv, 12, "minecraft:oak_log");
        assertEquals(12, targetSlot);

        MockSlotItem taken = removeItem(inv, targetSlot, 1);
        assertNotNull(taken);
        assertEquals(1, taken.count());

        assertNotNull(inv.get(12));
        assertEquals(63, inv.get(12).count(), "Stack count must decrement from 64 to 63");
    }

    @Test
    void testFallbackSlotScanningWhenRequestedSlotMoved() {
        List<MockSlotItem> inv = new ArrayList<>();
        for (int i = 0; i < 36; i++) inv.add(null);
        // Item is at slot 15, but payload requested slot 2 (which is empty)
        inv.set(15, new MockSlotItem("minecraft:dirt", 10));

        int targetSlot = findStarterSlot(inv, 2, "minecraft:dirt");
        assertEquals(15, targetSlot, "Fallback scan must locate matching item even if requested slot is empty");

        MockSlotItem taken = removeItem(inv, targetSlot, 1);
        assertNotNull(taken);
        assertEquals(9, inv.get(15).count());
    }

    @Test
    void testMoveDeductionConsumesStarterItemPerMove() {
        List<MockSlotItem> inv = new ArrayList<>();
        for (int i = 0; i < 36; i++) inv.add(null);
        inv.set(0, new MockSlotItem("minecraft:cobblestone", 2));

        // Move 1
        int slot1 = findStarterSlot(inv, -1, "minecraft:cobblestone");
        assertTrue(slot1 >= 0);
        MockSlotItem taken1 = removeItem(inv, slot1, 1);
        assertNotNull(taken1);
        assertEquals(1, inv.get(0).count());

        // Move 2
        int slot2 = findStarterSlot(inv, -1, "minecraft:cobblestone");
        assertTrue(slot2 >= 0);
        MockSlotItem taken2 = removeItem(inv, slot2, 1);
        assertNotNull(taken2);
        assertNull(inv.get(0), "Slot must be empty after 2 moves");

        // Move 3: should be blocked
        int slot3 = findStarterSlot(inv, -1, "minecraft:cobblestone");
        assertEquals(-1, slot3, "Move must be rejected when player runs out of starter items");
    }
}
