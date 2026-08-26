package io.github.lithum12.trackertips.event;

import io.github.lithum12.trackertips.TrackerTips;
import io.github.lithum12.trackertips.config.TTConfigManager;
import io.github.lithum12.trackertips.engine.HintEngine;
import io.github.lithum12.trackertips.player.PlayerHintDataProvider;
import io.github.lithum12.trackertips.player.TTCapabilities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegisterCommandsEvent;
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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        int interval = Math.max(1, TTConfigManager.settings().checkInterval);
        if (serverPlayer.tickCount % interval != 0) {
            return;
        }
        HintEngine.tickPlayer(serverPlayer);
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            HintEngine.checkPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TTCommands.register(event.getDispatcher());
    }
}