package com.langtonanimal.client;

import com.langtonanimal.config.AntConfig;
import com.langtonanimal.data.AntEntityData;
import com.langtonanimal.data.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * 烈焰棒交互状态：模式、作用范围、当前选中的生物。
 */
public final class RodState {

    public static final int MODE_SELECT = 0;
    public static final int MODE_EDIT = 1;
    public static final int MODE_RUN = 2;
    public static final int MODE_SPEED = 3;
    public static final int MODE_COUNT = 4;

    public static final int SCOPE_SINGLE = 0;
    public static final int SCOPE_PRESET = 1;
    public static final int SCOPE_ALL = 2;
    public static final int SCOPE_COUNT = 3;

    public static final int HIGHLIGHT_ALL_COLOR = 0xFFFFFF;

    private static int mode = MODE_SELECT;
    private static int scope = SCOPE_SINGLE;
    @Nullable
    private static UUID selected;
    @Nullable
    private static String editingName;
    private static boolean editingWasRunning;

    private RodState() {
    }

    public static int mode() {
        return mode;
    }

    public static int scope() {
        return scope;
    }

    public static void cycleMode(int direction) {
        mode = Math.floorMod(mode + direction, MODE_COUNT);
    }

    public static void cycleScope(int direction) {
        scope = Math.floorMod(scope + direction, SCOPE_COUNT);
    }

    public static void select(Entity entity) {
        selected = entity.getUUID();
    }

    public static boolean hasSelection() {
        return selected != null;
    }

    @Nullable
    public static Entity selectedEntity() {
        if (selected == null || Minecraft.getInstance().level == null) {
            return null;
        }
        for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            if (entity.getUUID().equals(selected)) {
                return entity;
            }
        }
        return null;
    }

    @Nullable
    public static AntEntityData selectedData() {
        Entity entity = selectedEntity();
        return entity == null ? null : entity.getExistingDataOrNull(ModAttachments.ANT_DATA);
    }

    /** 当前作用范围内的兰顿生物。 */
    public static List<Entity> targets() {
        Entity selectedEntity = selectedEntity();
        if (selectedEntity == null) {
            return List.of();
        }
        AntEntityData data = selectedEntity.getExistingDataOrNull(ModAttachments.ANT_DATA);
        if (data == null) {
            return List.of();
        }
        if (scope == SCOPE_SINGLE) {
            return List.of(selectedEntity);
        }
        List<Entity> result = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return result;
        }
        for (Entity entity : level.entitiesForRendering()) {
            AntEntityData other = entity.getExistingDataOrNull(ModAttachments.ANT_DATA);
            if (other == null) {
                continue;
            }
            if (scope == SCOPE_ALL || other.presetName().equals(data.presetName())) {
                result.add(entity);
            }
        }
        return result;
    }

    /** 发给服务端的目标标识：生物名 / 预设名 / all。 */
    @Nullable
    public static String targetName() {
        AntEntityData data = selectedData();
        if (data == null) {
            return scope == SCOPE_ALL ? AntConfig.ALL : null;
        }
        return switch (scope) {
            case SCOPE_PRESET -> data.presetName();
            case SCOPE_ALL -> AntConfig.ALL;
            default -> data.entityName();
        };
    }

    public static int colorFor(Entity entity) {
        if (scope == SCOPE_ALL) {
            return HIGHLIGHT_ALL_COLOR;
        }
        AntEntityData data = entity.getExistingDataOrNull(ModAttachments.ANT_DATA);
        return data == null ? HIGHLIGHT_ALL_COLOR : ClientState.colorOf(data.presetName());
    }

    public static void beginEdit(String entityName, boolean wasRunning) {
        editingName = entityName;
        editingWasRunning = wasRunning;
    }

    public static void endEdit() {
        editingName = null;
        editingWasRunning = false;
    }

    @Nullable
    public static String editingName() {
        return editingName;
    }

    public static boolean editingWasRunning() {
        return editingWasRunning;
    }
}
