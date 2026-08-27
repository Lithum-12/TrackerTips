package io.github.lithum12.trackertips.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;   // 新增导入
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerHintData {

    private final Set<ResourceLocation> shown = new HashSet<>();
    private final Map<ResourceLocation, Long> cooldownUntil = new HashMap<>();
    private final Map<ResourceLocation, Integer> triggerCounts = new HashMap<>();

    // ===== 新增：持久活跃提示集合 =====
    private final Set<ResourceLocation> activePersistent = new HashSet<>();

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

    public int getCount(ResourceLocation id) {
        return triggerCounts.getOrDefault(id, 0);
    }

    public void incrementCount(ResourceLocation id) {
        triggerCounts.put(id, getCount(id) + 1);
    }

    // ===== 新增：持久活跃状态查询与更新 =====
    public boolean isPersistentlyActive(ResourceLocation id) {
        return activePersistent.contains(id);
    }

    public void setPersistentlyActive(ResourceLocation id, boolean active) {
        if (active) {
            activePersistent.add(id);
        } else {
            activePersistent.remove(id);
        }
    }

    public void clearAll() {
        shown.clear();
        cooldownUntil.clear();
        triggerCounts.clear();
        // ===== 清空持久活跃集合 =====
        activePersistent.clear();
    }

    public void copyFrom(PlayerHintData other) {
        shown.clear();
        shown.addAll(other.shown);
        cooldownUntil.clear();
        cooldownUntil.putAll(other.cooldownUntil);
        triggerCounts.clear();
        triggerCounts.putAll(other.triggerCounts);
        // ===== 拷贝持久活跃状态 =====
        activePersistent.clear();
        activePersistent.addAll(other.activePersistent);
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();

        ListTag shownList = new ListTag();
        for (ResourceLocation id : shown) {
            shownList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("Shown", shownList);

        CompoundTag cooldowns = new CompoundTag();
        cooldownUntil.forEach((id, until) -> cooldowns.putLong(id.toString(), until));
        tag.put("Cooldowns", cooldowns);

        CompoundTag counts = new CompoundTag();
        triggerCounts.forEach((id, count) -> counts.putInt(id.toString(), count));
        tag.put("TriggerCounts", counts);

        // ===== 保存持久活跃列表 =====
        ListTag activeList = new ListTag();
        for (ResourceLocation id : activePersistent) {
            activeList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("ActivePersistent", activeList);

        return tag;
    }

    public void deserialize(CompoundTag tag) {
        shown.clear();
        cooldownUntil.clear();
        triggerCounts.clear();
        activePersistent.clear();   // 清空再读取

        ListTag shownList = tag.getList("Shown", Tag.TAG_STRING);
        for (Tag t : shownList) {
            shown.add(new ResourceLocation(t.getAsString()));
        }

        CompoundTag cooldowns = tag.getCompound("Cooldowns");
        for (String key : cooldowns.getAllKeys()) {
            cooldownUntil.put(new ResourceLocation(key), cooldowns.getLong(key));
        }

        CompoundTag counts = tag.getCompound("TriggerCounts");
        for (String key : counts.getAllKeys()) {
            triggerCounts.put(new ResourceLocation(key), counts.getInt(key));
        }

        // ===== 读取持久活跃列表 =====
        ListTag activeList = tag.getList("ActivePersistent", Tag.TAG_STRING);
        for (Tag t : activeList) {
            activePersistent.add(new ResourceLocation(t.getAsString()));
        }
    }
}