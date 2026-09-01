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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Loads, merges, and exposes TrackerTips' server-authoritative settings and hint definitions.
 *
 * <p>Settings and hints both layer two sources: a "global" copy under the game's
 * {@code config/trackertips} folder (shared across every world/server) and a per-world copy
 * under {@code <world>/trackertips} (copied from the global folder the first time a world is
 * loaded, then edited independently). The per-world copy always wins when both exist; see
 * {@link #load(MinecraftServer)}.
 *
 * <p><b>Addon entry point:</b> mods that want to ship hint definitions without requiring the
 * server owner to hand-write JSON files can call {@link #registerHintProvider(Supplier)} from
 * their own {@code FMLCommonSetupEvent} handler. Registered providers run on every
 * {@link #reload(MinecraftServer)} (including the initial load), and their definitions are
 * merged in before the JSON files are loaded - so a server owner can always override or disable
 * an addon-provided hint by placing a same-id JSON file in their own hints folder.
 */
public class TTConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static TTSettings settings = new TTSettings();
    private static final Map<ResourceLocation, HintDefinition> HINTS = new LinkedHashMap<>();
    private static final List<Supplier<Collection<HintDefinition>>> HINT_PROVIDERS = new ArrayList<>();

    public static TTSettings settings() {
        return settings;
    }

    public static Collection<HintDefinition> hints() {
        return HINTS.values();
    }

    /** @return the loaded hint definition with the given id, or {@code null} if none is loaded. */
    public static HintDefinition hintById(ResourceLocation id) {
        return HINTS.get(id);
    }

    /**
     * Addon entry point: registers a supplier of programmatically-built {@link HintDefinition}s.
     * Call this once, from your own mod's {@code FMLCommonSetupEvent} handler (guaranteed to run
     * after every mod, including TrackerTips, has constructed).
     *
     * <p>The supplier is invoked on every {@link #reload(MinecraftServer)}/{@link #init}, so it
     * should be cheap and side-effect-free; build fresh {@link HintDefinition} instances (or
     * return a cached immutable list) rather than doing file I/O each call. Definitions it
     * returns are merged in before global/world JSON files are loaded, so a server owner can
     * always override or remove one by adding a same-id file under their own {@code hints/}
     * folder.
     *
     * @param provider supplies this mod's hint definitions; may return an empty collection.
     */
    public static void registerHintProvider(Supplier<Collection<HintDefinition>> provider) {
        HINT_PROVIDERS.add(provider);
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

    /** Loads settings + hints for the first time this server session; also seeds global defaults and per-world copies if missing. */
    public static void init(MinecraftServer server) {
        try {
            ensureGlobalDefaults();
            copyToWorldIfAbsent(server);
            load(server);
        } catch (IOException e) {
            TrackerTips.LOGGER.error("[TrackerTips] Failed to load configuration", e);
        }
    }

    /** Re-reads settings + hints from disk (and re-invokes every registered hint provider); used by {@code /trackertips reload}. */
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
        for (Supplier<Collection<HintDefinition>> provider : HINT_PROVIDERS) {
            try {
                for (HintDefinition def : provider.get()) {
                    map.put(def.id(), def);
                }
            } catch (Exception e) {
                TrackerTips.LOGGER.error("[TrackerTips] A registered hint provider threw an exception", e);
            }
        }
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

    // Uses a "translate" key for the title/text and includes a title field.
    private static final String DEFAULT_WELCOME_JSON = """
            {
              "id": "trackertips:welcome",
              "once": true,
              "priority": 100,
              "cooldown": 0,
              "duration": 240,
              "require": "all",
              "accent": "F2C14E",
              "theme": "trackertips:default",
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