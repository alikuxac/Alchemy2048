package com.alikuxac.alchemy2048;

import com.alikuxac.alchemy2048.core.Direction2048;
import com.alikuxac.alchemy2048.menu.client.TileInterpolationEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TileInterpolationEngineTest {

    @Test
    void testEaseOutQuadInterpolation() {
        TileInterpolationEngine.TileRenderState tile = new TileInterpolationEngine.TileRenderState();
        tile.init(0.0f, 0.0f, (byte) 1);
        tile.startMove(100.0f, 200.0f, (byte) 1, false);

        assertEquals(0.0f, tile.progress, 0.001f);
        assertEquals(0.0f, tile.currentX, 0.001f);
        assertEquals(0.0f, tile.currentY, 0.001f);

        tile.update(0.5f);
        assertEquals(0.5f, tile.progress, 0.001f);
        assertEquals(75.0f, tile.currentX, 0.001f);
        assertEquals(150.0f, tile.currentY, 0.001f);

        tile.update(0.5f);
        assertEquals(1.0f, tile.progress, 0.001f);
        assertEquals(100.0f, tile.currentX, 0.001f);
        assertEquals(200.0f, tile.currentY, 0.001f);
    }

    @Test
    void testMergePopScaling() {
        TileInterpolationEngine.TileRenderState normalTile = new TileInterpolationEngine.TileRenderState();
        normalTile.init(0.0f, 0.0f, (byte) 1);
        normalTile.startMove(50.0f, 50.0f, (byte) 1, false);
        normalTile.update(0.5f);
        assertEquals(1.0f, normalTile.getScale(), 0.001f, "Unmerged tile should not pop");

        TileInterpolationEngine.TileRenderState mergedTile = new TileInterpolationEngine.TileRenderState();
        mergedTile.init(0.0f, 0.0f, (byte) 2);
        mergedTile.startMove(50.0f, 50.0f, (byte) 2, true);

        assertEquals(1.0f, mergedTile.getScale(), 0.001f);
        mergedTile.update(0.5f);
        assertEquals(1.15f, mergedTile.getScale(), 0.001f, "Merged tile must scale to 115% at peak");

        mergedTile.update(0.5f);
        assertEquals(1.0f, mergedTile.getScale(), 0.001f, "Merged tile must settle back to 1.0 at completion");
    }

    @Test
    void testSlideSimulationAndMerge() {
        TileInterpolationEngine engine = new TileInterpolationEngine();
        byte[] grid = new byte[16];
        grid[0] = 1;
        grid[1] = 1;

        TileInterpolationEngine.MoveResult result = engine.onMove(grid, Direction2048.LEFT, 0, 0, 20, 2);
        assertTrue(result.moved(), "Move left should succeed");
        assertEquals(2, result.maxMergedTier(), "Merged tier should be 2");
        assertTrue(engine.isAnimating(), "Engine should be animating after move");

        TileInterpolationEngine.TileRenderState[] pool = engine.getPool();
        int activeCount = 0;
        for (TileInterpolationEngine.TileRenderState state : pool) {
            if (state.active) {
                activeCount++;
                assertEquals(0.0f, state.targetX, 0.001f, "Target X for merge should be 0");
                assertTrue(state.isMerged, "Tile should be marked merged");
                assertEquals((byte) 2, state.tier);
            }
        }
        assertEquals(2, activeCount, "Two participating tiles should animate to target slot");
    }

    @Test
    void testBlockedMoveReturnsFalse() {
        TileInterpolationEngine engine = new TileInterpolationEngine();
        byte[] grid = new byte[16];
        grid[0] = 1;

        TileInterpolationEngine.MoveResult result = engine.onMove(grid, Direction2048.LEFT, 0, 0, 20, 2);
        assertFalse(result.moved(), "Blocked slide left should return false");
        assertEquals(0, result.maxMergedTier());
        assertFalse(engine.isAnimating());
    }
}

