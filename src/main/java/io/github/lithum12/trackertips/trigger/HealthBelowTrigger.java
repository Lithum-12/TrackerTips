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

    public boolean isCurrentlyBelow(ServerPlayer player) {
        return player.getHealth() <= threshold && player.isAlive();
    }

    @Override
    public boolean currentState(ServerPlayer player) {
        return isCurrentlyBelow(player);
    }

    @Override
    public boolean test(ServerPlayer player) {
        boolean isDangerNow = isCurrentlyBelow(player);
        Boolean wasDanger = wasInDanger.get(player.getUUID());
        if (wasDanger == null) { wasInDanger.put(player.getUUID(), isDangerNow); return false; }
        wasInDanger.put(player.getUUID(), isDangerNow);
        // Edge trigger: safe before, dangerous now
        return isDangerNow && !wasDanger;
    }
}