package io.github.lithum12.trackertips.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.lithum12.trackertips.trigger.HintChain;
import io.github.lithum12.trackertips.trigger.IHintTrigger;
import io.github.lithum12.trackertips.trigger.IEventHintTrigger;
import io.github.lithum12.trackertips.trigger.TriggerEvent;
import io.github.lithum12.trackertips.trigger.Triggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The parsed, immutable form of one event/hint JSON file (e.g. {@code hints/welcome.json}).
 *
 * <p>A definition bundles the display content (title/text/icon/theme/sound), the scheduling
 * rules (once/cooldown/duration/max_times/priority), and the {@link IHintTrigger} list that must
 * be satisfied for it to show, plus an optional {@link HintChain} describing a follow-up
 * condition to listen for once it <em>has</em> been shown (see {@link #chain()}).
 *
 * <p>Instances are effectively read-only value objects rebuilt from disk on every
 * {@code TTConfigManager#reload}; nothing here mutates after construction. Addon mods that want
 * to contribute definitions programmatically (rather than as JSON files) can register a
 * {@code TTConfigManager#registerHintProvider} supplying {@link HintDefinition} instances built
 * directly via the constructor, or by building the equivalent {@link JsonObject} and calling
 * {@link #fromJson(JsonObject)}.
 */
public class HintDefinition {

    private final ResourceLocation id;
    private final boolean once;
    private final int priority;
    private final int cooldown;
    private final int duration;
    private final boolean requireAll;
    private final int accentColor;
    private final String sound;
    private final float pitch;
    private final JsonElement text;
    private final List<IHintTrigger> triggers;
    private final JsonElement title;
    private final String icon;
    private final String theme;
    private final int maxTimes;
    private final HintChain chain;

    public HintDefinition(ResourceLocation id, boolean once, int priority, int cooldown, int duration,
                          boolean requireAll, int accentColor, String sound, float pitch, JsonElement title, JsonElement text, String icon,
                          List<IHintTrigger> triggers, int maxTimes, String theme, HintChain chain) {
        this.id = id;
        this.once = once;
        this.priority = priority;
        this.cooldown = cooldown;
        this.duration = duration;
        this.requireAll = requireAll;
        this.accentColor = accentColor;
        this.sound = sound;
        this.pitch = pitch;
        this.text = text;
        this.title = title;
        this.icon = icon;
        this.triggers = triggers;
        this.maxTimes = maxTimes;
        this.theme = theme == null || theme.isBlank() ? "trackertips:default" : theme;
        this.chain = chain;
    }

    /**
     * Parses one hint definition from its JSON form. See the class-level docs and
     * {@link HintChain} for the full field reference; unrecognized/missing fields fall back to
     * sensible defaults rather than throwing, since these files are hand-edited.
     *
     * @throws com.google.gson.JsonSyntaxException  if a required field (e.g. {@code id}) is missing or malformed.
     * @throws IllegalArgumentException              if a trigger (top-level or in {@link HintChain}) references an unregistered type.
     */
    public static HintDefinition fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "id"));
        boolean once = GsonHelper.getAsBoolean(json, "once", true);
        int priority = GsonHelper.getAsInt(json, "priority", 0);
        int cooldown = GsonHelper.getAsInt(json, "cooldown", 0);
        int duration = GsonHelper.getAsInt(json, "duration", 240); // Defaults to a 12-second duration; use "duration": -1 for a persistent hint
        boolean requireAll = GsonHelper.getAsString(json, "require", "any").equalsIgnoreCase("all");
        int accent = (int) Long.parseLong(GsonHelper.getAsString(json, "accent", "F2C14E"), 16);
        String sound = GsonHelper.getAsString(json, "sound", "");
        // Feature: "pitch" lets an event's sound play higher/lower than normal (vanilla's
        // Entity#playSound already accepts a pitch; this just exposes it in the event JSON).
        // Vanilla's own sound-playing pitch range is roughly 0.5-2.0, so that's clamped here too.
        float pitch = Mth.clamp(GsonHelper.getAsFloat(json, "pitch", 1.0F), 0.5F, 2.0F);
        JsonElement title = json.has("title") ? json.get("title") : null;
        JsonElement text = json.get("text");
        String icon = GsonHelper.getAsString(json, "icon", "");
        String theme = GsonHelper.getAsString(json, "theme", "trackertips:default");

        // Parses the maximum trigger count; 0 means unlimited.
        int maxTimes = GsonHelper.getAsInt(json, "max_times", 0);
        // Compatibility: "once": true without max_times is treated as a maximum of one trigger
        if (once && maxTimes <= 0) {
            maxTimes = 1;
        }

        List<IHintTrigger> triggers = new ArrayList<>();
        JsonArray array = json.has("triggers") ? json.getAsJsonArray("triggers") : new JsonArray();
        for (JsonElement element : array) {
            JsonObject triggerJson = element.getAsJsonObject();
            ResourceLocation type = new ResourceLocation(GsonHelper.getAsString(triggerJson, "type"));
            triggers.add(Triggers.create(type, triggerJson));
        }

        HintChain chain = parseChain(json);

        return new HintDefinition(id, once, priority, cooldown, duration, requireAll, accent, sound, pitch,
                title, text, icon, triggers, maxTimes, theme, chain);
    }

    /**
     * Feature: nested/chained listeners. Parses the optional {@code "chain"} object -
     * {@code {"trigger": {...}, "action": "dismiss"|"next", "next": "modid:hint_id"}} - into a
     * {@link HintChain}, or returns {@code null} if the definition has none.
     */
    private static HintChain parseChain(JsonObject json) {
        if (!json.has("chain") || !json.get("chain").isJsonObject()) return null;
        JsonObject chainJson = json.getAsJsonObject("chain");
        if (!chainJson.has("trigger") || !chainJson.get("trigger").isJsonObject()) return null;

        JsonObject triggerJson = chainJson.getAsJsonObject("trigger");
        ResourceLocation type = new ResourceLocation(GsonHelper.getAsString(triggerJson, "type"));
        IHintTrigger trigger = Triggers.create(type, triggerJson);

        HintChain.Action action = HintChain.Action.fromId(GsonHelper.getAsString(chainJson, "action", "dismiss"));
        ResourceLocation next = chainJson.has("next") && !GsonHelper.getAsString(chainJson, "next", "").isBlank()
                ? new ResourceLocation(GsonHelper.getAsString(chainJson, "next", ""))
                : null;

        return new HintChain(trigger, action, next);
    }

    /** Regular polling: event-driven triggers are skipped so an event condition isn't mistaken for a persistent state. */
    public boolean matches(ServerPlayer player) {
        if (triggers.isEmpty()) return false;

        boolean hasStateTrigger = false;
        boolean hasEventTrigger = false;
        boolean result = requireAll;
        for (IHintTrigger trigger : triggers) {
            if (trigger instanceof IEventHintTrigger eventTrigger && eventTrigger.isEventDriven()) {
                hasEventTrigger = true;
                continue;
            }
            hasStateTrigger = true;
            boolean matched = trigger.test(player);
            if (requireAll) result &= matched;
            else result |= matched;
        }

        // In "all" mode, if any event-driven condition is present, the whole check must wait for that Forge event to fire.
        if (requireAll && hasEventTrigger) return false;
        // A purely event-driven trigger obviously can't be satisfied by polling either.
        if (!hasStateTrigger) return false;
        return result;
    }

    /** Native Forge event trigger: event-driven conditions are supplied by `event`; regular state conditions are still checked live. */
    public boolean matchesEvent(ServerPlayer player, TriggerEvent event) {
        if (triggers.isEmpty()) return false;

        if (requireAll) {
            for (IHintTrigger trigger : triggers) {
                boolean matched;
                if (trigger instanceof IEventHintTrigger eventTrigger && eventTrigger.isEventDriven()) {
                    matched = eventTrigger.matchesEvent(player, event);
                } else {
                    matched = trigger.test(player);
                }
                if (!matched) return false;
            }
            return true;
        }

        // In "any" mode, only the event-driven trigger that actually matches the current event may fire it;
        // otherwise picking up one item could incorrectly trigger a completely unrelated game_time hint.
        for (IHintTrigger trigger : triggers) {
            if (trigger instanceof IEventHintTrigger eventTrigger
                    && eventTrigger.matchesEvent(player, event)) {
                return true;
            }
        }
        return false;
    }

    /** Current state for persistent state-based hints. Event-only triggers return false. */
    public boolean currentState(ServerPlayer player) {
        if (triggers.isEmpty()) return false;
        boolean hasState = false;
        boolean result = requireAll;
        for (IHintTrigger trigger : triggers) {
            if (trigger instanceof IEventHintTrigger eventTrigger && eventTrigger.isEventDriven()) {
                continue;
            }
            hasState = true;
            boolean matched = trigger.currentState(player);
            if (requireAll) result &= matched;
            else result |= matched;
        }
        return hasState && result;
    }

    /** @return this definition's stable, mod-namespaced identifier (e.g. {@code trackertips:welcome}). */
    public ResourceLocation id() { return id; }
    /** @return whether this hint should show at most once per player (equivalent to {@code max_times: 1} unless overridden). */
    public boolean once() { return once; }
    /** @return stacking priority; higher values render closer to the bottom of the on-screen stack. */
    public int priority() { return priority; }
    /** @return ticks that must pass before this hint can trigger again for the same player, after last being shown. */
    public int cooldown() { return cooldown; }
    /** @return how long the popup stays visible, in ticks; {@code <= 0} means persistent (tied to {@link #currentState}). */
    public int duration() { return duration; }
    public int accentColor() { return accentColor; }
    public String sound() { return sound; }
    public float pitch() { return pitch; }
    public JsonElement text() { return text; }
    public JsonElement title() { return title; }
    public String icon() { return icon; }

    // Getter needed by HintEngine.
    public int maxTimes() { return maxTimes; }
    public String theme() { return theme; }

    /**
     * Feature: nested/chained listeners. When non-null, showing this hint starts listening
     * (per-player) for {@link HintChain#trigger()}; once that matches, {@link HintChain#action()}
     * either dismisses this popup or shows {@link HintChain#next()} in its place. See
     * {@link HintChain} for the full JSON shape and semantics.
     *
     * @return the chain configuration, or {@code null} if this hint has none.
     */
    public HintChain chain() { return chain; }
}
