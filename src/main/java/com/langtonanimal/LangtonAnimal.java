package com.langtonanimal;

import com.langtonanimal.client.LangtonAnimalClient;
import com.langtonanimal.command.AntCommands;
import com.langtonanimal.data.ModAttachments;
import com.langtonanimal.engine.AntEngine;
import com.langtonanimal.network.ModNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(LangtonAnimal.MOD_ID)
public final class LangtonAnimal {

    public static final String MOD_ID = "langtonanimal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public LangtonAnimal(IEventBus modBus) {
        ModAttachments.ATTACHMENT_TYPES.register(modBus);
        modBus.addListener(ModNetwork::register);

        NeoForge.EVENT_BUS.addListener(AntCommands::register);
        NeoForge.EVENT_BUS.addListener(AntEngine::onServerTick);
        NeoForge.EVENT_BUS.addListener(AntEngine::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(AntEngine::onDeath);
        NeoForge.EVENT_BUS.addListener(ModNetwork::onLogin);
        NeoForge.EVENT_BUS.addListener(ModNetwork::onServerStarted);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            LangtonAnimalClient.init(modBus);
        }
    }
}
