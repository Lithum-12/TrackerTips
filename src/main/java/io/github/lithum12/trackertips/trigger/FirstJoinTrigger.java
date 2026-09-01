package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fires exactly once per player: the very first time they ever log into a world running
 * TrackerTips. Takes no configuration - {@code {"type": "trackertips:first_join"}} is the whole
 * trigger.
 *
 * <p>"First" is tracked per-player via {@code PlayerHintData#hasJoinedBefore()}, persisted with
 * the rest of that player's TrackerTips capability data, so it survives relogging, server
 * restarts, and (via the vanilla player-clone path) death/respawn. It is set the first time
 * {@code io.github.lithum12.trackertips.event.TTCommonEvents#onPlayerLoggedIn} observes the
 * player, which is also the moment {@link TriggerEvent#firstJoin()} is dispatched - so by the
 * time any {@code first_join} trigger is evaluated, the flag reflects "already handled",
 * guaranteeing the event (and this trigger) fire at most once per player, ever.
 */
public final class FirstJoinTrigger implements IEventHintTrigger {

    public static FirstJoinTrigger fromJson(JsonObject json) {
        return new FirstJoinTrigger();
    }

    @Override
    public boolean matchesEvent(ServerPlayer player, TriggerEvent event) {
        return event != null && event.type() == TriggerEvent.Type.FIRST_JOIN;
    }
}
