package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;

/** 维度切换使用 PlayerChangedDimensionEvent；进入维度时只触发一次。 */
public class InDimensionTrigger implements IEventHintTrigger {
    private final ResourceKey<Level> dimension;

    public InDimensionTrigger(ResourceKey<Level> dimension) { this.dimension = dimension; }

    public static InDimensionTrigger fromJson(JsonObject json) {
        String dim = GsonHelper.getAsString(json, "dimension", "minecraft:the_nether");
        return new InDimensionTrigger(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dim)));
    }

    @Override
    public boolean matchesEvent(ServerPlayer player, TriggerEvent event) {
        return event.type() == TriggerEvent.Type.DIMENSION_CHANGE
                && dimension.equals(event.toDimension())
                && !dimension.equals(event.fromDimension());
    }
}
