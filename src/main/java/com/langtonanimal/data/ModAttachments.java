package com.langtonanimal.data;

import com.langtonanimal.LangtonAnimal;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, LangtonAnimal.MOD_ID);

    public static final Supplier<AttachmentType<AntEntityData>> ANT_DATA = ATTACHMENT_TYPES.register(
            "ant_data",
            () -> AttachmentType.builder(AntEntityData::new)
                    .serialize(AntEntityData.CODEC)
                    .sync(AntEntityData.STREAM_CODEC)
                    .build());

    private ModAttachments() {
    }
}
