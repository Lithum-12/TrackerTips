package io.github.lithum12.trackertips.client.gui;

import com.google.gson.*;
import io.github.lithum12.trackertips.TrackerTips;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Opens one TrackerTips event JSON as a Cloth Config screen. */
public final class TTClothEventEditor {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TTClothEventEditor() { }

    public static Screen create(Screen parent, Path file) {
        JsonObject json;
        try {
            json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            TrackerTips.LOGGER.error("Failed to read TrackerTips event file: {}", file, e);
            return parent;
        }

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("trackertips.gui.event_editor", file.getFileName().toString()))
                .setSavingRunnable(() -> save(file, json));
        ConfigEntryBuilder entry = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("trackertips.gui.event.general"));
        addString(entry, general, "trackertips.gui.event.id", json, "id", "trackertips:example");
        general.addEntry(entry.startBooleanToggle(Component.translatable("trackertips.gui.event.once"), getBool(json, "once", true))
                .setDefaultValue(true).setSaveConsumer(v -> json.addProperty("once", v)).build());
        addInt(entry, general, "trackertips.gui.event.priority", json, "priority", 0);
        addInt(entry, general, "trackertips.gui.event.cooldown", json, "cooldown", 0);
        addInt(entry, general, "trackertips.gui.event.duration", json, "duration", 240);
        addString(entry, general, "trackertips.gui.event.require", json, "require", "any");
        addString(entry, general, "trackertips.gui.event.accent", json, "accent", "F2C14E");
        addString(entry, general, "trackertips.gui.event.sound", json, "sound", "");
        addString(entry, general, "trackertips.gui.event.icon", json, "icon", "");
        addInt(entry, general, "trackertips.gui.event.max_times", json, "max_times", 0);

        ConfigCategory content = builder.getOrCreateCategory(Component.translatable("trackertips.gui.event.content"));
        addJsonString(entry, content, "trackertips.gui.event.title", json, "title", "");
        addJsonString(entry, content, "trackertips.gui.event.text", json, "text", "[]");

        ConfigCategory triggers = builder.getOrCreateCategory(Component.translatable("trackertips.gui.event.triggers"));
        JsonArray array = json.has("triggers") && json.get("triggers").isJsonArray()
                ? json.getAsJsonArray("triggers") : new JsonArray();
        List<String> triggerValues = new ArrayList<>();
        for (JsonElement element : array) triggerValues.add(GSON.toJson(element));
        if (triggerValues.isEmpty()) triggerValues.add("{\"type\":\"trackertips:game_time\",\"mode\":\"after\",\"time\":200}");

        for (int i = 0; i < triggerValues.size(); i++) {
            final int index = i;
            triggers.addEntry(entry.startStrField(
                            Component.translatable("trackertips.gui.event.trigger", i + 1), triggerValues.get(i))
                    .setDefaultValue(triggerValues.get(i))
                    .setSaveConsumer(value -> setTrigger(json, index, value))
                    .build());
        }

        return builder.build();
    }

    private static void addString(ConfigEntryBuilder entry, ConfigCategory category, String label,
                                  JsonObject json, String key, String def) {
        String value = json.has(key) ? json.get(key).getAsString() : def;
        category.addEntry(entry.startStrField(Component.translatable(label), value)
                .setDefaultValue(def)
                .setSaveConsumer(v -> json.addProperty(key, v))
                .build());
    }

    private static void addInt(ConfigEntryBuilder entry, ConfigCategory category, String label,
                               JsonObject json, String key, int def) {
        int value = json.has(key) ? json.get(key).getAsInt() : def;
        category.addEntry(entry.startIntField(Component.translatable(label), value)
                .setDefaultValue(def)
                .setSaveConsumer(v -> json.addProperty(key, v))
                .build());
    }

    private static void addJsonString(ConfigEntryBuilder entry, ConfigCategory category, String label,
                                      JsonObject json, String key, String def) {
        String value = json.has(key) ? GSON.toJson(json.get(key)) : def;
        category.addEntry(entry.startStrField(Component.translatable(label), value)
                .setDefaultValue(def)
                .setSaveConsumer(v -> {
                    try { json.add(key, JsonParser.parseString(v)); }
                    catch (JsonParseException ignored) { }
                }).build());
    }

    private static void setTrigger(JsonObject json, int index, String value) {
        try {
            JsonElement parsed = JsonParser.parseString(value);
            JsonArray array = json.has("triggers") && json.get("triggers").isJsonArray()
                    ? json.getAsJsonArray("triggers") : new JsonArray();
            while (array.size() <= index) array.add(new JsonObject());
            array.set(index, parsed);
            json.add("triggers", array);
        } catch (JsonParseException ignored) { }
    }

    private static boolean getBool(JsonObject json, String key, boolean def) {
        return json.has(key) ? json.get(key).getAsBoolean() : def;
    }

    private static void save(Path file, JsonObject json) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
            TrackerTips.LOGGER.info("Saved TrackerTips event: {}", file);
        } catch (Exception e) {
            TrackerTips.LOGGER.error("Failed to save TrackerTips event: {}", file, e);
        }
    }
}
