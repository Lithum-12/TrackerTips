package io.github.lithum12.trackertips.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerHintData {

    private final Set<ResourceLocation> shown = new HashSet<>();
    private final Map<ResourceLocation, Long> cooldownUntil = new HashMap<>();

    public boolean hasShown(ResourceLocation id) {
        return shown.contains(id);
    }

    public void markShown(ResourceLocation id) {
        shown.add(id);
    }

    public boolean isInCooldown(ResourceLocation id, long gameTime) {
        Long until = cooldownUntil.get(id);
        return until != null && gameTime < until;
    }

    public void setCooldown(ResourceLocation id, long gameTime, int cooldownTicks) {
        cooldownUntil.put(id, gameTime + cooldownTicks);
    }

    public void copyFrom(PlayerHintData other) {
        shown.clear();
        shown.addAll(other.shown);
        cooldownUntil.clear();
        cooldownUntil.putAll(other.cooldownUntil);
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();

        ListTag shownList = new ListTag();
        for (ResourceLocation id : shown) {
            shownList.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        }
        tag.put("Shown", shownList);

        CompoundTag cooldowns = new CompoundTag();
        cooldownUntil.forEach((id, until) -> cooldowns.putLong(id.toString(), until));
        tag.put("Cooldowns", cooldowns);

        return tag;
    }

    public void deserialize(CompoundTag tag) {
        shown.clear();
        cooldownUntil.clear();

        ListTag shownList = tag.getList("Shown", Tag.TAG_STRING);
        for (Tag t : shownList) {
            shown.add(new ResourceLocation(t.getAsString()));
        }

        CompoundTag cooldowns = tag.getCompound("Cooldowns");
        for (String key : cooldowns.getAllKeys()) {
            cooldownUntil.put(new ResourceLocation(key), cooldowns.getLong(key));
        }
    }
}