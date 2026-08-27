package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import io.github.lithum12.trackertips.TrackerTips;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

/** 进度触发器：done 使用 AdvancementEvent，state 使用状态轮询。 */
public class AdvancementTrigger implements IEventHintTrigger {
    private final ResourceLocation advancementId;
    private final String mode;
    private boolean warned = false;

    public AdvancementTrigger(ResourceLocation advancementId, String mode) {
        this.advancementId = advancementId;
        this.mode = mode;
    }

    public static AdvancementTrigger fromJson(JsonObject json) {
        return new AdvancementTrigger(
                new ResourceLocation(GsonHelper.getAsString(json, "id")),
                GsonHelper.getAsString(json, "mode", "done"));
    }

    @Override
    public boolean matchesEvent(ServerPlayer player, TriggerEvent event) {
        if ("state".equalsIgnoreCase(mode) || event.type() != TriggerEvent.Type.ADVANCEMENT) {
            return false;
        }
        Advancement advancement = event.advancement();
        return advancement != null && advancementId.equals(advancement.getId());
    }

    @Override
    public boolean test(ServerPlayer player) {
        if (!"state".equalsIgnoreCase(mode)) return false;
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            if (!warned) {
                warned = true;
                TrackerTips.LOGGER.warn("[TrackerTips] Advancement not found; check the JSON id: {}", advancementId);
            }
            return false;
        }
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
}
