package com.langtonanimal.engine;

import com.langtonanimal.config.AntConfig;
import com.langtonanimal.config.AntPreset;
import com.langtonanimal.config.ConfigManager;
import com.langtonanimal.config.ConfigValidator;
import com.langtonanimal.data.AntBlockHistory;
import com.langtonanimal.data.AntEntityData;
import com.langtonanimal.data.AntRegistry;
import com.langtonanimal.data.ModAttachments;
import com.langtonanimal.network.ModNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * 命令与网络包共用的服务端操作。
 */
public final class AntActions {

    public record Outcome(int affected, @Nullable Component error) {
        public static Outcome ok(int affected) {
            return new Outcome(affected, null);
        }

        public static Outcome fail(Component error) {
            return new Outcome(0, error);
        }
    }

    private AntActions() {
    }

    @Nullable
    public static List<AntRegistry.AntRecord> resolve(MinecraftServer server, String target) {
        AntRegistry registry = AntRegistry.get(server);
        if (AntConfig.ALL.equalsIgnoreCase(target)) {
            return new ArrayList<>(registry.all());
        }
        AntRegistry.AntRecord single = registry.byName(target);
        if (single != null) {
            List<AntRegistry.AntRecord> list = new ArrayList<>(1);
            list.add(single);
            return list;
        }
        if (ConfigManager.get().findPreset(target) != null) {
            return registry.byPreset(target);
        }
        return null;
    }

