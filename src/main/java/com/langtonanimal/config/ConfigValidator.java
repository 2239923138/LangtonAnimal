package com.langtonanimal.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * 保存前的完备性校验，客户端与服务端共用同一套规则。
 */
public final class ConfigValidator {

    /** 明确排除的 Boss 类实体。 */
    private static final Set<String> BANNED_ENTITIES = Set.of("minecraft:ender_dragon", "minecraft:wither");

    private ConfigValidator() {
    }

    public static boolean isEntityAllowed(String entityId) {
        if (entityId == null) {
            return false;
        }
        String normalized = entityId.contains(":") ? entityId : "minecraft:" + entityId;
        if (BANNED_ENTITIES.contains(normalized)) {
            return false;
        }
        ResourceLocation rl = ResourceLocation.tryParse(normalized);
        if (rl == null) {
            return false;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
        if (type == null) {
            return false;
        }
        return type.getCategory() != MobCategory.MISC;
    }

    public static List<String> allowedEntityIds() {
        List<String> ids = new ArrayList<>();
        for (ResourceLocation key : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (isEntityAllowed(key.toString())) {
                ids.add(key.toString());
            }
        }
        ids.sort(String::compareTo);
        return ids;
    }

    public static String entityTypeId(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    @Nullable
    public static Component displayEntityName(String entityId) {
        String normalized = entityId.contains(":") ? entityId : "minecraft:" + entityId;
        ResourceLocation rl = ResourceLocation.tryParse(normalized);
        if (rl == null) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
        return type == null ? null : type.getDescription();
    }

    public static List<Component> validate(AntConfig config, Collection<String> liveEntityNames) {
        List<Component> errors = new ArrayList<>();
        String defaultBlock = BlockPalette.normalize(config.defaultBlockId());
        if (!BlockPalette.isValidBlockId(defaultBlock)) {
            errors.add(Component.translatable("langtonanimal.val.default_block_invalid", defaultBlock));
        }

        Set<String> seenPresets = new HashSet<>();
        Set<String> liveNames = new HashSet<>(liveEntityNames);

        for (AntPreset preset : config.presets()) {
            String name = preset.name() == null ? "" : preset.name().trim();
            String label = name.isEmpty() ? "?" : name;

            if (name.isEmpty()) {
                errors.add(Component.translatable("langtonanimal.val.name_empty"));
            } else if (name.equalsIgnoreCase(AntConfig.ALL)) {
                errors.add(Component.translatable("langtonanimal.val.name_reserved", name));
            } else if (!name.equals(preset.name()) || name.contains(" ")) {
                errors.add(Component.translatable("langtonanimal.val.name_illegal", preset.name()));
            } else if (!seenPresets.add(name)) {
                errors.add(Component.translatable("langtonanimal.val.name_dup_preset", name));
            } else if (liveNames.contains(name)) {
                errors.add(Component.translatable("langtonanimal.val.name_dup_entity", name));
            }

            String entityId = preset.entityType();
            if (!isEntityAllowed(entityId)) {
                if (entityId != null && BANNED_ENTITIES.contains(entityId)) {
                    errors.add(Component.translatable("langtonanimal.val.entity_banned", label, entityId));
                } else {
                    errors.add(Component.translatable("langtonanimal.val.entity_invalid", label,
                            entityId == null ? "null" : entityId));
                }
            }

            List<RuleEntry> rules = preset.rules();
            if (rules.isEmpty()) {
                continue;
            }

            Set<String> conditions = new HashSet<>();
            for (RuleEntry rule : rules) {
                String conditionId = BlockPalette.resolveId(rule.condition(), defaultBlock);
                if (conditionId == null || AntConfig.KEEP.equals(conditionId)
                        || !BlockPalette.isValidBlockId(conditionId)) {
                    errors.add(Component.translatable("langtonanimal.val.rule_cond_invalid", label,
                            String.valueOf(rule.condition())));
                } else if (!conditions.add(conditionId)) {
                    errors.add(Component.translatable("langtonanimal.val.cond_dup", label,
                            BlockPalette.displayName(conditionId, defaultBlock)));
                }

                String targetId = BlockPalette.resolveId(rule.target(), defaultBlock);
                if (!AntConfig.KEEP.equals(targetId)
                        && (targetId == null || !BlockPalette.isValidBlockId(targetId))) {
                    errors.add(Component.translatable("langtonanimal.val.rule_target_invalid", label,
                            String.valueOf(rule.target())));
                }
            }

            if (!conditions.contains(defaultBlock)) {
                errors.add(Component.translatable("langtonanimal.val.no_default_rule", label,
                        BlockPalette.displayName(defaultBlock, defaultBlock)));
            }

            for (RuleEntry rule : rules) {
                String targetId = BlockPalette.resolveId(rule.target(), defaultBlock);
                if (AntConfig.KEEP.equals(targetId)) {
                    continue;
                }
                if (targetId != null && BlockPalette.isValidBlockId(targetId) && !conditions.contains(targetId)) {
                    errors.add(Component.translatable("langtonanimal.val.no_closure", label,
                            BlockPalette.displayName(targetId, defaultBlock)));
                }
            }
        }
        return errors;
    }
}
