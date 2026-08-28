package io.github.lithum12.trackertips.trigger;

import net.minecraft.server.level.ServerPlayer;

public interface IHintTrigger {
    boolean test(ServerPlayer player);

    /**
     * Returns the current condition state. For normal state triggers this is
     * the same as test(). Edge-triggered triggers may override it so persistent
     * hints can stay visible after the transition has fired.
     */
    default boolean currentState(ServerPlayer player) {
        return test(player);
    }
}
