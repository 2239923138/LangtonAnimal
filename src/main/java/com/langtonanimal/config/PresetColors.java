package com.langtonanimal.config;

import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * 预设颜色调色板。颜色同时用于界面文字、头顶名称与烈焰棒高亮框。
 */
public final class PresetColors {

    public record Tone(String key, int rgb) {
        public String translationKey() {
            return "langtonanimal.color." + key;
        }
    }

    private static final List<Tone> TONES = List.of(
            new Tone("white", 0xFFFFFF),
            new Tone("light_gray", 0xC8C8C8),
            new Tone("gray", 0x8C8C8C),
            new Tone("red", 0xFF5555),
            new Tone("orange", 0xFF9933),
            new Tone("yellow", 0xFFE14D),
            new Tone("lime", 0x9BFF4D),
            new Tone("green", 0x55DD55),
            new Tone("teal", 0x36D6C3),
            new Tone("aqua", 0x55FFFF),
            new Tone("blue", 0x6FB2FF),
            new Tone("purple", 0xB47BFF),
            new Tone("magenta", 0xFF77FF),
            new Tone("pink", 0xFF9EC4),
            new Tone("brown", 0xC08A5A),
            new Tone("gold", 0xE0B040));

    private PresetColors() {
    }

    public static List<Tone> all() {
        return TONES;
    }

    public static Tone of(int rgb) {
        int normalized = rgb & 0xFFFFFF;
        for (Tone tone : TONES) {
            if (tone.rgb() == normalized) {
                return tone;
            }
        }
        return null;
    }

    public static Tone next(int rgb) {
        int normalized = rgb & 0xFFFFFF;
        for (int i = 0; i < TONES.size(); i++) {
            if (TONES.get(i).rgb() == normalized) {
                return TONES.get((i + 1) % TONES.size());
            }
        }
        return TONES.get(0);
    }

    public static Component display(int rgb) {
        Tone tone = of(rgb);
        if (tone != null) {
            return Component.translatable(tone.translationKey());
        }
        return Component.literal(String.format("#%06X", rgb & 0xFFFFFF));
    }
}
