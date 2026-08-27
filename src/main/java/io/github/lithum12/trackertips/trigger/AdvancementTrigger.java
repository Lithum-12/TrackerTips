package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import io.github.lithum12.trackertips.TrackerTips;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 进度触发器。
 * mode:
 *  - "done"  (默认) 刚达成进度的一瞬间触发（提示语义）
 *  - "state"  进度已完成就一直满足（状态语义）
 */
public class AdvancementTrigger implements IHintTrigger {

    private final ResourceLocation advancementId;
    private final String mode;
    private final Map<UUID, Boolean> lastState = new HashMap<>();
    private boolean warned = false; // 只警告一次，防止刷屏

    public AdvancementTrigger(ResourceLocation advancementId, String mode) {
        this.advancementId = advancementId;
        this.mode = mode;
    }

    public static AdvancementTrigger fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "id"));
        String mode = GsonHelper.getAsString(json, "mode", "done");
        return new AdvancementTrigger(id, mode);
    }

    @Override
    public boolean test(ServerPlayer player) {
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            if (!warned) {
                warned = true;
                TrackerTips.LOGGER.warn("[TrackerTips] 进度不存在，请检查 JSON 中的 id: {}", advancementId);
            }
            return false;
        }

        boolean now = player.getAdvancements().getOrStartProgress(advancement).isDone();
        Boolean last = lastState.get(player.getUUID());

        // 第一次检查：记录基准线
        if (last == null) {
            lastState.put(player.getUUID(), now);
            // state 模式允许对“之前就完成”的进度提示；done 模式只认新达成
            return "state".equals(mode) && now;
        }
        lastState.put(player.getUUID(), now);

        return switch (mode) {
            case "state" -> now;
            default -> now && !last; // 上升沿：刚达成
        };
    }
}