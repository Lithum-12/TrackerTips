package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HealthBelowTrigger implements IHintTrigger {
    private final float threshold;
    private final Map<UUID, Boolean> wasInDanger = new HashMap<>();

    public HealthBelowTrigger(float threshold) { this.threshold = threshold; }

    public static HealthBelowTrigger fromJson(JsonObject json) {
        return new HealthBelowTrigger(GsonHelper.getAsFloat(json, "health", 6.0f));
    }

    @Override
    public boolean test(ServerPlayer player) {
        boolean isDangerNow = player.getHealth() <= threshold && player.isAlive();
        Boolean wasDanger = wasInDanger.get(player.getUUID());
        if (wasDanger == null) { wasInDanger.put(player.getUUID(), isDangerNow); return false; }
        wasInDanger.put(player.getUUID(), isDangerNow);
        // 边缘触发：之前安全，现在危险
        return isDangerNow && !wasDanger;
    }
}