package com.langtonanimal.network;

import com.langtonanimal.LangtonAnimal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：启停某个目标（生物名 / 预设名 / all）。
 */
public record SetRunningPayload(String target, boolean running) implements CustomPacketPayload {

    public static final Type<SetRunningPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LangtonAnimal.MOD_ID, "set_running"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetRunningPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    SetRunningPayload::target,
                    ByteBufCodecs.BOOL,
                    SetRunningPayload::running,
                    SetRunningPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
