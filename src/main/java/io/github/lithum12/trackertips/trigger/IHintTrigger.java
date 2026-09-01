package io.github.lithum12.trackertips.trigger;

import net.minecraft.server.level.ServerPlayer;

/**
 * A single condition a {@code HintDefinition}'s trigger list can be made of.
 *
 * <p>This is TrackerTips' primary extension point for new condition types. Addon mods implement
 * this interface (or {@link IEventHintTrigger} for event-driven conditions), register a factory
 * for it with {@link Triggers#register}, and server owners can then reference the new type from
 * event JSON exactly like a built-in trigger, e.g. {@code {"type": "mymod:my_condition", ...}}.
 *
 * <p>Implementations should be small, stateless (or hold only the values parsed from JSON), and
 * safe to call every tick: {@link #test} may be invoked once per configured
 * {@code check_interval} for every online player, for every hint definition that references it.
 *
 * @see IEventHintTrigger for conditions that should only be (re)checked when a specific Forge
 *      event fires, rather than on a fixed polling interval.
 * @see Triggers#register(net.minecraft.resources.ResourceLocation, Triggers.TriggerFactory)
 */
public interface IHintTrigger {

    /**
     * Whether this condition currently holds for {@code player}. Called on the regular polling
     * interval (see {@code TTSettings#checkInterval}) for state-based triggers; event-driven
     * triggers (see {@link IEventHintTrigger}) are polled instead via {@link #currentState}.
     *
     * @param player the player being checked. Always server-side.
     * @return true if the condition is currently satisfied.
     */
    boolean test(ServerPlayer player);

    /**
     * Returns the current condition state, independent of any specific event.
     *
     * <p>For ordinary state triggers this defaults to {@link #test}. Edge-triggered/event-driven
     * triggers (e.g. "an item was just obtained") override this to report whatever *persistent*
     * state, if any, corresponds to the edge - this is what lets a {@code duration: -1} hint that
     * was shown by an event stay visible (or get hidden) as game state changes, without needing
     * the same event to fire again.
     *
     * @param player the player being checked.
     * @return the trigger's persistent state, or false if it has none.
     */
    default boolean currentState(ServerPlayer player) {
        return test(player);
    }
}
