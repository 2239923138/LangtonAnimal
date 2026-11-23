package com.langtonanimal.config;

/**
 * 一条规则：条件方块 -> 转向动作 + 目标替换方块。
 *
 * <p>{@code condition} 与 {@code target} 都是字符串标识符，可能是具体方块 ID，
 * 也可能是 {@link AntConfig#DEFAULT_BLOCK} / {@link AntConfig#KEEP} 这类语义标识符。
 */
public record RuleEntry(String condition, AntAction action, String target) {

    public RuleEntry withCondition(String condition) {
        return new RuleEntry(condition, action, target);
    }

    public RuleEntry withAction(AntAction action) {
        return new RuleEntry(condition, action, target);
    }

    public RuleEntry withTarget(String target) {
        return new RuleEntry(condition, action, target);
    }

    public RuleEntry copy() {
        return new RuleEntry(condition, action, target);
    }
}
