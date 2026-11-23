package com.langtonanimal.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

/**
 * 规则可用的方块调色板：16 色羊毛 / 16 色混凝土 / 16 色陶瓦，共 48 种。
 */
public final class BlockPalette {

    public enum Family {
        WOOL("wool", "_wool"),
        CONCRETE("concrete", "_concrete"),
        TERRACOTTA("terracotta", "_terracotta");

        private final String key;
        private final String suffix;

        Family(String key, String suffix) {
            this.key = key;
            this.suffix = suffix;
        }

        public String key() {
            return key;
        }

        public String suffix() {
            return suffix;
        }
    }

    public record Entry(Family family, DyeColor color, Block block) {
        public ResourceLocation id() {
            return BuiltInRegistries.BLOCK.getKey(block);
        }

        public String idString() {
            return id().toString();
        }

        public String translationKey() {
            return block.getDescriptionId();
        }
    }

    private static List<Entry> cache;

    private BlockPalette() {
    }

    public static List<Entry> all() {
        if (cache == null) {
            List<Entry> entries = new ArrayList<>(48);
            for (Family family : Family.values()) {
                for (DyeColor color : DyeColor.values()) {
                    ResourceLocation id = ResourceLocation.withDefaultNamespace(color.getName() + family.suffix());
                    Block block = BuiltInRegistries.BLOCK.get(id);
                    if (block != null) {
                        entries.add(new Entry(family, color, block));
                    }
                }
            }
            cache = Collections.unmodifiableList(entries);
        }
        return cache;
    }

    public static List<Entry> byFamily(Family family) {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : all()) {
            if (entry.family() == family) {
                result.add(entry);
            }
        }
        return result;
    }

    /** 规范化方块 ID：去空白、补命名空间、空值回落到默认草方块。 */
    public static String normalize(String raw) {
        if (raw == null) {
            return AntConfig.FALLBACK_DEFAULT_BLOCK;
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return AntConfig.FALLBACK_DEFAULT_BLOCK;
        }
        if (!trimmed.contains(":")) {
            trimmed = "minecraft:" + trimmed;
        }
        ResourceLocation rl = ResourceLocation.tryParse(trimmed);
        if (rl == null) {
            return AntConfig.FALLBACK_DEFAULT_BLOCK;
        }
        return rl.toString();
    }

    /** 方块实例 -> 具体方块 ID 字符串。 */
    public static String idOf(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    public static boolean isValidBlockId(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.BLOCK.getOptional(rl).isPresent();
    }

    public static boolean isKeep(String id) {
        return AntConfig.KEEP.equals(id);
    }

    public static boolean isDefaultRef(String id) {
        return AntConfig.DEFAULT_BLOCK.equals(id);
    }

    /**
     * 把语义标识符解析为具体方块 ID 字符串。KEEP 原样返回。
     */
    public static String resolveId(String id, String defaultBlockId) {
        if (id == null) {
            return null;
        }
        if (isKeep(id)) {
            return AntConfig.KEEP;
        }
        if (isDefaultRef(id)) {
            return normalize(defaultBlockId);
        }
        return normalize(id);
    }

    @Nullable
    public static Block resolveBlock(String id, String defaultBlockId) {
        String resolved = resolveId(id, defaultBlockId);
        if (resolved == null || AntConfig.KEEP.equals(resolved)) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(resolved);
        if (rl == null) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
    }

    @Nullable
    public static Block blockById(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
    }

    /** 用于界面显示的名称：方块 ID -> 本地化名称。 */
    public static net.minecraft.network.chat.Component displayName(String id, String defaultBlockId) {
        if (isKeep(id)) {
            return net.minecraft.network.chat.Component.translatable("langtonanimal.block.keep");
        }
        if (isDefaultRef(id)) {
            Block block = blockById(normalize(defaultBlockId));
            net.minecraft.network.chat.Component base = block == null
                    ? net.minecraft.network.chat.Component.literal(normalize(defaultBlockId))
                    : block.getName();
            return net.minecraft.network.chat.Component.translatable("langtonanimal.block.default_wrapped", base);
        }
        Block block = blockById(normalize(id));
        if (block == null) {
            return net.minecraft.network.chat.Component.literal(String.valueOf(id));
        }
        return block.getName();
    }
}
