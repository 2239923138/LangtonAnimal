package com.langtonanimal.network;

import com.langtonanimal.LangtonAnimal;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：把某个生物绑定到指定预设（非兰顿生物会被就地转化）。
 */
public record AssignPresetPayload(UUID entity, String presetName) implements CustomPacketPayload {

    public static final Type<AssignPresetPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LangtonAnimal.MOD_ID, "assign_preset"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AssignPresetPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    AssignPresetPayload::entity,
                    ByteBufCodecs.STRING_UTF8,
                    AssignPresetPayload::presetName,
                    AssignPresetPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
