package com.alikuxac.alchemy2048.core;

import com.alikuxac.alchemy2048.config.ServerConfig;

public final class EnergyLogic {
    public static final int BASE_COST = 20;
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;

    private EnergyLogic() {}

    public static int calculateMoveCost(byte maxTier) {
        int base = getBaseCost();
        if (maxTier <= 1) {
            return base;
        }

        int shift = Math.min(maxTier - 1, 30);
        long cost = (long) base << shift;
        return (int) Math.min(cost, getCapacity());
    }

    public static int getBaseCost() {
        return ServerConfig.baseEnergyCost > 0 ? ServerConfig.baseEnergyCost : BASE_COST;
    }

    public static int getCapacity() {
        return ServerConfig.energyCapacity > 0 ? ServerConfig.energyCapacity : CAPACITY;
    }

    public static int getMaxReceive() {
        return ServerConfig.maxEnergyReceive >= 0 ? ServerConfig.maxEnergyReceive : MAX_RECEIVE;
    }
}
