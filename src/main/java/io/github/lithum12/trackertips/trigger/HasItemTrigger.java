package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import io.github.lithum12.trackertips.trigger.IHintTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class HasItemTrigger implements IHintTrigger {

    private final Item item;
    private final int count;

    public HasItemTrigger(Item item, int count) {
        this.item = item;
        this.count = count;
    }

    public static HasItemTrigger fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "item"));
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            throw new IllegalArgumentException("[TrackerTips] 未知物品: " + id);
        }
        int count = GsonHelper.getAsInt(json, "count", 1);
        return new HasItemTrigger(item, count);
    }

    @Override
    public boolean test(ServerPlayer player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total >= count;
    }
}