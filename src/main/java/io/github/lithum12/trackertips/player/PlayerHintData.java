package io.github.lithum12.trackertips.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;   // Added import
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-player bookkeeping for TrackerTips: which hints have been shown, when they can trigger
 * again, how many times each has fired, which persistent/state hints are currently active, which
 * hints are currently listening for a {@code HintChain} follow-up condition, and whether this
 * player has ever joined a world running TrackerTips before.
 *
 * <p>An instance is attached to every {@code Player} via a Forge capability (see
 * {@code TTCapabilities} / {@code PlayerHintDataProvider}) and is serialized to/from that
 * player's saved data, so all of this survives logout, server restarts, and (through the vanilla
 * clone path handled in {@code TTCommonEvents#onPlayerClone}) death and respawn.
 *
 * <p>This class is intentionally not part of the addon-facing API surface - trigger/theme/content
 * extension points ({@code Triggers}, {@code TTThemeManager}, {@code TTConfigManager}) don't
 * require touching player state directly. It's documented here mainly so the persistence format
 * (and the invariants each field relies on) is clear to future maintainers.
 */
public class PlayerHintData {

    private final Set<ResourceLocation> shown = new HashSet<>();
    private final Map<ResourceLocation, Long> cooldownUntil = new HashMap<>();
    private final Map<ResourceLocation, Integer> triggerCounts = new HashMap<>();

    // ===== Added: persistently-active hint set =====
    private final Set<ResourceLocation> activePersistent = new HashSet<>();

    /**
     * Feature: nested/chained listeners. Hint ids currently listening for their configured
     * {@code HintChain}'s follow-up trigger, because that hint has been shown and hasn't yet had
     * its chain resolved (dismissed or advanced to the next hint). See {@code HintEngine}'s
     * chain-checking pass for how this set is consumed.
     */
    private final Set<ResourceLocation> chainListening = new HashSet<>();

    /**
     * Feature: first-join trigger. Whether this player has ever logged into a world running
     * TrackerTips before "now". Set the first time {@code TTCommonEvents#onPlayerLoggedIn}
     * observes the player; see {@code io.github.lithum12.trackertips.trigger.FirstJoinTrigger}.
     */
    private boolean joinedBefore;

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

    // ===== Added: persistently-active state query/update =====
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

    /** @return whether {@code id} currently has an active chain listener (see {@link #chainListening}). */
    public boolean isChainListening(ResourceLocation id) {
        return chainListening.contains(id);
    }

    /** Starts listening for {@code id}'s configured {@code HintChain} follow-up trigger. */
    public void startChainListening(ResourceLocation id) {
        chainListening.add(id);
    }

    /** Stops listening for {@code id}'s chain, e.g. once it matches or the hint is otherwise hidden. */
    public void stopChainListening(ResourceLocation id) {
        chainListening.remove(id);
    }

    /**
     * A snapshot copy of the currently chain-listening hint ids, safe to iterate while calling
     * {@link #stopChainListening}/{@link #startChainListening} for entries within it.
     */
    public Set<ResourceLocation> chainListeningIds() {
        return Set.copyOf(chainListening);
    }

    /** @return whether this player has ever joined a world running TrackerTips before "now". */
    public boolean hasJoinedBefore() {
        return joinedBefore;
    }

    /** Marks this player as having joined at least once; irreversible for the lifetime of their save data. */
    public void markJoinedBefore() {
        joinedBefore = true;
    }

    public void clearAll() {
        shown.clear();
        cooldownUntil.clear();
        triggerCounts.clear();
        // ===== Clear the persistently-active set =====
        activePersistent.clear();
        chainListening.clear();
        joinedBefore = false;
    }

    public void copyFrom(PlayerHintData other) {
        shown.clear();
        shown.addAll(other.shown);
        cooldownUntil.clear();
        cooldownUntil.putAll(other.cooldownUntil);
        triggerCounts.clear();
        triggerCounts.putAll(other.triggerCounts);
        // ===== Copy persistently-active state =====
        activePersistent.clear();
        activePersistent.addAll(other.activePersistent);
        chainListening.clear();
        chainListening.addAll(other.chainListening);
        joinedBefore = other.joinedBefore;
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

        // ===== Save the persistently-active list =====
        ListTag activeList = new ListTag();
        for (ResourceLocation id : activePersistent) {
            activeList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("ActivePersistent", activeList);

        ListTag chainList = new ListTag();
        for (ResourceLocation id : chainListening) {
            chainList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("ChainListening", chainList);

        tag.putBoolean("JoinedBefore", joinedBefore);

        return tag;
    }

    public void deserialize(CompoundTag tag) {
        shown.clear();
        cooldownUntil.clear();
        triggerCounts.clear();
        activePersistent.clear();   // Clear before reading
        chainListening.clear();

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

        // ===== Read the persistently-active list =====
        ListTag activeList = tag.getList("ActivePersistent", Tag.TAG_STRING);
        for (Tag t : activeList) {
            activePersistent.add(new ResourceLocation(t.getAsString()));
        }

        ListTag chainList = tag.getList("ChainListening", Tag.TAG_STRING);
        for (Tag t : chainList) {
            chainListening.add(new ResourceLocation(t.getAsString()));
        }

        joinedBefore = tag.getBoolean("JoinedBefore");
    }
}
