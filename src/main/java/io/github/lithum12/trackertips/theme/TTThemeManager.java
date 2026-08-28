package io.github.lithum12.trackertips.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.lithum12.trackertips.TrackerTips;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/** Loads user-editable themes from config/trackertips/themes. */
public final class TTThemeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, TTTheme> THEMES = new LinkedHashMap<>();
    private static boolean initialized;

    private TTThemeManager() {}

    public static Path folder() {
        return FMLPaths.CONFIGDIR.get().resolve(TrackerTips.MODID).resolve("themes");
    }

    public static synchronized void ensureDefaults() {
        try {
            Files.createDirectories(folder());
            Path defaultFile = folder().resolve("default.json");
            if (!Files.exists(defaultFile)) {
                save(TTTheme.defaults("trackertips:default"), defaultFile);
            }
            load();
        } catch (IOException e) {
            TrackerTips.LOGGER.error("Failed to initialize TrackerTips themes", e);
        }
    }

    public static synchronized void load() {
        THEMES.clear();
        try {
            Files.createDirectories(folder());
            try (Stream<Path> stream = Files.list(folder())) {
                stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted()
                        .forEach(TTThemeManager::loadFile);
            }
        } catch (IOException e) {
            TrackerTips.LOGGER.error("Failed to read TrackerTips themes", e);
        }
        THEMES.putIfAbsent("trackertips:default", TTTheme.defaults("trackertips:default"));
        initialized = true;
    }

    private static void loadFile(Path path) {
        try {
            JsonObject json = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            TTTheme theme = TTTheme.fromJson(json);
            THEMES.put(theme.id(), theme);
        } catch (Exception e) {
            TrackerTips.LOGGER.error("Failed to parse TrackerTips theme: {}", path, e);
        }
    }

    public static synchronized Collection<TTTheme> all() {
        if (!initialized) ensureDefaults();
        return java.util.List.copyOf(THEMES.values());
    }

    public static synchronized TTTheme get(String id) {
        if (!initialized) ensureDefaults();
        return THEMES.getOrDefault(id, THEMES.get("trackertips:default"));
    }

    public static synchronized void save(TTTheme theme) {
        save(theme, folder().resolve(fileName(theme.id())));
        THEMES.put(theme.id(), theme);
    }

    private static void save(TTTheme theme, Path file) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(theme.toJson()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            TrackerTips.LOGGER.error("Failed to save TrackerTips theme: {}", file, e);
        }
    }

    public static String fileName(String id) {
        String name = id == null ? "theme" : id.replace(':', '_').replace('/', '_');
        return name + ".json";
    }

    public static synchronized void delete(TTTheme theme) {
        if ("trackertips:default".equals(theme.id())) return;
        try {
            Files.deleteIfExists(folder().resolve(fileName(theme.id())));
            THEMES.remove(theme.id());
        } catch (IOException e) {
            TrackerTips.LOGGER.error("Failed to delete TrackerTips theme: {}", theme.id(), e);
        }
    }

    public static synchronized void resetDefault() {
        TTTheme theme = TTTheme.defaults("trackertips:default");
        save(theme);
    }
}
