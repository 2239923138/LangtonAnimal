package com.langtonanimal.network;

import com.langtonanimal.LangtonAnimal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：提交编辑后的配置 JSON，由服务端校验并落盘。
 */
public record SaveConfigPayload(String json) implements CustomPacketPayload {

    public static final Type<SaveConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LangtonAnimal.MOD_ID, "save_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveConfigPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    SaveConfigPayload::json,
                    SaveConfigPayload::new);

    public SaveConfigPayload(String json) {
        this.json = json == null ? "{}" : json;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
