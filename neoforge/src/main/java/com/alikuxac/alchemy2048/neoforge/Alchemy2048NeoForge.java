package com.alikuxac.alchemy2048.neoforge;

import com.alikuxac.alchemy2048.Alchemy2048Common;
import com.alikuxac.alchemy2048.config.ClientConfig;
import com.alikuxac.alchemy2048.config.ServerConfig;
import com.alikuxac.alchemy2048.menu.client.AlchemyMatrixScreen;
import com.alikuxac.alchemy2048.neoforge.config.NeoForgeConfigs;
import com.alikuxac.alchemy2048.neoforge.energy.NeoForgeEnergyStorage;
import com.alikuxac.alchemy2048.neoforge.network.NeoForgeNetwork;
import com.alikuxac.alchemy2048.neoforge.registry.NeoForgeRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(Alchemy2048Common.MOD_ID)
public class Alchemy2048NeoForge {
    public Alchemy2048NeoForge(IEventBus modBus, ModContainer modContainer) {
        Alchemy2048Common.init();
        NeoForgeConfigs.bind();
        modContainer.registerConfig(ModConfig.Type.SERVER, NeoForgeConfigs.SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, NeoForgeConfigs.CLIENT_SPEC);

        NeoForgeRegistries.register(modBus);
        modBus.addListener(NeoForgeNetwork::register);
        modBus.addListener(Alchemy2048NeoForge::onRegisterCapabilities);
        modBus.addListener(Alchemy2048NeoForge::onConfigLoad);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(Alchemy2048NeoForge::onRegisterScreens);
        }
    }

    private static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.SERVER) {
            ServerConfig.syncFromSpec();
        } else if (event.getConfig().getType() == ModConfig.Type.CLIENT) {
            ClientConfig.syncFromSpec();
        }
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                NeoForgeRegistries.ALCHEMY_MATRIX_BE.get(),
                (be, side) -> new NeoForgeEnergyStorage(be.getEnergyStorage())
        );
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(NeoForgeRegistries.ALCHEMY_MATRIX_MENU.get(), AlchemyMatrixScreen::new);
    }
}
