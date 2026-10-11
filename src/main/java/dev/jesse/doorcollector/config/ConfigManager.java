package dev.jesse.doorcollector.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jesse.doorcollector.DoorCollectorClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("better-routes-door-collector");
    private static final Path FILE = ROOT.resolve("config.json");
    private static CollectorConfig config = new CollectorConfig();

    private ConfigManager() {}

    public static void load() {
        try {
            Files.createDirectories(ROOT);
            if (Files.exists(FILE)) {
                try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                    CollectorConfig loaded = GSON.fromJson(reader, CollectorConfig.class);
                    if (loaded != null) config = loaded;
                }
            } else save();
        } catch (Exception e) { DoorCollectorClient.LOG.error("Could not load collector config", e); }
    }

    public static void save() {
        try {
            Files.createDirectories(ROOT);
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) { GSON.toJson(config, writer); }
        } catch (Exception e) { DoorCollectorClient.LOG.error("Could not save collector config", e); }
    }

    public static CollectorConfig get() { return config; }
    public static Path root() { return ROOT; }
}
