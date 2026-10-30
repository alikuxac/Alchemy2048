package com.alikuxac.alchemy2048;

import com.alikuxac.alchemy2048.core.PresetMigrationLogic;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PresetValidationAndMigrationTest {
    private static final Set<String> MOCK_REGISTRY = Set.of(
            "minecraft:oak_planks",
            "minecraft:cobblestone",
            "minecraft:stone",
            "minecraft:smooth_stone",
            "minecraft:deepslate",
            "minecraft:iron_ingot",
            "minecraft:gold_ingot",
            "minecraft:redstone",
            "minecraft:lapis_lazuli",
            "minecraft:diamond",
            "minecraft:netherite_ingot"
    );

    private static final List<String> VALID_11_LADDER = List.of(
            "minecraft:cobblestone",
            "minecraft:stone",
            "minecraft:smooth_stone",
            "minecraft:deepslate",
            "minecraft:iron_ingot",
            "minecraft:gold_ingot",
            "minecraft:redstone",
            "minecraft:lapis_lazuli",
            "minecraft:diamond",
            "minecraft:netherite_ingot",
            "minecraft:oak_planks"
    );

    @Test
    void testDisallowInvalidLadderSizeNotEleven() {
        List<String> rawLadder3 = List.of(
                "minecraft:cobblestone",
                "minecraft:stone",
                "minecraft:smooth_stone"
        );
        PresetMigrationLogic.CompactionResult<String> result3 = PresetMigrationLogic.compactAndValidate(
                true, rawLadder3, MOCK_REGISTRY::contains
        );
        assertFalse(result3.isValid(), "Preset with 3 items must be disabled / invalid");

        List<String> rawLadder4 = List.of(
                "minecraft:cobblestone",
                "minecraft:stone",
                "minecraft:smooth_stone",
                "minecraft:deepslate"
        );
        PresetMigrationLogic.CompactionResult<String> result4 = PresetMigrationLogic.compactAndValidate(
                true, rawLadder4, MOCK_REGISTRY::contains
        );
        assertFalse(result4.isValid(), "Preset with 4 items (not 11) must be disabled / invalid");
    }

    @Test
    void testAllowExactElevenItems() {
        PresetMigrationLogic.CompactionResult<String> result = PresetMigrationLogic.compactAndValidate(
                true,
                VALID_11_LADDER,
                MOCK_REGISTRY::contains
        );

        assertTrue(result.isValid(), "Preset with exactly 11 items (value 2048) must be valid");
        assertEquals(11, result.getActiveLadder().size());
    }

    @Test
    void testDisableIfStarterDeletedOrEmpty() {
        PresetMigrationLogic.CompactionResult<String> resultEmptyStarter = PresetMigrationLogic.compactAndValidate(
                false,
                VALID_11_LADDER,
                MOCK_REGISTRY::contains
        );
        assertFalse(resultEmptyStarter.isValid(), "Empty or deleted starter must disable preset");

        List<String> invalidFirstItem = new ArrayList<>(VALID_11_LADDER);
        invalidFirstItem.set(0, "uninstalled_mod:deleted_starter");

        PresetMigrationLogic.CompactionResult<String> resultInvalidFirstItem = PresetMigrationLogic.compactAndValidate(
                true,
                invalidFirstItem,
                MOCK_REGISTRY::contains
        );
        assertFalse(resultInvalidFirstItem.isValid(), "Deleted starter/tier 1 item must disable preset");
    }

    @Test
    void testCompactionWhenItemDeletedInMiddle() {
        List<String> rawLadder = new ArrayList<>(VALID_11_LADDER);
        rawLadder.set(3, "uninstalled_mod:ruby");

        PresetMigrationLogic.CompactionResult<String> result = PresetMigrationLogic.compactAndValidate(
                true,
                rawLadder,
                MOCK_REGISTRY::contains
        );

        assertTrue(result.isValid(), "Compacted preset with 10 valid items should remain valid");
        assertEquals(10, result.getActiveLadder().size(), "Active ladder should omit the missing item");

        assertEquals(1, result.remapTier(1));
        assertEquals(2, result.remapTier(2));
        assertEquals(3, result.remapTier(3));
        assertEquals(3, result.remapTier(4), "Deleted item in middle must be demoted to nearest lower tier");
        assertEquals(4, result.remapTier(5));
    }

    @Test
    void testCompactionWhenItemDeletedAtEnd() {
        List<String> rawLadder = new ArrayList<>(VALID_11_LADDER);
        rawLadder.set(10, "uninstalled_mod:future_ore");

        PresetMigrationLogic.CompactionResult<String> result = PresetMigrationLogic.compactAndValidate(
                true,
                rawLadder,
                MOCK_REGISTRY::contains
        );

        assertTrue(result.isValid(), "Ladder with 10 valid items should be valid");
        assertEquals(10, result.getActiveLadder().size());
        assertEquals(10, result.remapTier(11), "Deleted item at end must be demoted to nearest lower tier");
    }

    @Test
    void testCompactionBelowFourItemsDisablesPreset() {
        List<String> rawLadder = List.of(
                "minecraft:cobblestone",
                "minecraft:stone",
                "minecraft:smooth_stone",
                "uninstalled:mod1",
                "uninstalled:mod2",
                "uninstalled:mod3",
                "uninstalled:mod4",
                "uninstalled:mod5",
                "uninstalled:mod6",
                "uninstalled:mod7",
                "uninstalled:mod8"
        );

        PresetMigrationLogic.CompactionResult<String> result = PresetMigrationLogic.compactAndValidate(
                true,
                rawLadder,
                MOCK_REGISTRY::contains
        );

        assertFalse(result.isValid(), "Compacting to less than 4 items must disable preset");
        assertEquals(3, result.getActiveLadder().size());
    }

    @Test
    void testInGameGridRemappingAndDemotion() {
        List<String> originalSavedLadder = new ArrayList<>(VALID_11_LADDER);
        originalSavedLadder.set(3, "uninstalled_mod:ruby");

        List<String> currentRawLadder = new ArrayList<>(VALID_11_LADDER);
        currentRawLadder.set(3, "uninstalled_mod:ruby");

        PresetMigrationLogic.CompactionResult<String> currentResult = PresetMigrationLogic.compactAndValidate(
                true,
                currentRawLadder,
                MOCK_REGISTRY::contains
        );
        assertTrue(currentResult.isValid());

        byte[] grid = new byte[16];
        grid[0] = 1;
        grid[1] = 3;
        grid[2] = 4;
        grid[3] = 5;
        grid[4] = 6;

        boolean remapped = PresetMigrationLogic.remapGrid(
                grid,
                originalSavedLadder,
                currentResult.getActiveLadder(),
                currentResult.getRemapTable()
        );
        assertTrue(remapped);

        assertEquals(1, grid[0], "Tier 1 should stay 1");
        assertEquals(3, grid[1], "Tier 3 should stay 3");
        assertEquals(3, grid[2], "Deleted Tier 4 should demote to nearest lower valid tier (Tier 3)");
        assertEquals(4, grid[3], "Tier 5 (diamond) should shift to Tier 4");
        assertEquals(5, grid[4], "Tier 6 (netherite) should shift to Tier 5");
    }

    @Test
    void testMultipleConsecutiveDeletedItemsDemotion() {
        List<String> savedLadder = List.of(
                "minecraft:oak_planks",
                "minecraft:stone",
                "deleted:ore1",
                "deleted:ore2",
                "minecraft:diamond"
        );

        List<String> activeLadder = List.of(
                "minecraft:oak_planks",
                "minecraft:stone",
                "minecraft:diamond",
                "minecraft:netherite_ingot"
        );

        byte[] grid = new byte[16];
        grid[0] = 3;
        grid[1] = 4;
        grid[2] = 5;

        boolean remapped = PresetMigrationLogic.remapGrid(grid, savedLadder, activeLadder, null);
        assertTrue(remapped);

        assertEquals(2, grid[0], "Tier 3 (deleted:ore1) demotes to nearest lower valid tier = Tier 2 (stone)");
        assertEquals(2, grid[1], "Tier 4 (deleted:ore2) demotes to nearest lower valid tier = Tier 2 (stone)");
        assertEquals(3, grid[2], "Tier 5 (diamond) shifts to new Tier 3");
    }
}

