package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/** 通过 BlockEvent.BreakEvent 精确检测方块破坏。 */
public class MineBlockTrigger implements IEventHintTrigger {
    private final Block block;

    public MineBlockTrigger(Block block) { this.block = block; }

    public static MineBlockTrigger fromJson(JsonObject json) {
        Block b = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation(GsonHelper.getAsString(json, "block")));
        if (b == null) throw new IllegalArgumentException("[TrackerTips] Unknown block: " + json.get("block"));
        return new MineBlockTrigger(b);
    }

    @Override
    public boolean matchesEvent(ServerPlayer player, TriggerEvent event) {
        return event.type() == TriggerEvent.Type.MINE_BLOCK
                && event.blockState() != null
                && event.blockState().is(block);
    }
}
