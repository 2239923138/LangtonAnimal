package com.langtonanimal.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 地面改动历史：每个坐标只记录第一次被改成规则方块之前的原始状态，用于批量复原。
 */
public final class AntBlockHistory extends SavedData {

    public static final String FILE_ID = "langtonanimal_blocks";
    /** 记录上限，防止高速长时间运行把存档撑爆。超出后不再记录新的坐标。 */
    public static final int MAX_ENTRIES = 200_000;

    private record BlockKey(String dimension, long pos) {
    }

    private static final class Entry {
        final ResourceKey<Level> dimension;
        final BlockPos pos;
        final int stateId;
        final String owner;
        final String preset;

        Entry(ResourceKey<Level> dimension, BlockPos pos, int stateId, String owner, String preset) {
            this.dimension = dimension;
            this.pos = pos;
            this.stateId = stateId;
            this.owner = owner;
            this.preset = preset;
        }
    }

    private final Map<BlockKey, Entry> entries = new LinkedHashMap<>();

    public static AntBlockHistory get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(AntBlockHistory::new, AntBlockHistory::load), FILE_ID);
    }

    /** 首次触碰才记录，重复经过同一格不覆盖原始状态。 */
    public void record(ResourceKey<Level> dimension, BlockPos pos, BlockState original, String owner, String preset) {
        if (entries.size() >= MAX_ENTRIES) {
            return;
        }
        BlockKey key = new BlockKey(dimension.location().toString(), pos.asLong());
        if (entries.containsKey(key)) {
            return;
        }
        entries.put(key, new Entry(dimension, pos.immutable(), Block.getId(original), owner, preset));
        setDirty();
    }

    /**
     * 复原方块。{@code owner} 为生物名，{@code preset} 为预设名，都为 null 表示全部。
     *
     * @return 实际复原的方块数量
     */
    public int restore(MinecraftServer server, @Nullable String owner, @Nullable String preset) {
        boolean everything = owner == null && preset == null;
        List<BlockKey> restored = new ArrayList<>();
        int count = 0;
        for (Map.Entry<BlockKey, Entry> mapEntry : entries.entrySet()) {
            Entry entry = mapEntry.getValue();
            if (!everything && !entry.owner.equals(owner) && !entry.preset.equals(preset)) {
                continue;
            }
            ServerLevel level = server.getLevel(entry.dimension);
            if (level == null || !level.isLoaded(entry.pos)) {
                continue;
            }
            BlockState state = Block.stateById(entry.stateId);
            level.setBlockAndUpdate(entry.pos, state);
            restored.add(mapEntry.getKey());
            count++;
        }
        for (BlockKey key : restored) {
            entries.remove(key);
        }
        if (count > 0) {
            setDirty();
        }
        return count;
    }

    public int size() {
        return entries.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag item = new CompoundTag();
            item.putString("Dim", entry.dimension.location().toString());
            item.putLong("Pos", entry.pos.asLong());
            item.putInt("State", entry.stateId);
            item.putString("Owner", entry.owner);
            item.putString("Preset", entry.preset);
            list.add(item);
        }
        tag.put("Blocks", list);
        return tag;
    }

    public static AntBlockHistory load(CompoundTag tag, HolderLookup.Provider registries) {
        AntBlockHistory history = new AntBlockHistory();
        ListTag list = tag.getList("Blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            ResourceLocation dimensionId = ResourceLocation.tryParse(item.getString("Dim"));
            if (dimensionId == null) {
                continue;
            }
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            BlockPos pos = BlockPos.of(item.getLong("Pos"));
            Entry entry = new Entry(dimension, pos, item.getInt("State"),
                    item.getString("Owner"), item.getString("Preset"));
            history.entries.put(new BlockKey(dimensionId.toString(), pos.asLong()), entry);
        }
        return history;
    }
}
