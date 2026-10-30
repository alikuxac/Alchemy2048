package com.alikuxac.alchemy2048.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class PresetMigrationLogic {
    public static final int REQUIRED_LADDER_SIZE = 11;
    public static final int MIN_LADDER_SIZE = 4;

    private PresetMigrationLogic() {}

    public static class CompactionResult<T> {
        private final List<T> activeLadder;
        private final int[] remapTable;
        private final boolean valid;

        public CompactionResult(List<T> activeLadder, int[] remapTable, boolean valid) {
            this.activeLadder = activeLadder;
            this.remapTable = remapTable;
            this.valid = valid;
        }

        public List<T> getActiveLadder() {
            return activeLadder;
        }

        public int[] getRemapTable() {
            return remapTable;
        }

        public boolean isValid() {
            return valid;
        }

        public int remapTier(int oldTier) {
            if (oldTier <= 0) return 0;
            if (oldTier < remapTable.length) {
                return remapTable[oldTier];
            }
            return activeLadder.size();
        }
    }

    public static <T> CompactionResult<T> compactAndValidate(
            boolean isStarterValid,
            List<T> rawLadder,
            Predicate<T> isItemValid
    ) {
        if (rawLadder == null || rawLadder.isEmpty()) {
            return new CompactionResult<>(Collections.emptyList(), new int[1], false);
        }

        List<T> active = new ArrayList<>();
        int[] remap = new int[rawLadder.size() + 1];
        int lastValidTier = 0;

        for (int i = 0; i < rawLadder.size(); i++) {
            T item = rawLadder.get(i);
            int originalTier = i + 1;
            if (isItemValid.test(item)) {
                active.add(item);
                lastValidTier = active.size();
                remap[originalTier] = lastValidTier;
            } else {
                remap[originalTier] = lastValidTier;
            }
        }

        Set<T> uniqueItems = new HashSet<>(rawLadder);
        boolean hasDuplicates = uniqueItems.size() < rawLadder.size();

        boolean firstItemValid = isItemValid.test(rawLadder.getFirst());
        boolean exactConfiguredSize = rawLadder.size() == REQUIRED_LADDER_SIZE;
        boolean valid = isStarterValid && firstItemValid && !hasDuplicates && exactConfiguredSize && active.size() >= MIN_LADDER_SIZE;

        return new CompactionResult<>(Collections.unmodifiableList(active), remap, valid);
    }

    public static <T> boolean remapGrid(
            byte[] grid,
            List<T> savedLadder,
            List<T> activeLadder,
            int[] fallbackRemapTable
    ) {
        if (grid == null || activeLadder == null || activeLadder.size() < MIN_LADDER_SIZE) {
            return false;
        }

        boolean changed = false;
        for (int i = 0; i < grid.length; i++) {
            byte tier = grid[i];
            if (tier <= 0) continue;

            byte newTier;
            if (savedLadder != null && !savedLadder.isEmpty()) {
                int oldIndex = tier - 1;
                if (oldIndex < savedLadder.size()) {
                    T savedItem = savedLadder.get(oldIndex);
                    int foundIndex = activeLadder.indexOf(savedItem);
                    if (foundIndex >= 0) {
                        newTier = (byte) (foundIndex + 1);
                    } else {
                        int lowerTier = 0;
                        for (int prev = oldIndex - 1; prev >= 0; prev--) {
                            T prevItem = savedLadder.get(prev);
                            int prevIndex = activeLadder.indexOf(prevItem);
                            if (prevIndex >= 0) {
                                lowerTier = prevIndex + 1;
                                break;
                            }
                        }
                        newTier = (byte) lowerTier;
                    }
                } else {
                    newTier = (byte) Math.min(tier, activeLadder.size());
                }
            } else if (fallbackRemapTable != null) {
                newTier = (byte) (tier < fallbackRemapTable.length ? fallbackRemapTable[tier] : activeLadder.size());
            } else {
                newTier = (byte) Math.min(tier, activeLadder.size());
            }

            if (grid[i] != newTier) {
                grid[i] = newTier;
                changed = true;
            }
        }
        return changed;
    }
}

