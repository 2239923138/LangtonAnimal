package com.langtonanimal.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 全服受控生物索引。存放在主世界的 {@link SavedData} 中，
 * 即使生物所在区块未加载也能查询与做重名校验。
 */
public final class AntRegistry extends SavedData {

    public static final String FILE_ID = "langtonanimal_ants";

    public static final class AntRecord {
        public final UUID uuid;
        public String entityName;
        public String presetName;
        public ResourceKey<Level> dimension;
        public BlockPos pos;
        public boolean running;
        public int speed;

        public AntRecord(UUID uuid, String entityName, String presetName, ResourceKey<Level> dimension,
                         BlockPos pos, boolean running, int speed) {
            this.uuid = uuid;
            this.entityName = entityName;
            this.presetName = presetName;
            this.dimension = dimension;
            this.pos = pos;
            this.running = running;
            this.speed = speed;
        }
    }

    private final Map<UUID, AntRecord> byUuid = new LinkedHashMap<>();

    public static AntRegistry get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(AntRegistry::new, AntRegistry::load), FILE_ID);
    }

    public Collection<AntRecord> all() {
        return new ArrayList<>(byUuid.values());
    }

    @Nullable
    public AntRecord byUuid(UUID uuid) {
        return byUuid.get(uuid);
    }

    @Nullable
    public AntRecord byName(String name) {
        for (AntRecord record : byUuid.values()) {
            if (record.entityName.equals(name)) {
                return record;
            }
        }
        return null;
    }

    public boolean containsName(String name) {
        return byName(name) != null;
    }

    public List<String> names() {
        List<String> names = new ArrayList<>(byUuid.size());
        for (AntRecord record : byUuid.values()) {
            names.add(record.entityName);
        }
        return names;
    }

    public List<AntRecord> byPreset(String presetName) {
        List<AntRecord> result = new ArrayList<>();
        for (AntRecord record : byUuid.values()) {
            if (record.presetName.equals(presetName)) {
                result.add(record);
            }
        }
        return result;
    }

    public void add(AntRecord record) {
        byUuid.put(record.uuid, record);
        setDirty();
    }

    public void remove(UUID uuid) {
        if (byUuid.remove(uuid) != null) {
            setDirty();
        }
    }

    public boolean isEmpty() {
        return byUuid.isEmpty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (AntRecord record : byUuid.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Uuid", record.uuid);
            entry.putString("Name", record.entityName);
            entry.putString("Preset", record.presetName);
            entry.putString("Dimension", record.dimension.location().toString());
            entry.putLong("Pos", record.pos.asLong());
            entry.putBoolean("Running", record.running);
            entry.putInt("Speed", record.speed);
            list.add(entry);
        }
        tag.put("Ants", list);
        return tag;
    }

    public static AntRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        AntRegistry registry = new AntRegistry();
        ListTag list = tag.getList("Ants", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Uuid")) {
                continue;
            }
            ResourceLocation dimensionId = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dimensionId == null) {
                dimensionId = Level.OVERWORLD.location();
            }
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            AntRecord record = new AntRecord(
                    entry.getUUID("Uuid"),
                    entry.getString("Name"),
                    entry.getString("Preset"),
                    dimension,
                    BlockPos.of(entry.getLong("Pos")),
                    entry.getBoolean("Running"),
                    Math.max(1, entry.getInt("Speed")));
            registry.byUuid.put(record.uuid, record);
        }
        return registry;
    }
}
