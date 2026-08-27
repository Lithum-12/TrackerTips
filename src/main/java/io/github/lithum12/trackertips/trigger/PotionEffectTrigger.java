package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 药水效果触发器。
 * mode:
 *  - "added"   (默认) 刚获得效果的一瞬间触发（提示语义）
 *  - "active"  效果持续期间一直满足（状态语义，配合 cooldown）
 *  - "removed" 效果消失的一瞬间触发
 */
public class PotionEffectTrigger implements IHintTrigger {

    private final MobEffect effect;
    private final int amplifierMin;
    private final String mode;
    // 记录每个玩家上一次检查时是否拥有该效果，用于检测“变化”
    private final Map<UUID, Boolean> lastState = new HashMap<>();

    public PotionEffectTrigger(MobEffect effect, int amplifierMin, String mode) {
        this.effect = effect;
        this.amplifierMin = amplifierMin;
        this.mode = mode;
    }

    public static PotionEffectTrigger fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "effect"));
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect == null) {
            throw new IllegalArgumentException("[TrackerTips] 未知药水效果: " + id);
        }
        int amplifierMin = GsonHelper.getAsInt(json, "amplifier_min", 0);
        String mode = GsonHelper.getAsString(json, "mode", "added");
        return new PotionEffectTrigger(effect, amplifierMin, mode);
    }

    @Override
    public boolean test(ServerPlayer player) {
        boolean now = hasEffect(player);
        Boolean last = lastState.get(player.getUUID());

        // 第一次检查：只记录基准线，不触发（进游戏时身上已有的效果不算“新获得”）
        if (last == null) {
            lastState.put(player.getUUID(), now);
            return false;
        }
        lastState.put(player.getUUID(), now);

        return switch (mode) {
            case "active" -> now;          // 状态：有就触发
            case "removed" -> !now && last; // 下降沿：刚消失
            default -> now && !last;        // 上升沿：刚获得
        };
    }

    private boolean hasEffect(ServerPlayer player) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance != null && instance.getAmplifier() >= amplifierMin;
    }
}