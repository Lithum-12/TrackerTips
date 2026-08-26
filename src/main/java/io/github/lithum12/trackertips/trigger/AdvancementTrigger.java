package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import io.github.lithum12.trackertips.TrackerTips;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public class AdvancementTrigger implements IHintTrigger {

    private final ResourceLocation advancementId;
    private boolean warned = false; // 只警告一次，避免刷屏

    public AdvancementTrigger(ResourceLocation advancementId) {
        this.advancementId = advancementId;
    }

    public static AdvancementTrigger fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "id"));
        return new AdvancementTrigger(id);
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
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
}