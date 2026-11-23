package com.langtonanimal.client;

import com.langtonanimal.LangtonAnimal;
import com.langtonanimal.network.ModNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端入口：只在物理客户端加载。
 */
public final class LangtonAnimalClient {

    public static final KeyMapping OPEN_GUI = new KeyMapping(
            "key.langtonanimal.open_gui",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "key.categories.langtonanimal");

    private LangtonAnimalClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(LangtonAnimalClient::registerKeys);
        ModNetwork.clientReceiver = payload -> ClientState.acceptSync(payload.json(), payload.liveNames());
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onRenderNameTag);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onScroll);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onInteraction);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onRenderGui);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onRenderLevel);
        LangtonAnimal.LOGGER.info("LangtonAnimal client initialized");
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUI);
    }
}
