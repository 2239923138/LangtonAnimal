package com.langtonanimal.client;

import com.langtonanimal.config.AntConfig;
import com.langtonanimal.config.AntPreset;
import com.langtonanimal.config.ConfigManager;
import com.langtonanimal.network.SaveConfigPayload;
import java.util.List;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端侧配置缓存与编辑草稿。
 */
public final class ClientState {

    private static AntConfig synced = new AntConfig();
    private static List<String> liveNames = List.of();
    private static boolean syncedOnce;
    private static AntConfig draft;

    private ClientState() {
    }

    public static void acceptSync(String json, List<String> names) {
        synced = AntConfig.fromJson(json);
        liveNames = names == null ? List.of() : List.copyOf(names);
        syncedOnce = true;
    }

    public static AntConfig synced() {
        return synced;
    }

    public static int colorOf(String presetName) {
        AntPreset preset = presetName == null ? null : synced.findPreset(presetName);
        return preset == null ? 0xFFFFFF : preset.color();
    }

    public static List<String> liveNames() {
        return liveNames;
    }

    /** 打开界面时调用：以服务端同步内容（或本地文件）为基准建立草稿。 */
    public static void beginSession() {
        draft = syncedOnce ? synced.copy() : ConfigManager.get().copy();
    }

    public static AntConfig draft() {
        if (draft == null) {
            beginSession();
        }
        return draft;
    }

    public static void discardDraft() {
        draft = null;
    }

    public static void submit(AntConfig config) {
        PacketDistributor.sendToServer(new SaveConfigPayload(config.toJson()));
        discardDraft();
    }
}
