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

    /**
     * Feature: first-join trigger. Fires {@link TriggerEvent#firstJoin()} exactly once per
     * player - the very first time they're ever observed logging in - then permanently marks
     * them as having joined (see {@code PlayerHintData#markJoinedBefore}) so it never fires
     * again for that player, even across server restarts.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getCapability(TTCapabilities.HINT_DATA).ifPresent(data -> {
            if (data.hasJoinedBefore()) return;
            data.markJoinedBefore();
            HintEngine.triggerEvent(player, TriggerEvent.firstJoin());
        });
    }

    /** Regular state conditions are still polled at the configured interval. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer serverPlayer)) return;

        int interval = Math.max(1, TTConfigManager.settings().checkInterval);
        if (serverPlayer.tickCount % interval != 0) return;
        HintEngine.tickPlayer(serverPlayer);
    }

    /** Player picks up an item: no longer relies on polling inventory-count deltas, to avoid missing transient events. */
    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HintEngine.triggerEvent(player, TriggerEvent.itemObtained(event.getStack()));
        }
    }

    /** Player kills an entity. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity() == player) return;
        HintEngine.triggerEvent(player, TriggerEvent.kill(event.getEntity()));
    }

    /** Player breaks a block. */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            HintEngine.triggerEvent(player, TriggerEvent.mine(event.getState()));
        }
    }

    /** Potion effect gained. checkPlayer() is intentionally not called here, to avoid consuming the "added" event early. */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HintEngine.triggerEvent(player, TriggerEvent.potionAdded(event.getEffectInstance()));
        }
    }

    /** Potion effect removed. */
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HintEngine.triggerEvent(player, TriggerEvent.potionRemoved(event.getEffectInstance()));
        }
    }

    /** Advancement completed. */
    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HintEngine.triggerEvent(player, TriggerEvent.advancement(event.getAdvancement()));
        }
    }

    /** Player changes dimension. */
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
