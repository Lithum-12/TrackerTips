package io.github.lithum12.trackertips.trigger;

import net.minecraft.server.level.ServerPlayer;

/**
 * An {@link IHintTrigger} that should be (re)checked when a specific {@link TriggerEvent} fires,
 * rather than on TrackerTips' regular polling interval.
 *
 * <p>Most built-in triggers that correspond to a one-off action (an item pickup, a kill, a block
 * break, an advancement, a dimension change, a player's first join, ...) implement this instead
 * of relying purely on {@link IHintTrigger#test}, both for responsiveness (no polling delay) and
 * correctness (a transient action might not still be "true" the next time polling runs).
 *
 * <p>Addon mods adding their own event-driven trigger types should implement this interface,
 * matching against {@link TriggerEvent#type()} and whichever payload accessor is relevant (see
 * {@link TriggerEvent} for the full set). If the Forge event you need isn't already wrapped by
 * {@link TriggerEvent}, see that class's docs for how to add one, or use a plain
 * {@link IHintTrigger} and poll instead.
 */
public interface IEventHintTrigger extends IHintTrigger {

    /**
     * Whether this trigger is event-driven rather than state-polled. Defaults to {@code true};
     * a trigger implementing this interface but returning {@code false} here is treated as an
     * ordinary polled {@link IHintTrigger} instead (its {@link #test} is used, not
     * {@link #matchesEvent}). This exists mainly so a single class could support both modes if
     * ever needed; nearly all implementations should leave this at the default.
     */
    default boolean isEventDriven() {
        return true;
    }

    /**
     * Whether {@code event} satisfies this trigger for {@code player}.
     *
     * <p>Only called while the matching Forge event is being processed (see {@link TriggerEvent}
     * for how raw Forge events get wrapped and dispatched here). Implementations should check
     * {@link TriggerEvent#type()} first and return {@code false} for any event type they don't
     * care about, since a hint definition's whole trigger list is walked on every dispatched
     * event.
     *
     * @param player the player the event happened to.
     * @param event  the wrapped event; never null when this method is invoked from the engine.
     * @return true if this specific event satisfies the trigger.
     */
    boolean matchesEvent(ServerPlayer player, TriggerEvent event);

    /**
     * Event-driven triggers have no meaningful standalone "is it true right now" state outside
     * of an event firing, so this always returns {@code false}; use {@link #matchesEvent} instead.
     */
    @Override
    default boolean test(ServerPlayer player) {
        return false;
    }
}
