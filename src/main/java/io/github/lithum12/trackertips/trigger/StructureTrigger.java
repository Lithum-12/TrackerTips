package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * State trigger: true while the player is standing within the bounds of a piece of the given
 * structure (e.g. {@code minecraft:village_plains}, {@code minecraft:stronghold}, or a modded
 * structure's id). Configuration: {@code {"type": "trackertips:in_structure", "structure": "..."}}.
 *
 * <p>There's no dedicated "player entered a structure" Forge event to hook, so - like
 * {@code InDimensionTrigger}'s sibling {@code HealthBelowTrigger} and other purely spatial
 * conditions - this is polled on TrackerTips' regular check interval rather than event-driven.
 * A {@code duration: -1} hint using this trigger will stay visible for as long as the player
 * remains inside the structure and disappear once they leave, via {@link IHintTrigger#currentState}.
 */
public final class StructureTrigger implements IHintTrigger {
    private final String structureId;

    private StructureTrigger(String structureId) {
        this.structureId = structureId;
    }

    public static StructureTrigger fromJson(JsonObject json) {
        return new StructureTrigger(GsonHelper.getAsString(json, "structure", ""));
    }

    @Override
    public boolean test(ServerPlayer player) {
        if (structureId == null || structureId.isBlank()) return false;
        try {
            ServerLevel level = (ServerLevel) player.level();
            Structure structure = level.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE)
                    .get(new ResourceLocation(structureId));
            if (structure == null) return false;

            StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), structure);
            return start.isValid();
        } catch (Exception e) {
            // A malformed id (bad ResourceLocation, unknown structure) shouldn't crash the poll
            // loop for every other hint/trigger; just treat it as "not currently in range".
            return false;
        }
    }
}
