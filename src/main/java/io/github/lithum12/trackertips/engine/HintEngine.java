package io.github.lithum12.trackertips.engine;

import io.github.lithum12.trackertips.config.HintDefinition;
import io.github.lithum12.trackertips.config.TTConfigManager;
import io.github.lithum12.trackertips.network.ShowHintPacket;
import io.github.lithum12.trackertips.network.TTNetwork;
import io.github.lithum12.trackertips.player.PlayerHintData;
import io.github.lithum12.trackertips.player.TTCapabilities;
import net.minecraft.server.level.ServerPlayer;

public class HintEngine {

    public static void tickPlayer(ServerPlayer player) {
        if (!TTConfigManager.settings().enable) {
            return;
        }
        for (HintDefinition def : TTConfigManager.hints()) {
            tryShow(player, def);
        }
    }

    public static void checkPlayer(ServerPlayer player) {
        tickPlayer(player);
    }

    private static void tryShow(ServerPlayer player, HintDefinition def) {
        PlayerHintData data = player.getCapability(TTCapabilities.HINT_DATA).orElse(null);
        if (data == null) {
            return;
        }

        long now = player.level().getGameTime();

        if (def.once() && data.hasShown(def.id())) {
            return;
        }
        if (data.isInCooldown(def.id(), now)) {
            return;
        }
        if (!def.matches(player)) {
            return;
        }

        data.markShown(def.id());
        if (def.cooldown() > 0) {
            data.setCooldown(def.id(), now, def.cooldown());
        }

        int duration = def.duration() >= 0 ? def.duration() : TTConfigManager.settings().defaultDuration;

        TTNetwork.sendToPlayer(player, new ShowHintPacket(
                def.id(),
                def.text().toString(),
                duration,
                def.priority(),
                def.accentColor(),
                def.sound()
        ));
    }
}