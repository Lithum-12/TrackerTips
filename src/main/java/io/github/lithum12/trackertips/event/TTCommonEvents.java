package io.github.lithum12.trackertips.event;

import io.github.lithum12.trackertips.TrackerTips;
import io.github.lithum12.trackertips.config.TTConfigManager;
import io.github.lithum12.trackertips.engine.HintEngine;
import io.github.lithum12.trackertips.player.PlayerHintDataProvider;
import io.github.lithum12.trackertips.player.TTCapabilities;
import io.github.lithum12.trackertips.trigger.TriggerEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import io.github.lithum12.trackertips.command.TTCommands;

@Mod.EventBusSubscriber(modid = TrackerTips.MODID)
public class TTCommonEvents {

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        TTConfigManager.init(event.getServer());
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation(TrackerTips.MODID, "hint_data"),
                    new PlayerHintDataProvider()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(TTCapabilities.HINT_DATA).ifPresent(oldData ->
                event.getEntity().getCapability(TTCapabilities.HINT_DATA).ifPresent(newData ->
                        newData.copyFrom(oldData)));
        event.getOriginal().invalidateCaps();
    }

    /** 普通状态条件仍按配置的 interval 轮询。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer serverPlayer)) return;

        int interval = Math.max(1, TTConfigManager.settings().checkInterval);
        if (serverPlayer.tickCount % interval != 0) return;
        HintEngine.tickPlayer(serverPlayer);
    }

    /** 玩家拾取物品：不再靠背包数量差值轮询，避免漏掉瞬时事件。 */
    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HintEngine.triggerEvent(player, TriggerEvent.itemObtained(event.getStack()));
        }
    }

    /** 玩家击杀实体。 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity() == player) return;
        HintEngine.triggerEvent(player, TriggerEvent.kill(event.getEntity()));
    }

    /** 玩家破坏方块。 */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            HintEngine.triggerEvent(player, TriggerEvent.mine(event.getState()));
        }
    }

    /** 获得药水效果。这里不再调用 checkPlayer()，避免提前吃掉“added”事件。 */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HintEngine.triggerEvent(player, TriggerEvent.potionAdded(event.getEffectInstance()));
        }
    }

    /** 药水效果移除。 */
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HintEngine.triggerEvent(player, TriggerEvent.potionRemoved(event.getEffectInstance()));
        }
    }

    /** 进度达成。 */
    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HintEngine.triggerEvent(player, TriggerEvent.advancement(event.getAdvancement()));
        }
    }

    /** 玩家切换维度。 */
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HintEngine.triggerEvent(player, TriggerEvent.dimensionChange(
                    event.getFrom(), event.getTo()));
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TTCommands.register(event.getDispatcher());
    }
}
