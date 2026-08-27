package io.github.lithum12.trackertips.engine;

import io.github.lithum12.trackertips.config.HintDefinition;
import io.github.lithum12.trackertips.config.TTConfigManager;
import io.github.lithum12.trackertips.network.HideHintPacket;
import io.github.lithum12.trackertips.network.ShowHintPacket;
import io.github.lithum12.trackertips.network.TTNetwork;
import io.github.lithum12.trackertips.player.PlayerHintData;
import io.github.lithum12.trackertips.player.TTCapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class HintEngine {

    public static void tickPlayer(ServerPlayer player) {
        if (!TTConfigManager.settings().enable) {
            return;
        }
        PlayerHintData data = player.getCapability(TTCapabilities.HINT_DATA).orElse(null);
        if (data == null) {
            return;
        }
        for (HintDefinition def : TTConfigManager.hints()) {
            checkAndShow(player, def, data);
        }
    }

    public static void checkPlayer(ServerPlayer player) {
        tickPlayer(player);
    }

    /**
     * duration <= 0：状态绑定模式（条件在就显示，条件没了就隐藏）
     * duration >  0：传统倒计时模式（受 once / max_times / cooldown 约束）
     */
    private static void checkAndShow(ServerPlayer player, HintDefinition def, PlayerHintData data) {
        boolean matches = def.matches(player);

        if (def.duration() <= 0) {
            boolean isActive = data.isPersistentlyActive(def.id());
            if (matches && !isActive) {
                sendShowPacket(player, def);               // duration 原样透传（-1）
                data.setPersistentlyActive(def.id(), true);
            } else if (!matches && isActive) {
                TTNetwork.sendToPlayer(player, new HideHintPacket(def.id()));
                data.setPersistentlyActive(def.id(), false);
            }
        } else {
            tryShow(player, def, data);
        }
    }

    private static void tryShow(ServerPlayer player, HintDefinition def, PlayerHintData data) {
        long now = player.level().getGameTime();

        if (def.maxTimes() > 0 && data.getCount(def.id()) >= def.maxTimes()) {
            return;
        }
        if (def.once() && data.hasShown(def.id())) {
            return;
        }
        if (data.isInCooldown(def.id(), now)) {
            return;
        }
        if (!def.matches(player)) {
            return;
        }

        data.incrementCount(def.id());   // 【关键修复】之前漏了这行，max_times 才不生效
        data.markShown(def.id());
        if (def.cooldown() > 0) {
            data.setCooldown(def.id(), now, def.cooldown());
        }

        sendShowPacket(player, def);
    }

    /** 正常触发：duration 原样透传，<=0 代表永久提示，由客户端和 HideHintPacket 管理生死 */
    private static void sendShowPacket(ServerPlayer player, HintDefinition def) {
        sendShowPacket(player, def, def.duration());
    }

    private static void sendShowPacket(ServerPlayer player, HintDefinition def, int duration) {
        System.out.println("【服务端】触发成功！准备发包给 " + player.getName().getString() + "，ID: " + def.id());
        String titleJson = def.title() != null ? def.title().toString() : "";
        TTNetwork.sendToPlayer(player, new ShowHintPacket(
                def.id(),
                def.text().toString(),
                titleJson,
                def.icon(),
                duration,
                def.priority(),
                def.accentColor(),
                def.sound()
        ));
    }

    /** /trackertips test：无视条件强制弹；永久提示临时用默认时长预览 */
    public static void forceShow(ServerPlayer player, ResourceLocation id) {
        HintDefinition def = null;
        for (HintDefinition d : TTConfigManager.hints()) {
            if (d.id().equals(id)) {
                def = d;
                break;
            }
        }

        if (def == null) {
            player.sendSystemMessage(Component.translatable("trackertips.command.test.not_found", id).withStyle(ChatFormatting.RED));
            return;
        }

        int duration = def.duration() > 0 ? def.duration() : TTConfigManager.settings().defaultDuration;
        sendShowPacket(player, def, duration);
        player.sendSystemMessage(Component.translatable("trackertips.command.test.sent", id).withStyle(ChatFormatting.GREEN));
    }
}