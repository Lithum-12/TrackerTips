package io.github.lithum12.trackertips.trigger;

import net.minecraft.server.level.ServerPlayer;

public interface IHintTrigger {
    boolean test(ServerPlayer player);
}