package com.langtonanimal.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 预设：一种生物载体 + 一组规则 + 一个显示颜色。
 */
public final class AntPreset {

    public static final String DEFAULT_ENTITY = "minecraft:silverfish";
    public static final int DEFAULT_COLOR = 0x6FB2FF;

    private String name;
    private String entityType;
    private int color = DEFAULT_COLOR;
    private final List<RuleEntry> rules = new ArrayList<>();

    public AntPreset() {
        this("", DEFAULT_ENTITY);
    }

    public AntPreset(String name, String entityType) {
        this.name = name == null ? "" : name;
        this.entityType = entityType == null ? DEFAULT_ENTITY : entityType;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String entityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType == null ? DEFAULT_ENTITY : entityType;
    }

    public int color() {
        return color;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
    }

    public List<RuleEntry> rules() {
        return rules;
    }

    public AntPreset copy() {
        AntPreset copy = new AntPreset(name, entityType);
        copy.color = color;
        for (RuleEntry rule : rules) {
            copy.rules.add(rule.copy());
        }
        return copy;
    }

    /**
     * 查找匹配当前脚下方块的规则。{@code defaultBlockId} 用于解析语义标识符。
     */
    public RuleEntry findRule(String currentBlockId, String defaultBlockId) {
        if (currentBlockId == null) {
            return null;
        }
        for (RuleEntry rule : rules) {
            String resolved = BlockPalette.resolveId(rule.condition(), defaultBlockId);
            if (currentBlockId.equals(resolved)) {
                return rule;
            }
        }
        return null;
    }
}
