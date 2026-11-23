package com.langtonanimal.network;

import com.langtonanimal.config.AntConfig;
import com.langtonanimal.config.ConfigManager;
import com.langtonanimal.config.ConfigValidator;
import com.langtonanimal.data.AntRegistry;
import com.langtonanimal.engine.AntActions;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {

    /**
     * 客户端接收器。由客户端初始化时注入，避免服务端加载任何客户端类。
     */
    public static Consumer<ConfigSyncPayload> clientReceiver = payload -> {
    };

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC, ModNetwork::handleSync);
        registrar.playToServer(SaveConfigPayload.TYPE, SaveConfigPayload.STREAM_CODEC, ModNetwork::handleSave);
        registrar.playToServer(SetRunningPayload.TYPE, SetRunningPayload.STREAM_CODEC, ModNetwork::handleRunning);
        registrar.playToServer(AdjustSpeedPayload.TYPE, AdjustSpeedPayload.STREAM_CODEC, ModNetwork::handleSpeed);
        registrar.playToServer(AssignPresetPayload.TYPE, AssignPresetPayload.STREAM_CODEC, ModNetwork::handleAssign);
    }

    private static boolean canControl(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server != null && (server.isSingleplayer() || player.hasPermissions(2));
    }

    private static void handleRunning(SetRunningPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !canControl(player)) {
                return;
            }
            AntActions.Outcome outcome = AntActions.setRunning(player.getServer(), payload.target(), payload.running());
            if (outcome.error() != null) {
                player.displayClientMessage(outcome.error().copy().withStyle(ChatFormatting.RED), true);
            }
        });
    }

    private static void handleSpeed(AdjustSpeedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !canControl(player)) {
                return;
            }
            AntActions.Outcome outcome = AntActions.adjustSpeed(player.getServer(), payload.target(), payload.direction());
            if (outcome.error() != null) {
                player.displayClientMessage(outcome.error().copy().withStyle(ChatFormatting.RED), true);
            }
        });
    }

    private static void handleAssign(AssignPresetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !canControl(player)) {
                return;
            }
            AntActions.Outcome outcome = AntActions.assignPreset(player, payload.entity(), payload.presetName());
            if (outcome.error() != null) {
                player.displayClientMessage(outcome.error().copy().withStyle(ChatFormatting.RED), true);
            } else {
                player.displayClientMessage(Component.translatable("langtonanimal.msg.assigned", payload.presetName())
                        .withStyle(ChatFormatting.GREEN), true);
            }
        });
    }

    private static void handleSync(ConfigSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientReceiver.accept(payload));
    }

    private static void handleSave(SaveConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            if (!server.isSingleplayer() && !player.hasPermissions(2)) {
                player.displayClientMessage(
                        Component.translatable("langtonanimal.msg.no_permission").withStyle(ChatFormatting.RED), false);
                return;
            }
            AntConfig incoming = AntConfig.fromJson(payload.json());
            AntRegistry registry = AntRegistry.get(server);
            List<Component> errors = ConfigValidator.validate(incoming, registry.names());
            if (!errors.isEmpty()) {
                player.displayClientMessage(
                        Component.translatable("langtonanimal.msg.save_failed").withStyle(ChatFormatting.RED), false);
                for (Component error : errors) {
                    player.displayClientMessage(Component.literal(" - ").withStyle(ChatFormatting.RED)
                            .append(error.copy().withStyle(ChatFormatting.RED)), false);
                }
                return;
            }
            ConfigManager.replace(incoming);
            broadcast(server);
            player.displayClientMessage(
                    Component.translatable("langtonanimal.msg.save_ok").withStyle(ChatFormatting.GREEN), false);
        });
    }

    public static void broadcast(MinecraftServer server) {
        String json = ConfigManager.get().toJson();
        List<String> names = AntRegistry.get(server).names();
        PacketDistributor.sendToAllPlayers(new ConfigSyncPayload(json, names));
    }

    public static void sendTo(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        String json = ConfigManager.get().toJson();
        List<String> names = AntRegistry.get(server).names();
        PacketDistributor.sendToPlayer(player, new ConfigSyncPayload(json, names));
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ConfigManager.ensureLoaded();
            sendTo(player);
        }
    }

    public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        ConfigManager.reload();
        broadcast(event.getServer());
    }
}
