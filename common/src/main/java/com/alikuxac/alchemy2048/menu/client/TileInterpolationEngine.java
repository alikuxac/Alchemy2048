package com.alikuxac.alchemy2048.menu.client;

import com.alikuxac.alchemy2048.core.Direction2048;
import com.alikuxac.alchemy2048.core.Grid2048Logic;

public class TileInterpolationEngine {
    public static final int MAX_TILES = 32;
    public static final float ANIM_DURATION_MS = 110.0f;

    public static class TileRenderState {
        public float currentX;
        public float currentY;
        public float startX;
        public float startY;
        public float targetX;
        public float targetY;
        public float progress = 1.0f;
        public boolean isMerged = false;
        public byte tier = 0;
        public boolean active = false;

        public void init(float x, float y, byte tier) {
            this.currentX = x;
            this.currentY = y;
            this.startX = x;
            this.startY = y;
            this.targetX = x;
            this.targetY = y;
            this.progress = 1.0f;
            this.isMerged = false;
            this.tier = tier;
            this.active = tier > 0;
        }

        public void startMove(float toX, float toY, byte newTier, boolean merged) {
            this.startX = this.currentX;
            this.startY = this.currentY;
            this.targetX = toX;
            this.targetY = toY;
            this.progress = 0.0f;
            this.isMerged = merged;
            this.tier = newTier;
            this.active = true;
        }

        public void update(float delta) {
            if (progress < 1.0f) {
                progress = Math.min(1.0f, progress + delta);
                float t = 1.0f - (1.0f - progress) * (1.0f - progress);
                currentX = startX + (targetX - startX) * t;
                currentY = startY + (targetY - startY) * t;
            }
        }

        public float getScale() {
            if (isMerged && progress < 1.0f) {
                return 1.0f + 0.15f * (float) Math.sin(progress * Math.PI);
            }
            return 1.0f;
        }
    }

    private final TileRenderState[] pool = new TileRenderState[MAX_TILES];
    private final byte[] simGrid = new byte[Grid2048Logic.CELL_COUNT];
    private final int[] fromIndex = new int[Grid2048Logic.CELL_COUNT];
    private final int[] toIndex = new int[Grid2048Logic.CELL_COUNT];
    private final boolean[] merged = new boolean[Grid2048Logic.CELL_COUNT];
    private final byte[] resultTiers = new byte[Grid2048Logic.CELL_COUNT];

    public TileInterpolationEngine() {
        for (int i = 0; i < MAX_TILES; i++) {
            pool[i] = new TileRenderState();
        }
    }

    public TileRenderState[] getPool() {
        return pool;
    }

    public void update(float delta) {
        for (int i = 0; i < MAX_TILES; i++) {
            if (pool[i].active) {
                pool[i].update(delta);
            }
        }
    }

    public boolean isAnimating() {
        for (int i = 0; i < MAX_TILES; i++) {
            if (pool[i].active && pool[i].progress < 1.0f) {
                return true;
            }
        }
        return false;
    }

    public void syncFromGrid(byte[] grid, int gridStartX, int gridStartY, int cellSize, int cellGap) {
        if (isAnimating()) {
            return;
        }
        for (int i = 0; i < MAX_TILES; i++) {
            pool[i].active = false;
        }
        for (int row = 0; row < Grid2048Logic.SIZE; row++) {
            for (int col = 0; col < Grid2048Logic.SIZE; col++) {
                int idx = row * Grid2048Logic.SIZE + col;
                byte tier = grid[idx];
                if (tier > 0) {
                    float x = gridStartX + col * (cellSize + cellGap);
                    float y = gridStartY + row * (cellSize + cellGap);
                    pool[idx].init(x, y, tier);
                }
            }
        }
    }

    public record MoveResult(boolean moved, byte maxMergedTier) {}

