package com.alikuxac.alchemy2048.core;

import java.util.Random;

public final class Grid2048Logic {
    public static final int SIZE = 4;
    public static final int CELL_COUNT = SIZE * SIZE;
    public static final byte WIN_TIER = 11;
    public static final int WIN_VALUE = 2048;

    private Grid2048Logic() {}

    public static boolean hasWon(byte[] grid) {
        return getMaxTier(grid) >= WIN_TIER;
    }

    public static boolean slideAndMerge(byte[] grid, Direction2048 direction) {
        if (grid == null || grid.length != CELL_COUNT) {
            return false;
        }

        boolean changed = false;
        byte[] line = new byte[SIZE];

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                int index = getGridIndex(direction, i, j);
                line[j] = grid[index];
            }

            if (processLine(line)) {
                changed = true;
            }

            for (int j = 0; j < SIZE; j++) {
                int index = getGridIndex(direction, i, j);
                grid[index] = line[j];
            }
        }

        return changed;
    }

    private static boolean processLine(byte[] line) {
        boolean changed = false;

        int insertPos = 0;
        for (int i = 0; i < SIZE; i++) {
            if (line[i] != 0) {
                if (insertPos != i) {
                    line[insertPos] = line[i];
                    line[i] = 0;
                    changed = true;
                }
                insertPos++;
            }
        }

        for (int i = 0; i < SIZE - 1; i++) {
            if (line[i] != 0 && line[i] == line[i + 1]) {
                line[i] = (byte) Math.min(126, line[i] + 1);

                for (int j = i + 1; j < SIZE - 1; j++) {
                    line[j] = line[j + 1];
                }
                line[SIZE - 1] = 0;
                changed = true;
            }
        }

        return changed;
    }

    private static int getGridIndex(Direction2048 direction, int line, int step) {
        return switch (direction) {
            case LEFT -> line * SIZE + step;
            case RIGHT -> line * SIZE + (SIZE - 1 - step);
            case UP -> step * SIZE + line;
            case DOWN -> (SIZE - 1 - step) * SIZE + line;
        };
    }

    public static int spawnRandomTile(byte[] grid, Random random) {
        int emptyCount = 0;
        for (int i = 0; i < CELL_COUNT; i++) {
            if (grid[i] == 0) {
                emptyCount++;
            }
        }

        if (emptyCount == 0) {
            return -1;
        }

        int targetIndex = random.nextInt(emptyCount);
        int currentEmpty = 0;
        for (int i = 0; i < CELL_COUNT; i++) {
            if (grid[i] == 0) {
                if (currentEmpty == targetIndex) {
                    byte value = (byte) (random.nextFloat() < 0.9f ? 1 : 2);
                    grid[i] = value;
                    return i;
                }
                currentEmpty++;
            }
        }

        return -1;
    }

    public static boolean canMove(byte[] grid) {
        if (grid == null || grid.length != CELL_COUNT) {
            return false;
        }

        for (int i = 0; i < CELL_COUNT; i++) {
            if (grid[i] == 0) {
                return true;
            }
        }

        for (int row = 0; row < SIZE; row++) {
            int rowOffset = row * SIZE;
            for (int col = 0; col < SIZE - 1; col++) {
                if (grid[rowOffset + col] == grid[rowOffset + col + 1]) {
                    return true;
                }
            }
        }

        for (int col = 0; col < SIZE; col++) {
            for (int row = 0; row < SIZE - 1; row++) {
                if (grid[row * SIZE + col] == grid[(row + 1) * SIZE + col]) {
                    return true;
                }
            }
        }

        return false;
    }

    public static byte getMaxTier(byte[] grid) {
        if (grid == null) {
            return 0;
        }
        byte max = 0;
        for (byte b : grid) {
            if (b > max) {
                max = b;
            }
        }
        return max;
    }

    public static int calculateScore(byte[] grid) {
        if (grid == null) {
            return 0;
        }
        int score = 0;
        for (byte b : grid) {
            if (b > 0) {
                score += (1 << b);
            }
        }
        return score;
    }
}

