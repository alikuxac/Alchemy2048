package com.alikuxac.alchemy2048.config;

import com.alikuxac.alchemy2048.menu.client.GuiTheme;
import com.alikuxac.alchemy2048.menu.client.TileNumberDisplay;

import java.util.function.Supplier;

public final class ClientConfig {
    public static GuiTheme theme = GuiTheme.DARK;
    public static TileNumberDisplay numberDisplay = TileNumberDisplay.EXPONENTIAL;
    public static boolean playUiSounds = true;
    public static boolean showTileTooltip = true;

    public static Supplier<String> themeSpec = () -> GuiTheme.DARK.getId();
    public static Supplier<String> numberDisplaySpec = () -> TileNumberDisplay.EXPONENTIAL.getId();
    public static Supplier<Boolean> playUiSoundsSpec = () -> true;
    public static Supplier<Boolean> showTileTooltipSpec = () -> true;

    private ClientConfig() {}

    public static void syncFromSpec() {
        theme = GuiTheme.byId(themeSpec.get());
        numberDisplay = TileNumberDisplay.byId(numberDisplaySpec.get());
        playUiSounds = playUiSoundsSpec.get();
        showTileTooltip = showTileTooltipSpec.get();
    }

    public static void resetToDefaults() {
        theme = GuiTheme.DARK;
        numberDisplay = TileNumberDisplay.EXPONENTIAL;
        playUiSounds = true;
        showTileTooltip = true;
    }

    public static void resetForTests() {
        resetToDefaults();
        themeSpec = () -> GuiTheme.DARK.getId();
        numberDisplaySpec = () -> TileNumberDisplay.EXPONENTIAL.getId();
        playUiSoundsSpec = () -> true;
        showTileTooltipSpec = () -> true;
    }
}