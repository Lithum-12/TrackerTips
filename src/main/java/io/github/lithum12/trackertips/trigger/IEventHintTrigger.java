package io.github.lithum12.trackertips.trigger;

import net.minecraft.server.level.ServerPlayer;

/**
 * 需要由 Minecraft/Forge 原生事件驱动的触发器。
 * 这类触发器不会依赖低频 PlayerTick 轮询，因此不会漏掉瞬时事件。
 */
public interface IEventHintTrigger extends IHintTrigger {
    boolean matchesEvent(ServerPlayer player, TriggerEvent event);

    @Override
    default boolean test(ServerPlayer player) {
        // 事件型触发器不参与普通轮询；真正触发由 matchesEvent() 完成。
        return false;
    }
}