    public MoveResult onMove(byte[] currentGrid, Direction2048 direction, int gridStartX, int gridStartY, int cellSize, int cellGap) {
        System.arraycopy(currentGrid, 0, simGrid, 0, Grid2048Logic.CELL_COUNT);
        boolean anyMove = false;
        byte maxMergedTier = 0;
        int activeCount = 0;

        for (int i = 0; i < Grid2048Logic.CELL_COUNT; i++) {
            fromIndex[i] = -1;
            toIndex[i] = -1;
            merged[i] = false;
            resultTiers[i] = 0;
        }

        byte[] line = new byte[Grid2048Logic.SIZE];
        int[] origIndices = new int[Grid2048Logic.SIZE];

        for (int lineIdx = 0; lineIdx < Grid2048Logic.SIZE; lineIdx++) {
            for (int step = 0; step < Grid2048Logic.SIZE; step++) {
                int cellIdx = getGridIndex(direction, lineIdx, step);
                line[step] = simGrid[cellIdx];
                origIndices[step] = cellIdx;
            }

            int insertPos = 0;
            for (int step = 0; step < Grid2048Logic.SIZE; step++) {
                if (line[step] != 0) {
                    int fromIdx = origIndices[step];
                    int toStep = insertPos;
                    int toIdx = getGridIndex(direction, lineIdx, toStep);

                    fromIndex[activeCount] = fromIdx;
                    toIndex[activeCount] = toIdx;
                    resultTiers[activeCount] = line[step];
                    merged[activeCount] = false;

                    if (insertPos != step) {
                        line[insertPos] = line[step];
                        line[step] = 0;
                        anyMove = true;
                    }
                    origIndices[step] = -1;
                    origIndices[insertPos] = fromIdx;
                    insertPos++;
                    activeCount++;
                }
            }

            for (int step = 0; step < Grid2048Logic.SIZE - 1; step++) {
                if (line[step] != 0 && line[step] == line[step + 1]) {
                    byte mergedTier = (byte) Math.min(126, line[step] + 1);
                    line[step] = mergedTier;
                    anyMove = true;
                    if (mergedTier > maxMergedTier) {
                        maxMergedTier = mergedTier;
                    }

                    int targetCell = getGridIndex(direction, lineIdx, step);
                    int collapsedCell = getGridIndex(direction, lineIdx, step + 1);

                    for (int k = 0; k < activeCount; k++) {
                        if (toIndex[k] == targetCell) {
                            merged[k] = true;
                            resultTiers[k] = mergedTier;
                        } else if (toIndex[k] == collapsedCell) {
                            toIndex[k] = targetCell;
                            merged[k] = true;
                            resultTiers[k] = mergedTier;
                        }
                    }

                    for (int j = step + 1; j < Grid2048Logic.SIZE - 1; j++) {
                        line[j] = line[j + 1];
                        int shiftedFrom = getGridIndex(direction, lineIdx, j + 1);
                        int shiftedTo = getGridIndex(direction, lineIdx, j);
                        for (int k = 0; k < activeCount; k++) {
                            if (toIndex[k] == shiftedFrom) {
                                toIndex[k] = shiftedTo;
                            }
                        }
                    }
                    line[Grid2048Logic.SIZE - 1] = 0;
                }
            }
        }

        if (anyMove) {
            for (int i = 0; i < MAX_TILES; i++) {
                pool[i].active = false;
            }

            for (int i = 0; i < activeCount; i++) {
                int from = fromIndex[i];
                int to = toIndex[i];
                if (from < 0 || to < 0) continue;

                int fromCol = from % Grid2048Logic.SIZE;
                int fromRow = from / Grid2048Logic.SIZE;
                int toCol = to % Grid2048Logic.SIZE;
                int toRow = to / Grid2048Logic.SIZE;

                float startX = gridStartX + fromCol * (cellSize + cellGap);
                float startY = gridStartY + fromRow * (cellSize + cellGap);
                float targetX = gridStartX + toCol * (cellSize + cellGap);
                float targetY = gridStartY + toRow * (cellSize + cellGap);

                pool[i].init(startX, startY, resultTiers[i]);
                pool[i].startMove(targetX, targetY, resultTiers[i], merged[i]);
            }
        }

        return new MoveResult(anyMove, maxMergedTier);
    }

    private static int getGridIndex(Direction2048 direction, int line, int step) {
        return switch (direction) {
            case LEFT -> line * Grid2048Logic.SIZE + step;
            case RIGHT -> line * Grid2048Logic.SIZE + (Grid2048Logic.SIZE - 1 - step);
            case UP -> step * Grid2048Logic.SIZE + line;
            case DOWN -> (Grid2048Logic.SIZE - 1 - step) * Grid2048Logic.SIZE + line;
        };
    }
}

