package com.alikuxac.alchemy2048;

import com.alikuxac.alchemy2048.config.ClientConfig;
import com.alikuxac.alchemy2048.config.ServerConfig;
import com.alikuxac.alchemy2048.core.EnergyLogic;
import com.alikuxac.alchemy2048.menu.client.GuiTheme;
import com.alikuxac.alchemy2048.menu.client.TileNumberDisplay;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigAndLocalizationTest {

    @BeforeEach
    @AfterEach
    void resetConfigs() {
        ServerConfig.resetForTests();
        ClientConfig.resetForTests();
    }

    @Test
    void testServerConfigDefaults() {
        assertFalse(ServerConfig.enableContributionRatio);
        assertEquals(0.5, ServerConfig.coopTaxRatio, 0.001);
        assertEquals(0.5, ServerConfig.gachaFailRate, 0.001);
        assertEquals(20, ServerConfig.baseEnergyCost);
        assertEquals(50_000, ServerConfig.energyCapacity);
        assertEquals(2_500, ServerConfig.maxEnergyReceive);
        assertEquals(8.0, ServerConfig.mutexDistanceBlocks, 0.001);
    }

@Test
    void testDynamicEnergyLogicFromConfig() {
        assertEquals(20, EnergyLogic.getBaseCost());
        assertEquals(50_000, EnergyLogic.getCapacity());
        assertEquals(2_500, EnergyLogic.getMaxReceive());

        ServerConfig.baseEnergyCost = 50;
        ServerConfig.energyCapacity = 100_000;
        ServerConfig.maxEnergyReceive = 5_000;

        assertEquals(50, EnergyLogic.getBaseCost());
        assertEquals(100_000, EnergyLogic.getCapacity());
        assertEquals(5_000, EnergyLogic.getMaxReceive());

        assertEquals(50, EnergyLogic.calculateMoveCost((byte) 1));
        assertEquals(100, EnergyLogic.calculateMoveCost((byte) 2));
    }

    @Test
    void testServerConfigSyncFromSpec() {
        ServerConfig.baseEnergyCostSpec = () -> 123;
        ServerConfig.energyCapacitySpec = () -> 777;
        ServerConfig.maxEnergyReceiveSpec = () -> 42;
        ServerConfig.mutexDistanceBlocksSpec = () -> 16.0;
        ServerConfig.enableContributionRatioSpec = () -> true;
        ServerConfig.coopTaxRatioSpec = () -> 0.25;
        ServerConfig.gachaFailRateSpec = () -> 0.8;

        ServerConfig.syncFromSpec();

        assertEquals(123, ServerConfig.baseEnergyCost);
        assertEquals(777, ServerConfig.energyCapacity);
        assertEquals(42, ServerConfig.maxEnergyReceive);
        assertEquals(16.0, ServerConfig.mutexDistanceBlocks, 0.001);
        assertTrue(ServerConfig.enableContributionRatio);
        assertEquals(0.25, ServerConfig.coopTaxRatio, 0.001);
        assertEquals(0.8, ServerConfig.gachaFailRate, 0.001);
        assertEquals(123, EnergyLogic.getBaseCost());
        assertEquals(777, EnergyLogic.getCapacity());
    }

    @Test
    void testClientConfigSyncFromSpec() {
        ClientConfig.themeSpec = () -> "retro_2048";
        ClientConfig.numberDisplaySpec = () -> "tier_index";
        ClientConfig.showTileTooltipSpec = () -> false;
        ClientConfig.syncFromSpec();

        assertEquals(GuiTheme.RETRO_2048, ClientConfig.theme);
        assertEquals(TileNumberDisplay.TIER_INDEX, ClientConfig.numberDisplay);
        assertFalse(ClientConfig.showTileTooltip);
    }

    @Test
    void testClientThemes() {
        assertEquals(GuiTheme.DARK, ClientConfig.theme);
        GuiTheme nextTheme = ClientConfig.theme.next();
        assertEquals(GuiTheme.STEAMPUNK, nextTheme);
        assertEquals(GuiTheme.MINIMAL, nextTheme.next());
        assertEquals(GuiTheme.RETRO_2048, nextTheme.next().next());
        assertEquals(GuiTheme.DARK, nextTheme.next().next().next());

        assertNotNull(GuiTheme.byId("steampunk"));
        assertNotNull(GuiTheme.byId("minimal"));
        assertNotNull(GuiTheme.byId("retro_2048"));
        assertEquals(GuiTheme.DARK, GuiTheme.byId("unknown_fallback"));
    }

    @Test
    void testTileNumberDisplayFormatting() {
        assertEquals(TileNumberDisplay.EXPONENTIAL, ClientConfig.numberDisplay);
        assertEquals("2", TileNumberDisplay.EXPONENTIAL.format((byte) 1));
        assertEquals("2048", TileNumberDisplay.EXPONENTIAL.format((byte) 11));

        assertEquals("T1", TileNumberDisplay.TIER_INDEX.format((byte) 1));
        assertEquals("T11", TileNumberDisplay.TIER_INDEX.format((byte) 11));

        assertEquals("", TileNumberDisplay.NONE.format((byte) 1));
        assertEquals("", TileNumberDisplay.NONE.format((byte) 11));
    }
}

