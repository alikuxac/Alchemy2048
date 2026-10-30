package com.alikuxac.alchemy2048;

import com.alikuxac.alchemy2048.core.Direction2048;
import com.alikuxac.alchemy2048.core.Grid2048Logic;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class Grid2048LogicTest {
    @Test
    void testSlideLeftSimple() {
        byte[] grid = new byte[16];
        grid[0] = 1;
        grid[1] = 1;
        grid[2] = 0;
        grid[3] = 0;

        boolean changed = Grid2048Logic.slideAndMerge(grid, Direction2048.LEFT);
        assertTrue(changed);
        assertEquals(2, grid[0], "1 + 1 should merge into tier 2");
        assertEquals(0, grid[1]);
        assertEquals(0, grid[2]);
        assertEquals(0, grid[3]);
    }

    @Test
    void testSingleMergePerMoveConstraint() {
        byte[] grid = new byte[16];
        grid[0] = 1;
        grid[1] = 1;
        grid[2] = 1;
        grid[3] = 1;

        boolean changed = Grid2048Logic.slideAndMerge(grid, Direction2048.LEFT);
        assertTrue(changed);
        assertEquals(2, grid[0]);
        assertEquals(2, grid[1]);
        assertEquals(0, grid[2]);
        assertEquals(0, grid[3]);
    }

    @Test
    void testSlideRightWithGap() {
        byte[] grid = new byte[16];
        grid[0] = 1;
        grid[1] = 0;
        grid[2] = 0;
        grid[3] = 1;

        boolean changed = Grid2048Logic.slideAndMerge(grid, Direction2048.RIGHT);
        assertTrue(changed);
        assertEquals(0, grid[0]);
        assertEquals(0, grid[1]);
        assertEquals(0, grid[2]);
        assertEquals(2, grid[3]);
    }

    @Test
    void testSlideDown() {
        byte[] grid = new byte[16];
        grid[0] = 2;
        grid[4] = 0;
        grid[8] = 2;
        grid[12] = 0;

        boolean changed = Grid2048Logic.slideAndMerge(grid, Direction2048.DOWN);
        assertTrue(changed);
        assertEquals(0, grid[0]);
        assertEquals(0, grid[4]);
        assertEquals(0, grid[8]);
        assertEquals(3, grid[12], "2 + 2 moving down should merge to 3 at bottom");
    }

    @Test
    void testSlideUp() {
        byte[] grid = new byte[16];
        grid[12] = 3;
        grid[8] = 3;

        boolean changed = Grid2048Logic.slideAndMerge(grid, Direction2048.UP);
        assertTrue(changed);
        assertEquals(4, grid[0], "3 + 3 moving up should merge to 4 at top");
        assertEquals(0, grid[4]);
        assertEquals(0, grid[8]);
        assertEquals(0, grid[12]);
    }

    @Test
    void testNoMovePossibleWhenFullAndNoMatches() {
        byte[] grid = new byte[]{
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 16
        };

        assertFalse(Grid2048Logic.canMove(grid));
        assertFalse(Grid2048Logic.slideAndMerge(grid, Direction2048.LEFT));
        assertFalse(Grid2048Logic.slideAndMerge(grid, Direction2048.RIGHT));
        assertFalse(Grid2048Logic.slideAndMerge(grid, Direction2048.UP));
        assertFalse(Grid2048Logic.slideAndMerge(grid, Direction2048.DOWN));
    }

    @Test
    void testCanMoveWhenMatchesExist() {
        byte[] grid = new byte[]{
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 15
        };

        assertTrue(Grid2048Logic.canMove(grid));
    }

    @Test
    void testDirectionFromIdRejectsInvalid() {
        assertEquals(Direction2048.UP, Direction2048.fromId((byte) 0));
        assertEquals(Direction2048.DOWN, Direction2048.fromId((byte) 1));
        assertEquals(Direction2048.LEFT, Direction2048.fromId((byte) 2));
        assertEquals(Direction2048.RIGHT, Direction2048.fromId((byte) 3));
        assertNull(Direction2048.fromId((byte) 4), "Forged direction id must be rejected");
        assertNull(Direction2048.fromId((byte) -1));
    }

    @Test
    void testSpawnRandomTile() {
        byte[] grid = new byte[16];
        Random random = new Random(42);
        int spawned = Grid2048Logic.spawnRandomTile(grid, random);

        assertTrue(spawned >= 0 && spawned < 16);
        assertTrue(grid[spawned] == 1 || grid[spawned] == 2);
    }
}
