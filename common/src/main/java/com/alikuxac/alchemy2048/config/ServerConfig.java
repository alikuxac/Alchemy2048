package com.alikuxac.alchemy2048.config;

import java.util.function.Supplier;

public final class ServerConfig {
    public static boolean enableContributionRatio = false;
    public static double coopTaxRatio = 0.5;
    public static int baseEnergyCost = 20;
    public static int energyCapacity = 50_000;
    public static int maxEnergyReceive = 2_500;
    public static double mutexDistanceBlocks = 8.0;

    public static Supplier<Boolean> enableContributionRatioSpec = () -> false;
    public static Supplier<Double> coopTaxRatioSpec = () -> 0.5;
    public static Supplier<Integer> baseEnergyCostSpec = () -> 20;
    public static Supplier<Integer> energyCapacitySpec = () -> 50_000;
    public static Supplier<Integer> maxEnergyReceiveSpec = () -> 2_500;
    public static Supplier<Double> mutexDistanceBlocksSpec = () -> 8.0;

    private ServerConfig() {}

    public static void syncFromSpec() {
        enableContributionRatio = enableContributionRatioSpec.get();
        coopTaxRatio = coopTaxRatioSpec.get();
        baseEnergyCost = baseEnergyCostSpec.get();
        energyCapacity = energyCapacitySpec.get();
        maxEnergyReceive = maxEnergyReceiveSpec.get();
        mutexDistanceBlocks = mutexDistanceBlocksSpec.get();
    }

    public static void resetToDefaults() {
        enableContributionRatio = false;
        coopTaxRatio = 0.5;
        baseEnergyCost = 20;
        energyCapacity = 50_000;
        maxEnergyReceive = 2_500;
        mutexDistanceBlocks = 8.0;
    }

    public static void resetForTests() {
        resetToDefaults();
        enableContributionRatioSpec = () -> false;
        coopTaxRatioSpec = () -> 0.5;
        baseEnergyCostSpec = () -> 20;
        energyCapacitySpec = () -> 50_000;
        maxEnergyReceiveSpec = () -> 2_500;
        mutexDistanceBlocksSpec = () -> 8.0;
    }
}