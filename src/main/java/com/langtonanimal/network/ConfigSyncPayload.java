package com.langtonanimal.network;

import com.langtonanimal.LangtonAnimal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 -> 客户端：下发完整配置与当前在场生物名。
 */
public record ConfigSyncPayload(String json, List<String> liveNames) implements CustomPacketPayload {

    public static final Type<ConfigSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LangtonAnimal.MOD_ID, "config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ConfigSyncPayload::json,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
                    ConfigSyncPayload::liveNames,
                    ConfigSyncPayload::new);

    public ConfigSyncPayload(String json, List<String> liveNames) {
        this.json = json == null ? "{}" : json;
        this.liveNames = liveNames == null ? List.of() : List.copyOf(liveNames);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
