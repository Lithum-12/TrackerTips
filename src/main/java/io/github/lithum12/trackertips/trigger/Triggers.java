package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import io.github.lithum12.trackertips.TrackerTips;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry mapping a trigger type id (e.g. {@code trackertips:mine_block}) to the factory that
 * builds an {@link IHintTrigger} from that trigger's JSON object.
 *
 * <p><b>This is TrackerTips' main addon entry point for new trigger types.</b> To add a custom
 * trigger type from another mod:
 * <ol>
 *   <li>Implement {@link IHintTrigger} (state-polled) or {@link IEventHintTrigger} (event-driven).</li>
 *   <li>Write a {@code static IHintTrigger fromJson(JsonObject json)} factory method (or any
 *       {@link TriggerFactory}) that reads whatever fields your trigger needs.</li>
 *   <li>Call {@link #register(ResourceLocation, TriggerFactory)} with your own mod-namespaced id
 *       (e.g. {@code new ResourceLocation("mymod", "my_condition")}) from your own mod's
 *       {@code FMLCommonSetupEvent} handler. Common setup is guaranteed to run after every mod's
 *       constructor - including TrackerTips' own {@link #init()} call - so registration order
 *       between mods is never a problem.</li>
 * </ol>
 * Server owners can then reference the new type from event JSON exactly like a built-in one:
 * {@code {"type": "mymod:my_condition", ...}}.
 *
 * <p>Registration is a plain in-memory map, not tied to Forge's deferred registry system, since
 * trigger factories are simple, stateless functions with no need for the ordering/network-sync
 * guarantees a full registry provides.
 */
public final class Triggers {

    /**
     * Builds an {@link IHintTrigger} instance from one trigger's JSON object. Implementations
     * should read their own fields defensively (falling back to sensible defaults) since the
     * JSON is hand-edited by server owners and may be incomplete or malformed.
     */
    @FunctionalInterface
    public interface TriggerFactory {
        IHintTrigger create(JsonObject json);
    }

    private static final Map<ResourceLocation, TriggerFactory> REGISTRY = new HashMap<>();

    private Triggers() {}

    /** Registers every trigger type TrackerTips ships with. Called once from the mod constructor. */
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
        register(new ResourceLocation(TrackerTips.MODID, "first_join"), FirstJoinTrigger::fromJson);
    }

    /**
     * Registers a trigger type factory under {@code id}. Re-registering an existing id silently
     * replaces its factory (last call wins) - this lets a datapack/addon-style mod deliberately
     * override a built-in trigger's behavior if it needs to, though that should be rare.
     *
     * @param id      a mod-namespaced id, e.g. {@code new ResourceLocation("mymod", "my_trigger")}.
     * @param factory builds an {@link IHintTrigger} from one trigger's JSON object.
     */
    public static void register(ResourceLocation id, TriggerFactory factory) {
        REGISTRY.put(id, factory);
    }

    /**
     * Builds a trigger instance for the given type id.
     *
     * @throws IllegalArgumentException if no factory is registered for {@code id} - typically
     *                                   because the event JSON references a trigger type from a
     *                                   mod that isn't installed, or a misspelled built-in type.
     */
    public static IHintTrigger create(ResourceLocation id, JsonObject json) {
        TriggerFactory factory = REGISTRY.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown trigger type: " + id);
        }
        return factory.create(json);
    }
}
