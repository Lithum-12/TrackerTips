package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 智能触发器（事件触发）：只在玩家“新获得”物品时触发一次，
 * 背包里一直存在不会重复触发。
 */
public class ItemObtainedTrigger implements IHintTrigger {

    private final Item item;
    private final int count;
    // 记录每个玩家上一次检查时的物品数量，用来计算“增量”
    private final Map<UUID, Integer> lastCounts = new HashMap<>();

    public ItemObtainedTrigger(Item item, int count) {
        this.item = item;
        this.count = count;
    }

    public static ItemObtainedTrigger fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "item"));
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            throw new IllegalArgumentException("[TrackerTips] 未知物品: " + id);
        }
        int count = GsonHelper.getAsInt(json, "count", 1);
        return new ItemObtainedTrigger(item, count);
    }

    @Override
    public boolean test(ServerPlayer player) {
        int current = countInInventory(player);
        Integer last = lastCounts.get(player.getUUID());

        // 该玩家第一次检查：只记录基准线，不触发（进游戏前就有的物品不算“获得”）
        if (last == null) {
            lastCounts.put(player.getUUID(), current);
            return false;
        }

        lastCounts.put(player.getUUID(), current);

        // 只有数量“增加”才算获得；一次捡 5 个也只算一次事件
        return current - last >= count;
    }

    private int countInInventory(ServerPlayer player) {
        int total = 0;
        // 主背包 + 盔甲 + 副手（共 41 格）
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            total += countStack(player.getInventory().getItem(i));
        }
        // 加上合成栏的 2x2 格子（槽位 1~4），
        // 避免“放进合成格 / 拿回来”被误判为获得
        for (int i = 1; i <= 4; i++) {
            total += countStack(player.inventoryMenu.getSlot(i).getItem());
        }
        return total;
    }

    private int countStack(ItemStack stack) {
        return stack.is(item) ? stack.getCount() : 0;
    }
}