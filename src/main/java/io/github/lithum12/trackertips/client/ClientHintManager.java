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

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

@OnlyIn(Dist.CLIENT)
public class ClientHintManager {

    public static final List<ActiveHint> ACTIVE = new ArrayList<>();

    public static void show(ShowHintPacket packet) {
        // 【防重复刷屏】如果屏幕上已经有这个 ID 的提示，就重置它的年龄（刷新持续时间）
        for (ActiveHint existing : ACTIVE) {
            if (existing.id().equals(packet.id())) {
                existing.refresh(packet.duration());
                return; // 找到相同的了，刷新后直接退出，不再往下添加
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
            if (item != null) {
                icon = new ItemStack(item);
            }
        }

        // 传入 7 个参数（包含 id）
        ACTIVE.add(new ActiveHint(packet.id(), title, text, icon, packet.duration(), packet.priority(), packet.accent()));
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
        // 【补全】记录 ID
        private final ResourceLocation id;
        private final Component title;
        private final Component text;
        private final ItemStack icon;
        // 【修改】去掉 final，以便 refresh 方法修改它
        private int duration;
        private final int priority;
        private final int accent;
        private int age = 0;

        // 【补全】构造函数增加 ResourceLocation id 参数
        public ActiveHint(ResourceLocation id, Component title, Component text, ItemStack icon, int duration, int priority, int accent) {
            this.id = id;
            this.title = title;
            this.text = text;
            this.icon = icon;
            this.duration = duration;
            this.priority = priority;
            this.accent = accent;
        }

        // 【补全】刷新持续时间的方法
        public void refresh(int newDuration) {
            this.age = 0;
            this.duration = newDuration;
        }

        public boolean tick() {
            age++;
            return age >= duration;
        }

        public float alpha(int fadeIn, int fadeOut) {
            if (age < fadeIn) return age / (float) fadeIn;
            int remaining = duration - age;
            if (remaining < fadeOut) return Math.max(0, remaining / (float) fadeOut);
            return 1.0F;
        }

        // 【补全】获取 ID 的方法
        public ResourceLocation id() { return id; }
        public Component title() { return title; }
        public Component text() { return text; }
        public ItemStack icon() { return icon; }
        public int priority() { return priority; }
        public int accent() { return accent; }
    }
}