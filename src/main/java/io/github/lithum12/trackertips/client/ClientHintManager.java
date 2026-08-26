package io.github.lithum12.trackertips.client;

import io.github.lithum12.trackertips.network.ShowHintPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientHintManager {

    public static final List<ActiveHint> ACTIVE = new ArrayList<>();

    public static void show(ShowHintPacket packet) {
        MutableComponent text = Component.Serializer.fromJson(packet.textJson());
        if (text == null) {
            text = Component.literal(packet.textJson());
        }

        ACTIVE.add(new ActiveHint(text, packet.duration(), packet.priority(), packet.accent()));
        ACTIVE.sort(Comparator.comparingInt(ActiveHint::priority).reversed());

        if (!packet.sound().isEmpty()) {
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(packet.sound()));
            if (sound != null && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.playSound(sound, 1.0F, 1.0F);
            }
        }
    }

    public static void tick() {
        ACTIVE.removeIf(ActiveHint::tick);
    }

    public static class ActiveHint {

        private final Component text;
        private final int duration;
        private final int priority;
        private final int accent;
        private int age = 0;

        public ActiveHint(Component text, int duration, int priority, int accent) {
            this.text = text;
            this.duration = duration;
            this.priority = priority;
            this.accent = accent;
        }

        public boolean tick() {
            age++;
            return age >= duration;
        }

        public float alpha(int fadeIn, int fadeOut) {
            if (age < fadeIn) {
                return age / (float) fadeIn;
            }
            int remaining = duration - age;
            if (remaining < fadeOut) {
                return Math.max(0, remaining / (float) fadeOut);
            }
            return 1.0F;
        }

        public Component text() { return text; }
        public int priority() { return priority; }
        public int accent() { return accent; }
    }
}