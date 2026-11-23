package com.langtonanimal.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局配置文件的读写。文件位于 {@code <game dir>/config/langtonanimal/presets.json}，跨存档通用。
 */
public final class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("LangtonAnimal/Config");
    private static AntConfig config = new AntConfig();
    private static boolean loaded;

    private ConfigManager() {
    }

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("langtonanimal").resolve("presets.json");
    }

    public static synchronized AntConfig get() {
        ensureLoaded();
        return config;
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        reload();
    }

    public static synchronized void reload() {
        Path path = file();
        if (!Files.exists(path)) {
            config = new AntConfig();
            LOGGER.info("No config found at {}, using defaults", path);
            return;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            config = AntConfig.fromJson(json);
            LOGGER.info("Loaded {} preset(s) from {}", config.presets().size(), path);
        } catch (IOException ex) {
            LOGGER.error("Failed to read {}", path, ex);
        }
    }

    public static synchronized void replace(AntConfig newConfig) {
        config = newConfig;
        loaded = true;
        save();
    }

    public static synchronized void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, config.toJson(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.error("Failed to write {}", path, ex);
        }
    }
}
