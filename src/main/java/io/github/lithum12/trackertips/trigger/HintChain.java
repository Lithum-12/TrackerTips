package io.github.lithum12.trackertips.trigger;

import net.minecraft.resources.ResourceLocation;

/**
 * Describes a "chained" or "nested" follow-up condition attached to a {@code HintDefinition}.
 *
 * <p>Feature: when a hint that has a chain is shown, TrackerTips starts listening for
 * {@link #trigger()} specifically for the player it was shown to (see
 * {@code PlayerHintData#startChainListening}). As soon as that trigger matches, the chain's
 * {@link #action()} runs:
 * <ul>
 *   <li>{@link Action#DISMISS} - the popup is simply hidden.</li>
 *   <li>{@link Action#NEXT} - the popup is hidden and the hint identified by {@link #next()} is
 *       force-shown immediately, letting server owners build a short guided sequence of popups
 *       (e.g. "break a log" -&gt; once broken -&gt; "now open your inventory" -&gt; ...).</li>
 * </ul>
 *
 * <p>A chain's {@link #trigger()} can be any registered {@link IHintTrigger}, built-in or
 * addon-provided (see {@link Triggers}) - the same vocabulary used for a hint's own top-level
 * triggers. This is what makes chains "nested listeners": a popup's chain trigger is itself just
 * another trigger, evaluated the same way, just scoped to a single already-shown popup instead of
 * to the whole hint list.
 *
 * <p>Event JSON shape:
 * <pre>{@code
 * "chain": {
 *   "trigger": { "type": "trackertips:mine_block", "block": "minecraft:stone" },
 *   "action": "next",
 *   "next": "trackertips:welcome_step2"
 * }
 * }</pre>
 *
 * @param trigger the condition that ends this chain step.
 * @param action  what to do once {@link #trigger()} matches.
 * @param next    the hint id to force-show next, when {@link #action()} is {@link Action#NEXT}.
 *                Ignored (may be null) for {@link Action#DISMISS}.
 */
public record HintChain(IHintTrigger trigger, Action action, ResourceLocation next) {

    /** What happens once a {@link HintChain}'s {@link #trigger()} matches. */
    public enum Action {
        /** Hide the popup that owns this chain; no follow-up popup is shown. */
        DISMISS,
        /** Hide the popup that owns this chain and immediately show {@link HintChain#next()}. */
        NEXT;

        public static Action fromId(String id) {
            return "next".equalsIgnoreCase(id) ? NEXT : DISMISS;
        }
    }
}
