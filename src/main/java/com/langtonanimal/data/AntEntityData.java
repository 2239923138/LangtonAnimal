package com.langtonanimal.data;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 挂在载体生物身上的运行数据。
 */
public final class AntEntityData {

    public static final int DEFAULT_SPEED = 1;
    /** 每秒步数上限（20 tick/秒 × 64 步/tick）。 */
    public static final int MAX_SPEED = 1280;
    /** 单 tick 最多推进的步数，防止极端速度卡死服务端。 */
    public static final int MAX_STEPS_PER_TICK = 64;

    public static final Codec<AntEntityData> CODEC =
            CompoundTag.CODEC.xmap(AntEntityData::fromTag, AntEntityData::toTag);

    public static final StreamCodec<io.netty.buffer.ByteBuf, AntEntityData> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(AntEntityData::fromTag, AntEntityData::toTag);

    private String entityName = "";
    private String presetName = "";
    private Direction facing = Direction.NORTH;
    private int speed = DEFAULT_SPEED;
    private boolean running;
    private float stepAccumulator;
    @Nullable
    private BlockPos lastPos;
    private Direction lastFacing = Direction.NORTH;

    public String entityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName == null ? "" : entityName;
    }

    public String presetName() {
        return presetName;
    }

    public void setPresetName(String presetName) {
        this.presetName = presetName == null ? "" : presetName;
    }

    public Direction facing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing == null || facing.getAxis().isVertical() ? Direction.NORTH : facing;
    }

    public int speed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = Math.max(1, Math.min(MAX_SPEED, speed));
    }

    public boolean running() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public float stepAccumulator() {
        return stepAccumulator;
    }

    public void setStepAccumulator(float stepAccumulator) {
        this.stepAccumulator = stepAccumulator;
    }

    @Nullable
    public BlockPos lastPos() {
        return lastPos;
    }

    public Direction lastFacing() {
        return lastFacing;
    }

    public void setSnapshot(@Nullable BlockPos pos, Direction facing) {
        this.lastPos = pos;
        this.lastFacing = facing == null || facing.getAxis().isVertical() ? Direction.NORTH : facing;
    }

    public float yaw() {
        return facing.toYRot();
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("EntityName", entityName);
        tag.putString("PresetName", presetName);
        tag.putByte("Facing", (byte) facing.get3DDataValue());
        tag.putInt("Speed", speed);
        tag.putBoolean("Running", running);
        tag.putFloat("StepAcc", stepAccumulator);
        if (lastPos != null) {
            tag.putLong("LastPos", lastPos.asLong());
        }
        tag.putByte("LastFacing", (byte) lastFacing.get3DDataValue());
        return tag;
    }

    public static AntEntityData fromTag(CompoundTag tag) {
        AntEntityData data = new AntEntityData();
        data.entityName = tag.getString("EntityName");
        data.presetName = tag.getString("PresetName");
        data.setFacing(Direction.from3DDataValue(tag.getByte("Facing")));
        data.setSpeed(tag.getInt("Speed") == 0 ? DEFAULT_SPEED : tag.getInt("Speed"));
        data.running = tag.getBoolean("Running");
        data.stepAccumulator = tag.getFloat("StepAcc");
        data.lastPos = tag.contains("LastPos") ? BlockPos.of(tag.getLong("LastPos")) : null;
        data.setSnapshot(data.lastPos, Direction.from3DDataValue(tag.getByte("LastFacing")));
        return data;
    }
}
