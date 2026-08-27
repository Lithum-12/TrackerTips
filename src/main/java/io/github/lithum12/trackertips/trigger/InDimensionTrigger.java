package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InDimensionTrigger implements IHintTrigger {
    private final ResourceKey<Level> dimension;
    private final Map<UUID, ResourceKey<Level>> lastDimension = new HashMap<>();

    public InDimensionTrigger(ResourceKey<Level> dimension) { this.dimension = dimension; }

    public static InDimensionTrigger fromJson(JsonObject json) {
        String dim = GsonHelper.getAsString(json, "dimension", "minecraft:the_nether");
        return new InDimensionTrigger(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dim)));
    }

    @Override
    public boolean test(ServerPlayer player) {
        ResourceKey<Level> current = player.level().dimension();
        ResourceKey<Level> last = lastDimension.get(player.getUUID());
        if (last == null) { lastDimension.put(player.getUUID(), current); return false; }
        lastDimension.put(player.getUUID(), current);
        // 边缘触发：上一次不在，这一次在
        return current.equals(dimension) && !last.equals(dimension);
    }
}