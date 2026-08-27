package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MineBlockTrigger implements IHintTrigger {
    private final Block block;
    private final Map<UUID, Integer> lastMineCount = new HashMap<>();

    public MineBlockTrigger(Block block) {
        this.block = block;
    }

    public static MineBlockTrigger fromJson(JsonObject json) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(GsonHelper.getAsString(json, "block")));
        if (b == null) throw new IllegalArgumentException("[TrackerTips] 未知方块: " + json.get("block"));
        return new MineBlockTrigger(b);
    }

    @Override
    public boolean test(ServerPlayer player) {
        int currentMined = player.getStats().getValue(Stats.BLOCK_MINED, block);
        Integer lastMined = lastMineCount.get(player.getUUID());

        if (lastMined == null) {
            lastMineCount.put(player.getUUID(), currentMined);
            return false;
        }
        lastMineCount.put(player.getUUID(), currentMined);

        // 只要数量增加，就是刚挖到
        return currentMined > lastMined;
    }
}