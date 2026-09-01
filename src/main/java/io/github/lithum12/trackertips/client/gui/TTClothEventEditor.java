package io.github.lithum12.trackertips.client.gui;

import com.google.gson.*;
import io.github.lithum12.trackertips.TrackerTips;
import io.github.lithum12.trackertips.theme.TTTheme;
import io.github.lithum12.trackertips.theme.TTThemeManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cloth Config editor for a TrackerTips event.
 *
 * The JSON file remains the source of truth, but common Minecraft text
 * components and trigger properties are exposed as native Cloth Config fields.
 */
public final class TTClothEventEditor {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_TRIGGERS = 8;
    private static final int MAX_TEXT_COMPONENTS = 16;

    private enum TriggerType {
        GAME_TIME("trackertips:game_time", "trackertips.gui.trigger_type.game_time"),
        POTION_EFFECT("trackertips:potion_effect", "trackertips.gui.trigger_type.potion_effect"),
        HAS_ITEM("trackertips:has_item", "trackertips.gui.trigger_type.has_item"),
        ADVANCEMENT("trackertips:advancement", "trackertips.gui.trigger_type.advancement"),
        ITEM_OBTAINED("trackertips:item_obtained", "trackertips.gui.trigger_type.item_obtained"),
        IN_DIMENSION("trackertips:in_dimension", "trackertips.gui.trigger_type.in_dimension"),
        HEALTH_BELOW("trackertips:health_below", "trackertips.gui.trigger_type.health_below"),
        KILL_ENTITY("trackertips:kill_entity", "trackertips.gui.trigger_type.kill_entity"),
        MINE_BLOCK("trackertips:mine_block", "trackertips.gui.trigger_type.mine_block"),
        FIRST_JOIN("trackertips:first_join", "trackertips.gui.trigger_type.first_join");

        final String id;
        final String translationKey;

        TriggerType(String id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        static TriggerType fromId(String id) {
            for (TriggerType type : values()) {
                if (type.id.equals(id)) return type;
            }
            return GAME_TIME;
        }
    }

    private enum GameTimeMode {
        AFTER("after"), BEFORE("before"), RANGE("range");
        final String id;
        GameTimeMode(String id) { this.id = id; }
        static GameTimeMode from(String id) {
            for (GameTimeMode mode : values()) if (mode.id.equalsIgnoreCase(id)) return mode;
            return AFTER;
        }
    }

    private enum PotionMode {
        ADDED("added"), REMOVED("removed"), ACTIVE("active");
        final String id;
        PotionMode(String id) { this.id = id; }
        static PotionMode from(String id) {
            for (PotionMode mode : values()) if (mode.id.equalsIgnoreCase(id)) return mode;
            return ADDED;
        }
    }

    private enum AdvancementMode {
        DONE("done"), STATE("state");
        final String id;
        AdvancementMode(String id) { this.id = id; }
        static AdvancementMode from(String id) {
            for (AdvancementMode mode : values()) if (mode.id.equalsIgnoreCase(id)) return mode;
            return DONE;
        }
    }

    private enum ComponentKind {
        TEXT("text", "trackertips.gui.event.component_kind.text"),
        TRANSLATE("translate", "trackertips.gui.event.component_kind.translate");

        final String id;
        final String translationKey;

        ComponentKind(String id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        static ComponentKind from(JsonElement element) {
            if (element != null && element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("translate")) return TRANSLATE;
            }
            return TEXT;
        }
    }

    private TTClothEventEditor() {}

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
                .setTitle(Component.translatable("trackertips.gui.event_editor"))
                .setSavingRunnable(() -> save(file, json));
        ConfigEntryBuilder entry = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("trackertips.gui.event.general"));
        addString(entry, general, "trackertips.gui.event.id", json, "id", "trackertips:example");
        addBoolean(entry, general, "trackertips.gui.event.once", json, "once", true);
        addInt(entry, general, "trackertips.gui.event.priority", json, "priority", 0, -1000, 1000);
        addInt(entry, general, "trackertips.gui.event.cooldown", json, "cooldown", 0, 0, 72000);
        addInt(entry, general, "trackertips.gui.event.duration", json, "duration", 240, -1, 72000);
        addRequireDropdown(entry, general, json);
        addAccentColor(entry, general, json);
        addThemeDropdown(entry, general, json);
        addString(entry, general, "trackertips.gui.event.sound", json, "sound", "");
        addFloat(entry, general, "trackertips.gui.event.pitch", json, "pitch", 1.0F, 0.5F, 2.0F);
        addString(entry, general, "trackertips.gui.event.icon", json, "icon", "");
        addInt(entry, general, "trackertips.gui.event.max_times", json, "max_times", 0, 0, 100000);

