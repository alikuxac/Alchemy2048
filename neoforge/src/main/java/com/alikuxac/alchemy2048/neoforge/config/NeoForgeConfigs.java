package com.alikuxac.alchemy2048.neoforge.config;

import com.alikuxac.alchemy2048.config.ClientConfig;
import com.alikuxac.alchemy2048.config.ServerConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoForgeConfigs {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    private static final ModConfigSpec.BooleanValue ENABLE_CONTRIBUTION_RATIO;
    private static final ModConfigSpec.DoubleValue COOP_TAX_RATIO;
    private static final ModConfigSpec.IntValue BASE_ENERGY_COST;
    private static final ModConfigSpec.IntValue ENERGY_CAPACITY;
    private static final ModConfigSpec.IntValue MAX_ENERGY_RECEIVE;
    private static final ModConfigSpec.DoubleValue MUTEX_DISTANCE_BLOCKS;

    private static final ModConfigSpec.ConfigValue<String> THEME;
    private static final ModConfigSpec.ConfigValue<String> NUMBER_DISPLAY;
    private static final ModConfigSpec.BooleanValue PLAY_UI_SOUNDS;
    private static final ModConfigSpec.BooleanValue SHOW_TILE_TOOLTIP;

    static {
        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        server.push("gameplay");
        ENABLE_CONTRIBUTION_RATIO = server
                .comment("Scale co-op rewards by move participation instead of applying coopTaxRatio.")
                .define("enableContributionRatio", false);
        COOP_TAX_RATIO = server
                .comment("Probability a non-owner claim is taxed when enableContributionRatio is false.")
                .defineInRange("coopTaxRatio", 0.5, 0.0, 1.0);
        BASE_ENERGY_COST = server
                .comment("FE required for a Tier 1 move. Cost doubles per tier.")
                .defineInRange("baseEnergyCost", 20, 0, 10_000);
        ENERGY_CAPACITY = server
                .comment("Internal FE buffer of the Alchemy Matrix.")
                .defineInRange("energyCapacity", 50_000, 1_000, 1_000_000);
        MAX_ENERGY_RECEIVE = server
                .comment("Maximum FE piped into the block per tick.")
                .defineInRange("maxEnergyReceive", 2_500, 0, 100_000);
        MUTEX_DISTANCE_BLOCKS = server
                .comment("Distance a player may move from the machine before the mutex lock breaks.")
                .defineInRange("mutexDistanceBlocks", 8.0, 2.0, 64.0);
        server.pop();
        SERVER_SPEC = server.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        THEME = client
                .comment("GUI color theme: dark, steampunk, minimal, retro_2048.")
                .define("theme", "dark");
        NUMBER_DISPLAY = client
                .comment("Tile number overlay: exponential, tier_index, none.")
                .define("numberDisplay", "exponential");
        PLAY_UI_SOUNDS = client.define("playUiSounds", true);
        SHOW_TILE_TOOLTIP = client.define("showTileTooltip", true);
        CLIENT_SPEC = client.build();
    }

    private NeoForgeConfigs() {}

    public static void bind() {
        ServerConfig.enableContributionRatioSpec = () -> SERVER_SPEC.isLoaded() ? ENABLE_CONTRIBUTION_RATIO.get() : false;
        ServerConfig.coopTaxRatioSpec = () -> SERVER_SPEC.isLoaded() ? COOP_TAX_RATIO.get() : 0.5;
        ServerConfig.baseEnergyCostSpec = () -> SERVER_SPEC.isLoaded() ? BASE_ENERGY_COST.get() : 20;
        ServerConfig.energyCapacitySpec = () -> SERVER_SPEC.isLoaded() ? ENERGY_CAPACITY.get() : 50_000;
        ServerConfig.maxEnergyReceiveSpec = () -> SERVER_SPEC.isLoaded() ? MAX_ENERGY_RECEIVE.get() : 2_500;
        ServerConfig.mutexDistanceBlocksSpec = () -> SERVER_SPEC.isLoaded() ? MUTEX_DISTANCE_BLOCKS.get() : 8.0;

        ClientConfig.themeSpec = () -> CLIENT_SPEC.isLoaded() ? THEME.get() : "dark";
        ClientConfig.numberDisplaySpec = () -> CLIENT_SPEC.isLoaded() ? NUMBER_DISPLAY.get() : "exponential";
        ClientConfig.playUiSoundsSpec = () -> CLIENT_SPEC.isLoaded() ? PLAY_UI_SOUNDS.get() : true;
        ClientConfig.showTileTooltipSpec = () -> CLIENT_SPEC.isLoaded() ? SHOW_TILE_TOOLTIP.get() : true;
    }
}