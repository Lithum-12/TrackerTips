package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import io.github.lithum12.trackertips.TrackerTips;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class Triggers {

    private static final Map<ResourceLocation, Function<JsonObject, IHintTrigger>> REGISTRY = new HashMap<>();

    public static void init() {
        register(new ResourceLocation(TrackerTips.MODID, "game_time"), GameTimeTrigger::fromJson);
        register(new ResourceLocation(TrackerTips.MODID, "potion_effect"), PotionEffectTrigger::fromJson);
        register(new ResourceLocation(TrackerTips.MODID, "has_item"), HasItemTrigger::fromJson);
        register(new ResourceLocation(TrackerTips.MODID, "advancement"), AdvancementTrigger::fromJson);
        register(new ResourceLocation(TrackerTips.MODID, "item_obtained"), ItemObtainedTrigger::fromJson);
        register(new ResourceLocation(TrackerTips.MODID, "in_dimension"), InDimensionTrigger::fromJson);
        register(new ResourceLocation(TrackerTips.MODID, "health_below"), HealthBelowTrigger::fromJson);
        register(new ResourceLocation(TrackerTips.MODID, "kill_entity"), KillEntityTrigger::fromJson);
        register(new ResourceLocation(TrackerTips.MODID, "mine_block"), MineBlockTrigger::fromJson);
    }

    public static void register(ResourceLocation id, Function<JsonObject, IHintTrigger> factory) {
        REGISTRY.put(id, factory);
    }

    public static IHintTrigger create(ResourceLocation id, JsonObject json) {
        Function<JsonObject, IHintTrigger> factory = REGISTRY.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("未知触发器类型: " + id);
        }
        return factory.apply(json);
    }
}