        ConfigCategory content = builder.getOrCreateCategory(
                Component.translatable("trackertips.gui.event.content"));
        addTitleComponent(entry, content, json);
        addTextComponents(entry, content, json);

        ConfigCategory triggers = builder.getOrCreateCategory(
                Component.translatable("trackertips.gui.event.triggers"));
        addTriggers(entry, triggers, json);

        // Feature: nested/chained listeners. A hint may listen for one more trigger after it's
        // shown, then dismiss itself or hand off to another hint once that trigger matches.
        ConfigCategory chain = builder.getOrCreateCategory(
                Component.translatable("trackertips.gui.event.chain"));
        addChain(entry, chain, json);

        return builder.build();
    }

    private static void addBoolean(ConfigEntryBuilder entry, ConfigCategory category, String label,
                                   JsonObject json, String key, boolean def) {
        category.addEntry(entry.startBooleanToggle(Component.translatable(label), getBool(json, key, def))
                .setDefaultValue(def)
                .setSaveConsumer(v -> json.addProperty(key, v))
                .build());
    }

    private static void addString(ConfigEntryBuilder entry, ConfigCategory category, String label,
                                  JsonObject json, String key, String def) {
        category.addEntry(entry.startStrField(Component.translatable(label), getString(json, key, def))
                .setDefaultValue(def)
                .setSaveConsumer(v -> json.addProperty(key, v))
                .build());
    }

    private static void addInt(ConfigEntryBuilder entry, ConfigCategory category, String label,
                               JsonObject json, String key, int def, int min, int max) {
        category.addEntry(entry.startIntField(Component.translatable(label), getInt(json, key, def))
                .setDefaultValue(def).setMin(min).setMax(max)
                .setSaveConsumer(v -> json.addProperty(key, v))
                .build());
    }

    // Feature: exposes a per-event sound "pitch" in Cloth Config. The underlying playback call
    // (Entity#playSound) already accepted a pitch argument; only the JSON schema/editor lacked it.
    private static void addFloat(ConfigEntryBuilder entry, ConfigCategory category, String label,
                                 JsonObject json, String key, float def, float min, float max) {
        category.addEntry(entry.startFloatField(Component.translatable(label), getFloat(json, key, def))
                .setDefaultValue(def).setMin(min).setMax(max)
                .setSaveConsumer(v -> json.addProperty(key, v))
                .build());
    }

    private static void addRequireDropdown(ConfigEntryBuilder entry, ConfigCategory category, JsonObject json) {
        String value = getString(json, "require", "all");
        if (!value.equals("all") && !value.equals("any")) value = "all";

        category.addEntry(entry.startStringDropdownMenu(
                        Component.translatable("trackertips.gui.event.require"), value,
                        v -> Component.translatable("trackertips.gui.event.require." + v))
                .setSelections(List.of("all", "any"))
                .setDefaultValue("all")
                .setSaveConsumer(v -> json.addProperty("require", v))
                .build());
    }

    private static void addAccentColor(ConfigEntryBuilder entry, ConfigCategory category, JsonObject json) {
        int value = parseColorNameOrHex(getString(json, "accent", "F2C14E"), 0xF2C14E);
        category.addEntry(entry.startColorField(
                        Component.translatable("trackertips.gui.event.accent"), value)
                .setDefaultValue(0xF2C14E)
                .setSaveConsumer(v -> json.addProperty("accent", String.format(Locale.ROOT, "%06X", v & 0xFFFFFF)))
                .build());
    }

    private static void addThemeDropdown(ConfigEntryBuilder entry, ConfigCategory category, JsonObject json) {
        TTThemeManager.ensureDefaults();
        List<String> ids = new ArrayList<>();
        for (TTTheme theme : TTThemeManager.all()) ids.add(theme.id());
        if (ids.isEmpty()) ids.add("trackertips:default");

        String current = getString(json, "theme", ids.get(0));
        if (!ids.contains(current)) current = ids.get(0);

        category.addEntry(entry.startStringDropdownMenu(
                        Component.translatable("trackertips.gui.event.theme"), current,
                        id -> {
                            TTTheme theme = TTThemeManager.get(id);
                            return Component.literal(theme == null ? id : theme.name());
                        })
                .setSelections(ids)
                .setDefaultValue(ids.get(0))
                .setSaveConsumer(v -> json.addProperty("theme", v))
                .build());
    }

    /* ---------------------------------------------------------------------
     * Minecraft text component editor
     * --------------------------------------------------------------------- */

    private static void addTitleComponent(ConfigEntryBuilder entry, ConfigCategory category, JsonObject json) {
        JsonElement titleElement = json.get("title");
        ComponentKind titleKind = ComponentKind.from(titleElement);
        JsonObject titleObject = normalizeComponent(titleElement, titleKind);
        var titleSub = entry.startSubCategory(
                Component.translatable("trackertips.gui.event.title_component"))
                .setExpanded(true);

        titleSub.add(entry.startEnumSelector(
                        Component.translatable("trackertips.gui.event.component_kind"),
                        ComponentKind.class, titleKind)
                .setEnumNameProvider(value -> Component.translatable(((ComponentKind) value).translationKey))
                .setDefaultValue(titleKind)
                .setSaveConsumer(value -> {
                    if (value == ComponentKind.TRANSLATE) {
                        String v = getString(titleObject, "translate", "");
                        titleObject.remove("text");
                        titleObject.addProperty("translate", v);
                    } else {
                        String v = getString(titleObject, "text", "");
                        titleObject.remove("translate");
                        titleObject.addProperty("text", v);
                    }
                    json.add("title", titleObject);
                })
                .build());
        titleSub.add(entry.startStrField(
                        Component.translatable("trackertips.gui.event.component_value"),
                        componentValue(titleObject))
                .setDefaultValue("")
                .setSaveConsumer(value -> {
                    if (titleObject.has("translate")) titleObject.addProperty("translate", value);
                    else titleObject.addProperty("text", value);
                    json.add("title", titleObject);
                })
                .build());
        titleSub.add(entry.startColorField(
                        Component.translatable("trackertips.gui.event.component_color"),
                        parseColorNameOrHex(getString(titleObject, "color", "FFFFFF"), 0xFFFFFF))
                .setDefaultValue(0xFFFFFF)
                .setSaveConsumer(value -> {
                    if ((value & 0xFFFFFF) == 0xFFFFFF) titleObject.remove("color");
                    else titleObject.addProperty("color", String.format(Locale.ROOT, "%06X", value & 0xFFFFFF));
                    json.add("title", titleObject);
                })
                .build());
        titleSub.add(entry.startBooleanToggle(
                        Component.translatable("trackertips.gui.event.component_bold"),
                        getBool(titleObject, "bold", false))
                .setDefaultValue(false)
                .setSaveConsumer(value -> {
                    setBoolean(titleObject, "bold", value);
                    json.add("title", titleObject);
                })
                .build());
        titleSub.add(entry.startBooleanToggle(
                        Component.translatable("trackertips.gui.event.component_italic"),
                        getBool(titleObject, "italic", false))
                .setDefaultValue(false)
                .setSaveConsumer(value -> {
                    setBoolean(titleObject, "italic", value);
                    json.add("title", titleObject);
                })
                .build());
        category.addEntry(titleSub.build());
    }

    private static void addTextComponents(ConfigEntryBuilder entry, ConfigCategory category, JsonObject json) {
        JsonArray source = json.has("text") && json.get("text").isJsonArray()
                ? json.getAsJsonArray("text") : new JsonArray();
        int count = Math.min(Math.max(source.size(), 1), MAX_TEXT_COMPONENTS);

        JsonArray working = new JsonArray();
        for (int index = 0; index < count; index++) {
            JsonElement element = index < source.size() ? source.get(index).deepCopy() : new JsonPrimitive("");
            JsonObject component = normalizeComponent(element, ComponentKind.from(element));
            working.add(component);

            var sub = entry.startSubCategory(Component.translatable(
                    "trackertips.gui.event.text_component", index + 1)).setExpanded(index == 0);

            sub.add(entry.startEnumSelector(
                            Component.translatable("trackertips.gui.event.component_kind"),
                            ComponentKind.class,
                            ComponentKind.from(element))
                    .setEnumNameProvider(value -> Component.translatable(((ComponentKind) value).translationKey))
                    .setDefaultValue(ComponentKind.from(element))
                    .setSaveConsumer(value -> {
                        if (value == ComponentKind.TRANSLATE) {
                            String v = getString(component, "translate", "");
                            component.remove("text");
                            component.addProperty("translate", v);
                        } else {
                            String v = getString(component, "text", "");
                            component.remove("translate");
                            component.addProperty("text", v);
                        }
                    })
                    .build());

            sub.add(entry.startStrField(
                            Component.translatable("trackertips.gui.event.component_value"),
                            componentValue(component))
                    .setDefaultValue("")
                    .setSaveConsumer(value -> {
                        if (component.has("translate")) component.addProperty("translate", value);
                        else component.addProperty("text", value);
                    })
                    .build());

            sub.add(entry.startColorField(
                            Component.translatable("trackertips.gui.event.component_color"),
                            parseColorNameOrHex(getString(component, "color", "FFFFFF"), 0xFFFFFF))
                    .setDefaultValue(0xFFFFFF)
                    .setSaveConsumer(value -> {
                        if ((value & 0xFFFFFF) == 0xFFFFFF) component.remove("color");
                        else component.addProperty("color", String.format(Locale.ROOT, "%06X", value & 0xFFFFFF));
                    })
                    .build());

            sub.add(entry.startBooleanToggle(
                            Component.translatable("trackertips.gui.event.component_bold"),
                            getBool(component, "bold", false))
                    .setDefaultValue(false)
                    .setSaveConsumer(value -> setBoolean(component, "bold", value))
                    .build());

            sub.add(entry.startBooleanToggle(
                            Component.translatable("trackertips.gui.event.component_italic"),
                            getBool(component, "italic", false))
                    .setDefaultValue(false)
                    .setSaveConsumer(value -> setBoolean(component, "italic", value))
                    .build());

            category.addEntry(sub.build());
        }
        json.add("text", working);
    }

    private static JsonObject getOrCreateComponent(JsonObject root, String key, ComponentKind kind) {
        return normalizeComponent(root.has(key) ? root.get(key) : new JsonPrimitive(""), kind);
    }

    private static JsonObject normalizeComponent(JsonElement source, ComponentKind kind) {
        if (source != null && source.isJsonObject()) return source.getAsJsonObject().deepCopy();
        JsonObject object = new JsonObject();
        String value = source != null && source.isJsonPrimitive() ? source.getAsString() : "";
        object.addProperty(kind == ComponentKind.TRANSLATE ? "translate" : "text", value);
        return object;
    }

    private static String componentValue(JsonElement element) {
        if (element == null || element.isJsonNull()) return "";
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("translate")) return getString(object, "translate", "");
            return getString(object, "text", "");
        }
        return element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private static String getComponentString(JsonElement element, String key, String def) {
        return element != null && element.isJsonObject()
                ? getString(element.getAsJsonObject(), key, def)
                : def;
    }

    private static boolean getComponentBool(JsonElement element, String key, boolean def) {
        return element != null && element.isJsonObject()
                ? getBool(element.getAsJsonObject(), key, def) : def;
    }

    private static void updateComponentColor(JsonObject root, String key, int value) {
        JsonObject object = getOrCreateComponent(root, key, ComponentKind.from(root.get(key)));
        if ((value & 0xFFFFFF) == 0xFFFFFF) object.remove("color");
        else object.addProperty("color", String.format(Locale.ROOT, "%06X", value & 0xFFFFFF));
        root.add(key, object);
    }

    private static void updateComponentBoolean(JsonObject root, String key, String property, boolean value) {
        JsonObject object = getOrCreateComponent(root, key, ComponentKind.from(root.get(key)));
        setBoolean(object, property, value);
        root.add(key, object);
    }

    private static void setBoolean(JsonObject object, String key, boolean value) {
        if (value) object.addProperty(key, true);
        else object.remove(key);
    }

    /* ---------------------------------------------------------------------
     * Trigger editor
     * --------------------------------------------------------------------- */

    private static void addTriggers(ConfigEntryBuilder entry, ConfigCategory category, JsonObject json) {
        JsonArray source = json.has("triggers") && json.get("triggers").isJsonArray()
                ? json.getAsJsonArray("triggers") : new JsonArray();
        JsonArray working = new JsonArray();

        int count = Math.min(Math.max(source.size(), 1), MAX_TRIGGERS);
        for (int index = 0; index < count; index++) {
            JsonObject trigger = index < source.size() && source.get(index).isJsonObject()
                    ? source.get(index).getAsJsonObject().deepCopy()
                    : defaultTrigger(TriggerType.GAME_TIME);
            working.add(trigger);
            addTrigger(entry, category, trigger, index);
        }
        json.add("triggers", working);
    }

    private static JsonObject defaultTrigger(TriggerType type) {
        JsonObject trigger = new JsonObject();
        trigger.addProperty("type", type.id);
        addDefaultProperties(trigger, type);
        return trigger;
    }

    private static void addDefaultProperties(JsonObject trigger, TriggerType type) {
        switch (type) {
            case GAME_TIME -> {
                trigger.addProperty("mode", "after");
                trigger.addProperty("time", 200);
            }
            case POTION_EFFECT -> {
                trigger.addProperty("mode", "added");
                trigger.addProperty("effect", "minecraft:speed");
                trigger.addProperty("amplifier_min", 0);
            }
            case HAS_ITEM, ITEM_OBTAINED -> {
                trigger.addProperty("item", "minecraft:stone");
                trigger.addProperty("count", 1);
            }
            case ADVANCEMENT -> {
                trigger.addProperty("mode", "done");
                trigger.addProperty("id", "minecraft:story/root");
            }
            case IN_DIMENSION -> trigger.addProperty("dimension", "minecraft:the_nether");
            case HEALTH_BELOW -> trigger.addProperty("health", 6.0);
            case KILL_ENTITY -> trigger.addProperty("entity", "minecraft:zombie");
            case MINE_BLOCK -> trigger.addProperty("block", "minecraft:stone");
            case FIRST_JOIN -> { /* no extra fields */ }
        }
    }

    private static void addTrigger(ConfigEntryBuilder entry, ConfigCategory category,
                                   JsonObject trigger, int index) {
        TriggerType type = TriggerType.fromId(getString(trigger, "type", TriggerType.GAME_TIME.id));

        // Bug fix: all of a trigger's fields (the type dropdown AND the type-specific
        // fields below it) share this one JsonObject. Cloth Config invokes every field's
        // save consumer when the screen is saved, in declaration order, regardless of
        // whether that field's value actually changed. Previously, switching the trigger
        // type wiped the object and wrote fresh defaults for the new type (see
        // replaceTriggerType), but the type-specific fields further down were still bound
        // to the OLD type (the UI for them isn't rebuilt until the editor is reopened) and
        // unconditionally wrote their stale key/value pairs straight back afterwards. That
        // silently corrupted the saved JSON with leftover properties from the previous type
        // every time the type was changed, which is what made the trigger editor look like
        // it "stopped accepting changes" after the first save. activeType tracks whichever
        // type is actually selected right now so the stale fields below can refuse to write
        // once they no longer match it.
        TriggerType[] activeType = { type };

        var sub = entry.startSubCategory(Component.translatable(
                "trackertips.gui.event.trigger", index + 1)).setExpanded(index == 0);

        sub.add(entry.startEnumSelector(
                        Component.translatable("trackertips.gui.event.trigger_type"),
                        TriggerType.class,
                        type)
                .setEnumNameProvider(value -> Component.translatable(((TriggerType) value).translationKey))
                .setDefaultValue(type)
                .setSaveConsumer(value -> {
                    activeType[0] = value;
                    replaceTriggerType(trigger, value);
                })
                .build());

        // Bug fix: every setSaveConsumer below is guarded with "activeType[0] == type"
        // (type here is the type this switch-branch was built for). If the user switches
        // the trigger type via the dropdown above, activeType[0] changes and these stale,
        // not-yet-rebuilt fields stop writing into `trigger`, instead of silently
        // reintroducing properties that belong to the old, no-longer-selected type.
        // The UI for the newly selected type's fields still only appears after the editor
        // is reopened (Cloth Config categories can't be rebuilt in place), but at least the
        // saved JSON is no longer corrupted by the switch.
        switch (type) {
            case GAME_TIME -> {
                sub.add(entry.startEnumSelector(
                                Component.translatable("trackertips.gui.event.game_time_mode"),
                                GameTimeMode.class,
                                GameTimeMode.from(getString(trigger, "mode", "after")))
                        .setEnumNameProvider(value -> Component.translatable("trackertips.gui.event.mode." + ((GameTimeMode) value).id))
                        .setDefaultValue(GameTimeMode.AFTER)
                        .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("mode", v.id); })
                        .build());
                sub.add(entry.startLongField(
                                Component.translatable("trackertips.gui.event.time"),
                                getLong(trigger, "time", 200))
                        .setMin(0)
                        .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("time", v); })
                        .build());
                sub.add(entry.startLongField(
                                Component.translatable("trackertips.gui.event.end_time"),
                                getLong(trigger, "end_time", 400))
                        .setMin(0)
                        .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("end_time", v); })
                        .build());
            }
            case POTION_EFFECT -> {
                sub.add(entry.startEnumSelector(
                                Component.translatable("trackertips.gui.event.potion_mode"),
                                PotionMode.class,
                                PotionMode.from(getString(trigger, "mode", "added")))
                        .setEnumNameProvider(value -> Component.translatable("trackertips.gui.event.potion_mode." + ((PotionMode) value).id))
                        .setDefaultValue(PotionMode.ADDED)
                        .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("mode", v.id); })
                        .build());
                sub.add(entry.startStrField(Component.translatable("trackertips.gui.event.effect"),
                                getString(trigger, "effect", "minecraft:speed"))
                        .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("effect", v); }).build());
                sub.add(entry.startIntField(Component.translatable("trackertips.gui.event.amplifier_min"),
                                getInt(trigger, "amplifier_min", 0))
                        .setMin(0).setMax(255)
                        .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("amplifier_min", v); }).build());
            }
            case HAS_ITEM, ITEM_OBTAINED -> {
                sub.add(entry.startStrField(Component.translatable("trackertips.gui.event.item"),
                                getString(trigger, "item", "minecraft:stone"))
                        .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("item", v); }).build());
                sub.add(entry.startIntField(Component.translatable("trackertips.gui.event.count"),
                                getInt(trigger, "count", 1))
                        .setMin(1).setMax(99999)
                        .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("count", v); }).build());
            }
            case ADVANCEMENT -> {
                sub.add(entry.startEnumSelector(
                                Component.translatable("trackertips.gui.event.advancement_mode"),
                                AdvancementMode.class,
                                AdvancementMode.from(getString(trigger, "mode", "done")))
                        .setEnumNameProvider(value -> Component.translatable("trackertips.gui.event.advancement_mode." + ((AdvancementMode) value).id))
                        .setDefaultValue(AdvancementMode.DONE)
                        .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("mode", v.id); })
                        .build());
                sub.add(entry.startStrField(Component.translatable("trackertips.gui.event.advancement_id"),
                                getString(trigger, "id", "minecraft:story/root"))
                        .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("id", v); }).build());
            }
            case IN_DIMENSION -> sub.add(entry.startStrField(
                            Component.translatable("trackertips.gui.event.dimension"),
                            getString(trigger, "dimension", "minecraft:the_nether"))
                    .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("dimension", v); }).build());
            case HEALTH_BELOW -> sub.add(entry.startFloatField(
                            Component.translatable("trackertips.gui.event.health"),
                            getFloat(trigger, "health", 6.0F))
                    .setMin(0).setMax(1000)
                    .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("health", v); }).build());
            case KILL_ENTITY -> sub.add(entry.startStrField(
                            Component.translatable("trackertips.gui.event.entity"),
                            getString(trigger, "entity", "minecraft:zombie"))
                    .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("entity", v); }).build());
            case MINE_BLOCK -> sub.add(entry.startStrField(
                            Component.translatable("trackertips.gui.event.block"),
                            getString(trigger, "block", "minecraft:stone"))
                    .setSaveConsumer(v -> { if (activeType[0] == type) trigger.addProperty("block", v); }).build());
            case FIRST_JOIN -> { /* no extra fields */ }
        }

        category.addEntry(sub.build());
    }

    /**
     * Feature: nested/chained listeners. Builds the UI for a hint's optional {@code "chain"}:
     * an enable toggle, an action (dismiss/next) dropdown, a "next hint id" field, and - reusing
     * the exact same per-type field pattern as {@link #addTrigger} - a single trigger to listen
     * for once this hint has been shown. See {@link io.github.lithum12.trackertips.trigger.HintChain}
     * for the runtime semantics.
     */
    private static void addChain(ConfigEntryBuilder entry, ConfigCategory category, JsonObject json) {
        boolean chainEnabled = json.has("chain") && json.get("chain").isJsonObject();
        JsonObject chainJson = chainEnabled ? json.getAsJsonObject("chain").deepCopy() : new JsonObject();

        JsonObject triggerJson = chainJson.has("trigger") && chainJson.get("trigger").isJsonObject()
                ? chainJson.getAsJsonObject("trigger").deepCopy()
                : defaultTrigger(TriggerType.MINE_BLOCK);
        chainJson.add("trigger", triggerJson);
        String initialAction = getString(chainJson, "action", "dismiss");
        chainJson.addProperty("action", initialAction);
        String initialNext = getString(chainJson, "next", "");
        chainJson.addProperty("next", initialNext);

        // Enabling writes "chain" back onto the event JSON at save time; disabling removes the
        // key entirely. The fields below stay populated either way so re-enabling later in the
        // same editing session doesn't lose whatever was configured.
        category.addEntry(entry.startBooleanToggle(
                        Component.translatable("trackertips.gui.event.chain_enabled"), chainEnabled)
                .setDefaultValue(false)
                .setSaveConsumer(v -> {
                    if (v) json.add("chain", chainJson);
                    else json.remove("chain");
                })
                .build());

        category.addEntry(entry.startStringDropdownMenu(
                        Component.translatable("trackertips.gui.event.chain_action"), initialAction,
                        v -> Component.translatable("trackertips.gui.event.chain_action." + v))
                .setSelections(List.of("dismiss", "next"))
                .setDefaultValue("dismiss")
                .setSaveConsumer(v -> chainJson.addProperty("action", v))
                .build());

        category.addEntry(entry.startStrField(
                        Component.translatable("trackertips.gui.event.chain_next"), initialNext)
                .setSaveConsumer(v -> chainJson.addProperty("next", v))
                .build());

        TriggerType type = TriggerType.fromId(getString(triggerJson, "type", TriggerType.MINE_BLOCK.id));
        TriggerType[] activeType = { type };

        var sub = entry.startSubCategory(Component.translatable("trackertips.gui.event.chain_trigger"))
                .setExpanded(chainEnabled);

        sub.add(entry.startEnumSelector(
                        Component.translatable("trackertips.gui.event.trigger_type"),
                        TriggerType.class,
                        type)
                .setEnumNameProvider(value -> Component.translatable(((TriggerType) value).translationKey))
                .setDefaultValue(type)
                .setSaveConsumer(value -> {
                    activeType[0] = value;
                    replaceTriggerType(triggerJson, value);
                })
                .build());

        // See addTrigger() for why every field below is guarded on activeType[0] == type.
        switch (type) {
            case GAME_TIME -> {
                sub.add(entry.startEnumSelector(
                                Component.translatable("trackertips.gui.event.game_time_mode"),
                                GameTimeMode.class,
                                GameTimeMode.from(getString(triggerJson, "mode", "after")))
                        .setEnumNameProvider(value -> Component.translatable("trackertips.gui.event.mode." + ((GameTimeMode) value).id))
                        .setDefaultValue(GameTimeMode.AFTER)
                        .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("mode", v.id); })
                        .build());
                sub.add(entry.startLongField(
                                Component.translatable("trackertips.gui.event.time"),
                                getLong(triggerJson, "time", 200))
                        .setMin(0)
                        .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("time", v); })
                        .build());
                sub.add(entry.startLongField(
                                Component.translatable("trackertips.gui.event.end_time"),
                                getLong(triggerJson, "end_time", 400))
                        .setMin(0)
                        .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("end_time", v); })
                        .build());
            }
            case POTION_EFFECT -> {
                sub.add(entry.startEnumSelector(
                                Component.translatable("trackertips.gui.event.potion_mode"),
                                PotionMode.class,
                                PotionMode.from(getString(triggerJson, "mode", "added")))
                        .setEnumNameProvider(value -> Component.translatable("trackertips.gui.event.potion_mode." + ((PotionMode) value).id))
                        .setDefaultValue(PotionMode.ADDED)
                        .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("mode", v.id); })
                        .build());
                sub.add(entry.startStrField(Component.translatable("trackertips.gui.event.effect"),
                                getString(triggerJson, "effect", "minecraft:speed"))
                        .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("effect", v); }).build());
                sub.add(entry.startIntField(Component.translatable("trackertips.gui.event.amplifier_min"),
                                getInt(triggerJson, "amplifier_min", 0))
                        .setMin(0).setMax(255)
                        .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("amplifier_min", v); }).build());
            }
            case HAS_ITEM, ITEM_OBTAINED -> {
                sub.add(entry.startStrField(Component.translatable("trackertips.gui.event.item"),
                                getString(triggerJson, "item", "minecraft:stone"))
                        .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("item", v); }).build());
                sub.add(entry.startIntField(Component.translatable("trackertips.gui.event.count"),
                                getInt(triggerJson, "count", 1))
                        .setMin(1).setMax(99999)
                        .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("count", v); }).build());
            }
            case ADVANCEMENT -> {
                sub.add(entry.startEnumSelector(
                                Component.translatable("trackertips.gui.event.advancement_mode"),
                                AdvancementMode.class,
                                AdvancementMode.from(getString(triggerJson, "mode", "done")))
                        .setEnumNameProvider(value -> Component.translatable("trackertips.gui.event.advancement_mode." + ((AdvancementMode) value).id))
                        .setDefaultValue(AdvancementMode.DONE)
                        .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("mode", v.id); })
                        .build());
                sub.add(entry.startStrField(Component.translatable("trackertips.gui.event.advancement_id"),
                                getString(triggerJson, "id", "minecraft:story/root"))
                        .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("id", v); }).build());
            }
            case IN_DIMENSION -> sub.add(entry.startStrField(
                            Component.translatable("trackertips.gui.event.dimension"),
                            getString(triggerJson, "dimension", "minecraft:the_nether"))
                    .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("dimension", v); }).build());
            case HEALTH_BELOW -> sub.add(entry.startFloatField(
                            Component.translatable("trackertips.gui.event.health"),
                            getFloat(triggerJson, "health", 6.0F))
                    .setMin(0).setMax(1000)
                    .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("health", v); }).build());
            case KILL_ENTITY -> sub.add(entry.startStrField(
                            Component.translatable("trackertips.gui.event.entity"),
                            getString(triggerJson, "entity", "minecraft:zombie"))
                    .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("entity", v); }).build());
            case MINE_BLOCK -> sub.add(entry.startStrField(
                            Component.translatable("trackertips.gui.event.block"),
                            getString(triggerJson, "block", "minecraft:stone"))
                    .setSaveConsumer(v -> { if (activeType[0] == type) triggerJson.addProperty("block", v); }).build());
            case FIRST_JOIN -> { /* no extra fields */ }
        }

        category.addEntry(sub.build());
    }

    private static void replaceTriggerType(JsonObject trigger, TriggerType type) {
        List<String> known = List.of(
                "mode", "time", "end_time", "effect", "amplifier_min", "item", "count",
                "id", "dimension", "health", "entity", "block"
        );
        for (String key : known) trigger.remove(key);
        trigger.addProperty("type", type.id);
        addDefaultProperties(trigger, type);
    }

    /* ---------------------------------------------------------------------
     * Helpers
     * --------------------------------------------------------------------- */

    private static String getString(JsonObject json, String key, String def) {
        try {
            return json.has(key) ? json.get(key).getAsString() : def;
        } catch (Exception ignored) {
            return def;
        }
    }

    private static int getInt(JsonObject json, String key, int def) {
        try {
            return json.has(key) ? json.get(key).getAsInt() : def;
        } catch (Exception ignored) {
            return def;
        }
    }

    private static long getLong(JsonObject json, String key, long def) {
        try {
            return json.has(key) ? json.get(key).getAsLong() : def;
        } catch (Exception ignored) {
            return def;
        }
    }

    private static float getFloat(JsonObject json, String key, float def) {
        try {
            return json.has(key) ? json.get(key).getAsFloat() : def;
        } catch (Exception ignored) {
            return def;
        }
    }

    private static boolean getBool(JsonObject json, String key, boolean def) {
        try {
            return json.has(key) ? json.get(key).getAsBoolean() : def;
        } catch (Exception ignored) {
            return def;
        }
    }

    private static int parseColorNameOrHex(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        String clean = value.trim();
        ChatFormatting formatting = ChatFormatting.getByName(clean);
        if (formatting != null && formatting.getColor() != null) return formatting.getColor();
        return parseColor(clean, fallback);
    }

    private static int parseColor(String value, int fallback) {
        try {
            String clean = value.trim();
            if (clean.startsWith("#")) clean = clean.substring(1);
            if (clean.length() != 6) throw new NumberFormatException();
            return Integer.parseInt(clean, 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return fallback;
        }
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
