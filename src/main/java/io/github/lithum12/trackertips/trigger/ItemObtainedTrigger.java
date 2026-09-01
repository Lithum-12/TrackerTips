package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Precisely detects newly obtained items via Forge's ItemPickupEvent. */
public class ItemObtainedTrigger implements IEventHintTrigger {
    private final Item item;
    private final int count;

    public ItemObtainedTrigger(Item item, int count) {
        this.item = item;
        this.count = Math.max(1, count);
    }

    public static ItemObtainedTrigger fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "item"));
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            throw new IllegalArgumentException("[TrackerTips] Unknown item: " + id);
        }
        return new ItemObtainedTrigger(item, GsonHelper.getAsInt(json, "count", 1));
    }

    @Override
    public boolean matchesEvent(ServerPlayer player, TriggerEvent event) {
        if (event.type() != TriggerEvent.Type.ITEM_OBTAINED || event.itemStack() == null) {
            return false;
        }
        ItemStack stack = event.itemStack();
        return stack.is(item) && stack.getCount() >= count;
    }
}
