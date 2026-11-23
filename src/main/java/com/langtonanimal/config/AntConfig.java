package com.langtonanimal.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * 全局配置：外层默认初始方块 + 全部预设。
 */
public final class AntConfig {

    public static final String DEFAULT_BLOCK = "langtonanimal:default_block";
    public static final String KEEP = "langtonanimal:keep";
    /** 命令中代表「所有兰顿生物」的保留词。 */
    public static final String ALL = "all";
    public static final String FALLBACK_DEFAULT_BLOCK = "minecraft:grass_block";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private String defaultBlockId = FALLBACK_DEFAULT_BLOCK;
    private final List<AntPreset> presets = new ArrayList<>();

    public String defaultBlockId() {
        return defaultBlockId;
    }

    public void setDefaultBlockId(String id) {
        this.defaultBlockId = BlockPalette.normalize(id);
    }

    public List<AntPreset> presets() {
        return presets;
    }

    @Nullable
    public AntPreset findPreset(String name) {
        if (name == null) {
            return null;
        }
        for (AntPreset preset : presets) {
            if (preset.name().equals(name)) {
                return preset;
            }
        }
        return null;
    }

    public boolean removePreset(String name) {
        return presets.removeIf(p -> p.name().equals(name));
    }

    public AntConfig copy() {
        AntConfig copy = new AntConfig();
        copy.defaultBlockId = defaultBlockId;
        for (AntPreset preset : presets) {
            copy.presets.add(preset.copy());
        }
        return copy;
    }

    /** 新建预设的默认模板：一条针对默认初始方块的规则。 */
    public static AntPreset newTemplatePreset(String name) {
        AntPreset preset = new AntPreset(name, AntPreset.DEFAULT_ENTITY);
        preset.rules().add(new RuleEntry(DEFAULT_BLOCK, AntAction.RIGHT, "minecraft:white_wool"));
        preset.rules().add(new RuleEntry("minecraft:white_wool", AntAction.LEFT, DEFAULT_BLOCK));
        return preset;
    }

    public String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("defaultBlockId", defaultBlockId);
        JsonArray presetArray = new JsonArray();
        for (AntPreset preset : presets) {
            JsonObject presetObject = new JsonObject();
            presetObject.addProperty("name", preset.name());
            presetObject.addProperty("entityType", preset.entityType());
            presetObject.addProperty("color", String.format("%06X", preset.color()));
            JsonArray ruleArray = new JsonArray();
            for (RuleEntry rule : preset.rules()) {
                JsonObject ruleObject = new JsonObject();
                ruleObject.addProperty("condition", rule.condition());
                ruleObject.addProperty("action", rule.action().id());
                ruleObject.addProperty("target", rule.target());
                ruleArray.add(ruleObject);
            }
            presetObject.add("rules", ruleArray);
            presetArray.add(presetObject);
        }
        root.add("presets", presetArray);
        return GSON.toJson(root);
    }

    private static int parseColor(String raw) {
        if (raw == null) {
            return AntPreset.DEFAULT_COLOR;
        }
        String value = raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException ex) {
            return AntPreset.DEFAULT_COLOR;
        }
    }

    public static AntConfig fromJson(String json) {
        AntConfig config = new AntConfig();
        if (json == null || json.isBlank()) {
            return config;
        }
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(json);
        } catch (RuntimeException ex) {
            return config;
        }
        if (!parsed.isJsonObject()) {
            return config;
        }
        JsonObject root = parsed.getAsJsonObject();
        if (root.has("defaultBlockId") && root.get("defaultBlockId").isJsonPrimitive()) {
            config.defaultBlockId = BlockPalette.normalize(root.get("defaultBlockId").getAsString());
        }
        if (root.has("presets") && root.get("presets").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("presets")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject presetObject = element.getAsJsonObject();
                String name = presetObject.has("name") ? presetObject.get("name").getAsString() : "";
                String entityType = presetObject.has("entityType")
                        ? presetObject.get("entityType").getAsString()
                        : AntPreset.DEFAULT_ENTITY;
                AntPreset preset = new AntPreset(name, entityType);
                if (presetObject.has("color") && presetObject.get("color").isJsonPrimitive()) {
                    preset.setColor(parseColor(presetObject.get("color").getAsString()));
                }
                if (presetObject.has("rules") && presetObject.get("rules").isJsonArray()) {
                    for (JsonElement ruleElement : presetObject.getAsJsonArray("rules")) {
                        if (!ruleElement.isJsonObject()) {
                            continue;
                        }
                        JsonObject ruleObject = ruleElement.getAsJsonObject();
                        String condition = ruleObject.has("condition")
                                ? ruleObject.get("condition").getAsString()
                                : DEFAULT_BLOCK;
                        String target = ruleObject.has("target") ? ruleObject.get("target").getAsString() : KEEP;
                        AntAction action = AntAction.byId(ruleObject.has("action")
                                ? ruleObject.get("action").getAsString()
                                : "right");
                        preset.rules().add(new RuleEntry(condition, action, target));
                    }
                }
                config.presets.add(preset);
            }
        }
        return config;
    }
}
