package io.github.lithum12.trackertips.trigger;

import net.minecraft.server.level.ServerPlayer;

/** Trigger that can be matched from a Forge event. */
public interface IEventHintTrigger extends IHintTrigger {
    /** Whether this trigger is event-driven rather than state-polled. */
    default boolean isEventDriven() {
        return true;
    }

    boolean matchesEvent(ServerPlayer player, TriggerEvent event);

    @Override
    default boolean test(ServerPlayer player) {
        return false;
    }
}
