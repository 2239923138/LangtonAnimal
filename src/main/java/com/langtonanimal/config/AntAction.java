package com.langtonanimal.config;

import net.minecraft.core.Direction;

/**
 * 转向动作。动作只影响朝向，移动始终沿朝向走一格。
 */
public enum AntAction {
    LEFT("left") {
        @Override
        public Direction apply(Direction facing) {
            return facing.getCounterClockWise();
        }
    },
    RIGHT("right") {
        @Override
        public Direction apply(Direction facing) {
            return facing.getClockWise();
        }
    },
    BACK("back") {
        @Override
        public Direction apply(Direction facing) {
            return facing.getOpposite();
        }
    },
    STRAIGHT("straight") {
        @Override
        public Direction apply(Direction facing) {
            return facing;
        }
    };

    private final String id;

    AntAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public abstract Direction apply(Direction facing);

    public AntAction cycle() {
        AntAction[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static AntAction byId(String id) {
        if (id != null) {
            for (AntAction action : values()) {
                if (action.id.equalsIgnoreCase(id) || action.name().equalsIgnoreCase(id)) {
                    return action;
                }
            }
        }
        return RIGHT;
    }
}
