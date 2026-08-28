package io.github.lithum12.trackertips.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.lithum12.trackertips.TrackerTips;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class TTConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static TTSettings settings = new TTSettings();
    private static final Map<ResourceLocation, HintDefinition> HINTS = new LinkedHashMap<>();

    public static TTSettings settings() {
        return settings;
    }

    public static Collection<HintDefinition> hints() {
        return HINTS.values();
    }


    /** Client GUI helper: reads the global settings file without requiring a running server. */
    public static TTSettings readGlobalSettings() {
        try {
            ensureGlobalDefaults();
            Path cfg = globalFolder().resolve("global_config.json");
            return GSON.fromJson(Files.readString(cfg, StandardCharsets.UTF_8), TTSettings.class);
        } catch (Exception e) {
            TrackerTips.LOGGER.error("Failed to read global TrackerTips settings", e);
            return new TTSettings();
        }
    }

    /** Client GUI helper: writes the global settings file. */
    public static void saveGlobalSettings(TTSettings value) {
        try {
            Files.createDirectories(globalFolder());
            Files.writeString(globalFolder().resolve("global_config.json"),
                    GSON.toJson(value), StandardCharsets.UTF_8);
        } catch (Exception e) {
            TrackerTips.LOGGER.error("Failed to save global TrackerTips settings", e);
        }
    }

    public static Path globalFolder() {
        return FMLPaths.CONFIGDIR.get().resolve(TrackerTips.MODID);
    }

    public static Path worldFolder(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(TrackerTips.MODID);
    }

    public static void init(MinecraftServer server) {
        try {
            ensureGlobalDefaults();
            copyToWorldIfAbsent(server);
            load(server);
        } catch (IOException e) {
            TrackerTips.LOGGER.error("[TrackerTips] Failed to load configuration", e);
        }
    }

    public static void reload(MinecraftServer server) {
        try {
            ensureGlobalDefaults();
            load(server);
            TrackerTips.LOGGER.info("[TrackerTips] Configuration reloaded; loaded {} hint definitions.", HINTS.size());
        } catch (IOException e) {
            TrackerTips.LOGGER.error("[TrackerTips] Failed to reload configuration", e);
        }
    }

    private static void ensureGlobalDefaults() throws IOException {
        Path folder = globalFolder();
        Files.createDirectories(folder.resolve("hints"));

        Path cfg = folder.resolve("global_config.json");
        if (!Files.exists(cfg)) {
            Files.writeString(cfg, GSON.toJson(new TTSettings()), StandardCharsets.UTF_8);
        }

        Path welcome = folder.resolve("hints").resolve("welcome.json");
        if (!Files.exists(welcome)) {
            Files.writeString(welcome, DEFAULT_WELCOME_JSON, StandardCharsets.UTF_8);
        }
    }

    private static void copyToWorldIfAbsent(MinecraftServer server) throws IOException {
        Path global = globalFolder();
        Path world = worldFolder(server);
        Files.createDirectories(world.resolve("hints"));

        Path globalCfg = global.resolve("global_config.json");
        Path worldCfg = world.resolve("world_config.json");
        if (Files.exists(globalCfg) && !Files.exists(worldCfg)) {
            Files.copy(globalCfg, worldCfg);
        }

        Path globalHints = global.resolve("hints");
        if (Files.isDirectory(globalHints)) {
            try (Stream<Path> list = Files.list(globalHints)) {
                for (Path file : list.filter(p -> p.toString().endsWith(".json")).toList()) {
                    Path target = world.resolve("hints").resolve(file.getFileName().toString());
                    if (!Files.exists(target)) {
                        Files.copy(file, target);
                    }
                }
            }
        }
    }

    private static void load(MinecraftServer server) throws IOException {
        TTSettings loaded = new TTSettings();

        Path globalCfg = globalFolder().resolve("global_config.json");
        Path worldCfg = worldFolder(server).resolve("world_config.json");

        if (Files.exists(globalCfg)) {
            loaded = GSON.fromJson(Files.readString(globalCfg, StandardCharsets.UTF_8), TTSettings.class);
        }
        if (Files.exists(worldCfg)) {
            loaded = GSON.fromJson(Files.readString(worldCfg, StandardCharsets.UTF_8), TTSettings.class);
        }

        Map<ResourceLocation, HintDefinition> map = new LinkedHashMap<>();
        loadHintFolder(globalFolder().resolve("hints"), map);
        loadHintFolder(worldFolder(server).resolve("hints"), map);

        settings = loaded;
        HINTS.clear();
        HINTS.putAll(map);
    }

    private static void loadHintFolder(Path folder, Map<ResourceLocation, HintDefinition> map) {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> list = Files.list(folder)) {
            list.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try {
                    JsonObject json = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
                    HintDefinition def = HintDefinition.fromJson(json);
                    map.put(def.id(), def);
                } catch (Exception e) {
                    TrackerTips.LOGGER.error("[TrackerTips] Failed to parse: {}", p, e);
                }
            });
        } catch (IOException e) {
            TrackerTips.LOGGER.error("[TrackerTips] Failed to read directory: {}", folder, e);
        }
    }

    // 【修改点】使用了 translate 翻译键，并加上了 title 字段
    private static final String DEFAULT_WELCOME_JSON = """
            {
              "id": "trackertips:welcome",
              "once": true,
              "priority": 100,
              "cooldown": 0,
              "duration": 240,
              "require": "all",
              "accent": "F2C14E",
              "sound": "minecraft:block.note_block.pling",
              "title": {
                "translate": "trackertips.hint.welcome.title",
                "color": "gold",
                "bold": true
              },
              "text": [
                {
                  "translate": "trackertips.hint.welcome.text",
                  "color": "gray"
                }
              ],
              "triggers": [
                { "type": "trackertips:game_time", "mode": "after", "time": 200 }
              ]
            }
            """;
}