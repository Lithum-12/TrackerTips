package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

public class PotionEffectTrigger implements IHintTrigger {

    private final MobEffect effect;
    private final int amplifierMin;

    public PotionEffectTrigger(MobEffect effect, int amplifierMin) {
        this.effect = effect;
        this.amplifierMin = amplifierMin;
    }

    public static PotionEffectTrigger fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "effect"));
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect == null) {
            throw new IllegalArgumentException("未知药水效果: " + id);
        }
        int amplifierMin = GsonHelper.getAsInt(json, "amplifier_min", 0);
        return new PotionEffectTrigger(effect, amplifierMin);
    }

    @Override
    public boolean test(ServerPlayer player) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance != null && instance.getAmplifier() >= amplifierMin;
    }
}