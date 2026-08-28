package io.github.lithum12.trackertips.engine;

import io.github.lithum12.trackertips.config.HintDefinition;
import io.github.lithum12.trackertips.config.TTConfigManager;
import io.github.lithum12.trackertips.network.HideHintPacket;
import io.github.lithum12.trackertips.network.ShowHintPacket;
import io.github.lithum12.trackertips.network.TTNetwork;
import io.github.lithum12.trackertips.player.PlayerHintData;
import io.github.lithum12.trackertips.player.TTCapabilities;
import io.github.lithum12.trackertips.trigger.TriggerEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class HintEngine {

    /** 普通状态条件的低频轮询入口。 */
    public static void tickPlayer(ServerPlayer player) {
        if (!TTConfigManager.settings().enable) return;
        PlayerHintData data = player.getCapability(TTCapabilities.HINT_DATA).orElse(null);
        if (data == null) return;

        for (HintDefinition def : TTConfigManager.hints()) {
            checkAndShow(player, def, data, false, null);
        }
    }

    /** 保留给兼容代码；事件现在应使用 triggerEvent()。 */
    public static void checkPlayer(ServerPlayer player) {
        tickPlayer(player);
    }

    /**
     * Forge 原生事件入口。
     * 事件发生后只检查与该事件有关的 event-trigger，同时检查 all 模式下的状态条件。
     */
    public static void triggerEvent(ServerPlayer player, TriggerEvent event) {
        if (!TTConfigManager.settings().enable) return;
        PlayerHintData data = player.getCapability(TTCapabilities.HINT_DATA).orElse(null);
        if (data == null) return;

        for (HintDefinition def : TTConfigManager.hints()) {
            checkAndShow(player, def, data, true, event);
        }
    }

    private static void checkAndShow(ServerPlayer player, HintDefinition def, PlayerHintData data,
                                     boolean eventMode, TriggerEvent event) {
        if (def.duration() <= 0) {
            boolean matches = eventMode ? def.matchesEvent(player, event) : def.matches(player);
            boolean currentState = def.currentState(player);
            boolean isActive = data.isPersistentlyActive(def.id());

            // A state trigger (such as health_below or potion_effect:active)
            // must remain visible while its condition remains true.
            if (!eventMode) {
                matches = currentState;
            }

            if (matches && !isActive) {
                sendShowPacket(player, def);
                data.setPersistentlyActive(def.id(), true);
            } else if (!currentState && isActive) {
                TTNetwork.sendToPlayer(player, new HideHintPacket(def.id()));
                data.setPersistentlyActive(def.id(), false);
            }
            return;
        }

        long now = player.level().getGameTime();
        if (def.maxTimes() > 0 && data.getCount(def.id()) >= def.maxTimes()) return;
        if (def.once() && data.hasShown(def.id())) return;
        if (data.isInCooldown(def.id(), now)) return;

        boolean matches = eventMode ? def.matchesEvent(player, event) : def.matches(player);
        if (!matches) return;

        data.incrementCount(def.id());
        data.markShown(def.id());
        if (def.cooldown() > 0) data.setCooldown(def.id(), now, def.cooldown());
        sendShowPacket(player, def);
    }

    private static void sendShowPacket(ServerPlayer player, HintDefinition def) {
        sendShowPacket(player, def, def.duration());
    }

    private static void sendShowPacket(ServerPlayer player, HintDefinition def, int duration) {
        TrackerTipsLogger.logTrigger(player, def);
        String titleJson = def.title() != null ? def.title().toString() : "";
        TTNetwork.sendToPlayer(player, new ShowHintPacket(
                def.id(), def.text().toString(), titleJson, def.icon(), duration,
                def.priority(), def.accentColor(), def.sound()));
    }

    public static void forceShow(ServerPlayer player, ResourceLocation id) {
        HintDefinition def = null;
        for (HintDefinition d : TTConfigManager.hints()) {
            if (d.id().equals(id)) { def = d; break; }
        }
        if (def == null) {
            player.sendSystemMessage(Component.translatable("trackertips.command.test.not_found", id)
                    .withStyle(ChatFormatting.RED));
            return;
        }
        int duration = def.duration() > 0 ? def.duration() : TTConfigManager.settings().defaultDuration;
        sendShowPacket(player, def, duration);
        player.sendSystemMessage(Component.translatable("trackertips.command.test.sent", id)
                .withStyle(ChatFormatting.GREEN));
    }

    private static final class TrackerTipsLogger {
        static void logTrigger(ServerPlayer player, HintDefinition def) {
            io.github.lithum12.trackertips.TrackerTips.LOGGER.debug(
                    "[TrackerTips] Triggered hint {} -> {}", def.id(), player.getGameProfile().getName());
        }
    }
}
