package com.alikuxac.alchemy2048;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Alchemy2048Common {
    public static final String MOD_ID = "alchemy2048";
    public static final String MOD_NAME = "Alchemy 2048";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private Alchemy2048Common() {}

    public static void init() {
        LOGGER.info("{} common initialized", MOD_NAME);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
