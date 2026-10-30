package com.alikuxac.alchemy2048.forge;

import com.alikuxac.alchemy2048.Alchemy2048Common;
import com.alikuxac.alchemy2048.block.entity.AlchemyMatrixBlockEntity;
import com.alikuxac.alchemy2048.config.ClientConfig;
import com.alikuxac.alchemy2048.config.ServerConfig;
import com.alikuxac.alchemy2048.forge.block.entity.ForgeAlchemyMatrixBlockEntity;
import com.alikuxac.alchemy2048.forge.config.ForgeConfigs;
import com.alikuxac.alchemy2048.forge.network.ForgeNetwork;
import com.alikuxac.alchemy2048.forge.registry.ForgeRegistries;
import com.alikuxac.alchemy2048.menu.client.AlchemyMatrixScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Alchemy2048Common.MOD_ID)
public class Alchemy2048Forge {
    public Alchemy2048Forge(FMLJavaModLoadingContext context) {
        this(context.getModEventBus());
    }

    public Alchemy2048Forge() {
        this(FMLJavaModLoadingContext.get().getModEventBus());
    }

    public Alchemy2048Forge(IEventBus modBus) {
        Alchemy2048Common.init();
        AlchemyMatrixBlockEntity.FACTORY = ForgeAlchemyMatrixBlockEntity::new;

        ForgeConfigs.bind();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ForgeConfigs.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ForgeConfigs.CLIENT_SPEC);

        ForgeRegistries.register(modBus);
        modBus.addListener(Alchemy2048Forge::onCommonSetup);
        modBus.addListener(Alchemy2048Forge::onConfigLoad);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(Alchemy2048Forge::onClientSetup);
        }
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        ForgeNetwork.register();
    }

    private static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == ForgeConfigs.SERVER_SPEC) {
            ServerConfig.syncFromSpec();
        } else if (event.getConfig().getSpec() == ForgeConfigs.CLIENT_SPEC) {
            ClientConfig.syncFromSpec();
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ForgeRegistries.ALCHEMY_MATRIX_MENU.get(), AlchemyMatrixScreen::new));
    }
}