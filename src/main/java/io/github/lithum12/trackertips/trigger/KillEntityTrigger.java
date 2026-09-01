package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

/** Precisely detects player kills via LivingDeathEvent. */
public class KillEntityTrigger implements IEventHintTrigger {
    private final EntityType<?> entityType;

    public KillEntityTrigger(EntityType<?> entityType) { this.entityType = entityType; }

    public static KillEntityTrigger fromJson(JsonObject json) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                new ResourceLocation(GsonHelper.getAsString(json, "entity")));
        if (type == null) throw new IllegalArgumentException("[TrackerTips] Unknown entity: " + json.get("entity"));
        return new KillEntityTrigger(type);
    }

    @Override
    public boolean matchesEvent(ServerPlayer player, TriggerEvent event) {
        return event.type() == TriggerEvent.Type.KILL_ENTITY
                && event.entity() != null
                && event.entity().getType() == entityType;
    }
}