    public static Outcome setRunning(MinecraftServer server, String target, boolean running) {
        List<AntRegistry.AntRecord> records = resolve(server, target);
        if (records == null) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.target_missing", target));
        }
        if (records.isEmpty()) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.group_empty", target));
        }
        AntRegistry registry = AntRegistry.get(server);
        for (AntRegistry.AntRecord record : records) {
            applyRunning(server, registry, record, running);
        }
        return Outcome.ok(records.size());
    }

    public static void applyRunning(MinecraftServer server, AntRegistry registry,
                                    AntRegistry.AntRecord record, boolean running) {
        record.running = running;
        registry.setDirty();
        ServerLevel level = server.getLevel(record.dimension);
        if (level == null || !level.isLoaded(record.pos)) {
            return;
        }
        Entity entity = level.getEntity(record.uuid);
        if (entity instanceof Mob mob) {
            AntEntityData data = mob.getExistingDataOrNull(ModAttachments.ANT_DATA);
            if (data != null) {
                data.setRunning(running);
                if (running) {
                    data.setStepAccumulator(0.0F);
                }
                mob.syncData(ModAttachments.ANT_DATA);
            }
        }
    }

    public static Outcome setSpeed(MinecraftServer server, String target, int speed) {
        List<AntRegistry.AntRecord> records = resolve(server, target);
        if (records == null) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.target_missing", target));
        }
        if (records.isEmpty()) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.group_empty", target));
        }
        for (AntRegistry.AntRecord record : records) {
            applySpeed(server, record, speed);
        }
        AntRegistry.get(server).setDirty();
        return Outcome.ok(records.size());
    }

    /** 滚轮调速：方向 +1 加速、-1 减速，步长随当前速度增长。 */
    public static Outcome adjustSpeed(MinecraftServer server, String target, int direction) {
        List<AntRegistry.AntRecord> records = resolve(server, target);
        if (records == null) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.target_missing", target));
        }
        if (records.isEmpty()) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.group_empty", target));
        }
        for (AntRegistry.AntRecord record : records) {
            int step = Math.max(1, record.speed / 10);
            applySpeed(server, record, record.speed + direction * step);
        }
        AntRegistry.get(server).setDirty();
        return Outcome.ok(records.size());
    }

    private static void applySpeed(MinecraftServer server, AntRegistry.AntRecord record, int speed) {
        int clamped = Math.max(1, Math.min(AntEntityData.MAX_SPEED, speed));
        record.speed = clamped;
        ServerLevel level = server.getLevel(record.dimension);
        if (level == null || !level.isLoaded(record.pos)) {
            return;
        }
        Entity entity = level.getEntity(record.uuid);
        if (entity instanceof Mob mob) {
            AntEntityData data = mob.getExistingDataOrNull(ModAttachments.ANT_DATA);
            if (data != null) {
                data.setSpeed(clamped);
                data.setStepAccumulator(0.0F);
                mob.syncData(ModAttachments.ANT_DATA);
            }
        }
    }

    public static Outcome remove(MinecraftServer server, String target) {
        List<AntRegistry.AntRecord> records = resolve(server, target);
        if (records == null) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.target_missing", target));
        }
        if (records.isEmpty()) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.group_empty", target));
        }
        AntRegistry registry = AntRegistry.get(server);
        for (AntRegistry.AntRecord record : records) {
            ServerLevel level = server.getLevel(record.dimension);
            if (level != null) {
                Entity entity = level.getEntity(record.uuid);
                if (entity != null) {
                    entity.discard();
                }
            }
            registry.remove(record.uuid);
        }
        ModNetwork.broadcast(server);
        return Outcome.ok(records.size());
    }

    public static Outcome restore(MinecraftServer server, String target) {
        String owner = null;
        String preset = null;
        if (!AntConfig.ALL.equalsIgnoreCase(target)) {
            AntRegistry registry = AntRegistry.get(server);
            if (registry.byName(target) != null) {
                owner = target;
            } else if (ConfigManager.get().findPreset(target) != null) {
                preset = target;
            } else {
                return Outcome.fail(Component.translatable("langtonanimal.msg.target_missing", target));
            }
        }
        int count = AntBlockHistory.get(server).restore(server, owner, preset);
        return Outcome.ok(count);
    }

    /**
     * 把某个生物绑定到指定预设。生物不是兰顿生物时，就地转化成新的兰顿生物。
     */
    public static Outcome assignPreset(ServerPlayer player, UUID uuid, String presetName) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.target_missing", presetName));
        }
        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(uuid);
        if (entity == null || entity.distanceToSqr(player) > 64.0D * 64.0D) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.assign_out_of_range"));
        }
        if (!(entity instanceof Mob mob)) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.assign_not_mob"));
        }
        AntConfig config = ConfigManager.get();
        AntPreset preset = config.findPreset(presetName);
        if (preset == null) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.preset_missing", presetName));
        }
        AntRegistry registry = AntRegistry.get(server);
        AntEntityData data = mob.getExistingDataOrNull(ModAttachments.ANT_DATA);
        if (data != null) {
            data.setPresetName(preset.name());
            data.setSpeed(Math.max(1, data.speed()));
            data.setRunning(true);
            mob.syncData(ModAttachments.ANT_DATA);
            AntRegistry.AntRecord record = registry.byUuid(mob.getUUID());
            if (record != null) {
                record.presetName = preset.name();
                record.running = true;
                registry.setDirty();
            }
            ModNetwork.broadcast(server);
            return Outcome.ok(1);
        }
        String typeId = ConfigValidator.entityTypeId(mob.getType());
        if (!ConfigValidator.isEntityAllowed(typeId)) {
            return Outcome.fail(Component.translatable("langtonanimal.msg.entity_invalid", typeId));
        }
        String name = uniqueName(config, registry, preset.name());
        AntEntityData created = new AntEntityData();
        created.setEntityName(name);
        created.setPresetName(preset.name());
        created.setFacing(mob.getDirection());
        created.setSpeed(1);
        created.setRunning(false);
        mob.setData(ModAttachments.ANT_DATA, created);
        mob.setCustomName(Component.literal(name));
        mob.setCustomNameVisible(true);
        AntEngine.snapToGrid(mob, created.facing());
        AntEngine.applyGuards(mob, created);
        mob.syncData(ModAttachments.ANT_DATA);
        registry.add(new AntRegistry.AntRecord(mob.getUUID(), name, preset.name(), level.dimension(),
                mob.blockPosition(), false, 1));
        ModNetwork.broadcast(server);
        return Outcome.ok(1);
    }

    private static String uniqueName(AntConfig config, AntRegistry registry, String base) {
        String prefix = base.replace(" ", "_");
        int index = 1;
        while (true) {
            String candidate = prefix + "_" + index;
            if (registry.byName(candidate) == null && config.findPreset(candidate) == null) {
                return candidate;
            }
            index++;
        }
    }
}
