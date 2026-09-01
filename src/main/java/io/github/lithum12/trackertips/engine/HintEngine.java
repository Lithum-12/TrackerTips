package io.github.lithum12.trackertips.engine;

import io.github.lithum12.trackertips.config.HintDefinition;
import io.github.lithum12.trackertips.config.TTConfigManager;
import io.github.lithum12.trackertips.network.HideHintPacket;
import io.github.lithum12.trackertips.network.ShowHintPacket;
import io.github.lithum12.trackertips.network.TTNetwork;
import io.github.lithum12.trackertips.player.PlayerHintData;
import io.github.lithum12.trackertips.player.TTCapabilities;
import io.github.lithum12.trackertips.trigger.HintChain;
import io.github.lithum12.trackertips.trigger.IEventHintTrigger;
import io.github.lithum12.trackertips.trigger.IHintTrigger;
import io.github.lithum12.trackertips.trigger.TriggerEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server-side scheduler that decides, for a given player, which hint popups should be shown,
 * kept visible, or hidden, and drives {@code HintChain} follow-up listeners.
 *
 * <p>There are two entry points, both safe to call as often as needed:
 * <ul>
 *   <li>{@link #tickPlayer(ServerPlayer)} - the low-frequency polling path, driven by
 *       {@code TTCommonEvents#onPlayerTick} at {@code TTSettings#checkInterval}. Only
 *       state-based triggers (plain {@link IHintTrigger}s, and any {@link IEventHintTrigger}
 *       explicitly not event-driven) are (re)checked here.</li>
 *   <li>{@link #triggerEvent(ServerPlayer, TriggerEvent)} - the responsive path, called
 *       immediately whenever {@code TTCommonEvents} observes a relevant Forge event. Event-driven
 *       triggers are checked here instead of waiting for the next poll.</li>
 * </ul>
 * Both paths also drive {@link #checkChainListeners}, which resolves any {@code HintChain}
 * follow-up conditions started by {@link #checkAndShow} - see that method and {@link HintChain}
 * for the nested/chained-listener feature this implements.
 */
public class HintEngine {

    /** Low-frequency polling entry point for regular state conditions. */
    public static void tickPlayer(ServerPlayer player) {
        if (!TTConfigManager.settings().enable) return;
        PlayerHintData data = player.getCapability(TTCapabilities.HINT_DATA).orElse(null);
        if (data == null) return;

        checkChainListeners(player, data, false, null);

        for (HintDefinition def : TTConfigManager.hints()) {
            checkAndShow(player, def, data, false, null);
        }
    }

    /** Kept for backward compatibility; new code should use triggerEvent(). */
    public static void checkPlayer(ServerPlayer player) {
        tickPlayer(player);
    }

    /**
     * Native Forge event entry point.
     * After an event fires, only checks the event-triggers relevant to it, while also checking state conditions in "all" mode.
     */
    public static void triggerEvent(ServerPlayer player, TriggerEvent event) {
        if (!TTConfigManager.settings().enable) return;
        PlayerHintData data = player.getCapability(TTCapabilities.HINT_DATA).orElse(null);
        if (data == null) return;

        checkChainListeners(player, data, true, event);

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
                startChainIfConfigured(data, def);
            } else if (!currentState && isActive) {
                hideHint(player, data, def.id());
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
        startChainIfConfigured(data, def);
    }

    /**
     * Feature: nested/chained listeners. If {@code def} has a {@link HintChain} configured,
     * starts listening for its follow-up trigger for this player. Resolution happens in
     * {@link #checkChainListeners}.
     */
    private static void startChainIfConfigured(PlayerHintData data, HintDefinition def) {
        if (def.chain() != null) {
            data.startChainListening(def.id());
        }
    }

    /**
     * Feature: nested/chained listeners. Resolves every hint id this player is currently
     * chain-listening on (see {@link PlayerHintData#chainListeningIds()}): if that hint's
     * {@link HintChain#trigger()} now matches, the chain fires - either dismissing the popup
     * ({@link HintChain.Action#DISMISS}) or hiding it and force-showing
     * {@link HintChain#next()} ({@link HintChain.Action#NEXT}), continuing the sequence if the
     * next hint has its own chain configured.
     *
     * <p>State-based chain triggers are only (re)checked from the polling path
     * ({@code eventMode == false}); event-driven chain triggers are only checked from
     * {@link #triggerEvent} while the matching event is being processed - mirroring how
     * {@link HintDefinition#matches}/{@link HintDefinition#matchesEvent} treat top-level triggers.
     */
    private static void checkChainListeners(ServerPlayer player, PlayerHintData data,
                                            boolean eventMode, TriggerEvent event) {
        for (ResourceLocation hintId : data.chainListeningIds()) {
            HintDefinition def = TTConfigManager.hintById(hintId);
            HintChain chain = def != null ? def.chain() : null;
            if (chain == null) {
                // The hint was removed/edited (e.g. via /trackertips reload) since it started
                // listening; nothing left to resolve.
                data.stopChainListening(hintId);
                continue;
            }

            IHintTrigger trigger = chain.trigger();
            boolean isEventDriven = trigger instanceof IEventHintTrigger eventTrigger && eventTrigger.isEventDriven();
            if (isEventDriven != eventMode) continue;

            boolean matched = isEventDriven
                    ? ((IEventHintTrigger) trigger).matchesEvent(player, event)
                    : trigger.test(player);
            if (!matched) continue;

            resolveChain(player, data, hintId, chain);
        }
    }

    private static void resolveChain(ServerPlayer player, PlayerHintData data, ResourceLocation hintId, HintChain chain) {
        data.stopChainListening(hintId);
        hideHint(player, data, hintId);

        if (chain.action() != HintChain.Action.NEXT || chain.next() == null) return;

        HintDefinition nextDef = TTConfigManager.hintById(chain.next());
        if (nextDef == null) {
            io.github.lithum12.trackertips.TrackerTips.LOGGER.warn(
                    "[TrackerTips] Hint '{}' chains to unknown hint id '{}'", hintId, chain.next());
            return;
        }

        int duration = nextDef.duration() > 0 ? nextDef.duration() : TTConfigManager.settings().defaultDuration;
        sendShowPacket(player, nextDef, duration);
        data.markShown(nextDef.id());
        data.incrementCount(nextDef.id());
        if (nextDef.duration() <= 0) data.setPersistentlyActive(nextDef.id(), true);
        startChainIfConfigured(data, nextDef);
    }

    /** Sends a hide packet for {@code id} and cancels any pending chain listener it had. */
    private static void hideHint(ServerPlayer player, PlayerHintData data, ResourceLocation id) {
        TTNetwork.sendToPlayer(player, new HideHintPacket(id));
        data.stopChainListening(id);
    }

    private static void sendShowPacket(ServerPlayer player, HintDefinition def) {
        sendShowPacket(player, def, def.duration());
    }

    private static void sendShowPacket(ServerPlayer player, HintDefinition def, int duration) {
        TrackerTipsLogger.logTrigger(player, def);
        String titleJson = def.title() != null ? def.title().toString() : "";
        TTNetwork.sendToPlayer(player, new ShowHintPacket(
                def.id(), def.text().toString(), titleJson, def.icon(), duration,
                def.priority(), def.accentColor(), def.sound(), def.pitch(), def.theme()));
    }

    /** Forces {@code id} to display for {@code player} immediately, bypassing its own triggers/cooldown/max_times. Used by {@code /trackertips test}. */
    public static void forceShow(ServerPlayer player, ResourceLocation id) {
        HintDefinition def = TTConfigManager.hintById(id);
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
