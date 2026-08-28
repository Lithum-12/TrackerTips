package io.github.lithum12.trackertips.client;

import io.github.lithum12.trackertips.network.ShowHintPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientHintManager {
    public static final List<ActiveHint> ACTIVE = new ArrayList<>();
    private static boolean visible = true;

    public static boolean isVisible() { return visible; }
    public static boolean toggleVisible() {
        visible = !visible;
        return visible;
    }

    public static void show(ShowHintPacket packet) {
        for (ActiveHint hint : ACTIVE) {
            if (hint.id().equals(packet.id())) {
                // 🚨【致命修复 2】防包轰炸机制：防止服务端每 Tick 疯狂发包导致 age 被无限重置为 0，从而永远不消失
                if (hint.duration <= 0) return; // 永久提示已存在，无需刷新
                if (hint.age < 20) return;     // 刚显示不到1秒(20tick)，忽略重复包

                hint.refresh(packet.duration());
                return;
            }
        }

        MutableComponent text = Component.Serializer.fromJson(packet.textJson());
        if (text == null) text = Component.literal(packet.textJson());
        Component title = null;
        if (!packet.titleJson().isEmpty()) {
            title = Component.Serializer.fromJson(packet.titleJson());
        }
        ItemStack icon = ItemStack.EMPTY;
        if (!packet.icon().isEmpty()) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(packet.icon()));
            if (item != null) icon = new ItemStack(item);
        }

        ACTIVE.add(new ActiveHint(packet.id(), title, text, icon,
                packet.duration(), packet.priority(), packet.accent(), packet.theme()));
        ACTIVE.sort(Comparator.comparingInt(ActiveHint::priority).reversed());

        if (!packet.sound().isEmpty()) {
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(packet.sound()));
            if (sound != null && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.playSound(sound, 1.0F, 1.0F);
            }
        }
    }

    public static void hide(ResourceLocation id) {
        ACTIVE.removeIf(h -> h.id().equals(id));
    }

    public static void dismissCurrent() {
        if (!ACTIVE.isEmpty()) {
            ACTIVE.remove(0);
        }
    }

    public static void tick() {
        ACTIVE.removeIf(ActiveHint::tick);
    }

    public static class ActiveHint {
        private final ResourceLocation id;
        private final Component title;
        private final Component text;
        private final ItemStack icon;
        private final int priority;
        private final int accent;
        private final String theme;
        private int duration;
        public int age = 0; // 改为 public 方便外部防轰炸判断

        public ActiveHint(ResourceLocation id, Component title, Component text, ItemStack icon,
                          int duration, int priority, int accent, String theme) {
            this.id = id;
            this.title = title;
            this.text = text;
            this.icon = icon;
            this.duration = duration;
            this.priority = priority;
            this.accent = accent;
            this.theme = theme;
        }

        public boolean tick() {
            if (duration <= 0) return false;
            age++;
            return age >= duration;
        }

        public void refresh(int newDuration) {
            age = 0;
            duration = newDuration;
        }

        public float alpha(int fadeIn, int fadeOut) {
            if (fadeIn > 0 && age < fadeIn) return age / (float) fadeIn;
            if (duration <= 0) return 1.0F;
            int remaining = duration - age;
            if (fadeOut > 0 && remaining < fadeOut) return Math.max(0, remaining / (float) fadeOut);
            return 1.0F;
        }

        public ResourceLocation id() { return id; }
        public Component title() { return title; }
        public Component text() { return text; }
        public ItemStack icon() { return icon; }
        public int priority() { return priority; }
        public int accent() { return accent; }
        public String theme() { return theme; }
    }
}