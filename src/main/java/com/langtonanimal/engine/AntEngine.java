package com.langtonanimal.engine;

import com.langtonanimal.config.AntConfig;
import com.langtonanimal.config.AntPreset;
import com.langtonanimal.config.BlockPalette;
import com.langtonanimal.config.ConfigManager;
import com.langtonanimal.config.RuleEntry;
import com.langtonanimal.data.AntBlockHistory;
import com.langtonanimal.data.AntEntityData;
import com.langtonanimal.data.AntRegistry;
import com.langtonanimal.data.ModAttachments;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 服务端步进引擎：网格对齐、单步执行、异常回退与载体行为防护。
 */
public final class AntEngine {

    private AntEngine() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        AntRegistry registry = AntRegistry.get(server);
        if (registry.isEmpty()) {
            return;
        }
        AntConfig config = ConfigManager.get();
        AntBlockHistory history = AntBlockHistory.get(server);
        for (AntRegistry.AntRecord record : registry.all()) {
            ServerLevel level = server.getLevel(record.dimension);
            if (level == null || !level.isLoaded(record.pos)) {
                continue;
            }
            Entity entity = level.getEntity(record.uuid);
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            AntEntityData data = mob.getExistingDataOrNull(ModAttachments.ANT_DATA);
            if (data == null) {
                continue;
            }
            applyGuards(mob, data);
            if (!data.running()) {
                data.setStepAccumulator(0.0F);
                continue;
            }
            float accumulated = data.stepAccumulator() + data.speed() / 20.0F;
            int steps = (int) accumulated;
            if (steps <= 0) {
                data.setStepAccumulator(accumulated);
                continue;
            }
            data.setStepAccumulator(accumulated - steps);
            steps = Math.min(steps, AntEntityData.MAX_STEPS_PER_TICK);
            for (int i = 0; i < steps; i++) {
                if (!step(server, level, mob, data, record, registry, config, history)) {
                    break;
                }
            }
        }
    }

    /**
     * 每 tick 施加的防护：禁止原版 AI、静音、无敌、禁刷没、关闭重力、清火、清目标。
     */
    public static void applyGuards(Mob mob, AntEntityData data) {
        mob.setNoAi(true);
        mob.setSilent(true);
        mob.setInvulnerable(true);
        mob.setPersistenceRequired();
        mob.setNoGravity(true);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.setTarget(null);
        if (mob.getRemainingFireTicks() > 0 || mob.isOnFire()) {
            mob.clearFire();
        }
        if (mob.getAirSupply() < mob.getMaxAirSupply()) {
            mob.setAirSupply(mob.getMaxAirSupply());
        }
        String name = data.entityName();
        if (!name.isEmpty()) {
            Component current = mob.getCustomName();
            if (current == null || !current.getString().equals(name)) {
                mob.setCustomName(Component.literal(name));
            }
        }
        if (!mob.isCustomNameVisible()) {
            mob.setCustomNameVisible(true);
        }
    }

    private static boolean step(MinecraftServer server, ServerLevel level, Mob mob, AntEntityData data,
                                AntRegistry.AntRecord record, AntRegistry registry, AntConfig config,
                                AntBlockHistory history) {
        String presetName = data.presetName();
        AntPreset preset = config.findPreset(presetName);
        if (preset == null) {
            abort(server, level, mob, data, record, registry,
                    Component.translatable("langtonanimal.error.preset_missing", presetName));
            return false;
        }
        if (preset.rules().isEmpty()) {
            return false;
        }

        BlockPos foot = mob.blockPosition();
        BlockState below = level.getBlockState(foot.below());
        String currentBlockId = BlockPalette.idOf(below.getBlock());
        RuleEntry rule = preset.findRule(currentBlockId, config.defaultBlockId());
        if (rule == null) {
            abort(server, level, mob, data, record, registry,
                    Component.translatable("langtonanimal.error.unexpected_block", below.getBlock().getName()));
            return false;
        }

        Direction previousFacing = data.facing();
        Direction newFacing = rule.action().apply(previousFacing);

        if (!BlockPalette.isKeep(rule.target())) {
            Block targetBlock = BlockPalette.resolveBlock(rule.target(), config.defaultBlockId());
            if (targetBlock != null && targetBlock != below.getBlock()) {
                history.record(level.dimension(), foot.below(), below, data.entityName(), data.presetName());
                level.setBlockAndUpdate(foot.below(), targetBlock.defaultBlockState());
            }
        }

        BlockPos next = foot.relative(newFacing);
        if (!level.isLoaded(next)) {
            abort(server, level, mob, data, record, registry,
                    Component.translatable("langtonanimal.error.unloaded"));
            return false;
        }

        AABB targetBox = mob.getDimensions(mob.getPose())
                .makeBoundingBox(next.getX() + 0.5D, next.getY(), next.getZ() + 0.5D);
        if (!level.noCollision(mob, targetBox)) {
            abort(server, level, mob, data, record, registry,
                    Component.translatable("langtonanimal.error.blocked"));
            return false;
        }
        BlockPos supportPos = next.below();
        if (!level.getBlockState(supportPos).isFaceSturdy(level, supportPos, Direction.UP)) {
            abort(server, level, mob, data, record, registry,
                    Component.translatable("langtonanimal.error.suspended"));
            return false;
        }

        data.setSnapshot(foot, previousFacing);
        data.setFacing(newFacing);
        moveTo(mob, next, newFacing);
        record.pos = next;
        registry.setDirty();
        return true;
    }

    private static void moveTo(Mob mob, BlockPos pos, Direction facing) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;
        mob.teleportTo(x, y, z);
        mob.setPos(x, y, z);
        float yaw = facing.toYRot();
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setDeltaMovement(Vec3.ZERO);
    }

    private static void abort(MinecraftServer server, ServerLevel level, Mob mob, AntEntityData data,
                              AntRegistry.AntRecord record, AntRegistry registry, Component reason) {
        data.setRunning(false);
        record.running = false;
        registry.setDirty();

        BlockPos lastPos = data.lastPos();
        if (lastPos != null && level.isLoaded(lastPos)) {
            moveTo(mob, lastPos, data.lastFacing());
            data.setFacing(data.lastFacing());
            record.pos = lastPos;
            data.setSnapshot(null, data.lastFacing());
        }
        mob.syncData(ModAttachments.ANT_DATA);

        Component message = Component.translatable("langtonanimal.msg.error",
                        Component.literal(data.entityName()).withStyle(ChatFormatting.WHITE),
                        Component.literal("[" + record.pos.getX() + ", " + record.pos.getY() + ", "
                                + record.pos.getZ() + "]").withStyle(ChatFormatting.GRAY),
                        reason)
                .withStyle(ChatFormatting.RED);
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        AntRegistry registry = AntRegistry.get(level.getServer());
        AntRegistry.AntRecord record = registry.byUuid(mob.getUUID());
        if (record == null) {
            return;
        }
        AntEntityData data = mob.getExistingDataOrNull(ModAttachments.ANT_DATA);
        if (data == null) {
            return;
        }
        data.setEntityName(record.entityName);
        data.setPresetName(record.presetName);
        data.setRunning(record.running);
        data.setSpeed(record.speed);
        record.pos = mob.blockPosition();
        applyGuards(mob, data);
        mob.syncData(ModAttachments.ANT_DATA);
        registry.setDirty();
    }

    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (mob.getExistingDataOrNull(ModAttachments.ANT_DATA) == null) {
            return;
        }
        MinecraftServer server = mob.getServer();
        if (server == null) {
            return;
        }
        AntRegistry registry = AntRegistry.get(server);
        if (registry.byUuid(mob.getUUID()) != null) {
            registry.remove(mob.getUUID());
        }
    }

    /** 把生物放回格中心并重置朝向，用于召唤与手动修正。 */
    public static void snapToGrid(Mob mob, @Nullable Direction facing) {
        BlockPos pos = mob.blockPosition();
        mob.teleportTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        mob.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        if (facing != null) {
            mob.setYRot(facing.toYRot());
            mob.setYHeadRot(facing.toYRot());
            mob.setYBodyRot(facing.toYRot());
        }
        mob.setDeltaMovement(Vec3.ZERO);
    }
}
