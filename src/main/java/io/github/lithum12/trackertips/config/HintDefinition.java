package io.github.lithum12.trackertips.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.lithum12.trackertips.trigger.IHintTrigger;
import io.github.lithum12.trackertips.trigger.IEventHintTrigger;
import io.github.lithum12.trackertips.trigger.TriggerEvent;
import io.github.lithum12.trackertips.trigger.Triggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public class HintDefinition {

    private final ResourceLocation id;
    private final boolean once;
    private final int priority;
    private final int cooldown;
    private final int duration;
    private final boolean requireAll;
    private final int accentColor;
    private final String sound;
    private final JsonElement text;
    private final List<IHintTrigger> triggers;
    private final JsonElement title;
    private final String icon;
    private final int maxTimes;

    public HintDefinition(ResourceLocation id, boolean once, int priority, int cooldown, int duration,
                          boolean requireAll, int accentColor, String sound, JsonElement title, JsonElement text, String icon,
                          List<IHintTrigger> triggers, int maxTimes) {
        this.id = id;
        this.once = once;
        this.priority = priority;
        this.cooldown = cooldown;
        this.duration = duration;
        this.requireAll = requireAll;
        this.accentColor = accentColor;
        this.sound = sound;
        this.text = text;
        this.title = title;
        this.icon = icon;
        this.triggers = triggers;
        this.maxTimes = maxTimes;
    }

    public static HintDefinition fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "id"));
        boolean once = GsonHelper.getAsBoolean(json, "once", true);
        int priority = GsonHelper.getAsInt(json, "priority", 0);
        int cooldown = GsonHelper.getAsInt(json, "cooldown", 0);
        int duration = GsonHelper.getAsInt(json, "duration", 240); // Defaults to a 12-second duration; use "duration": -1 for a persistent hint
        boolean requireAll = GsonHelper.getAsString(json, "require", "any").equalsIgnoreCase("all");
        int accent = (int) Long.parseLong(GsonHelper.getAsString(json, "accent", "F2C14E"), 16);
        String sound = GsonHelper.getAsString(json, "sound", "");
        JsonElement title = json.has("title") ? json.get("title") : null;
        JsonElement text = json.get("text");
        String icon = GsonHelper.getAsString(json, "icon", "");

        // 【修改点 1】解析最大触发次数，0 表示无限次
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

        // 【修改点 2】把 maxTimes 传进构造器（第 13 个参数）
        return new HintDefinition(id, once, priority, cooldown, duration, requireAll, accent, sound,
                title, text, icon, triggers, maxTimes);
    }

    /** 普通轮询：事件型触发器不参与，避免把事件型条件误当成持续状态。 */
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

        // all 模式只要包含事件条件，就必须等对应 Forge 事件发生。
        if (requireAll && hasEventTrigger) return false;
        // 纯事件触发器当然也不能靠轮询触发。
        if (!hasStateTrigger) return false;
        return result;
    }

    /** Forge 原生事件触发：事件型条件由 event 提供，普通状态条件仍实时检查。 */
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

        // any 模式下只有“当前事件对应的事件型 trigger”才有资格触发，
        // 否则一次拾取物品可能把一个完全无关的 game_time hint 也触发出来。
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

    public ResourceLocation id() { return id; }
    public boolean once() { return once; }
    public int priority() { return priority; }
    public int cooldown() { return cooldown; }
    public int duration() { return duration; }
    public int accentColor() { return accentColor; }
    public String sound() { return sound; }
    public JsonElement text() { return text; }
    public JsonElement title() { return title; }
    public String icon() { return icon; }

    // 【修改点 3】getter，HintEngine 里要用
    public int maxTimes() { return maxTimes; }
}