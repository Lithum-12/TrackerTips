package io.github.lithum12.trackertips.trigger;

import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** 统一包装 Forge 事件，避免每个 HintTrigger 都直接依赖事件总线。 */
public final class TriggerEvent {
    public enum Type {
        ITEM_OBTAINED,
        KILL_ENTITY,
        MINE_BLOCK,
        POTION_ADDED,
        POTION_REMOVED,
        ADVANCEMENT,
        DIMENSION_CHANGE
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

    public static TriggerEvent itemObtained(ItemStack stack) {
        return new TriggerEvent(Type.ITEM_OBTAINED, stack, null, null, null, null, null, null);
    }

    public static TriggerEvent kill(Entity entity) {
        return new TriggerEvent(Type.KILL_ENTITY, null, entity, null, null, null, null, null);
    }

    public static TriggerEvent mine(BlockState state) {
        return new TriggerEvent(Type.MINE_BLOCK, null, null, state, null, null, null, null);
    }

    public static TriggerEvent potionAdded(MobEffectInstance effect) {
        return new TriggerEvent(Type.POTION_ADDED, null, null, null, effect, null, null, null);
    }

    public static TriggerEvent potionRemoved(MobEffectInstance effect) {
        return new TriggerEvent(Type.POTION_REMOVED, null, null, null, effect, null, null, null);
    }

    public static TriggerEvent advancement(Advancement advancement) {
        return new TriggerEvent(Type.ADVANCEMENT, null, null, null, null, advancement, null, null);
    }

    public static TriggerEvent dimensionChange(ResourceKey<Level> from, ResourceKey<Level> to) {
        return new TriggerEvent(Type.DIMENSION_CHANGE, null, null, null, null, null, from, to);
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
