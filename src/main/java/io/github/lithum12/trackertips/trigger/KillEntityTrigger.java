package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillEntityTrigger implements IHintTrigger {
    private final EntityType<?> entityType;
    private final Map<UUID, Integer> lastKillCount = new HashMap<>();

    public KillEntityTrigger(EntityType<?> entityType) { this.entityType = entityType; }

    public static KillEntityTrigger fromJson(JsonObject json) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(GsonHelper.getAsString(json, "entity")));
        if (type == null) throw new IllegalArgumentException("[TrackerTips] 未知实体: " + json.get("entity"));
        return new KillEntityTrigger(type);
    }

    @Override
    public boolean test(ServerPlayer player) {
        int currentKills = player.getStats().getValue(Stats.ENTITY_KILLED, entityType);
        Integer lastKills = lastKillCount.get(player.getUUID());
        if (lastKills == null) { lastKillCount.put(player.getUUID(), currentKills); return false; }
        lastKillCount.put(player.getUUID(), currentKills);
        return currentKills > lastKills; // 只要数量增加，就是刚击杀
    }
}