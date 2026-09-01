package io.github.lithum12.trackertips.trigger;

import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A single, immutable snapshot of a Forge event that TrackerTips knows how to react to.
 *
 * <p>{@link IEventHintTrigger} implementations never subscribe to the Forge event bus directly;
 * instead, {@code io.github.lithum12.trackertips.event.TTCommonEvents} listens for the raw Forge
 * events and wraps the relevant details into one of these before handing it to
 * {@code HintEngine.triggerEvent(ServerPlayer, TriggerEvent)}. This keeps every trigger
 * implementation decoupled from Forge's event classes and gives addon mods a single, stable
 * surface to target instead of the ever-changing set of vanilla/Forge event types.
 *
 * <p>Only the fields relevant to {@link #type()} are populated; every other accessor returns
 * {@code null}. Check {@link #type()} before reading any other field.
 *
 * <p><b>Extending this class:</b> addon mods that need to react to an event TrackerTips doesn't
 * already wrap can either match a matching {@link IHintTrigger} against
 * {@code player}/{@code event} state directly (state triggers, via {@link IHintTrigger#test}),
 * or, if a new Forge event needs wrapping, add a new {@link Type} constant and factory method
 * here and fire it from their own event listener via
 * {@code HintEngine.triggerEvent(player, TriggerEvent.myNewType(...))}. TrackerTips does not
 * currently expose {@link Type} as an open registry; if you need one, ask upstream or open a PR.
 */
public final class TriggerEvent {
    /** Identifies which fields of a {@link TriggerEvent} are populated. */
    public enum Type {
        ITEM_OBTAINED,
        KILL_ENTITY,
        MINE_BLOCK,
        POTION_ADDED,
        POTION_REMOVED,
        ADVANCEMENT,
        DIMENSION_CHANGE,
        /** Fired exactly once per player: the very first time they ever join a world running TrackerTips. */
        FIRST_JOIN
    }

    private final Type type;
    private final ItemStack itemStack;
    private final Entity entity;
    private final BlockState blockState;
    private final MobEffectInstance effect;
    private final Advancement advancement;
    private final ResourceKey<Level> fromDimension;
    private final ResourceKey<Level> toDimension;

    private TriggerEvent(Type type, ItemStack itemStack, Entity entity, BlockState blockState,
                         MobEffectInstance effect, Advancement advancement,
                         ResourceKey<Level> fromDimension, ResourceKey<Level> toDimension) {
        this.type = type;
        this.itemStack = itemStack;
        this.entity = entity;
        this.blockState = blockState;
        this.effect = effect;
        this.advancement = advancement;
        this.fromDimension = fromDimension;
        this.toDimension = toDimension;
    }

    /** @param stack the item the player just picked up. */
    public static TriggerEvent itemObtained(ItemStack stack) {
        return new TriggerEvent(Type.ITEM_OBTAINED, stack, null, null, null, null, null, null);
    }

    /** @param entity the entity the player just killed. */
    public static TriggerEvent kill(Entity entity) {
        return new TriggerEvent(Type.KILL_ENTITY, null, entity, null, null, null, null, null);
    }

    /** @param state the block state that was just broken. */
    public static TriggerEvent mine(BlockState state) {
        return new TriggerEvent(Type.MINE_BLOCK, null, null, state, null, null, null, null);
    }

    /** @param effect the potion effect instance that was just added to the player. */
    public static TriggerEvent potionAdded(MobEffectInstance effect) {
        return new TriggerEvent(Type.POTION_ADDED, null, null, null, effect, null, null, null);
    }

    /** @param effect the potion effect instance that was just removed from the player. */
    public static TriggerEvent potionRemoved(MobEffectInstance effect) {
        return new TriggerEvent(Type.POTION_REMOVED, null, null, null, effect, null, null, null);
    }

    /** @param advancement the advancement the player just earned. */
    public static TriggerEvent advancement(Advancement advancement) {
        return new TriggerEvent(Type.ADVANCEMENT, null, null, null, null, advancement, null, null);
    }

    /**
     * @param from the dimension the player just left.
     * @param to   the dimension the player just entered.
     */
    public static TriggerEvent dimensionChange(ResourceKey<Level> from, ResourceKey<Level> to) {
        return new TriggerEvent(Type.DIMENSION_CHANGE, null, null, null, null, null, from, to);
    }

    /**
     * A player's very first login to a world running TrackerTips. Carries no extra payload;
     * only {@link #type()} matters. See {@code PlayerHintData#hasJoinedBefore()} for how the
     * "first" determination is made and persisted.
     */
    public static TriggerEvent firstJoin() {
        return new TriggerEvent(Type.FIRST_JOIN, null, null, null, null, null, null, null);
    }

    public Type type() { return type; }
    public ItemStack itemStack() { return itemStack; }
    public Entity entity() { return entity; }
    public BlockState blockState() { return blockState; }
    public MobEffectInstance effect() { return effect; }
    public Advancement advancement() { return advancement; }
    public ResourceKey<Level> fromDimension() { return fromDimension; }
    public ResourceKey<Level> toDimension() { return toDimension; }
}
