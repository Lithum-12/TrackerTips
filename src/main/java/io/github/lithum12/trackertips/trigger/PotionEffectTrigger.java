package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 药水效果触发器。
 * added / removed 使用 Forge 原生事件，active 使用普通状态轮询。
 */
public class PotionEffectTrigger implements IEventHintTrigger {
    private final MobEffect effect;
    private final int amplifierMin;
    private final String mode;

    public PotionEffectTrigger(MobEffect effect, int amplifierMin, String mode) {
        this.effect = effect;
        this.amplifierMin = amplifierMin;
        this.mode = mode;
    }

    public static PotionEffectTrigger fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "effect"));
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect == null) {
            throw new IllegalArgumentException("[TrackerTips] Unknown effect: " + id);
        }
        return new PotionEffectTrigger(effect,
                GsonHelper.getAsInt(json, "amplifier_min", 0),
                GsonHelper.getAsString(json, "mode", "added"));
    }

    @Override
    public boolean isEventDriven() {
        return !"active".equalsIgnoreCase(mode);
    }

    @Override
    public boolean matchesEvent(ServerPlayer player, TriggerEvent event) {
        if ("active".equalsIgnoreCase(mode)) return false;
        if (event.effect() == null) return false;
        if (!hasEffect(event.effect())) return false;

        if ("removed".equalsIgnoreCase(mode)) {
            return event.type() == TriggerEvent.Type.POTION_REMOVED;
        }
        return event.type() == TriggerEvent.Type.POTION_ADDED;
    }

    @Override
    public boolean test(ServerPlayer player) {
        if (!"active".equalsIgnoreCase(mode)) return false;
        MobEffectInstance instance = player.getEffect(effect);
        return instance != null && instance.getAmplifier() >= amplifierMin;
    }

    private boolean hasEffect(MobEffectInstance instance) {
        return instance.getEffect() == effect && instance.getAmplifier() >= amplifierMin;
    }
}
